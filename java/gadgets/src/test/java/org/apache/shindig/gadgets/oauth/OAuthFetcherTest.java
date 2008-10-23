/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.gadgets.oauth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.collect.Lists;

import net.oauth.OAuth;
import net.oauth.OAuth.Parameter;

import org.apache.commons.codec.binary.Base64;
import org.apache.shindig.auth.BasicSecurityToken;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.cache.DefaultCacheProvider;
import org.apache.shindig.common.crypto.BasicBlobCrypter;
import org.apache.shindig.common.util.CharsetUtil;
import org.apache.shindig.gadgets.FakeGadgetSpecFactory;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.RequestSigningException;
import org.apache.shindig.gadgets.http.BasicHttpCache;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.oauth.AccessorInfo.OAuthParamLocation;
import org.apache.shindig.gadgets.oauth.BasicOAuthStoreConsumerKeyAndSecret.KeyType;
import org.apache.shindig.gadgets.oauth.OAuthArguments.UseToken;
import org.apache.shindig.gadgets.oauth.testing.FakeOAuthServiceProvider;
import org.apache.shindig.gadgets.oauth.testing.MakeRequestClient;
import org.apache.shindig.gadgets.oauth.testing.FakeOAuthServiceProvider.TokenPair;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Tests for signing requests.
 */
public class OAuthFetcherTest {

  private OAuthFetcherConfig fetcherConfig;
  private FakeOAuthServiceProvider serviceProvider;
  private BasicOAuthStore base;
  private List<LogRecord> logRecords = Lists.newArrayList();

  public static final String GADGET_URL = "http://www.example.com/gadget.xml";
  public static final String GADGET_URL_NO_KEY = "http://www.example.com/nokey.xml";
  public static final String GADGET_URL_HEADER = "http://www.example.com/header.xml";
  public static final String GADGET_URL_BODY = "http://www.example.com/body.xml";

  @Before
  public void setUp() throws Exception {
    base = new BasicOAuthStore();
    serviceProvider = new FakeOAuthServiceProvider();
    fetcherConfig = new OAuthFetcherConfig(
        new BasicBlobCrypter("abcdefghijklmnop".getBytes()),
        getOAuthStore(base),
        new BasicHttpCache(new DefaultCacheProvider(),10));
    
    Logger logger = Logger.getLogger(OAuthFetcher.class.getName());
    logger.addHandler(new Handler() {
      @Override
      public void close() throws SecurityException {
      }

      @Override
      public void flush() {
      }

      @Override
      public void publish(LogRecord arg0) {
        logRecords.add(arg0);
      }
    });
  }

  /**
   * Builds a nicely populated fake token store.
   */
  public static GadgetOAuthTokenStore getOAuthStore(BasicOAuthStore base) throws GadgetException {
    if (base == null) {
      base = new BasicOAuthStore();
    }
    addValidConsumer(base);
    addInvalidConsumer(base);
    addAuthHeaderConsumer(base);
    addBodyConsumer(base);
    addDefaultKey(base);
    GadgetOAuthTokenStore store = new GadgetOAuthTokenStore(base, new FakeGadgetSpecFactory());
    base.initFromConfigString("{}");
    return store;
  }

  private static void addValidConsumer(BasicOAuthStore base) {
    addConsumer(
        base,
        GADGET_URL,
        FakeGadgetSpecFactory.SERVICE_NAME,
        FakeOAuthServiceProvider.CONSUMER_KEY,
        FakeOAuthServiceProvider.CONSUMER_SECRET);
  }

  private static void addInvalidConsumer(BasicOAuthStore base) {
    addConsumer(
        base,
        GADGET_URL_NO_KEY,
        FakeGadgetSpecFactory.SERVICE_NAME_NO_KEY,
        "garbage_key", "garbage_secret");
  }
  
  private static void addAuthHeaderConsumer(BasicOAuthStore base) {
    addConsumer(
        base,
        GADGET_URL_HEADER,
        FakeGadgetSpecFactory.SERVICE_NAME,
        FakeOAuthServiceProvider.CONSUMER_KEY,
        FakeOAuthServiceProvider.CONSUMER_SECRET);
  }
  
  private static void addBodyConsumer(BasicOAuthStore base) {
    addConsumer(
        base,
        GADGET_URL_BODY,
        FakeGadgetSpecFactory.SERVICE_NAME,
        FakeOAuthServiceProvider.CONSUMER_KEY,
        FakeOAuthServiceProvider.CONSUMER_SECRET);
  }

  private static void addConsumer(
      BasicOAuthStore base,
      String gadgetUrl,
      String serviceName,
      String consumerKey,
      String consumerSecret) {
    BasicOAuthStoreConsumerIndex providerKey = new BasicOAuthStoreConsumerIndex();
    providerKey.setGadgetUri(gadgetUrl);
    providerKey.setServiceName(serviceName);

    BasicOAuthStoreConsumerKeyAndSecret kas = new BasicOAuthStoreConsumerKeyAndSecret(
        consumerKey, consumerSecret, KeyType.HMAC_SYMMETRIC, null);

    base.setConsumerKeyAndSecret(providerKey, kas);
  }
  
