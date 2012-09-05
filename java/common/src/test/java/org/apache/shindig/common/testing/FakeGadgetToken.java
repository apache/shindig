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
package org.apache.shindig.common.testing;

import java.util.EnumSet;
import java.util.Map;

import org.apache.shindig.auth.AbstractSecurityToken;
import org.apache.shindig.auth.AuthenticationMode;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.auth.SecurityTokenCodec;
import org.apache.shindig.auth.SecurityTokenException;

/**
 * A fake SecurityToken implementation to help testing.
 */
public class FakeGadgetToken extends AbstractSecurityToken {

  private String authMode = AuthenticationMode.SECURITY_TOKEN_URL_PARAMETER.name();
  private String updated;

  public String getAuthenticationMode() {
    return authMode;
  }

  public boolean isAnonymous() {
    return false;
  }

  public FakeGadgetToken() {}
  /**
   * Create a fake security token from a map of parameter strings, keys are one of:
   * ownerId, viewerId, domain, appUrl, appId, trustedJson, module
   *
   * @param paramMap
   * @return The fake token
   */
  public FakeGadgetToken(Map<String, String> paramMap) {
    this(
      paramMap.get("appId"),
      paramMap.get("appUrl"),
      paramMap.get("domain"),
      paramMap.get("ownerId"),
      paramMap.get("trustedJson"),
      paramMap.get("viewerId"),
      paramMap.get("module")
    );
  }

  public FakeGadgetToken(String appId, String appUrl, String domain, String ownerId, String trustedJson, String viewerId, String moduleId) {
    setAppId(appId);
    setAppUrl(appUrl);
    setDomain(domain);
    setOwnerId(ownerId);
    setTrustedJson(trustedJson);
    setViewerId(viewerId);

    if (moduleId != null) {
      setModuleId(Long.parseLong(moduleId));
    }
  }

  /**
   * SecurityTokenCodec for testing - this allows passing around a
   * security token of format key=value&key2=value2, where key is one of:
   * ownerId, viewerId, domain, appUrl, appId, trustedJson, module
   */
  public static class Codec implements SecurityTokenCodec {
    public SecurityToken createToken(Map<String, String> tokenParameters)  {
      return new FakeGadgetToken(tokenParameters);
    }

    public String encodeToken(SecurityToken token) throws SecurityTokenException {
      return null; // NOT USED
    }

    public int getTokenTimeToLive() {
      return 0; // Not used.
    }

    public int getTokenTimeToLive(String container) {
      return 0; // Not used.
    }
  }

  public FakeGadgetToken setAuthenticationMode(String authMode) {
    this.authMode = authMode;
    return this;
  }

  public FakeGadgetToken setUpdatedToken(String updated) {
    this.updated = updated;
    return this;
  }

  public String getUpdatedToken() {
    return updated;
  }

  @Override
  protected EnumSet<Keys> getMapKeys() {
    return EnumSet.noneOf(Keys.class);
  }

  public FakeGadgetToken setAppUrl(String appUrl) {
    return (FakeGadgetToken)super.setAppUrl(appUrl);
  }

  public FakeGadgetToken setOwnerId(String ownerId) {
    return (FakeGadgetToken)super.setOwnerId(ownerId);
  }

  public FakeGadgetToken setViewerId(String viewerId) {
    return (FakeGadgetToken)super.setViewerId(viewerId);
  }

  public FakeGadgetToken setAppId(String appId) {
    return (FakeGadgetToken)super.setAppId(appId);
  }

  public FakeGadgetToken setDomain(String domain) {
    return (FakeGadgetToken)super.setDomain(domain);
  }

  public FakeGadgetToken setContainer(String container) {
    return (FakeGadgetToken)super.setContainer(container);
  }

  public FakeGadgetToken setModuleId(long moduleId) {
    return (FakeGadgetToken)super.setModuleId(moduleId);
  }

  public FakeGadgetToken setExpiresAt(Long expiresAt) {
    return (FakeGadgetToken)super.setExpiresAt(expiresAt);
  }

  public FakeGadgetToken setTrustedJson(String trustedJson) {
    return (FakeGadgetToken)super.setTrustedJson(trustedJson);
  }

  public FakeGadgetToken setActiveUrl(String activeUrl) {
    return (FakeGadgetToken)super.setActiveUrl(activeUrl);
  }
}
