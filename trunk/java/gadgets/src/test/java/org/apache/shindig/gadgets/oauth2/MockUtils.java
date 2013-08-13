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

import com.google.common.collect.Maps;
import com.google.inject.Provider;

import org.apache.shindig.auth.AbstractSecurityToken;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.crypto.BasicBlobCrypter;
import org.apache.shindig.common.crypto.BlobCrypter;
import org.apache.shindig.common.crypto.BlobExpiredException;
import org.apache.shindig.common.servlet.Authority;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.AuthType;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.GadgetSpecFactory;
import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.apache.shindig.gadgets.oauth2.handler.BasicAuthenticationHandler;
import org.apache.shindig.gadgets.oauth2.handler.ClientAuthenticationHandler;
import org.apache.shindig.gadgets.oauth2.handler.StandardAuthenticationHandler;
import org.apache.shindig.gadgets.oauth2.handler.TokenAuthorizationResponseHandler;
import org.apache.shindig.gadgets.oauth2.handler.TokenEndpointResponseHandler;
import org.apache.shindig.gadgets.oauth2.persistence.OAuth2Cache;
import org.apache.shindig.gadgets.oauth2.persistence.OAuth2Client;
import org.apache.shindig.gadgets.oauth2.persistence.OAuth2Encrypter;
import org.apache.shindig.gadgets.oauth2.persistence.OAuth2EncryptionException;
import org.apache.shindig.gadgets.oauth2.persistence.OAuth2Persister;
import org.apache.shindig.gadgets.oauth2.persistence.OAuth2TokenPersistence;
import org.apache.shindig.gadgets.oauth2.persistence.sample.InMemoryCache;
import org.apache.shindig.gadgets.oauth2.persistence.sample.JSONOAuth2Persister;
import org.apache.shindig.gadgets.spec.GadgetSpec;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

public class MockUtils {
  protected static final String ACCESS_SECRET = "accessSecret";
  protected static final String AUTHORIZE_URL = "http://www.example.com/authorize";
  protected static final String CLIENT_ID1 = "clientId1";
  protected static final String CLIENT_ID2 = "clientId2";
  protected static final String CLIENT_SECRET1 = "clientSecret1";
  protected static final String CLIENT_SECRET2 = "clientSecret2";
  protected static final Map<String, String> EMPTY_MAP = Collections.emptyMap();
  protected static final OAuth2Encrypter encrypter = new DummyEncrypter();
  protected static final BlobCrypter stateCrypter = new DummyStateCrypter();
  protected static final String GADGET_URI1 = "http://www.example.com/1";
  protected static final String GADGET_URI2 = "http://www.example.com/2";
  protected static final String MAC_SECRET = "mac_secret";
  protected static final String REDIRECT_URI = "https://www.example.com/gadgets/oauth2callback";
  protected static final String REFRESH_SECRET = "refreshSecret";
  protected static final Integer REFRESH_TOKEN_INDEX = new Integer(81037012);
  protected static final String SCOPE = "testScope";
  protected static final String SERVICE_NAME = "serviceName";
  protected static final String STATE = "1234";
  protected static final String TOKEN_URL = "http://www.example.com/token";
  protected static final String USER = "testUser";

  protected static OAuth2Store dummyStore = null;

  static class DummyAuthority implements Authority {
    public String getAuthority() {
      return "authority";
    }

    public String getOrigin() {
      return "origin";
    }

    public String getScheme() {
      return "scheme";
    }
  }

  static class DummyStateCrypter extends BasicBlobCrypter {
    public DummyStateCrypter() {
      super("xxxxxxxxxxxxxxxx");
    }
  }

  static class DummyEncrypter implements OAuth2Encrypter {

    public byte[] decrypt(final byte[] encryptedSecret) throws OAuth2EncryptionException {
      final byte[] bytesOut = new byte[encryptedSecret.length];
      for (int i = 0; i < encryptedSecret.length; i++) {
        bytesOut[i] = (byte) (encryptedSecret[i] - 1);
      }
      return bytesOut;

    }

    public byte[] encrypt(final byte[] plainSecret) throws OAuth2EncryptionException {
      final byte[] bytesOut = new byte[plainSecret.length];
      for (int i = 0; i < plainSecret.length; i++) {
        bytesOut[i] = (byte) (plainSecret[i] + 1);
      }
      return bytesOut;
    }
  }

