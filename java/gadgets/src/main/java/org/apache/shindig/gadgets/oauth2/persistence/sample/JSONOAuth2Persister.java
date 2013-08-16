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

import com.google.caja.util.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import org.apache.shindig.common.Nullable;
import org.apache.shindig.common.servlet.Authority;
import org.apache.shindig.common.util.ResourceLoader;
import org.apache.shindig.gadgets.oauth2.OAuth2Accessor;
import org.apache.shindig.gadgets.oauth2.OAuth2Message;
import org.apache.shindig.gadgets.oauth2.OAuth2Token;
import org.apache.shindig.gadgets.oauth2.OAuth2Token.Type;
import org.apache.shindig.gadgets.oauth2.logger.FilteredLogger;
import org.apache.shindig.gadgets.oauth2.persistence.OAuth2Client;
import org.apache.shindig.gadgets.oauth2.persistence.OAuth2Encrypter;
import org.apache.shindig.gadgets.oauth2.persistence.OAuth2EncryptionException;
import org.apache.shindig.gadgets.oauth2.persistence.OAuth2PersistenceException;
import org.apache.shindig.gadgets.oauth2.persistence.OAuth2Persister;
import org.apache.shindig.gadgets.oauth2.persistence.OAuth2TokenPersistence;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Persistence implementation that reads <code>config/oauth2.json</code> on startup
 *
 */
@Singleton
public class JSONOAuth2Persister implements OAuth2Persister {
  private static final String ALLOW_MODULE_OVERRIDE = "allowModuleOverride";
  private static final String AUTHORIZATION_HEADER = "usesAuthorizationHeader";
  private static final String AUTHORIZATION_URL = "authorizationUrl";
  private static final String CLIENT_AUTHENTICATION = "client_authentication";
  private static final String CLIENT_NAME = "clientName";
  private static final String CLIENTS = "clients";
  private static final String ENDPOINTS = "endpoints";
  private static final String GADGET_BINDGINGS = "gadgetBindings";
  private static final String NO_CLIENT_AUTHENTICATION = "NONE";
  private static final String OAUTH2_CONFIG = "config/oauth2.json";
  private static final String PROVIDER_NAME = "providerName";
  private static final String PROVIDERS = "providers";
  private static final String TOKEN_URL = "tokenUrl";
  private static final String TYPE = "type";
  private static final String URL_PARAMETER = "usesUrlParameter";
  private static final String ALLOWED_DOMAINS = "allowedDomains";

  private final JSONObject configFile;
  private final String contextRoot;
  private final OAuth2Encrypter encrypter;
  private final String globalRedirectUri;

  private final Authority authority;

  private static final String LOG_CLASS = JSONOAuth2Persister.class.getName();
  private static final FilteredLogger LOG = FilteredLogger
          .getFilteredLogger(JSONOAuth2Persister.LOG_CLASS);

  @Inject
  public JSONOAuth2Persister(final OAuth2Encrypter encrypter, final Authority authority,
          final String globalRedirectUri, @Nullable
          @Named("shindig.contextroot")
          final String contextRoot) throws OAuth2PersistenceException {
    this.encrypter = encrypter;
    this.authority = authority;
    this.globalRedirectUri = globalRedirectUri;
    this.contextRoot = contextRoot;
    try {
      this.configFile = new JSONObject(
              JSONOAuth2Persister.getJSONString(JSONOAuth2Persister.OAUTH2_CONFIG));
    } catch (final Exception e) {
      if (JSONOAuth2Persister.LOG.isLoggable()) {
        JSONOAuth2Persister.LOG.log("OAuth2PersistenceException", e);
      }
      throw new OAuth2PersistenceException(e);
    }
  }

  public JSONOAuth2Persister(final OAuth2Encrypter encrypter, final Authority authority,
          final String globalRedirectUri, @Nullable
          @Named("shindig.contextroot")
          final String contextRoot, final JSONObject configFile) {
    this.encrypter = encrypter;
    this.authority = authority;
    this.globalRedirectUri = globalRedirectUri;
    this.contextRoot = contextRoot;
    this.configFile = configFile;
  }

  public OAuth2Token createToken() {
    return new OAuth2TokenPersistence(this.encrypter);
  }

  public static OAuth2Client findClient(@SuppressWarnings("unused")
  final Integer index) {
    return null;
  }

  public OAuth2Client findClient(final String providerName, final String gadgetUri)
          throws OAuth2PersistenceException {
    return null;
  }

  public static OAuth2Provider findProvider(@SuppressWarnings("unused")
  final Integer index) {
    return null;
  }

  public static OAuth2Provider findProvider(@SuppressWarnings("unused")
  final String providerName) {
    return null;
  }

  public static OAuth2Token findToken(@SuppressWarnings("unused")
  final Integer index) {
    return null;
  }

  public OAuth2Token findToken(final String providerName, final String gadgetUri,
          final String user, final String scope, final Type type) throws OAuth2PersistenceException {
    return null;
  }

