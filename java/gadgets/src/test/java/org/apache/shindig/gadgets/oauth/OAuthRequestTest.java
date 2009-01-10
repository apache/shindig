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

import org.apache.shindig.auth.BasicSecurityToken;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.crypto.BasicBlobCrypter;
import org.apache.shindig.common.util.CharsetUtil;
import org.apache.shindig.common.util.FakeTimeSource;
import org.apache.shindig.gadgets.FakeGadgetSpecFactory;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.oauth.AccessorInfo.OAuthParamLocation;
import org.apache.shindig.gadgets.oauth.BasicOAuthStoreConsumerKeyAndSecret.KeyType;
import org.apache.shindig.gadgets.oauth.OAuthArguments.UseToken;
import org.apache.shindig.gadgets.oauth.testing.FakeOAuthServiceProvider;
import org.apache.shindig.gadgets.oauth.testing.MakeRequestClient;
import org.apache.shindig.gadgets.oauth.testing.FakeOAuthServiceProvider.TokenPair;

import com.google.common.collect.Lists;

import net.oauth.OAuth;
import net.oauth.OAuth.Parameter;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Tests for signing requests.
 */
public class OAuthRequestTest {

  private OAuthFetcherConfig fetcherConfig;
  private FakeOAuthServiceProvider serviceProvider;
  private BasicOAuthStore base;
  private final List<LogRecord> logRecords = Lists.newArrayList();
  private final FakeTimeSource clock = new FakeTimeSource();

  public static final String GADGET_URL = "http://www.example.com/gadget.xml";
  public static final String GADGET_URL_NO_KEY = "http://www.example.com/nokey.xml";
  public static final String GADGET_URL_HEADER = "http://www.example.com/header.xml";
  public static final String GADGET_URL_BODY = "http://www.example.com/body.xml";
  public static final String GADGET_URL_BAD_OAUTH_URL = "http://www.example.com/badoauthurl.xml";
  public static final String GADGET_URL_APPROVAL_PARAMS =
      "http://www.example.com/approvalparams.xml";

