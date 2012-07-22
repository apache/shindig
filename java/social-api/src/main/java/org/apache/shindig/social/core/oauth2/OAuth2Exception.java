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
 * Represents an exception while dancing with OAuth 2.0.
 */
public class OAuth2Exception extends Exception {

  private static final long serialVersionUID = -5892464438773813010L;
  private OAuth2NormalizedResponse response;

  /**
   * Constructs an OAuth2Exception.
   *
   * @param response is the normalized response that should be used to
   * formulate a server response.
   */
  public OAuth2Exception(OAuth2NormalizedResponse response) {
    super(response.getErrorDescription());
    this.response = response;
  }

  /**
   * Retrieves the normalized response.
   *
   * @return OAuth2NormalizedResponse encapsulates the OAuth error
   */
  public OAuth2NormalizedResponse getNormalizedResponse() {
    return response;
  }
}
