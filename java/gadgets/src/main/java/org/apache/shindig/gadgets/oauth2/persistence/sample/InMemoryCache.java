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
package org.apache.shindig.gadgets.oauth2.persistence.sample;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.shindig.gadgets.oauth2.OAuth2Accessor;
import org.apache.shindig.gadgets.oauth2.OAuth2CallbackState;
import org.apache.shindig.gadgets.oauth2.OAuth2Token;
import org.apache.shindig.gadgets.oauth2.OAuth2Token.Type;
import org.apache.shindig.gadgets.oauth2.persistence.MapCache;
import org.apache.shindig.gadgets.oauth2.persistence.OAuth2Cache;
import org.apache.shindig.gadgets.oauth2.persistence.OAuth2Client;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * {@link OAuth2Cache} implementation using in-memory {@link HashMap}s.
 *
 */
@Singleton
public class InMemoryCache extends MapCache {
  private final Map<String, OAuth2Accessor> accessors;
  private final Map<String, OAuth2Client> clients;
  private final Map<String, OAuth2Token> tokens;

  @Inject
  public InMemoryCache() {
    final Map<String, OAuth2Token> tMap = Maps.newHashMap();
    this.tokens = Collections.synchronizedMap(tMap);
    final Map<String, OAuth2Client> cMap = Maps.newHashMap();
    this.clients = Collections.synchronizedMap(cMap);
    final Map<String, OAuth2Accessor> aMap = Maps.newHashMap();
    this.accessors = Collections.synchronizedMap(aMap);
  }

  @Override
  protected Map<String, OAuth2Client> getClientMap() {
    return this.clients;
  }

  @Override
  protected Map<String, OAuth2Token> getTokenMap() {
    return this.tokens;
  }

  @Override
  protected Map<String, OAuth2Accessor> getAccessorMap() {
    return this.accessors;
  }

  // getXXXKey() methods are overridden here even though they don't do anything
  // Since this is a sample class it's to make it evident to other developers
  // that they can override key management themselves.

  @Override
  protected String getClientKey(final OAuth2Client client) {
    return super.getClientKey(client);
  }

  @Override
  protected String getClientKey(final String gadgetUri, final String serviceName) {
    return super.getClientKey(gadgetUri, serviceName);
  }

  @Override
  protected String getAccessorKey(final OAuth2CallbackState state) {
    return super.getAccessorKey(state);
  }

  @Override
  protected String getAccessorKey(final OAuth2Accessor accessor) {
    return super.getAccessorKey(accessor);
  }

  @Override
  protected String getTokenKey(final String gadgetUri, final String serviceName, final String user,
          final String scope, final Type type) {
    return super.getTokenKey(gadgetUri, serviceName, user, scope, type);
  }

  @Override
  protected String getTokenKey(final OAuth2Token token) {
    return super.getTokenKey(token);
  }
}
