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

import com.google.inject.Inject;
import com.google.inject.Provider;

import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.oauth2.OAuth2Accessor;
import org.apache.shindig.gadgets.oauth2.OAuth2Error;
import org.apache.shindig.gadgets.oauth2.OAuth2Message;
import org.apache.shindig.gadgets.oauth2.OAuth2Store;
import org.apache.shindig.gadgets.oauth2.OAuth2Token;
import org.apache.shindig.gadgets.oauth2.logger.FilteredLogger;

import org.json.JSONObject;

import java.util.Map;

/**
 *
 * See {@link TokenEndpointResponseHandler}
 *
 * Handles the "client_credentials" flow
 */
public class TokenAuthorizationResponseHandler implements TokenEndpointResponseHandler {
  private static final String LOG_CLASS = CodeAuthorizationResponseHandler.class.getName();
  private static final FilteredLogger LOG = FilteredLogger
          .getFilteredLogger(TokenAuthorizationResponseHandler.LOG_CLASS);

  private static final OAuth2Error ERROR = OAuth2Error.TOKEN_RESPONSE_PROBLEM;

  private final Provider<OAuth2Message> oauth2MessageProvider;
  private final OAuth2Store store;

  @Inject
  public TokenAuthorizationResponseHandler(final Provider<OAuth2Message> oauth2MessageProvider,
          final OAuth2Store store) {
    this.oauth2MessageProvider = oauth2MessageProvider;
    this.store = store;

    if (TokenAuthorizationResponseHandler.LOG.isLoggable()) {
      TokenAuthorizationResponseHandler.LOG.log("this.oauth2MessageProvider = {0}",
              this.oauth2MessageProvider);
      TokenAuthorizationResponseHandler.LOG.log("this.store = {0}", this.store);
    }
  }