  static class DummyHostProvider implements Provider<Authority> {
    private static final Authority authority = new DummyAuthority();

    public Authority get() {
      return DummyHostProvider.authority;
    }
  }

  public static class DummyHttpFetcher implements HttpFetcher {
    public HttpRequest request;

    public HttpResponse fetch(final HttpRequest request) throws GadgetException {
      this.request = request;
      final HttpResponseBuilder builder = new HttpResponseBuilder();
      builder.setStrictNoCache();
      builder.setHttpStatusCode(HttpResponse.SC_OK);
      builder.setHeader("Content-Type", "application/json");
      builder.setContent("{\"access_token\":\"xxx\",\"token_type\":\"Bearer\",\"expires_in\":\"1\",\"refresh_token\":\"yyy\",\"example_parameter\":\"example_value\"}");
      return builder.create();
    }
  }

  static class DummyMessageProvider implements Provider<OAuth2Message> {
    public OAuth2Message get() {
      return new BasicOAuth2Message();
    }
  }

  static class DummySecurityToken extends AbstractSecurityToken {

    public DummySecurityToken(final String ownerId, final String viewerId, final String appUrl) {
      this.setOwnerId(ownerId);
      this.setViewerId(viewerId);
      this.setAppUrl(appUrl);
    }

    @Override
    public String getAppId() {
      return "";
    }

    @Override
    public String getDomain() {
      return "";
    }

    @Override
    public String getContainer() {
      return "";
    }

    @Override
    public long getModuleId() {
      return 0;
    }

    @Override
    public Long getExpiresAt() {
      return 0L;
    }

    public boolean isExpired(final int maxAge) {
      return false;
    }

    public AbstractSecurityToken enforceNotExpired(final int maxAge) throws BlobExpiredException {
      return this;
    }

    public String getUpdatedToken() {
      return "";
    }

    public String getAuthenticationMode() {
      return "";
    }

    @Override
    public String getTrustedJson() {
      return "";
    }

    public boolean isAnonymous() {
      return false;
    }

    @Override
    public String getActiveUrl() {
      return this.getAppUrl();
    }

    @Override
    protected EnumSet<Keys> getMapKeys() {
      return EnumSet.noneOf(Keys.class);
    }
  }

  static class DummyGadgetSpecFactory implements GadgetSpecFactory {
    private static final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?><Module><ModulePrefs title=\"\"><OAuth2><Service name=\"serviceName\" scope=\"testScope\"></Service></OAuth2></ModulePrefs><Content type=\"html\"></Content></Module>";

    public GadgetSpec getGadgetSpec(final GadgetContext context) throws GadgetException {
      final Uri contextUri = context.getUrl();
      if (contextUri != null && contextUri.toString().equals(MockUtils.GADGET_URI1)) {
        return new GadgetSpec(context.getUrl(), DummyGadgetSpecFactory.xml);
      }

      throw new GadgetException(GadgetException.Code.OAUTH_STORAGE_ERROR);
    }

    public Uri getGadgetUri(GadgetContext context) throws GadgetException {
        return context.getUrl();
    }
  }

  private static void setTokenCommons(final OAuth2TokenPersistence token) throws Exception {
    token.setExpiresAt(1L);
    token.setGadgetUri(MockUtils.GADGET_URI1);
    token.setIssuedAt(0L);
    token.setMacAlgorithm("");
    token.setMacExt("");
    token.setMacSecret(new byte[] {});
    token.setProperties(MockUtils.EMPTY_MAP);
    token.setScope(MockUtils.SCOPE);
    token.setSecret(MockUtils.ACCESS_SECRET.getBytes("UTF-8"));
    token.setServiceName(MockUtils.SERVICE_NAME);
    token.setTokenType(OAuth2Message.BEARER_TOKEN_TYPE);
    token.setUser(MockUtils.USER);
  }

  protected static OAuth2TokenPersistence getAccessToken() throws Exception {
    final OAuth2TokenPersistence accessToken = new OAuth2TokenPersistence(
            MockUtils.getDummyEncrypter());
    MockUtils.setTokenCommons(accessToken);
    accessToken.setType(OAuth2Token.Type.ACCESS);
    return accessToken;
  }

