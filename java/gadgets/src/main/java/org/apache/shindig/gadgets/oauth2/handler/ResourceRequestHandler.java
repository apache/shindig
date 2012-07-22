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
import org.apache.shindig.gadgets.oauth2.OAuth2Token;

/**
 *
 * Enables inject of token type handlers to add OAuth2 auth data to resource
 * requests.
 *
 * By default shindig supports "Bearer" token types.
 *
 * Matches on {@link OAuth2Token#getTokenType()}
 *
 * All matching handlers are executed.
 *
 */
public interface ResourceRequestHandler {
  /**
   * Do the handler magic for the token type.
   *
   * @param accessor
   * @param request
   * @return {@link OAuth2HandlerError} if one occurs
   */
  public OAuth2HandlerError addOAuth2Params(final OAuth2Accessor accessor, final HttpRequest request);

  /**
   *
   * @return the token type this handler handles
   */
  public String getTokenType();
}
