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
package org.apache.shindig.gadgets;

import org.apache.shindig.common.crypto.BasicBlobCrypter;
import org.apache.shindig.common.crypto.BlobCrypter;
import org.apache.shindig.common.crypto.Crypto;
import org.apache.shindig.common.util.ResourceLoader;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.oauth.BasicOAuthStore;
import org.apache.shindig.gadgets.oauth.BasicOAuthStoreConsumerKeyAndSecret;
import org.apache.shindig.gadgets.oauth.OAuthStore;
import org.apache.shindig.gadgets.oauth.BasicOAuthStoreConsumerKeyAndSecret.KeyType;
import org.apache.shindig.gadgets.preload.HttpPreloader;
import org.apache.shindig.gadgets.preload.Preloader;
import org.apache.shindig.gadgets.render.RenderingContentRewriter;
import org.apache.shindig.gadgets.rewrite.ContentRewriter;
import org.apache.shindig.gadgets.rewrite.lexer.DefaultContentRewriter;

import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.CreationException;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.google.inject.spi.Message;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Creates a module to supply all of the Basic* classes
 */
public class DefaultGuiceModule extends AbstractModule {

  private static final Logger logger
      = Logger.getLogger(DefaultGuiceModule.class.getName());

  private final Properties properties;
  private final String oauthConfig;

  private final static String DEFAULT_PROPERTIES = "gadgets.properties";
  private final static String OAUTH_CONFIG = "config/oauth.json";

  public final static String OAUTH_STATE_CRYPTER_ANNOTATION = "shindig.oauth.state-key";
  public final static String OAUTH_SIGNING_KEY_NAME = "shindig.signing.key-name";
  public final static String OAUTH_SIGNING_KEY_FILE = "shindig.signing.key-file";

  /** {@inheritDoc} */
  @Override
  protected void configure() {
    Names.bindProperties(this.binder(), properties);

    ExecutorService service = Executors.newCachedThreadPool();
    bind(Executor.class).toInstance(service);
    bind(ExecutorService.class).toInstance(service);

    bind(new TypeLiteral<List<ContentRewriter>>(){}).toProvider(ContentRewritersProvider.class);
    bind(new TypeLiteral<List<Preloader>>(){}).toProvider(PreloaderProvider.class);

    // TODO: This is not the proper way to use a Guice module. It needs to be fixed before we can
    // do a release.
    try {
      configureOAuthStore();
    } catch (Throwable t) {
      // Since this happens at startup, we don't want to kill the server just
      // because we can't initialize the OAuth config.
      logger.log(Level.WARNING, "Failed to initialize OAuth", t);
    }
    try {
      configureOAuthStateCrypter();
    } catch (Throwable t) {
      // Since this happens at startup, we don't want to kill the server just
      // because we can't initialize the OAuth config.
      logger.log(Level.WARNING, "Failed to initialize Crypter", t);
    }

    // We perform static injection on HttpResponse for cache TTLs.
    requestStaticInjection(HttpResponse.class);
  }

