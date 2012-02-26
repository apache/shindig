/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.shindig.gadgets.oauth2;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

/**
 *
 * Interface representing an OAuth2Message parser that is injected into the
 * {@link OAuth2Request} layer.
 *
 * It also contains the OAuth 2.0 constants.
 *
 * With the simplicity of the OAuth 2.0 client it is unlikely that another
 * version of this class will need to be injected, but it can be with
 * <code>com.google.inject.Provider<OAuth2Message></code>
 *
 */
public interface OAuth2Message {
  public final static String ACCESS_DENIED = "access_denied";
  public final static String ACCESS_TOKEN = "access_token";
  public final static String AUTHORIZATION = "code";
  public final static String AUTHORIZATION_CODE = "authorization_code";
  public final static String AUTHORIZATION_HEADER = "Authorization";
  public final static String BASIC_AUTH_TYPE = "Basic";
  public final static String BEARER_TOKEN_TYPE = "Bearer";
  public final static String BODYHASH = "bodyhash";
  public final static String CLIENT_CREDENTIALS = "client_credentials";
  public final static String CLIENT_ID = "client_id";
  public final static String CLIENT_SECRET = "client_secret";
  public final static String CONFIDENTIAL_CLIENT_TYPE = "confidential";
  public final static String ERROR = "error";
  public final static String ERROR_DESCRIPTION = "error_description";
  public final static String ERROR_URI = "error_uri";
  public final static String EXPIRES_IN = "expires_in";
  public final static String GRANT_TYPE = "grant_type";
  public final static String HMAC_SHA_1 = "hmac-sha-1";
  public final static String HMAC_SHA_256 = "hmac-sha-256";
  public final static String ID = "id";
  public final static String INVALID_CLIENT = "invalid_client";
  public final static String INVALID_GRANT = "invalid_grant";
  public final static String INVALID_REQUEST = "invalid_request";
  public final static String INVALID_SCOPE = "invalid_scope";
  public final static String MAC = "mac";
  public final static String MAC_ALGORITHM = "algorithm";
  public final static String MAC_EXT = "ext";
  public final static String MAC_HEADER = "MAC";
  public final static String MAC_SECRET = "secret";
  public final static String MAC_TOKEN_TYPE = "mac";
  public final static String NO_GRANT_TYPE = "NONE";
  public final static String NONCE = "nonce";
  public final static String PUBLIC_CLIENT_TYPE = "public";
  public final static String REDIRECT_URI = "redirect_uri";
  public final static String REFRESH_TOKEN = "refresh_token";
  public final static String RESPONSE_TYPE = "response_type";
  public final static String SCOPE = "scope";
  public final static String SERVER_ERROR = "server_error";
  public final static String STANDARD_AUTH_TYPE = "STANDARD";
  public final static String STATE = "state";
  public final static String TEMPORARILY_UNAVAILABLE = "temporarily_unavailable";
  public final static String TOKEN_RESPONSE = "token";
  public final static String TOKEN_TYPE = "token_type";
  public final static String UNAUTHORIZED_CLIENT = "authorized_client";
  public final static String UNSUPPORTED_GRANT_TYPE = "unsupported_grant_type";
  public final static String UNSUPPORTED_RESPONSE_TYPE = "unsupported_response_type";

  /**
   * After a message is parsed it may contain an access token.
   *
   * @return the access_token in the message
   */
  public String getAccessToken();

  /**
   * If this is an Authorization Code flow this method will return the
   * authorization_code from the message.
   *
   * @return authorization_code in the message
   */
  public String getAuthorization();

  /**
   * <code>null</code> error indicates the message parsed cleanly and the
   * service provider did not return an error.
   *
   * @return the error from the service provider
   */
  public OAuth2Error getError();

  /**
   *
   * @return the optional error_description from the service provider
   */
  public String getErrorDescription();

  /**
   *
   * @return the optional error_uri from the service provider
   */
  public String getErrorUri();

  /**
   *
   * @return "expires_in" parameter in the message
   */
  public String getExpiresIn();

  /**
   * The MAC Algorithm
   * http://tools.ietf.org/html/draft-hammer-oauth-v2-mac-token-05#section-5
   *
   * @return
   */
  public String getMacAlgorithm();

  /**
   * The MAC Secret
   * http://tools.ietf.org/html/draft-hammer-oauth-v2-mac-token-05#section-5
   *
   * @return
   */
  public String getMacSecret();

  /**
   *
   * @return a general {@link Map} of all parameters in the message
   */
  public Map<String, String> getParameters();

  /**
   *
   * @return the "refresh_token" in the message
   */
  public String getRefreshToken();

  /**
   *
   * @return the optional state string in the message
   */
  public String getState();

  /**
   *
   * @return the "token_type" type in the message
   */
  public String getTokenType();

  /**
   * Additional properties that went unparsed (i.e. aren't part of the core
   * OAuth2, Bearer Token or MAC Token specs.
   *
   * @return
   */
  public Map<String, String> getUnparsedProperties();

  /**
   * Populates an OAuth2Message from a query fragment. Not very useful in
   * shindig.
   *
   * @param fragment
   */
  public void parseFragment(String fragment);

  /**
   * Populates an OAuth2Message from a JSON response body.
   *
   * @param jsonString
   *          returned from token endpoint request
   */
  public void parseJSON(String jsonString);

  /**
   * Populates an OAuth2Message from a URL query string.
   *
   * @param queryString
   *          from redirect_uri called by servcie provider
   */
  public void parseQuery(String queryString);

  /**
   * Populates an OAuth2Message from the entire {@link HttpServletRequest}
   *
   *
   * @param request
   *          to parse
   */
  public void parseRequest(HttpServletRequest request);

  /**
   *
   * @param error
   */
  public void setError(OAuth2Error error);

  /**
   *
   * @param errorDescription
   */
  public void setErrorDescription(String errorDescription);

  /**
   *
   * @param errorUri
   */
  public void setErrorUri(String errorUri);
}
