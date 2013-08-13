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

import org.apache.shindig.gadgets.GadgetSpecFactory;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

public class OAuth2FetcherConfigTest {
  @Test
  public void testOAuth2FetcherConfig_1() throws Exception {
    final GadgetOAuth2TokenStore tokenStore = new GadgetOAuth2TokenStore(
        EasyMock.createNiceMock(OAuth2Store.class),
        EasyMock.createNiceMock(GadgetSpecFactory.class));
    final boolean viewerAccessTokensEnabled = true;

    final OAuth2FetcherConfig result = new OAuth2FetcherConfig(tokenStore,
        viewerAccessTokensEnabled);

    Assert.assertNotNull(result);
    Assert.assertEquals(true, result.isViewerAccessTokensEnabled());
  }

  @Test
  public void testGetOAuth2Store_1() throws Exception {
    final OAuth2FetcherConfig fixture = new OAuth2FetcherConfig(new GadgetOAuth2TokenStore(
        EasyMock.createNiceMock(OAuth2Store.class),
        EasyMock.createNiceMock(GadgetSpecFactory.class)), true);

    final OAuth2Store result = fixture.getOAuth2Store();

    Assert.assertNotNull(result);
    Assert.assertEquals(false, result.clearCache());
  }

  @Test
  public void testGetTokenStore_1() throws Exception {
    final OAuth2FetcherConfig fixture = new OAuth2FetcherConfig(new GadgetOAuth2TokenStore(
        EasyMock.createNiceMock(OAuth2Store.class),
        EasyMock.createNiceMock(GadgetSpecFactory.class)), true);

    final GadgetOAuth2TokenStore result = fixture.getTokenStore();

    Assert.assertNotNull(result);
  }

  @Test
  public void testIsViewerAccessTokensEnabled_1() throws Exception {
    final OAuth2FetcherConfig fixture = new OAuth2FetcherConfig(new GadgetOAuth2TokenStore(
        EasyMock.createNiceMock(OAuth2Store.class),
        EasyMock.createNiceMock(GadgetSpecFactory.class)), true);

    final boolean result = fixture.isViewerAccessTokensEnabled();

    Assert.assertEquals(true, result);
  }

  @Test
  public void testIsViewerAccessTokensEnabled_2() throws Exception {
    final OAuth2FetcherConfig fixture = new OAuth2FetcherConfig(new GadgetOAuth2TokenStore(
        EasyMock.createNiceMock(OAuth2Store.class),
        EasyMock.createNiceMock(GadgetSpecFactory.class)), false);

    final boolean result = fixture.isViewerAccessTokensEnabled();

    Assert.assertEquals(false, result);
  }
}
