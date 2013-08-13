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
package org.apache.shindig.social.core.oauth2.validators;

import org.apache.shindig.social.core.oauth2.OAuth2Exception;
import org.apache.shindig.social.core.oauth2.OAuth2NormalizedRequest;

/**
 * Validator interface for OAuth 2.0 requests.
 */
public interface OAuth2RequestValidator {

  /**
   * Validates an OAuth 2.0 request.
   *
   * @param req is the normalized OAuth 2.0 request to validate
   *
   * @throws OAuth2Exception if the request failed to validate
   */
  public void validateRequest(OAuth2NormalizedRequest req) throws OAuth2Exception;
}
