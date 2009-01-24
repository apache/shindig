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
 * A special class of Token representing the anonymous viewer/owner
 *
 * All methods except for isAnonymous will throw IllegalArgumentExceptions
 */
public class AnonymousSecurityToken implements SecurityToken {

  private String container;

  public AnonymousSecurityToken() {
    this.container = "default";
  }
  public AnonymousSecurityToken(String container) {
    this.container = container;
  }

  public boolean isAnonymous() {
    return true;
  }

  public String toSerialForm() {
    return "";
  }

  public String getOwnerId() {
    return "";
  }

  public String getViewerId() {
    return "";
  }

  public String getAppId() {
    return "";
  }

  public String getDomain() {
    return "";
  }

  public String getContainer() {
    return this.container;
  }

  public String getAppUrl() {
    return "";
  }

  public long getModuleId() {
    return 0L;
  }

  public String getUpdatedToken() {
    return "";
  }

  public String getTrustedJson() {
    return "";
  }
}