  protected static OAuth2TokenPersistence getBadMacToken() throws Exception {
    final OAuth2TokenPersistence accessToken = new OAuth2TokenPersistence(
            MockUtils.getDummyEncrypter());
    MockUtils.setTokenCommons(accessToken);
    accessToken.setMacAlgorithm(OAuth2Message.HMAC_SHA_256);
    accessToken.setMacExt("1 2 3");
    accessToken.setMacSecret(MockUtils.MAC_SECRET.getBytes("UTF-8"));
    accessToken.setTokenType(OAuth2Message.MAC_TOKEN_TYPE);
    accessToken.setType(OAuth2Token.Type.ACCESS);
    return accessToken;
  }

  private static void setClientCommons(final OAuth2Client client) throws Exception {
    client.setAuthorizationUrl(MockUtils.AUTHORIZE_URL);
    client.setGrantType(OAuth2Message.AUTHORIZATION_CODE);
    client.setRedirectUri(MockUtils.REDIRECT_URI);
    client.setServiceName(MockUtils.SERVICE_NAME);
    client.setTokenUrl(MockUtils.TOKEN_URL);
  }

  protected static OAuth2Client getClient_Code_Confidential() throws Exception {
    final OAuth2Client client = new OAuth2Client(MockUtils.getDummyEncrypter());
    MockUtils.setClientCommons(client);
    client.setClientAuthenticationType(OAuth2Message.BASIC_AUTH_TYPE);
    client.setClientId(MockUtils.CLIENT_ID1);
    client.setClientSecret(MockUtils.CLIENT_SECRET1.getBytes("UTF-8"));
    client.setGadgetUri(MockUtils.GADGET_URI1);
    client.setType(OAuth2Accessor.Type.CONFIDENTIAL);
    client.setAllowModuleOverride(true);
    client.setAuthorizationHeader(true);
    client.setUrlParameter(false);

    return client;
  }

  protected static OAuth2Client getClient_Code_Public() throws Exception {
    final OAuth2Client client = new OAuth2Client(MockUtils.getDummyEncrypter());
    MockUtils.setClientCommons(client);
    client.setClientAuthenticationType(OAuth2Message.STANDARD_AUTH_TYPE);
    client.setClientId(MockUtils.CLIENT_ID2);
    client.setClientSecret(MockUtils.CLIENT_SECRET2.getBytes("UTF-8"));
    client.setGadgetUri(MockUtils.GADGET_URI2);
    client.setType(OAuth2Accessor.Type.PUBLIC);
    client.setAllowModuleOverride(false);
    client.setAuthorizationHeader(false);
    client.setUrlParameter(true);

    return client;
  }

  protected static OAuth2Arguments getDummyArguments() throws Exception {
    final Map<String, String> map = Maps.newHashMap();
    map.put("OAUTH_SCOPE", MockUtils.SCOPE);
    map.put("OAUTH_SERVICE_NAME", MockUtils.SERVICE_NAME);
    return new OAuth2Arguments(AuthType.OAUTH2, map);
  }

  protected static List<ClientAuthenticationHandler> getDummyClientAuthHandlers() throws Exception {
    final List<ClientAuthenticationHandler> ret = new ArrayList<ClientAuthenticationHandler>(2);
    ret.add(new BasicAuthenticationHandler());
    ret.add(new StandardAuthenticationHandler());
    return ret;
  }

  protected static BlobCrypter getDummyStateCrypter() {
    return MockUtils.stateCrypter;
  }

  protected static OAuth2Encrypter getDummyEncrypter() {
    return MockUtils.encrypter;
  }

  protected static HttpFetcher getDummyFetcher() throws Exception {
    return new DummyHttpFetcher();
  }

  protected static Authority getDummyAuthority() {
    return new DummyAuthority();
  }

  protected static Provider<OAuth2Message> getDummyMessageProvider() {
    return new DummyMessageProvider();
  }

  protected static JSONOAuth2Persister getDummyPersister() throws Exception {
    final JSONObject configFile = new JSONObject(MockUtils.getJSONString());
    return new JSONOAuth2Persister(MockUtils.getDummyEncrypter(), MockUtils.getDummyAuthority(),
            MockUtils.REDIRECT_URI, "xxx", configFile);
  }

  protected static GadgetSpecFactory getDummySpecFactory() {
    return new DummyGadgetSpecFactory();
  }

