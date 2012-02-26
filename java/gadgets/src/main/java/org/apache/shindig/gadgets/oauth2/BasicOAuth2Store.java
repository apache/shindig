/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.shindig.gadgets.oauth2;

import java.util.Set;

import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.GadgetException.Code;
import org.apache.shindig.gadgets.oauth2.logger.FilteredLogger;
import org.apache.shindig.gadgets.oauth2.persistence.OAuth2Cache;
import org.apache.shindig.gadgets.oauth2.persistence.OAuth2CacheException;
import org.apache.shindig.gadgets.oauth2.persistence.OAuth2Client;
import org.apache.shindig.gadgets.oauth2.persistence.OAuth2Encrypter;
import org.apache.shindig.gadgets.oauth2.persistence.OAuth2PersistenceException;
import org.apache.shindig.gadgets.oauth2.persistence.OAuth2Persister;

import com.google.inject.Inject;

/**
 * see {@link OAuth2Store}
 *
 * Default OAuth2Store.
 *
 * Uses 3 Guice bindings to achieve storage implementation.
 *
 * 1) {@link OAuth2Persister} 2) {@link OAuth2Cache} 3) {@link OAuth2Encrypter}
 *
 */
public class BasicOAuth2Store implements OAuth2Store {
  private final static String LOG_CLASS = BasicOAuth2Store.class.getName();
  private static final FilteredLogger LOG = FilteredLogger
      .getFilteredLogger(BasicOAuth2Store.LOG_CLASS);

  private final OAuth2Cache cache;
  private final String globalRedirectUri;
  private final OAuth2Persister persister;

  @Inject
  public BasicOAuth2Store(final OAuth2Cache cache, final OAuth2Persister persister,
      final String globalRedirectUri) {
    this.cache = cache;
    this.persister = persister;
    this.globalRedirectUri = globalRedirectUri;
    if (BasicOAuth2Store.LOG.isLoggable()) {
      BasicOAuth2Store.LOG.log("this.cache = {0}", this.cache);
      BasicOAuth2Store.LOG.log("this.persister = {0}", this.persister);
      BasicOAuth2Store.LOG.log("this.globalRedirectUri = {0}", this.globalRedirectUri);
    }
  }

  public boolean clearCache() throws GadgetException {
    final boolean isLogging = BasicOAuth2Store.LOG.isLoggable();
    if (isLogging) {
      BasicOAuth2Store.LOG.entering(BasicOAuth2Store.LOG_CLASS, "clearCache");
    }

    try {
      this.cache.clearClients();
      this.cache.clearTokens();
    } catch (final OAuth2PersistenceException e) {
      if (isLogging) {
        BasicOAuth2Store.LOG.log("Error clearing OAuth2 cache", e);
      }
      throw new GadgetException(Code.OAUTH_STORAGE_ERROR, "Error clearing OAuth2 cache", e);
    }

    if (isLogging) {
      BasicOAuth2Store.LOG.exiting(BasicOAuth2Store.LOG_CLASS, "clearCache", true);
    }

    return true;
  }

  public OAuth2Token createToken() {
    final boolean isLogging = BasicOAuth2Store.LOG.isLoggable();
    if (isLogging) {
      BasicOAuth2Store.LOG.entering(BasicOAuth2Store.LOG_CLASS, "createToken");
    }

    final OAuth2Token ret = this.persister.createToken();

    if (isLogging) {
      BasicOAuth2Store.LOG.exiting(BasicOAuth2Store.LOG_CLASS, "clearCache", ret);
    }

    return ret;
  }

