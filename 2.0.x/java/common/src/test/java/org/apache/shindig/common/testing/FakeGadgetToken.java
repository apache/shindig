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

import org.apache.shindig.auth.AbstractSecurityToken;
import org.apache.shindig.auth.AuthenticationMode;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.auth.SecurityTokenCodec;

import com.google.common.collect.Maps;
import org.apache.shindig.auth.SecurityTokenException;

import java.util.Map;

/**
 * A fake SecurityToken implementation to help testing.
 */
public class FakeGadgetToken extends AbstractSecurityToken implements SecurityToken {

  private String updatedToken = null;
  private String trustedJson = null;

  private String ownerId = null;
  private String viewerId = null;
  private String appId = null;
  private String domain = null;
  private String container = null;
  private String appUrl = null;
  private String activeUrl = null;
  private String authMode = AuthenticationMode.SECURITY_TOKEN_URL_PARAMETER.name();
  private int moduleId = 0;
  private Long expiresAt = null;

  public FakeGadgetToken setUpdatedToken(String updatedToken) {
    this.updatedToken = updatedToken;
    return this;
  }

  public FakeGadgetToken setTrustedJson(String trustedJson) {
    this.trustedJson = trustedJson;
    return this;
  }

  public FakeGadgetToken setOwnerId(String ownerId) {
    this.ownerId = ownerId;
    return this;
  }

  public FakeGadgetToken setViewerId(String viewerId) {
    this.viewerId = viewerId;
    return this;
  }

  public FakeGadgetToken setAppId(String appId) {
    this.appId = appId;
    return this;
  }

  public FakeGadgetToken setDomain(String domain) {
    this.domain = domain;
    return this;
  }

  public FakeGadgetToken setContainer(String container) {
    this.container = container;
    return this;
  }

  public FakeGadgetToken setAppUrl(String appUrl) {
    this.appUrl = appUrl;
    return this;
  }

  public FakeGadgetToken setModuleId(int moduleId) {
    this.moduleId = moduleId;
    return this;
  }

  public FakeGadgetToken setExpiresAt(Long expiresAt) {
    this.expiresAt = expiresAt;
    return this;
  }

  public FakeGadgetToken setActiveUrl(String activeUrl) {
    this.activeUrl = activeUrl;
    return this;
  }

  public void setAuthenticationMode(String authMode) {
    this.authMode = authMode;
  }

  public String getOwnerId() {
    return ownerId;
  }

  public String getViewerId() {
    return viewerId;
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

  public long getModuleId() {
    return moduleId;
  }

  public Long getExpiresAt() {
    return expiresAt;
  }

  public String getUpdatedToken() {
    return updatedToken;
  }

  public String getAuthenticationMode() {
    return authMode;
  }

  public String getTrustedJson() {
    return trustedJson;
  }

  public boolean isAnonymous() {
    return false;
  }
  
  public String getActiveUrl() {
    return activeUrl;
  }

  /**
   * Create a fake security token parameter string, allows passing around a
   * security token of format key=value&key2=value2, where key is one of:
   * ownerId, viewerId, domain, appUrl, appId, trustedJson, module.
   *
   * Useful for creating tokens that can be decoded with FakeGadgetToken.Decoder
   *
   * @param tokenString the parameter string
   * @return The fake token
   */
  public static SecurityToken createToken(String tokenString)  {
    String keyValuePairs[] = tokenString.split("&");
    Map<String, String> paramMap = Maps.newHashMap();

    for (String keyValuePair : keyValuePairs) {
      String[] keyAndValue = keyValuePair.split("=");
      if (keyAndValue.length == 2) {
        paramMap.put(keyAndValue[0], keyAndValue[1]);
      }
    }

    return createToken(paramMap);
  }

  /**
   * Create a fake security token from a map of parameter strings, keys are one of:
   * ownerId, viewerId, domain, appUrl, appId, trustedJson, module
   *
   * @param paramMap
   * @return The fake token
   */
  public static FakeGadgetToken createToken(Map<String, String> paramMap) {
    FakeGadgetToken fakeToken = new FakeGadgetToken();

    fakeToken.setAppId(paramMap.get("appId"));
    fakeToken.setAppUrl(paramMap.get("appUrl"));
    fakeToken.setDomain(paramMap.get("domain"));
    fakeToken.setOwnerId(paramMap.get("ownerId"));
    fakeToken.setTrustedJson(paramMap.get("trustedJson"));
    fakeToken.setViewerId(paramMap.get("viewerId"));

    String moduleIdStr = paramMap.get("module");
    if (moduleIdStr != null) {
      fakeToken.setModuleId(Integer.parseInt(moduleIdStr));
    }

    return fakeToken;
  }

  /**
   * SecurityTokenCodec for testing - this allows passing around a
   * security token of format key=value&key2=value2, where key is one of:
   * ownerId, viewerId, domain, appUrl, appId, trustedJson, module
   */
  public static class Codec implements SecurityTokenCodec {
    public SecurityToken createToken(Map<String, String> tokenParameters)  {
      return FakeGadgetToken.createToken(tokenParameters);
    }

    public String encodeToken(SecurityToken token) throws SecurityTokenException {
      return null; // NOT USED
    }
  }
}
