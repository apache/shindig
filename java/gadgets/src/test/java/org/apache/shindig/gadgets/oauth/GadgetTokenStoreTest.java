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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.apache.shindig.common.crypto.BasicBlobCrypter;
import org.apache.shindig.common.crypto.BlobCrypter;
import org.apache.shindig.common.testing.FakeGadgetToken;
import org.apache.shindig.gadgets.FakeGadgetSpecFactory;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.oauth.AccessorInfo.HttpMethod;
import org.apache.shindig.gadgets.oauth.AccessorInfo.OAuthParamLocation;
import org.apache.shindig.gadgets.oauth.BasicOAuthStoreConsumerKeyAndSecret.KeyType;
import org.apache.shindig.gadgets.oauth.OAuthArguments.UseToken;
import org.apache.shindig.gadgets.oauth.OAuthStore.TokenInfo;
import org.junit.Before;
import org.junit.Test;

public class GadgetTokenStoreTest {

  private static final String GADGET_URL = "http://www.example.com/gadget.xml";
  
  public static final String GADGET_SPEC =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
      "  <Module>\n" +
      "    <ModulePrefs title=\"hello world example\">\n" +
      "   \n" +
      "    <OAuth>\n" +
      "      <Service name='testservice'>" +
      "        <Access " + 
      "          url='" + FakeOAuthServiceProvider.ACCESS_TOKEN_URL + '\'' +
      "          param_location='uri-query' " +
      "          method='GET'" +
      "        />" +
      "        <Request " + 
      "          url='" + FakeOAuthServiceProvider.REQUEST_TOKEN_URL + '\'' +
      "          param_location='uri-query' " +
      "          method='GET'" +
      "        />" +
      "        <Authorization " + 
      "          url='" + FakeOAuthServiceProvider.APPROVAL_URL + '\'' +
      "        />" +
      "      </Service>" +
      "    </OAuth>\n" +
      "    </ModulePrefs>\n" +
      "    <Content type=\"html\">\n" +
      "       <![CDATA[\n" +
      "         Hello, world!\n" +
      "       ]]>\n" +
      "       \n" +
      "    </Content>\n" +
      "  </Module>";

  private BasicOAuthStore backingStore;
  private GadgetOAuthTokenStore store;
  private FakeGadgetToken socialToken;
  private FakeGadgetToken privateToken;
  private BlobCrypter stateCrypter;
  private OAuthClientState clientState;

  @Before
  public void setUp() throws Exception {
    backingStore = new BasicOAuthStore();
    backingStore.setDefaultKey(
        new BasicOAuthStoreConsumerKeyAndSecret("key", "secret", KeyType.RSA_PRIVATE, "keyname"));
    store = new GadgetOAuthTokenStore(backingStore, new FakeGadgetSpecFactory());
    
    socialToken = new FakeGadgetToken();
    socialToken.setOwnerId("owner");
    socialToken.setViewerId("viewer");
    socialToken.setAppUrl(GADGET_URL);
    
    privateToken = new FakeGadgetToken();
    privateToken.setOwnerId("owner");
    privateToken.setViewerId("owner");
    privateToken.setAppUrl(GADGET_URL);
    
    stateCrypter = new BasicBlobCrypter("abcdefghijklmnop".getBytes());
    clientState = new OAuthClientState(stateCrypter);
  }
  
  @Test
  public void testGetOAuthAccessor_signedFetch() throws Exception {
    OAuthArguments arguments = new OAuthArguments();
    arguments.setUseToken(UseToken.NEVER);
    AccessorInfo info = store.getOAuthAccessor(socialToken, arguments, clientState);
    assertEquals(OAuthParamLocation.URI_QUERY, info.getParamLocation());
    assertEquals("keyname", info.getConsumer().getKeyName());
    assertEquals("key", info.getConsumer().getConsumer().consumerKey);
    assertNull(info.getConsumer().getConsumer().consumerSecret);
    assertNull(info.getAccessor().requestToken);
    assertNull(info.getAccessor().accessToken);
    assertNull(info.getAccessor().tokenSecret);
  }
  
