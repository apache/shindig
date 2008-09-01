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
import static org.junit.Assert.assertTrue;

import org.apache.shindig.auth.BasicSecurityToken;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.cache.DefaultCacheProvider;
import org.apache.shindig.common.crypto.BasicBlobCrypter;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.util.CharsetUtil;
import org.apache.shindig.gadgets.FakeGadgetSpecFactory;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.BasicHttpCache;
import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.oauth.FakeOAuthServiceProvider.TokenPair;
import org.apache.shindig.gadgets.oauth.OAuthStore.OAuthParamLocation;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

/**
 * Primitive test of the main code paths in OAuthFetcher.
 *
 * This is a fairly crappy regression test, so if you find yourself wanting
 * to modify this code, you should probably write additional test cases first.
 */
public class OAuthFetcherTest {

  private OAuthFetcherConfig fetcherConfig;
  private FakeOAuthServiceProvider serviceProvider;

  public static final String GADGET_URL = "http://www.example.com/gadget.xml";
  public static final String GADGET_URL_NO_KEY = "http://www.example.com/nokey.xml";
  public static final String GADGET_URL_HEADER = "http://www.example.com/header.xml";
  public static final String GADGET_URL_BODY = "http://www.example.com/body.xml";

  @Before
  public void setUp() throws Exception {
    serviceProvider = new FakeOAuthServiceProvider();
    fetcherConfig = new OAuthFetcherConfig(
        new BasicBlobCrypter("abcdefghijklmnop".getBytes()),
        getOAuthStore(),
        new BasicHttpCache(new DefaultCacheProvider(),10));
  }

