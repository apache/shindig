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

import java.util.EnumSet;
import java.util.Map;
/**
 * Authentication based on a provided BlobCrypter.
 *
 * Wire format is "&lt;container&gt;:&lt;encrypted-and-signed-token&gt;"
 *
 * Container is included so different containers can use different security tokens if necessary.
 */
public class BlobCrypterSecurityToken extends AbstractSecurityToken {
  private static final EnumSet<Keys> MAP_KEYS = EnumSet.of(
    Keys.OWNER, Keys.VIEWER, Keys.APP_URL, Keys.MODULE_ID, Keys.EXPIRES, Keys.TRUSTED_JSON
  );

  /**
   * Create a new security token.
   *
   * @param container container that is issuing the token
   * @param domain domain to use for signed fetch with default signed fetch key.
   * @param activeUrl
   * @param values Other values to init into the token.
   */
  public BlobCrypterSecurityToken(String container, String domain, String activeUrl, Map<String, String> values) {
    if (values != null) {
      loadFromMap(values);
    }
    setContainer(container).setDomain(domain).setActiveUrl(activeUrl);
  }

  // Our tokens are static, we could change this to periodically update the token.
  public String getUpdatedToken() {
    return null;
  }

  public String getAuthenticationMode() {
    return AuthenticationMode.SECURITY_TOKEN_URL_PARAMETER.name();
  }

  public boolean isAnonymous() {
    return false;
  }

  // Legacy value for signed fetch, opensocial 0.8 prefers opensocial_app_url
  @Override
  public String getAppId() {
    return getAppUrl();
  }

  protected EnumSet<Keys> getMapKeys() {
    return MAP_KEYS;
  }

  public static BlobCrypterSecurityToken fromToken(SecurityToken token) {
    BlobCrypterSecurityToken interpretedToken = new BlobCrypterSecurityToken(token.getContainer(),
        token.getDomain(), token.getActiveUrl(), null);
    interpretedToken
        .setAppId(token.getAppId())
        .setAppUrl(token.getAppUrl())
        .setExpiresAt(token.getExpiresAt())
        .setModuleId(token.getModuleId())
        .setOwnerId(token.getOwnerId())
        .setTrustedJson(token.getTrustedJson())
        .setViewerId(token.getViewerId());

    return interpretedToken;
  }
}