  protected static SecurityToken getDummySecurityToken(final String ownerId, final String viewerId,
          final String appUrl) {
    return new DummySecurityToken(ownerId, viewerId, appUrl);
  }

  protected static OAuth2Store getDummyStore() throws Exception {
    if (MockUtils.dummyStore == null) {
      final OAuth2Cache cache = new InMemoryCache();
      final OAuth2Persister persister = MockUtils.getDummyPersister();
      final OAuth2Encrypter encrypter = MockUtils.getDummyEncrypter();
      final BlobCrypter stateCrypter = MockUtils.getDummyStateCrypter();
      MockUtils.dummyStore = MockUtils.getDummyStore(cache, persister, encrypter,
              MockUtils.REDIRECT_URI, null, null, stateCrypter);
    }

    MockUtils.dummyStore.clearCache();
    MockUtils.dummyStore.init();

    return MockUtils.dummyStore;
  }

  protected static OAuth2Store getDummyStore(final OAuth2Cache cache,
          final OAuth2Persister persister, final OAuth2Encrypter encrypter,
          final String globalRedirectUri, final Authority authority, final String contextRoot,
          final BlobCrypter stateCrypter) {
    final OAuth2Store store = new BasicOAuth2Store(cache, persister, encrypter, globalRedirectUri,
            authority, contextRoot, stateCrypter);

    return store;
  }

  protected static List<TokenEndpointResponseHandler> getDummyTokenEndpointResponseHandlers()
          throws Exception {
    final List<TokenEndpointResponseHandler> ret = new ArrayList<TokenEndpointResponseHandler>(1);
    ret.add(new TokenAuthorizationResponseHandler(MockUtils.getDummyMessageProvider(), MockUtils
            .getDummyStore()));
    return ret;
  }

  protected static String getJSONString() throws IOException {
    return MockUtils.loadFile("org/apache/shindig/gadgets/oauth2/oauth2_test.json");
  }

  protected static OAuth2TokenPersistence getMacToken() throws Exception {
    final OAuth2TokenPersistence accessToken = new OAuth2TokenPersistence(
            MockUtils.getDummyEncrypter());

    MockUtils.setTokenCommons(accessToken);
    accessToken.setMacAlgorithm(OAuth2Message.HMAC_SHA_1);
    accessToken.setMacExt("1 2 3");
    accessToken.setMacSecret(MockUtils.MAC_SECRET.getBytes("UTF-8"));
    accessToken.setTokenType(OAuth2Message.MAC_TOKEN_TYPE);
    accessToken.setType(OAuth2Token.Type.ACCESS);

    return accessToken;
  }

  private static BasicOAuth2Accessor getOAuth2AccessorCommon() throws Exception {
    final OAuth2Cache cache = new InMemoryCache();
    final OAuth2Persister persister = MockUtils.getDummyPersister();
    final OAuth2Encrypter encrypter = MockUtils.getDummyEncrypter();
    final OAuth2Store store = MockUtils.getDummyStore(cache, persister, encrypter,
            MockUtils.REDIRECT_URI, null, null, MockUtils.stateCrypter);
    final BasicOAuth2Accessor accessor = new BasicOAuth2Accessor(MockUtils.GADGET_URI1,
            MockUtils.SERVICE_NAME, MockUtils.USER, MockUtils.SCOPE, true, store,
            MockUtils.REDIRECT_URI, null, null);

    accessor.setAccessToken(MockUtils.getAccessToken());
    accessor.setAuthorizationUrl(MockUtils.AUTHORIZE_URL);
    accessor.setClientAuthenticationType(OAuth2Message.BASIC_AUTH_TYPE);
    accessor.setClientId(MockUtils.CLIENT_ID1);
    accessor.setClientSecret(MockUtils.CLIENT_SECRET1.getBytes("UTF-8"));
    accessor.setErrorUri(null);
    accessor.setGrantType(OAuth2Message.AUTHORIZATION);
    accessor.setRedirectUri(MockUtils.REDIRECT_URI);
    accessor.setRefreshToken(MockUtils.getRefreshToken());
    accessor.setTokenUrl(MockUtils.TOKEN_URL);
    accessor.setType(OAuth2Accessor.Type.CONFIDENTIAL);
    accessor.setAuthorizationHeader(Boolean.TRUE);
    accessor.setRedirecting(Boolean.FALSE);
    accessor.setUrlParameter(Boolean.FALSE);

    return accessor;
  }

