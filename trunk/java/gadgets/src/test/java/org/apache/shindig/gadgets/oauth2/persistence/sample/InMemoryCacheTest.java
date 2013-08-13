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
package org.apache.shindig.gadgets.oauth2.persistence.sample;

import org.apache.shindig.gadgets.oauth2.BasicOAuth2Accessor;
import org.apache.shindig.gadgets.oauth2.MockUtils;
import org.apache.shindig.gadgets.oauth2.OAuth2Accessor;
import org.apache.shindig.gadgets.oauth2.OAuth2CallbackState;
import org.apache.shindig.gadgets.oauth2.OAuth2Message;
import org.apache.shindig.gadgets.oauth2.OAuth2Store;
import org.apache.shindig.gadgets.oauth2.OAuth2Token;
import org.apache.shindig.gadgets.oauth2.OAuth2Token.Type;
import org.apache.shindig.gadgets.oauth2.persistence.OAuth2Cache;
import org.apache.shindig.gadgets.oauth2.persistence.OAuth2Client;
import org.apache.shindig.gadgets.oauth2.persistence.OAuth2TokenPersistence;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.HashSet;

public class InMemoryCacheTest extends MockUtils {

  private InMemoryCache cache;

  @Before
  public void setUp() throws Exception {
    this.cache = new InMemoryCache();
    Assert.assertNotNull(this.cache);
    Assert.assertTrue(OAuth2Cache.class.isInstance(this.cache));

    this.cache.storeClient(MockUtils.getClient_Code_Confidential());
    this.cache.storeClient(MockUtils.getClient_Code_Public());

    this.cache.storeToken(MockUtils.getAccessToken());
    this.cache.storeToken(MockUtils.getRefreshToken());

    this.cache.storeOAuth2Accessor(MockUtils.getOAuth2Accessor_Code());
    this.cache.storeOAuth2Accessor(MockUtils.getOAuth2Accessor_Error());
  }

  @Test
  public void testClearClients_1() throws Exception {
    Assert.assertNotNull(this.cache.getClient(MockUtils.GADGET_URI1, MockUtils.SERVICE_NAME));

    this.cache.clearClients();

    Assert.assertNull(this.cache.getClient(MockUtils.GADGET_URI1, MockUtils.SERVICE_NAME));
  }

  @Test
  public void testClearTokens_1() throws Exception {
    Assert.assertNotNull(this.cache.getToken(MockUtils.GADGET_URI1, MockUtils.SERVICE_NAME,
            MockUtils.USER, MockUtils.SCOPE, Type.ACCESS));

    this.cache.clearTokens();

    Assert.assertNull(this.cache.getToken(MockUtils.GADGET_URI1, MockUtils.SERVICE_NAME,
            MockUtils.USER, MockUtils.SCOPE, Type.ACCESS));
  }

  @Test
  public void testGetClient_1() throws Exception {
    final OAuth2Client result = this.cache.getClient(MockUtils.GADGET_URI1, MockUtils.SERVICE_NAME);

    Assert.assertNotNull(result);
    Assert.assertEquals(MockUtils.CLIENT_ID1, result.getClientId());
  }

  @Test
  public void testGetOAuth2Accessor_1() throws Exception {
    final OAuth2Accessor accessor = MockUtils.getOAuth2Accessor_Code();

    final OAuth2CallbackState state = new OAuth2CallbackState(MockUtils.getDummyStateCrypter());
    state.setGadgetUri(accessor.getGadgetUri());
    state.setServiceName(accessor.getServiceName());
    state.setUser(accessor.getUser());
    state.setScope(accessor.getScope());

    final OAuth2Accessor result = this.cache.getOAuth2Accessor(state);

    Assert.assertNotNull(result);
    Assert.assertEquals(MockUtils.CLIENT_ID1, result.getClientId());
  }

  @Test
  public void testGetOAuth2Accessor_3() throws Exception {
    final OAuth2CallbackState state = new OAuth2CallbackState(MockUtils.getDummyStateCrypter());
    state.setGadgetUri("BAD");
    state.setServiceName("BAD");
    state.setUser("BAD");
    state.setScope("BAD");
    final OAuth2Accessor result = this.cache.getOAuth2Accessor(state);

    Assert.assertNull(result);
  }

  @Test
  public void testGetToken_1() throws Exception {
    final OAuth2Token result = this.cache.getToken(MockUtils.GADGET_URI1, MockUtils.SERVICE_NAME,
            MockUtils.USER, MockUtils.SCOPE, Type.ACCESS);

    Assert.assertNotNull(result);
    Assert.assertEquals(MockUtils.ACCESS_SECRET, new String(result.getSecret(), "UTF-8"));
  }

  @Test
  public void testRemoveClient_1() throws Exception {

    OAuth2Client result = this.cache.getClient(MockUtils.GADGET_URI1, MockUtils.SERVICE_NAME);

    Assert.assertNotNull(result);

    result = this.cache.removeClient(result);

    Assert.assertNotNull(result);

    result = this.cache.removeClient(result);

    Assert.assertNull(result);
  }

