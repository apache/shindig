/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.social.oauth;

import javax.servlet.http.HttpServletRequest;

/**
 * A class that encapsulates the OAuth-related authentication information about
 * an HTTP request. If a servlet requires that a request was made by a specific
 * client, it could to the following:
 *
 * public void doGet(HttpServletRequest req, HttpServletResponse resp) {
 *   ...
 *   OAuthContext authContext = OAuthContext.forRequest(req);
 *   if (authContext.getAuthMethod() == OAuthContext.AuthMethod.NONE) {
 *     respondWithError();
 *   } else {
 *      String consumer = authContext.getConsumerKey();
 *      if (clientIsAllowed(consumer)) {
 *        handleRequest(req, resp);
 *      } else {
 *        respondWithError():
 *      }
 *   }
 */
public class OAuthContext {

  /*
   * The different authentication methods.
   */
  public enum AuthMethod {
    NONE,  // no authentication attempted, or authentication failed

    OAUTH, // OAuth succeeded, which means we'll have a consumer key and an
           // OAuth token

    SIGNED // SignedFetch succeeded, in which case we'll just have a consumer
           // key
  }

  static final String OAUTH_CONTEXT =
    "org.apache.shindig.social.oauth.context";

  private AuthMethod authMethod;
  private String consumerKey;
  private String oauthToken;

  /**
   * Returns the OAuth context object for this http request. If no OAuth
   * context object exists, then a newly-created context object for this
   * request is returned.
   */
  public static OAuthContext fromRequest(HttpServletRequest req) {
    OAuthContext result = (OAuthContext)req.getAttribute(OAUTH_CONTEXT);
    return (result == null)
           ? newContextForRequest(req)
           : result;
  }

  /**
   * Makes a new OAuth context object and stores it in the HttpServletRequest
   * @param req
   * @return the newly-created object.
   */
  static OAuthContext newContextForRequest(HttpServletRequest req) {
    OAuthContext context = new OAuthContext();
    req.setAttribute(OAUTH_CONTEXT, context);
    return context;
  }

  // newly-created contexts know of no authentication
  OAuthContext() {
    this.authMethod = AuthMethod.NONE;
    this.consumerKey = null;
    this.oauthToken = null;
  }

  /**
   * Returns the method of authentication used by the client.
   */
  public AuthMethod getAuthMethod() {
    return authMethod;
  }

  public void setAuthMethod(AuthMethod method) {
    authMethod = method;
  }

  /**
   * Returns the consumer key that was authenticated by the server. This value
   * should only be trusted if getAuthMethod() returns OAUTH or SIGNED.
   */
  public String getConsumerKey() {
    return consumerKey;
  }

  public void setConsumerKey(String key) {
    consumerKey = key;
  }

  /**
   * Returns the OAuth token that was authenticated by the server. This value
   * should only be trusted if getAuthMethod() return OAUTH.
   */
  public String getOAuthToken() {
    return oauthToken;
  }

  public void setOAuthToken(String token) {
    oauthToken = token;
  }
}
