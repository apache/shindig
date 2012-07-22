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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.collect.Lists;

import net.oauth.OAuth;
import net.oauth.OAuth.Parameter;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.shindig.auth.BasicSecurityToken;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.crypto.BasicBlobCrypter;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.uri.UriBuilder;
import org.apache.shindig.common.util.CharsetUtil;
import org.apache.shindig.common.util.FakeTimeSource;
import org.apache.shindig.gadgets.FakeGadgetSpecFactory;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.GadgetSpecFactory;
import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.oauth.AccessorInfo.OAuthParamLocation;
import org.apache.shindig.gadgets.oauth.BasicOAuthStoreConsumerKeyAndSecret.KeyType;
import org.apache.shindig.gadgets.oauth.OAuthArguments.UseToken;
import org.apache.shindig.gadgets.oauth.testing.FakeOAuthServiceProvider;
import org.apache.shindig.gadgets.oauth.testing.MakeRequestClient;
import org.apache.shindig.gadgets.oauth.testing.FakeOAuthServiceProvider.TokenPair;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Tests for signing requests.
 */
public class OAuthRequestTest {

  private OAuthFetcherConfig fetcherConfig;
  private FakeOAuthServiceProvider serviceProvider;
  private OAuthCallbackGenerator callbackGenerator;
  private BasicOAuthStore base;
  private Logger logger;
  protected final List<LogRecord> logRecords = Lists.newArrayList();
  private final FakeTimeSource clock = new FakeTimeSource();

  public static final String GADGET_URL = "http://www.example.com/gadget.xml";
  public static final String GADGET_URL_NO_KEY = "http://www.example.com/nokey.xml";
  public static final String GADGET_URL_HEADER = "http://www.example.com/header.xml";
  public static final String GADGET_URL_BODY = "http://www.example.com/body.xml";
  public static final String GADGET_URL_BAD_OAUTH_URL = "http://www.example.com/badoauthurl.xml";
  public static final String GADGET_URL_APPROVAL_PARAMS =
      "http://www.example.com/approvalparams.xml";
  public static final String GADGET_MAKE_REQUEST_URL =
      "http://127.0.0.1/gadgets/makeRequest?params=foo";

  @Before
  public void setUp() throws Exception {
    base = new BasicOAuthStore();
    base.setDefaultCallbackUrl(GadgetTokenStoreTest.DEFAULT_CALLBACK);
    serviceProvider = new FakeOAuthServiceProvider(clock);
    callbackGenerator = createNullCallbackGenerator();
    fetcherConfig = new OAuthFetcherConfig(
        new BasicBlobCrypter("abcdefghijklmnop".getBytes()),
        getOAuthStore(base),
        clock,
        callbackGenerator,
        false);

    logger = Logger.getLogger(OAuthResponseParams.class.getName());
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
    logger.setLevel(Level.FINE);
  }

  private OAuthCallbackGenerator createNullCallbackGenerator() {
    return new OAuthCallbackGenerator() {
      public String generateCallback(OAuthFetcherConfig fetcherConfig, String baseCallback,
          HttpRequest request, OAuthResponseParams responseParams) {
        return null;
      }
    };
  }

  private OAuthCallbackGenerator createRealCallbackGenerator() {
    return new OAuthCallbackGenerator() {
      public String generateCallback(OAuthFetcherConfig fetcherConfig, String baseCallback,
          HttpRequest request, OAuthResponseParams responseParams) {
        SecurityToken st = request.getSecurityToken();
        Uri activeUrl = Uri.parse(st.getActiveUrl());
        assertEquals(GADGET_MAKE_REQUEST_URL, activeUrl.toString());
        assertEquals(GadgetTokenStoreTest.DEFAULT_CALLBACK, baseCallback);
        return new UriBuilder()
            .setScheme("http")
            .setAuthority(activeUrl.getAuthority())
            .setPath("/realcallback")
            .toString();
      }
    };
  }

  /**
   * Builds a nicely populated fake token store.
   */
  public GadgetOAuthTokenStore getOAuthStore(BasicOAuthStore base) {
    return getOAuthStore(base, new FakeGadgetSpecFactory());
  }

  private GadgetOAuthTokenStore getOAuthStore(BasicOAuthStore base,
      GadgetSpecFactory specFactory) {
    if (base == null) {
      base = new BasicOAuthStore();
      base.setDefaultCallbackUrl(GadgetTokenStoreTest.DEFAULT_CALLBACK);
    }
    addValidConsumer(base);
    addInvalidConsumer(base);
    addAuthHeaderConsumer(base);
    addBodyConsumer(base);
    addBadOAuthUrlConsumer(base);
    addApprovalParamsConsumer(base);
    addDefaultKey(base);
    return new GadgetOAuthTokenStore(base, specFactory);
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
        consumerKey, consumerSecret, KeyType.HMAC_SYMMETRIC, null, null);

