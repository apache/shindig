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
package org.apache.shindig.social.oauth;

import net.oauth.OAuth;
import net.oauth.OAuthMessage;
import net.oauth.server.OAuthServlet;

import org.apache.commons.lang.NotImplementedException;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class OAuthServletFilter implements Filter {


  public void init(FilterConfig filterConfig) {
  }

  public void destroy() {
  }

  public void doFilter(ServletRequest request, ServletResponse response,
      FilterChain chain) throws IOException, ServletException {

    if (!(request instanceof HttpServletRequest)) {
      throw new ServletException("OAuth filter can only handle HTTP");
    }

    if (!(response instanceof HttpServletResponse)) {
      throw new ServletException("OAuth filter can only handle HTTP");
    }

    HttpServletRequest req = (HttpServletRequest)request;
    HttpServletResponse res = (HttpServletResponse)response;
    OAuthContext authContext = OAuthContext.newContextForRequest(req);

    OAuthMessage requestMessage = OAuthServlet.getMessage(req, null);

    if (requestMessage.getParameter(OAuth.OAUTH_SIGNATURE) == null) {
      // doesn't seem to be an OAuth request
      chain.doFilter(request, response);
      return;
    }

    if (requestMessage.getToken() == null) {
      handleSignedFetch(requestMessage, authContext);
    } else {
      handleFullOAuth(requestMessage, authContext);
    }

    chain.doFilter(request, response);
  }

  private void handleFullOAuth(OAuthMessage requestMessage,
      OAuthContext authContext) {
    throw new NotImplementedException("full OAuth support not yet implemented");
  }

  private void handleSignedFetch(OAuthMessage requestMessage,
      OAuthContext context) {
    // TODO implement this method
  }
}
