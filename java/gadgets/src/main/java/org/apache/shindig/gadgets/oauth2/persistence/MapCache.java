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
import org.apache.shindig.gadgets.oauth2.OAuth2Token.Type;

import java.util.Collection;
import java.util.Map;

public abstract class MapCache implements OAuth2Cache {
  protected abstract Map<String, OAuth2Client> getClientMap();

  protected abstract Map<String, OAuth2Token> getTokenMap();

  protected abstract Map<String, OAuth2Accessor> getAccessorMap();

  public void clearClients() throws OAuth2CacheException {
    this.getClientMap().clear();
  }

  public void clearTokens() throws OAuth2CacheException {
    this.getTokenMap().clear();
  }

  public void clearAccessors() {
    this.getAccessorMap().clear();
  }

  public void storeTokens(final Collection<OAuth2Token> storeTokens) throws OAuth2CacheException {
    if (storeTokens != null) {
      for (final OAuth2Token token : storeTokens) {
        this.storeToken(token);
      }
    }
  }

  public boolean isPrimed() {
    return false;
  }

  public OAuth2Client getClient(final String gadgetUri, final String serviceName) {
    OAuth2Client ret = null;
    final String clientKey = this.getClientKey(gadgetUri, serviceName);
    if (clientKey != null) {
      ret = this.getClientMap().get(clientKey);
    }

    return ret;
  }

  public OAuth2Accessor getOAuth2Accessor(final OAuth2CallbackState state) {
    OAuth2Accessor ret = null;
    final String accessorKey = this.getAccessorKey(state);
    if (accessorKey != null) {
      ret = this.getAccessorMap().get(accessorKey);
    }

    return ret;
  }

  public OAuth2Token getToken(final String gadgetUri, final String serviceName, final String user,
          final String scope, final Type type) {
    OAuth2Token ret = null;
    final String tokenKey = this.getTokenKey(gadgetUri, serviceName, user, scope, type);
    if (tokenKey != null) {
      ret = this.getTokenMap().get(tokenKey);
    }

    return ret;
  }

  public OAuth2Client removeClient(final OAuth2Client client) {
    OAuth2Client ret = null;
    final String clientKey = this.getClientKey(client);
    if (clientKey != null) {
      ret = this.getClientMap().remove(clientKey);
    }

    return ret;
  }

  public OAuth2Accessor removeOAuth2Accessor(final OAuth2Accessor accessor) {
    OAuth2Accessor ret = null;
    final String accessorKey = this.getAccessorKey(accessor);
    if (accessorKey != null) {
      ret = this.getAccessorMap().remove(accessorKey);
    }

    return ret;
  }

  public OAuth2Token removeToken(final OAuth2Token token) {
    OAuth2Token ret = null;
    final String tokenKey = this.getTokenKey(token);
    if (tokenKey != null) {
      ret = this.getTokenMap().remove(tokenKey);
    }

    return ret;
  }

  public void storeClient(final OAuth2Client client) throws OAuth2CacheException {
    if (client != null) {
      final String clientKey = this.getClientKey(client.getGadgetUri(), client.getServiceName());
      this.getClientMap().put(clientKey, client);
    }
  }

  public void storeClients(final Collection<OAuth2Client> clients) throws OAuth2CacheException {
    if (clients != null) {
      for (final OAuth2Client client : clients) {
        this.storeClient(client);
      }
    }
  }

  public void storeOAuth2Accessor(final OAuth2Accessor accessor) {
    if (accessor != null) {
      final String accessorKey = this.getAccessorKey(accessor);
      this.getAccessorMap().put(accessorKey, accessor);
    }
  }

  public void storeToken(final OAuth2Token token) throws OAuth2CacheException {
    if (token != null) {
      final String tokenKey = this.getTokenKey(token);
      this.getTokenMap().put(tokenKey, token);
    }
  }

  protected String getClientKey(final OAuth2Client client) {
    return this.getClientKey(client.getGadgetUri(), client.getServiceName());
  }

  protected String getClientKey(final String gadgetUri, final String serviceName) {
    if (gadgetUri == null || serviceName == null) {
      return null;
    }
    final StringBuilder buf = new StringBuilder(gadgetUri.length() + serviceName.length() + 1);
    buf.append(gadgetUri);
    buf.append(':');
    buf.append(serviceName);
    return buf.toString();
  }

  protected String getAccessorKey(final OAuth2CallbackState state) {
    return this.getAccessorKey(state.getGadgetUri(), state.getServiceName(), state.getUser(),
            state.getScope());
  }

  protected String getAccessorKey(final String gadgetUri, final String serviceName,
          final String user, final String scope) {
    if (gadgetUri == null || serviceName == null || user == null) {
      return null;
    }

    final String s = scope == null ? "" : scope;

    final StringBuilder buf = new StringBuilder(gadgetUri.length() + serviceName.length()
            + user.length() + s.length() + 3);
    buf.append(gadgetUri);
    buf.append(':');
    buf.append(serviceName);
    buf.append(':');
    buf.append(user);
    buf.append(':');
    buf.append(s);

    return buf.toString();
  }

  protected String getAccessorKey(final OAuth2Accessor accessor) {
    return this.getAccessorKey(accessor.getGadgetUri(), accessor.getServiceName(),
            accessor.getUser(), accessor.getScope());
  }

  protected String getTokenKey(final String gadgetUri, final String serviceName, final String user,
          final String scope, final Type type) {
    if (gadgetUri == null || serviceName == null || user == null) {
      return null;
    }

    final String s;
    if (scope == null) {
      s = "";
    } else {
      s = scope;
    }

    final String t = type.name();

    final StringBuilder buf = new StringBuilder(gadgetUri.length() + serviceName.length()
            + user.length() + s.length() + t.length() + 4);
    buf.append(gadgetUri);
    buf.append(':');
    buf.append(serviceName);
    buf.append(':');
    buf.append(user);
    buf.append(':');
    buf.append(s);
    buf.append(':');
    buf.append(t);

    return buf.toString();
  }

  protected String getTokenKey(final OAuth2Token token) {
    return this.getTokenKey(token.getGadgetUri(), token.getServiceName(), token.getUser(),
            token.getScope(), token.getType());
  }
}
