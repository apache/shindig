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
package org.apache.shindig.auth;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.shindig.common.crypto.BasicBlobCrypter;
import org.apache.shindig.common.crypto.BlobCrypter;
import org.apache.shindig.common.crypto.BlobCrypterException;
import org.apache.shindig.common.util.ResourceLoader;
import org.apache.shindig.config.ContainerConfig;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Provides security token decoding services.  Configuration is via containers.js.  Each container
 * should specify (or inherit)
 *
 * securityTokenKeyFile: path to file containing a key to use for verifying tokens.
 * signedFetchDomain: oauth_consumer_key value to use for signed fetch using default key.
 *
 * Creating a key is best done with a command line like this:
 * <pre>
 *     dd if=/dev/random bs=32 count=1  | openssl base64 > /tmp/key.txt
 * </pre>
 * Wire format is "&lt;container&gt;:&lt;encrypted-and-signed-token&gt;"
 *
 * @since 2.0.0
 */
@Singleton
public class BlobCrypterSecurityTokenCodec implements SecurityTokenCodec, ContainerConfig.ConfigObserver {
  private static final Logger LOG = Logger.getLogger(BlobCrypterSecurityTokenCodec.class.getName());

  public static final String SECURITY_TOKEN_KEY_FILE = "gadgets.securityTokenKeyFile";

  public static final String SECURITY_TOKEN_KEY = "gadgets.securityTokenKey";

  public static final String SIGNED_FETCH_DOMAIN = "gadgets.signedFetchDomain";

  /**
   * Keys are container ids, values are crypters
   */
  protected Map<String, BlobCrypter> crypters = Maps.newHashMap();

  /**
   * Keys are container ids, values are domains used for signed fetch.
   */
  protected Map<String, String> domains = Maps.newHashMap();

  @Inject
  public BlobCrypterSecurityTokenCodec(ContainerConfig config) {
    try {
      config.addConfigObserver(this, false);
      loadContainers(config, config.getContainers(), crypters, domains);
    } catch (IOException e) {
      // Someone specified securityTokenKeyFile, but we couldn't load the key.  That merits killing
      // the server.
      LOG.log(Level.SEVERE, "Error while initializing BlobCrypterSecurityTokenCodec", e);
      throw new RuntimeException(e);
    }
  }

  public void containersChanged(
      ContainerConfig config, Collection<String> changed, Collection<String> removed) {
    Map<String, BlobCrypter> newCrypters = Maps.newHashMap(crypters);
    Map<String, String> newDomains = Maps.newHashMap(domains);
    try {
      loadContainers(config, changed, newCrypters, newDomains);
      for (String container : removed) {
        newCrypters.remove(container);
        newDomains.remove(container);
      }
    } catch (IOException e) {
      // Someone specified securityTokenKeyFile, but we couldn't load the key.
      // Keep the old configuration.
      LOG.log(Level.WARNING, "There was an error loading an updated container configuration. "
          + "Keeping old configuration.", e);
      return;
    }
    crypters = newCrypters;
    domains = newDomains;
  }

  private void loadContainers(ContainerConfig config, Collection<String> containers,
      Map<String, BlobCrypter> crypters, Map<String, String> domains) throws IOException {
    for (String container : containers) {
      String keyFile = config.getString(container, SECURITY_TOKEN_KEY_FILE);
      String key = config.getString(container, SECURITY_TOKEN_KEY);
      if (key != null) {
        BlobCrypter crypter = loadCrypter(key);
        crypters.put(container, crypter);
      } else if (keyFile != null) {
        BlobCrypter crypter = loadCrypter(getKeyFromFile(keyFile));
        crypters.put(container, crypter);
      }
      String domain = config.getString(container, SIGNED_FETCH_DOMAIN);
      domains.put(container, domain);
    }
  }

  /**
   * Gets a key from the given keyFile.
   *
   * @param keyFile the key file from which to read the key
   * @return the key
   * @throws IOException if there was an error read the key file
   */
  @VisibleForTesting
  protected String getKeyFromFile(String keyFile) throws IOException {
    return IOUtils.toString(ResourceLoader.open(keyFile), "UTF-8");
  }

  /**
   * Load a BlobCrypter from the key file.  Override this if you have your own
   * BlobCrypter implementation.
   *
   * @param key The key to use for encryption
   * @return The BlobCrypter.
   * @throws IOException If the key file is invalid.
   */
  protected BlobCrypter loadCrypter(String key) throws IOException {
    return new BasicBlobCrypter(key);
  }

  /**
   * Decrypt and verify the provided security token.
   */
  public SecurityToken createToken(Map<String, String> tokenParameters)
      throws SecurityTokenException {
    String token = tokenParameters.get(SecurityTokenCodec.SECURITY_TOKEN_NAME);
    if (StringUtils.isBlank(token)) {
      // No token is present, assume anonymous access
      return new AnonymousSecurityToken();
    }
    String[] fields = StringUtils.split(token, ':');
    if (fields.length != 2) {
      throw new SecurityTokenException("Invalid security token " + token);
    }
    String container = fields[0];
    BlobCrypter crypter = crypters.get(container);
    if (crypter == null) {
      throw new SecurityTokenException("Unknown container " + token);
    }
    String domain = domains.get(container);
    String activeUrl = tokenParameters.get(SecurityTokenCodec.ACTIVE_URL_NAME);
    String crypted = fields[1];
    try {
      BlobCrypterSecurityToken st = new BlobCrypterSecurityToken(container, domain, activeUrl,
          crypter.unwrap(crypted));
      return st.enforceNotExpired();
    } catch (BlobCrypterException e) {
      throw new SecurityTokenException(e);
    }
  }

  /**
   * Encrypt and sign the token.  The returned value is *not* web safe, it should be URL
   * encoded before being used as a form parameter.
   */
  public String encodeToken(SecurityToken token) throws SecurityTokenException {
    if (!token.getAuthenticationMode().equals(
            AuthenticationMode.SECURITY_TOKEN_URL_PARAMETER.name())) {
      throw new SecurityTokenException("Can only encode BlogCrypterSecurityTokens");
    }

    // Test code sends in real AbstractTokens, they have modified time sources in them so
    // that we can test token expiration, production tokens are proxied via the SecurityToken interface.
    AbstractSecurityToken aToken = token instanceof AbstractSecurityToken ?
        (AbstractSecurityToken)token : BlobCrypterSecurityToken.fromToken(token);

    BlobCrypter crypter = crypters.get(aToken.getContainer());
    if (crypter == null) {
      throw new SecurityTokenException("Unknown container " + aToken.getContainer());
    }

    try {
      aToken.setExpires();
      return aToken.getContainer() + ':' + crypter.wrap(aToken.toMap());
    } catch (BlobCrypterException e) {
      throw new SecurityTokenException(e);
    }
  }

  public int getTokenTimeToLive() {
    return AbstractSecurityToken.MAX_TOKEN_TTL;
  }
}