  /**
   * Create a store for OAuth consumer keys and access tokens.  By default consumer keys are read
   * from config/oauth.json, and access tokens are stored in memory.
   *
   * We read the default key from disk, in a location specified in our properties file.
   *
   * TODO: This doesn't belong here! It can't be reused by anyone who wants to customize shindig,
   * which *FORCES* everyone to re-implement it.
   */
  private void configureOAuthStore() throws GadgetException {
    BasicOAuthStore store = new BasicOAuthStore();
    bind(OAuthStore.class).toInstance(store);
    store.initFromConfigString(oauthConfig);

    String keyName = properties.getProperty(OAUTH_SIGNING_KEY_NAME);
    String keyFile = properties.getProperty(OAUTH_SIGNING_KEY_FILE);
    BasicOAuthStoreConsumerKeyAndSecret defaultKey = null;
    if (keyFile != null) {
      try {
        logger.info("Loading OAuth signing key from " + keyFile);
        String privateKey = IOUtils.toString(ResourceLoader.open(keyFile), "UTF-8");
        privateKey = BasicOAuthStore.convertFromOpenSsl(privateKey);
        defaultKey = new BasicOAuthStoreConsumerKeyAndSecret(null, privateKey, KeyType.RSA_PRIVATE,
            keyName);
      } catch (IOException e) {
        logger.log(Level.WARNING, "Couldn't load key file " + keyFile, e);
      }
    }
    if (defaultKey != null) {
      store.setDefaultKey(defaultKey);
    } else {
      logger.log(Level.WARNING, "Couldn't load OAuth signing key.  To create a key, run:\n" +
      		"  openssl req -newkey rsa:1024 -days 365 -nodes -x509 -keyout testkey.pem \\\n" +
      		"     -out testkey.pem -subj '/CN=mytestkey'\n" +
      		"  openssl pkcs8 -in testkey.pem -out oauthkey.pem -topk8 -nocrypt -outform PEM\n" +
      		"\n" +
      		"Then edit gadgets.properties and add these lines:\n" +
      		OAUTH_SIGNING_KEY_FILE + "=<path-to-oauthkey.pem>\n" +
      		OAUTH_SIGNING_KEY_NAME + "=mykey\n");
    }
  }

  /**
   * Create a crypter to handle OAuth state.  This can be loaded from disk, if
   * shindig.oauth.state-key-file is specified in your gadgets.properties file, or it can be
   * created using a random key.
   */
  private void configureOAuthStateCrypter() {
    // Create the oauth state crypter based on a file from disk
    BasicBlobCrypter oauthCrypter = null;
    String keyFileName = properties.getProperty("shindig.oauth.state-key-file");
    if (keyFileName != null) {
      logger.info("Loading OAuth state crypter from " + keyFileName);
      try {
        oauthCrypter = new BasicBlobCrypter(new File(keyFileName));
      } catch (IOException e) {
        logger.log(Level.SEVERE, "Failed to load " + keyFileName, e);
      }
    }
    if (oauthCrypter == null) {
      logger.info("Creating OAuth state crypter with random key");
      oauthCrypter = new BasicBlobCrypter(
          Crypto.getRandomBytes(BasicBlobCrypter.MASTER_KEY_MIN_LEN));
    }
    bind(BlobCrypter.class).annotatedWith(
        Names.named(OAUTH_STATE_CRYPTER_ANNOTATION))
        .toInstance(oauthCrypter);
  }

  public DefaultGuiceModule(Properties properties, String oauthConfig) {
    this.properties = properties;
    this.oauthConfig = oauthConfig;
  }

  /**
   * Creates module with standard properties.
   */
  public DefaultGuiceModule() {
    Properties properties = null;
    String oauthConfig = null;
    try {
      InputStream is = ResourceLoader.openResource(DEFAULT_PROPERTIES);
      properties = new Properties();
      properties.load(is);
      try {
        oauthConfig = ResourceLoader.getContent(OAUTH_CONFIG);
      } catch (IOException e) {
        logger.log(Level.WARNING, "Can't load " + OAUTH_CONFIG, e);
      }
    } catch (IOException e) {
      throw new CreationException(Arrays.asList(
          new Message("Unable to load properties: " + DEFAULT_PROPERTIES)));
    }
    this.properties = properties;
    this.oauthConfig = oauthConfig;
  }

  private static class ContentRewritersProvider implements Provider<List<ContentRewriter>> {
    private final List<ContentRewriter> rewriters;

    @Inject
    public ContentRewritersProvider(DefaultContentRewriter optimizingRewriter,
                                    RenderingContentRewriter renderingRewriter) {
      rewriters = Lists.<ContentRewriter>newArrayList(optimizingRewriter, renderingRewriter);
    }

    public List<ContentRewriter> get() {
      return rewriters;
    }
  }

  private static class PreloaderProvider implements Provider<List<Preloader>> {
    private final List<Preloader> preloaders;

    @Inject
    public PreloaderProvider(HttpPreloader httpPreloader) {
      preloaders = Lists.<Preloader>newArrayList(httpPreloader);
    }

    public List<Preloader> get() {
      return preloaders;
    }
  }
}
