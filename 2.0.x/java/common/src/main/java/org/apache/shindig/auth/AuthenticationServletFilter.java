/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.auth;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import org.apache.shindig.common.servlet.InjectedFilter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

/**
 * Filter that attempts to authenticate an incoming HTTP request. It uses the guice injected
 * AuthenticationHandlers to try and generate a SecurityToken from the request. Once it finds a non
 * null token it passes that on the chain.
 *
 * If you wish to add a container specific type of auth system simply register an
 * additional handler.
 */
public class AuthenticationServletFilter extends InjectedFilter {
  public static final String AUTH_TYPE_OAUTH = "OAuth";

  // At some point change this to a container specific realm
  private static final String realm = "shindig";

  private List<AuthenticationHandler> handlers;

  private static final Logger LOG = Logger.getLogger(
      AuthenticationServletFilter.class.getName());

  @Inject
  public void setAuthenticationHandlers(List<AuthenticationHandler> handlers) {
    this.handlers = handlers;
  }

  public void destroy() { }

  public void doFilter(ServletRequest request, ServletResponse response,
      FilterChain chain) throws IOException, ServletException {

    if (!(request instanceof HttpServletRequest && response instanceof HttpServletResponse)) {
      throw new ServletException("Auth filter can only handle HTTP");
    }

    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse resp = (HttpServletResponse) response;

    try {
      for (AuthenticationHandler handler : handlers) {
        SecurityToken token = handler.getSecurityTokenFromRequest(req);
        if (token != null) {
          new AuthInfo(req).setAuthType(handler.getName()).setSecurityToken(token);
          callChain(chain, req, resp);
          return;
        } else {
          String authHeader = handler.getWWWAuthenticateHeader(realm);
          if (authHeader != null) {
              resp.addHeader("WWW-Authenticate", authHeader);
          }
        }
      }

      // We did not find a security token so we will just pass null
      callChain(chain, req, resp);
    } catch (AuthenticationHandler.InvalidAuthenticationException iae) {
      Throwable cause = iae.getCause();
      LOG.log(Level.INFO, iae.getMessage(), cause);

      if (iae.getAdditionalHeaders() != null) {
        for (Map.Entry<String,String> entry : iae.getAdditionalHeaders().entrySet()) {
          resp.addHeader(entry.getKey(), entry.getValue());
        }
      }
      if (iae.getRedirect() != null) {
        resp.sendRedirect(iae.getRedirect());
      } else {
        // For now append the cause message if set, this allows us to send any underlying oauth errors
        String message = (cause==null) ? iae.getMessage() : iae.getMessage() + cause.getMessage();

        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, message);
      }
    }
  }

  private void callChain(FilterChain chain, HttpServletRequest request,
      HttpServletResponse response) throws IOException, ServletException {
    if (request.getAttribute(AuthenticationHandler.STASHED_BODY) != null) {
      chain.doFilter(new StashedBodyRequestwrapper(request), response);
    } else {
      chain.doFilter(request, response);      
    }
  }

  private static class StashedBodyRequestwrapper extends HttpServletRequestWrapper {

    final InputStream rawStream;
    ServletInputStream stream;
    BufferedReader reader;


    StashedBodyRequestwrapper(HttpServletRequest wrapped) {
      super(wrapped);
      rawStream = new ByteArrayInputStream(
          (byte[])wrapped.getAttribute(AuthenticationHandler.STASHED_BODY));
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
      Preconditions.checkState(reader == null, "The methods getInputStream() and getReader() are mutually exclusive.");

      if (stream == null) {
        stream = new ServletInputStream() {
          public int read() throws IOException {
            return rawStream.read();
          }
        };
      }
      return stream;
    }

    @Override
    public BufferedReader getReader() throws IOException {
      Preconditions.checkState(stream == null, "The methods getInputStream() and getReader() are mutually exclusive.");

      if (reader == null) {
        Charset charset = Charset.forName(getCharacterEncoding());
        if (charset == null) {
          charset =  Charsets.UTF_8;
        }
        reader = new BufferedReader(new InputStreamReader(rawStream, charset));
      }
      return reader;
    }
  }
}