  @Test
  public void testGetOAuthAccessor_useToken_noOAuthInSpec() throws Exception {
    OAuthArguments arguments = new OAuthArguments();
    arguments.setUseToken(UseToken.IF_AVAILABLE);
    try {
      store.getOAuthAccessor(socialToken, arguments, clientState);
      fail();
    } catch (GadgetException e) {
      // good
    }
  }
  
  @Test
  public void testGetOAuthAccessor_signedFetch_hmacKey() throws Exception {
    BasicOAuthStoreConsumerIndex index = new BasicOAuthStoreConsumerIndex();
    index.setGadgetUri(GADGET_URL);
    index.setServiceName("hmac");
    BasicOAuthStoreConsumerKeyAndSecret cks =
        new BasicOAuthStoreConsumerKeyAndSecret("hmac", "hmacsecret", KeyType.HMAC_SYMMETRIC, null);
    backingStore.setConsumerKeyAndSecret(index, cks);
    OAuthArguments arguments = new OAuthArguments();
    arguments.setUseToken(UseToken.NEVER);
    arguments.setServiceName("hmac");
    AccessorInfo info = store.getOAuthAccessor(socialToken, arguments, clientState);
    assertEquals(OAuthParamLocation.URI_QUERY, info.getParamLocation());
    assertEquals(null, info.getConsumer().getKeyName());
    assertEquals("hmac", info.getConsumer().getConsumer().consumerKey);
    assertEquals("hmacsecret", info.getConsumer().getConsumer().consumerSecret);
    assertNull(info.getAccessor().requestToken);
    assertNull(info.getAccessor().accessToken);
    assertNull(info.getAccessor().tokenSecret);
  }

  @Test
  public void testGetOAuthAccessor_signedFetch_badServiceName() throws Exception {
    BasicOAuthStoreConsumerIndex index = new BasicOAuthStoreConsumerIndex();
    index.setGadgetUri(GADGET_URL);
    index.setServiceName("otherservice");
    BasicOAuthStoreConsumerKeyAndSecret cks =
        new BasicOAuthStoreConsumerKeyAndSecret("hmac", "hmacsecret", KeyType.HMAC_SYMMETRIC, null);
    backingStore.setConsumerKeyAndSecret(index, cks);
    OAuthArguments arguments = new OAuthArguments();
    arguments.setUseToken(UseToken.NEVER);
    arguments.setServiceName("hmac");
    AccessorInfo info = store.getOAuthAccessor(socialToken, arguments, clientState);
    assertEquals("keyname", info.getConsumer().getKeyName());
    assertEquals("key", info.getConsumer().getConsumer().consumerKey);
  }
  
  @Test
  public void testGetOAuthAccessor_signedFetch_defaultHmac() throws Exception {
    BasicOAuthStoreConsumerIndex index = new BasicOAuthStoreConsumerIndex();
    index.setGadgetUri(GADGET_URL);
    index.setServiceName("");
    BasicOAuthStoreConsumerKeyAndSecret cks =
        new BasicOAuthStoreConsumerKeyAndSecret("hmac", "hmacsecret", KeyType.HMAC_SYMMETRIC, null);
    backingStore.setConsumerKeyAndSecret(index, cks);
    OAuthArguments arguments = new OAuthArguments();
    arguments.setUseToken(UseToken.NEVER);
    AccessorInfo info = store.getOAuthAccessor(socialToken, arguments, clientState);
    assertEquals(OAuthParamLocation.URI_QUERY, info.getParamLocation());
    assertEquals(null, info.getConsumer().getKeyName());
    assertEquals("hmac", info.getConsumer().getConsumer().consumerKey);
    assertEquals("hmacsecret", info.getConsumer().getConsumer().consumerSecret);
    assertNull(info.getAccessor().requestToken);
    assertNull(info.getAccessor().accessToken);
    assertNull(info.getAccessor().tokenSecret);
  }
  
