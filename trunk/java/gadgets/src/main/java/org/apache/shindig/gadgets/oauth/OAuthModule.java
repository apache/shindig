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

import org.apache.shindig.common.crypto.BasicBlobCrypter;
import org.apache.shindig.common.crypto.BlobCrypter;
import org.apache.shindig.common.crypto.Crypto;
import org.apache.shindig.common.logging.i18n.MessageKeys;
import org.apache.shindig.common.servlet.Authority;
import org.apache.shindig.common.util.ResourceLoader;
import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.gadgets.oauth.BasicOAuthStoreConsumerKeyAndSecret.KeyType;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Loads pre-reqs for OAuth.
 */
public class OAuthModule extends AbstractModule {

  //class name for logging purpose
  private static final String classname = OAuthModule.class.getName();
  private static final Logger LOG = Logger.getLogger(classname, MessageKeys.MESSAGES);


  private static final String OAUTH_CONFIG = "config/oauth.json";
  private static final String OAUTH_SIGNING_KEY_FILE = "shindig.signing.key-file";
  private static final String OAUTH_SIGNING_KEY_NAME = "shindig.signing.key-name";
  private static final String OAUTH_CALLBACK_URL = "shindig.signing.global-callback-url";


  @Override
  protected void configure() {
    // Used for encrypting client-side OAuth state.
    bind(BlobCrypter.class).annotatedWith(Names.named(OAuthFetcherConfig.OAUTH_STATE_CRYPTER))
        .toProvider(OAuthCrypterProvider.class);

    // Used for persistent storage of OAuth access tokens.
    bind(OAuthStore.class).toProvider(OAuthStoreProvider.class);
    bind(OAuthRequest.class).toProvider(OAuthRequestProvider.class);
  }

  @Singleton
  public static class OAuthCrypterProvider implements Provider<BlobCrypter> {

    private final BlobCrypter crypter;

    @Inject
    public OAuthCrypterProvider(@Named("shindig.signing.state-key") String stateCrypterPath)
        throws IOException {
      if (StringUtils.isBlank(stateCrypterPath)) {
        LOG.info("Using random key for OAuth client-side state encryption");
        if (LOG.isLoggable(Level.INFO)) {
          LOG.logp(Level.INFO, classname, "OAuthCrypterProvider constructor", MessageKeys.USING_RANDOM_KEY);
        }
        crypter = new BasicBlobCrypter(Crypto.getRandomBytes(BasicBlobCrypter.MASTER_KEY_MIN_LEN));
      } else {
        if (LOG.isLoggable(Level.INFO)) {
          LOG.logp(Level.INFO, classname, "OAuthCrypterProvider constructor", MessageKeys.USING_FILE, new Object[] {stateCrypterPath});
        }
        crypter = new BasicBlobCrypter(new File(stateCrypterPath));
      }
    }

    public BlobCrypter get() {
      return crypter;
    }
  }

  public static class OAuthRequestProvider implements Provider<OAuthRequest> {
    private final HttpFetcher fetcher;
    private final OAuthFetcherConfig config;

    @Inject
    public OAuthRequestProvider(HttpFetcher fetcher, OAuthFetcherConfig config) {
      this.fetcher = fetcher;
      this.config = config;
    }

    public OAuthRequest get() {
      return new OAuthRequest(config, fetcher);
    }
  }

  @Singleton
  public static class OAuthStoreProvider implements Provider<OAuthStore> {

    private final BasicOAuthStore store;

    @Inject
    public OAuthStoreProvider(
        @Named(OAUTH_SIGNING_KEY_FILE) String signingKeyFile,
        @Named(OAUTH_SIGNING_KEY_NAME) String signingKeyName,
        @Named(OAUTH_CALLBACK_URL) String defaultCallbackUrl,
        Authority authority) {
      store = new BasicOAuthStore();
      loadDefaultKey(signingKeyFile, signingKeyName);
      store.setDefaultCallbackUrl(defaultCallbackUrl);
      store.setAuthority(authority);
      loadConsumers();
    }

    private void loadDefaultKey(String signingKeyFile, String signingKeyName) {
      BasicOAuthStoreConsumerKeyAndSecret key = null;
      if (!StringUtils.isBlank(signingKeyFile)) {
        try {
          if (LOG.isLoggable(Level.INFO)) {
            LOG.logp(Level.INFO, classname, "loadDefaultKey", MessageKeys.LOAD_KEY_FILE_FROM, new Object[] {signingKeyFile});
          }
          String privateKey = IOUtils.toString(ResourceLoader.open(signingKeyFile), "UTF-8");
          privateKey = BasicOAuthStore.convertFromOpenSsl(privateKey);
          key = new BasicOAuthStoreConsumerKeyAndSecret(null, privateKey, KeyType.RSA_PRIVATE,
              signingKeyName, null);
        } catch (Throwable t) {
           if (LOG.isLoggable(Level.WARNING)) {
            LOG.logp(Level.WARNING, classname, "loadDefaultKey", MessageKeys.COULD_NOT_LOAD_KEY_FILE, new Object[] {signingKeyFile});
            LOG.logp(Level.WARNING, classname, "loadDefaultKey", "",t);
          }
        }
      }
      if (key != null) {
        store.setDefaultKey(key);
      } else {
        if (LOG.isLoggable(Level.WARNING)) {
          LOG.logp(Level.WARNING, classname, "loadDefaultKey", MessageKeys.COULD_NOT_LOAD_SIGN_KEY, new Object[] {OAUTH_SIGNING_KEY_FILE,OAUTH_SIGNING_KEY_NAME});
        }
      }
    }

    private void loadConsumers() {
      try {
        String oauthConfigString = ResourceLoader.getContent(OAUTH_CONFIG);
        store.initFromConfigString(oauthConfigString);
      } catch (Throwable t) {
        if (LOG.isLoggable(Level.WARNING)) {
          LOG.logp(Level.WARNING, classname, "loadConsumers", MessageKeys.FAILED_TO_INIT, new Object[] {OAUTH_CONFIG});
          LOG.log(Level.WARNING, "", t);
        }
      }
    }

    public OAuthStore get() {
      return store;
    }
  }
}
