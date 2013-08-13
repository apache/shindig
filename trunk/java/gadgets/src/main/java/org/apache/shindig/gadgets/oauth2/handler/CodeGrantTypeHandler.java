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
package org.apache.shindig.gadgets.oauth2.handler;

import com.google.common.collect.Maps;

import org.apache.shindig.common.crypto.BlobCrypterException;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.oauth2.OAuth2Accessor;
import org.apache.shindig.gadgets.oauth2.OAuth2CallbackState;
import org.apache.shindig.gadgets.oauth2.OAuth2Error;
import org.apache.shindig.gadgets.oauth2.OAuth2Message;
import org.apache.shindig.gadgets.oauth2.OAuth2RequestException;
import org.apache.shindig.gadgets.oauth2.OAuth2Utils;

import java.util.Map;

/**
 *
 * See {@link GrantRequestHandler}
 *
 * Handles the "authorization_code" flow
 */
public class CodeGrantTypeHandler implements GrantRequestHandler {
  private static final OAuth2Error ERROR = OAuth2Error.CODE_GRANT_PROBLEM;

  public HttpRequest getAuthorizationRequest(final OAuth2Accessor accessor,
          final String completeAuthorizationUrl) throws OAuth2RequestException {
    throw new OAuth2RequestException(CodeGrantTypeHandler.ERROR,
            "inappropriate call to CodeGrantTypeHandler.getAuthorizationRequest()", null);
  }

  public String getCompleteUrl(final OAuth2Accessor accessor) throws OAuth2RequestException {
    if (accessor == null) {
      throw new OAuth2RequestException(CodeGrantTypeHandler.ERROR, "accessor is null", null);
    }

    if (!accessor.isValid() || accessor.isErrorResponse() || accessor.isRedirecting()) {
      throw new OAuth2RequestException(CodeGrantTypeHandler.ERROR, "accessor is invalid", null);
    }

    if (!accessor.getGrantType().equalsIgnoreCase(OAuth2Message.AUTHORIZATION)) {
      throw new OAuth2RequestException(CodeGrantTypeHandler.ERROR, "grant type is not code", null);
    }

    final Map<String, String> queryParams = Maps.newHashMapWithExpectedSize(4);
    queryParams.put(OAuth2Message.RESPONSE_TYPE, this.getGrantType());
    queryParams.put(OAuth2Message.CLIENT_ID, accessor.getClientId());
    final String redirectUri = accessor.getRedirectUri();
    if (redirectUri != null && redirectUri.length() > 0) {
      queryParams.put(OAuth2Message.REDIRECT_URI, redirectUri);
    }

    final OAuth2CallbackState state = accessor.getState();
    if (state != null) {
      try {
        queryParams.put(OAuth2Message.STATE, state.getEncryptedState());
      } catch (final BlobCrypterException e) {
        throw new OAuth2RequestException(OAuth2Error.CODE_GRANT_PROBLEM, "encryption problem", e);
      }
    }

    final String scope = accessor.getScope();
    if (scope != null && scope.length() > 0) {
      queryParams.put(OAuth2Message.SCOPE, scope);
    }

    // add any additional parameters
    for (final Map.Entry<String, String> entry : accessor.getAdditionalRequestParams().entrySet()) {
      queryParams.put(entry.getKey(), entry.getValue());
    }

    return OAuth2Utils.buildUrl(accessor.getAuthorizationUrl(), queryParams, null);
  }

  public String getGrantType() {
    return OAuth2Message.AUTHORIZATION;
  }

  public static String getResponseType() {
    return OAuth2Message.AUTHORIZATION_CODE;
  }

  public boolean isAuthorizationEndpointResponse() {
    return true;
  }

  public boolean isRedirectRequired() {
    return true;
  }

  public boolean isTokenEndpointResponse() {
    return false;
  }
}