  protected static OAuth2Accessor getOAuth2Accessor_Code() throws Exception {
    final BasicOAuth2Accessor accessor = MockUtils.getOAuth2AccessorCommon();

    return accessor;
  }

  protected static OAuth2Accessor getOAuth2Accessor_Error() {
    final OAuth2Accessor accessor = new BasicOAuth2Accessor(null,
            OAuth2Error.GET_OAUTH2_ACCESSOR_PROBLEM, "test contextMessage", null);

    return accessor;
  }

  protected static OAuth2Accessor getOAuth2Accessor_MacToken() throws Exception {
    final BasicOAuth2Accessor accessor = MockUtils.getOAuth2AccessorCommon();

    accessor.setAccessToken(MockUtils.getMacToken());
    accessor.setRefreshToken(null);

    return accessor;
  }

  protected static OAuth2Accessor getOAuth2Accessor_BadMacToken() throws Exception {
    final BasicOAuth2Accessor accessor = MockUtils.getOAuth2AccessorCommon();

    accessor.setAccessToken(MockUtils.getBadMacToken());
    accessor.setRefreshToken(null);

    return accessor;
  }

  protected static OAuth2Accessor getOAuth2Accessor_StandardAuth() throws Exception {
    final BasicOAuth2Accessor accessor = MockUtils.getOAuth2AccessorCommon();

    accessor.setClientAuthenticationType(OAuth2Message.STANDARD_AUTH_TYPE);

    return accessor;
  }

  protected static OAuth2Accessor getOAuth2Accessor_ClientCredentials() throws Exception {
    final BasicOAuth2Accessor accessor = MockUtils.getOAuth2AccessorCommon();

    accessor.setGrantType(OAuth2Message.CLIENT_CREDENTIALS);

    return accessor;
  }

  protected static OAuth2Accessor getOAuth2Accessor_Redirecting() throws Exception {
    final BasicOAuth2Accessor accessor = MockUtils.getOAuth2AccessorCommon();

    accessor.setRedirecting(Boolean.TRUE);

    return accessor;
  }

  protected static OAuth2Accessor getOAuth2Accessor_ClientCredentialsRedirecting() throws Exception {
    final OAuth2Cache cache = new InMemoryCache();
    final OAuth2Persister persister = MockUtils.getDummyPersister();
    final OAuth2Encrypter encrypter = MockUtils.getDummyEncrypter();
    final OAuth2Store store = MockUtils.getDummyStore(cache, persister, encrypter,
            MockUtils.REDIRECT_URI, null, null, MockUtils.stateCrypter);
    final BasicOAuth2Accessor accessor = new BasicOAuth2Accessor(MockUtils.GADGET_URI1,
            MockUtils.SERVICE_NAME, MockUtils.USER, MockUtils.SCOPE, true, store,
            MockUtils.REDIRECT_URI, null, null);

    accessor.setGrantType(OAuth2Message.CLIENT_CREDENTIALS);
    accessor.setRedirecting(Boolean.TRUE);

    return accessor;
  }

  protected static OAuth2TokenPersistence getRefreshToken() throws Exception {
    final OAuth2TokenPersistence refreshToken = new OAuth2TokenPersistence(
            MockUtils.getDummyEncrypter());
    refreshToken.setExpiresAt(1L);
    refreshToken.setGadgetUri(MockUtils.GADGET_URI1);
    refreshToken.setIssuedAt(0L);
    refreshToken.setMacAlgorithm("");
    refreshToken.setMacExt("");
    refreshToken.setMacSecret(new byte[] {});
    refreshToken.setProperties(MockUtils.EMPTY_MAP);
    refreshToken.setScope(MockUtils.SCOPE);
    refreshToken.setSecret(MockUtils.ACCESS_SECRET.getBytes("UTF-8"));
    refreshToken.setServiceName(MockUtils.SERVICE_NAME);
    refreshToken.setTokenType(OAuth2Message.MAC_TOKEN_TYPE);
    refreshToken.setType(OAuth2Token.Type.REFRESH);
    refreshToken.setUser(MockUtils.USER);
    return refreshToken;
  }

  protected static String loadFile(final String path) throws IOException {
    final InputStream is = MockUtils.class.getClassLoader().getResourceAsStream(path);
    return IOUtils.toString(is, "UTF-8");
  }
}