  public OAuth2Client getClient(final String gadgetUri, final String serviceName)
      throws GadgetException {
    final boolean isLogging = BasicOAuth2Store.LOG.isLoggable();
    if (isLogging) {
      BasicOAuth2Store.LOG.entering(BasicOAuth2Store.LOG_CLASS, "getClient", new Object[] {
          gadgetUri, serviceName });
    }

    final Integer index = this.cache.getClientIndex(gadgetUri, serviceName);

    if (isLogging) {
      BasicOAuth2Store.LOG.log("index = {0}", index);
    }

    OAuth2Client client = this.cache.getClient(index);

    if (isLogging) {
      BasicOAuth2Store.LOG.log("client from cache = {0}", client);
    }

    if (client == null) {
      try {
        client = this.persister.findClient(gadgetUri, serviceName);
        if (client != null) {
          this.cache.storeClient(client);
        }
      } catch (final OAuth2PersistenceException e) {
        if (isLogging) {
          BasicOAuth2Store.LOG.log("Error loading OAuth2 client ", e);
        }
        throw new GadgetException(Code.OAUTH_STORAGE_ERROR, "Error loading OAuth2 client "
            + serviceName, e);
      }
    }

    if (isLogging) {
      BasicOAuth2Store.LOG.exiting(BasicOAuth2Store.LOG_CLASS, "getClient", client);
    }

    return client;
  }

  public OAuth2Accessor getOAuth2Accessor(final Integer index) {
    final boolean isLogging = BasicOAuth2Store.LOG.isLoggable();
    if (isLogging) {
      BasicOAuth2Store.LOG.entering(BasicOAuth2Store.LOG_CLASS, "getOAuth2Accessor", index);
    }

    final OAuth2Accessor ret = this.cache.getOAuth2Accessor(index);

    if (isLogging) {
      BasicOAuth2Store.LOG.exiting(BasicOAuth2Store.LOG_CLASS, "getOAuth2Accessor", ret);
    }

    return ret;
  }

  public OAuth2Accessor getOAuth2Accessor(final String gadgetUri, final String serviceName,
      final String user, final String scope) throws GadgetException {
    final boolean isLogging = BasicOAuth2Store.LOG.isLoggable();
    if (isLogging) {
      BasicOAuth2Store.LOG.entering(BasicOAuth2Store.LOG_CLASS, "getOAuth2Accessor", new Object[] {
          gadgetUri, serviceName, user, scope });
    }

    final Integer index = this.cache.getOAuth2AccessorIndex(gadgetUri, serviceName, user, scope);

    OAuth2Accessor ret = this.cache.getOAuth2Accessor(index);

    if ((ret == null) || (!ret.isValid())) {
      final OAuth2Client client = this.getClient(gadgetUri, serviceName);

      if (client != null) {
        final OAuth2Token accessToken = this.getToken(gadgetUri, serviceName, user, scope,
            OAuth2Token.Type.ACCESS);
        final OAuth2Token refreshToken = this.getToken(gadgetUri, serviceName, user, scope,
            OAuth2Token.Type.REFRESH);

        final BasicOAuth2Accessor newAccessor = new BasicOAuth2Accessor(gadgetUri, serviceName,
            user, scope, client.isAllowModuleOverride(), this, this.globalRedirectUri);
        newAccessor.setAccessToken(accessToken);
        newAccessor.setAuthorizationUrl(client.getAuthorizationUrl());
        newAccessor.setClientAuthenticationType(client.getClientAuthenticationType());
        newAccessor.setAuthorizationHeader(client.isAuthorizationHeader());
        newAccessor.setUrlParameter(client.isUrlParameter());
        newAccessor.setClientId(client.getClientId());
        newAccessor.setClientSecret(client.getClientSecret());
        newAccessor.setGrantType(client.getGrantType());
        newAccessor.setRedirectUri(client.getRedirectUri());
        newAccessor.setRefreshToken(refreshToken);
        newAccessor.setTokenUrl(client.getTokenUrl());
        newAccessor.setType(client.getType());
        ret = newAccessor;

        this.storeOAuth2Accessor(ret);
      }
    }

    if (isLogging) {
      BasicOAuth2Store.LOG.exiting(BasicOAuth2Store.LOG_CLASS, "getOAuth2Accessor", ret);
    }

    return ret;
  }

