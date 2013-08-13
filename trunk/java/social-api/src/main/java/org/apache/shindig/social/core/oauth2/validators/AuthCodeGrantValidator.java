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
package org.apache.shindig.social.core.oauth2.validators;

import javax.servlet.http.HttpServletResponse;

import org.apache.shindig.social.core.oauth2.OAuth2Client;
import org.apache.shindig.social.core.oauth2.OAuth2Code;
import org.apache.shindig.social.core.oauth2.OAuth2DataService;
import org.apache.shindig.social.core.oauth2.OAuth2Exception;
import org.apache.shindig.social.core.oauth2.OAuth2NormalizedRequest;
import org.apache.shindig.social.core.oauth2.OAuth2NormalizedResponse;
import org.apache.shindig.social.core.oauth2.OAuth2Client.Flow;
import org.apache.shindig.social.core.oauth2.OAuth2Types.ErrorType;

import com.google.inject.Inject;

public class AuthCodeGrantValidator implements OAuth2GrantValidator {

  private OAuth2DataService service;

  @Inject
  public AuthCodeGrantValidator(OAuth2DataService service) {
    this.service = service;
  }

  public String getGrantType() {
    return "authorization_code";
  }

  public void validateRequest(OAuth2NormalizedRequest servletRequest)
      throws OAuth2Exception {
    OAuth2Client client = service.getClient(servletRequest.getClientId());
    if (client == null || client.getFlow() != Flow.AUTHORIZATION_CODE) {
      OAuth2NormalizedResponse resp = new OAuth2NormalizedResponse();
      resp.setError(ErrorType.INVALID_CLIENT.toString());
      resp.setErrorDescription("Invalid client");
      resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
      throw new OAuth2Exception(resp);
    }
    OAuth2Code authCode = service.getAuthorizationCode(
        servletRequest.getClientId(), servletRequest.getAuthorizationCode());
    if (authCode == null) {
      OAuth2NormalizedResponse response = new OAuth2NormalizedResponse();
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      response.setError(ErrorType.INVALID_GRANT.toString());
      response.setErrorDescription("Bad authorization code");
      response.setBodyReturned(true);
      throw new OAuth2Exception(response);
    }
    if (servletRequest.getRedirectURI() != null
        && !servletRequest.getRedirectURI().equals(authCode.getRedirectURI())) {
      OAuth2NormalizedResponse response = new OAuth2NormalizedResponse();
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      response.setError(ErrorType.INVALID_GRANT.toString());
      response
          .setErrorDescription("The redirect URI does not match the one used in the authorization request");
      response.setBodyReturned(true);
      throw new OAuth2Exception(response);
    }

    // ensure authorization code has not already been used
    if (authCode.getRelatedAccessToken() != null) {
      service.unregisterAccessToken(client.getId(), authCode
          .getRelatedAccessToken().getValue());
      OAuth2NormalizedResponse response = new OAuth2NormalizedResponse();
      response.setStatus(HttpServletResponse.SC_FORBIDDEN);
      response.setError(ErrorType.INVALID_GRANT.toString());
      response
          .setErrorDescription("The authorization code has already been used to generate an access token");
      response.setBodyReturned(true);
      throw new OAuth2Exception(response);
    }
  }
}