  private static void addDefaultKey(BasicOAuthStore base) {
    BasicOAuthStoreConsumerKeyAndSecret defaultKey = new BasicOAuthStoreConsumerKeyAndSecret(
        "signedfetch", FakeOAuthServiceProvider.PRIVATE_KEY_TEXT, KeyType.RSA_PRIVATE, "foo");
    base.setDefaultKey(defaultKey);
  }


  /**
   * Builds gadget token for testing a service with parameters in the query.
   */
  public static SecurityToken getNormalSecurityToken(String owner, String viewer) throws Exception {
    return getSecurityToken(owner, viewer, GADGET_URL);
  }

  /**
   * Builds gadget token for testing services without a key.
   */
  public static SecurityToken getNokeySecurityToken(String owner, String viewer) throws Exception {
    return getSecurityToken(owner, viewer, GADGET_URL_NO_KEY);
  }
  
  /**
   * Builds gadget token for testing a service that wants parameters in a header.
   */
  public static SecurityToken getHeaderSecurityToken(String owner, String viewer) throws Exception {
    return getSecurityToken(owner, viewer, GADGET_URL_HEADER);
  }
  
  /**
   * Builds gadget token for testing a service that wants parameters in the request body.
   */
  public static SecurityToken getBodySecurityToken(String owner, String viewer) throws Exception {
    return getSecurityToken(owner, viewer, GADGET_URL_BODY);
  }

  public static SecurityToken getSecurityToken(String owner, String viewer, String gadget)
      throws Exception {
    return new BasicSecurityToken(owner, viewer, "app", "container.com", gadget, "0");
  }

  @After
  public void tearDown() throws Exception {
  }
  
  private MakeRequestClient makeNonSocialClient(String owner, String viewer, String gadget)
      throws Exception {
    SecurityToken securityToken = getSecurityToken(owner, viewer, gadget);
    return new MakeRequestClient(securityToken, fetcherConfig, serviceProvider,
        FakeGadgetSpecFactory.SERVICE_NAME);
  }
  
  private MakeRequestClient makeSocialOAuthClient(String owner, String viewer, String gadget)
      throws Exception {
    SecurityToken securityToken = getSecurityToken(owner, viewer, gadget);
    MakeRequestClient client = new MakeRequestClient(securityToken, fetcherConfig, serviceProvider,
        FakeGadgetSpecFactory.SERVICE_NAME);
    client.getBaseArgs().setUseToken(UseToken.IF_AVAILABLE);
    return client;
  }
  
  private MakeRequestClient makeSignedFetchClient(String owner, String viewer, String gadget)
      throws Exception {
    SecurityToken securityToken = getSecurityToken(owner, viewer, gadget);
    MakeRequestClient client = new MakeRequestClient(securityToken, fetcherConfig, serviceProvider,
        null);
    client.setBaseArgs(client.makeSignedFetchArguments());
    return client;
  }

  @Test
  public void testOAuthFlow() throws Exception {
    MakeRequestClient client = makeNonSocialClient("owner", "owner", GADGET_URL);
    
    HttpResponse response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("", response.getResponseAsString());
    client.approveToken("user_data=hello-oauth");
    
    response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("User data is hello-oauth", response.getResponseAsString());
    checkEmptyLog();
  }
  
  @Test
  public void testOAuthFlow_tokenReused() throws Exception {
    MakeRequestClient client = makeNonSocialClient("owner", "owner", GADGET_URL);
    
    HttpResponse response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("", response.getResponseAsString());
    client.approveToken("user_data=hello-oauth");
    
    response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("User data is hello-oauth", response.getResponseAsString());
    
    // Check out what happens if the client-side oauth state vanishes.
    MakeRequestClient client2 = makeNonSocialClient("owner", "owner", GADGET_URL);
    response = client2.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("User data is hello-oauth", response.getResponseAsString());     
  }
  
  @Test
  public void testOAuthFlow_unauthUser() throws Exception {
    MakeRequestClient client = makeNonSocialClient(null, null, GADGET_URL);
    HttpResponse response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("", response.getResponseAsString());
    assertEquals(403, response.getHttpStatusCode());
    assertEquals(OAuthError.UNAUTHENTICATED.toString(), response.getMetadata().get("oauthError"));
  }
  
  @Test
  public void testAccessTokenNotUsedForSocialPage() throws Exception {
    MakeRequestClient client = makeNonSocialClient("owner", "owner", GADGET_URL);
    
    HttpResponse response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("", response.getResponseAsString());
    client.approveToken("user_data=hello-oauth");
    
    response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("User data is hello-oauth", response.getResponseAsString());
    
    MakeRequestClient friend = makeNonSocialClient("owner", "friend", GADGET_URL);
    response = friend.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("", response.getResponseAsString());
    assertEquals(403, response.getHttpStatusCode());
    assertEquals(OAuthError.NOT_OWNER.toString(), response.getMetadata().get("oauthError"));
  }
  
