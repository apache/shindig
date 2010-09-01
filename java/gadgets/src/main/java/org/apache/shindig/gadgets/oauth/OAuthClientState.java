/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.shindig.gadgets.oauth;

import com.google.common.collect.Maps;

import org.apache.shindig.common.crypto.BlobCrypter;
import org.apache.shindig.common.crypto.BlobCrypterException;

import java.util.Map;

/**
 * Class to handle OAuth fetcher state stored client side.  The state is
 * stored as a signed, encrypted, time stamped blob.
 */
public class OAuthClientState {
  /**
   * Maximum age for our client state; if this is exceeded we start over. One
   * hour is a fairly arbitrary time limit here.
   */
  private static final int CLIENT_STATE_MAX_AGE_SECS = 3600;

  // Our client state is encrypted key/value pairs.  These are the key names.
  private static final String REQ_TOKEN_KEY = "r";
  private static final String REQ_TOKEN_SECRET_KEY = "rs";
  private static final String ACCESS_TOKEN_KEY = "a";
  private static final String ACCESS_TOKEN_SECRET_KEY = "as";
  private static final String OWNER_KEY = "o";
  private static final String SESSION_HANDLE_KEY = "sh";
  private static final String ACCESS_TOKEN_EXPIRATION_KEY = "e";

  /** Name/value pairs */
  private final Map<String, String> state;

  /** Crypter to use when sending these to the client */
  private final BlobCrypter crypter;

  /**
   * Create a new, empty client state blob.
   * 
   * @param crypter
   */
  public OAuthClientState(BlobCrypter crypter) {
    this.state = Maps.newHashMap();
    this.crypter = crypter;
  }

  /**
   * Initialize client state based on an encrypted blob passed by the
   * client.
   *
   * @param crypter
   * @param stateBlob
   */
  public OAuthClientState(BlobCrypter crypter, String stateBlob) {
    this.crypter = crypter;
    Map<String, String> state = null;
    if (stateBlob != null) {
      try {
        state = crypter.unwrap(stateBlob, CLIENT_STATE_MAX_AGE_SECS);
      } catch (BlobCrypterException e) {
        // Probably too old, pretend we never saw it at all.
      }
    }
    if (state == null) {
      state = Maps.newHashMap();
    }
    this.state = state; 
  }

  /**
   * @return true if there is no state to store with the client.
   */
  public boolean isEmpty() {
    // Might contain just a timestamp
    return (state.isEmpty() || (state.size() == 1 && state.containsKey("t")));
  }

  /**
   * @return an encrypted blob of state to store with the client.
   * @throws BlobCrypterException
   */
  public String getEncryptedState() throws BlobCrypterException {
    return crypter.wrap(state);
  }

  /**
   * OAuth request token
   */
  public String getRequestToken() {
    return state.get(REQ_TOKEN_KEY);
  }

  public void setRequestToken(String requestToken) {
    setNullCheck(REQ_TOKEN_KEY, requestToken);
  }

  /**
   * OAuth request token secret
   */
  public String getRequestTokenSecret() {
    return state.get(REQ_TOKEN_SECRET_KEY);
  }

  public void setRequestTokenSecret(String requestTokenSecret) {
    setNullCheck(REQ_TOKEN_SECRET_KEY, requestTokenSecret);
  }

  /**
   * OAuth access token.
   */
  public String getAccessToken() {
    return state.get(ACCESS_TOKEN_KEY);
  }

  public void setAccessToken(String accessToken) {
    setNullCheck(ACCESS_TOKEN_KEY, accessToken);
  }

  /**
   * OAuth access token secret.
   */
  public String getAccessTokenSecret() {
    return state.get(ACCESS_TOKEN_SECRET_KEY);
  }

  public void setAccessTokenSecret(String accessTokenSecret) {
    setNullCheck(ACCESS_TOKEN_SECRET_KEY, accessTokenSecret);
  }

  /**
   * Session handle (http://oauth.googlecode.com/svn/spec/ext/session/1.0/drafts/1/spec.html)
   */
  public String getSessionHandle() {
    return state.get(SESSION_HANDLE_KEY);
  }

  public void setSessionHandle(String sessionHandle) {
    setNullCheck(SESSION_HANDLE_KEY, sessionHandle);
  }

  /**
   * Expiration of access token
   * (http://oauth.googlecode.com/svn/spec/ext/session/1.0/drafts/1/spec.html)
   */
  public long getTokenExpireMillis() {
    String expiration = state.get(ACCESS_TOKEN_EXPIRATION_KEY);
    if (expiration == null) {
      return 0;
    }
    return Long.parseLong(expiration);
  }

  public void setTokenExpireMillis(long expirationMillis) {
    setNullCheck(ACCESS_TOKEN_EXPIRATION_KEY, Long.toString(expirationMillis));
  }

  /**
   * Owner of the OAuth token.
   */
  public String getOwner() {
    return state.get(OWNER_KEY);
  }

  public void setOwner(String owner) {
    setNullCheck(OWNER_KEY, owner);
  }

  private void setNullCheck(String key, String value) {
    if (value == null) {
      state.remove(key);
    } else {
      state.put(key, value);
    }
  }
}
