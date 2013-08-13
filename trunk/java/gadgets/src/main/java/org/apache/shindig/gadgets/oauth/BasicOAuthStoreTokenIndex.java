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
package org.apache.shindig.gadgets.oauth;

import com.google.common.base.Objects;

/**
 * Simple class representing oauth token store
 */
public class BasicOAuthStoreTokenIndex {

  private String userId;
  private String gadgetUri;
  private long moduleId;
  private String tokenName;
  private String serviceName;

  public String getUserId() {
    return userId;
  }
  public void setUserId(String userId) {
    this.userId = userId;
  }
  public String getGadgetUri() {
    return gadgetUri;
  }
  public void setGadgetUri(String gadgetUri) {
    this.gadgetUri = gadgetUri;
  }
  public long getModuleId() {
    return moduleId;
  }
  public void setModuleId(long moduleId) {
    this.moduleId = moduleId;
  }
  public String getTokenName() {
    return tokenName;
  }
  public void setTokenName(String tokenName) {
    this.tokenName = tokenName;
  }
  public String getServiceName() {
    return serviceName;
  }
  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(gadgetUri, moduleId, serviceName, tokenName, userId);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof BasicOAuthStoreTokenIndex)) {
      return false;
    }
    final BasicOAuthStoreTokenIndex other = (BasicOAuthStoreTokenIndex) obj;
    if (gadgetUri == null) {
      if (other.gadgetUri != null) return false;
    } else if (!gadgetUri.equals(other.gadgetUri)) return false;
    if (moduleId != other.moduleId) return false;
    if (serviceName == null) {
      if (other.serviceName != null) return false;
    } else if (!serviceName.equals(other.serviceName)) return false;
    if (tokenName == null) {
      if (other.tokenName != null) return false;
    } else if (!tokenName.equals(other.tokenName)) return false;
    if (userId == null) {
      if (other.userId != null) return false;
    } else if (!userId.equals(other.userId)) return false;
    return true;
  }
}