  @Before
  public void setUp() throws Exception {
    base = new BasicOAuthStore();
    serviceProvider = new FakeOAuthServiceProvider(clock);
    fetcherConfig = new OAuthFetcherConfig(
        new BasicBlobCrypter("abcdefghijklmnop".getBytes()),
        getOAuthStore(base),
        clock);

    Logger logger = Logger.getLogger(OAuthResponseParams.class.getName());
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
    addBadOAuthUrlConsumer(base);
    addApprovalParamsConsumer(base);
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

  private static void addBadOAuthUrlConsumer(BasicOAuthStore base) {
    addConsumer(
        base,
        GADGET_URL_BAD_OAUTH_URL,
        FakeGadgetSpecFactory.SERVICE_NAME,
        FakeOAuthServiceProvider.CONSUMER_KEY,
        FakeOAuthServiceProvider.CONSUMER_SECRET);
  }

  private static void addApprovalParamsConsumer(BasicOAuthStore base) {
    addConsumer(
        base,
        GADGET_URL_APPROVAL_PARAMS,
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

  /** Client that does OAuth and sends opensocial_* params */
  private MakeRequestClient makeNonSocialClient(String owner, String viewer, String gadget)
      throws Exception {
    SecurityToken securityToken = getSecurityToken(owner, viewer, gadget);
    MakeRequestClient client = new MakeRequestClient(securityToken, fetcherConfig, serviceProvider,
        FakeGadgetSpecFactory.SERVICE_NAME);
    client.getBaseArgs().setSignOwner(true);
    client.getBaseArgs().setSignViewer(true);
    return client;
  }

  /** Client that does OAuth and does not send opensocial_* params */
  private MakeRequestClient makeStrictNonSocialClient(String owner, String viewer, String gadget)
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
    assertNull("Should not return oauthError for revoked token",
        response.getMetadata().get("oauthError"));
    String errorText = response.getMetadata().get("oauthErrorText");
    assertNotNull(errorText);
    checkStringContains("should return original request", errorText, "GET /data?cachebust=2\n");
    checkStringContains("should return signed request", errorText, "GET /data?cachebust=2&");
    checkStringContains("should remove secret", errorText, "oauth_token_secret=REMOVED");
    checkStringContains("should return response", errorText, "HTTP/1.1 403");
    checkStringContains("should return response", errorText, "oauth_problem=\"token_revoked\"");

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
    String errorText = response.getMetadata().get("oauthErrorText");
    checkStringContains("oauthErrorText mismatch", errorText,
        "Service provider rejected request");
    checkStringContains("oauthErrorText mismatch", errorText,
        "oauth_problem_advice=\"invalid%20consumer%3A%20garbage_key\"");
    checkStringContains("should return original request", errorText, "GET /data\n");
    checkStringContains("should return request token request", errorText,
        "GET /request?param=foo&");
  }

  @Test
  public void testBrokenRequestTokenResponse() throws Exception {
    SecurityToken securityToken = getSecurityToken("owner", "owner", GADGET_URL_BAD_OAUTH_URL);
    MakeRequestClient client = new MakeRequestClient(securityToken, fetcherConfig, serviceProvider,
        FakeGadgetSpecFactory.SERVICE_NAME);

    HttpResponse response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals(403, response.getHttpStatusCode());
    assertEquals("", response.getResponseAsString());
    Map<String, String> metadata = response.getMetadata();
    assertNotNull(metadata);
    assertEquals("UNKNOWN_PROBLEM", metadata.get("oauthError"));
    String errorText = response.getMetadata().get("oauthErrorText");
    checkStringContains("oauthErrorText mismatch", errorText,
        "No oauth_token returned from service provider");
    checkStringContains("oauthErrorText mismatch", errorText,
        "GET /echo?mary_had_a_little_lamb");
  }

  @Test
  public void testBrokenAccessTokenResponse() throws Exception {
    SecurityToken securityToken = getSecurityToken("owner", "owner", GADGET_URL_BAD_OAUTH_URL);
    MakeRequestClient client = new MakeRequestClient(securityToken, fetcherConfig, serviceProvider,
        FakeGadgetSpecFactory.SERVICE_NAME);
    // This lets us skip the access token step
    client.getBaseArgs().setRequestToken("reqtoken");
    client.getBaseArgs().setRequestTokenSecret("reqtokensecret");

    HttpResponse response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals(403, response.getHttpStatusCode());
    assertEquals("", response.getResponseAsString());
    Map<String, String> metadata = response.getMetadata();
    assertNotNull(metadata);
    assertEquals("UNKNOWN_PROBLEM", metadata.get("oauthError"));
    String errorText = response.getMetadata().get("oauthErrorText");
    checkStringContains("oauthErrorText mismatch", errorText,
        "No oauth_token_secret returned from service provider");
    checkStringContains("oauthErrorText mismatch", errorText,
        "with_fleece_as_white_as_snow");
  }

  @Test
  public void testExtraApprovalParams() throws Exception {
    MakeRequestClient client = makeNonSocialClient("owner", "owner", GADGET_URL_APPROVAL_PARAMS);

    HttpResponse response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("", response.getResponseAsString());
    String approvalUrl = response.getMetadata().get("oauthApprovalUrl");
    assertTrue(approvalUrl, approvalUrl.indexOf(
        "http://www.example.com/authorize?oauth_callback=foo&oauth_token=") == 0);
    client.approveToken("user_data=hello-oauth");

    response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("User data is hello-oauth", response.getResponseAsString());
    checkEmptyLog();
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
    assertEquals("UNKNOWN_PROBLEM", metadata.get("oauthError"));
    checkStringContains("oauthErrorText mismatch", metadata.get("oauthErrorText"),
        "some vague error");
    checkStringContains("oauthErrorText mismatch", metadata.get("oauthErrorText"),
        "HTTP/1.1 403");
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
    checkStringContains("oauthErrorText mismatch", metadata.get("oauthErrorText"),
        "Service provider rejected request");
    checkStringContains("oauthErrorText missing request entry", metadata.get("oauthErrorText"),
        "GET /data?cachebust=2\n");
    checkStringContains("oauthErrorText missing request entry", metadata.get("oauthErrorText"),
        "GET /data?cachebust=2&opensocial_owner_id=owner");

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
        "Failed to retrieve OAuth URLs, spec for gadget does " +
        "not contain OAuth service nosuchservice.  Known services: testservice"));
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
    assertNull(response.getMetadata().get("oauthError"));
    String errorText = response.getMetadata().get("oauthErrorText");
    checkStringContains("Should return sent request", errorText, "GET /data?opensocial_owner");
    checkStringContains("Should return response", errorText, "HTTP/1.1 401");
    checkStringContains("Should return response", errorText, "some vague error");
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
  public void testTrickyParametersInQuery() throws Exception {
    MakeRequestClient client = makeSignedFetchClient("o", "v", "http://www.example.com/app");
    String tricky = "%6fpensocial_owner_id=gotcha";
    HttpResponse resp = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL + "?" + tricky);
    assertEquals(OAuthError.INVALID_REQUEST.toString(),
        resp.getMetadata().get(OAuthResponseParams.ERROR_CODE));
    checkStringContains("Wrong error text", resp.getMetadata().get("oauthErrorText"),
        "invalid parameter name opensocial_owner_id, applications may not override opensocial " +
        "or oauth parameters");
  }

