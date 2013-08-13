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

import java.util.Map;

import com.google.common.collect.Maps;

/**
 * Wraps OAuth 2.0 response elements including headers and body parameters.
 *
 * TODO (Eric): document this class, including bodyReturned
 */
public class OAuth2NormalizedResponse {

  private Map<String, String> headers;
  private Map<String, String> respParams;
  private int status;
  private boolean bodyReturned;

  private static final String ERROR = "error";
  private static final String ERROR_DESCRIPTION = "error_description";
  private static final String ERROR_URI = "error_uri";
  private static final String STATE = "state";
  private static final String CODE = "code";
  private static final String ACCESS_TOKEN = "access_token";
  private static final String TOKEN_TYPE = "token_type";
  private static final String EXPIRES_IN = "expires_in";
  private static final String REFRESH_TOKEN = "refresh_token";
  private static final String SCOPE = "scope";

  public OAuth2NormalizedResponse() {
    this.headers = Maps.newHashMap();
    this.respParams = Maps.newHashMap();
    this.status = -1;
    this.bodyReturned = false;
  }

  public void setStatus(int status) {
    this.status = status;
  }

  public int getStatus() {
    return status;
  }

  public void setBodyReturned(boolean bodyReturned) {
    this.bodyReturned = bodyReturned;
  }

  public boolean isBodyReturned() {
    return bodyReturned;
  }

  // ------------------------------- HEADER FIELDS ----------------------------
  public Map<String, String> getHeaders() {
    return headers;
  }

  public void setHeaders(Map<String, String> headers) {
    this.headers = headers;
  }

  public void setHeader(String key, String value) {
    headers.put(key, value);
  }

  // ------------------------------ RESPONSE FIELDS ---------------------------
  public Map<String, String> getResponseParameters() {
    return respParams;
  }

  public void setResponseParameters(Map<String, String> responseParams) {
    this.respParams = responseParams;
  }

  public void setError(String error) {
    respParams.put(ERROR, error);
  }

  public String getError() {
    return respParams.get(ERROR);
  }

  public void setErrorDescription(String errorDescription) {
    respParams.put(ERROR_DESCRIPTION, errorDescription);
  }

  public String getErrorDescription() {
    return respParams.get(ERROR_DESCRIPTION);
  }

  public void setErrorUri(String errorUri) {
    respParams.put(ERROR_URI, errorUri);
  }

  public String getErrorUri() {
    return respParams.get(ERROR_URI);
  }

  public void setState(String state) {
    respParams.put(STATE, state);
  }

  public String getState() {
    return respParams.get(STATE);
  }

  public void setCode(String code) {
    respParams.put(CODE, code);
  }

  public String getCode() {
    return respParams.get(CODE);
  }

  public void setAccessToken(String accessToken) {
    respParams.put(ACCESS_TOKEN, accessToken);
  }

  public String getAccessToken() {
    return respParams.get(ACCESS_TOKEN);
  }

  public void setTokenType(String tokenType) {
    respParams.put(TOKEN_TYPE, tokenType);
  }

  public String getTokenType() {
    return respParams.get(TOKEN_TYPE);
  }

  public void setExpiresIn(String expiresIn) {
    respParams.put(EXPIRES_IN, expiresIn);
  }

  public String getExpiresIn() {
    return respParams.get(EXPIRES_IN);
  }

  public void setRefreshToken(String refreshToken) {
    respParams.put(REFRESH_TOKEN, refreshToken);
  }

  public String getRefreshToken() {
    return respParams.get(REFRESH_TOKEN);
  }

  public void setScope(String scope) {
    respParams.put(SCOPE, scope);
  }

  public String getScope() {
    return respParams.get(SCOPE);
  }
}
