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

/**
 * A SecurityToken that forwards all methods calls to another token. Subclasses should override
 * one or more methods to change or add behavior.
 *
 * @since 2.0.0
 */
public abstract class ForwardingSecurityToken implements SecurityToken {
  SecurityToken delegate;
  protected ForwardingSecurityToken(SecurityToken delegate) {
    this.delegate = delegate;
  }

  public String getOwnerId() {
    return delegate.getOwnerId();
  }

  public String getViewerId() {
    return delegate.getViewerId();
  }

  public String getAppId() {
    return delegate.getAppId();
  }

  public String getDomain() {
    return delegate.getDomain();
  }

  public String getContainer() {
    return delegate.getContainer();
  }

  public String getAppUrl() {
    return delegate.getAppUrl();
  }

  public long getModuleId() {
    return delegate.getModuleId();
  }

  public Long getExpiresAt() {
    return delegate.getExpiresAt();
  }

  public boolean isExpired() {
    return delegate.isExpired();
  }

  public String getUpdatedToken() {
    return delegate.getUpdatedToken();
  }

  public String getAuthenticationMode() {
    return delegate.getAuthenticationMode();
  }

  public String getTrustedJson() {
    return delegate.getTrustedJson();
  }

  public boolean isAnonymous() {
    return delegate.isAnonymous();
  }

  public String getActiveUrl() {
    return delegate.getActiveUrl();
  }
}
