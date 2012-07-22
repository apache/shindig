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
package org.apache.shindig.gadgets.oauth;

import org.apache.shindig.common.crypto.BlobCrypter;
import org.apache.shindig.common.util.TimeSource;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * Configuration parameters for an OAuthRequest
 */
public class OAuthFetcherConfig {

  public static final String OAUTH_STATE_CRYPTER = "shindig.oauth.state-crypter";

  private final BlobCrypter stateCrypter;
  private final GadgetOAuthTokenStore tokenStore;
  private final TimeSource clock;
  private final OAuthCallbackGenerator oauthCallbackGenerator;
  private final boolean viewerAccessTokensEnabled;

  @Inject
  public OAuthFetcherConfig(
      @Named(OAUTH_STATE_CRYPTER) BlobCrypter stateCrypter,
      GadgetOAuthTokenStore tokenStore,
      TimeSource clock,
      OAuthCallbackGenerator oauthCallbackGenerator,
      @Named("shindig.signing.viewer-access-tokens-enabled") boolean viewerAccessTokensEnabled) {
    this.stateCrypter = stateCrypter;
    this.tokenStore = tokenStore;
    this.clock = clock;
    this.oauthCallbackGenerator = oauthCallbackGenerator;
    this.viewerAccessTokensEnabled = viewerAccessTokensEnabled;
  }

  /**
   * @return A BlobCrypter Used to encrypt state stored on the client.
   */
  public BlobCrypter getStateCrypter() {
    return stateCrypter;
  }

  /**
   * @return the persistent token storage.
   */
  public GadgetOAuthTokenStore getTokenStore() {
    return tokenStore;
  }

  /**
   * @return the Clock
   */
  public TimeSource getClock() {
    return clock;
  }

  /**
   * @return callback Url generator
   */
  public OAuthCallbackGenerator getOAuthCallbackGenerator() {
    return oauthCallbackGenerator;
  }

  /**
   * @return true if the owner pages do not allow user controlled javascript
   */
  public boolean isViewerAccessTokensEnabled() {
     return viewerAccessTokensEnabled;
  }
}