    base.setConsumerKeyAndSecret(providerKey, kas);
  }

  private static void addDefaultKey(BasicOAuthStore base) {
    BasicOAuthStoreConsumerKeyAndSecret defaultKey = new BasicOAuthStoreConsumerKeyAndSecret(
        "signedfetch", FakeOAuthServiceProvider.PRIVATE_KEY_TEXT, KeyType.RSA_PRIVATE, "foo", null);
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
    return new BasicSecurityToken(owner, viewer, "app", "container.com", gadget, "0", "default",
        GADGET_MAKE_REQUEST_URL, null);
  }

  @After
  public void tearDown() throws Exception {
  }

  /** Client that does OAuth and sends opensocial_* params */
  private MakeRequestClient makeNonSocialClient(String owner, String viewer, String gadget)
      throws Exception {
    SecurityToken securityToken = getSecurityToken(owner, viewer, gadget);
    serviceProvider.setExpectedRequestSecurityToken( securityToken );
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
    serviceProvider.setExpectedRequestSecurityToken( securityToken );
    return new MakeRequestClient(securityToken, fetcherConfig, serviceProvider,
        FakeGadgetSpecFactory.SERVICE_NAME);
  }

  private MakeRequestClient makeSocialOAuthClient(String owner, String viewer, String gadget)
      throws Exception {
    SecurityToken securityToken = getSecurityToken(owner, viewer, gadget);
    serviceProvider.setExpectedRequestSecurityToken( securityToken );
    MakeRequestClient client = new MakeRequestClient(securityToken, fetcherConfig, serviceProvider,
        FakeGadgetSpecFactory.SERVICE_NAME);
    client.getBaseArgs().setUseToken(UseToken.IF_AVAILABLE);
    return client;
  }

  private MakeRequestClient makeSignedFetchClient(String owner, String viewer, String gadget)
      throws Exception {
    SecurityToken securityToken = getSecurityToken(owner, viewer, gadget);
    serviceProvider.setExpectedRequestSecurityToken( securityToken );
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
  public void testOAuthFlow_withCallbackVerifier() throws Exception {
    fetcherConfig = new OAuthFetcherConfig(
        new BasicBlobCrypter("abcdefghijklmnop".getBytes()),
        getOAuthStore(base),
        clock,
        createRealCallbackGenerator(),
        false);
    MakeRequestClient client = makeNonSocialClient("owner", "owner", GADGET_URL);

    HttpResponse response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("", response.getResponseAsString());
    client.approveToken("user_data=hello-oauth");

    response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("User data is hello-oauth", response.getResponseAsString());
    checkEmptyLog();
  }

  @Test
  public void testOAuthFlow_badCallbackVerifier() throws Exception {
    fetcherConfig = new OAuthFetcherConfig(
        new BasicBlobCrypter("abcdefghijklmnop".getBytes()),
        getOAuthStore(base),
        clock,
        createRealCallbackGenerator(),
        false);
    MakeRequestClient client = makeNonSocialClient("owner", "owner", GADGET_URL);

    HttpResponse response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("", response.getResponseAsString());

    client.approveToken("user_data=hello-oauth");
    client.setReceivedCallbackUrl("nonsense");
    response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("", response.getResponseAsString());
    assertNotNull(response.getMetadata().get("oauthErrorText"));

    client.approveToken("user_data=try-again");
    response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("User data is try-again", response.getResponseAsString());
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
    assertEquals(-1, response.getCacheTtl());
    assertEquals(OAuthError.UNAUTHENTICATED.name(), response.getMetadata().get("oauthError"));
  }

  @Test
  public void testOAuthFlow_noViewer() throws Exception {
    for (boolean secureOwner : Arrays.asList(true, false)) {
      // Test both with/without secure owner pages
      fetcherConfig = new OAuthFetcherConfig(
        new BasicBlobCrypter("abcdefghijklmnop".getBytes()),
        getOAuthStore(base),
        clock, callbackGenerator,
        secureOwner);

      MakeRequestClient client = makeNonSocialClient("owner", null, GADGET_URL);

      HttpResponse response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
      assertEquals("", response.getResponseAsString());
      assertEquals(403, response.getHttpStatusCode());
      assertEquals(-1, response.getCacheTtl());
      assertEquals(OAuthError.UNAUTHENTICATED.name(), response.getMetadata().get("oauthError"));
    }
  }

  @Test
  public void testOAuthFlow_noSpec() throws Exception {
    fetcherConfig = new OAuthFetcherConfig(
        new BasicBlobCrypter("abcdefghijklmnop".getBytes()),
        getOAuthStore(base, null),
        clock, callbackGenerator,
        false);

    MakeRequestClient client = makeNonSocialClient("owner", "owner", GADGET_URL);
    setNoSpecOptions(client);

    HttpResponse response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("", response.getResponseAsString());
    client.approveToken("user_data=hello-oauth");

    response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("User data is hello-oauth", response.getResponseAsString());
    checkEmptyLog();
  }

  private void setNoSpecOptions(MakeRequestClient client) {
    client.getBaseArgs().setRequestOption(OAuthArguments.PROGRAMMATIC_CONFIG_PARAM, "true");
    client.getBaseArgs().setRequestOption(OAuthArguments.PARAM_LOCATION_PARAM, "uri-query");
    client.getBaseArgs().setRequestOption(OAuthArguments.REQUEST_METHOD_PARAM, "GET");
    client.getBaseArgs().setRequestOption(OAuthArguments.REQUEST_TOKEN_URL_PARAM,
        FakeOAuthServiceProvider.REQUEST_TOKEN_URL);
    client.getBaseArgs().setRequestOption(OAuthArguments.ACCESS_TOKEN_URL_PARAM,
        FakeOAuthServiceProvider.ACCESS_TOKEN_URL);
    client.getBaseArgs().setRequestOption(OAuthArguments.AUTHORIZATION_URL_PARAM,
        FakeOAuthServiceProvider.APPROVAL_URL);
  }

  @Test
  public void testOAuthFlow_noSpecNoRequestTokenUrl() throws Exception {
    fetcherConfig = new OAuthFetcherConfig(
        new BasicBlobCrypter("abcdefghijklmnop".getBytes()),
        getOAuthStore(base, null),
        clock, null, false);

    MakeRequestClient client = makeNonSocialClient("owner", "owner", GADGET_URL);
    setNoSpecOptions(client);
    client.getBaseArgs().removeRequestOption(OAuthArguments.REQUEST_TOKEN_URL_PARAM);

    HttpResponse response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("", response.getResponseAsString());
    assertEquals(403, response.getHttpStatusCode());
    assertEquals(OAuthError.BAD_OAUTH_TOKEN_URL.name(),
        response.getMetadata().get("oauthError"));
    String errorText = response.getMetadata().get("oauthErrorText");
    assertNotNull(errorText);
    checkStringContains("should report no request token url", errorText,
        "No request token URL specified");
  }

  @Test
  public void testOAuthFlow_noSpecNoAccessTokenUrl() throws Exception {
    fetcherConfig = new OAuthFetcherConfig(
        new BasicBlobCrypter("abcdefghijklmnop".getBytes()),
        getOAuthStore(base, null),
        clock, callbackGenerator, false);

    MakeRequestClient client = makeNonSocialClient("owner", "owner", GADGET_URL);
    setNoSpecOptions(client);
    client.getBaseArgs().removeRequestOption(OAuthArguments.ACCESS_TOKEN_URL_PARAM);

    // Get the request token
    HttpResponse response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);

    // try to swap for access token
    response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);

    assertEquals("", response.getResponseAsString());
    assertEquals(403, response.getHttpStatusCode());
    assertEquals(OAuthError.BAD_OAUTH_TOKEN_URL.name(),
        response.getMetadata().get("oauthError"));
    String errorText = response.getMetadata().get("oauthErrorText");
    assertNotNull(errorText);
    checkStringContains("should report no access token url", errorText,
        "No access token URL specified");
  }

  @Test
  public void testOAuthFlow_noSpecNoApprovalUrl() throws Exception {
    fetcherConfig = new OAuthFetcherConfig(
        new BasicBlobCrypter("abcdefghijklmnop".getBytes()),
        getOAuthStore(base, null),
        clock, callbackGenerator, false);

    MakeRequestClient client = makeNonSocialClient("owner", "owner", GADGET_URL);
    setNoSpecOptions(client);
    client.getBaseArgs().removeRequestOption(OAuthArguments.AUTHORIZATION_URL_PARAM);

    HttpResponse response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);

    assertEquals("", response.getResponseAsString());
    assertEquals(403, response.getHttpStatusCode());
    assertEquals(OAuthError.BAD_OAUTH_TOKEN_URL.name(),
        response.getMetadata().get("oauthError"));
    String errorText = response.getMetadata().get("oauthErrorText");
    assertNotNull(errorText);
    checkStringContains("should report no authorization url", errorText,
        "No authorization URL specified");
  }

  @Test
  public void testOAuthFlow_noSpecAuthHeader() throws Exception {
    serviceProvider.setParamLocation(OAuthParamLocation.AUTH_HEADER);
    fetcherConfig = new OAuthFetcherConfig(
        new BasicBlobCrypter("abcdefghijklmnop".getBytes()),
        getOAuthStore(base, null),
        clock, callbackGenerator, false);

    MakeRequestClient client = makeNonSocialClient("owner", "owner", GADGET_URL);
    setNoSpecOptions(client);
    client.getBaseArgs().setRequestOption(OAuthArguments.PARAM_LOCATION_PARAM, "auth-header");

    HttpResponse response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("", response.getResponseAsString());
    client.approveToken("user_data=hello-oauth");

    response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("User data is hello-oauth", response.getResponseAsString());
    checkEmptyLog();
  }

  @Test
  public void testOAuthFlow_noSpecPostBody() throws Exception {
    serviceProvider.setParamLocation(OAuthParamLocation.POST_BODY);
    fetcherConfig = new OAuthFetcherConfig(
        new BasicBlobCrypter("abcdefghijklmnop".getBytes()),
        getOAuthStore(base, null),
        clock, callbackGenerator, false);

    MakeRequestClient client = makeNonSocialClient("owner", "owner", GADGET_URL);
    setNoSpecOptions(client);
    client.getBaseArgs().setRequestOption(OAuthArguments.REQUEST_METHOD_PARAM, "POST");
    client.getBaseArgs().setRequestOption(OAuthArguments.PARAM_LOCATION_PARAM, "post-body");

    HttpResponse response = client.sendFormPost(FakeOAuthServiceProvider.RESOURCE_URL, "");
    assertEquals("", response.getResponseAsString());
    client.approveToken("user_data=hello-oauth");

    response = client.sendFormPost(FakeOAuthServiceProvider.RESOURCE_URL, "");
    assertEquals("User data is hello-oauth", response.getResponseAsString());
    checkEmptyLog();
  }

  @Test
  public void testOAuthFlow_noSpecPostBodyAndHeader() throws Exception {
    serviceProvider.setParamLocation(OAuthParamLocation.POST_BODY);
    serviceProvider.addParamLocation(OAuthParamLocation.AUTH_HEADER);
    fetcherConfig = new OAuthFetcherConfig(
        new BasicBlobCrypter("abcdefghijklmnop".getBytes()),
        getOAuthStore(base, null),
        clock, callbackGenerator, false);

    MakeRequestClient client = makeNonSocialClient("owner", "owner", GADGET_URL);
    setNoSpecOptions(client);
    client.getBaseArgs().setRequestOption(OAuthArguments.REQUEST_METHOD_PARAM, "POST");
    client.getBaseArgs().setRequestOption(OAuthArguments.PARAM_LOCATION_PARAM, "post-body");

    HttpResponse response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("", response.getResponseAsString());
    client.approveToken("user_data=hello-oauth");

    response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("User data is hello-oauth", response.getResponseAsString());
    checkEmptyLog();
  }

  @Test
  public void testOAuthFlow_noSpecInvalidUrl() throws Exception {
    fetcherConfig = new OAuthFetcherConfig(
        new BasicBlobCrypter("abcdefghijklmnop".getBytes()),
        getOAuthStore(base, null),
        clock, null, false);

    MakeRequestClient client = makeNonSocialClient("owner", "owner", GADGET_URL);
    setNoSpecOptions(client);
    client.getBaseArgs().setRequestOption(OAuthArguments.REQUEST_TOKEN_URL_PARAM, "foo");

    HttpResponse response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("", response.getResponseAsString());
    assertEquals(403, response.getHttpStatusCode());
    assertEquals(OAuthError.INVALID_URL.name(),
        response.getMetadata().get("oauthError"));
    String errorText = response.getMetadata().get("oauthErrorText");
    assertNotNull(errorText);
    checkStringContains("should report invalid url", errorText, "Invalid URL: foo");
  }

  @Test
  public void testOAuthFlow_noSpecBlankUrl() throws Exception {
    fetcherConfig = new OAuthFetcherConfig(
        new BasicBlobCrypter("abcdefghijklmnop".getBytes()),
        getOAuthStore(base, null),
        clock, null, false);

    MakeRequestClient client = makeNonSocialClient("owner", "owner", GADGET_URL);
    setNoSpecOptions(client);
    client.getBaseArgs().setRequestOption(OAuthArguments.REQUEST_TOKEN_URL_PARAM, "");

    HttpResponse response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("", response.getResponseAsString());
    assertEquals(403, response.getHttpStatusCode());
    assertEquals(OAuthError.INVALID_URL.name(),
        response.getMetadata().get("oauthError"));
    String errorText = response.getMetadata().get("oauthErrorText");
    assertNotNull(errorText);
    checkStringContains("should report invalid url", errorText, "Invalid URL: ");
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
    assertEquals(OAuthError.NOT_OWNER.name(), response.getMetadata().get("oauthError"));
  }

  @Test
  public void testAccessTokenOkForSecureOwnerPage() throws Exception {
    fetcherConfig = new OAuthFetcherConfig(
        new BasicBlobCrypter("abcdefghijklmnop".getBytes()),
        getOAuthStore(base),
        clock,
        callbackGenerator,
        true);

    MakeRequestClient client = makeNonSocialClient("owner", "owner", GADGET_URL);

    HttpResponse response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("", response.getResponseAsString());
    client.approveToken("user_data=hello-oauth");

    response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("User data is hello-oauth", response.getResponseAsString());

    MakeRequestClient friend = makeNonSocialClient("owner", "friend", GADGET_URL);
    response = friend.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("", response.getResponseAsString());
    assertEquals(200, response.getHttpStatusCode());
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
    Assert.assertNotSame("azn header: " + aznHeader, aznHeader.indexOf("OAuth"), -1);
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
    Assert.assertNotSame("body: " + echoedBody, echoedBody.indexOf("oauth_consumer_key="), -1);
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
    Assert.assertNotSame("body: " + echoedBody, echoedBody.indexOf("oauth_consumer_key="), -1);
    Assert.assertNotSame("body: " + echoedBody, echoedBody.indexOf("foo=bar&foo=baz"), -1);
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
    Assert.assertNotSame("azn header: " + aznHeader, aznHeader.indexOf("OAuth"), -1);
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
    assertEquals(HttpResponse.SC_FORBIDDEN, response.getHttpStatusCode());
    assertEquals("parameter_absent", response.getMetadata().get("oauthError"));
    assertNull(response.getMetadata().get("oauthApprovalUrl"));
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
    checkStringContains("should return response", errorText, "HTTP/1.1 401");
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
    assertEquals("MISSING_OAUTH_PARAMETER", metadata.get("oauthError"));
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
    assertEquals("MISSING_OAUTH_PARAMETER", metadata.get("oauthError"));
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
    Assert.assertSame(approvalUrl, 0, approvalUrl.indexOf(
        "http://www.example.com/authorize?oauth_callback=foo&oauth_token="));
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
    assertEquals("MISSING_OAUTH_PARAMETER", metadata.get("oauthError"));
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
  public void testError400() throws Exception {
    MakeRequestClient client = makeNonSocialClient("owner", "owner", GADGET_URL);

    HttpResponse response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("", response.getResponseAsString());
    client.approveToken("user_data=hello-oauth");

    response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL + "?cachebust=1");
    assertEquals("User data is hello-oauth", response.getResponseAsString());

    response = client.sendGet(FakeOAuthServiceProvider.ERROR_400);
    assertEquals("bad request", response.getResponseAsString());
    assertEquals(400, response.getHttpStatusCode());

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
        "GET /data?cachebust=2&oauth_body_hash=2jm");

    assertEquals(1, serviceProvider.getRequestTokenCount());
    assertEquals(1, serviceProvider.getAccessTokenCount());
    assertEquals(3, serviceProvider.getResourceAccessCount());

    serviceProvider.setConsumersThrottled(false);
    client.clearState();
    response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL + "?cachebust=3");
    assertEquals("User data is hello-oauth", response.getResponseAsString());

    assertEquals(1, serviceProvider.getRequestTokenCount());
    assertEquals(1, serviceProvider.getAccessTokenCount());
    assertEquals(4, serviceProvider.getResourceAccessCount());
  }

  @Test
  public void testConsumerThrottled_vagueErrors() throws Exception {
    serviceProvider.setVagueErrors(true);
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
    assertEquals(403, response.getHttpStatusCode());
    assertEquals("some vague error", response.getResponseAsString());
    Map<String, String> metadata = response.getMetadata();
    assertNotNull(metadata);
    assertNull(metadata.get("oauthError"));
    checkStringContains("oauthErrorText missing request entry", metadata.get("oauthErrorText"),
        "GET /data?cachebust=2\n");
    checkStringContains("oauthErrorText missing request entry", metadata.get("oauthErrorText"),
        "GET /data?cachebust=2&oauth_body_hash=2jm");

    assertEquals(1, serviceProvider.getRequestTokenCount());
    assertEquals(1, serviceProvider.getAccessTokenCount());
    assertEquals(3, serviceProvider.getResourceAccessCount());

    serviceProvider.setConsumersThrottled(false);

    client.clearState(); // remove any cached oauth tokens

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
    assertTrue(contains(queryParams, "xoauth_public_key", "foo"));
    assertFalse(contains(queryParams, "opensocial_proxied_content", "1"));
  }

  @Test
  public void testSignedFetch_authHeader() throws Exception {
    serviceProvider.setParamLocation(OAuthParamLocation.AUTH_HEADER);
    MakeRequestClient client = makeSignedFetchClient("o", "v", "http://www.example.com/app");
    client.getBaseArgs().setRequestOption(OAuthArguments.PROGRAMMATIC_CONFIG_PARAM, "true");
    client.getBaseArgs().setRequestOption(OAuthArguments.PARAM_LOCATION_PARAM, "auth-header");

    HttpResponse resp = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    String auth = resp.getHeader(FakeOAuthServiceProvider.AUTHZ_ECHO_HEADER);
    assertNotNull("Should have echoed authz header", auth);
    checkStringContains("should have opensocial params in header", auth,
        "opensocial_owner_id=\"o\"");
  }

  @Test
  public void testSignedFetchParametersSetProxiedContent() throws Exception {
    MakeRequestClient client = makeSignedFetchClient("o", "v", "http://www.example.com/app");
    client.getBaseArgs().setProxiedContentRequest(true);
    HttpResponse resp = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    List<Parameter> queryParams = OAuth.decodeForm(resp.getResponseAsString());
    assertTrue(contains(queryParams, "opensocial_owner_id", "o"));
    assertTrue(contains(queryParams, "opensocial_viewer_id", "v"));
    assertTrue(contains(queryParams, "opensocial_app_id", "app"));
    assertTrue(contains(queryParams, OAuth.OAUTH_CONSUMER_KEY, "signedfetch"));
    assertTrue(contains(queryParams, "xoauth_signature_publickey", "foo"));
    assertTrue(contains(queryParams, "xoauth_public_key", "foo"));
    assertTrue(contains(queryParams, "opensocial_proxied_content", "1"));
  }

  @Test
  public void testPostBinaryData() throws Exception {
    byte[] raw = { 0, 1, 2, 3, 4, 5 };
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
    byte[] raw = { 0, 1, 2, 3, 4, 5 };
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
  public void testGetWithFormEncodedBody() throws Exception {
    MakeRequestClient client = makeSignedFetchClient("o", "v", "http://www.example.com/app");
    HttpResponse resp = client.sendGetWithBody(FakeOAuthServiceProvider.RESOURCE_URL,
        OAuth.FORM_ENCODED, "war=peace&yes=no".getBytes());
    assertEquals("war=peace&yes=no", resp.getHeader(FakeOAuthServiceProvider.BODY_ECHO_HEADER));
  }

  @Test
  public void testGetWithRawBody() throws Exception {
    MakeRequestClient client = makeSignedFetchClient("o", "v", "http://www.example.com/app");
    HttpResponse resp = client.sendGetWithBody(FakeOAuthServiceProvider.RESOURCE_URL,
        "application/json", "war=peace&yes=no".getBytes());
    assertEquals("war=peace&yes=no", resp.getHeader(FakeOAuthServiceProvider.BODY_ECHO_HEADER));
    List<Parameter> queryParams = OAuth.decodeForm(resp.getResponseAsString());
    checkContains(queryParams, "oauth_body_hash", "MfhwxPN6ns5CwQAZN9OcJXu3Jv4=");
  }

  @Test
  public void testGetTamperedRawContent() throws Exception {
    byte[] raw = { 0, 1, 2, 3, 4, 5 };
    MakeRequestClient client = makeSignedFetchClient("o", "v", "http://www.example.com/app");
    // Tamper with the body before it hits the service provider
    client.setNextFetcher(new HttpFetcher() {
      public HttpResponse fetch(HttpRequest request) throws GadgetException {
        request.setPostBody("yo momma".getBytes());
        return serviceProvider.fetch(request);
      }
    });
    try {
      client.sendGetWithBody(FakeOAuthServiceProvider.RESOURCE_URL,
          "funky-content", raw);
      fail("Should have thrown with oauth_body_hash mismatch");
    } catch (RuntimeException e) {
      // good
    }
  }

  @Test(expected=RuntimeException.class)
  public void testGetTamperedFormContent() throws Exception {
    MakeRequestClient client = makeSignedFetchClient("o", "v", "http://www.example.com/app");
    // Tamper with the body before it hits the service provider
    client.setNextFetcher(new HttpFetcher() {
      public HttpResponse fetch(HttpRequest request) throws GadgetException {
        request.setPostBody("foo=quux".getBytes());
        return serviceProvider.fetch(request);
      }
    });
    client.sendGetWithBody(FakeOAuthServiceProvider.RESOURCE_URL,
        OAuth.FORM_ENCODED, "foo=bar".getBytes());
    fail("Should have thrown with oauth signature mismatch");
  }

  @Test(expected=RuntimeException.class)
  public void testGetTamperedRemoveRawContent() throws Exception {
    byte[] raw = { 0, 1, 2, 3, 4, 5 };
    MakeRequestClient client = makeSignedFetchClient("o", "v", "http://www.example.com/app");
    // Tamper with the body before it hits the service provider
    client.setNextFetcher(new HttpFetcher() {
      public HttpResponse fetch(HttpRequest request) throws GadgetException {
        request.setPostBody(ArrayUtils.EMPTY_BYTE_ARRAY);
        request.setHeader("Content-Type", "application/x-www-form-urlencoded");
        return serviceProvider.fetch(request);
      }
    });
    client.sendGetWithBody(FakeOAuthServiceProvider.RESOURCE_URL,
        "funky-content", raw);
    fail("Should have thrown with body hash in form encoded request");
  }

  @Test(expected=RuntimeException.class)
  public void testPostTamperedRawContent() throws Exception {
    byte[] raw = { 0, 1, 2, 3, 4, 5 };
    MakeRequestClient client = makeSignedFetchClient("o", "v", "http://www.example.com/app");
    // Tamper with the body before it hits the service provider
    client.setNextFetcher(new HttpFetcher() {
      public HttpResponse fetch(HttpRequest request) throws GadgetException {
        request.setPostBody("yo momma".getBytes());
        return serviceProvider.fetch(request);
      }
    });
    client.sendRawPost(FakeOAuthServiceProvider.RESOURCE_URL,
       "funky-content", raw);
    fail("Should have thrown with oauth_body_hash mismatch");
  }

  @Test(expected=RuntimeException.class)
  public void testPostTamperedFormContent() throws Exception {
    MakeRequestClient client = makeSignedFetchClient("o", "v", "http://www.example.com/app");
    // Tamper with the body before it hits the service provider
    client.setNextFetcher(new HttpFetcher() {
      public HttpResponse fetch(HttpRequest request) throws GadgetException {
        request.setPostBody("foo=quux".getBytes());
        return serviceProvider.fetch(request);
      }
    });
    client.sendFormPost(FakeOAuthServiceProvider.RESOURCE_URL, "foo=bar");
    fail("Should have thrown with oauth signature mismatch");
  }

  @Test(expected=RuntimeException.class)
  public void testPostTamperedRemoveRawContent() throws Exception {
    byte[] raw = { 0, 1, 2, 3, 4, 5 };
    MakeRequestClient client = makeSignedFetchClient("o", "v", "http://www.example.com/app");
    // Tamper with the body before it hits the service provider
    client.setNextFetcher(new HttpFetcher() {
      public HttpResponse fetch(HttpRequest request) throws GadgetException {
        request.setPostBody(ArrayUtils.EMPTY_BYTE_ARRAY);
        request.setHeader("Content-Type", "application/x-www-form-urlencoded");
        return serviceProvider.fetch(request);
      }
    });
    client.sendRawPost(FakeOAuthServiceProvider.RESOURCE_URL,
        "funky-content", raw);
    fail("Should have thrown with body hash in form encoded request");
  }

  @Test
  public void testSignedFetch_error401() throws Exception {
    assertEquals(0, base.getAccessTokenRemoveCount());
    serviceProvider.setConsumerUnauthorized(true);
    serviceProvider.setVagueErrors(true);
    MakeRequestClient client = makeSignedFetchClient("o", "v", "http://www.example.com/app");

    HttpResponse response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertNull(response.getMetadata().get("oauthError"));
    String errorText = response.getMetadata().get("oauthErrorText");
    checkStringContains("Should return sent request", errorText, "GET /data");
    checkStringContains("Should return response", errorText, "HTTP/1.1 401");
    checkStringContains("Should return response", errorText, "some vague error");
    assertEquals(0, base.getAccessTokenRemoveCount());
  }

  @Test
  public void testSignedFetch_error403() throws Exception {
    assertEquals(0, base.getAccessTokenRemoveCount());
    serviceProvider.setConsumersThrottled(true);
    serviceProvider.setVagueErrors(true);
    MakeRequestClient client = makeSignedFetchClient("o", "v", "http://www.example.com/app");

    HttpResponse response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertNull(response.getMetadata().get("oauthError"));
    String errorText = response.getMetadata().get("oauthErrorText");
    checkStringContains("Should return sent request", errorText, "GET /data");
    checkStringContains("Should return response", errorText, "HTTP/1.1 403");
    checkStringContains("Should return response", errorText, "some vague error");
    assertEquals(0, base.getAccessTokenRemoveCount());
  }

  @Test
  public void testSignedFetch_unnamedConsumerKey() throws Exception {
    BasicOAuthStoreConsumerKeyAndSecret defaultKey = new BasicOAuthStoreConsumerKeyAndSecret(
        null, FakeOAuthServiceProvider.PRIVATE_KEY_TEXT, KeyType.RSA_PRIVATE, "foo", null);
    base.setDefaultKey(defaultKey);
    MakeRequestClient client = makeSignedFetchClient("o", "v", "http://www.example.com/app");
    HttpResponse resp = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    List<Parameter> queryParams = OAuth.decodeForm(resp.getResponseAsString());
    assertTrue(contains(queryParams, "opensocial_owner_id", "o"));
    assertTrue(contains(queryParams, "opensocial_viewer_id", "v"));
    assertTrue(contains(queryParams, "opensocial_app_id", "app"));
    assertTrue(contains(queryParams, OAuth.OAUTH_CONSUMER_KEY, "container.com"));
    assertTrue(contains(queryParams, "xoauth_signature_publickey", "foo"));
    assertTrue(contains(queryParams, "xoauth_public_key", "foo"));
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
    assertTrue(contains(queryParams, "xoauth_public_key", "foo"));
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
    HttpResponse resp = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL + '?' + tricky);
    assertEquals(OAuthError.INVALID_PARAMETER.name(),
        resp.getMetadata().get(OAuthResponseParams.ERROR_CODE));
    checkStringContains("Wrong error text", resp.getMetadata().get("oauthErrorText"),
        "Invalid parameter name opensocial_owner_id, applications may not override " +
        "oauth, xoauth, or opensocial parameters");
  }

  @Test
  public void testTrickyParametersInBody() throws Exception {
    MakeRequestClient client = makeSignedFetchClient("o", "v", "http://www.example.com/app");
    String tricky = "%6fpensocial_owner_id=gotcha";
    HttpResponse resp = client.sendFormPost(FakeOAuthServiceProvider.RESOURCE_URL, tricky);
    assertEquals(OAuthError.INVALID_PARAMETER.name(),
        resp.getMetadata().get(OAuthResponseParams.ERROR_CODE));
    checkStringContains("Wrong error text", resp.getMetadata().get("oauthErrorText"),
        "Invalid parameter name opensocial_owner_id, applications may not override " +
        "oauth, xoauth, or opensocial parameters");
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
        FakeOAuthServiceProvider.RESOURCE_URL + '?' + weird + "=foo");
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
    assertEquals(OAuthError.INVALID_PARAMETER.name(),
        resp.getMetadata().get(OAuthResponseParams.ERROR_CODE));
    checkStringContains("Wrong error text", resp.getMetadata().get("oauthErrorText"),
        "Invalid parameter name opensocial_foo");
  }

  @Test
  public void testStripOAuthParamsFromQuery() throws Exception {
    MakeRequestClient client = makeSignedFetchClient("o", "v", "http://www.example.com/app");
    HttpResponse resp =
        client.sendFormPost(FakeOAuthServiceProvider.RESOURCE_URL + "?oauth_foo=bar", "name=value");
    assertEquals(OAuthError.INVALID_PARAMETER.name(),
        resp.getMetadata().get(OAuthResponseParams.ERROR_CODE));
    checkStringContains("Wrong error text", resp.getMetadata().get("oauthErrorText"),
        "Invalid parameter name oauth_foo");
  }

  @Test
  public void testStripOpenSocialParamsFromBody() throws Exception {
    MakeRequestClient client = makeSignedFetchClient("o", "v", "http://www.example.com/app");
    HttpResponse resp =
        client.sendFormPost(FakeOAuthServiceProvider.RESOURCE_URL, "opensocial_foo=bar");
    assertEquals(OAuthError.INVALID_PARAMETER.name(),
        resp.getMetadata().get(OAuthResponseParams.ERROR_CODE));
    checkStringContains("Wrong error text", resp.getMetadata().get("oauthErrorText"),
        "Invalid parameter name opensocial_foo");
  }

  @Test
  public void testStripOAuthParamsFromBody() throws Exception {
    MakeRequestClient client = makeSignedFetchClient("o", "v", "http://www.example.com/app");
    HttpResponse resp = client.sendFormPost(FakeOAuthServiceProvider.RESOURCE_URL, "oauth_foo=bar");
    assertEquals(OAuthError.INVALID_PARAMETER.name(),
        resp.getMetadata().get(OAuthResponseParams.ERROR_CODE));
    checkStringContains("Wrong error text", resp.getMetadata().get("oauthErrorText"),
        "Invalid parameter name oauth_foo");
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
    assertEquals("application/json; charset=UTF-8", response.getHeader("Content-Type"));
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

  @Test(expected=RuntimeException.class)
  public void testAccessTokenData_noDirectRequest() throws Exception {
    serviceProvider.setReturnAccessTokenData(true);

    MakeRequestClient client = makeNonSocialClient("owner", "owner", GADGET_URL);

    HttpResponse response = client.sendGet(FakeOAuthServiceProvider.ACCESS_TOKEN_URL);
    assertEquals("", response.getResponseAsString());
    client.approveToken("user_data=hello-oauth");

    response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("User data is hello-oauth", response.getResponseAsString());

    client.sendGet(FakeOAuthServiceProvider.ACCESS_TOKEN_URL);
    fail("Service provider should have rejected bogus request to access token URL");
  }

  @Test
  public void testNextFetchReturnsNull() throws Exception {
    serviceProvider.setReturnNull(true);
    MakeRequestClient client = makeNonSocialClient("owner", "owner", GADGET_URL);
    HttpResponse response = client.sendGet(FakeOAuthServiceProvider.ACCESS_TOKEN_URL);
    assertEquals("MISSING_SERVER_RESPONSE", response.getMetadata().get("oauthError"));
    assertEquals("", response.getResponseAsString());
    String oauthErrorText = response.getMetadata().get("oauthErrorText");
    checkStringContains("should say no response", oauthErrorText, "No response from server");
    checkStringContains("should show request", oauthErrorText,
        "GET /request?param=foo&opensocial_owner_id=owner");
    checkStringContains("should log empty response", oauthErrorText, "Received response 1:\n\n");
    checkLogContains("No response from server");
    checkLogContains("GET /request?param=foo&opensocial_owner_id=owner");
    checkLogContains("OAuth error [MISSING_SERVER_RESPONSE, No response from server] for " +
        "application http://www.example.com/gadget.xml");
  }

  @Test
  public void testNextFetchThrowsGadgetException() throws Exception {
    serviceProvider.setThrow(
        new GadgetException(GadgetException.Code.FAILED_TO_RETRIEVE_CONTENT, "mildly wrong"));
    MakeRequestClient client = makeNonSocialClient("owner", "owner", GADGET_URL);
    HttpResponse response = client.sendGet(FakeOAuthServiceProvider.ACCESS_TOKEN_URL);
    assertEquals("MISSING_SERVER_RESPONSE", response.getMetadata().get("oauthError"));
    assertEquals("", response.getResponseAsString());
    String oauthErrorText = response.getMetadata().get("oauthErrorText");
    checkStringContains("should say no response", oauthErrorText, "No response from server");
    checkStringContains("should show request", oauthErrorText,
        "GET /request?param=foo&opensocial_owner_id=owner");
    checkStringContains("should log empty response", oauthErrorText, "Received response 1:\n\n");
    checkLogContains("No response from server");
    checkLogContains("GET /request?param=foo&opensocial_owner_id=owner");
    checkLogContains("OAuth error [MISSING_SERVER_RESPONSE, No response from server] for " +
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
    //checkLogContains("OAuth fetch unexpected fatal erro");
    checkLogContains("GET /request?param=foo&opensocial_owner_id=owner");
    checkLogContains("OAuth error [very, very wrong] for " +
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

    client.setTrustedParam("opensocial_owner_id", "overridden_opensocial_owner_id");

    HttpResponse response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("", response.getResponseAsString());
    client.approveToken("user_data=hello-oauth");

    response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("User data is hello-oauth", response.getResponseAsString());
    assertEquals(12, serviceProvider.getTrustedParamCount());
  }

  /**
   * Test different behaviors of trusted parameters.
   * 1) pass two parameters with same name, the latter will win.
   * 2) parameter name starting with 'oauth' 'oauth' or 'opensocial'.
   * 3) trusted parameter can override existing parameter.
   */
  @Test
  public void testTrustedParamsMisc() throws Exception {
    serviceProvider.setCheckTrustedParams(true);
    MakeRequestClient client = makeNonSocialClient("owner", "owner", GADGET_URL);
    client.setTrustedParam("oauth_magic", "foo");
    client.setTrustedParam("opensocial_magic", "bar");

    client.setTrustedParam("xoauth_magic", "quux_overridden");
    client.setTrustedParam("xoauth_magic", "quux");

    client.setTrustedParam("opensocial_owner_id", "overridden_opensocial_owner_id");

    HttpResponse response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("", response.getResponseAsString());
    client.approveToken("user_data=hello-oauth");

    response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("User data is hello-oauth", response.getResponseAsString());
    assertEquals(12, serviceProvider.getTrustedParamCount());
  }

  /**
   * Test trusted parameters will always be sent when signOwner and signViewer
   * are false.
   */
  @Test
  public void testAlwaysAppendTrustedParams() throws Exception {
    serviceProvider.setCheckTrustedParams(true);
    MakeRequestClient client = makeStrictNonSocialClient("owner", "owner", GADGET_URL);
    client.setTrustedParam("oauth_magic", "foo");
    client.setTrustedParam("opensocial_magic", "bar");
    client.setTrustedParam("xoauth_magic", "quux");

    client.setTrustedParam("opensocial_owner_id", "overridden_opensocial_owner_id");

    HttpResponse response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("", response.getResponseAsString());
    client.approveToken("user_data=hello-oauth");

    response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals("User data is hello-oauth", response.getResponseAsString());
    assertEquals(12, serviceProvider.getTrustedParamCount());
  }

  /**
   * Test invalid trusted parameters which are not prefixed with 'oauth' 'xoauth' or 'opensocial'.
   */
  @Test
  public void testTrustedParamsInvalidParameter() throws Exception {
    serviceProvider.setCheckTrustedParams(true);
    MakeRequestClient client = makeNonSocialClient("owner", "owner", GADGET_URL);
    client.setTrustedParam("oauth_magic", "foo");
    client.setTrustedParam("opensocial_magic", "bar");
    client.setTrustedParam("xoauth_magic", "quux");
    client.setTrustedParam("opensocial_owner_id", "overridden_opensocial_owner_id");
    client.setTrustedParam("invalid_trusted_parameter", "invalid");

    HttpResponse response = client.sendGet(FakeOAuthServiceProvider.RESOURCE_URL);
    assertEquals(HttpResponse.SC_FORBIDDEN, response.getHttpStatusCode());
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

  private void checkContains(List<Parameter> params, String key, String value) {
    for (Parameter p : params) {
      if (p.getKey().equals(key)) {
        assertEquals(value, p.getValue());
        return;
      }
    }
    fail("List did not contain " + key + '=' + value + "; instead was " + params);
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
    if ((logger.getLevel()!=null)&&(logger.getLevel().equals(Level.OFF))) {
        return;
    }
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
      fail(message + ", expected [" + expected + "], got + [" + text + ']');
    }
  }
}
