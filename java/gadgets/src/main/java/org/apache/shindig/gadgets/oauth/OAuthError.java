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
package org.apache.shindig.gadgets.oauth;

/**
 * Error strings to be returned to gadgets as "oauthError" data.
 */
public enum OAuthError {
  /**
   * The request cannot be completed because the gadget's OAuth configuration
   * is incorrect. Generic message.
   */
  BAD_OAUTH_CONFIGURATION("%s"),

  /**
   * The request cannot be completed because the gadget didn't specify
   * an endpoint required for redirection-based authorization.
   */
  BAD_OAUTH_TOKEN_URL("No %s URL specified"),

  /**
   * The request cannot be completed due to missing oauth field(s)
   */
  MISSING_OAUTH_PARAMETER("No %s returned from service provider"),

  /**
   * The request did not yield a response from the server
   */
  MISSING_SERVER_RESPONSE("No response from server"),

  /**
   * The requested HTTP method is not supported
   */
  UNSUPPORTED_HTTP_METHOD("Unknown method: %s"),

  /**
   * The request cannot be completed for an unspecified reason.
   */
  UNKNOWN_PROBLEM("%s"),

  /**
   * The user is not authenticated.
   */
  UNAUTHENTICATED("Unauthenticated OAuth fetch"),

  /**
   * The user is not the owner of the page.
   */
  NOT_OWNER("Non-Secure Owner Page. Only page owners can grant OAuth approval"),

  /**
   * The URL is invalid
   */
  INVALID_URL("Invalid URL: %s"),

  /**
   * The request contains an invalid parameter.
   */
  INVALID_PARAMETER("Invalid parameter name %s, applications may not override"
      + " oauth, xoauth, or opensocial parameters"),

  /**
   * The request contains an invalid trusted parameter.
   */
  INVALID_TRUSTED_PARAMETER("Invalid trusted parameter name %s, parameter"
      + " must start with oauth, xoauth, or opensocial"),

  UNKNOWN_PARAMETER_LOCATION("Unknown parameter location: %s"),

  /**
   * The request cannot be completed because the request options were invalid.
   * Generic message.
   */
  INVALID_REQUEST("%s"),
  ;

  private final String formatString;

  OAuthError(String formatString) {
    this.formatString = formatString;
  }

  @Override
  public String toString() {
    return formatString;
  }
}
