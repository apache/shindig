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
package org.apache.shindig.auth;

/**
 * An abstract representation of a signing token.
 * Use in conjunction with @code SecurityTokenCodec.
 */
public interface SecurityToken {

  /**
   * @return the owner from the token, or null if there is none.
   */
  String getOwnerId();

  /**
   * @return the viewer from the token, or null if there is none.
   */
  String getViewerId();

  /**
   * @return the application id from the token, or null if there is none.
   */
  String getAppId();

  /**
   * @return the domain from the token, or null if there is none.
   */
  String getDomain();

  /**
   * @return The container.
   */
  String getContainer();

  /**
   * @return the URL of the application
   */
  String getAppUrl();

  /**
   * @return the module ID of the application
   */
  long getModuleId();

  /**
   * @return The time in seconds since epoc that this token expires or
   *   <code>null</code> if unknown or indeterminate.
   */
  Long getExpiresAt();

  /**
   * @return true if the token is no longer valid.
   */
  boolean isExpired();

  /**
   * @return an updated version of the token to return to the gadget, or null
   * if there is no need to update the token.
   */
  String getUpdatedToken();

  /**
   * @return the authentication mechanism used to generate this security token
   * @see AuthenticationMode
   */
  String getAuthenticationMode();

  /**
   * @return a string formatted JSON object from the container, or null if there
   * is no JSON from the container.
   */
  String getTrustedJson();

  /**
   * @return true if the token is for an anonymous viewer/owner
   */
  boolean isAnonymous();

  /**
   * @return the URL being used by the current request and null if not specified.
   *
   * The returned URL must contain at least protocol, host, and port.
   *
   * The returned URL may contain path or query parameters.
   *
   */
  String getActiveUrl();
}
