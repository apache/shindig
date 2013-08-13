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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.shindig.social.core.oauth2.OAuth2Types.ErrorType;
import org.apache.shindig.social.core.oauth2.OAuth2Types.TokenFormat;

/**
 * Handles requests to the OAuth 2.0 authorization end-point.
 */
public class OAuth2AuthorizationHandler {

  private OAuth2Service service;

  public OAuth2AuthorizationHandler(OAuth2Service service) {
    this.service = service;
  }

  /**
   * Handles an OAuth 2.0 authorization request.
   *
   * @param request is the original request
   * @param response is the response of the request
   * @return OAuth2NormalizedResponse represents the OAuth 2.0 response
   *
   * @throws ServletException
   * @throws IOException
   */
  public OAuth2NormalizedResponse handle(HttpServletRequest request,
      HttpServletResponse response) throws ServletException, IOException {
    try {
      // normalize the request
      OAuth2NormalizedRequest normalizedReq = new OAuth2NormalizedRequest(
          request);

      // process request according to flow
      OAuth2NormalizedResponse normalizedResp = new OAuth2NormalizedResponse();
      if (normalizedReq.getResponseType() != null) {
        switch (normalizedReq.getEnumeratedResponseType()) {
        case CODE:
          // authorization code flow
          service.validateRequestForAuthCode(normalizedReq);
          OAuth2Code authCode = service.grantAuthorizationCode(normalizedReq);

          // send response
          normalizedResp.setCode(authCode.getValue());
          if (normalizedReq.getState() != null)
            normalizedResp.setState(normalizedReq.getState());
          normalizedResp.setHeader(
              "Location",
              OAuth2Utils.buildUrl(authCode.getRedirectURI(),
                  normalizedResp.getResponseParameters(), null));
          normalizedResp.setStatus(HttpServletResponse.SC_FOUND);
          normalizedResp.setBodyReturned(false);
          return normalizedResp;
        case TOKEN:
          // implicit flow
          service.validateRequestForAccessToken(normalizedReq);
          OAuth2Code accessToken = service.grantAccessToken(normalizedReq);

          // send response
          normalizedResp.setAccessToken(accessToken.getValue());
          normalizedResp.setTokenType(TokenFormat.BEARER.toString());
          normalizedResp.setExpiresIn((accessToken.getExpiration() - System
              .currentTimeMillis()) + "");
          if (normalizedReq.getState() != null)
            normalizedResp.setState(normalizedReq.getState());
          normalizedResp.setHeader("Location", OAuth2Utils.buildUrl(
              accessToken.getRedirectURI(), null,
              normalizedResp.getResponseParameters()));
          normalizedResp.setStatus(HttpServletResponse.SC_FOUND);
          normalizedResp.setBodyReturned(false);
          return normalizedResp;
        default:
          OAuth2NormalizedResponse resp = new OAuth2NormalizedResponse();
          resp.setError(ErrorType.UNSUPPORTED_RESPONSE_TYPE.toString());
          resp.setErrorDescription("Unsupported response type");
          resp.setStatus(HttpServletResponse.SC_FOUND);
          resp.setBodyReturned(false);
          throw new OAuth2Exception(resp);
        }
      } else {
        OAuth2NormalizedResponse resp = new OAuth2NormalizedResponse();
        resp.setError(ErrorType.UNSUPPORTED_RESPONSE_TYPE.toString());
        resp.setErrorDescription("Unsupported response type");
        resp.setStatus(HttpServletResponse.SC_FOUND);
        resp.setBodyReturned(false);
        throw new OAuth2Exception(resp);
      }
    } catch (OAuth2Exception oae) {
      return oae.getNormalizedResponse();
    }
  }
}
