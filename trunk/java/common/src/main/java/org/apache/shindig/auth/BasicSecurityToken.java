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

/**
 * Primitive token implementation that uses strings as tokens.
 */
public class BasicSecurityToken extends AbstractSecurityToken {
  private static final EnumSet<Keys> SUPPORTED = EnumSet.noneOf(Keys.class);

  public BasicSecurityToken(String owner, String viewer, String app,
      String domain, String appUrl, String moduleId, String container, String activeUrl, Long expiresAt) {
    setOwnerId(owner).setViewerId(viewer).setAppId(app).setDomain(domain).setAppUrl(appUrl);
    if (moduleId != null)
      setModuleId(Long.parseLong(moduleId));
    setContainer(container).setActiveUrl(activeUrl).setExpiresAt(expiresAt);
  }

  public BasicSecurityToken() { }

  public String getUpdatedToken() {
    return null;
  }

  public String getAuthenticationMode() {
    return AuthenticationMode.SECURITY_TOKEN_URL_PARAMETER.name();
  }

  public boolean isAnonymous() {
    return false;
  }

  /* (non-Javadoc)
   * @see org.apache.shindig.auth.AbstractSecurityToken#getSupportedKeys()
   *
   * The codec for this token does not use a BlobCrypter, so we don't need the
   * toMap and loadFromMap functionality.
   */
  protected EnumSet<Keys> getMapKeys() {
    return SUPPORTED;
  }
}
