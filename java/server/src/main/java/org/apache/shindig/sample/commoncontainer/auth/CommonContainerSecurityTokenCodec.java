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
package org.apache.shindig.sample.commoncontainer.auth;

import java.util.Map;

import org.apache.shindig.auth.BasicSecurityTokenCodec;
import org.apache.shindig.auth.BlobCrypterSecurityTokenCodec;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.auth.SecurityTokenCodec;
import org.apache.shindig.auth.SecurityTokenException;
import org.apache.shindig.common.util.Utf8UrlCoder;
import org.apache.shindig.config.ContainerConfig;

import com.google.common.base.Joiner;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Creates a dummy security token used for testing common container sample.
 */
@Singleton
public class CommonContainerSecurityTokenCodec implements SecurityTokenCodec {

  private static final String SECURITY_TOKEN_TYPE = "gadgets.securityTokenType";

  private final SecurityTokenCodec codec;

  @Inject
  public CommonContainerSecurityTokenCodec(ContainerConfig config) {
    String tokenType = config.getString(ContainerConfig.DEFAULT_CONTAINER,
        SECURITY_TOKEN_TYPE);
    if ("insecure".equals(tokenType)) {
      codec = new BasicSecurityTokenCodec();
    } else if ("secure".equals(tokenType)) {
      codec = new BlobCrypterSecurityTokenCodec(config);
    } else {
      throw new RuntimeException("Unknown security token type specified in "
          + ContainerConfig.DEFAULT_CONTAINER + " container configuration. "
          + SECURITY_TOKEN_TYPE + ": " + tokenType);
    }
  }

  public SecurityToken createToken(Map<String, String> tokenParameters)
      throws SecurityTokenException {
    TestSecurityTokenCodec testSecurityToken = new TestSecurityTokenCodec();
    return testSecurityToken;
  }

  public String encodeToken(SecurityToken token) throws SecurityTokenException {
    if (token != null) {
      return Joiner.on(":").join(Utf8UrlCoder.encode(token.getOwnerId()),
          Utf8UrlCoder.encode(token.getViewerId()),
          Utf8UrlCoder.encode(token.getAppId()),
          Utf8UrlCoder.encode(token.getDomain()),
          Utf8UrlCoder.encode(token.getAppUrl()),
          Long.toString(token.getModuleId()),
          Utf8UrlCoder.encode(token.getContainer()));
    }
    return null;
  }

  public Long getTokenExpiration(SecurityToken token)
      throws SecurityTokenException {
    return codec.getTokenExpiration(token);
  }
}
