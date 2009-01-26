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

import org.apache.shindig.config.ContainerConfig;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.Map;

/**
 * Default implementation of security tokens.  Decides based on default container configuration
 * whether to use real crypto for security tokens or to use a simple insecure implementation that
 * is useful for testing.
 *
 * Example configuration in container.js for insecure security tokens:
 *    gadgets.securityTokenType = insecure
 *
 * Example configuration in container.js for blob crypter based security tokens:
 *    gadgets.securityTokenType = secure
 *
 * The insecure implementation is BasicSecurityTokenDecoder.
 *
 * The secure implementation is BlobCrypterSecurityTokenDecoder.
 */
@Singleton
public class DefaultSecurityTokenDecoder implements SecurityTokenDecoder {

  private static final String SECURITY_TOKEN_TYPE = "gadgets.securityTokenType";

  private final SecurityTokenDecoder decoder;

  @Inject
  public DefaultSecurityTokenDecoder(ContainerConfig config) {
    String tokenType = config.getString(ContainerConfig.DEFAULT_CONTAINER, SECURITY_TOKEN_TYPE);
    if ("insecure".equals(tokenType)) {
      decoder = new BasicSecurityTokenDecoder();
    } else if ("secure".equals(tokenType)) {
      decoder = new BlobCrypterSecurityTokenDecoder(config);
    } else {
      throw new RuntimeException("Unknown security token type specified in " +
          ContainerConfig.DEFAULT_CONTAINER + " container configuration. " +
          SECURITY_TOKEN_TYPE + ": " + tokenType);
    }
  }

  public SecurityToken createToken(Map<String, String> tokenParameters)
      throws SecurityTokenException {
    return decoder.createToken(tokenParameters);
  }

}
