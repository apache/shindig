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

import org.apache.shindig.common.servlet.InjectedFilter;

import com.google.inject.Inject;

import java.io.IOException;
import java.util.List;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
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
    for (AuthenticationHandler handler : handlers) {
      SecurityToken token = handler.getSecurityTokenFromRequest(req);
      if (token != null) {
        new AuthInfo(req).setAuthType(handler.getName()).setSecurityToken(token);
        chain.doFilter(req, response);
        return;
      } else {
          String authHeader = handler.getWWWAuthenticateHeader(realm);
          if (authHeader != null) {
              resp.addHeader("WWW-Authenticate", authHeader);
          }
      }
    }
    // We did not find a security token so we will just pass null
    chain.doFilter(req, response);
  }
}
