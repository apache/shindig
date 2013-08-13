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
import org.apache.shindig.social.core.oauth2.OAuth2DataService;
import org.apache.shindig.social.core.oauth2.OAuth2Exception;
import org.apache.shindig.social.core.oauth2.OAuth2NormalizedRequest;
import org.apache.shindig.social.core.oauth2.OAuth2NormalizedResponse;
import org.apache.shindig.social.core.oauth2.OAuth2Client.ClientType;
import org.apache.shindig.social.core.oauth2.OAuth2Client.Flow;
import org.apache.shindig.social.core.oauth2.OAuth2Types.ErrorType;

import com.google.inject.Inject;

public class ClientCredentialsGrantValidator implements OAuth2GrantValidator {

  private OAuth2DataService service;

  @Inject
  public ClientCredentialsGrantValidator(OAuth2DataService service) {
    this.service = service;
  }

  public void setOAuth2DataService(OAuth2DataService service) {
    this.service = service;
  }

  public String getGrantType() {
    return "client_credentials";
  }

  public void validateRequest(OAuth2NormalizedRequest req)
      throws OAuth2Exception {
    OAuth2Client cl = service.getClient(req.getClientId());
    if (cl == null || cl.getFlow() != Flow.CLIENT_CREDENTIALS) {
      throwAccessDenied("Bad client id or password");
    }
    if (cl.getType() != ClientType.CONFIDENTIAL) {
      throwAccessDenied("Client credentials flow does not support public clients");
    }
    if (!cl.getSecret().equals(req.getClientSecret())) {
      throwAccessDenied("Bad client id or password");
    }
  }

  private void throwAccessDenied(String msg) throws OAuth2Exception {
    OAuth2NormalizedResponse resp = new OAuth2NormalizedResponse();
    resp.setError(ErrorType.ACCESS_DENIED.toString());
    resp.setErrorDescription(msg);
    resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
    throw new OAuth2Exception(resp);
  }
}
