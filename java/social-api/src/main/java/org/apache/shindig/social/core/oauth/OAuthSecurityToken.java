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
package org.apache.shindig.social.core.oauth;

import java.util.EnumSet;

import org.apache.shindig.auth.AbstractSecurityToken;
import org.apache.shindig.auth.AuthenticationMode;

/**
 * A SecurityToken that represents two/three legged OAuth requests
 */
public class OAuthSecurityToken extends AbstractSecurityToken {
  private static final EnumSet<Keys> MAPKEYS = EnumSet.of(
    Keys.VIEWER, Keys.OWNER, Keys.APP_URL, Keys.APP_ID, Keys.DOMAIN, Keys.CONTAINER, Keys.EXPIRES
  );

  private final String authMode;

  public OAuthSecurityToken(String userId, String appUrl, String appId, String domain,
      String container, Long expiresAt) {
    this(userId, appUrl, appId, domain, container, expiresAt, AuthenticationMode.OAUTH.name());
  }

  public OAuthSecurityToken(String userId, String appUrl, String appId, String domain,
      String container, Long expiresAt, String authMode) {

    setViewerId(userId);
    setOwnerId(userId);
    setAppUrl(appUrl);
    setAppId(appId);
    setDomain(domain);
    setContainer(container);
    setExpiresAt(expiresAt);
    this.authMode = authMode;
  }

  // We don't support this concept yet. We probably don't need to as opensocial calls don't
  // currently depend on the app instance id.
  @Override
  public long getModuleId() {
    throw new UnsupportedOperationException();
  }

  public String getUpdatedToken() {
    throw new UnsupportedOperationException();
  }

  public String getAuthenticationMode() {
    return authMode;
  }

  @Override
  public String getTrustedJson() {
    throw new UnsupportedOperationException();
  }

  public boolean isAnonymous() {
    return false;
  }

  @Override
  protected EnumSet<Keys> getMapKeys() {
    return MAPKEYS;
  }
}