  @Test
  public void testTrickyParametersInBody() throws Exception {
    MakeRequestClient client = makeSignedFetchClient("o", "v", "http://www.example.com/app");
    String tricky = "%6fpensocial_owner_id=gotcha";
    HttpResponse resp = client.sendFormPost(FakeOAuthServiceProvider.RESOURCE_URL, tricky);
    assertEquals(OAuthError.INVALID_REQUEST.toString(),
        resp.getMetadata().get(OAuthResponseParams.ERROR_CODE));
    checkStringContains("Wrong error text", resp.getMetadata().get("oauthErrorText"),
        "invalid parameter name opensocial_owner_id, applications may not override opensocial " +
        "or oauth parameters");
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

  @Test
  public void testStripOpenSocialParamsFromQuery() throws Exception {
    MakeRequestClient client = makeSignedFetchClient("o", "v", "http://www.example.com/app");
    HttpResponse resp =
        client.sendFormPost(FakeOAuthServiceProvider.RESOURCE_URL + "?opensocial_foo=bar", null);
    assertEquals(OAuthError.INVALID_REQUEST.toString(),
        resp.getMetadata().get(OAuthResponseParams.ERROR_CODE));
    checkStringContains("Wrong error text", resp.getMetadata().get("oauthErrorText"),
        "invalid parameter name opensocial_foo");
  }

  @Test
  public void testStripOAuthParamsFromQuery() throws Exception {
    MakeRequestClient client = makeSignedFetchClient("o", "v", "http://www.example.com/app");
    HttpResponse resp =
        client.sendFormPost(FakeOAuthServiceProvider.RESOURCE_URL + "?oauth_foo=bar", "name=value");
    assertEquals(OAuthError.INVALID_REQUEST.toString(),
        resp.getMetadata().get(OAuthResponseParams.ERROR_CODE));
    checkStringContains("Wrong error text", resp.getMetadata().get("oauthErrorText"),
        "invalid parameter name oauth_foo");
  }

  @Test
  public void testStripOpenSocialParamsFromBody() throws Exception {
    MakeRequestClient client = makeSignedFetchClient("o", "v", "http://www.example.com/app");
    HttpResponse resp =
        client.sendFormPost(FakeOAuthServiceProvider.RESOURCE_URL, "opensocial_foo=bar");
    assertEquals(OAuthError.INVALID_REQUEST.toString(),
        resp.getMetadata().get(OAuthResponseParams.ERROR_CODE));
    checkStringContains("Wrong error text", resp.getMetadata().get("oauthErrorText"),
        "invalid parameter name opensocial_foo");
  }

  @Test
  public void testStripOAuthParamsFromBody() throws Exception {
    MakeRequestClient client = makeSignedFetchClient("o", "v", "http://www.example.com/app");
    HttpResponse resp = client.sendFormPost(FakeOAuthServiceProvider.RESOURCE_URL, "oauth_foo=bar");
    assertEquals(OAuthError.INVALID_REQUEST.toString(),
        resp.getMetadata().get(OAuthResponseParams.ERROR_CODE));
    checkStringContains("Wrong error text", resp.getMetadata().get("oauthErrorText"),
        "invalid parameter name oauth_foo");
  }

  // Test we can refresh an expired access token.
  @Test
  public void testAccessTokenExpires_onClient() throws Exception {
    serviceProvider.setSessionExtension(true);
    MakeRequestClient client = makeNonSocialClient("owner", "owner", GADGET_URL);

    HttpResponse response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("", response.getResponseAsString());
    client.approveToken("user_data=hello-oauth");

    response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("User data is hello-oauth", response.getResponseAsString());

    assertEquals(1, serviceProvider.getRequestTokenCount());
    assertEquals(1, serviceProvider.getAccessTokenCount());
    assertEquals(1, serviceProvider.getResourceAccessCount());

    clock.incrementSeconds(FakeOAuthServiceProvider.TOKEN_EXPIRATION_SECONDS + 1);

    response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL + "?cb=1");
    assertEquals("User data is hello-oauth", response.getResponseAsString());

    assertEquals(1, serviceProvider.getRequestTokenCount());
    assertEquals(2, serviceProvider.getAccessTokenCount());
    assertEquals(2, serviceProvider.getResourceAccessCount());

    response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL + "?cb=3");
    assertEquals("User data is hello-oauth", response.getResponseAsString());

