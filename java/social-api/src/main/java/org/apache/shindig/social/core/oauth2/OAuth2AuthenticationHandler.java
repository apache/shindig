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

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.apache.shindig.auth.AnonymousSecurityToken;
import org.apache.shindig.auth.AuthenticationHandler;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.logging.i18n.MessageKeys;

import com.google.inject.Inject;

/**
 * Authentication handler for OAuth 2.0.  Authenticates requests for resources
 * using one of the OAuth 2.0 flows.
 */
public class OAuth2AuthenticationHandler implements AuthenticationHandler {

  //class name for logging purpose
  private static final String classname = OAuth2AuthenticationHandler.class.getName();
  private static final Logger LOG = Logger.getLogger(classname,MessageKeys.MESSAGES);

  private OAuth2Service store;

  public String getName() {
    return "OAuth2";
  }

  @Inject
  public OAuth2AuthenticationHandler(OAuth2Service store) {
    this.store = store;
  }

  /**
   * Only denies authentication when an invalid bearer token is received.
   * Unauthenticated requests can pass through to other AuthenticationHandlers.
   */
  public SecurityToken getSecurityTokenFromRequest(HttpServletRequest request)
      throws InvalidAuthenticationException {

    OAuth2NormalizedRequest normalizedReq;
    try {
      normalizedReq = new OAuth2NormalizedRequest(request);
    } catch (OAuth2Exception oae) {   // request failed to normalize
      LOG.logp(Level.WARNING, classname, "getSecurityTokenFromRequest", MessageKeys.INVALID_OAUTH);
      return null;
    }
    try {
      if (normalizedReq.getAccessToken() != null) {
        store.validateRequestForResource(normalizedReq, null);
        return createSecurityTokenForValidatedRequest(normalizedReq);
      }
    } catch (OAuth2Exception oae) {
      // TODO (Eric): process OAuth2Exception properly
      throw new InvalidAuthenticationException("Something went wrong: ", oae);
    }
    return null;
  }

  public String getWWWAuthenticateHeader(String realm) {
    return String.format("Bearer realm=\"%s\"", realm);
  }

  /**
   * Return a security token for the request.
   *
   * The request was validated against the {@link OAuth2Service}.
   *
   * @param request
   * @return the security token for the request
   * @throws InvalidAuthenticationException if the token can not be created
   */
  protected SecurityToken createSecurityTokenForValidatedRequest(OAuth2NormalizedRequest request)
      throws InvalidAuthenticationException {
    return new AnonymousSecurityToken(); // Return your valid security token
  }
}