  @Test
  public void testParamsInHeader() throws Exception {
    serviceProvider.setParamLocation(OAuthParamLocation.AUTH_HEADER);
    MakeRequestClient client = makeNonSocialClient("owner", "owner", GADGET_URL_HEADER);
    HttpResponse response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("", response.getResponseAsString());
    client.approveToken("user_data=hello-oauth");
    
    response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("User data is hello-oauth", response.getResponseAsString());

    String aznHeader = response.getHeader(FakeOAuthServiceProvider.AUTHZ_ECHO_HEADER);
    assertNotNull(aznHeader);
    assertTrue("azn header: " + aznHeader, aznHeader.indexOf("OAuth") != -1);
  }
  
  @Test
  public void testParamsInBody() throws Exception {
    serviceProvider.setParamLocation(OAuthParamLocation.POST_BODY);
    MakeRequestClient client = makeNonSocialClient("owner", "owner", GADGET_URL_BODY);
    HttpResponse response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("", response.getResponseAsString());
    client.approveToken("user_data=hello-oauth");
    
    response = client.sendFormPost(FakeOAuthServiceProvider.RESOURCE_URL, "");
    assertEquals("User data is hello-oauth", response.getResponseAsString());
    
    String echoedBody = response.getHeader(FakeOAuthServiceProvider.BODY_ECHO_HEADER);
    assertNotNull(echoedBody);
    assertTrue("body: " + echoedBody, echoedBody.indexOf("oauth_consumer_key=") != -1);
  }
  
  @Test
  public void testParamsInBody_withExtraParams() throws Exception {
    serviceProvider.setParamLocation(OAuthParamLocation.POST_BODY);
    MakeRequestClient client = makeNonSocialClient("owner", "owner", GADGET_URL_BODY);
    HttpResponse response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("", response.getResponseAsString());
    client.approveToken("user_data=hello-oauth");
    
    response = client.sendFormPost(FakeOAuthServiceProvider.RESOURCE_URL, "foo=bar&foo=baz");
    assertEquals("User data is hello-oauth", response.getResponseAsString());
    
    String echoedBody = response.getHeader(FakeOAuthServiceProvider.BODY_ECHO_HEADER);
    assertNotNull(echoedBody);
    assertTrue("body: " + echoedBody, echoedBody.indexOf("oauth_consumer_key=") != -1);
    assertTrue("body: " + echoedBody, echoedBody.indexOf("foo=bar&foo=baz") != -1);    
  }
  
  @Test
  public void testParamsInBody_forGetRequest() throws Exception {
    serviceProvider.setParamLocation(OAuthParamLocation.POST_BODY);
    serviceProvider.addParamLocation(OAuthParamLocation.AUTH_HEADER);

    MakeRequestClient client = makeNonSocialClient("owner", "owner", GADGET_URL_BODY);
    HttpResponse response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("", response.getResponseAsString());
    client.approveToken("user_data=hello-oauth");
    
    response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("User data is hello-oauth", response.getResponseAsString());
    
    String aznHeader = response.getHeader(FakeOAuthServiceProvider.AUTHZ_ECHO_HEADER);
    assertNotNull(aznHeader);
    assertTrue("azn header: " + aznHeader, aznHeader.indexOf("OAuth") != -1);
  }
  
  @Test
  public void testParamsInBody_forGetRequestStrictSp() throws Exception {
    serviceProvider.setParamLocation(OAuthParamLocation.POST_BODY);

    MakeRequestClient client = makeNonSocialClient("owner", "owner", GADGET_URL_BODY);
    HttpResponse response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("", response.getResponseAsString());
    client.approveToken("user_data=hello-oauth");
    
    response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("", response.getResponseAsString());
    assertNotNull(response.getMetadata().get("oauthApprovalUrl"));
  }
 
  @Test
  public void testRevokedAccessToken() throws Exception {
    MakeRequestClient client = makeNonSocialClient("owner", "owner", GADGET_URL);
    
    HttpResponse response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("", response.getResponseAsString());
    client.approveToken("user_data=hello-oauth");
    
    response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL + "?cachebust=1");
    assertEquals("User data is hello-oauth", response.getResponseAsString());

    serviceProvider.revokeAllAccessTokens();
    