  public Integer getOAuth2AccessorIndex(final String gadgetUri, final String serviceName,
      final String user, final String scope) {
    return this.cache.getOAuth2AccessorIndex(gadgetUri, serviceName, user, scope);
  }

  public OAuth2Token getToken(final String gadgetUri, final String serviceName, final String user,
      final String scope, final OAuth2Token.Type type) throws GadgetException {

    final boolean isLogging = BasicOAuth2Store.LOG.isLoggable();
    if (isLogging) {
      BasicOAuth2Store.LOG.entering(BasicOAuth2Store.LOG_CLASS, "getToken", new Object[] {
          gadgetUri, serviceName, user, scope, type });
    }

    final Integer index = this.cache.getTokenIndex(gadgetUri, serviceName, user, scope, type);
    OAuth2Token token = this.cache.getToken(index);
    if (token == null) {
      try {
        token = this.persister.findToken(gadgetUri, serviceName, user, scope, type);
        if (token != null) {
          this.cache.storeToken(token);
        }
      } catch (final OAuth2PersistenceException e) {
        throw new GadgetException(Code.OAUTH_STORAGE_ERROR, "Error loading OAuth2 token " + index,
            e);
      }
    }

    if (isLogging) {
      BasicOAuth2Store.LOG.exiting(BasicOAuth2Store.LOG_CLASS, "getToken", token);
    }

    return token;
  }

  public boolean init() throws GadgetException {
    final boolean isLogging = BasicOAuth2Store.LOG.isLoggable();
    if (isLogging) {
      BasicOAuth2Store.LOG.entering(BasicOAuth2Store.LOG_CLASS, "init");
    }

    this.clearCache();

    try {
      final Set<OAuth2Client> clients = this.persister.loadClients();
      if (isLogging) {
        BasicOAuth2Store.LOG.log("clients = {0}", clients);
      }
      this.cache.storeClients(clients);
    } catch (final OAuth2PersistenceException e) {
      throw new GadgetException(Code.OAUTH_STORAGE_ERROR, "Error loading OAuth2 clients", e);
    }

    try {
      final Set<OAuth2Token> tokens = this.persister.loadTokens();
      if (isLogging) {
        BasicOAuth2Store.LOG.log("tokens = {0}", tokens);
      }
      this.cache.storeTokens(tokens);
    } catch (final OAuth2PersistenceException e) {
      throw new GadgetException(Code.OAUTH_STORAGE_ERROR, "Error loading OAuth2 tokens", e);
    }

    if (isLogging) {
      BasicOAuth2Store.LOG.exiting(BasicOAuth2Store.LOG_CLASS, "init", true);
    }

    return true;
  }

  public OAuth2Accessor removeOAuth2Accessor(final OAuth2Accessor accessor) {
    final boolean isLogging = BasicOAuth2Store.LOG.isLoggable();
    if (isLogging) {
      BasicOAuth2Store.LOG.entering(BasicOAuth2Store.LOG_CLASS, "removeOAuth2Accessor", accessor);
    }

    final OAuth2Accessor ret = null;

    if (accessor != null) {
      final Integer index = this.cache.getOAuth2AccessorIndex(accessor.getGadgetUri(),
          accessor.getServiceName(), accessor.getUser(), accessor.getScope());
      return this.cache.removeOAuth2Accessor(index);
    }

    if (isLogging) {
      BasicOAuth2Store.LOG.exiting(BasicOAuth2Store.LOG_CLASS, "removeOAuth2Accessor", ret);
    }

    return ret;
  }

  public OAuth2Token removeToken(final OAuth2Token token) throws GadgetException {
    final boolean isLogging = BasicOAuth2Store.LOG.isLoggable();
    if (isLogging) {
      BasicOAuth2Store.LOG.entering(BasicOAuth2Store.LOG_CLASS, "removeToken", token);
    }

    if (token != null) {
      if (isLogging) {
        BasicOAuth2Store.LOG.exiting(BasicOAuth2Store.LOG_CLASS, "removeToken", token);
      }

      return this.removeToken(token.getGadgetUri(), token.getServiceName(), token.getUser(),
          token.getScope(), token.getType());
    }

    if (isLogging) {
      BasicOAuth2Store.LOG.exiting(BasicOAuth2Store.LOG_CLASS, "removeOAuth2Accessor", null);
    }

    return null;
  }

