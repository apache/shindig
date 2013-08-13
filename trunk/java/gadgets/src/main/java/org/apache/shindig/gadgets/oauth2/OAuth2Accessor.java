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

import org.apache.shindig.gadgets.oauth2.handler.ClientAuthenticationHandler;

import java.io.Serializable;
import java.util.Map;

/**
 * OAuth2 related data accessor.
 *
 * Every {@link OAuth2Request} will create an accessor and store it in the OAuth2Store while the
 * request is being issued. It will be removed when the request is done (success or failure.)
 *
 * OAuth2Accessor implementations should be {@link Serializable} to facilitate cluster storage and
 * caching across the various phases of OAuth 2.0 flows.
 */

public interface OAuth2Accessor extends Serializable {
  /**
   *
   * Enumeration of possible accessor types
   *
   */
  public enum Type {
    CONFIDENTIAL, PUBLIC, UNKNOWN
  }

  /**
   *
   * @return the access {@link OAuth2Token} or <code>null</code>
   */
  OAuth2Token getAccessToken();

  /**
   *
   * @return the authorization endpoint for this accessor.
   */
  String getAuthorizationUrl();

  /**
   * see {@link ClientAuthenticationHandler}
   *
   * @return the type of client authentication the service provider expects
   */
  String getClientAuthenticationType();

  /**
   *
   * @return the "client_id" for this accessor
   */
  String getClientId();

  /**
   *
   * @return the "client_secret" for this accessor
   */
  byte[] getClientSecret();

  /**
   *
   *
   */
  OAuth2Error getError();

  /**
   *
   */
  String getErrorContextMessage();

  /**
   *
   * @return the error exception, if this is an error, otherwise <code>null</code>
   */
  Throwable getErrorException();

  /**
   *
   * @return the error uri, if this is an error, otherwise <code>null</code>
   */
  String getErrorUri();

  /**
   *
   * @return the URI of the gadget issuing the request
   */
  String getGadgetUri();

  /**
   *
   * @return grant_type of this client, e.g. "code" or "client_credentials"
   */
  String getGrantType();

  /**
   *
   * @return redirect_uri of the client for this accessor
   */
  String getRedirectUri();

  /**
   *
   * @return the refresh {@link OAuth2Token} or <code>null</code>
   */
  OAuth2Token getRefreshToken();

  /**
   *
   * @return the additional oauth2 request params (never <code>null</code>)
   */
  Map<String, String> getAdditionalRequestParams();

  /**
   * if the gadget request or gadget spec specifies a scope it will be set here
   *
   * See {@link http://tools.ietf.org/html/draft-ietf-oauth-v2-21#section-3.3}
   *
   * @return scope of the request, or "" if none was specified
   */
  String getScope();

  /**
   *
   * @return the service name from the gadget spec, defaults to ""
   */
  String getServiceName();

  /**
   *
   * @return the state to include on authorization requests
   */
  OAuth2CallbackState getState();

  /**
   *
   * @return the token endpoint for this accessor.
   */
  String getTokenUrl();

  /**
   *
   * @return the {@link Type} of client for this accessor
   */
  Type getType();

  /**
   *
   * @return of the page viewer
   */
  String getUser();

  /**
   * invalidates the accessor once the request is done.
   *
   */
  void invalidate();

  /**
   *
   * @return <code>true</code> if the gadget's <ModulePrefs> can override accessor settings
   */
  boolean isAllowModuleOverrides();

  /**
   * Indicates the service provider wants the access token in an "Authorization:" header, per the
   * spec.
   *
   * @return
   */
  boolean isAuthorizationHeader();

  /**
   *
   * @return if an error response needs to be sent to the client
   */
  boolean isErrorResponse();

  /**
   * is this accessor in the middle of a authorize redirect?
   *
   * @return
   */
  boolean isRedirecting();

  /**
   * Indicates the service provider wants the access token in an URL Parameter. This goes against
   * the spec but Google, Facebook and Microsoft all expect it.
   *
   * @return
   */
  boolean isUrlParameter();

  boolean isValid();

  /**
   * updates the access token for the request (does not add it to {@link OAuth2Store})
   *
   * @param accessToken
   */
  void setAccessToken(OAuth2Token accessToken);

  /**
   * updates the authorization endpoint url
   *
   * @param authorizationUrl
   */
  void setAuthorizationUrl(String authorizationUrl);

  /**
   *
   * @param exception
   * @param error
   * @param contextMessage
   * @param errorUri
   */
  void setErrorResponse(Throwable exception, OAuth2Error error, String contextMessage,
          String errorUri);

  /**
   * Used to communicate that we are in a redirect authorization flow and the accessor should be
   * preserved.
   *
   * @param redirecting
   */
  void setRedirecting(boolean redirecting);

  /**
   * updates the refresh token for the request (does not add it to {@link OAuth2Store})
   *
   * @param accessToken
   */
  void setRefreshToken(OAuth2Token refreshToken);

  /**
   * set the oauth2 request parameters
   *
   * @param requestParams
   */
  void setAdditionalRequestParams(Map<String, String> requestParams);

  /**
   * updates the token endpoint url
   *
   * @param tokenUrl
   */
  void setTokenUrl(String tokenUrl);

  /**
   * sets the domains of allowed resource servers
   *
   * @param allowedDomains
   */
  void setAllowedDomains(String[] allowedDomains);

  /**
   * gets the domains of allowed resource servers
   *
   * @return allowed domains, may be empty but never <code>null</code>
   */
  String[] getAllowedDomains();
}
