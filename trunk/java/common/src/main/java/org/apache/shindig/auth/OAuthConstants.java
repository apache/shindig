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
 * OAuth constants not found in the upstream OAuth library
 */
public final class OAuthConstants {
  private OAuthConstants() {}
  public static final String OAUTH_SESSION_HANDLE = "oauth_session_handle";
  public static final String OAUTH_EXPIRES_IN = "oauth_expires_in";
  public static final String OAUTH_BODY_HASH = "oauth_body_hash";

  public static final String PROBLEM_ACCESS_TOKEN_EXPIRED = "access_token_expired";
  public static final String PROBLEM_PARAMETER_MISSING = "parameter_missing";
  public static final String PROBLEM_TOKEN_INVALID = "token_invalid";
  public static final String PROBLEM_BAD_VERIFIER = "bad_verifier";
}
