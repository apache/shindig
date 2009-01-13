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

import com.google.common.collect.Maps;

import org.apache.shindig.common.crypto.BasicBlobCrypter;
import org.apache.shindig.common.crypto.BlobCrypter;
import org.apache.shindig.common.crypto.BlobCrypterException;

import java.util.Map;

/**
 * Primitive token implementation that uses strings as tokens.
 */
public class BasicSecurityToken implements SecurityToken {
  /** serialized form of the token */
  private final String token;

  /** data from the token */
  private final Map<String, String> tokenData;

  /** tool to use for signing and encrypting the token */
  private BlobCrypter crypter = new BasicBlobCrypter(INSECURE_KEY);

  private static final byte[] INSECURE_KEY =
    { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

  private static final String OWNER_KEY = "o";
  private static final String APP_KEY = "a";
  private static final String VIEWER_KEY = "v";
  private static final String DOMAIN_KEY = "d";
  private static final String APPURL_KEY = "u";
  private static final String MODULE_KEY = "m";

  public String toSerialForm() {
    return token;
  }

  /**
   * Generates a token from an input string
   * @param token String form of token
   * @param maxAge max age of the token (in seconds)
   * @throws BlobCrypterException
   */
  public BasicSecurityToken(String token, int maxAge)
  throws BlobCrypterException {
    this.token = token;
    this.tokenData = crypter.unwrap(token, maxAge);
  }

  public BasicSecurityToken(String owner, String viewer, String app,
      String domain, String appUrl, String moduleId) throws BlobCrypterException {
    tokenData = Maps.newHashMapWithExpectedSize(5);
    putNullSafe(OWNER_KEY, owner);
    putNullSafe(VIEWER_KEY, viewer);
    putNullSafe(APP_KEY, app);
    putNullSafe(DOMAIN_KEY, domain);
    putNullSafe(APPURL_KEY, appUrl);
    putNullSafe(MODULE_KEY, moduleId);
    token = crypter.wrap(tokenData);
  }

  private void putNullSafe(String key, String value) {
    if (value != null) {
      tokenData.put(key, value);
    }
  }

  /**
   * {@inheritDoc}
   */
  public String getAppId() {
    return tokenData.get(APP_KEY);
  }

  /**
   * {@inheritDoc}
   */
  public String getDomain() {
    return tokenData.get(DOMAIN_KEY);
  }

  /**
   * {@inheritDoc}
   */
  public String getOwnerId() {
    return tokenData.get(OWNER_KEY);
  }

  /**
   * {@inheritDoc}
   */
  public String getViewerId() {
    return tokenData.get(VIEWER_KEY);
  }

  /**
   * {@inheritDoc}
   */
  public String getAppUrl() {
    return tokenData.get(APPURL_KEY);
  }

  /**
   * {@inheritDoc}
   */
  public long getModuleId() {
    return Long.parseLong(tokenData.get(MODULE_KEY));
  }

  /**
   * {@inheritDoc}
   */
  public String getUpdatedToken() {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  public String getTrustedJson() {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  public boolean isAnonymous() {
    return false;
  }
}
