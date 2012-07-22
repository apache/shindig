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
 * A collection of OAuth 2.0's enumerated types.
 */
public class OAuth2Types {

  /**
   * Enumerated error types in the OAuth 2.0 specification.
   */
  public static enum ErrorType {
    INVALID_REQUEST("invalid_request"),
    INVALID_CLIENT("invalid_client"),
    INVALID_GRANT("invalid_grant"),
    UNAUTHORIZED_CLIENT("unauthorized_client"),
    UNSUPPORTED_GRANT_TYPE("unsupported_grant_type"),
    INVALID_SCOPE("invalid_scope"), ACCESS_DENIED("access_denied"),
    UNSUPPORTED_RESPONSE_TYPE("unsupported_response_type"),
    SERVER_ERROR("server_error"),
    TEMPORARILY_UNAVAILABLE("temporarily_unavailable");

    private final String name;

    private ErrorType(String name) {
      this.name = name;
    }

    public String toString() {
      return name;
    }
  }

  /**
   * Enumerated grant types in the OAuth 2.0 specification.
   */
  public static enum GrantType {
    REFRESH_TOKEN("refresh_token"),
    AUTHORIZATION_CODE("authorization_code"),
    PASSWORD("password"),
    CLIENT_CREDENTIALS("client_credentials"),
    CUSTOM("custom");

    private final String name;

    private GrantType(String name) {
      this.name = name;
    }

    public String toString() {
      return name;
    }
  }

  /**
   * Enumerated response types in the OAuth 2.0 specification.
   */
  public static enum ResponseType {
    CODE("code"), TOKEN("token");

    private final String name;

    private ResponseType(String name) {
      this.name = name;
    }

    public String toString() {
      return name;
    }
  }

  /**
   * Enumerated token types in the OAuth 2.0 specification.
   */
  public static enum CodeType {
    AUTHORIZATION_CODE("authorization_code"),
    ACCESS_TOKEN("access_token"),
    REFRESH_TOKEN("refresh_token");

    private final String name;

    private CodeType(String name) {
      this.name = name;
    }

    public String toString() {
      return name;
    }
  }

  /**
   * Enumerated token types in the OAuth 2.0 specification.
   */
  public static enum TokenFormat {
    BEARER("bearer"),
    MAC("mac");

    private final String name;

    private TokenFormat(String name) {
      this.name = name;
    }

    public String toString() {
      return name;
    }
  }
}
