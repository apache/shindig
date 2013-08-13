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

import java.util.EnumSet;
import java.util.Map;

import org.apache.shindig.auth.AbstractSecurityToken;


/**
 * Token used to persist information for the {@link OAuthCallbackState}
 */
public class OAuthCallbackStateToken extends AbstractSecurityToken {
  private static final EnumSet<Keys> MAP_KEYS = EnumSet.of(Keys.EXPIRES);
  private static final String REAL_CALLBACK_URL_KEY = "u";

  private String realCallbackUrl;

  public OAuthCallbackStateToken () {}

  public OAuthCallbackStateToken (Map<String, String> values) {
    loadFromMap(values);
    setRealCallbackUrl(values.get(REAL_CALLBACK_URL_KEY));
  }

  public String getUpdatedToken() {
    return null;
  }

  public String getAuthenticationMode() {
    return null;
  }

  public boolean isAnonymous() {
    return false;
  }

  protected EnumSet<Keys> getMapKeys() {
    return MAP_KEYS;
  }

  public OAuthCallbackStateToken setRealCallbackUrl(String realCallbackUrl) {
    this.realCallbackUrl = realCallbackUrl;
    return this;
  }

  @Override
  protected int getMaxTokenTTL() {
    return 600;
  }

  public String getRealCallbackUrl() {
    return realCallbackUrl;
  }

  @Override
  public Map<String, String> toMap() {
    Map<String, String> map = super.toMap();
    map.put(REAL_CALLBACK_URL_KEY, getRealCallbackUrl());
    return map;
  }
}

