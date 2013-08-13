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
package org.apache.shindig.gadgets.oauth2;

import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;

/**
 * Implements OAuth2 fetch for gadgets.
 *
 * OAuth 2.0 authorization_code flows will redirects the user to the OAuth 2.0
 * service provider site to obtain the user's permission to access their data.
 *
 * See {@link http://oauth.net/2/}.
 *
 */
public interface OAuth2Request {
  /**
   * OAuth 2.0 authenticated request
   *
   * @param request
   *          gadget request
   *
   * @return the response to send to the client, never <code>null</code>
   */
  public HttpResponse fetch(final HttpRequest request);
}
