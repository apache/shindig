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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.net.URI;
import java.util.Map;

import org.apache.shindig.common.BasicSecurityToken;
import org.apache.shindig.common.SecurityToken;
import org.apache.shindig.common.crypto.BasicBlobCrypter;
import org.apache.shindig.common.crypto.BlobCrypter;
import org.apache.shindig.gadgets.FakeGadgetSpecFactory;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.BasicHttpCache;
import org.apache.shindig.gadgets.http.HttpCache;
import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.oauth.FakeOAuthServiceProvider.TokenPair;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Primitive test of the main code paths in OAuthFetcher.
 *
 * This is a fairly crappy regression test, so if you find yourself wanting
 * to modify this code, you should probably write additional test cases first.
 */
public class OAuthFetcherTest {

  private GadgetOAuthTokenStore tokenStore;
  private BlobCrypter blobCrypter;
  private FakeOAuthServiceProvider serviceProvider;
  private HttpCache cache;

  public static final String GADGET_URL = "http://www.example.com/gadget.xml";
  public static final String GADGET_URL_NO_KEY =
      "http://www.example.com/nokey.xml";

  @Before
  public void setUp() throws Exception {
    serviceProvider = new FakeOAuthServiceProvider();
    tokenStore = getOAuthStore();
    blobCrypter = new BasicBlobCrypter("abcdefghijklmnop".getBytes());
    cache = new BasicHttpCache(10);
  }