  @Test
  public void testGetOAuthAccessor_socialOAuth_socialPage() throws Exception {
    BasicOAuthStoreConsumerIndex index = new BasicOAuthStoreConsumerIndex();
    index.setGadgetUri(GADGET_URL);
    index.setServiceName("testservice");
    BasicOAuthStoreConsumerKeyAndSecret cks =
        new BasicOAuthStoreConsumerKeyAndSecret("hmac", "hmacsecret", KeyType.HMAC_SYMMETRIC, null);
    backingStore.setConsumerKeyAndSecret(index, cks);
    OAuthArguments arguments = new OAuthArguments();
    arguments.setServiceName("testservice");
    arguments.setUseToken(UseToken.IF_AVAILABLE);
    AccessorInfo info = store.getOAuthAccessor(socialToken, arguments, clientState);
    assertEquals(OAuthParamLocation.URI_QUERY, info.getParamLocation());
    assertEquals(null, info.getConsumer().getKeyName());
    assertEquals("hmac", info.getConsumer().getConsumer().consumerKey);
    assertEquals("hmacsecret", info.getConsumer().getConsumer().consumerSecret);
    assertNull(info.getAccessor().requestToken);
    assertNull(info.getAccessor().accessToken);
    assertNull(info.getAccessor().tokenSecret);
  }
  
  @Test
  public void testGetOAuthAccessor_socialOAuth_privatePage() throws Exception {
    BasicOAuthStoreConsumerIndex index = new BasicOAuthStoreConsumerIndex();
    index.setGadgetUri(GADGET_URL);
    index.setServiceName("testservice");
    BasicOAuthStoreConsumerKeyAndSecret cks =
        new BasicOAuthStoreConsumerKeyAndSecret("hmac", "hmacsecret", KeyType.HMAC_SYMMETRIC, null);
    backingStore.setConsumerKeyAndSecret(index, cks);
    OAuthArguments arguments = new OAuthArguments();
    arguments.setServiceName("testservice");
    arguments.setUseToken(UseToken.IF_AVAILABLE);
    AccessorInfo info = store.getOAuthAccessor(privateToken, arguments, clientState);
    assertEquals(OAuthParamLocation.URI_QUERY, info.getParamLocation());
    assertEquals(null, info.getConsumer().getKeyName());
    assertEquals("hmac", info.getConsumer().getConsumer().consumerKey);
    assertEquals("hmacsecret", info.getConsumer().getConsumer().consumerSecret);
    assertNull(info.getAccessor().requestToken);
    assertNull(info.getAccessor().accessToken);
    assertNull(info.getAccessor().tokenSecret);
  }
  
  @Test
  public void testGetOAuthAccessor_socialOAuth_withToken() throws Exception {
    BasicOAuthStoreConsumerIndex index = new BasicOAuthStoreConsumerIndex();
    index.setGadgetUri(GADGET_URL);
    index.setServiceName("testservice");
    BasicOAuthStoreConsumerKeyAndSecret cks =
        new BasicOAuthStoreConsumerKeyAndSecret("hmac", "hmacsecret", KeyType.HMAC_SYMMETRIC, null);
    backingStore.setConsumerKeyAndSecret(index, cks);
    
    backingStore.setTokenInfo(privateToken, null, "testservice", "",
        new TokenInfo("token", "secret"));
    
    // Owner views their own page
    OAuthArguments arguments = new OAuthArguments();
    arguments.setServiceName("testservice");
    arguments.setUseToken(UseToken.IF_AVAILABLE);
    AccessorInfo info = store.getOAuthAccessor(privateToken, arguments, clientState);
    assertEquals(OAuthParamLocation.URI_QUERY, info.getParamLocation());
    assertEquals(null, info.getConsumer().getKeyName());
    assertEquals("hmac", info.getConsumer().getConsumer().consumerKey);
    assertEquals("hmacsecret", info.getConsumer().getConsumer().consumerSecret);
    assertNull(info.getAccessor().requestToken);
    assertEquals("token", info.getAccessor().accessToken);
    assertEquals("secret", info.getAccessor().tokenSecret);
    
    // Friend views page
    info = store.getOAuthAccessor(socialToken, arguments, clientState);
    assertEquals(OAuthParamLocation.URI_QUERY, info.getParamLocation());
    assertEquals(null, info.getConsumer().getKeyName());
    assertEquals("hmac", info.getConsumer().getConsumer().consumerKey);
    assertEquals("hmacsecret", info.getConsumer().getConsumer().consumerSecret);
    assertNull(info.getAccessor().requestToken);
    assertNull(info.getAccessor().accessToken);
    assertNull(info.getAccessor().tokenSecret);
  }
  
