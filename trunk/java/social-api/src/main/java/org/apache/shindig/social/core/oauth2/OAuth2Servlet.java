/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.shindig.social.core.oauth2;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.shindig.common.logging.i18n.MessageKeys;
import org.apache.shindig.common.servlet.HttpUtil;
import org.apache.shindig.common.servlet.InjectedServlet;
import org.json.JSONObject;

import com.google.inject.Inject;

/**
 * Main servlet to catch OAuth 2.0 requests.
 */
public class OAuth2Servlet extends InjectedServlet {

  private static final long serialVersionUID = -4257719224664564922L;
  private static OAuth2AuthorizationHandler authorizationHandler;
  private static OAuth2TokenHandler tokenHandler;

  //class name for logging purpose
  private static final String classname = OAuth2Servlet.class.getName();
  private static final Logger LOG = Logger.getLogger(classname,MessageKeys.MESSAGES);

  @Inject
  public void setOAuth2Service(OAuth2Service oauthService) {
    authorizationHandler = new OAuth2AuthorizationHandler(oauthService);
    tokenHandler = new OAuth2TokenHandler(oauthService);
  }

  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    HttpUtil.setNoCache(response);
    String path = request.getPathInfo();
    if (path.endsWith("authorize")) {
      sendOAuth2Response(response, authorizationHandler.handle(request, response));
    } else if (path.endsWith("token")) {
      sendOAuth2Response(response, tokenHandler.handle(request, response));
    } else {
      response.sendError(HttpServletResponse.SC_NOT_FOUND, "Unknown URL");
    }
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    doGet(request, response);
  }

  /**
   * Sends an OAuth 2.0 response based on an OAuth2NormalizedResponse object.
   *
   * @param servletResp is the servlet's response object
   * @param normalizedResp maintains the headers and body fields to respond with
   */
  private void sendOAuth2Response(HttpServletResponse servletResp,
      OAuth2NormalizedResponse normalizedResp) {
    // set status
    servletResp.setStatus(normalizedResp.getStatus());

    // set body parameters
    Map<String, String> respParams = normalizedResp.getResponseParameters();
    if (normalizedResp.isBodyReturned() && respParams != null) {
      PrintWriter out = null;
      try {
        servletResp.setHeader("Content-Type", "application/json");
        out = servletResp.getWriter();
        out.println(new JSONObject(respParams).toString());
        out.flush();
      } catch (IOException e) {
        LOG.logp(Level.WARNING, classname, "getBodyAsString", MessageKeys.INVALID_OAUTH, e);
        throw new RuntimeException(e);
      } finally {
        IOUtils.closeQuietly(out);
      }
    }

    // set headers
    Map<String, String> headers = normalizedResp.getHeaders();
    if (headers != null) {
      for (String key : headers.keySet()) {
        servletResp.setHeader(key, headers.get(key));
      }
    }
  }
}
