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

import java.util.Collection;
import java.util.HashSet;

import org.apache.shindig.gadgets.oauth2.BasicOAuth2Accessor;
import org.apache.shindig.gadgets.oauth2.MockUtils;
import org.apache.shindig.gadgets.oauth2.OAuth2Accessor;
import org.apache.shindig.gadgets.oauth2.OAuth2Message;
import org.apache.shindig.gadgets.oauth2.OAuth2Store;
import org.apache.shindig.gadgets.oauth2.OAuth2Token;
import org.apache.shindig.gadgets.oauth2.persistence.OAuth2Cache;
import org.apache.shindig.gadgets.oauth2.persistence.OAuth2Client;
import org.apache.shindig.gadgets.oauth2.persistence.OAuth2TokenPersistence;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

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
    Assert.assertNotNull(this.cache.getClient(MockUtils.CLIENT_INDEX1));

    this.cache.clearClients();

    Assert.assertNull(this.cache.getClient(MockUtils.CLIENT_INDEX1));
  }

  @Test
  public void testClearTokens_1() throws Exception {
    Assert.assertNotNull(this.cache.getToken(MockUtils.ACCESS_TOKEN_INDEX));

    this.cache.clearTokens();

    Assert.assertNull(this.cache.getToken(MockUtils.ACCESS_TOKEN_INDEX));
  }

  @Test
  public void testGetClient_1() throws Exception {
    final OAuth2Client result = this.cache.getClient(MockUtils.CLIENT_INDEX1);

    Assert.assertNotNull(result);
    Assert.assertEquals(MockUtils.CLIENT_ID1, result.getClientId());
  }

  @Test
  public void testGetClientIndex_1() throws Exception {

    final Integer result = this.cache.getClientIndex(MockUtils.GADGET_URI1, MockUtils.SERVICE_NAME);

    Assert.assertNotNull(result);
    Assert.assertEquals(MockUtils.CLIENT_INDEX1, result);
  }

  @Test
  public void testGetOAuth2Accessor_1() throws Exception {
    final OAuth2Accessor result = this.cache.getOAuth2Accessor(MockUtils.ACCESSOR_INDEX1);

    Assert.assertNotNull(result);
    Assert.assertEquals(MockUtils.CLIENT_ID1, result.getClientId());
  }

  @Test
  public void testGetOAuth2Accessor_2() throws Exception {
    final OAuth2Accessor result = this.cache.getOAuth2Accessor(null);

    Assert.assertNull(result);
  }

  @Test
  public void testGetOAuth2Accessor_3() throws Exception {
    final OAuth2Accessor result = this.cache.getOAuth2Accessor(MockUtils.BAD_INDEX);

    Assert.assertNull(result);
  }

  @Test
  public void testGetOAuth2AccessorIndex_1() throws Exception {

    final Integer result = this.cache.getOAuth2AccessorIndex(MockUtils.GADGET_URI1,
        MockUtils.SERVICE_NAME, MockUtils.USER, MockUtils.SCOPE);

    Assert.assertNotNull(result);
    Assert.assertEquals(MockUtils.ACCESSOR_INDEX1, result);
  }

  @Test
  public void testGetToken_1() throws Exception {
    final OAuth2Token result = this.cache.getToken(MockUtils.ACCESS_TOKEN_INDEX);

    Assert.assertNotNull(result);
    Assert.assertEquals(MockUtils.ACCESS_SECRET, new String(result.getSecret(), "UTF-8"));
  }

  @Test
  public void testGetTokenMockUtilsIndex_1() throws Exception {
    final Integer result = this.cache.getTokenIndex(MockUtils.getAccessToken());

    Assert.assertNotNull(result);
    Assert.assertEquals(MockUtils.ACCESS_TOKEN_INDEX, result);
  }

  @Test
  public void testGetTokenIndex_2() throws Exception {

    final OAuth2Token token = null;

    final Integer result = this.cache.getTokenIndex(token);

    Assert.assertNull(result);
  }

  @Test
  public void testGetTokenIndex_3() throws Exception {

    final Integer result = this.cache.getTokenIndex(MockUtils.GADGET_URI1, MockUtils.SERVICE_NAME,
        MockUtils.USER, MockUtils.SCOPE, OAuth2Token.Type.ACCESS);

    Assert.assertNotNull(result);
    Assert.assertEquals(MockUtils.ACCESS_TOKEN_INDEX, result);
  }

  @Test
  public void testRemoveClient_1() throws Exception {

    OAuth2Client result = this.cache.getClient(MockUtils.CLIENT_INDEX1);

    Assert.assertNotNull(result);

    result = this.cache.removeClient(MockUtils.CLIENT_INDEX1);

    Assert.assertNotNull(result);

    result = this.cache.removeClient(MockUtils.CLIENT_INDEX1);

    Assert.assertNull(result);
  }

  @Test
  public void testRemoveOAuth2Accessor_1() throws Exception {

    final OAuth2Accessor result = this.cache.removeOAuth2Accessor(MockUtils.BAD_INDEX);

    Assert.assertNull(result);
  }

  @Test
  public void testRemoveToken_1() throws Exception {

    OAuth2Token result = this.cache.getToken(MockUtils.ACCESS_TOKEN_INDEX);

    Assert.assertNotNull(result);

    result = this.cache.removeToken(MockUtils.ACCESS_TOKEN_INDEX);

    Assert.assertNotNull(result);

    result = this.cache.removeToken(MockUtils.ACCESS_TOKEN_INDEX);

    Assert.assertNull(result);

  }

  @Test
  public void testStoreClient_1() throws Exception {

    OAuth2Client client = new OAuth2Client(MockUtils.getDummyEncrypter());
    client.setGadgetUri("xxx");
    client.setServiceName("yyy");

    final Integer result = this.cache.storeClient(client);

    Assert.assertEquals(909248813, result.intValue());

    client = this.cache.getClient(result);

    Assert.assertNotNull(client);
    Assert.assertEquals("xxx", client.getGadgetUri());
    Assert.assertEquals("yyy", client.getServiceName());
  }

  @Test
  public void testStoreClient_2() throws Exception {

    final OAuth2Client client = null;

    final Integer result = this.cache.storeClient(client);

    Assert.assertNull(result);
  }

  @Test
  public void testStoreClients_1() throws Exception {

    this.cache.clearClients();

    final Collection<OAuth2Client> clients = new HashSet<OAuth2Client>();
    clients.add(MockUtils.getClient_Code_Confidential());
    clients.add(MockUtils.getClient_Code_Public());

    this.cache.storeClients(clients);

    Assert.assertNotNull(this.cache.getClient(MockUtils.CLIENT_INDEX1));
    Assert.assertNotNull(this.cache.getClient(MockUtils.CLIENT_INDEX2));
  }

  @Test
  public void testStoreOAuth2Accessor_1() throws Exception {
    final OAuth2Store store = MockUtils.getDummyStore(this.cache, null, null);
    OAuth2Accessor accessor = new BasicOAuth2Accessor("XXX", "YYY", "ZZZ", "", false, store, "AAA");

    final Integer result = this.cache.storeOAuth2Accessor(accessor);

    Assert.assertEquals(-1664180105, result.intValue());

    accessor = this.cache.getOAuth2Accessor(result);

    Assert.assertNotNull(accessor);
    Assert.assertEquals("XXX", accessor.getGadgetUri());
    Assert.assertEquals("YYY", accessor.getServiceName());
    Assert.assertEquals("ZZZ", accessor.getUser());
    Assert.assertEquals("", accessor.getScope());
    Assert.assertEquals(false, accessor.isAllowModuleOverrides());
    Assert.assertEquals("AAA", accessor.getRedirectUri());
    Assert.assertEquals("-1664180105", accessor.getState());
  }

  @Test
  public void testStoreOAuth2Accessor_2() throws Exception {

    final OAuth2Accessor accessor = null;

    final Integer result = this.cache.storeOAuth2Accessor(accessor);

    Assert.assertEquals(null, result);
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

    final Integer result = this.cache.storeToken(token);

    Assert.assertEquals(460203885, result.intValue());

    token = this.cache.getToken(result);

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
  public void testStoreToken_2() throws Exception {

    final OAuth2Token token = null;

    final Integer result = this.cache.storeToken(token);

    Assert.assertEquals(null, result);
  }

  @Test
  public void testStoreTokens_1() throws Exception {
    this.cache.clearTokens();

    final Collection<OAuth2Token> tokens = new HashSet<OAuth2Token>(2);
    tokens.add(MockUtils.getAccessToken());
    tokens.add(MockUtils.getRefreshToken());

    this.cache.storeTokens(tokens);

    Assert.assertNotNull(this.cache.getToken(MockUtils.ACCESS_TOKEN_INDEX));
    Assert.assertNotNull(this.cache.getToken(MockUtils.REFRESH_TOKEN_INDEX));
  }
}