  /**
   * Builds a nicely populated fake token store.
   */
  public static GadgetOAuthTokenStore getOAuthStore() throws GadgetException {
    BasicOAuthStore base = new BasicOAuthStore();
    addValidConsumer(base);
    addInvalidConsumer(base);
    addAuthHeaderConsumer(base);
    addBodyConsumer(base);
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
    OAuthStore.ProviderKey providerKey = new OAuthStore.ProviderKey();
    providerKey.setGadgetUri(gadgetUrl);
    providerKey.setServiceName(serviceName);

    OAuthStore.ConsumerKeyAndSecret kas = new OAuthStore.ConsumerKeyAndSecret(
        consumerKey, consumerSecret, OAuthStore.KeyType.HMAC_SYMMETRIC);

    base.setOAuthConsumerKeyAndSecret(providerKey, kas);
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

  @SuppressWarnings("unused")
  public HttpFetcher getFetcher(SecurityToken authToken,
      OAuthArguments params) throws GadgetException {
    OAuthFetcher fetcher = new OAuthFetcher(fetcherConfig, serviceProvider, authToken, params);
    return fetcher;
  }

  @Test
  public void testOAuthFlow() throws Exception {
    HttpFetcher fetcher;
    HttpRequest request;
    HttpResponse response;

    fetcher = getFetcher(
        getNormalSecurityToken("owner", "owner"),
        new OAuthArguments(FakeGadgetSpecFactory.SERVICE_NAME, null, null, false));
    request = new HttpRequest(Uri.parse(FakeOAuthServiceProvider.RESOURCE_URL));
    response = fetcher.fetch(request);
    String clientState = response.getMetadata().get("oauthState");
    assertNotNull(clientState);
    String approvalUrl = response.getMetadata().get("oauthApprovalUrl");
    assertNotNull(approvalUrl);

    serviceProvider.browserVisit(approvalUrl + "&user_data=hello-oauth");

    fetcher = getFetcher(
        getNormalSecurityToken("owner", "owner"),
        new OAuthArguments(FakeGadgetSpecFactory.SERVICE_NAME, null, clientState, false));
    request = new HttpRequest(Uri.parse(FakeOAuthServiceProvider.RESOURCE_URL));
    response = fetcher.fetch(request);
    assertEquals("User data is hello-oauth", response.getResponseAsString());
    assertNull(response.getHeader(FakeOAuthServiceProvider.AUTHZ_ECHO_HEADER));

    fetcher = getFetcher(
        getNormalSecurityToken("owner", "somebody else"),
        new OAuthArguments(FakeGadgetSpecFactory.SERVICE_NAME, null, null, false));
    request = new HttpRequest(Uri.parse(FakeOAuthServiceProvider.RESOURCE_URL));
    response = fetcher.fetch(request);
    assertEquals("User data is hello-oauth", response.getResponseAsString());

    fetcher = getFetcher(
        getNormalSecurityToken("somebody else", "somebody else"),
        new OAuthArguments(FakeGadgetSpecFactory.SERVICE_NAME, null, null, false));
    request = new HttpRequest(Uri.parse(FakeOAuthServiceProvider.RESOURCE_URL));
    response = fetcher.fetch(request);
    clientState = response.getMetadata().get("oauthState");
    assertNotNull(clientState);
    approvalUrl = response.getMetadata().get("oauthApprovalUrl");
    assertNotNull(approvalUrl);

    serviceProvider.browserVisit(approvalUrl + "&user_data=somebody%20else");

    fetcher = getFetcher(
        getNormalSecurityToken("somebody else", "somebody else"),
        new OAuthArguments(FakeGadgetSpecFactory.SERVICE_NAME, null, clientState, false));
    request = new HttpRequest(Uri.parse(FakeOAuthServiceProvider.RESOURCE_URL));
    response = fetcher.fetch(request);
    assertEquals("User data is somebody else", response.getResponseAsString());
  }
  
  @Test
  public void testParamsInHeader() throws Exception {
    HttpFetcher fetcher;
    HttpRequest request;
    HttpResponse response;
    
    serviceProvider.setParamLocation(OAuthParamLocation.AUTH_HEADER);
    
    fetcher = getFetcher(
        getHeaderSecurityToken("owner", "owner"),
        new OAuthArguments(FakeGadgetSpecFactory.SERVICE_NAME, null, null, false));
    request = new HttpRequest(Uri.parse(FakeOAuthServiceProvider.RESOURCE_URL));
    response = fetcher.fetch(request);
    String clientState = response.getMetadata().get("oauthState");
    assertNotNull(clientState);
    String approvalUrl = response.getMetadata().get("oauthApprovalUrl");
    assertNotNull(approvalUrl);

    serviceProvider.browserVisit(approvalUrl + "&user_data=hello-oauth");

    fetcher = getFetcher(
        getHeaderSecurityToken("owner", "owner"),
        new OAuthArguments(FakeGadgetSpecFactory.SERVICE_NAME, null, clientState, false));
    request = new HttpRequest(Uri.parse(FakeOAuthServiceProvider.RESOURCE_URL));
    response = fetcher.fetch(request);
    assertEquals("User data is hello-oauth", response.getResponseAsString());
    
    String aznHeader = response.getHeader(FakeOAuthServiceProvider.AUTHZ_ECHO_HEADER);
    assertNotNull(aznHeader);
    assertTrue("azn header: " + aznHeader, aznHeader.indexOf("OAuth") != -1);
  }
  
  @Test
  public void testParamsInBody() throws Exception {
    HttpFetcher fetcher;
    HttpRequest request;
    HttpResponse response;
    
    serviceProvider.setParamLocation(OAuthParamLocation.POST_BODY);

    fetcher = getFetcher(
        getBodySecurityToken("owner", "owner"),
        new OAuthArguments(FakeGadgetSpecFactory.SERVICE_NAME, null, null, false));
    request = new HttpRequest(Uri.parse(FakeOAuthServiceProvider.RESOURCE_URL));
    response = fetcher.fetch(request);
    String clientState = response.getMetadata().get("oauthState");
    assertNotNull(clientState);
    String approvalUrl = response.getMetadata().get("oauthApprovalUrl");
    assertNotNull(approvalUrl);

    serviceProvider.browserVisit(approvalUrl + "&user_data=hello-oauth");

    fetcher = getFetcher(
        getBodySecurityToken("owner", "owner"),
        new OAuthArguments(FakeGadgetSpecFactory.SERVICE_NAME, null, clientState, false));
    request = new HttpRequest(Uri.parse(FakeOAuthServiceProvider.RESOURCE_URL));
    request.setHeader("content-type", "application/x-www-form-urlencoded");
    request.setMethod("POST");
    response = fetcher.fetch(request);
    assertEquals("User data is hello-oauth", response.getResponseAsString());
    
    String echoedBody = response.getHeader(FakeOAuthServiceProvider.BODY_ECHO_HEADER);
    assertNotNull(echoedBody);
    assertTrue("body: " + echoedBody, echoedBody.indexOf("oauth_consumer_key=") != -1);
  }
  
  @Test
  public void testParamsInBody_withExtraParams() throws Exception {
    HttpFetcher fetcher;
    HttpRequest request;
    HttpResponse response;
    
    serviceProvider.setParamLocation(OAuthParamLocation.POST_BODY);

    fetcher = getFetcher(
        getBodySecurityToken("owner", "owner"),
        new OAuthArguments(FakeGadgetSpecFactory.SERVICE_NAME, null, null, false));
    request = new HttpRequest(Uri.parse(FakeOAuthServiceProvider.RESOURCE_URL));
    response = fetcher.fetch(request);
    String clientState = response.getMetadata().get("oauthState");
    assertNotNull(clientState);
    String approvalUrl = response.getMetadata().get("oauthApprovalUrl");
    assertNotNull(approvalUrl);

    serviceProvider.browserVisit(approvalUrl + "&user_data=hello-oauth");

    fetcher = getFetcher(
        getBodySecurityToken("owner", "owner"),
        new OAuthArguments(FakeGadgetSpecFactory.SERVICE_NAME, null, clientState, false));
    request = new HttpRequest(Uri.parse(FakeOAuthServiceProvider.RESOURCE_URL));
    request.setHeader("content-type", "application/x-www-form-urlencoded");
    request.setMethod("POST");
    request.setPostBody(CharsetUtil.getUtf8Bytes("foo=bar&foo=baz"));
    response = fetcher.fetch(request);
    assertEquals("User data is hello-oauth", response.getResponseAsString());
    
    String echoedBody = response.getHeader(FakeOAuthServiceProvider.BODY_ECHO_HEADER);
    assertNotNull(echoedBody);
    assertTrue("body: " + echoedBody, echoedBody.indexOf("oauth_consumer_key=") != -1);
    assertTrue("body: " + echoedBody, echoedBody.indexOf("foo=bar&foo=baz") != -1);
  }
  
  @Test
  public void testParamsInBody_forGetRequest() throws Exception {
    HttpFetcher fetcher;
    HttpRequest request;
    HttpResponse response;

    // We're sending a GET request with an auth-header, let the SP look in the header for the authz
    // params.
    serviceProvider.setParamLocation(OAuthParamLocation.POST_BODY);
    serviceProvider.addParamLocation(OAuthParamLocation.AUTH_HEADER);

    fetcher = getFetcher(
        getBodySecurityToken("owner", "owner"),
        new OAuthArguments(FakeGadgetSpecFactory.SERVICE_NAME, null, null, false));
    request = new HttpRequest(Uri.parse(FakeOAuthServiceProvider.RESOURCE_URL));
    response = fetcher.fetch(request);
    String clientState = response.getMetadata().get("oauthState");
    assertNotNull(clientState);
    String approvalUrl = response.getMetadata().get("oauthApprovalUrl");
    assertNotNull(approvalUrl);

    serviceProvider.browserVisit(approvalUrl + "&user_data=hello-oauth");

    fetcher = getFetcher(
        getBodySecurityToken("owner", "owner"),
        new OAuthArguments(FakeGadgetSpecFactory.SERVICE_NAME, null, clientState, false));
    request = new HttpRequest(Uri.parse(FakeOAuthServiceProvider.RESOURCE_URL));
    
    response = fetcher.fetch(request);
    assertEquals("User data is hello-oauth", response.getResponseAsString());
    
    String aznHeader = response.getHeader(FakeOAuthServiceProvider.AUTHZ_ECHO_HEADER);
    assertNotNull(aznHeader);
    assertTrue("azn header: " + aznHeader, aznHeader.indexOf("OAuth") != -1);
  }
  
  @Test
  public void testParamsInBody_forGetRequestStrictSp() throws Exception {
    HttpFetcher fetcher;
    HttpRequest request;
    HttpResponse response;

    serviceProvider.setParamLocation(OAuthParamLocation.POST_BODY);

    fetcher = getFetcher(
        getBodySecurityToken("owner", "owner"),
        new OAuthArguments(FakeGadgetSpecFactory.SERVICE_NAME, null, null, false));
    request = new HttpRequest(Uri.parse(FakeOAuthServiceProvider.RESOURCE_URL));
    response = fetcher.fetch(request);
    String clientState = response.getMetadata().get("oauthState");
    assertNotNull(clientState);
    String approvalUrl = response.getMetadata().get("oauthApprovalUrl");
    assertNotNull(approvalUrl);

    serviceProvider.browserVisit(approvalUrl + "&user_data=hello-oauth");

    fetcher = getFetcher(
        getBodySecurityToken("owner", "owner"),
        new OAuthArguments(FakeGadgetSpecFactory.SERVICE_NAME, null, clientState, false));
    request = new HttpRequest(Uri.parse(FakeOAuthServiceProvider.RESOURCE_URL));
    
    // Failed because the SP doesn't accept authz headers
    response = fetcher.fetch(request);
    approvalUrl = response.getMetadata().get("oauthApprovalUrl");
    assertNotNull(approvalUrl);
  }
  
  @Test
  public void testPlainTextParams() throws Exception {
    HttpFetcher fetcher;
    HttpRequest request;
    HttpResponse response;
    
    serviceProvider.setParamLocation(OAuthParamLocation.AUTH_HEADER);
    
    fetcher = getFetcher(
        getHeaderSecurityToken("owner", "owner"),
        new OAuthArguments(FakeGadgetSpecFactory.SERVICE_NAME, null, null, false));
    request = new HttpRequest(Uri.parse(FakeOAuthServiceProvider.RESOURCE_URL));
    response = fetcher.fetch(request);
    String clientState = response.getMetadata().get("oauthState");
    assertNotNull(clientState);
    String approvalUrl = response.getMetadata().get("oauthApprovalUrl");
    assertNotNull(approvalUrl);

    serviceProvider.browserVisit(approvalUrl + "&user_data=hello-oauth");

    fetcher = getFetcher(
        getHeaderSecurityToken("owner", "owner"),
        new OAuthArguments(FakeGadgetSpecFactory.SERVICE_NAME, null, clientState, false));
    request = new HttpRequest(Uri.parse(FakeOAuthServiceProvider.RESOURCE_URL));
    response = fetcher.fetch(request);
    assertEquals("User data is hello-oauth", response.getResponseAsString());
    
    String aznHeader = response.getHeader(FakeOAuthServiceProvider.AUTHZ_ECHO_HEADER);
    assertNotNull(aznHeader);
    assertTrue("azn header: " + aznHeader, aznHeader.indexOf("OAuth") != -1);
  }

  @Test
  public void testRevokedAccessToken() throws Exception {
    HttpFetcher fetcher;
    HttpRequest request;
    HttpResponse response;

    fetcher = getFetcher(
        getNormalSecurityToken("owner", "owner"),
        new OAuthArguments(FakeGadgetSpecFactory.SERVICE_NAME, null, null, false));
    request = new HttpRequest(Uri.parse(FakeOAuthServiceProvider.RESOURCE_URL));
    response = fetcher.fetch(request);
    String clientState = response.getMetadata().get("oauthState");
    assertNotNull(clientState);
    String approvalUrl = response.getMetadata().get("oauthApprovalUrl");
    assertNotNull(approvalUrl);

    serviceProvider.browserVisit(approvalUrl + "&user_data=hello-oauth");

    fetcher = getFetcher(
        getNormalSecurityToken("owner", "owner"),
        new OAuthArguments(FakeGadgetSpecFactory.SERVICE_NAME, null, clientState, false));
    request = new HttpRequest(Uri.parse(FakeOAuthServiceProvider.RESOURCE_URL));
    response = fetcher.fetch(request);
    assertEquals("User data is hello-oauth", response.getResponseAsString());

    serviceProvider.revokeAllAccessTokens();

    fetcher = getFetcher(
        getNormalSecurityToken("owner", "owner"),
        new OAuthArguments(FakeGadgetSpecFactory.SERVICE_NAME, null, clientState, false));
    request = new HttpRequest(Uri.parse(FakeOAuthServiceProvider.RESOURCE_URL));
    request.setIgnoreCache(true);
    response = fetcher.fetch(request);

    clientState = response.getMetadata().get("oauthState");
    assertNotNull(clientState);
    approvalUrl = response.getMetadata().get("oauthApprovalUrl");
    assertNotNull(approvalUrl);

    serviceProvider.browserVisit(approvalUrl + "&user_data=reapproved");
    fetcher = getFetcher(
        getNormalSecurityToken("owner", "owner"),
        new OAuthArguments(FakeGadgetSpecFactory.SERVICE_NAME, null, clientState, false));
    request = new HttpRequest(Uri.parse(FakeOAuthServiceProvider.RESOURCE_URL));
    request.setIgnoreCache(true);
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
        getNormalSecurityToken("owner", "owner"),
        new OAuthArguments(FakeGadgetSpecFactory.SERVICE_NAME, null, null, false));
    request = new HttpRequest(Uri.parse(FakeOAuthServiceProvider.RESOURCE_URL));
    response = fetcher.fetch(request);
    String clientState = response.getMetadata().get("oauthState");
    assertNotNull(clientState);
    String approvalUrl = response.getMetadata().get("oauthApprovalUrl");
    assertNotNull(approvalUrl);

    serviceProvider.browserVisit(approvalUrl + "&user_data=hello-oauth");

    fetcher = getFetcher(
        getNormalSecurityToken("owner", "owner"),
        new OAuthArguments(FakeGadgetSpecFactory.SERVICE_NAME, null, clientState, false));
    request = new HttpRequest(Uri.parse(FakeOAuthServiceProvider.RESOURCE_URL));
    response = fetcher.fetch(request);
    assertEquals("User data is hello-oauth", response.getResponseAsString());

    serviceProvider.revokeAllAccessTokens();

    fetcher = getFetcher(
        getNormalSecurityToken("owner", "owner"),
        new OAuthArguments(FakeGadgetSpecFactory.SERVICE_NAME, null, clientState, false));
    request = new HttpRequest(Uri.parse(FakeOAuthServiceProvider.RESOURCE_URL));
    request.setIgnoreCache(true);
    response = fetcher.fetch(request);

    clientState = response.getMetadata().get("oauthState");
    assertNotNull(clientState);
    approvalUrl = response.getMetadata().get("oauthApprovalUrl");
    assertNotNull(approvalUrl);

    serviceProvider.browserVisit(approvalUrl + "&user_data=reapproved");
    fetcher = getFetcher(
        getNormalSecurityToken("owner", "owner"),
        new OAuthArguments(FakeGadgetSpecFactory.SERVICE_NAME, null, clientState, false));
    request = new HttpRequest(Uri.parse(FakeOAuthServiceProvider.RESOURCE_URL));
    request.setIgnoreCache(true);
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
        new OAuthArguments(FakeGadgetSpecFactory.SERVICE_NAME_NO_KEY, null, null, false));
    request = new HttpRequest(Uri.parse(FakeOAuthServiceProvider.RESOURCE_URL));
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
        new OAuthArguments(FakeGadgetSpecFactory.SERVICE_NAME_NO_KEY, null, null, false));
    request = new HttpRequest(Uri.parse(FakeOAuthServiceProvider.RESOURCE_URL));
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
        getNormalSecurityToken("owner", "owner"),
        new OAuthArguments(FakeGadgetSpecFactory.SERVICE_NAME, null, null, false));
    request = new HttpRequest(Uri.parse(FakeOAuthServiceProvider.RESOURCE_URL));
    request.setIgnoreCache(true);
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
        getNormalSecurityToken("owner", "owner"),
        new OAuthArguments(FakeGadgetSpecFactory.SERVICE_NAME, null, clientState, false));
    request = new HttpRequest(Uri.parse(FakeOAuthServiceProvider.RESOURCE_URL));
    request.setIgnoreCache(true);
    response = fetcher.fetch(request);
    assertEquals("User data is hello-oauth", response.getResponseAsString());
    clientState = response.getMetadata().get("oauthState");

    assertEquals(1, serviceProvider.getRequestTokenCount());
    assertEquals(1, serviceProvider.getAccessTokenCount());
    assertEquals(1, serviceProvider.getResourceAccessCount());

    fetcher = getFetcher(
        getNormalSecurityToken("owner", "owner"),
        new OAuthArguments(FakeGadgetSpecFactory.SERVICE_NAME, null, clientState, false));
    request = new HttpRequest(Uri.parse(FakeOAuthServiceProvider.RESOURCE_URL));
    request.setIgnoreCache(true);
    response = fetcher.fetch(request);
    assertEquals("User data is hello-oauth", response.getResponseAsString());
    clientState = response.getMetadata().get("oauthState");

    assertEquals(1, serviceProvider.getRequestTokenCount());
    assertEquals(1, serviceProvider.getAccessTokenCount());
    assertEquals(2, serviceProvider.getResourceAccessCount());

    serviceProvider.setConsumersThrottled(true);

    fetcher = getFetcher(
        getNormalSecurityToken("owner", "owner"),
        new OAuthArguments(FakeGadgetSpecFactory.SERVICE_NAME, null, null,
            false));
    request = new HttpRequest(Uri.parse(FakeOAuthServiceProvider.RESOURCE_URL));
    request.setIgnoreCache(true);
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
        getNormalSecurityToken("owner", "owner"),
        new OAuthArguments(FakeGadgetSpecFactory.SERVICE_NAME, null,
            clientState, false));
    request = new HttpRequest(Uri.parse(FakeOAuthServiceProvider.RESOURCE_URL));
    request.setIgnoreCache(true);
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
        getNormalSecurityToken("owner", "owner"),
        new OAuthArguments("nosuchservice", null, null, false));
    request = new HttpRequest(
        Uri.parse(FakeOAuthServiceProvider.RESOURCE_URL));
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

    OAuthArguments params = new OAuthArguments(
        FakeGadgetSpecFactory.SERVICE_NAME, null, null, false, reqToken.token,
            reqToken.secret);

    fetcher = getFetcher(getNormalSecurityToken("owner", "owner"), params);
    request = new HttpRequest(Uri.parse(FakeOAuthServiceProvider.RESOURCE_URL));
    response = fetcher.fetch(request);

    String clientState = response.getMetadata().get("oauthState");
    assertNotNull(clientState);
    String approvalUrl = response.getMetadata().get("oauthApprovalUrl");
    assertNull(approvalUrl);
    assertEquals("User data is preapproved", response.getResponseAsString());
    assertEquals(0, serviceProvider.getRequestTokenCount());
    assertEquals(1, serviceProvider.getAccessTokenCount());
    assertEquals(1, serviceProvider.getResourceAccessCount());

    fetcher = getFetcher(getNormalSecurityToken("owner", "owner"), params);
    request = new HttpRequest(Uri.parse(FakeOAuthServiceProvider.RESOURCE_URL));
    request.setIgnoreCache(true);
    response = fetcher.fetch(request);

    assertEquals("User data is preapproved", response.getResponseAsString());
    assertEquals(0, serviceProvider.getRequestTokenCount());
    assertEquals(1, serviceProvider.getAccessTokenCount());
    assertEquals(2, serviceProvider.getResourceAccessCount());

    fetcher = getFetcher(getNormalSecurityToken("owner", "owner"), params);
    request = new HttpRequest(Uri.parse(FakeOAuthServiceProvider.RESOURCE_URL));
    request.setIgnoreCache(true);
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

    OAuthArguments params = new OAuthArguments(
        FakeGadgetSpecFactory.SERVICE_NAME, null, null, false, "garbage", "garbage");

    fetcher = getFetcher(getNormalSecurityToken("owner", "owner"), params);
    request = new HttpRequest(Uri.parse(FakeOAuthServiceProvider.RESOURCE_URL));
    response = fetcher.fetch(request);
    String clientState = response.getMetadata().get("oauthState");
    assertNotNull(clientState);
    String approvalUrl = response.getMetadata().get("oauthApprovalUrl");
    assertNotNull(approvalUrl);

    serviceProvider.browserVisit(approvalUrl + "&user_data=hello-oauth");

    params = new OAuthArguments(
        FakeGadgetSpecFactory.SERVICE_NAME, null, clientState, false, "garbage", "garbage");

    fetcher = getFetcher(getNormalSecurityToken("owner", "owner"), params);
    request = new HttpRequest(Uri.parse(FakeOAuthServiceProvider.RESOURCE_URL));
    response = fetcher.fetch(request);
    assertEquals("User data is hello-oauth", response.getResponseAsString());
    clientState = response.getMetadata().get("oauthState");

    params = new OAuthArguments(
        FakeGadgetSpecFactory.SERVICE_NAME, null, clientState, false, "garbage", "garbage");
    fetcher = getFetcher(getNormalSecurityToken("owner", "owner"), params);
    request = new HttpRequest(Uri.parse(FakeOAuthServiceProvider.RESOURCE_URL));
    response = fetcher.fetch(request);
    assertEquals("User data is hello-oauth", response.getResponseAsString());
  }

  @Test
  public void testPreapprovedToken_invalidWithOutClientState()
      throws Exception {
    HttpFetcher fetcher;
    HttpRequest request;
    HttpResponse response;

    OAuthArguments params = new OAuthArguments(
        FakeGadgetSpecFactory.SERVICE_NAME, null, null, false, "garbage", "garbage");

    fetcher = getFetcher(getNormalSecurityToken("owner", "owner"), params);
    request = new HttpRequest(Uri.parse(FakeOAuthServiceProvider.RESOURCE_URL));
    response = fetcher.fetch(request);
    String clientState = response.getMetadata().get("oauthState");
    assertNotNull(clientState);
    String approvalUrl = response.getMetadata().get("oauthApprovalUrl");
    assertNotNull(approvalUrl);

    serviceProvider.browserVisit(approvalUrl + "&user_data=hello-oauth");

    params = new OAuthArguments(
        FakeGadgetSpecFactory.SERVICE_NAME, null, clientState, false, "garbage", "garbage");

    fetcher = getFetcher(getNormalSecurityToken("owner", "owner"), params);
    request = new HttpRequest(Uri.parse(FakeOAuthServiceProvider.RESOURCE_URL));
    response = fetcher.fetch(request);
    assertEquals("User data is hello-oauth", response.getResponseAsString());
    clientState = response.getMetadata().get("oauthState");

    // Simulates a user leaving the page and then returning
    params = new OAuthArguments(
        FakeGadgetSpecFactory.SERVICE_NAME, null, null, false, "garbage",
        "garbage");
    fetcher = getFetcher(getNormalSecurityToken("owner", "owner"), params);
    request = new HttpRequest(Uri.parse(FakeOAuthServiceProvider.RESOURCE_URL));
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
        getNormalSecurityToken("owner", "owner"),
        new OAuthArguments(FakeGadgetSpecFactory.SERVICE_NAME, null, null, false));
    request = new HttpRequest(Uri.parse(FakeOAuthServiceProvider.RESOURCE_URL));
    response = fetcher.fetch(request);
    String clientState = response.getMetadata().get("oauthState");
    assertNotNull(clientState);
    String approvalUrl = response.getMetadata().get("oauthApprovalUrl");
    assertNotNull(approvalUrl);

    serviceProvider.browserVisit(approvalUrl + "&user_data=hello-oauth");

    fetcher = getFetcher(
        getNormalSecurityToken("owner", "owner"),
        new OAuthArguments(FakeGadgetSpecFactory.SERVICE_NAME, null, clientState, false));
    request = new HttpRequest(Uri.parse(FakeOAuthServiceProvider.RESOURCE_URL));
    response = fetcher.fetch(request);
    assertEquals("User data is hello-oauth", response.getResponseAsString());

    assertEquals(1, serviceProvider.getRequestTokenCount());
    assertEquals(1, serviceProvider.getAccessTokenCount());
    assertEquals(1, serviceProvider.getResourceAccessCount());

    fetcher = getFetcher(
        getNormalSecurityToken("owner", "somebody else"),
        new OAuthArguments(FakeGadgetSpecFactory.SERVICE_NAME, null, null, false));
    request = new HttpRequest(Uri.parse(FakeOAuthServiceProvider.RESOURCE_URL));
    response = fetcher.fetch(request);
    assertEquals("User data is hello-oauth", response.getResponseAsString());

    assertEquals(1, serviceProvider.getRequestTokenCount());
    assertEquals(1, serviceProvider.getAccessTokenCount());
    assertEquals(1, serviceProvider.getResourceAccessCount());
  }
}
