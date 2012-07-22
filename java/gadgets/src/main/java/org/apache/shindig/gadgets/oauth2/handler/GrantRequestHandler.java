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

import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.oauth2.OAuth2Accessor;
import org.apache.shindig.gadgets.oauth2.OAuth2RequestException;

/**
 * Enables injection of new Grant Type schemes into the system.
 *
 * If a {@link GrantRequestHandler#getGrantType()} matches a
 * {@link OAuth2Accessor#getGrantType()} it will be invoked to initiate the
 * grant request.
 *
 * By default "code" and "client_credentials" are supported.
 *
 * Only one GrantRequestHandler will be executed (first to match.)
 *
 */
public interface GrantRequestHandler {
  /**
   * If {@link #isRedirectRequired()} is false the system will executes this
   * request.
   *
   * @param accessor
   * @param completeAuthorizationUrl
   * @return HttpRequest
   * @throws OAuth2RequestException
   */
  public HttpRequest getAuthorizationRequest(OAuth2Accessor accessor,
      String completeAuthorizationUrl) throws OAuth2RequestException;

  /**
   * Url to send redirects to.
   *
   * @param accessor
   * @return String complete url
   * @throws OAuth2RequestException
   */
  public String getCompleteUrl(OAuth2Accessor accessor) throws OAuth2RequestException;

  /**
   *
   * @return the grant_type this handler initiates
   */
  public String getGrantType();

  /**
   *
   * @return true if the response is from the authorization endpoint, i.e. code
   */
  public boolean isAuthorizationEndpointResponse();

  /**
   *
   * @return true to redirect the client to the
   *         {@link #getCompleteUrl(OAuth2Accessor)}
   */
  public boolean isRedirectRequired();

  /**
   *
   * @return true if the response is from the token endpoint i.e.
   *         client_credentials
   */
  public boolean isTokenEndpointResponse();
}