    response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL + "?cachebust=2");
    assertEquals("", response.getResponseAsString());
    assertNotNull(response.getMetadata().get("oauthApprovalUrl"));
    
    client.approveToken("user_data=reapproved");

    response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL + "?cachebust=3");
    assertEquals("User data is reapproved", response.getResponseAsString());
  }

  @Test
  public void testError401() throws Exception {
    serviceProvider.setVagueErrors(true);
    MakeRequestClient client = makeNonSocialClient("owner", "owner", GADGET_URL);
    
    HttpResponse response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("", response.getResponseAsString());
    client.approveToken("user_data=hello-oauth");
    
    response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL + "?cachebust=1");
    assertEquals("User data is hello-oauth", response.getResponseAsString());

    serviceProvider.revokeAllAccessTokens();
    
    response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL + "?cachebust=2");
    checkLogContains("GET /data?cachebust=2");
    checkLogContains("HTTP/1.1 401");
    assertEquals("", response.getResponseAsString());
    assertNotNull(response.getMetadata().get("oauthApprovalUrl"));
    
    client.approveToken("user_data=reapproved");

    response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL + "?cachebust=3");
    assertEquals("User data is reapproved", response.getResponseAsString());
  }

  @Test
  public void testUnknownConsumerKey() throws Exception {
    SecurityToken securityToken = getSecurityToken("owner", "owner", GADGET_URL_NO_KEY);
    MakeRequestClient client = new MakeRequestClient(securityToken, fetcherConfig, serviceProvider,
        FakeGadgetSpecFactory.SERVICE_NAME_NO_KEY);    
    
    HttpResponse response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("", response.getResponseAsString());
    
    Map<String, String> metadata = response.getMetadata();
    assertNotNull(metadata);
    assertEquals("consumer_key_unknown", metadata.get("oauthError"));
    assertEquals(
        "invalid consumer: garbage_key",
        metadata.get("oauthErrorText"));
  }

  @Test
  public void testError403() throws Exception {
    serviceProvider.setVagueErrors(true);
    SecurityToken securityToken = getSecurityToken("owner", "owner", GADGET_URL_NO_KEY);
    MakeRequestClient client = new MakeRequestClient(securityToken, fetcherConfig, serviceProvider,
        FakeGadgetSpecFactory.SERVICE_NAME_NO_KEY);    
    
    HttpResponse response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("", response.getResponseAsString());
    Map<String, String> metadata = response.getMetadata();
    assertNotNull(metadata);
    assertEquals("403", metadata.get("oauthError"));
    assertNull(metadata.get("oauthErrorText"));
    checkLogContains("HTTP/1.1 403");
    checkLogContains("GET /request");
    checkLogContains("some vague error");
  }
  
  @Test
  public void testError404() throws Exception {
    MakeRequestClient client = makeNonSocialClient("owner", "owner", GADGET_URL);
    
    HttpResponse response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("", response.getResponseAsString());
    client.approveToken("user_data=hello-oauth");
    
    response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL + "?cachebust=1");
    assertEquals("User data is hello-oauth", response.getResponseAsString());
    
    response = client.sendGet(FakeOAuthServiceProvider.NOT_FOUND_URL);
    assertEquals("not found", response.getResponseAsString());
    assertEquals(404, response.getHttpStatusCode());
    
    response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL + "?cachebust=3");
    assertEquals("User data is hello-oauth", response.getResponseAsString());
  }

  @Test
  public void testConsumerThrottled() throws Exception {
    assertEquals(0, serviceProvider.getRequestTokenCount());
    assertEquals(0, serviceProvider.getAccessTokenCount());
    assertEquals(0, serviceProvider.getResourceAccessCount());

    MakeRequestClient client = makeNonSocialClient("owner", "owner", GADGET_URL);
    
    HttpResponse response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("", response.getResponseAsString());
 
    assertEquals(1, serviceProvider.getRequestTokenCount());
    assertEquals(0, serviceProvider.getAccessTokenCount());
    assertEquals(0, serviceProvider.getResourceAccessCount());

    client.approveToken("user_data=hello-oauth");    
    response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("User data is hello-oauth", response.getResponseAsString());

    assertEquals(1, serviceProvider.getRequestTokenCount());
    assertEquals(1, serviceProvider.getAccessTokenCount());
    assertEquals(1, serviceProvider.getResourceAccessCount());
    
    response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL + "?cachebust=1");
    assertEquals("User data is hello-oauth", response.getResponseAsString());

    assertEquals(1, serviceProvider.getRequestTokenCount());
    assertEquals(1, serviceProvider.getAccessTokenCount());
    assertEquals(2, serviceProvider.getResourceAccessCount());

    serviceProvider.setConsumersThrottled(true);

    response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL + "?cachebust=2");
    assertEquals("", response.getResponseAsString());
    Map<String, String> metadata = response.getMetadata();
    assertNotNull(metadata);
    assertEquals("consumer_key_refused", metadata.get("oauthError"));
    assertEquals(
        "exceeded quota",
        metadata.get("oauthErrorText"));

    assertEquals(1, serviceProvider.getRequestTokenCount());
    assertEquals(1, serviceProvider.getAccessTokenCount());
    assertEquals(3, serviceProvider.getResourceAccessCount());

    serviceProvider.setConsumersThrottled(false);

    response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL + "?cachebust=3");
    assertEquals("User data is hello-oauth", response.getResponseAsString());

    assertEquals(1, serviceProvider.getRequestTokenCount());
    assertEquals(1, serviceProvider.getAccessTokenCount());
    assertEquals(4, serviceProvider.getResourceAccessCount());
  }

  @Test
  public void testSocialOAuth_tokenRevoked() throws Exception {
    MakeRequestClient client = makeNonSocialClient("owner", "owner", GADGET_URL);
    
    HttpResponse response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("", response.getResponseAsString());

    client.approveToken("user_data=hello-oauth");    
    response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("User data is hello-oauth", response.getResponseAsString());
    
    serviceProvider.revokeAllAccessTokens();
    
    assertEquals(0, base.getAccessTokenRemoveCount());
    client = makeSocialOAuthClient("owner", "owner", GADGET_URL);
    response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL + "?cb=1");
    assertEquals("", response.getResponseAsString());
    assertEquals(1, base.getAccessTokenRemoveCount());
  }
  
  @Test
  public void testWrongServiceName() throws Exception {
    SecurityToken securityToken = getSecurityToken("owner", "owner", GADGET_URL);
    MakeRequestClient client = new MakeRequestClient(securityToken, fetcherConfig, serviceProvider,
        "nosuchservice");    
    
    HttpResponse response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    Map<String, String> metadata = response.getMetadata();
    assertNull(metadata.get("oauthApprovalUrl"));
    assertEquals("BAD_OAUTH_CONFIGURATION", metadata.get("oauthError"));
    String errorText = metadata.get("oauthErrorText");
    assertTrue(errorText, errorText.startsWith(
        "Spec for gadget http://www.example.com/gadget.xml does not contain OAuth service " +
        "nosuchservice.  Known services: testservice"));
  }

  @Test
  public void testPreapprovedToken() throws Exception {
    MakeRequestClient client = makeNonSocialClient("owner", "owner", GADGET_URL);
    TokenPair reqToken = serviceProvider.getPreapprovedToken("preapproved");
    client.getBaseArgs().setRequestToken(reqToken.token);
    client.getBaseArgs().setRequestTokenSecret(reqToken.secret);

    HttpResponse response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("User data is preapproved", response.getResponseAsString());
    
    assertEquals(0, serviceProvider.getRequestTokenCount());
    assertEquals(1, serviceProvider.getAccessTokenCount());
    assertEquals(1, serviceProvider.getResourceAccessCount());

    response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL + "?cachebust=1");
    assertEquals("User data is preapproved", response.getResponseAsString());

    assertEquals(0, serviceProvider.getRequestTokenCount());
    assertEquals(1, serviceProvider.getAccessTokenCount());
    assertEquals(2, serviceProvider.getResourceAccessCount());

    response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL + "?cachebust=2");
    assertEquals("User data is preapproved", response.getResponseAsString());
    assertEquals(0, serviceProvider.getRequestTokenCount());
    assertEquals(1, serviceProvider.getAccessTokenCount());
    assertEquals(3, serviceProvider.getResourceAccessCount());
  }

  @Test
  public void testPreapprovedToken_invalid() throws Exception {
    MakeRequestClient client = makeNonSocialClient("owner", "owner", GADGET_URL);
    client.getBaseArgs().setRequestToken("garbage");
    client.getBaseArgs().setRequestTokenSecret("garbage");

    HttpResponse response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("", response.getResponseAsString());
    assertEquals(1, serviceProvider.getRequestTokenCount());
    assertEquals(1, serviceProvider.getAccessTokenCount());
    assertEquals(0, serviceProvider.getResourceAccessCount());
    
    client.approveToken("user_data=hello-oauth");
    
    response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("User data is hello-oauth", response.getResponseAsString());
    
    assertEquals(1, serviceProvider.getRequestTokenCount());
    assertEquals(2, serviceProvider.getAccessTokenCount());
    assertEquals(1, serviceProvider.getResourceAccessCount());
  }
  
  @Test
  public void testPreapprovedToken_notUsedIfAccessTokenExists() throws Exception {
    MakeRequestClient client = makeNonSocialClient("owner", "owner", GADGET_URL);
    TokenPair reqToken = serviceProvider.getPreapprovedToken("preapproved");
    client.getBaseArgs().setRequestToken(reqToken.token);
    client.getBaseArgs().setRequestTokenSecret(reqToken.secret);

    HttpResponse response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("User data is preapproved", response.getResponseAsString());
    
    assertEquals(0, serviceProvider.getRequestTokenCount());
    assertEquals(1, serviceProvider.getAccessTokenCount());
    assertEquals(1, serviceProvider.getResourceAccessCount());

    MakeRequestClient client2 = makeNonSocialClient("owner", "owner", GADGET_URL);

    response = client2.sendGet(FakeOAuthServiceProvider.RESOURCE_URL + "?cachebust=1");
    assertEquals("User data is preapproved", response.getResponseAsString());

    assertEquals(0, serviceProvider.getRequestTokenCount());
    assertEquals(1, serviceProvider.getAccessTokenCount());
    assertEquals(2, serviceProvider.getResourceAccessCount());
  }

  @Test
  public void testCachedResponse() throws Exception {
    assertEquals(0, serviceProvider.getRequestTokenCount());
    assertEquals(0, serviceProvider.getAccessTokenCount());
    assertEquals(0, serviceProvider.getResourceAccessCount());

    MakeRequestClient client = makeNonSocialClient("owner", "owner", GADGET_URL);
    
    HttpResponse response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("", response.getResponseAsString());
    client.approveToken("user_data=hello-oauth");
    
    response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("User data is hello-oauth", response.getResponseAsString());
    
    assertEquals(1, serviceProvider.getRequestTokenCount());
    assertEquals(1, serviceProvider.getAccessTokenCount());
    assertEquals(1, serviceProvider.getResourceAccessCount());

    response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("User data is hello-oauth", response.getResponseAsString());

    assertEquals(1, serviceProvider.getRequestTokenCount());
    assertEquals(1, serviceProvider.getAccessTokenCount());
    assertEquals(1, serviceProvider.getResourceAccessCount());
  }
  
  @Test
  public void testSignedFetchParametersSet() throws Exception {
    MakeRequestClient client = makeSignedFetchClient("o", "v", "http://www.example.com/app");
    HttpResponse resp = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    List<Parameter> queryParams = OAuth.decodeForm(resp.getResponseAsString());
    assertTrue(contains(queryParams, "opensocial_owner_id", "o"));
    assertTrue(contains(queryParams, "opensocial_viewer_id", "v"));
    assertTrue(contains(queryParams, "opensocial_app_id", "app"));
    assertTrue(contains(queryParams, OAuth.OAUTH_CONSUMER_KEY, "signedfetch"));
    assertTrue(contains(queryParams, "xoauth_signature_publickey", "foo"));
  }
  
  @Test
  public void testPostBinaryData() throws Exception {
    byte[] raw = new byte[] { 0, 1, 2, 3, 4, 5 };
    MakeRequestClient client = makeSignedFetchClient("o", "v", "http://www.example.com/app");
    HttpResponse resp = client.sendRawPost(FakeOAuthServiceProvider.RESOURCE_URL, null, raw);
    List<Parameter> queryParams = OAuth.decodeForm(resp.getResponseAsString());
    assertTrue(contains(queryParams, "opensocial_owner_id", "o"));
    assertTrue(contains(queryParams, OAuth.OAUTH_CONSUMER_KEY, "signedfetch"));
    String echoed = resp.getHeader(FakeOAuthServiceProvider.RAW_BODY_ECHO_HEADER);
    byte[] echoedBytes = Base64.decodeBase64(CharsetUtil.getUtf8Bytes(echoed));
    assertTrue(Arrays.equals(raw, echoedBytes));
  }

  @Test
  public void testPostWeirdContentType() throws Exception {
    byte[] raw = new byte[] { 0, 1, 2, 3, 4, 5 };
    MakeRequestClient client = makeSignedFetchClient("o", "v", "http://www.example.com/app");
    HttpResponse resp = client.sendRawPost(FakeOAuthServiceProvider.RESOURCE_URL,
        "funky-content", raw);
    List<Parameter> queryParams = OAuth.decodeForm(resp.getResponseAsString());
    assertTrue(contains(queryParams, "opensocial_owner_id", "o"));
    assertTrue(contains(queryParams, OAuth.OAUTH_CONSUMER_KEY, "signedfetch"));
    String echoed = resp.getHeader(FakeOAuthServiceProvider.RAW_BODY_ECHO_HEADER);
    byte[] echoedBytes = Base64.decodeBase64(CharsetUtil.getUtf8Bytes(echoed));
    assertTrue(Arrays.equals(raw, echoedBytes));
  }

  @Test
  public void testSignedFetch_error401() throws Exception {
    assertEquals(0, base.getAccessTokenRemoveCount());
    serviceProvider.setConsumersThrottled(true);
    serviceProvider.setVagueErrors(true);
    MakeRequestClient client = makeSignedFetchClient("o", "v", "http://www.example.com/app");

    HttpResponse response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals(0, base.getAccessTokenRemoveCount());
  }
  
  @Test
  public void testSignedFetch_unnamedConsumerKey() throws Exception {
    BasicOAuthStoreConsumerKeyAndSecret defaultKey = new BasicOAuthStoreConsumerKeyAndSecret(
        null, FakeOAuthServiceProvider.PRIVATE_KEY_TEXT, KeyType.RSA_PRIVATE, "foo");
    base.setDefaultKey(defaultKey);
    MakeRequestClient client = makeSignedFetchClient("o", "v", "http://www.example.com/app");
    HttpResponse resp = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    List<Parameter> queryParams = OAuth.decodeForm(resp.getResponseAsString());
    assertTrue(contains(queryParams, "opensocial_owner_id", "o"));
    assertTrue(contains(queryParams, "opensocial_viewer_id", "v"));
    assertTrue(contains(queryParams, "opensocial_app_id", "app"));
    assertTrue(contains(queryParams, OAuth.OAUTH_CONSUMER_KEY, "container.com"));
    assertTrue(contains(queryParams, "xoauth_signature_publickey", "foo"));
  }
  
  @Test
  public void testSignedFetch_extraQueryParameters() throws Exception {
    MakeRequestClient client = makeSignedFetchClient("o", "v", "http://www.example.com/app");
    HttpResponse resp = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL + "?foo=bar&foo=baz");
    List<Parameter> queryParams = OAuth.decodeForm(resp.getResponseAsString());
    assertTrue(contains(queryParams, "opensocial_owner_id", "o"));
    assertTrue(contains(queryParams, "opensocial_viewer_id", "v"));
    assertTrue(contains(queryParams, "opensocial_app_id", "app"));
    assertTrue(contains(queryParams, OAuth.OAUTH_CONSUMER_KEY, "signedfetch"));
    assertTrue(contains(queryParams, "xoauth_signature_publickey", "foo"));
  }
  
  @Test
  public void testNoSignViewer() throws Exception {
    MakeRequestClient client = makeSignedFetchClient("o", "v", "http://www.example.com/app");
    client.getBaseArgs().setSignViewer(false);
    HttpResponse resp = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    List<Parameter> queryParams = OAuth.decodeForm(resp.getResponseAsString());
    assertTrue(contains(queryParams, "opensocial_owner_id", "o"));
    assertFalse(contains(queryParams, "opensocial_viewer_id", "v"));
  }
  
  @Test
  public void testNoSignOwner() throws Exception {
    MakeRequestClient client = makeSignedFetchClient("o", "v", "http://www.example.com/app");
    client.getBaseArgs().setSignOwner(false);
    HttpResponse resp = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    List<Parameter> queryParams = OAuth.decodeForm(resp.getResponseAsString());
    assertFalse(contains(queryParams, "opensocial_owner_id", "o"));
    assertTrue(contains(queryParams, "opensocial_viewer_id", "v"));
  }

  @Test
  public void testCacheHit() throws Exception {
    MakeRequestClient client = makeSignedFetchClient("o", "v", "http://www.example.com/app");
    client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals(1, serviceProvider.getResourceAccessCount());

    client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals(1, serviceProvider.getResourceAccessCount());
  }

  @Test
  public void testCacheMiss_noOwner() throws Exception {
    MakeRequestClient client = makeSignedFetchClient("o", "v", "http://www.example.com/app");
    client.getBaseArgs().setSignOwner(false);
    client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals(1, serviceProvider.getResourceAccessCount());
    
    MakeRequestClient client2 = makeSignedFetchClient("o", "v", "http://www.example.com/app");
    client2.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals(2, serviceProvider.getResourceAccessCount());
  }
  
  @Test
  public void testCacheHit_ownerOnly() throws Exception {
    MakeRequestClient client = makeSignedFetchClient("o", "v1", "http://www.example.com/app");
    client.getBaseArgs().setSignViewer(false);
    client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals(1, serviceProvider.getResourceAccessCount());
    
    MakeRequestClient client2 = makeSignedFetchClient("o", "v2", "http://www.example.com/app");
    client2.getBaseArgs().setSignViewer(false);
    client2.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals(1, serviceProvider.getResourceAccessCount());
  }
  
  @Test
  public void testCacheMiss_bypassCache() throws Exception {
    MakeRequestClient client = makeSignedFetchClient("o", "v1", "http://www.example.com/app");
    client.getBaseArgs().setSignViewer(false);
    client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals(1, serviceProvider.getResourceAccessCount());
    
    MakeRequestClient client2 = makeSignedFetchClient("o", "v", "http://www.example.com/app");
    client2.setIgnoreCache(true);
    client2.getBaseArgs().setSignViewer(false);
    client2.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals(2, serviceProvider.getResourceAccessCount());
  }
  
  @Test(expected = RequestSigningException.class)
  public void testTrickyParametersInQuery() throws Exception {
    MakeRequestClient client = makeSignedFetchClient("o", "v", "http://www.example.com/app");
    String tricky = "%6fpensocial_owner_id=gotcha";
    client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL + "?" + tricky);
  }
  
  @Test(expected = RequestSigningException.class)
  public void testTrickyParametersInBody() throws Exception {
    MakeRequestClient client = makeSignedFetchClient("o", "v", "http://www.example.com/app");
    String tricky = "%6fpensocial_owner_id=gotcha";
    client.sendFormPost(FakeOAuthServiceProvider.RESOURCE_URL, tricky);
  }
  
  @Test
  public void testGetNoQuery() throws Exception {
    MakeRequestClient client = makeSignedFetchClient("o", "v", "http://www.example.com/app");
    HttpResponse resp = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    List<Parameter> queryParams = OAuth.decodeForm(resp.getResponseAsString());
    assertTrue(contains(queryParams, "opensocial_owner_id", "o"));
    assertTrue(contains(queryParams, "opensocial_viewer_id", "v"));
  }
  
  @Test
  public void testGetWithQuery() throws Exception {
    MakeRequestClient client = makeSignedFetchClient("o", "v", "http://www.example.com/app");
    HttpResponse resp = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL + "?a=b");
    List<Parameter> queryParams = OAuth.decodeForm(resp.getResponseAsString());
    assertTrue(contains(queryParams, "a", "b"));
  }

  @Test
  public void testGetWithQueryMultiParam() throws Exception {
    MakeRequestClient client = makeSignedFetchClient("o", "v", "http://www.example.com/app");
    HttpResponse resp = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL + "?a=b&a=c");
    List<Parameter> queryParams = OAuth.decodeForm(resp.getResponseAsString());
    assertTrue(contains(queryParams, "a", "b"));
    assertTrue(contains(queryParams, "a", "c"));
  }

  @Test
  public void testValidParameterCharacters() throws Exception {
    MakeRequestClient client = makeSignedFetchClient("o", "v", "http://www.example.com/app");
    String weird = "~!@$*()-_[]:,./";
    HttpResponse resp = client.sendGet(
        FakeOAuthServiceProvider.RESOURCE_URL + "?" + weird + "=foo");
    List<Parameter> queryParams = OAuth.decodeForm(resp.getResponseAsString());
    assertTrue(contains(queryParams, weird, "foo"));
  }
  

  @Test
  public void testPostNoQueryNoData() throws Exception {
    MakeRequestClient client = makeSignedFetchClient("o", "v", "http://www.example.com/app");
    HttpResponse resp = client.sendFormPost(FakeOAuthServiceProvider.RESOURCE_URL, null);
    List<Parameter> queryParams = OAuth.decodeForm(resp.getResponseAsString());
    assertTrue(contains(queryParams, "opensocial_owner_id", "o"));
    assertEquals("", resp.getHeader(FakeOAuthServiceProvider.BODY_ECHO_HEADER));
  }

  @Test
  public void testPostWithQueryNoData() throws Exception {
    MakeRequestClient client = makeSignedFetchClient("o", "v", "http://www.example.com/app");
    HttpResponse resp = client.sendFormPost(
        FakeOAuthServiceProvider.RESOURCE_URL + "?name=value", null);
    List<Parameter> queryParams = OAuth.decodeForm(resp.getResponseAsString());
    assertTrue(contains(queryParams, "name", "value"));
    assertEquals("", resp.getHeader(FakeOAuthServiceProvider.BODY_ECHO_HEADER));
  }

  @Test
  public void testPostNoQueryWithData() throws Exception {
    MakeRequestClient client = makeSignedFetchClient("o", "v", "http://www.example.com/app");
    HttpResponse resp = client.sendFormPost(
        FakeOAuthServiceProvider.RESOURCE_URL, "name=value");
    List<Parameter> queryParams = OAuth.decodeForm(resp.getResponseAsString());
    assertFalse(contains(queryParams, "name", "value"));
    assertEquals("name=value", resp.getHeader(FakeOAuthServiceProvider.BODY_ECHO_HEADER));
  }

  @Test
  public void testPostWithQueryWithData() throws Exception {
    MakeRequestClient client = makeSignedFetchClient("o", "v", "http://www.example.com/app");
    HttpResponse resp = client.sendFormPost(
        FakeOAuthServiceProvider.RESOURCE_URL + "?queryName=queryValue", "name=value");
    List<Parameter> queryParams = OAuth.decodeForm(resp.getResponseAsString());
    assertTrue(contains(queryParams, "queryName", "queryValue"));
    assertEquals("name=value", resp.getHeader(FakeOAuthServiceProvider.BODY_ECHO_HEADER));
  }

  @Test(expected = RequestSigningException.class)
  public void testStripOpenSocialParamsFromQuery() throws Exception {
    MakeRequestClient client = makeSignedFetchClient("o", "v", "http://www.example.com/app");
    client.sendFormPost(FakeOAuthServiceProvider.RESOURCE_URL + "?opensocial_foo=bar", null);
  }

  @Test(expected = RequestSigningException.class)
  public void testStripOAuthParamsFromQuery() throws Exception {
    MakeRequestClient client = makeSignedFetchClient("o", "v", "http://www.example.com/app");
    client.sendFormPost(FakeOAuthServiceProvider.RESOURCE_URL + "?oauth_foo=bar", "name=value");
  }

  @Test(expected = RequestSigningException.class)
  public void testStripOpenSocialParamsFromBody() throws Exception {
    MakeRequestClient client = makeSignedFetchClient("o", "v", "http://www.example.com/app");
    client.sendFormPost(FakeOAuthServiceProvider.RESOURCE_URL, "opensocial_foo=bar");
  }

  @Test(expected = RequestSigningException.class)
  public void testStripOAuthParamsFromBody() throws Exception {
    MakeRequestClient client = makeSignedFetchClient("o", "v", "http://www.example.com/app");
    client.sendFormPost(FakeOAuthServiceProvider.RESOURCE_URL, "oauth_foo=bar");
  }

  // Checks whether the given parameter list contains the specified
  // key/value pair
  private boolean contains(List<Parameter> params, String key, String value) {
    for (Parameter p : params) {
      if (p.getKey().equals(key) && p.getValue().equals(value)) {
        return true;
      }
    }
    return false;
  }

  private String getLogText() {
    StringBuilder logText = new StringBuilder();
    for (LogRecord record : logRecords) {
      logText.append(record.getMessage());
    }
    return logText.toString();
  }
  
  private void checkLogContains(String text) {
    String logText = getLogText();
    if (!logText.contains(text)) {
      fail("Should have logged '" + text + "', instead got " + logText);
    }
  }
  
  private void checkEmptyLog() {
    assertEquals("", getLogText());
  }
} 
