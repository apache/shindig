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

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.shindig.common.ContainerConfig;
import org.apache.shindig.common.crypto.BasicBlobCrypter;
import org.apache.shindig.common.crypto.BlobCrypter;
import org.apache.shindig.common.crypto.BlobCrypterException;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Provides security token decoding services.  Configuration is via containers.js.  Each container
 * should specify (or inherit)
 * 
 * securityTokenKeyFile: path to file containing a key to use for verifying tokens.
 * signedFetchDomain: oauth_consumer_key value to use for signed fetch using default key.
 * 
 * Creating a key is best done with a command line like this:
 * 
 *     dd if=/dev/random bs=32 count=1  | openssl base64 > /tmp/key.txt
 * 
 * Wire format is "<container>:<encrypted-and-signed-token>"
 */
@Singleton
public class BlobCrypterSecurityTokenDecoder implements SecurityTokenDecoder {

  public static final String SECURITY_TOKEN_KEY_FILE = "gadgets.securityTokenKeyFile";
  
  public static final String SIGNED_FETCH_DOMAIN = "gadgets.signedFetchDomain";
  
  /**
   * Keys are container ids, values are crypters
   */
  private Map<String, BlobCrypter> crypters = Maps.newHashMap();
  
  /**
   * Keys are container ids, values are domains used for signed fetch.
   */
  private Map<String, String> domains = Maps.newHashMap();

  @Inject
  public BlobCrypterSecurityTokenDecoder(ContainerConfig config) {
    try {
      for (String container : config.getContainers()) {
        String keyFile = config.get(container, SECURITY_TOKEN_KEY_FILE);
        if (keyFile != null) {
          BlobCrypter crypter = loadCrypterFromFile(new File(keyFile));
          crypters.put(container, crypter);
        }
        String domain = config.get(container, SIGNED_FETCH_DOMAIN);
        domains.put(container, domain);
      }
    } catch (IOException e) {
      // Someone specified securityTokenKeyFile, but we couldn't load the key.  That merits killing
      // the server.
      throw new RuntimeException(e);
    }
  }
  
  /**
   * Load a BlobCrypter from the specified file.  Override this if you have your own
   * BlobCrypter implementation.
   */
  protected BlobCrypter loadCrypterFromFile(File file) throws IOException {
    return new BasicBlobCrypter(file);
  }
  
  /**
   * Decrypt and verify the provided security token.
   */
  public SecurityToken createToken(Map<String, String> tokenParameters)
      throws SecurityTokenException {
    String token = tokenParameters.get(SecurityTokenDecoder.SECURITY_TOKEN_NAME);
    if (token == null || token.trim().length() == 0) {
      // No token is present, assume anonymous access
      return new AnonymousSecurityToken();
    }
    String[] fields = token.split(":");
    if (fields.length != 2) {
      throw new SecurityTokenException("Invalid security token " + token);
    }
    String container = fields[0];
    BlobCrypter crypter = crypters.get(container);
    if (crypter == null) {
      throw new SecurityTokenException("Unknown container " + token);
    }
    String domain = domains.get(container);
    String crypted = fields[1];
    try {
      return BlobCrypterSecurityToken.decrypt(crypter, container, domain, crypted);
    } catch (BlobCrypterException e) {
      throw new SecurityTokenException(e);
    }
  }
}
