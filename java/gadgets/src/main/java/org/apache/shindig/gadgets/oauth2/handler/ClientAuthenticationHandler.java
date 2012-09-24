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

/**
 * See {@link OAuth2Accessor#getClientAuthenticationType()} See {@link http
 * ://tools.ietf.org/html/draft-ietf-oauth-v2-21#section-2.3}
 *
 * Enables injection of new Client Authentication schemes into the system.
 *
 * If a {@link ClientAuthenticationHandler#geClientAuthenticationType()} matches a
 * {@link OAuth2Accessor#getClientAuthenticationType()} it will be invoked for the outbound request
 * to the service provider.
 *
 * By default "Basic" and "STANDARD" (client_id and client_secret added to request parameters) are
 * supported.
 */

public interface ClientAuthenticationHandler {
  /**
   * Handler implementation will modify request as necessary.
   *
   * @param request
   * @param accessor
   * @return indicates failure by returning a {@link OAuth2HandlerError}
   */
  OAuth2HandlerError addOAuth2Authentication(HttpRequest request, OAuth2Accessor accessor);

  /**
   *
   * @return the Client Authentication type for this handler
   */
  String geClientAuthenticationType();
}
