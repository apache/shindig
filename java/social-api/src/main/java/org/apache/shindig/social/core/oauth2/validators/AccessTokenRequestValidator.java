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

import org.apache.shindig.social.core.oauth2.OAuth2Client;
import org.apache.shindig.social.core.oauth2.OAuth2DataService;
import org.apache.shindig.social.core.oauth2.OAuth2Exception;
import org.apache.shindig.social.core.oauth2.OAuth2NormalizedRequest;
import org.apache.shindig.social.core.oauth2.OAuth2NormalizedResponse;
import org.apache.shindig.social.core.oauth2.OAuth2Client.Flow;
import org.apache.shindig.social.core.oauth2.OAuth2Types.ErrorType;

import javax.servlet.http.HttpServletResponse;

import java.util.ArrayList;
import java.util.List;

import com.google.inject.Inject;

public class AccessTokenRequestValidator implements OAuth2RequestValidator {

  private OAuth2DataService store = null;
  private List<OAuth2GrantValidator> grantValidators; // grant validators

  @Inject
  public AccessTokenRequestValidator(OAuth2DataService store) {
    this.grantValidators = new ArrayList<OAuth2GrantValidator>();
    grantValidators.add(new AuthCodeGrantValidator(store));
    grantValidators.add(new ClientCredentialsGrantValidator(store));
    this.store = store;
  }

  public void validateRequest(OAuth2NormalizedRequest req)
      throws OAuth2Exception {
    if (req.getGrantType() != null) {
      for (OAuth2GrantValidator validator : grantValidators) {
        if (validator.getGrantType().equals(req.getGrantType())) {
          validator.validateRequest(req);
          return; // request validated
        }
      }
      OAuth2NormalizedResponse response = new OAuth2NormalizedResponse();
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      response.setError(ErrorType.UNSUPPORTED_GRANT_TYPE.toString());
      response.setErrorDescription("Unsupported grant type");
      response.setBodyReturned(true);
      throw new OAuth2Exception(response);
    } else { // implicit flow does not include grant type
      if (req.getResponseType() == null
          || !req.getResponseType().equals("token")) {
        OAuth2NormalizedResponse resp = new OAuth2NormalizedResponse();
        resp.setError(ErrorType.UNSUPPORTED_RESPONSE_TYPE.toString());
        resp.setErrorDescription("Unsupported response type");
        resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
        throw new OAuth2Exception(resp);
      }
      OAuth2Client client = store.getClient(req.getClientId());
      if (client == null || client.getFlow() != Flow.IMPLICIT) {
        OAuth2NormalizedResponse resp = new OAuth2NormalizedResponse();
        resp.setError(ErrorType.INVALID_CLIENT.toString());
        resp.setErrorDescription(req.getClientId()
            + " is not a registered implicit client");
        resp.setBodyReturned(true);
        resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
        throw new OAuth2Exception(resp);
      }
      if (req.getRedirectURI() == null && client.getRedirectURI() == null) {
        OAuth2NormalizedResponse resp = new OAuth2NormalizedResponse();
        resp.setError(ErrorType.INVALID_REQUEST.toString());
        resp.setErrorDescription("No redirect_uri registered or received in request");
        resp.setBodyReturned(true);
        resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
        throw new OAuth2Exception(resp);
      }
      if (req.getRedirectURI() != null
          && !req.getRedirectURI().equals(client.getRedirectURI())) {
        OAuth2NormalizedResponse resp = new OAuth2NormalizedResponse();
        resp.setError(ErrorType.INVALID_REQUEST.toString());
        resp.setErrorDescription("Redirect URI does not match the one registered for this client");
        resp.setBodyReturned(true);
        resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
        throw new OAuth2Exception(resp);
      }
      return; // request validated
    }
  }
}
