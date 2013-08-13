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

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.GadgetContext;

/**
 * GadgetContext for use when handling an OAuth2 request.
 */
public class OAuth2GadgetContext extends GadgetContext {

  private final Uri appUrl;
  private final boolean bypassSpecCache;
  private final String container;
  private final String scope;
  private final SecurityToken securityToken;

  public OAuth2GadgetContext(final SecurityToken securityToken, final OAuth2Arguments arguments,
      final Uri gadgetUri) {
    this.securityToken = securityToken;
    this.container = securityToken.getContainer();
    this.appUrl = gadgetUri;
    this.bypassSpecCache = arguments.getBypassSpecCache();
    this.scope = arguments.getScope();
  }

  @Override
  public String getContainer() {
    return this.container;
  }

  @Override
  public boolean getIgnoreCache() {
    return this.bypassSpecCache;
  }

  public String getScope() {
    return this.scope;
  }

  @Override
  public SecurityToken getToken() {
    return this.securityToken;
  }

  @Override
  public Uri getUrl() {
    return this.appUrl;
  }
}
