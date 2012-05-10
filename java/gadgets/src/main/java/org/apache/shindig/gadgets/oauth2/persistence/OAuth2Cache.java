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
package org.apache.shindig.gadgets.oauth2.persistence;

import org.apache.shindig.gadgets.oauth2.OAuth2Accessor;
import org.apache.shindig.gadgets.oauth2.OAuth2CallbackState;
import org.apache.shindig.gadgets.oauth2.OAuth2Token;

import java.util.Collection;

/**
 * Used by {@link OAuth2Store} to cache OAuth2 data.
 *
 * Default implementation is in-memory HashMaps for shindig.
 *
 */
public interface OAuth2Cache {
  /**
   * Clears all cached {@link OAuth2Client}s.
   *
   * @throws OAuth2CacheException
   */
  void clearAccessors() throws OAuth2CacheException;

  /**
   * Clears all cached {@link OAuth2Client}s.
   *
   * @throws OAuth2CacheException
   */
  void clearClients() throws OAuth2CacheException;

  /**
   * Clears all cached {@link OAuth2Token}s.
   *
   * @throws OAuth2CacheException
   */
  void clearTokens() throws OAuth2CacheException;

  /**
   * Find an {@link OAuth2Client}.
   *
   * @param gadgetUri
   * @param serviceName
   * @return OAuth2Client
   */
  OAuth2Client getClient(String gadgetUri, String serviceName);

  /**
   * Find an {@link OAuth2Accessor} by state.
   *
   * @param state
   * @return OAuth2Accessor
   */
  OAuth2Accessor getOAuth2Accessor(OAuth2CallbackState state);

  /**
   * Find an {@link OAuth2Token} based on index
   *
   * @param gadgetUri
   * @param serviceName
   * @param user
   * @param scope
   * @param type
   * @return an OAuth2Token
   */
  OAuth2Token getToken(String gadgetUri, String serviceName, String user, String scope,
          OAuth2Token.Type type);

  /**
   * @return true if the cache has already been primed. (presumably by another node.)
   */
  boolean isPrimed();

  /**
   * Remove the given client;
   *
   * @param client
   * @return the client that was removed, or <code>null</code> if removal failed
   */
  OAuth2Client removeClient(OAuth2Client client);

  /**
   * Remove the given accessor.
   *
   * @param accessor
   * @return the accessor that was removed, or <code>null</code> if removal failed
   */
  OAuth2Accessor removeOAuth2Accessor(OAuth2Accessor accessor);

  /**
   * Remove the given token;
   *
   * @param token
   * @return the token that was removed, or <code>null</code> if removal failed
   */
  OAuth2Token removeToken(OAuth2Token token);

  /**
   * Stores the given client.
   *
   * @param index
   * @param client
   * @throws OAuth2CacheException
   */
  void storeClient(OAuth2Client client) throws OAuth2CacheException;

  /**
   * Store all clients in the collection.
   *
   * @param clients
   * @throws OAuth2CacheException
   */
  void storeClients(Collection<OAuth2Client> clients) throws OAuth2CacheException;

  /**
   * Stores the given accessor.
   *
   * @param accessor
   */
  void storeOAuth2Accessor(OAuth2Accessor accessor);

  /**
   * Stores the given token.
   */
  void storeToken(OAuth2Token token) throws OAuth2CacheException;

  /**
   * Stores all tokens in the collection.
   *
   * @param tokens
   * @throws OAuth2CacheException
   */
  void storeTokens(Collection<OAuth2Token> tokens) throws OAuth2CacheException;
}
