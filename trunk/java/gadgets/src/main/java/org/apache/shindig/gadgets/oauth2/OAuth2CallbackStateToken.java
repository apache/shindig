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
package org.apache.shindig.gadgets.oauth2;

import org.apache.shindig.auth.AbstractSecurityToken;

import java.io.Serializable;
import java.util.EnumSet;
import java.util.Map;

/**
 *
 */
public class OAuth2CallbackStateToken extends AbstractSecurityToken implements Serializable {
  private static final long serialVersionUID = -3913197153778386101L;
  private static final EnumSet<Keys> MAP_KEYS = EnumSet.of(Keys.EXPIRES);
  private static final String GADGET_URI = "g";
  private static final String SERVICE_NAME = "sn";
  private static final String USER = "u";
  private static final String SCOPE = "sc";

  private String gadgetUri;
  private String serviceName;
  private String user;
  private String scope;

  OAuth2CallbackStateToken() {
    // used by OAuth2CallbackState
  }

  public OAuth2CallbackStateToken(final Map<String, String> values) {
    this.loadFromMap(values);
  }

  @Override
  protected AbstractSecurityToken loadFromMap(final Map<String, String> map) {
    super.loadFromMap(map);
    final String g = map.get(OAuth2CallbackStateToken.GADGET_URI);
    if (g != null) {
      this.setGadgetUri(g);
    }

    final String sn = map.get(OAuth2CallbackStateToken.SERVICE_NAME);
    if (sn != null) {
      this.setServiceName(sn);
    }

    final String u = map.get(OAuth2CallbackStateToken.USER);
    if (u != null) {
      this.setUser(u);
    }

    final String sc = map.get(OAuth2CallbackStateToken.SCOPE);
    if (sc != null) {
      this.setScope(sc);
    }

    return this;
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

  @Override
  protected EnumSet<Keys> getMapKeys() {
    return OAuth2CallbackStateToken.MAP_KEYS;
  }

  public String getGadgetUri() {
    return this.gadgetUri;
  }

  public String getServiceName() {
    return this.serviceName;
  }

  public String getUser() {
    return this.user;
  }

  public String getScope() {
    return this.scope;
  }

  public OAuth2CallbackStateToken setGadgetUri(final String gadgetUri) {
    this.gadgetUri = gadgetUri;
    return this;
  }

  public OAuth2CallbackStateToken setServiceName(final String serviceName) {
    this.serviceName = serviceName;
    return this;
  }

  public OAuth2CallbackStateToken setUser(final String user) {
    this.user = user;
    return this;
  }

  public OAuth2CallbackStateToken setScope(final String scope) {
    this.scope = scope;
    return this;
  }

  /**
   * Returns token time to live in seconds.
   */
  @Override
  protected int getMaxTokenTTL() {
    return 600;
  }

  @Override
  public Map<String, String> toMap() {
    final Map<String, String> map = super.toMap();
    final String g = this.getGadgetUri();
    if (g != null) {
      map.put(OAuth2CallbackStateToken.GADGET_URI, g);
    }

    final String sn = this.getServiceName();
    if (sn != null) {
      map.put(OAuth2CallbackStateToken.SERVICE_NAME, sn);
    }

    final String u = this.getUser();
    if (u != null) {
      map.put(OAuth2CallbackStateToken.USER, u);
    }

    final String sc = this.getScope();
    if (sc != null) {
      map.put(OAuth2CallbackStateToken.SCOPE, sc);
    }

    return map;
  }
}
