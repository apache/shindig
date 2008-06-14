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

import org.apache.shindig.common.BasicSecurityToken;
import org.apache.shindig.common.SecurityToken;
import org.apache.shindig.common.crypto.BasicBlobCrypter;
import org.apache.shindig.common.crypto.BlobCrypter;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.GadgetSpecFactory;
import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.Map;

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

  public static final String SERVICE_NAME = "testservice";
  public static final String SERVICE_NAME_NO_KEY = "nokey";
  public static final String GADGET_URL = "http://www.example.com/gadget.xml";
  public static final String GADGET_URL_NO_KEY =
      "http://www.example.com/nokey.xml";

  @Before
  public void setUp() throws Exception {
    serviceProvider = new FakeOAuthServiceProvider();
    tokenStore = getOAuthStore();
    blobCrypter = new BasicBlobCrypter("abcdefghijklmnop".getBytes());
  }

  /**
   * Builds a nicely populated fake token store.
   */
  public static GadgetOAuthTokenStore getOAuthStore() throws GadgetException {
    BasicOAuthStore base = new BasicOAuthStore();
    addValidConsumer(base);
    addInvalidConsumer(base);
    return new BasicGadgetOAuthTokenStore(base, new FakeGadgetSpecFactory());
  }

  private static void addValidConsumer(BasicOAuthStore base) {
    addConsumer(
        base,
        GADGET_URL,
        SERVICE_NAME,
        FakeOAuthServiceProvider.CONSUMER_KEY,
        FakeOAuthServiceProvider.CONSUMER_SECRET);
  }

  private static void addInvalidConsumer(BasicOAuthStore base) {
    addConsumer(base, GADGET_URL_NO_KEY, SERVICE_NAME_NO_KEY, "garbage_key",
        "garbage_secret");
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

  public HttpFetcher getFetcher(
      SecurityToken authToken, OAuthRequestParams params) throws GadgetException {
    OAuthFetcher fetcher = new OAuthFetcher(
        tokenStore, blobCrypter, serviceProvider, authToken, params);
    fetcher.init();
    return fetcher;
  }

  @Test
  public void testOAuthFlow() throws Exception {
    HttpFetcher fetcher;
    HttpRequest request;
    HttpResponse response;

    fetcher = getFetcher(
        getSecurityToken("owner", "owner"),
        new OAuthRequestParams(SERVICE_NAME, null, null, false));
    request = new HttpRequest(
        new URI(FakeOAuthServiceProvider.RESOURCE_URL));
    response = fetcher.fetch(request);
    String clientState = response.getMetadata().get("oauthState");
    assertNotNull(clientState);
    String approvalUrl = response.getMetadata().get("approvalUrl");
    assertNotNull(approvalUrl);

    serviceProvider.browserVisit(approvalUrl + "&user_data=hello-oauth");

    fetcher = getFetcher(
        getSecurityToken("owner", "owner"),
        new OAuthRequestParams(SERVICE_NAME, null, clientState, false));
    request = new HttpRequest(
        new URI(FakeOAuthServiceProvider.RESOURCE_URL));
    response = fetcher.fetch(request);
    assertEquals("User data is hello-oauth", response.getResponseAsString());

    fetcher = getFetcher(
        getSecurityToken("owner", "somebody else"),
        new OAuthRequestParams(SERVICE_NAME, null, null, false));
    request = new HttpRequest(
        new URI(FakeOAuthServiceProvider.RESOURCE_URL));
    response = fetcher.fetch(request);
    assertEquals("User data is hello-oauth", response.getResponseAsString());

    fetcher = getFetcher(
        getSecurityToken("somebody else", "somebody else"),
        new OAuthRequestParams(SERVICE_NAME, null, null, false));
    request = new HttpRequest(
        new URI(FakeOAuthServiceProvider.RESOURCE_URL));
    response = fetcher.fetch(request);
    clientState = response.getMetadata().get("oauthState");
    assertNotNull(clientState);
    approvalUrl = response.getMetadata().get("approvalUrl");
    assertNotNull(approvalUrl);

    serviceProvider.browserVisit(approvalUrl + "&user_data=somebody%20else");

    fetcher = getFetcher(
        getSecurityToken("somebody else", "somebody else"),
        new OAuthRequestParams(SERVICE_NAME, null, clientState, false));
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
        new OAuthRequestParams(SERVICE_NAME, null, null, false));
    request = new HttpRequest(
        new URI(FakeOAuthServiceProvider.RESOURCE_URL));
    response = fetcher.fetch(request);
    String clientState = response.getMetadata().get("oauthState");
    assertNotNull(clientState);
    String approvalUrl = response.getMetadata().get("approvalUrl");
    assertNotNull(approvalUrl);

    serviceProvider.browserVisit(approvalUrl + "&user_data=hello-oauth");

    fetcher = getFetcher(
        getSecurityToken("owner", "owner"),
        new OAuthRequestParams(SERVICE_NAME, null, clientState, false));
    request = new HttpRequest(
        new URI(FakeOAuthServiceProvider.RESOURCE_URL));
    response = fetcher.fetch(request);
    assertEquals("User data is hello-oauth", response.getResponseAsString());

    serviceProvider.revokeAllAccessTokens();

    fetcher = getFetcher(
        getSecurityToken("owner", "owner"),
        new OAuthRequestParams(SERVICE_NAME, null, clientState, false));
    request = new HttpRequest(
        new URI(FakeOAuthServiceProvider.RESOURCE_URL));
    response = fetcher.fetch(request);

    clientState = response.getMetadata().get("oauthState");
    assertNotNull(clientState);
    approvalUrl = response.getMetadata().get("approvalUrl");
    assertNotNull(approvalUrl);

    serviceProvider.browserVisit(approvalUrl + "&user_data=reapproved");
    fetcher = getFetcher(
        getSecurityToken("owner", "owner"),
        new OAuthRequestParams(SERVICE_NAME, null, clientState, false));
    request = new HttpRequest(
        new URI(FakeOAuthServiceProvider.RESOURCE_URL));
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
        new OAuthRequestParams(SERVICE_NAME_NO_KEY, null, null, false));
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
        new OAuthRequestParams(SERVICE_NAME, null, null, false));
    request = new HttpRequest(
        new URI(FakeOAuthServiceProvider.RESOURCE_URL));
    response = fetcher.fetch(request);
    clientState = response.getMetadata().get("oauthState");
    assertNotNull(clientState);
    String approvalUrl = response.getMetadata().get("approvalUrl");
    assertNotNull(approvalUrl);

    assertEquals(1, serviceProvider.getRequestTokenCount());
    assertEquals(0, serviceProvider.getAccessTokenCount());
    assertEquals(0, serviceProvider.getResourceAccessCount());

    serviceProvider.browserVisit(approvalUrl + "&user_data=hello-oauth");

    fetcher = getFetcher(
        getSecurityToken("owner", "owner"),
        new OAuthRequestParams(SERVICE_NAME, null, clientState, false));
    request = new HttpRequest(
        new URI(FakeOAuthServiceProvider.RESOURCE_URL));
    response = fetcher.fetch(request);
    assertEquals("User data is hello-oauth", response.getResponseAsString());
    clientState = response.getMetadata().get("oauthState");

    assertEquals(1, serviceProvider.getRequestTokenCount());
    assertEquals(1, serviceProvider.getAccessTokenCount());
    assertEquals(1, serviceProvider.getResourceAccessCount());

    fetcher = getFetcher(
        getSecurityToken("owner", "owner"),
        new OAuthRequestParams(SERVICE_NAME, null, clientState, false));
    request = new HttpRequest(
        new URI(FakeOAuthServiceProvider.RESOURCE_URL));
    response = fetcher.fetch(request);
    assertEquals("User data is hello-oauth", response.getResponseAsString());
    clientState = response.getMetadata().get("oauthState");

    assertEquals(1, serviceProvider.getRequestTokenCount());
    assertEquals(1, serviceProvider.getAccessTokenCount());
    assertEquals(2, serviceProvider.getResourceAccessCount());

    serviceProvider.setConsumersThrottled(true);

    fetcher = getFetcher(
        getSecurityToken("owner", "owner"),
        new OAuthRequestParams(SERVICE_NAME, null, null, false));
    request = new HttpRequest(
        new URI(FakeOAuthServiceProvider.RESOURCE_URL));
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
        new OAuthRequestParams(SERVICE_NAME, null, clientState, false));
    request = new HttpRequest(
        new URI(FakeOAuthServiceProvider.RESOURCE_URL));
    response = fetcher.fetch(request);
    clientState = response.getMetadata().get("oauthState");

    assertEquals("User data is hello-oauth", response.getResponseAsString());

    assertEquals(1, serviceProvider.getRequestTokenCount());
    assertEquals(1, serviceProvider.getAccessTokenCount());
    assertEquals(4, serviceProvider.getResourceAccessCount());
  }

  private static class FakeGadgetSpecFactory implements GadgetSpecFactory {
    public GadgetSpec getGadgetSpec(GadgetContext context) {
      // we don't need this one for this test
      return null;
    }
    public GadgetSpec getGadgetSpec(URI gadgetUri, boolean ignoreCache)
        throws GadgetException {
      if (gadgetUri.toString().contains("nokey")) {
        String nokeySpec = GadgetTokenStoreTest.GADGET_SPEC.replace(
            SERVICE_NAME, SERVICE_NAME_NO_KEY);
        return new GadgetSpec(gadgetUri, nokeySpec);
      } else {
        return new GadgetSpec(gadgetUri, GadgetTokenStoreTest.GADGET_SPEC);
      }
    }
  }
}
