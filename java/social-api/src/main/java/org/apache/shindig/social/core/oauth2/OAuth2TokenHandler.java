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
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.shindig.social.core.oauth2.OAuth2Types.TokenFormat;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;

/**
 * Handles operations to the OAuth 2.0 token end point.
 *
 * TODO (Eric): generate refreshToken & associate with accessToken
 */
public class OAuth2TokenHandler {

  private OAuth2Service service;

  /**
   * Constructs the token handler with the OAuth2Service.
   *
   * @param service is the service that will support this handler
   */
  public OAuth2TokenHandler(OAuth2Service service) {
    this.service = service;
  }

  /**
   * Handles an OAuth 2.0 request to the token endpoint.
   *
   * @param request is the servlet request object
   * @param response is the servlet response object
   * @return OAuth2NormalizedResponse encapsulates the request's response
   *
   * @throws ServletException
   * @throws IOException
   */
  public OAuth2NormalizedResponse handle(HttpServletRequest request,
      HttpServletResponse response) throws ServletException, IOException {
    try {
      // normalize the request
      OAuth2NormalizedRequest normalizedReq = new OAuth2NormalizedRequest(request);

      // grant access token
      service.authenticateClient(normalizedReq);
      service.validateRequestForAccessToken(normalizedReq);
      OAuth2Code accessToken = service.grantAccessToken(normalizedReq);

      // send response
      OAuth2NormalizedResponse normalizedResp = new OAuth2NormalizedResponse();
      normalizedResp.setAccessToken(accessToken.getValue());
      normalizedResp.setTokenType(TokenFormat.BEARER.toString());
      normalizedResp.setExpiresIn((accessToken.getExpiration() - System.currentTimeMillis() + ""));
      normalizedResp.setScope(listToString(accessToken.getScope()));
      normalizedResp.setStatus(HttpServletResponse.SC_OK);
      normalizedResp.setBodyReturned(true);
      if (normalizedReq.getState() != null) normalizedResp.setState(normalizedReq.getState());
      return normalizedResp;
    } catch (OAuth2Exception oae) {
      return oae.getNormalizedResponse();
    }
  }

  /**
   * Private utility to comma-delimit a list of Strings
   */
  @VisibleForTesting
  protected static String listToString(List<String> list) {
    if (list == null)
      return "";
    return Joiner.on(' ').join(list);
  }
}