  @Test
  public void testGetOAuthAccessor_fullOAuth_socialPage() throws Exception {
    BasicOAuthStoreConsumerIndex index = new BasicOAuthStoreConsumerIndex();
    index.setGadgetUri(GADGET_URL);
    index.setServiceName("testservice");
    BasicOAuthStoreConsumerKeyAndSecret cks =
        new BasicOAuthStoreConsumerKeyAndSecret("hmac", "hmacsecret", KeyType.HMAC_SYMMETRIC, null);
    backingStore.setConsumerKeyAndSecret(index, cks);
    OAuthArguments arguments = new OAuthArguments();
    arguments.setServiceName("testservice");
    arguments.setUseToken(UseToken.ALWAYS);
    AccessorInfo info = store.getOAuthAccessor(socialToken, arguments, clientState);
    assertEquals(OAuthParamLocation.URI_QUERY, info.getParamLocation());
    assertEquals(null, info.getConsumer().getKeyName());
    assertEquals("hmac", info.getConsumer().getConsumer().consumerKey);
    assertEquals("hmacsecret", info.getConsumer().getConsumer().consumerSecret);
    assertNull(info.getAccessor().requestToken);
    assertNull(info.getAccessor().accessToken);
    assertNull(info.getAccessor().tokenSecret);
  }
  
  @Test
  public void testGetOAuthAccessor_serviceNotFound() throws Exception {
    OAuthArguments arguments = new OAuthArguments();
    arguments.setServiceName("no such service");
    arguments.setUseToken(UseToken.ALWAYS);
    try {
      store.getOAuthAccessor(socialToken, arguments, clientState);
      fail();
    } catch (GadgetException e) {
      // good.
    }
  }
  
  @Test
  public void testGetOAuthAccessor_oauthParamsInBody() throws Exception {
    OAuthArguments arguments = new OAuthArguments();
    arguments.setServiceName("testservice");
    arguments.setUseToken(UseToken.ALWAYS);
    privateToken.setAppUrl("http://www.example.com/body.xml");
    AccessorInfo info = store.getOAuthAccessor(privateToken, arguments, clientState);
    assertEquals(
        FakeOAuthServiceProvider.REQUEST_TOKEN_URL,
        info.getConsumer().getConsumer().serviceProvider.requestTokenURL);
    assertEquals(
        FakeOAuthServiceProvider.APPROVAL_URL,
        info.getConsumer().getConsumer().serviceProvider.userAuthorizationURL);
    assertEquals(
        FakeOAuthServiceProvider.ACCESS_TOKEN_URL,
        info.getConsumer().getConsumer().serviceProvider.accessTokenURL);
    assertEquals(HttpMethod.POST, info.getHttpMethod());
    assertEquals(OAuthParamLocation.POST_BODY, info.getParamLocation());
  }
  
  @Test
  public void testGetOAuthAccessor_oauthParamsInHeader() throws Exception {
    OAuthArguments arguments = new OAuthArguments();
    arguments.setServiceName("testservice");
    arguments.setUseToken(UseToken.ALWAYS);
    privateToken.setAppUrl("http://www.example.com/header.xml");
    AccessorInfo info = store.getOAuthAccessor(privateToken, arguments, clientState);
    assertEquals(
        FakeOAuthServiceProvider.REQUEST_TOKEN_URL,
        info.getConsumer().getConsumer().serviceProvider.requestTokenURL);
    assertEquals(
        FakeOAuthServiceProvider.APPROVAL_URL,
        info.getConsumer().getConsumer().serviceProvider.userAuthorizationURL);
    assertEquals(
        FakeOAuthServiceProvider.ACCESS_TOKEN_URL,
        info.getConsumer().getConsumer().serviceProvider.accessTokenURL);
    assertEquals(HttpMethod.GET, info.getHttpMethod());
    assertEquals(OAuthParamLocation.AUTH_HEADER, info.getParamLocation());
  }
  