  /**
   * Builds a nicely populated fake token store.
   */
  public static GadgetOAuthTokenStore getOAuthStore() throws GadgetException {
    BasicOAuthStore base = new BasicOAuthStore();
    addValidConsumer(base);
    addInvalidConsumer(base);
    BasicGadgetOAuthTokenStore store = new BasicGadgetOAuthTokenStore(base,
        new FakeGadgetSpecFactory());
    store.initFromConfigString("{}");
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

  private static void addConsumer(
      BasicOAuthStore base,
      String gadgetUrl,
      String serviceName,
      String consumerKey,
      String consumerSecret) {
    OAuthStore.ProviderKey providerKey = new OAuthStore.ProviderKey();
    providerKey.setGadgetUri(gadgetUrl);
    providerKey.setServiceName(serviceName);

    OAuthStore.ConsumerKeyAndSecret kas = new OAuthStore.ConsumerKeyAndSecret(
        consumerKey, consumerSecret, OAuthStore.KeyType.HMAC_SYMMETRIC);

    base.setOAuthConsumerKeyAndSecret(providerKey, kas);
  }

  /**
   * Builds a nicely populated gadget token.
   */
  public static SecurityToken getSecurityToken(String owner, String viewer)
      throws Exception {
    return new BasicSecurityToken(owner, viewer, "app", "container.com",
        GADGET_URL, "0");
  }

  /**
   * Builds a nicely populated gadget token.
   */
  public static SecurityToken getNokeySecurityToken(String owner, String viewer)
      throws Exception {
    return new BasicSecurityToken(owner, viewer, "app", "container.com",
        GADGET_URL_NO_KEY, "0");
  }

  @After
  public void tearDown() throws Exception {
  }

  @SuppressWarnings("unused")
  public HttpFetcher getFetcher(SecurityToken authToken,
      OAuthRequestParams params) throws GadgetException {
    OAuthFetcher fetcher = new OAuthFetcher(
        tokenStore, blobCrypter, serviceProvider, authToken, params, cache);
    return fetcher;
  }

  @Test
  public void testOAuthFlow() throws Exception {
    HttpFetcher fetcher;
    HttpRequest request;
    HttpResponse response;

    fetcher = getFetcher(
        getSecurityToken("owner", "owner"),
        new OAuthRequestParams(FakeGadgetSpecFactory.SERVICE_NAME, null, null,
            false));
    request = new HttpRequest(
        new URI(FakeOAuthServiceProvider.RESOURCE_URL));
    response = fetcher.fetch(request);
    String clientState = response.getMetadata().get("oauthState");
    assertNotNull(clientState);
    String approvalUrl = response.getMetadata().get("oauthApprovalUrl");
    assertNotNull(approvalUrl);

    serviceProvider.browserVisit(approvalUrl + "&user_data=hello-oauth");

    fetcher = getFetcher(
        getSecurityToken("owner", "owner"),
        new OAuthRequestParams(FakeGadgetSpecFactory.SERVICE_NAME, null,
            clientState, false));
    request = new HttpRequest(
        new URI(FakeOAuthServiceProvider.RESOURCE_URL));
    response = fetcher.fetch(request);
    assertEquals("User data is hello-oauth", response.getResponseAsString());

    fetcher = getFetcher(
        getSecurityToken("owner", "somebody else"),
        new OAuthRequestParams(FakeGadgetSpecFactory.SERVICE_NAME, null, null,
            false));
    request = new HttpRequest(
        new URI(FakeOAuthServiceProvider.RESOURCE_URL));
    response = fetcher.fetch(request);
    assertEquals("User data is hello-oauth", response.getResponseAsString());

    fetcher = getFetcher(
        getSecurityToken("somebody else", "somebody else"),
        new OAuthRequestParams(FakeGadgetSpecFactory.SERVICE_NAME, null, null,
            false));
    request = new HttpRequest(
        new URI(FakeOAuthServiceProvider.RESOURCE_URL));
    response = fetcher.fetch(request);
    clientState = response.getMetadata().get("oauthState");
    assertNotNull(clientState);
    approvalUrl = response.getMetadata().get("oauthApprovalUrl");
    assertNotNull(approvalUrl);

    serviceProvider.browserVisit(approvalUrl + "&user_data=somebody%20else");

    fetcher = getFetcher(
        getSecurityToken("somebody else", "somebody else"),
        new OAuthRequestParams(FakeGadgetSpecFactory.SERVICE_NAME, null,
            clientState, false));
    request = new HttpRequest(
        new URI(FakeOAuthServiceProvider.RESOURCE_URL));
    response = fetcher.fetch(request);
    assertEquals("User data is somebody else", response.getResponseAsString());
  }

  @Test
  public void testRevokedAccessToken() throws Exception {
    HttpFetcher fetcher;
    HttpRequest request;
    HttpResponse response;

    fetcher = getFetcher(
        getSecurityToken("owner", "owner"),
        new OAuthRequestParams(FakeGadgetSpecFactory.SERVICE_NAME, null, null,
            false));
    request = new HttpRequest(
        new URI(FakeOAuthServiceProvider.RESOURCE_URL));
    response = fetcher.fetch(request);
    String clientState = response.getMetadata().get("oauthState");
    assertNotNull(clientState);
    String approvalUrl = response.getMetadata().get("oauthApprovalUrl");
    assertNotNull(approvalUrl);

    serviceProvider.browserVisit(approvalUrl + "&user_data=hello-oauth");

    fetcher = getFetcher(
        getSecurityToken("owner", "owner"),
        new OAuthRequestParams(FakeGadgetSpecFactory.SERVICE_NAME, null,
            clientState, false));
    request = new HttpRequest(
        new URI(FakeOAuthServiceProvider.RESOURCE_URL));
    response = fetcher.fetch(request);
    assertEquals("User data is hello-oauth", response.getResponseAsString());

    serviceProvider.revokeAllAccessTokens();

    fetcher = getFetcher(
        getSecurityToken("owner", "owner"),
        new OAuthRequestParams(FakeGadgetSpecFactory.SERVICE_NAME, null,
            clientState, false));
    request = new HttpRequest(
        new URI(FakeOAuthServiceProvider.RESOURCE_URL));
    request.getOptions().ignoreCache = true;
    response = fetcher.fetch(request);

    clientState = response.getMetadata().get("oauthState");
    assertNotNull(clientState);
    approvalUrl = response.getMetadata().get("oauthApprovalUrl");
    assertNotNull(approvalUrl);

    serviceProvider.browserVisit(approvalUrl + "&user_data=reapproved");
    fetcher = getFetcher(
        getSecurityToken("owner", "owner"),
        new OAuthRequestParams(FakeGadgetSpecFactory.SERVICE_NAME, null,
            clientState, false));
    request = new HttpRequest(
        new URI(FakeOAuthServiceProvider.RESOURCE_URL));
    request.getOptions().ignoreCache = true;
    response = fetcher.fetch(request);
    assertEquals("User data is reapproved", response.getResponseAsString());
  }
  

  @Test
  public void testError401() throws Exception {
    HttpFetcher fetcher;
    HttpRequest request;
    HttpResponse response;
    
    serviceProvider.setVagueErrors(true);

    fetcher = getFetcher(
        getSecurityToken("owner", "owner"),
        new OAuthRequestParams(FakeGadgetSpecFactory.SERVICE_NAME, null, null,
            false));
    request = new HttpRequest(
        new URI(FakeOAuthServiceProvider.RESOURCE_URL));
    response = fetcher.fetch(request);
    String clientState = response.getMetadata().get("oauthState");
    assertNotNull(clientState);
    String approvalUrl = response.getMetadata().get("oauthApprovalUrl");
    assertNotNull(approvalUrl);

    serviceProvider.browserVisit(approvalUrl + "&user_data=hello-oauth");

    fetcher = getFetcher(
        getSecurityToken("owner", "owner"),
        new OAuthRequestParams(FakeGadgetSpecFactory.SERVICE_NAME, null,
            clientState, false));
    request = new HttpRequest(
        new URI(FakeOAuthServiceProvider.RESOURCE_URL));
    response = fetcher.fetch(request);
    assertEquals("User data is hello-oauth", response.getResponseAsString());

    serviceProvider.revokeAllAccessTokens();

    fetcher = getFetcher(
        getSecurityToken("owner", "owner"),
        new OAuthRequestParams(FakeGadgetSpecFactory.SERVICE_NAME, null,
            clientState, false));
    request = new HttpRequest(
        new URI(FakeOAuthServiceProvider.RESOURCE_URL));
    request.getOptions().ignoreCache = true;
    response = fetcher.fetch(request);

    clientState = response.getMetadata().get("oauthState");
    assertNotNull(clientState);
    approvalUrl = response.getMetadata().get("oauthApprovalUrl");
    assertNotNull(approvalUrl);

    serviceProvider.browserVisit(approvalUrl + "&user_data=reapproved");
    fetcher = getFetcher(
        getSecurityToken("owner", "owner"),
        new OAuthRequestParams(FakeGadgetSpecFactory.SERVICE_NAME, null,
            clientState, false));
    request = new HttpRequest(
        new URI(FakeOAuthServiceProvider.RESOURCE_URL));
    request.getOptions().ignoreCache = true;
    response = fetcher.fetch(request);
    assertEquals("User data is reapproved", response.getResponseAsString());
  }

  @Test
  public void testUnknownConsumerKey() throws Exception {
    HttpFetcher fetcher;
    HttpRequest request;
    HttpResponse response;

    fetcher = getFetcher(
        getNokeySecurityToken("owner", "owner"),
        new OAuthRequestParams(FakeGadgetSpecFactory.SERVICE_NAME_NO_KEY, null,
            null, false));
    request = new HttpRequest(
        new URI(FakeOAuthServiceProvider.RESOURCE_URL));
    response = fetcher.fetch(request);
    Map<String, String> metadata = response.getMetadata();
    assertNotNull(metadata);
    assertEquals("consumer_key_unknown", metadata.get("oauthError"));
    assertEquals(
        "invalid consumer: garbage_key",
        metadata.get("oauthErrorText"));
  }
  
  @Test
  public void testError403() throws Exception {
    HttpFetcher fetcher;
    HttpRequest request;
    HttpResponse response;
    
    serviceProvider.setVagueErrors(true);

    fetcher = getFetcher(
        getNokeySecurityToken("owner", "owner"),
        new OAuthRequestParams(FakeGadgetSpecFactory.SERVICE_NAME_NO_KEY, null,
            null, false));
    request = new HttpRequest(
        new URI(FakeOAuthServiceProvider.RESOURCE_URL));
    response = fetcher.fetch(request);
    Map<String, String> metadata = response.getMetadata();
    assertNotNull(metadata);
    assertEquals("403", metadata.get("oauthError"));
    assertNull(metadata.get("oauthErrorText"));
  }

  @Test
  public void testConsumerThrottled() throws Exception {
    HttpFetcher fetcher;
    HttpRequest request;
    HttpResponse response;
    String clientState;

    assertEquals(0, serviceProvider.getRequestTokenCount());
    assertEquals(0, serviceProvider.getAccessTokenCount());
    assertEquals(0, serviceProvider.getResourceAccessCount());

    fetcher = getFetcher(
        getSecurityToken("owner", "owner"),
        new OAuthRequestParams(FakeGadgetSpecFactory.SERVICE_NAME, null, null,
            false));
    request = new HttpRequest(
        new URI(FakeOAuthServiceProvider.RESOURCE_URL));
    request.getOptions().ignoreCache = true;
    response = fetcher.fetch(request);
    clientState = response.getMetadata().get("oauthState");
    assertNotNull(clientState);
    String approvalUrl = response.getMetadata().get("oauthApprovalUrl");
    assertNotNull(approvalUrl);

    assertEquals(1, serviceProvider.getRequestTokenCount());
    assertEquals(0, serviceProvider.getAccessTokenCount());
    assertEquals(0, serviceProvider.getResourceAccessCount());

    serviceProvider.browserVisit(approvalUrl + "&user_data=hello-oauth");

    fetcher = getFetcher(
        getSecurityToken("owner", "owner"),
        new OAuthRequestParams(FakeGadgetSpecFactory.SERVICE_NAME, null,
            clientState, false));
    request = new HttpRequest(new URI(FakeOAuthServiceProvider.RESOURCE_URL));
    request.getOptions().ignoreCache = true;
    response = fetcher.fetch(request);
    assertEquals("User data is hello-oauth", response.getResponseAsString());
    clientState = response.getMetadata().get("oauthState");

    assertEquals(1, serviceProvider.getRequestTokenCount());
    assertEquals(1, serviceProvider.getAccessTokenCount());
    assertEquals(1, serviceProvider.getResourceAccessCount());

    fetcher = getFetcher(
        getSecurityToken("owner", "owner"),
        new OAuthRequestParams(FakeGadgetSpecFactory.SERVICE_NAME, null,
            clientState, false));
    request = new HttpRequest(new URI(FakeOAuthServiceProvider.RESOURCE_URL));
    request.getOptions().ignoreCache = true;
    response = fetcher.fetch(request);
    assertEquals("User data is hello-oauth", response.getResponseAsString());
    clientState = response.getMetadata().get("oauthState");

    assertEquals(1, serviceProvider.getRequestTokenCount());
    assertEquals(1, serviceProvider.getAccessTokenCount());
    assertEquals(2, serviceProvider.getResourceAccessCount());

    serviceProvider.setConsumersThrottled(true);

    fetcher = getFetcher(
        getSecurityToken("owner", "owner"),
        new OAuthRequestParams(FakeGadgetSpecFactory.SERVICE_NAME, null, null,
            false));
    request = new HttpRequest(new URI(FakeOAuthServiceProvider.RESOURCE_URL));
    request.getOptions().ignoreCache = true;
    response = fetcher.fetch(request);
    Map<String, String> metadata = response.getMetadata();
    assertNotNull(metadata);
    assertEquals("consumer_key_refused", metadata.get("oauthError"));
    assertEquals(
        "exceeded quota",
        metadata.get("oauthErrorText"));
    clientState = response.getMetadata().get("oauthState");

    assertEquals(1, serviceProvider.getRequestTokenCount());
    assertEquals(1, serviceProvider.getAccessTokenCount());
    assertEquals(3, serviceProvider.getResourceAccessCount());

    serviceProvider.setConsumersThrottled(false);

    fetcher = getFetcher(
        getSecurityToken("owner", "owner"),
        new OAuthRequestParams(FakeGadgetSpecFactory.SERVICE_NAME, null,
            clientState, false));
    request = new HttpRequest(new URI(FakeOAuthServiceProvider.RESOURCE_URL));
    request.getOptions().ignoreCache = true;
    response = fetcher.fetch(request);
    clientState = response.getMetadata().get("oauthState");

    assertEquals("User data is hello-oauth", response.getResponseAsString());

    assertEquals(1, serviceProvider.getRequestTokenCount());
    assertEquals(1, serviceProvider.getAccessTokenCount());
    assertEquals(4, serviceProvider.getResourceAccessCount());
  }
  
  @Test
  public void testWrongServiceName() throws Exception {
    HttpFetcher fetcher;
    HttpRequest request;
    HttpResponse response;

    fetcher = getFetcher(
        getSecurityToken("owner", "owner"),
        new OAuthRequestParams("nosuchservice", null, null, false));
    request = new HttpRequest(
        new URI(FakeOAuthServiceProvider.RESOURCE_URL));
    response = fetcher.fetch(request);
    
    Map<String, String> metadata = response.getMetadata();
    assertNull(metadata.get("oauthApprovalUrl"));
    assertEquals("BAD_OAUTH_CONFIGURATION", metadata.get("oauthError"));
    String errorText = metadata.get("oauthErrorText");
    assertEquals(
        0,
        errorText.indexOf("Spec does not contain OAuth service " +
        		"'nosuchservice'.  Known services: 'testservice'\n"));
  }
  
  @Test
  public void testPreapprovedToken() throws Exception {
    HttpFetcher fetcher;
    HttpRequest request;
    HttpResponse response;
    
    assertEquals(0, serviceProvider.getRequestTokenCount());
    assertEquals(0, serviceProvider.getAccessTokenCount());
    assertEquals(0, serviceProvider.getResourceAccessCount());
    
    TokenPair reqToken = serviceProvider.getPreapprovedToken("preapproved");
    
    OAuthRequestParams params = new OAuthRequestParams(
        FakeGadgetSpecFactory.SERVICE_NAME, null, null, false, reqToken.token,
            reqToken.secret);

    fetcher = getFetcher(getSecurityToken("owner", "owner"), params);
    request = new HttpRequest(new URI(FakeOAuthServiceProvider.RESOURCE_URL));
    response = fetcher.fetch(request);
    
    String clientState = response.getMetadata().get("oauthState");
    assertNotNull(clientState);
    String approvalUrl = response.getMetadata().get("oauthApprovalUrl");
    assertNull(approvalUrl);
    assertEquals("User data is preapproved", response.getResponseAsString());
    assertEquals(0, serviceProvider.getRequestTokenCount());
    assertEquals(1, serviceProvider.getAccessTokenCount());
    assertEquals(1, serviceProvider.getResourceAccessCount());
    
    fetcher = getFetcher(getSecurityToken("owner", "owner"), params);
    request = new HttpRequest(new URI(FakeOAuthServiceProvider.RESOURCE_URL));
    request.getOptions().ignoreCache = true;
    response = fetcher.fetch(request);
    
    assertEquals("User data is preapproved", response.getResponseAsString());
    assertEquals(0, serviceProvider.getRequestTokenCount());
    assertEquals(1, serviceProvider.getAccessTokenCount());
    assertEquals(2, serviceProvider.getResourceAccessCount());
    
    fetcher = getFetcher(getSecurityToken("owner", "owner"), params);
    request = new HttpRequest(new URI(FakeOAuthServiceProvider.RESOURCE_URL));
    request.getOptions().ignoreCache = true;
    response = fetcher.fetch(request);
    
    assertEquals("User data is preapproved", response.getResponseAsString());
    assertEquals(0, serviceProvider.getRequestTokenCount());
    assertEquals(1, serviceProvider.getAccessTokenCount());
    assertEquals(3, serviceProvider.getResourceAccessCount());
  }
  
  @Test
  public void testPreapprovedToken_invalid() throws Exception {
    HttpFetcher fetcher;
    HttpRequest request;
    HttpResponse response;

    OAuthRequestParams params = new OAuthRequestParams(
        FakeGadgetSpecFactory.SERVICE_NAME, null, null, false, "garbage",
        "garbage");
    
    fetcher = getFetcher(getSecurityToken("owner", "owner"), params);
    request = new HttpRequest(
        new URI(FakeOAuthServiceProvider.RESOURCE_URL));
    response = fetcher.fetch(request);
    String clientState = response.getMetadata().get("oauthState");
    assertNotNull(clientState);
    String approvalUrl = response.getMetadata().get("oauthApprovalUrl");
    assertNotNull(approvalUrl);

    serviceProvider.browserVisit(approvalUrl + "&user_data=hello-oauth");

    params = new OAuthRequestParams(
        FakeGadgetSpecFactory.SERVICE_NAME, null, clientState, false, "garbage",
            "garbage");
    
    fetcher = getFetcher(getSecurityToken("owner", "owner"), params);
    request = new HttpRequest(
        new URI(FakeOAuthServiceProvider.RESOURCE_URL));
    response = fetcher.fetch(request);
    assertEquals("User data is hello-oauth", response.getResponseAsString());
    clientState = response.getMetadata().get("oauthState");
    
    params = new OAuthRequestParams(
        FakeGadgetSpecFactory.SERVICE_NAME, null, clientState, false, "garbage",
            "garbage");
    fetcher = getFetcher(getSecurityToken("owner", "owner"), params);
    request = new HttpRequest(
        new URI(FakeOAuthServiceProvider.RESOURCE_URL));
    response = fetcher.fetch(request);
    assertEquals("User data is hello-oauth", response.getResponseAsString());
  }
  
  @Test
  public void testPreapprovedToken_invalidWithOutClientState()
      throws Exception {
    HttpFetcher fetcher;
    HttpRequest request;
    HttpResponse response;

    OAuthRequestParams params = new OAuthRequestParams(
        FakeGadgetSpecFactory.SERVICE_NAME, null, null, false, "garbage",
        "garbage");
    
    fetcher = getFetcher(getSecurityToken("owner", "owner"), params);
    request = new HttpRequest(
        new URI(FakeOAuthServiceProvider.RESOURCE_URL));
    response = fetcher.fetch(request);
    String clientState = response.getMetadata().get("oauthState");
    assertNotNull(clientState);
    String approvalUrl = response.getMetadata().get("oauthApprovalUrl");
    assertNotNull(approvalUrl);

    serviceProvider.browserVisit(approvalUrl + "&user_data=hello-oauth");

    params = new OAuthRequestParams(
        FakeGadgetSpecFactory.SERVICE_NAME, null, clientState, false, "garbage",
        "garbage");
    
    fetcher = getFetcher(getSecurityToken("owner", "owner"), params);
    request = new HttpRequest(
        new URI(FakeOAuthServiceProvider.RESOURCE_URL));
    response = fetcher.fetch(request);
    assertEquals("User data is hello-oauth", response.getResponseAsString());
    clientState = response.getMetadata().get("oauthState");
    
    // Simulates a user leaving the page and then returning
    params = new OAuthRequestParams(
        FakeGadgetSpecFactory.SERVICE_NAME, null, null, false, "garbage",
        "garbage");
    fetcher = getFetcher(getSecurityToken("owner", "owner"), params);
    request = new HttpRequest(
        new URI(FakeOAuthServiceProvider.RESOURCE_URL));
    response = fetcher.fetch(request);
    assertEquals("User data is hello-oauth", response.getResponseAsString());
  }
  
  @Test
  public void testCachedResponse() throws Exception {
    HttpFetcher fetcher;
    HttpRequest request;
    HttpResponse response;
    
    assertEquals(0, serviceProvider.getRequestTokenCount());
    assertEquals(0, serviceProvider.getAccessTokenCount());
    assertEquals(0, serviceProvider.getResourceAccessCount());

    fetcher = getFetcher(
        getSecurityToken("owner", "owner"),
        new OAuthRequestParams(FakeGadgetSpecFactory.SERVICE_NAME, null, null,
            false));
    request = new HttpRequest(new URI(FakeOAuthServiceProvider.RESOURCE_URL));
    response = fetcher.fetch(request);
    String clientState = response.getMetadata().get("oauthState");
    assertNotNull(clientState);
    String approvalUrl = response.getMetadata().get("oauthApprovalUrl");
    assertNotNull(approvalUrl);

    serviceProvider.browserVisit(approvalUrl + "&user_data=hello-oauth");

    fetcher = getFetcher(
        getSecurityToken("owner", "owner"),
        new OAuthRequestParams(FakeGadgetSpecFactory.SERVICE_NAME, null,
            clientState, false));
    request = new HttpRequest(new URI(FakeOAuthServiceProvider.RESOURCE_URL));
    response = fetcher.fetch(request);
    assertEquals("User data is hello-oauth", response.getResponseAsString());

    assertEquals(1, serviceProvider.getRequestTokenCount());
    assertEquals(1, serviceProvider.getAccessTokenCount());
    assertEquals(1, serviceProvider.getResourceAccessCount());
    
    fetcher = getFetcher(
        getSecurityToken("owner", "somebody else"),
        new OAuthRequestParams(FakeGadgetSpecFactory.SERVICE_NAME, null, null,
            false));
    request = new HttpRequest(
        new URI(FakeOAuthServiceProvider.RESOURCE_URL));
    response = fetcher.fetch(request);
    assertEquals("User data is hello-oauth", response.getResponseAsString());
    
    assertEquals(1, serviceProvider.getRequestTokenCount());
    assertEquals(1, serviceProvider.getAccessTokenCount());
    assertEquals(1, serviceProvider.getResourceAccessCount());
  }
}
