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
package org.apache.shindig.sample.commoncontainer.auth;

import org.apache.shindig.auth.AbstractSecurityToken;
import org.apache.shindig.auth.AuthenticationMode;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.config.ContainerConfig;

/**
 * A stub token, with viewer, owner and other test values needed to test the common container sample
 */
public class TestSecurityTokenCodec extends AbstractSecurityToken implements
    SecurityToken {
  private final String container;
  private final long moduleId;
  private final String appUrl;
  private final Long expiresAt;

  public TestSecurityTokenCodec() {
    this(ContainerConfig.DEFAULT_CONTAINER);
  }

  public TestSecurityTokenCodec(String container) {
    this(container, 0L, "http://shindig.commoncontainer.sampleURL", null);
  }

  public TestSecurityTokenCodec(String container, long moduleId,
      String appUrl, Long expiresAt) {
    this.container = container;
    this.moduleId = moduleId;
    this.appUrl = appUrl;
    this.expiresAt = expiresAt;
  }

  public boolean isAnonymous() {
    return true;
  }

  public String getOwnerId() {
    return "john.doe";
  }

  public String getViewerId() {
    return "john.doe";
  }

  public String getAppId() {
    return "appid";
  }

  public String getDomain() {
    return "sampleDomain";
  }

  public String getContainer() {
    return "default";
  }

  public String getAppUrl() {
    return "http://shindig.commoncontainer.sampleURL";
  }

  public long getModuleId() {
    return 0;
  }

  public Long getExpiresAt() {
    return null;
  }

  public String getUpdatedToken() {
    return "";
  }

  public String getAuthenticationMode() {
    return AuthenticationMode.UNAUTHENTICATED.name();
  }

  public String getTrustedJson() {
    return "";
  }
}
