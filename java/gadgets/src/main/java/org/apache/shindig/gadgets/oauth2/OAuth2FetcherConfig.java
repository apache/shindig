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

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * Configuration parameters for an OAuth2Request
 */
public class OAuth2FetcherConfig {
  private final GadgetOAuth2TokenStore tokenStore;
  private final boolean viewerAccessTokensEnabled;
  public static final String OAUTH2_STATE_CRYPTER = "shindig.oauth2.state-crypter";

  @Inject
  public OAuth2FetcherConfig(final GadgetOAuth2TokenStore tokenStore,
          @Named("shindig.oauth2.viewer-access-tokens-enabled")
          final boolean viewerAccessTokensEnabled) {
    this.tokenStore = tokenStore;
    this.viewerAccessTokensEnabled = viewerAccessTokensEnabled;
  }

  /**
   * @return the store with persisted client and token information
   */
  public OAuth2Store getOAuth2Store() {
    return this.tokenStore.getOAuth2Store();
  }

  /**
   * @return the store with gadget spec information
   */
  public GadgetOAuth2TokenStore getTokenStore() {
    return this.tokenStore;
  }

  /**
   * @return true if the owner pages do not allow user controlled javascript
   */
  public boolean isViewerAccessTokensEnabled() {
    return this.viewerAccessTokensEnabled;
  }
}