  public OAuth2Token removeToken(final String gadgetUri, final String serviceName,
      final String user, final String scope, final OAuth2Token.Type type) throws GadgetException {

    final boolean isLogging = BasicOAuth2Store.LOG.isLoggable();
    if (isLogging) {
      BasicOAuth2Store.LOG.entering(BasicOAuth2Store.LOG_CLASS, "removeToken", new Object[] {
          gadgetUri, serviceName, user, scope, type });
    }

    final Integer index = this.cache.getTokenIndex(gadgetUri, serviceName, user, scope, type);
    try {
      final OAuth2Token token = this.cache.removeToken(index);
      if (token != null) {
        this.persister.removeToken(gadgetUri, serviceName, user, scope, type);
      }

      if (isLogging) {
        BasicOAuth2Store.LOG.exiting(BasicOAuth2Store.LOG_CLASS, "removeToken", token);
      }

      return token;
    } catch (final OAuth2PersistenceException e) {
      if (isLogging) {
        BasicOAuth2Store.LOG.log("Error loading OAuth2 token ", e);
      }
      throw new GadgetException(Code.OAUTH_STORAGE_ERROR, "Error loading OAuth2 token "
          + serviceName, e);
    }
  }

  public static boolean runImport(final OAuth2Persister source, final OAuth2Persister target,
      final boolean clean) {
    if (BasicOAuth2Store.LOG.isLoggable()) {
      BasicOAuth2Store.LOG.entering(BasicOAuth2Store.LOG_CLASS, "runImport", new Object[] { source,
          target, clean });
    }

    // No import for default persistence
    return false;
  }

  public void setToken(final OAuth2Token token) throws GadgetException {
    final boolean isLogging = BasicOAuth2Store.LOG.isLoggable();
    if (isLogging) {
      BasicOAuth2Store.LOG.entering(BasicOAuth2Store.LOG_CLASS, "setToken", token);
    }

    if (token != null) {
      final Integer index = this.cache.getTokenIndex(token);
      final OAuth2Token existingToken = this.getToken(token.getGadgetUri(), token.getServiceName(),
          token.getUser(), token.getScope(), token.getType());
      try {
        if (existingToken == null) {
          this.persister.insertToken(token);
        } else {
          this.cache.removeToken(index);
          this.persister.updateToken(token);
        }
        this.cache.storeToken(token);
      } catch (final OAuth2CacheException e) {
        if (isLogging) {
          BasicOAuth2Store.LOG.log("Error storing OAuth2 token " + index, e);
        }
        throw new GadgetException(Code.OAUTH_STORAGE_ERROR, "Error storing OAuth2 token " + index,
            e);
      } catch (final OAuth2PersistenceException e) {
        if (isLogging) {
          BasicOAuth2Store.LOG.log("Error storing OAuth2 token " + index, e);
        }
        throw new GadgetException(Code.OAUTH_STORAGE_ERROR, "Error storing OAuth2 token " + index,
            e);
      }
    }

    if (isLogging) {
      BasicOAuth2Store.LOG.exiting(BasicOAuth2Store.LOG_CLASS, "setToken");
    }
  }

  public void storeOAuth2Accessor(final OAuth2Accessor accessor) {
    final boolean isLogging = BasicOAuth2Store.LOG.isLoggable();
    if (isLogging) {
      BasicOAuth2Store.LOG.entering(BasicOAuth2Store.LOG_CLASS, "storeOAuth2Accessor", accessor);
    }

    this.cache.storeOAuth2Accessor(accessor);

    if (isLogging) {
      BasicOAuth2Store.LOG.exiting(BasicOAuth2Store.LOG_CLASS, "storeOAuth2Accessor");
    }
  }
}