  public void insertToken(final OAuth2Token token) {
    // does nothing
  }

  public Set<OAuth2Client> loadClients() throws OAuth2PersistenceException {
    final Map<String, OAuth2GadgetBinding> gadgetBindings = this.loadGadgetBindings();
    final Map<String, OAuth2Provider> providers = this.loadProviders();

    final Map<String, OAuth2Client> internalMap = Maps.newHashMap();

    try {
      final JSONObject clients = this.configFile.getJSONObject(JSONOAuth2Persister.CLIENTS);
      for (final Iterator<?> j = clients.keys(); j.hasNext();) {
        final String clientName = (String) j.next();
        final JSONObject settings = clients.getJSONObject(clientName);

        final OAuth2Client client = new OAuth2Client(this.encrypter);

        final String providerName = settings.getString(JSONOAuth2Persister.PROVIDER_NAME);
        final OAuth2Provider provider = providers.get(providerName);
        client.setAuthorizationUrl(provider.getAuthorizationUrl());
        client.setClientAuthenticationType(provider.getClientAuthenticationType());
        client.setAuthorizationHeader(provider.isAuthorizationHeader());
        client.setUrlParameter(provider.isUrlParameter());
        client.setTokenUrl(provider.getTokenUrl());

        String redirectUri = settings.optString(OAuth2Message.REDIRECT_URI, null);
        if (redirectUri == null) {
          redirectUri = this.globalRedirectUri;
        }
        final String secret = settings.optString(OAuth2Message.CLIENT_SECRET);
        final String clientId = settings.getString(OAuth2Message.CLIENT_ID);
        final String typeS = settings.optString(JSONOAuth2Persister.TYPE, null);
        String grantType = settings.optString(OAuth2Message.GRANT_TYPE, null);
        final String sharedToken = settings.optString(OAuth2Message.SHARED_TOKEN, "false");
        if ("true".equalsIgnoreCase(sharedToken)) {
          client.setSharedToken(true);
        }

        try {
          client.setEncryptedSecret(secret.getBytes("UTF-8"));
        } catch (final OAuth2EncryptionException e) {
          throw new OAuth2PersistenceException(e);
        }

        client.setClientId(clientId);

        if (this.authority != null) {
          redirectUri = redirectUri.replace("%authority%", this.authority.getAuthority());
          redirectUri = redirectUri.replace("%contextRoot%", this.contextRoot);
          redirectUri = redirectUri.replace("%origin%", this.authority.getOrigin());
          redirectUri = redirectUri.replace("%scheme", this.authority.getScheme());
        }
        client.setRedirectUri(redirectUri);

        if (grantType == null || grantType.length() == 0) {
          grantType = OAuth2Message.AUTHORIZATION;
        }

        client.setGrantType(grantType);

        OAuth2Accessor.Type type = OAuth2Accessor.Type.UNKNOWN;
        if (OAuth2Message.CONFIDENTIAL_CLIENT_TYPE.equals(typeS)) {
          type = OAuth2Accessor.Type.CONFIDENTIAL;
        } else if (OAuth2Message.PUBLIC_CLIENT_TYPE.equals(typeS)) {
          type = OAuth2Accessor.Type.PUBLIC;
        }
        client.setType(type);

        final JSONArray dArray = settings.optJSONArray(JSONOAuth2Persister.ALLOWED_DOMAINS);
        if (dArray != null) {
          final ArrayList<String> domains = new ArrayList<String>();
          for (int i = 0; i < dArray.length(); i++) {
            domains.add(dArray.optString(i));
          }
          client.setAllowedDomains(domains.toArray(new String[0]));
        }

        internalMap.put(clientName, client);
      }
    } catch (final Exception e) {
      if (JSONOAuth2Persister.LOG.isLoggable()) {
        JSONOAuth2Persister.LOG.log("OAuth2PersistenceException", e);
      }
      throw new OAuth2PersistenceException(e);
    }

    final Set<OAuth2Client> ret = new HashSet<OAuth2Client>(gadgetBindings.size());
    for (final OAuth2GadgetBinding binding : gadgetBindings.values()) {
      final String clientName = binding.getClientName();
      final OAuth2Client cachedClient = internalMap.get(clientName);
      final OAuth2Client client = cachedClient.clone();
      client.setGadgetUri(binding.getGadgetUri());
      client.setServiceName(binding.getGadgetServiceName());
      client.setAllowModuleOverride(binding.isAllowOverride());
      ret.add(client);
    }

    return ret;
  }

