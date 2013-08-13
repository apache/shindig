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
import org.apache.shindig.common.EasyMockTestCase;
import org.apache.shindig.gadgets.http.HttpCache;

import org.junit.Test;

/**
 * Simple test for a simple class.
 */
public class OAuthFetcherConfigTest extends EasyMockTestCase {

  @Test
  public void testOAuthFetcherConfig() {
    BlobCrypter crypter = mock(BlobCrypter.class);
    mock(HttpCache.class);
    GadgetOAuthTokenStore tokenStore = mock(GadgetOAuthTokenStore.class);
    OAuthCallbackGenerator callbackGenerator = mock(OAuthCallbackGenerator.class);
    OAuthFetcherConfig config = new OAuthFetcherConfig(crypter, tokenStore, new TimeSource(),
        callbackGenerator, false);
    assertEquals(crypter, config.getStateCrypter());
    assertEquals(tokenStore, config.getTokenStore());
    assertEquals(callbackGenerator, config.getOAuthCallbackGenerator());
    assertFalse(config.isViewerAccessTokensEnabled());
  }
}
