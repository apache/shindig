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
package org.apache.shindig.auth;

import org.apache.shindig.common.crypto.BlobCrypter;
import org.apache.shindig.common.crypto.BlobCrypterException;

import com.google.common.collect.Maps;

import java.util.Map;

/**
 * Authentication based on a provided BlobCrypter.
 *
 * Wire format is "&lt;container&gt;:&lt;encrypted-and-signed-token&gt;"
 *
 * Container is included so different containers can use different security tokens if necessary.
 */
public class BlobCrypterSecurityToken extends AbstractSecurityToken implements SecurityToken {

  protected static final int MAX_TOKEN_LIFETIME_SECS = 3600;

  protected static final String OWNER_KEY = "o";
  protected static final String VIEWER_KEY = "v";
  protected static final String GADGET_KEY = "g";
  protected static final String GADGET_INSTANCE_KEY = "i";
  protected static final String TRUSTED_JSON_KEY = "j";
  protected static final String EXPIRES_KEY = "x";

  protected final BlobCrypter crypter;
  protected final String container;
  protected final String domain;

  protected String ownerId;
  protected String viewerId;
  protected String appUrl;
  protected long moduleId;
  protected Long expiresAt;

  protected String trustedJson;
  protected String activeUrl;

  /**
   * Create a new security token.
   *
   * @param crypter used for encryption and signing
   * @param container container that is issuing the token
   * @param domain domain to use for signed fetch with default signed fetch key.
   */
  public BlobCrypterSecurityToken(BlobCrypter crypter, String container, String domain) {
    this.crypter = crypter;
    this.container = container;
    this.domain = domain;
  }

  /**
   * Decrypt and verify a token.  Note this is not public, use BlobCrypterSecurityTokenCodec
   * instead.
   *
   * @param crypter crypter to use for decryption
   * @param container container that minted the token
   * @param domain oauth_consumer_key to use for signed fetch with default key
   * @param token the encrypted token (just the portion after the first ":")
   * @return the decrypted, verified token.
   *
   * @throws BlobCrypterException
   */
  static BlobCrypterSecurityToken decrypt(BlobCrypter crypter, String container, String domain,
        String token, String activeUrl) throws BlobCrypterException {
    Map<String, String> values = crypter.unwrap(token, MAX_TOKEN_LIFETIME_SECS);
    BlobCrypterSecurityToken t = new BlobCrypterSecurityToken(crypter, container, domain);
    setTokenValues(t, values);
    t.setActiveUrl(activeUrl);
    return t;
  }
  
  protected static void setTokenValues(BlobCrypterSecurityToken token, Map<String, String> values) {
    token.setOwnerId(values.get(OWNER_KEY));
    token.setViewerId(values.get(VIEWER_KEY));
    token.setAppUrl(values.get(GADGET_KEY));
    String moduleId = values.get(GADGET_INSTANCE_KEY);
    if (moduleId != null) {
      token.setModuleId(Long.parseLong(moduleId));
    }
    String expiresAt = values.get(EXPIRES_KEY);
    if (expiresAt != null) {
      token.setExpiresAt(Long.parseLong(expiresAt));
    }
    token.setTrustedJson(values.get(TRUSTED_JSON_KEY));
  }

  /**
   * Encrypt and sign the token.  The returned value is *not* web safe, it should be URL
   * encoded before being used as a form parameter.
   */
  public String encrypt() throws BlobCrypterException {
    Map<String, String> values = buildValuesMap();
    return container + ':' + crypter.wrap(values);
  }

  protected Map<String, String> buildValuesMap() {
    Map<String, String> values = Maps.newHashMap();
    if (ownerId != null) {
      values.put(OWNER_KEY, ownerId);
    }
    if (viewerId != null) {
      values.put(VIEWER_KEY, viewerId);
    }
    if (appUrl != null) {
      values.put(GADGET_KEY, appUrl);
    }
    if (moduleId != 0) {
      values.put(GADGET_INSTANCE_KEY, Long.toString(moduleId));
    }
    if (expiresAt != null) {
      values.put(EXPIRES_KEY, Long.toString(expiresAt));
    }
    if (trustedJson != null) {
      values.put(TRUSTED_JSON_KEY, trustedJson);
    }
    return values;                                                                                           
   }

  // Legacy value for signed fetch, opensocial 0.8 prefers opensocial_app_url
  public String getAppId() {
    return appUrl;
  }

  public String getAppUrl() {
    return appUrl;
  }

  public void setAppUrl(String appUrl) {
    this.appUrl = appUrl;
  }

  public String getContainer() {
    return container;
  }

  // Used for oauth_consumer_key for signed fetch with default key.  This is a weird spot for this.
  public String getDomain() {
    return domain;
  }

  public long getModuleId() {
    return moduleId;
  }

  public void setModuleId(long moduleId) {
    this.moduleId = moduleId;
  }

  public Long getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Long expiresAt) {
    this.expiresAt = expiresAt;
  }

  public String getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(String ownerId) {
    this.ownerId = ownerId;
  }

  public String getTrustedJson() {
    return trustedJson;
  }

  public void setTrustedJson(String trustedJson) {
    this.trustedJson = trustedJson;
  }

  // Our tokens are static, we could change this to periodically update the token.
  public String getUpdatedToken() {
    return null;
  }

  public String getAuthenticationMode() {
    return AuthenticationMode.SECURITY_TOKEN_URL_PARAMETER.name();
  }

  public String getViewerId() {
    return viewerId;
  }

  public void setViewerId(String viewerId) {
    this.viewerId = viewerId;
  }

  public boolean isAnonymous() {
    return false;
  }

  public void setActiveUrl(String activeUrl) {
    this.activeUrl = activeUrl;
  }

  public String getActiveUrl() {
    if (activeUrl == null) {
      throw new UnsupportedOperationException("No active URL available");
    }
    return activeUrl;
  }
}
