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

import com.google.common.collect.Maps;

import com.google.inject.Singleton;

import net.oauth.OAuth;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthServiceProvider;
import net.oauth.signature.RSA_SHA1;

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.servlet.Authority;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.oauth.BasicOAuthStoreConsumerKeyAndSecret.KeyType;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.Map;

/**
 * Simple implementation of the {@link OAuthStore} interface. We use a
 * in-memory hash map. If initialized with a private key, then the store will
 * return an OAuthAccessor in {@code getOAuthAccessor} that uses that private
 * key if no consumer key and secret could be found.
 */
@Singleton
public class BasicOAuthStore implements OAuthStore {

  private static final String CONSUMER_SECRET_KEY = "consumer_secret";
  private static final String CONSUMER_KEY_KEY = "consumer_key";
  private static final String KEY_TYPE_KEY = "key_type";
  private static final String CALLBACK_URL = "callback_url";
  private static final String OAUTH_BODY_HASH_KEY = "bodyHash";

  /**
   * HashMap of provider and consumer information. Maps BasicOAuthStoreConsumerIndexs (i.e.
   * nickname of a service provider and the gadget that uses that nickname) to
   * {@link BasicOAuthStoreConsumerKeyAndSecret}s.
   */
  private final Map<BasicOAuthStoreConsumerIndex, BasicOAuthStoreConsumerKeyAndSecret> consumerInfos;

  /**
   * HashMap of token information. Maps BasicOAuthStoreTokenIndexs (i.e. gadget id, token
   * nickname, module id, etc.) to TokenInfos (i.e. access token and token
   * secrets).
   */
  private final Map<BasicOAuthStoreTokenIndex, TokenInfo> tokens;

  /**
   * Key to use when no other key is found.
   */
  private BasicOAuthStoreConsumerKeyAndSecret defaultKey;

  /**
   * Callback to use when no per-key callback URL is found.
   */
  private String defaultCallbackUrl;

  /** Number of times we looked up a consumer key */
  private int consumerKeyLookupCount = 0;

  /** Number of times we looked up an access token */
  private int accessTokenLookupCount = 0;

  /** Number of times we added an access token */
  private int accessTokenAddCount = 0;

  /** Number of times we removed an access token */
  private int accessTokenRemoveCount = 0;

  private Authority authority;

  public BasicOAuthStore() {
    consumerInfos = Maps.newHashMap();
    tokens = Maps.newHashMap();
  }

  public void initFromConfigString(String oauthConfigStr) throws GadgetException {
    try {
      JSONObject oauthConfigs = new JSONObject(oauthConfigStr);
      for (Iterator<?> i = oauthConfigs.keys(); i.hasNext();) {
        String url = (String) i.next();
        URI gadgetUri = new URI(url);
        JSONObject oauthConfig = oauthConfigs.getJSONObject(url);
        storeConsumerInfos(gadgetUri, oauthConfig);
      }
    } catch (JSONException e) {
      throw new GadgetException(GadgetException.Code.OAUTH_STORAGE_ERROR, e);
    } catch (URISyntaxException e) {
      throw new GadgetException(GadgetException.Code.OAUTH_STORAGE_ERROR, e);
    }
  }

  private void storeConsumerInfos(URI gadgetUri, JSONObject oauthConfig)
      throws JSONException, GadgetException {
    for (String serviceName : JSONObject.getNames(oauthConfig)) {
      JSONObject consumerInfo = oauthConfig.getJSONObject(serviceName);
      storeConsumerInfo(gadgetUri, serviceName, consumerInfo);
    }
  }

  private void storeConsumerInfo(URI gadgetUri, String serviceName, JSONObject consumerInfo)
      throws JSONException, GadgetException {
    realStoreConsumerInfo(gadgetUri, serviceName, consumerInfo);
  }

  private void realStoreConsumerInfo(URI gadgetUri, String serviceName, JSONObject consumerInfo)
      throws JSONException {
    String callbackUrl = consumerInfo.optString(CALLBACK_URL, null);
    String consumerSecret = consumerInfo.getString(CONSUMER_SECRET_KEY);
    String consumerKey = consumerInfo.getString(CONSUMER_KEY_KEY);
    String keyTypeStr = consumerInfo.getString(KEY_TYPE_KEY);
    boolean oauthBodyHash = true;
    String oauthBodyHashString = consumerInfo.optString(OAUTH_BODY_HASH_KEY);
    if ("false".equalsIgnoreCase(oauthBodyHashString)) {
      oauthBodyHash = false;
    }
    KeyType keyType = KeyType.HMAC_SYMMETRIC;

    if ("RSA_PRIVATE".equals(keyTypeStr)) {
      keyType = KeyType.RSA_PRIVATE;
      consumerSecret = convertFromOpenSsl(consumerSecret);
    } else if ("PLAINTEXT".equals(keyTypeStr)) {
      keyType = KeyType.PLAINTEXT;
    }

    BasicOAuthStoreConsumerKeyAndSecret kas = new BasicOAuthStoreConsumerKeyAndSecret(
        consumerKey, consumerSecret, keyType, null, callbackUrl, oauthBodyHash);

    BasicOAuthStoreConsumerIndex index = new BasicOAuthStoreConsumerIndex();
    index.setGadgetUri(gadgetUri.toASCIIString());
    index.setServiceName(serviceName);
    setConsumerKeyAndSecret(index, kas);
  }