  public OAuth2HandlerError handleResponse(final OAuth2Accessor accessor,
          final HttpResponse response) {
    final boolean isLogging = TokenAuthorizationResponseHandler.LOG.isLoggable();

    if (isLogging) {
      if (response != null) {
        TokenAuthorizationResponseHandler.LOG.entering(TokenAuthorizationResponseHandler.LOG_CLASS,
                "getAuthorizationBody", new Object[] { accessor, "non-null response" });
      } else {
        TokenAuthorizationResponseHandler.LOG.entering(TokenAuthorizationResponseHandler.LOG_CLASS,
                "getAuthorizationBody", new Object[] { accessor, null });
      }
    }

    OAuth2HandlerError ret = null;

    try {
      if (response == null) {
        ret = TokenAuthorizationResponseHandler.getError("response is null");
      }

      if (ret == null && (accessor == null || !accessor.isValid() || accessor.isErrorResponse())) {
        ret = TokenAuthorizationResponseHandler.getError("accessor is invalid " + accessor);
      }

      if (ret == null && response != null) {
        final int responseCode = response.getHttpStatusCode();
        if (responseCode != HttpResponse.SC_OK) {
          ret = TokenAuthorizationResponseHandler.getError("can't handle error response code "
                  + responseCode);
        }

        if (ret == null) {
          final long issuedAt = System.currentTimeMillis();

          final String contentType = response.getHeader("Content-Type");
          final String responseString = response.getResponseAsString();
          final OAuth2Message msg = this.oauth2MessageProvider.get();

          if (contentType.startsWith("text/plain")) {
            // Facebook does this
            msg.parseQuery('?' + responseString);
          } else if (contentType.startsWith("application/json")) {
            // Google does this
            final JSONObject responseJson = new JSONObject(responseString);
            msg.parseJSON(responseJson.toString());
          } else {
            if (isLogging) {
              TokenAuthorizationResponseHandler.LOG.log("Unhandled Content-Type {0}", contentType);
              TokenAuthorizationResponseHandler.LOG.exiting(
                      TokenAuthorizationResponseHandler.LOG_CLASS, "handleResponse", null);
            }
            ret = TokenAuthorizationResponseHandler.getError("Unhandled Content-Type "
                    + contentType);
          }

          final OAuth2Error error = msg.getError();
          if (error != null) {
            ret = getError("error parsing request", null, msg.getErrorUri(),
                    msg.getErrorDescription());
          } else if (error == null && accessor != null) {
            final String accessToken = msg.getAccessToken();
            final String refreshToken = msg.getRefreshToken();
            final String expiresIn = msg.getExpiresIn();
            final String tokenType = msg.getTokenType();
            final String providerName = accessor.getServiceName();
            final String gadgetUri = accessor.getGadgetUri();
            final String scope = accessor.getScope();
            final String user = accessor.getUser();
            final String macAlgorithm = msg.getMacAlgorithm();
            final String macSecret = msg.getMacSecret();
            final Map<String, String> unparsedProperties = msg.getUnparsedProperties();

            if (accessToken != null) {
              final OAuth2Token storedAccessToken = this.store.createToken();
              storedAccessToken.setIssuedAt(issuedAt);
              if (expiresIn != null) {
                storedAccessToken.setExpiresAt(issuedAt + Long.decode(expiresIn) * 1000);
              } else {
                storedAccessToken.setExpiresAt(0);
              }
              storedAccessToken.setGadgetUri(gadgetUri);
              storedAccessToken.setServiceName(providerName);
              storedAccessToken.setScope(scope);
              storedAccessToken.setSecret(accessToken.getBytes("UTF-8"));
              storedAccessToken.setTokenType(tokenType);
              storedAccessToken.setType(OAuth2Token.Type.ACCESS);
              storedAccessToken.setUser(user);
              if (macAlgorithm != null) {
                storedAccessToken.setMacAlgorithm(macAlgorithm);
              }
              if (macSecret != null) {
                storedAccessToken.setMacSecret(macSecret.getBytes("UTF-8"));
              }
              storedAccessToken.setProperties(unparsedProperties);
              this.store.setToken(storedAccessToken);
              accessor.setAccessToken(storedAccessToken);
            }

            if (refreshToken != null) {
              final OAuth2Token storedRefreshToken = this.store.createToken();
              storedRefreshToken.setExpiresAt(0);
              storedRefreshToken.setGadgetUri(gadgetUri);
              storedRefreshToken.setServiceName(providerName);
              storedRefreshToken.setScope(scope);
              storedRefreshToken.setSecret(refreshToken.getBytes("UTF-8"));
              storedRefreshToken.setTokenType(tokenType);
              storedRefreshToken.setType(OAuth2Token.Type.REFRESH);
              storedRefreshToken.setUser(user);
              this.store.setToken(storedRefreshToken);
              accessor.setRefreshToken(storedRefreshToken);
            }
          }
        }
      }
    } catch (final Exception e) {
      if (isLogging) {
        TokenAuthorizationResponseHandler.LOG.log(
                "exception thrown handling authorization response", e);
      }
      return TokenAuthorizationResponseHandler.getError(
              "exception thrown handling authorization response", e, "", "");
    }

    if (isLogging) {
      TokenAuthorizationResponseHandler.LOG.exiting(TokenAuthorizationResponseHandler.LOG_CLASS,
              "handleResponse", ret);
    }

    return ret;
  }

  public boolean handlesResponse(final OAuth2Accessor accessor, final HttpResponse response) {
    if (accessor == null || !accessor.isValid() || accessor.isErrorResponse()) {
      return false;
    }

    return response != null;
  }

  private static OAuth2HandlerError getError(final String contextMessage) {
    return TokenAuthorizationResponseHandler.getError(contextMessage, null, "", "");
  }

  private static OAuth2HandlerError getError(final String contextMessage, final Exception e,
          final String uri, final String description) {
    return new OAuth2HandlerError(TokenAuthorizationResponseHandler.ERROR, contextMessage, e, uri,
            description);
  }
}
