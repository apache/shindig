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
package org.apache.shindig.social.core.oauth;

import org.apache.shindig.auth.AbstractSecurityToken;
import org.apache.shindig.auth.AuthenticationMode;
import org.apache.shindig.auth.SecurityToken;

/**
 * A SecurityToken that represents two/three legged OAuth requests
 */
public class OAuthSecurityToken extends AbstractSecurityToken implements SecurityToken {
  private final String userId;
  private final String appUrl;
  private final String appId;
  private final String domain;
  private final String container;
  private final String authMode;
  private final Long expiresAt;

  public OAuthSecurityToken(String userId, String appUrl, String appId, String domain,
      String container, Long expiresAt) {
    this(userId, appUrl, appId, domain, container, expiresAt, AuthenticationMode.OAUTH.name());
  }

  public OAuthSecurityToken(String userId, String appUrl, String appId, String domain,
      String container, Long expiresAt, String authMode) {
    this.userId = userId;
    this.appUrl = appUrl;
    this.appId = appId;
    this.domain = domain;
    this.container = container;
    this.authMode = authMode;
    this.expiresAt = null; // TODO add
  }

  public String getOwnerId() {
    return userId;
  }

  public String getViewerId() {
    return userId;
  }

  public String getAppId() {
    return appId;
  }

  public String getDomain() {
    return domain;
  }

  public String getContainer() {
    return container;
  }

  public String getAppUrl() {
    return appUrl;
  }

  public Long getExpiresAt() {
    return expiresAt;
  }

  // We don't support this concept yet. We probably don't need to as opensocial calls don't
  // currently depend on the app instance id.
  public long getModuleId() {
    throw new UnsupportedOperationException();
  }

  public String getUpdatedToken() {
    throw new UnsupportedOperationException();
  }

  public String getAuthenticationMode() {
    return authMode;
  }

  public String getTrustedJson() {
    throw new UnsupportedOperationException();
  }

  public boolean isAnonymous() {
    return false;
  }
}
