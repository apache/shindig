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
package org.apache.shindig.common;

/**
 * A special class of Token representing the anonymous viewer/owner
 *
 * All methods except for isAnonymous will throw IllegalArgumentExceptions
 */
public class AnonymousSecurityToken implements SecurityToken {
  private static final SecurityToken instance = new AnonymousSecurityToken();

  /**
   * Private method, please use getInstance()
   */
  private AnonymousSecurityToken() {
  }

  public static SecurityToken getInstance() {
    return instance;
  }

  public String toSerialForm() {
    throw new IllegalArgumentException();
  }

  public String getOwnerId() {
    throw new IllegalArgumentException();
  }

  public String getViewerId() {
    throw new IllegalArgumentException();
  }

  public String getAppId() {
    throw new IllegalArgumentException();
  }

  public String getDomain() {
    throw new IllegalArgumentException();
  }

  public String getAppUrl() {
    throw new IllegalArgumentException();
  }

  public long getModuleId() {
    throw new IllegalArgumentException();
  }

  public String getUpdatedToken() {
    throw new IllegalArgumentException();
  }

  public String getTrustedJson() {
    throw new IllegalArgumentException();
  }

  public boolean isAnonymous() {
    return true;
  }
}