  @Test
  public void testRemoveToken_1() throws Exception {

    OAuth2Token result = this.cache.getToken(MockUtils.GADGET_URI1, MockUtils.SERVICE_NAME,
            MockUtils.USER, MockUtils.SCOPE, Type.ACCESS);

    Assert.assertNotNull(result);

    result = this.cache.removeToken(result);

    Assert.assertNotNull(result);

    result = this.cache.removeToken(result);

    Assert.assertNull(result);

  }

  @Test
  public void testStoreClient_1() throws Exception {

    OAuth2Client client = new OAuth2Client(MockUtils.getDummyEncrypter());
    client.setGadgetUri("xxx");
    client.setServiceName("yyy");

    this.cache.storeClient(client);

    client = this.cache.getClient(client.getGadgetUri(), client.getServiceName());

    Assert.assertNotNull(client);
    Assert.assertEquals("xxx", client.getGadgetUri());
    Assert.assertEquals("yyy", client.getServiceName());
  }

  @Test
  public void testStoreClients_1() throws Exception {

    this.cache.clearClients();

    final Collection<OAuth2Client> clients = new HashSet<OAuth2Client>();
    clients.add(MockUtils.getClient_Code_Confidential());
    clients.add(MockUtils.getClient_Code_Public());

    this.cache.storeClients(clients);

    Assert.assertNotNull(this.cache.getClient(MockUtils.GADGET_URI1, MockUtils.SERVICE_NAME));
    Assert.assertNotNull(this.cache.getClient(MockUtils.GADGET_URI2, MockUtils.SERVICE_NAME));
  }

  @Test
  public void testStoreOAuth2Accessor_1() throws Exception {
    final OAuth2Store store = MockUtils.getDummyStore(this.cache, null, null, null, null, null,
            null);
    OAuth2Accessor accessor = new BasicOAuth2Accessor("XXX", "YYY", "ZZZ", "", false, store, "AAA",
            null, null);

    this.cache.storeOAuth2Accessor(accessor);

    final OAuth2CallbackState state = new OAuth2CallbackState(MockUtils.getDummyStateCrypter());
    state.setGadgetUri(accessor.getGadgetUri());
    state.setServiceName(accessor.getServiceName());
    state.setUser(accessor.getUser());
    state.setScope(accessor.getScope());
    accessor = this.cache.getOAuth2Accessor(state);

    Assert.assertNotNull(accessor);
    Assert.assertEquals("XXX", accessor.getGadgetUri());
    Assert.assertEquals("YYY", accessor.getServiceName());
    Assert.assertEquals("ZZZ", accessor.getUser());
    Assert.assertEquals("", accessor.getScope());
    Assert.assertEquals(false, accessor.isAllowModuleOverrides());
    Assert.assertEquals("AAA", accessor.getRedirectUri());
  }

  @Test
  public void testStoreToken_1() throws Exception {
    OAuth2Token token = new OAuth2TokenPersistence(MockUtils.getDummyEncrypter());
    token.setGadgetUri("xxx");
    token.setServiceName("yyy");
    token.setExpiresAt(2);
    token.setIssuedAt(1);
    token.setMacAlgorithm(OAuth2Message.HMAC_SHA_1);
    token.setMacSecret("shh, it's a secret".getBytes("UTF-8"));
    token.setScope("mac_scope");
    token.setSecret("i'll never tell".getBytes("UTF-8"));
    token.setTokenType(OAuth2Message.MAC_TOKEN_TYPE);
    token.setType(OAuth2Token.Type.ACCESS);
    token.setUser("zzz");

    this.cache.storeToken(token);

    token = this.cache.getToken(token.getGadgetUri(), token.getServiceName(), token.getUser(),
            token.getScope(), token.getType());

    Assert.assertNotNull(token);
    Assert.assertEquals("xxx", token.getGadgetUri());
    Assert.assertEquals("yyy", token.getServiceName());

    Assert.assertEquals(2, token.getExpiresAt());
    Assert.assertEquals(1, token.getIssuedAt());
    Assert.assertEquals(OAuth2Message.HMAC_SHA_1, token.getMacAlgorithm());
    Assert.assertEquals("shh, it's a secret", new String(token.getMacSecret(), "UTF-8"));
    Assert.assertEquals("mac_scope", token.getScope());
    Assert.assertEquals("i'll never tell", new String(token.getSecret(), "UTF-8"));
    Assert.assertEquals(OAuth2Message.MAC_TOKEN_TYPE, token.getTokenType());
    Assert.assertEquals(OAuth2Token.Type.ACCESS, token.getType());
    Assert.assertEquals("zzz", token.getUser());
  }

  @Test
  public void testStoreTokens_1() throws Exception {
    this.cache.clearTokens();

    final Collection<OAuth2Token> tokens = new HashSet<OAuth2Token>(2);

    final OAuth2Token accessToken = MockUtils.getAccessToken();
    final OAuth2Token refreshToken = MockUtils.getRefreshToken();

    tokens.add(accessToken);
    tokens.add(refreshToken);

    this.cache.storeTokens(tokens);

    Assert.assertNotNull(this.cache.getToken(accessToken.getGadgetUri(),
            accessToken.getServiceName(), accessToken.getUser(), accessToken.getScope(),
            accessToken.getType()));
    Assert.assertNotNull(this.cache.getToken(refreshToken.getGadgetUri(),
            refreshToken.getServiceName(), refreshToken.getUser(), refreshToken.getScope(),
            refreshToken.getType()));
  }
}
