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
import com.google.inject.Inject;

import org.apache.shindig.auth.AnonymousSecurityToken;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.oauth2.OAuth2Accessor;
import org.apache.shindig.gadgets.oauth2.OAuth2Error;
import org.apache.shindig.gadgets.oauth2.OAuth2Message;
import org.apache.shindig.gadgets.oauth2.OAuth2RequestException;
import org.apache.shindig.gadgets.oauth2.OAuth2Utils;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

/**
 *
 * See {@link GrantRequestHandler}
 *
 * Handles the "client_credentials" flow
 */
public class ClientCredentialsGrantTypeHandler implements GrantRequestHandler {
  private static final OAuth2Error ERROR = OAuth2Error.CLIENT_CREDENTIALS_PROBLEM;

  private final List<ClientAuthenticationHandler> clientAuthenticationHandlers;

  @Inject
  public ClientCredentialsGrantTypeHandler(
          final List<ClientAuthenticationHandler> clientAuthenticationHandlers) {
    this.clientAuthenticationHandlers = clientAuthenticationHandlers;
  }

  private String getAuthorizationBody(final OAuth2Accessor accessor) throws OAuth2RequestException {
    String ret = "";

    final Map<String, String> queryParams = Maps.newHashMap();
    queryParams.put(OAuth2Message.GRANT_TYPE, this.getGrantType());

    final String clientId = accessor.getClientId();
    final byte[] secretBytes = accessor.getClientSecret();
    String secret;
    try {
      secret = new String(secretBytes, "UTF-8");
    } catch (final UnsupportedEncodingException e) {
      throw new OAuth2RequestException(OAuth2Error.CLIENT_CREDENTIALS_PROBLEM,
              "error getting authorization body", e);
    }
    queryParams.put(OAuth2Message.CLIENT_ID, clientId);
    queryParams.put(OAuth2Message.CLIENT_SECRET, secret);

    ret = OAuth2Utils.buildUrl(ret, queryParams, null);

    final char firstChar = ret.charAt(0);
    if (firstChar == '?' || firstChar == '&') {
      ret = ret.substring(1);
    }

    return ret;
  }

  public HttpRequest getAuthorizationRequest(final OAuth2Accessor accessor,
          final String completeAuthorizationUrl) throws OAuth2RequestException {

    if (completeAuthorizationUrl == null || completeAuthorizationUrl.length() == 0) {
      throw new OAuth2RequestException(ClientCredentialsGrantTypeHandler.ERROR,
              "completeAuthorizationUrl is null", null);
    }

    if (accessor == null) {
      throw new OAuth2RequestException(ClientCredentialsGrantTypeHandler.ERROR, "accessor is null",
              null);
    }

    if (!accessor.isValid() || accessor.isErrorResponse() || accessor.isRedirecting()) {
      throw new OAuth2RequestException(ClientCredentialsGrantTypeHandler.ERROR,
              "accessor is invalid", null);
    }

    if (!accessor.getGrantType().equalsIgnoreCase(OAuth2Message.CLIENT_CREDENTIALS)) {
      throw new OAuth2RequestException(ClientCredentialsGrantTypeHandler.ERROR,
              "grant type is not client_credentials", null);
    }

    final HttpRequest request = new HttpRequest(Uri.parse(completeAuthorizationUrl));
    request.setMethod("GET");
    request.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
    request.setSecurityToken(new AnonymousSecurityToken("", 0L, accessor.getGadgetUri()));

    for (final ClientAuthenticationHandler clientAuthenticationHandler : this.clientAuthenticationHandlers) {
      if (clientAuthenticationHandler.geClientAuthenticationType().equalsIgnoreCase(
              accessor.getClientAuthenticationType())) {
        final OAuth2HandlerError error = clientAuthenticationHandler.addOAuth2Authentication(
                request, accessor);
        if (error != null) {
          throw new OAuth2RequestException(error.getError(), error.getContextMessage(),
                  error.getCause(), error.getUri(), error.getDescription());
        }
      }
    }

    try {
      request.setPostBody(this.getAuthorizationBody(accessor).getBytes("UTF-8"));
    } catch (final UnsupportedEncodingException e) {
      throw new OAuth2RequestException(OAuth2Error.CLIENT_CREDENTIALS_PROBLEM,
              "ClientCredentialsGrantTypeHandler - exception setting post body", e);
    }

    return request;
  }

  public String getCompleteUrl(final OAuth2Accessor accessor) throws OAuth2RequestException {

    if (accessor == null) {
      throw new OAuth2RequestException(ClientCredentialsGrantTypeHandler.ERROR, "accessor is null",
              null);
    }

    if (!accessor.isValid() || accessor.isErrorResponse() || accessor.isRedirecting()) {
      throw new OAuth2RequestException(ClientCredentialsGrantTypeHandler.ERROR,
              "accessor is invalid", null);
    }

    if (!accessor.getGrantType().equalsIgnoreCase(OAuth2Message.CLIENT_CREDENTIALS)) {
      throw new OAuth2RequestException(ClientCredentialsGrantTypeHandler.ERROR,
              "grant type is not client_credentials", null);
    }

    String ret;
    try {
      final Map<String, String> queryParams = Maps.newHashMapWithExpectedSize(4);
      queryParams.put(OAuth2Message.GRANT_TYPE, this.getGrantType());

      final String clientId = accessor.getClientId();
      final byte[] secretBytes = accessor.getClientSecret();
      final String secret = new String(secretBytes, "UTF-8");
      queryParams.put(OAuth2Message.CLIENT_ID, clientId);
      queryParams.put(OAuth2Message.CLIENT_SECRET, secret);

      final String scope = accessor.getScope();
      if (scope != null && scope.length() > 0) {
        queryParams.put(OAuth2Message.SCOPE, scope);
      }

      ret = OAuth2Utils.buildUrl(accessor.getTokenUrl(), queryParams, null);
    } catch (final UnsupportedEncodingException e) {
      throw new OAuth2RequestException(OAuth2Error.CLIENT_CREDENTIALS_PROBLEM,
              "problem getting complete url", e);
    }

    return ret;
  }

  public String getGrantType() {
    return OAuth2Message.CLIENT_CREDENTIALS;
  }

  public boolean isAuthorizationEndpointResponse() {
    return false;
  }

  public boolean isRedirectRequired() {
    return false;
  }

  public boolean isTokenEndpointResponse() {
    return true;
  }
}