    assertEquals(1, serviceProvider.getRequestTokenCount());
    assertEquals(2, serviceProvider.getAccessTokenCount());
    assertEquals(3, serviceProvider.getResourceAccessCount());

    clock.incrementSeconds(FakeOAuthServiceProvider.TOKEN_EXPIRATION_SECONDS + 1);

    response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL + "?cb=4");
    assertEquals("User data is hello-oauth", response.getResponseAsString());

    assertEquals(1, serviceProvider.getRequestTokenCount());
    assertEquals(3, serviceProvider.getAccessTokenCount());
    assertEquals(4, serviceProvider.getResourceAccessCount());

    checkEmptyLog();
  }

  // Tests the case where the server doesn't tell us when the token will expire.  This requires
  // an extra round trip to discover that the token has expired.
  @Test
  public void testAccessTokenExpires_onClientNoPredictedExpiration() throws Exception {
    serviceProvider.setSessionExtension(true);
    serviceProvider.setReportExpirationTimes(false);
    MakeRequestClient client = makeNonSocialClient("owner", "owner", GADGET_URL);

    HttpResponse response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("", response.getResponseAsString());
    client.approveToken("user_data=hello-oauth");

    response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("User data is hello-oauth", response.getResponseAsString());

    assertEquals(1, serviceProvider.getRequestTokenCount());
    assertEquals(1, serviceProvider.getAccessTokenCount());
    assertEquals(1, serviceProvider.getResourceAccessCount());

    clock.incrementSeconds(FakeOAuthServiceProvider.TOKEN_EXPIRATION_SECONDS + 1);

    response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL + "?cb=1");
    assertEquals("User data is hello-oauth", response.getResponseAsString());

    assertEquals(1, serviceProvider.getRequestTokenCount());
    assertEquals(2, serviceProvider.getAccessTokenCount());
    assertEquals(3, serviceProvider.getResourceAccessCount());

    response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL + "?cb=3");
    assertEquals("User data is hello-oauth", response.getResponseAsString());

    assertEquals(1, serviceProvider.getRequestTokenCount());
    assertEquals(2, serviceProvider.getAccessTokenCount());
    assertEquals(4, serviceProvider.getResourceAccessCount());

    clock.incrementSeconds(FakeOAuthServiceProvider.TOKEN_EXPIRATION_SECONDS + 1);

    response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL + "?cb=4");
    assertEquals("User data is hello-oauth", response.getResponseAsString());

    assertEquals(1, serviceProvider.getRequestTokenCount());
    assertEquals(3, serviceProvider.getAccessTokenCount());
    assertEquals(6, serviceProvider.getResourceAccessCount());
  }

  @Test
  public void testAccessTokenExpires_onServer() throws Exception {
    serviceProvider.setSessionExtension(true);
    MakeRequestClient client = makeNonSocialClient("owner", "owner", GADGET_URL);

    HttpResponse response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("", response.getResponseAsString());
    client.approveToken("user_data=hello-oauth");

    response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("User data is hello-oauth", response.getResponseAsString());
    assertEquals(1, serviceProvider.getRequestTokenCount());
    assertEquals(1, serviceProvider.getAccessTokenCount());
    assertEquals(1, serviceProvider.getResourceAccessCount());

    // clears oauthState
    client = makeNonSocialClient("owner", "owner", GADGET_URL);

    clock.incrementSeconds(FakeOAuthServiceProvider.TOKEN_EXPIRATION_SECONDS + 1);

    response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL + "?cb=1");
    assertEquals("User data is hello-oauth", response.getResponseAsString());

    assertEquals(1, serviceProvider.getRequestTokenCount());
    assertEquals(2, serviceProvider.getAccessTokenCount());
    assertEquals(2, serviceProvider.getResourceAccessCount());
  }

  @Test
  public void testAccessTokenExpired_andRevoked() throws Exception {
    serviceProvider.setSessionExtension(true);
    MakeRequestClient client = makeNonSocialClient("owner", "owner", GADGET_URL);

    HttpResponse response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("", response.getResponseAsString());
    client.approveToken("user_data=hello-oauth");

    response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("User data is hello-oauth", response.getResponseAsString());

    assertEquals(1, serviceProvider.getRequestTokenCount());
    assertEquals(1, serviceProvider.getAccessTokenCount());
    assertEquals(1, serviceProvider.getResourceAccessCount());

    clock.incrementSeconds(FakeOAuthServiceProvider.TOKEN_EXPIRATION_SECONDS + 1);
    serviceProvider.revokeAllAccessTokens();

    response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL + "?cb=1");
    assertEquals("", response.getResponseAsString());
    assertEquals(2, serviceProvider.getRequestTokenCount());
    assertEquals(2, serviceProvider.getAccessTokenCount());
    assertEquals(1, serviceProvider.getResourceAccessCount());

    client.approveToken("user_data=renewed");

    response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL + "?cb=1");
    assertEquals(2, serviceProvider.getRequestTokenCount());
    assertEquals(3, serviceProvider.getAccessTokenCount());
    assertEquals(2, serviceProvider.getResourceAccessCount());
    assertEquals("User data is renewed", response.getResponseAsString());
    checkLogContains("oauth_token_secret=REMOVED");
  }

  @Test
  public void testBadSessionHandle() throws Exception {
    serviceProvider.setSessionExtension(true);
    MakeRequestClient client = makeNonSocialClient("owner", "owner", GADGET_URL);

    HttpResponse response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("", response.getResponseAsString());
    client.approveToken("user_data=hello-oauth");

    response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("User data is hello-oauth", response.getResponseAsString());

    assertEquals(1, serviceProvider.getRequestTokenCount());
    assertEquals(1, serviceProvider.getAccessTokenCount());
    assertEquals(1, serviceProvider.getResourceAccessCount());

    clock.incrementSeconds(FakeOAuthServiceProvider.TOKEN_EXPIRATION_SECONDS + 1);
    serviceProvider.changeAllSessionHandles();

    response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL + "?cb=1");
    assertEquals("", response.getResponseAsString());
    assertEquals(2, serviceProvider.getRequestTokenCount());
    assertEquals(2, serviceProvider.getAccessTokenCount());
    assertEquals(1, serviceProvider.getResourceAccessCount());

    client.approveToken("user_data=renewed");

    response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL + "?cb=1");
    assertEquals(2, serviceProvider.getRequestTokenCount());
    assertEquals(3, serviceProvider.getAccessTokenCount());
    assertEquals(2, serviceProvider.getResourceAccessCount());
    assertEquals("User data is renewed", response.getResponseAsString());
    checkLogContains("oauth_session_handle=REMOVED");
  }

  @Test
  public void testExtraParamsRejected() throws Exception {
    serviceProvider.setRejectExtraParams(true);
    MakeRequestClient client = makeNonSocialClient("owner", "owner", GADGET_URL);

    HttpResponse response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("parameter_rejected", response.getMetadata().get("oauthError"));
  }

  @Test
  public void testExtraParamsSuppressed() throws Exception {
    serviceProvider.setRejectExtraParams(true);
    MakeRequestClient client = makeStrictNonSocialClient("owner", "owner", GADGET_URL);

    HttpResponse response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("", response.getResponseAsString());
    client.approveToken("user_data=hello-oauth");

    response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("User data is hello-oauth", response.getResponseAsString());
  }

  @Test
  public void testCanRetrieveAccessTokenData() throws Exception {
    serviceProvider.setReturnAccessTokenData(true);

    MakeRequestClient client = makeNonSocialClient("owner", "owner", GADGET_URL);

    HttpResponse response = client.sendGet(FakeOAuthServiceProvider.ACCESS_TOKEN_URL);
    assertEquals("", response.getResponseAsString());
    client.approveToken("user_data=hello-oauth");

    response = client.sendGet(FakeOAuthServiceProvider.ACCESS_TOKEN_URL);
    assertEquals("application/json; charset=utf-8", response.getHeader("Content-Type"));
    JSONObject json = new JSONObject(response.getResponseAsString());
    assertEquals("userid value", json.get("userid"));
    assertEquals("xoauth_stuff value", json.get("xoauth_stuff"));

    response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("User data is hello-oauth", response.getResponseAsString());
  }

  @Test
  public void testAccessTokenData_noOAuthParams() throws Exception {
    serviceProvider.setReturnAccessTokenData(true);

    MakeRequestClient client = makeNonSocialClient("owner", "owner", GADGET_URL);

    HttpResponse response = client.sendGet(FakeOAuthServiceProvider.ACCESS_TOKEN_URL);
    assertEquals("", response.getResponseAsString());
    client.approveToken("user_data=hello-oauth");

    response = client.sendGet(FakeOAuthServiceProvider.ACCESS_TOKEN_URL);
    JSONObject json = new JSONObject(response.getResponseAsString());
    assertEquals("userid value", json.get("userid"));
    assertEquals("xoauth_stuff value", json.get("xoauth_stuff"));
    assertEquals(2, json.length());

    response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("User data is hello-oauth", response.getResponseAsString());
  }

  @Test
  public void testAccessTokenData_noDirectRequest() throws Exception {
    serviceProvider.setReturnAccessTokenData(true);

    MakeRequestClient client = makeNonSocialClient("owner", "owner", GADGET_URL);

    HttpResponse response = client.sendGet(FakeOAuthServiceProvider.ACCESS_TOKEN_URL);
    assertEquals("", response.getResponseAsString());
    client.approveToken("user_data=hello-oauth");

    response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("User data is hello-oauth", response.getResponseAsString());

    response = client.sendGet(FakeOAuthServiceProvider.ACCESS_TOKEN_URL);
    assertEquals("", response.getResponseAsString());
    assertTrue(response.getMetadata().containsKey("oauthApprovalUrl"));
  }

  @Test
  public void testNextFetchReturnsNull() throws Exception {
    serviceProvider.setReturnNull(true);
    MakeRequestClient client = makeNonSocialClient("owner", "owner", GADGET_URL);
    HttpResponse response = client.sendGet(FakeOAuthServiceProvider.ACCESS_TOKEN_URL);
    assertEquals("UNKNOWN_PROBLEM", response.getMetadata().get("oauthError"));
    assertEquals("", response.getResponseAsString());
    String oauthErrorText = response.getMetadata().get("oauthErrorText");
    checkStringContains("should say no response", oauthErrorText, "No response from server");
    checkStringContains("should show request", oauthErrorText,
        "GET /request?param=foo&opensocial_owner_id=owner");
    checkStringContains("should log empty response", oauthErrorText, "Received response 1:\n\n");
    checkLogContains("No response from server");
    checkLogContains("GET /request?param=foo&opensocial_owner_id=owner");
    checkLogContains("OAuth error [UNKNOWN_PROBLEM, No response from server] for " +
        "application http://www.example.com/gadget.xml");
  }

  @Test
  public void testNextFetchThrowsGadgetException() throws Exception {
    serviceProvider.setThrow(
        new GadgetException(GadgetException.Code.FAILED_TO_RETRIEVE_CONTENT, "mildly wrong"));
    MakeRequestClient client = makeNonSocialClient("owner", "owner", GADGET_URL);
    HttpResponse response = client.sendGet(FakeOAuthServiceProvider.ACCESS_TOKEN_URL);
    assertEquals("UNKNOWN_PROBLEM", response.getMetadata().get("oauthError"));
    assertEquals("", response.getResponseAsString());
    String oauthErrorText = response.getMetadata().get("oauthErrorText");
    checkStringContains("should say no response", oauthErrorText, "No response from server");
    checkStringContains("should show request", oauthErrorText,
        "GET /request?param=foo&opensocial_owner_id=owner");
    checkStringContains("should log empty response", oauthErrorText, "Received response 1:\n\n");
    checkLogContains("No response from server");
    checkLogContains("GET /request?param=foo&opensocial_owner_id=owner");
    checkLogContains("OAuth error [UNKNOWN_PROBLEM, No response from server] for " +
        "application http://www.example.com/gadget.xml");
    checkLogContains("GadgetException");
    checkLogContains("mildly wrong");
  }

  @Test
  public void testNextFetchThrowsRuntimeException() throws Exception {
    serviceProvider.setThrow(new RuntimeException("very, very wrong"));
    MakeRequestClient client = makeNonSocialClient("owner", "owner", GADGET_URL);
    try {
      client.sendGet(FakeOAuthServiceProvider.ACCESS_TOKEN_URL);
      fail("Should have thrown");
    } catch (RuntimeException e) {
      // good
    }
    checkLogContains("OAuth fetch unexpected fatal erro");
    checkLogContains("GET /request?param=foo&opensocial_owner_id=owner");
    checkLogContains("OAuth error [null, null] for " +
        "application http://www.example.com/gadget.xml");
    checkLogContains("RuntimeException");
    checkLogContains("very, very wrong");
  }
  
  @Test
  public void testTrustedParams() throws Exception {
    serviceProvider.setCheckTrustedParams(true);
    MakeRequestClient client = makeNonSocialClient("owner", "owner", GADGET_URL);
    client.setTrustedParam("oauth_magic", "foo");
    client.setTrustedParam("opensocial_magic", "bar");
    client.setTrustedParam("xoauth_magic", "quux");

    HttpResponse response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("", response.getResponseAsString());
    client.approveToken("user_data=hello-oauth");

    response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("User data is hello-oauth", response.getResponseAsString());
    assertEquals(9, serviceProvider.getTrustedParamCount());
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
      if (record.getThrown() != null) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        record.getThrown().printStackTrace(pw);
        pw.flush();
        logText.append(sw.toString());
      }
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

  private void checkStringContains(String message, String text, String expected) {
    if (!text.contains(expected)) {
      fail(message + ", expected [" + expected + "], got + [" + text + "]");
    }
  }
}