  @Test
  public void testAccessTokenFromServerDatabase() throws Exception {
    OAuthArguments arguments = new OAuthArguments();
    arguments.setServiceName("testservice");
    arguments.setUseToken(UseToken.ALWAYS);
    store.storeTokenKeyAndSecret(privateToken, null, arguments, new TokenInfo("access", "secret"));
    
    AccessorInfo info = store.getOAuthAccessor(privateToken, arguments, clientState);
    assertNull(info.getAccessor().requestToken);
    assertEquals("access", info.getAccessor().accessToken);
    assertEquals("secret", info.getAccessor().tokenSecret);
  }
  
  @Test
  public void testAccessTokenFromClient() throws Exception {
    OAuthArguments arguments = new OAuthArguments();
    arguments.setServiceName("testservice");
    arguments.setUseToken(UseToken.ALWAYS);
    store.storeTokenKeyAndSecret(privateToken, null, arguments, new TokenInfo("access", "secret"));
    
    clientState.setAccessToken("clienttoken");
    clientState.setAccessTokenSecret("clienttokensecret");
    
    AccessorInfo info = store.getOAuthAccessor(privateToken, arguments, clientState);
    assertNull(info.getAccessor().requestToken);
    assertEquals("clienttoken", info.getAccessor().accessToken);
    assertEquals("clienttokensecret", info.getAccessor().tokenSecret);
  }
  
  @Test
  public void testRequestTokenFromClientState() throws Exception {
    OAuthArguments arguments = new OAuthArguments();
    arguments.setServiceName("testservice");
    arguments.setUseToken(UseToken.ALWAYS);
    store.storeTokenKeyAndSecret(privateToken, null, arguments, new TokenInfo("access", "secret"));
    
    clientState.setRequestToken("request");
    clientState.setRequestTokenSecret("requestsecret");
    
    AccessorInfo info = store.getOAuthAccessor(privateToken, arguments, clientState);
    assertEquals("request", info.getAccessor().requestToken);
    assertEquals("requestsecret", info.getAccessor().tokenSecret);
    assertNull(info.getAccessor().accessToken);
  }
  
  @Test
  public void testRequestTokenFromClient_preferTokenInStorage() throws Exception {
    OAuthArguments arguments = new OAuthArguments();
    arguments.setServiceName("testservice");
    arguments.setUseToken(UseToken.ALWAYS);
    arguments.setRequestToken("preapproved");
    arguments.setRequestTokenSecret("preapprovedsecret");
    store.storeTokenKeyAndSecret(privateToken, null, arguments, new TokenInfo("access", "secret"));
    
    AccessorInfo info = store.getOAuthAccessor(privateToken, arguments, clientState);
    assertNull(info.getAccessor().requestToken);
    assertEquals("access", info.getAccessor().accessToken);
    assertEquals("secret", info.getAccessor().tokenSecret);
  }
  
  @Test
  public void testRequestTokenFromClient_noTokenInStorage() throws Exception {
    OAuthArguments arguments = new OAuthArguments();
    arguments.setServiceName("testservice");
    arguments.setUseToken(UseToken.ALWAYS);
    arguments.setRequestToken("preapproved");
    arguments.setRequestTokenSecret("preapprovedsecret");
    
    AccessorInfo info = store.getOAuthAccessor(privateToken, arguments, clientState);
    assertNull(info.getAccessor().accessToken);
    assertEquals("preapproved", info.getAccessor().requestToken);
    assertEquals("preapprovedsecret", info.getAccessor().tokenSecret);
  }
  
  @Test
  public void testRemoveToken() throws Exception {
    OAuthArguments arguments = new OAuthArguments();
    arguments.setServiceName("testservice");
    arguments.setUseToken(UseToken.ALWAYS);
    store.storeTokenKeyAndSecret(privateToken, null, arguments, new TokenInfo("access", "secret"));
    
    AccessorInfo info = store.getOAuthAccessor(privateToken, arguments, clientState);
    assertNull(info.getAccessor().requestToken);
    assertEquals("access", info.getAccessor().accessToken);
    assertEquals("secret", info.getAccessor().tokenSecret);
    
    store.removeToken(privateToken, null, arguments);
    
    info = store.getOAuthAccessor(privateToken, arguments, clientState);
    assertNull(info.getAccessor().requestToken);
    assertNull(info.getAccessor().accessToken);
    assertNull(info.getAccessor().tokenSecret);
  }
}
