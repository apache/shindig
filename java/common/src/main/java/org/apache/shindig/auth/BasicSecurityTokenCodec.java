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

import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.shindig.common.crypto.BlobCrypterException;
import org.apache.shindig.common.util.Utf8UrlCoder;
import org.apache.shindig.config.ContainerConfig;

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.google.inject.Singleton;

/**
 * A SecurityTokenCodec implementation that just provides dummy data to satisfy
 * tests and API calls. Do not use this for any security applications.
 *
 * @since 2.0.0
 */
@Singleton
public class BasicSecurityTokenCodec implements SecurityTokenCodec, ContainerConfig.ConfigObserver {

  // Logging
  private static final String CLASSNAME = BasicSecurityTokenCodec.class.getName();
  private static final Logger LOG = Logger.getLogger(CLASSNAME);

  private static final int OWNER_INDEX = 0;
  private static final int VIEWER_INDEX = 1;
  private static final int APP_ID_INDEX = 2;
  private static final int DOMAIN_INDEX = 3;
  private static final int APP_URL_INDEX = 4;
  private static final int MODULE_ID_INDEX = 5;
  private static final int CONTAINER_ID_INDEX = 6;
  private static final int EXPIRY_INDEX = 7; // for back compat, conditionally check later
  private static final int TOKEN_COUNT = CONTAINER_ID_INDEX + 1;
  private Map<String, Integer> tokenTTLs = Maps.newHashMap();

  /**
   * Encodes a token using the a plaintext dummy format.
   * @param token token to encode
   * @return token with values separated by colons
   */
  public String encodeToken(SecurityToken token) {
    Long expires = null;
    Integer tokenTTL = this.tokenTTLs.get(token.getContainer());
    if (token instanceof AbstractSecurityToken) {
      if (tokenTTL != null) {
        ((AbstractSecurityToken) token).setExpires(tokenTTL);
      } else {
        ((AbstractSecurityToken) token).setExpires();
      }
      expires = token.getExpiresAt();
    } else {
      // Quick and dirty token expire calculation.
      AbstractSecurityToken localToken = new BasicSecurityToken();
      if (tokenTTL != null) {
        localToken.setExpires(tokenTTL);
      } else {
        localToken.setExpires();
      }
      expires = localToken.getExpiresAt();
    }

    String encoded = Joiner.on(":").join(
        Utf8UrlCoder.encode(token.getOwnerId()),
        Utf8UrlCoder.encode(token.getViewerId()),
        Utf8UrlCoder.encode(token.getAppId()),
        Utf8UrlCoder.encode(token.getDomain()),
        Utf8UrlCoder.encode(token.getAppUrl()),
        Long.toString(token.getModuleId(), 10),
        Utf8UrlCoder.encode(token.getContainer()));

    if (expires != null) {
      encoded = Joiner.on(':').join(encoded, Long.toString(expires, 10));
    }

    return encoded;
  }


  /**
   * {@inheritDoc}
   *
   * Returns a token with some faked out values.
   */
  public SecurityToken createToken(Map<String, String> parameters)
      throws SecurityTokenException {

    final String token = parameters.get(SecurityTokenCodec.SECURITY_TOKEN_NAME);
    if (token == null || token.trim().length() == 0) {
      // No token is present, assume anonymous access
      return new AnonymousSecurityToken();
    }

    try {
      String[] tokens = StringUtils.split(token, ':');
      if (tokens.length < TOKEN_COUNT) {
        throw new SecurityTokenException("Malformed security token");
      }

      Long expires = null;
      if (tokens.length > TOKEN_COUNT && !tokens[EXPIRY_INDEX].equals("")) {
        expires = Long.parseLong(Utf8UrlCoder.decode(tokens[EXPIRY_INDEX]), 10);
      }

      BasicSecurityToken basicToken = new BasicSecurityToken(
          Utf8UrlCoder.decode(tokens[OWNER_INDEX]),
          Utf8UrlCoder.decode(tokens[VIEWER_INDEX]),
          Utf8UrlCoder.decode(tokens[APP_ID_INDEX]),
          Utf8UrlCoder.decode(tokens[DOMAIN_INDEX]),
          Utf8UrlCoder.decode(tokens[APP_URL_INDEX]),
          Utf8UrlCoder.decode(tokens[MODULE_ID_INDEX]),
          Utf8UrlCoder.decode(tokens[CONTAINER_ID_INDEX]),
          parameters.get(SecurityTokenCodec.ACTIVE_URL_NAME),
          expires);
      return basicToken.enforceNotExpired();
    } catch (BlobCrypterException e) {
      throw new SecurityTokenException(e);
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new SecurityTokenException(e);
    }
  }

  public int getTokenTimeToLive() {
    return AbstractSecurityToken.DEFAULT_MAX_TOKEN_TTL;
  }

  public int getTokenTimeToLive(String container) {
    Integer tokenTTL = this.tokenTTLs.get(container);
    if (tokenTTL == null) {
      return getTokenTimeToLive();
    }
    return tokenTTL;
  }

  /**
   * Creates a basic signer
   */
  public BasicSecurityTokenCodec() {}

  /**
   * Creates a basic signer that can observe container configuration changes
   * @param config the container config to observe
   */
  public BasicSecurityTokenCodec(ContainerConfig config) {
    config.addConfigObserver(this, true);
  }

  /**
   * {@inheritDoc}
   */
  public void containersChanged(ContainerConfig config, Collection<String> changed,
          Collection<String> removed) {
    for (String container : removed) {
      this.tokenTTLs.remove(container);
    }

    for (String container : changed) {
      int tokenTTL = config.getInt(container, SECURITY_TOKEN_TTL_CONFIG);
      // 0 means the value was not defined or NaN.  0 shouldn't be a valid TTL anyway.
      if (tokenTTL > 0) {
        this.tokenTTLs.put(container, tokenTTL);
      } else {
        LOG.logp(Level.WARNING, CLASSNAME, "containersChanged",
                "Token TTL for container \"{0}\" was {1} and will be ignored.",
                new Object[] { container, tokenTTL });
      }
    }
  }
}
