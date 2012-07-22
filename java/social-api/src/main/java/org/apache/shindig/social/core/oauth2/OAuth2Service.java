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

/**
 * Services to support the OAuth 2.0 specification flows and enforcement.
 *
 * TODO (Eric): include grant methods?
 */
public interface OAuth2Service {

  /**
   * Retrieves the underlying data service.
   */
  public OAuth2DataService getDataService();

  // --------------------------- VALIDATION SERVICES --------------------------
  /**
   * Validates a client.
   */
  public void authenticateClient(OAuth2NormalizedRequest req)
      throws OAuth2Exception;

  /**
   * Validates a client's request for an authorization token.
   */
  public void validateRequestForAuthCode(OAuth2NormalizedRequest req)
      throws OAuth2Exception;

  /**
   * Validates a client's request for an access token.
   */
  public void validateRequestForAccessToken(OAuth2NormalizedRequest req)
      throws OAuth2Exception;

  /**
   * Validates a client's request to use access a resource.
   */
  public void validateRequestForResource(OAuth2NormalizedRequest req,
      Object resourceRequest) throws OAuth2Exception;

  // ------------------- GENERATION & REGISTRATION OF CODES -------------------
  /**
   * Grants an authorization code to the given client by generating and
   * registering the code.
   */
  public OAuth2Code grantAuthorizationCode(OAuth2NormalizedRequest req);

  /**
   * Grants an access token to the given client by generating and registering
   * the access token.
   */
  public OAuth2Code grantAccessToken(OAuth2NormalizedRequest req);

  /**
   * Grants a refresh token to the given client by generating and registering
   * the refresh token.
   */
  public OAuth2Code grantRefreshToken(OAuth2NormalizedRequest req);

  // ------------------------ TOKEN GENERATION SERVICES -----------------------
  /**
   * Generates an authorization code from a client OAuth 2.0 request.
   */
  public OAuth2Code generateAuthorizationCode(OAuth2NormalizedRequest req);

  /**
   * Generates an access token from a client OAuth 2.0 request.
   */
  public OAuth2Code generateAccessToken(OAuth2NormalizedRequest req);

  /**
   * Generates a refresh token from a client OAuth 2.0 request.
   */
  public OAuth2Code generateRefreshToken(OAuth2NormalizedRequest req);
}
