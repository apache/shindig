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

import org.apache.shindig.common.SecurityToken;

public class OAuthSecurityToken implements SecurityToken {
  private final String userId;
  private final String appUrl;
  private final String appId;
  private final String domain;

  public OAuthSecurityToken(String userId, String appUrl, String appId, String domain) {
    this.userId = userId;
    this.appUrl = appUrl;
    this.appId = appId;
    this.domain = domain;
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

  public String getAppUrl() {
    return appUrl;
  }

  // We don't support this concept yet. We probably don't need to as opensocial calls don't
  // currently depend on the app instance id.
  public long getModuleId() {
    throw new UnsupportedOperationException();
  }

  // Not needed for this basic token
  public String toSerialForm() {
    throw new UnsupportedOperationException();
  }

  public String getUpdatedToken() {
    throw new UnsupportedOperationException();
  }

  public String getTrustedJson() {
    throw new UnsupportedOperationException();
  }
  public boolean isAnonymous() {
    return false;
  }
}