  // Support standard openssl keys by stripping out the headers and blank lines
  public static String convertFromOpenSsl(String privateKey) {
    return privateKey.replaceAll("-----[A-Z ]*-----", "").replace("\n", "");
  }

  public void setDefaultKey(BasicOAuthStoreConsumerKeyAndSecret defaultKey) {
    this.defaultKey = defaultKey;
  }

  public void setDefaultCallbackUrl(String defaultCallbackUrl) {
    this.defaultCallbackUrl = defaultCallbackUrl;
  }

  public void setConsumerKeyAndSecret(
      BasicOAuthStoreConsumerIndex providerKey, BasicOAuthStoreConsumerKeyAndSecret keyAndSecret) {
    consumerInfos.put(providerKey, keyAndSecret);
  }

  public void setAuthority(Authority authority) {
    this.authority = authority;
  }

  public ConsumerInfo getConsumerKeyAndSecret(
      SecurityToken securityToken, String serviceName, OAuthServiceProvider provider)
      throws GadgetException {
    ++consumerKeyLookupCount;
    BasicOAuthStoreConsumerIndex pk = new BasicOAuthStoreConsumerIndex();
    pk.setGadgetUri(securityToken.getAppUrl());
    pk.setServiceName(serviceName);
    BasicOAuthStoreConsumerKeyAndSecret cks = consumerInfos.get(pk);
    if (cks == null) {
      cks = defaultKey;
    }
    if (cks == null) {
      throw new GadgetException(GadgetException.Code.INTERNAL_SERVER_ERROR,
          "No key for gadget " + securityToken.getAppUrl() + " and service " + serviceName);
    }
    OAuthConsumer consumer;
    final KeyType keyType = cks.getKeyType();
    if (keyType == KeyType.RSA_PRIVATE) {
      consumer = new OAuthConsumer(null, cks.getConsumerKey(), null, provider);
      // The oauth.net java code has lots of magic.  By setting this property here, code thousands
      // of lines away knows that the consumerSecret value in the consumer should be treated as
      // an RSA private key and not an HMAC key.
      consumer.setProperty(OAuth.OAUTH_SIGNATURE_METHOD, OAuth.RSA_SHA1);
      consumer.setProperty(RSA_SHA1.PRIVATE_KEY, cks.getConsumerSecret());
    } else if  (keyType == KeyType.PLAINTEXT) {
      consumer = new OAuthConsumer(null, cks.getConsumerKey(), cks.getConsumerSecret(), provider);
      consumer.setProperty(OAuth.OAUTH_SIGNATURE_METHOD, "PLAINTEXT");
    } else {
      consumer = new OAuthConsumer(null, cks.getConsumerKey(), cks.getConsumerSecret(), provider);
      consumer.setProperty(OAuth.OAUTH_SIGNATURE_METHOD, OAuth.HMAC_SHA1);
    }
    String callback = (cks.getCallbackUrl() != null ? cks.getCallbackUrl() : defaultCallbackUrl);

    if (authority != null) {
      callback = callback.replace("%authority%", authority.getAuthority());
    }

    return new ConsumerInfo(consumer, cks.getKeyName(), callback, cks.isOauthBodyHash());
  }

  private BasicOAuthStoreTokenIndex makeBasicOAuthStoreTokenIndex(
      SecurityToken securityToken, String serviceName, String tokenName) {
    BasicOAuthStoreTokenIndex tokenKey = new BasicOAuthStoreTokenIndex();
    tokenKey.setGadgetUri(securityToken.getAppUrl());
    tokenKey.setModuleId(securityToken.getModuleId());
    tokenKey.setServiceName(serviceName);
    tokenKey.setTokenName(tokenName);
    tokenKey.setUserId(securityToken.getViewerId());
    return tokenKey;
  }

  public TokenInfo getTokenInfo(SecurityToken securityToken, ConsumerInfo consumerInfo,
      String serviceName, String tokenName) {
    ++accessTokenLookupCount;
    BasicOAuthStoreTokenIndex tokenKey =
        makeBasicOAuthStoreTokenIndex(securityToken, serviceName, tokenName);
    return tokens.get(tokenKey);
  }

  public void setTokenInfo(SecurityToken securityToken, ConsumerInfo consumerInfo,
      String serviceName, String tokenName, TokenInfo tokenInfo) {
    ++accessTokenAddCount;
    BasicOAuthStoreTokenIndex tokenKey =
        makeBasicOAuthStoreTokenIndex(securityToken, serviceName, tokenName);
    tokens.put(tokenKey, tokenInfo);
  }

  public void removeToken(SecurityToken securityToken, ConsumerInfo consumerInfo,
      String serviceName, String tokenName) {
    ++accessTokenRemoveCount;
    BasicOAuthStoreTokenIndex tokenKey =
        makeBasicOAuthStoreTokenIndex(securityToken, serviceName, tokenName);
    tokens.remove(tokenKey);
  }

  public int getConsumerKeyLookupCount() {
    return consumerKeyLookupCount;
  }

  public int getAccessTokenLookupCount() {
    return accessTokenLookupCount;
  }

  public int getAccessTokenAddCount() {
    return accessTokenAddCount;
  }

  public int getAccessTokenRemoveCount() {
    return accessTokenRemoveCount;
  }
}
