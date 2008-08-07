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
package org.apache.shindig.social.core.oauth;

import org.apache.shindig.common.SecurityToken;
import org.apache.shindig.common.servlet.GuiceServletContextListener;
import org.apache.shindig.social.opensocial.oauth.AuthenticationHandler;

import com.google.inject.Inject;
import com.google.inject.Injector;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import java.util.List;

/**
 * Filter that attempts to authenticate an incoming HTTP request. It uses the guice injected
 * AuthenticationHandlers to try and generate a SecurityToken from the request. Once it finds a non
 * null token it passes that on the chain.
 *
 * If you wish to add a container specific type of auth system simply register an
 * additional handler.
 */
public class AuthenticationServletFilter implements Filter {
  public static final String AUTH_TYPE_OAUTH = "OAuth";

  private List<AuthenticationHandler> handlers;

  /**
   * Initializes the filter. We retrieve the Guice injector and ask for all
   * the injected methods to be called, setting a variety of helper objects
   * and configuration state.
   */
  public void init(FilterConfig filterConfig) throws ServletException {
    ServletContext context = filterConfig.getServletContext();
    Injector injector = (Injector)
        context.getAttribute(GuiceServletContextListener.INJECTOR_ATTRIBUTE);
    if (injector == null) {
      throw new UnavailableException(
          "Guice Injector not found! Make sure you registered "
          + GuiceServletContextListener.class.getName() + " as a listener");
    }
    injector.injectMembers(this);
  }

  @Inject
  public void setAuthenticationHandlers(AuthenticationHandlerProvider handlerProvider) {
    this.handlers = handlerProvider.get();
  }

  public void destroy() { }

  public void doFilter(ServletRequest request, ServletResponse response,
      FilterChain chain) throws IOException, ServletException {

    if (!(request instanceof HttpServletRequest)) {
      throw new ServletException("Auth filter can only handle HTTP");
    }

    HttpServletRequest req = (HttpServletRequest) request;

    for (AuthenticationHandler handler : handlers) {
      SecurityToken token = handler.getSecurityTokenFromRequest(req);
      if (token != null) {
        chain.doFilter(new SecurityTokenRequest(req, token, handler.getName()), response);
        return;
      }
    }

    // We did not find a security token so we will just pass null
    chain.doFilter(new SecurityTokenRequest(req, null, null), response);
  }

  public static class SecurityTokenRequest extends HttpServletRequestWrapper {
    private final SecurityToken token;
    private final String authType;

    public SecurityTokenRequest(HttpServletRequest request, SecurityToken token, String authType) {
      super(request);
      this.token = token;
      this.authType = authType;
    }

    public SecurityToken getToken() {
      return token;
    }

    @Override
    public String getAuthType() {
      return authType;
    }
  }
}