  protected Map<String, OAuth2GadgetBinding> loadGadgetBindings() throws OAuth2PersistenceException {
    final Map<String, OAuth2GadgetBinding> ret = Maps.newHashMap();

    try {
      final JSONObject bindings = this.configFile
              .getJSONObject(JSONOAuth2Persister.GADGET_BINDGINGS);
      for (final Iterator<?> i = bindings.keys(); i.hasNext();) {
        final String gadgetUriS = (String) i.next();
        String gadgetUri = null;
        if (this.authority != null) {
          gadgetUri = gadgetUriS.replace("%authority%", this.authority.getAuthority());
          gadgetUri = gadgetUri.replace("%contextRoot%", this.contextRoot);
          gadgetUri = gadgetUri.replace("%origin%", this.authority.getOrigin());
          gadgetUri = gadgetUri.replace("%scheme%", this.authority.getScheme());
        }

        final JSONObject binding = bindings.getJSONObject(gadgetUriS);
        for (final Iterator<?> j = binding.keys(); j.hasNext();) {
          final String gadgetServiceName = (String) j.next();
          final JSONObject settings = binding.getJSONObject(gadgetServiceName);
          final String clientName = settings.getString(JSONOAuth2Persister.CLIENT_NAME);
          final boolean allowOverride = settings
                  .getBoolean(JSONOAuth2Persister.ALLOW_MODULE_OVERRIDE);
          final OAuth2GadgetBinding gadgetBinding = new OAuth2GadgetBinding(gadgetUri,
                  gadgetServiceName, clientName, allowOverride);

          ret.put(gadgetBinding.getGadgetUri() + ':' + gadgetBinding.getGadgetServiceName(),
                  gadgetBinding);
        }
      }

    } catch (final JSONException e) {
      if (JSONOAuth2Persister.LOG.isLoggable()) {
        JSONOAuth2Persister.LOG.log("OAuth2PersistenceException", e);
      }
      throw new OAuth2PersistenceException(e);
    }

    return ret;
  }

  protected Map<String, OAuth2Provider> loadProviders() throws OAuth2PersistenceException {
    final Map<String, OAuth2Provider> ret = Maps.newHashMap();

    try {
      final JSONObject providers = this.configFile.getJSONObject(JSONOAuth2Persister.PROVIDERS);
      for (final Iterator<?> i = providers.keys(); i.hasNext();) {
        final String providerName = (String) i.next();
        final JSONObject provider = providers.getJSONObject(providerName);
        final JSONObject endpoints = provider.getJSONObject(JSONOAuth2Persister.ENDPOINTS);

        final String clientAuthenticationType = provider.optString(
                JSONOAuth2Persister.CLIENT_AUTHENTICATION,
                JSONOAuth2Persister.NO_CLIENT_AUTHENTICATION);

        final boolean authorizationHeader = provider.optBoolean(
                JSONOAuth2Persister.AUTHORIZATION_HEADER, false);

        final boolean urlParameter = provider.optBoolean(JSONOAuth2Persister.URL_PARAMETER, false);

        String authorizationUrl = endpoints.optString(JSONOAuth2Persister.AUTHORIZATION_URL, null);

        if (this.authority != null && authorizationUrl != null) {
          authorizationUrl = authorizationUrl.replace("%authority%", this.authority.getAuthority());
          authorizationUrl = authorizationUrl.replace("%contextRoot%", this.contextRoot);
          authorizationUrl = authorizationUrl.replace("%origin%", this.authority.getOrigin());
          authorizationUrl = authorizationUrl.replace("%scheme%", this.authority.getScheme());
        }

        String tokenUrl = endpoints.optString(JSONOAuth2Persister.TOKEN_URL, null);
        if (this.authority != null && tokenUrl != null) {
          tokenUrl = tokenUrl.replace("%authority%", this.authority.getAuthority());
          tokenUrl = tokenUrl.replace("%contextRoot%", this.contextRoot);
          tokenUrl = tokenUrl.replace("%origin%", this.authority.getOrigin());
          tokenUrl = tokenUrl.replace("%scheme%", this.authority.getScheme());
        }

        final OAuth2Provider oauth2Provider = new OAuth2Provider();

        oauth2Provider.setName(providerName);
        oauth2Provider.setAuthorizationUrl(authorizationUrl);
        oauth2Provider.setTokenUrl(tokenUrl);
        oauth2Provider.setClientAuthenticationType(clientAuthenticationType);
        oauth2Provider.setAuthorizationHeader(authorizationHeader);
        oauth2Provider.setUrlParameter(urlParameter);

        ret.put(oauth2Provider.getName(), oauth2Provider);
      }
    } catch (final JSONException e) {
      if (JSONOAuth2Persister.LOG.isLoggable()) {
        JSONOAuth2Persister.LOG.log("OAuth2PersistenceException", e);
      }
      throw new OAuth2PersistenceException(e);
    }

    return ret;
  }

  public Set<OAuth2Token> loadTokens() throws OAuth2PersistenceException {
    return Collections.emptySet();
  }

  public static boolean removeToken(@SuppressWarnings("unused")
  final Integer index) {
    // does nothing
    return false;
  }

  public boolean removeToken(final String providerName, final String gadgetUri, final String user,
          final String scope, final Type type) {
    return false;
  }

  public void updateToken(final OAuth2Token token) {
    // does nothing
  }

  private static String getJSONString(final String location) throws IOException {
    return ResourceLoader.getContent(location);
  }
}
