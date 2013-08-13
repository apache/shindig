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

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.oauth2.OAuth2Accessor;
import org.apache.shindig.gadgets.oauth2.OAuth2Error;
import org.apache.shindig.gadgets.oauth2.OAuth2Message;
import org.apache.shindig.gadgets.oauth2.OAuth2Token;
import org.apache.shindig.gadgets.oauth2.OAuth2Utils;

import java.util.Map;

/**
 *
 * See {@link ResourceRequestHandler}
 *
 * Handles the mac token type
 */
public class BearerTokenHandler implements ResourceRequestHandler {
  public static final String TOKEN_TYPE = OAuth2Message.BEARER_TOKEN_TYPE;
  private static final OAuth2Error ERROR = OAuth2Error.BEARER_TOKEN_PROBLEM;

  public OAuth2HandlerError addOAuth2Params(final OAuth2Accessor accessor, final HttpRequest request) {
    try {
      if (accessor == null || !accessor.isValid() || accessor.isErrorResponse()) {
        return BearerTokenHandler.getError("accessor is invalid " + accessor);
      }

      if (request == null) {
        return BearerTokenHandler.getError("request is null");
      }

      final Uri unAuthorizedRequestUri = request.getUri();

      if (unAuthorizedRequestUri == null) {
        return BearerTokenHandler.getError("unAuthorizedRequestUri is null");
      }

      final OAuth2Token accessToken = accessor.getAccessToken();

      if (accessToken == null || accessToken.getTokenType().length() == 0) {
        return BearerTokenHandler.getError("accessToken is invalid " + accessToken);
      }

      if (!BearerTokenHandler.TOKEN_TYPE.equalsIgnoreCase(accessToken.getTokenType())) {
        return BearerTokenHandler.getError("token type mismatch expected "
                + BearerTokenHandler.TOKEN_TYPE + " but got " + accessToken.getTokenType());
      }

      if (accessor.isUrlParameter()) {
        final Map<String, String> queryParams = Maps.newHashMap();
        final byte[] secretBytes = accessToken.getSecret();
        final String secret = new String(secretBytes, "UTF-8");
        queryParams.put(OAuth2Message.ACCESS_TOKEN, secret);
        final String authorizedUriString = OAuth2Utils.buildUrl(unAuthorizedRequestUri.toString(),
                queryParams, null);

        request.setUri(Uri.parse(authorizedUriString));
      }

      if (accessor.isAuthorizationHeader()) {
        request.setHeader("Authorization", BearerTokenHandler.TOKEN_TYPE + ' '
                + new String(accessToken.getSecret(), "UTF-8"));
      }

      return null;
    } catch (final Exception e) {
      return BearerTokenHandler.getError("Exception occurred " + e.getMessage(), e);
    }
  }

  public String getTokenType() {
    return BearerTokenHandler.TOKEN_TYPE;
  }

  private static OAuth2HandlerError getError(final String contextMessage) {
    return BearerTokenHandler.getError(contextMessage, null);
  }

  private static OAuth2HandlerError getError(final String contextMessage, final Exception e) {
    return new OAuth2HandlerError(BearerTokenHandler.ERROR, contextMessage, e, "", "");
  }
}
