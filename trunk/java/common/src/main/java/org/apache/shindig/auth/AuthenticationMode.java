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
 * Enumeration of known authentication modes
 */
public enum AuthenticationMode {

  /**
   * The request has no authentication associated with it. Used for anonymous requests
   */
  UNAUTHENTICATED,

  /**
   * Used by rendered gadgets to authenticate calls to the container
   */
  SECURITY_TOKEN_URL_PARAMETER,

  /**
   * A fully validated 3-legged OAuth call by a 3rd party on behalf of a user of the
   * receiving domain. viewerid should always be available
   */
  OAUTH,

  /**
   * A call by a validated 3rd party on its own behalf. Can emulate a call on behalf of a user
   * of the receiving domain subject to ACL checking but is not required to do so. viewerid may or
   * may not be available
   */
  OAUTH_CONSUMER_REQUEST,

  /**
   * The request is from a logged in user of the receiving domain
   */
  COOKIE
}
