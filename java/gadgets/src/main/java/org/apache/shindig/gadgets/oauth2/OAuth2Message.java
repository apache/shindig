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
package org.apache.shindig.gadgets.oauth2;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

/**
 *
 * Interface representing an OAuth2Message parser that is injected into the {@link OAuth2Request}
 * layer.
 *
 * It also contains the OAuth 2.0 constants.
 *
 * With the simplicity of the OAuth 2.0 client it is unlikely that another version of this class
 * will need to be injected, but it can be with
 * <code>com.google.inject.Provider<OAuth2Message></code>
 *
 */
public interface OAuth2Message {
  String ACCESS_DENIED = "access_denied";
  String ACCESS_TOKEN = "access_token";
  String AUTHORIZATION = "code";
  String AUTHORIZATION_CODE = "authorization_code";
  String AUTHORIZATION_HEADER = "Authorization";
  String BASIC_AUTH_TYPE = "Basic";
  String BEARER_TOKEN_TYPE = "Bearer";
  String BODYHASH = "bodyhash";
  String CLIENT_CREDENTIALS = "client_credentials";
  String CLIENT_ID = "client_id";
  String CLIENT_SECRET = "client_secret";
  String CONFIDENTIAL_CLIENT_TYPE = "confidential";
  String ERROR = "error";
  String ERROR_DESCRIPTION = "error_description";
  String ERROR_URI = "error_uri";
  String EXPIRES_IN = "expires_in";
  String GRANT_TYPE = "grant_type";
  String HMAC_SHA_1 = "hmac-sha-1";
  String HMAC_SHA_256 = "hmac-sha-256";
  String ID = "id";
  String INVALID_CLIENT = "invalid_client";
  String INVALID_GRANT = "invalid_grant";
  String INVALID_REQUEST = "invalid_request";
  String INVALID_SCOPE = "invalid_scope";
  String MAC = "mac";
  String MAC_ALGORITHM = "algorithm";
  String MAC_EXT = "ext";
  String MAC_HEADER = "MAC";
  String MAC_SECRET = "secret";
  String MAC_TOKEN_TYPE = "mac";
  String NO_GRANT_TYPE = "NONE";
  String NONCE = "nonce";
  String PUBLIC_CLIENT_TYPE = "public";
  String REDIRECT_URI = "redirect_uri";
  String REFRESH_TOKEN = "refresh_token";
  String RESPONSE_TYPE = "response_type";
  String SCOPE = "scope";
  String SERVER_ERROR = "server_error";
  String SHARED_TOKEN = "sharedToken";
  String STANDARD_AUTH_TYPE = "STANDARD";
  String STATE = "state";
  String TEMPORARILY_UNAVAILABLE = "temporarily_unavailable";
  String TOKEN_RESPONSE = "token";
  String TOKEN_TYPE = "token_type";
  String UNAUTHORIZED_CLIENT = "authorized_client";
  String UNSUPPORTED_GRANT_TYPE = "unsupported_grant_type";
  String UNSUPPORTED_RESPONSE_TYPE = "unsupported_response_type";

  /**
   * After a message is parsed it may contain an access token.
   *
   * @return the access_token in the message
   */
  String getAccessToken();

  /**
   * If this is an Authorization Code flow this method will return the authorization_code from the
   * message.
   *
   * @return authorization_code in the message
   */
  String getAuthorization();

  /**
   * <code>null</code> error indicates the message parsed cleanly and the service provider did not
   * return an error.
   *
   * @return the error from the service provider
   */
  OAuth2Error getError();

  /**
   *
   * @return the optional error_description from the service provider
   */
  String getErrorDescription();

  /**
   *
   * @return the optional error_uri from the service provider
   */
  String getErrorUri();

  /**
   *
   * @return "expires_in" parameter in the message
   */
  String getExpiresIn();

  /**
   * The MAC Algorithm http://tools.ietf.org/html/draft-hammer-oauth-v2-mac-token-05#section-5
   *
   * @return
   */
  String getMacAlgorithm();

  /**
   * The MAC Secret http://tools.ietf.org/html/draft-hammer-oauth-v2-mac-token-05#section-5
   *
   * @return
   */
  String getMacSecret();

  /**
   *
   * @return a general {@link Map} of all parameters in the message
   */
  Map<String, String> getParameters();

  /**
   *
   * @return the "refresh_token" in the message
   */
  String getRefreshToken();

  /**
   *
   * @return the optional state string in the message
   */
  String getState();

  /**
   *
   * @return the "token_type" type in the message
   */
  String getTokenType();

  /**
   * Additional properties that went unparsed (i.e. aren't part of the core OAuth2, Bearer Token or
   * MAC Token specs.
   *
   * @return
   */
  Map<String, String> getUnparsedProperties();

  /**
   * Populates an OAuth2Message from a query fragment. Not very useful in shindig.
   *
   * @param fragment
   */
  void parseFragment(String fragment);

  /**
   * Populates an OAuth2Message from a JSON response body.
   *
   * @param jsonString
   *          returned from token endpoint request
   */
  void parseJSON(String jsonString);

  /**
   * Populates an OAuth2Message from a URL query string.
   *
   * @param queryString
   *          from redirect_uri called by servcie provider
   */
  void parseQuery(String queryString);

  /**
   * Populates an OAuth2Message from the entire {@link HttpServletRequest}
   *
   *
   * @param request
   *          to parse
   */
  void parseRequest(HttpServletRequest request);

  /**
   *
   * @param error
   */
  void setError(OAuth2Error error);

  /**
   *
   * @param errorDescription
   */
  void setErrorDescription(String errorDescription);

  /**
   *
   * @param errorUri
   */
  void setErrorUri(String errorUri);
}
