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

import org.apache.shindig.gadgets.oauth2.BasicOAuth2Store;
import org.apache.shindig.gadgets.oauth2.OAuth2Accessor;
import org.apache.shindig.gadgets.oauth2.OAuth2Store;
import org.apache.shindig.gadgets.oauth2.OAuth2Token;
import org.apache.shindig.gadgets.oauth2.persistence.sample.JSONOAuth2Persister;

import java.util.Set;

/**
 * Interface, used primarily by {@link OAuth2Store}, to manage {@link OAuth2Accessor} and
 * {@link OAuth2Token} storage.
 *
 * An {@link OAuth2Accessor} has the same basic information as the {@link OAuth2Client}, merged with
 * gadget spec and request prefs.
 *
 * {@link OAuth2Accessor} is short lived, for the life of one request.
 *
 * {@link OAuth2Clients} is intended to be persisted and cached.
 *
 * The default persister for shindig is {@link JSONOAuth2Persister}
 *
 */
public interface OAuth2Persister {
  /**
   * Retrieves a client from the persistence layer. Returns <code>null</code> if not found.
   *
   * @param gadgetUri
   * @param serviceName
   * @return the client in the given mapping, must return <code>null</code> if the client is not
   *         found
   * @throws OAuth2PersistenceException
   */
  OAuth2Client findClient(String gadgetUri, String serviceName) throws OAuth2PersistenceException;

  /**
   *
   * @param gadgetUri
   * @param serviceName
   * @param user
   * @param scope
   * @param type
   * @return the token in the given mapping
   * @throws OAuth2PersistenceException
   */
  OAuth2Token findToken(String gadgetUri, String serviceName, String user, String scope,
          OAuth2Token.Type type) throws OAuth2PersistenceException;

  /**
   * Inserts a new {@link OAuth2Token} into the persistence layer.
   *
   * @param token
   * @throws OAuth2PersistenceException
   */
  void insertToken(OAuth2Token token) throws OAuth2PersistenceException;

  /**
   * Load all the clients from the persistence layer. The {@link BasicOAuth2Store#init()} method
   * will call this to prepopulate the cache.
   *
   * @return
   * @throws OAuth2PersistenceException
   */
  Set<OAuth2Client> loadClients() throws OAuth2PersistenceException;

  /**
   * Load all the tokens from the persistence layer. The {@link BasicOAuth2Store#init()} method will
   * call this to prepopulate the cache.
   *
   * @return
   * @throws OAuth2PersistenceException
   */
  Set<OAuth2Token> loadTokens() throws OAuth2PersistenceException;

  /**
   * Removes a token from the persistence layer.
   *
   * @param gadgetUri
   * @param serviceName
   * @param user
   * @param scope
   * @param type
   * @return
   * @throws OAuth2PersistenceException
   */
  boolean removeToken(String gadgetUri, String serviceName, String user, String scope,
          OAuth2Token.Type type) throws OAuth2PersistenceException;

  /**
   * Updates an existing {@link OAuth2Token} in the persistence layer.
   *
   * @param token
   * @throws OAuth2PersistenceException
   */
  void updateToken(OAuth2Token token) throws OAuth2PersistenceException;
}
