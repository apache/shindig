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
package org.apache.shindig.common;

import org.apache.shindig.common.crypto.BlobCrypterException;

import com.google.inject.Singleton;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Map;

/**
 * A SecurityTokenDecoder implementation that just provides dummy data to satisfy
 * tests and API calls. Do not use this for any security applications.
 */
@Singleton
public class BasicSecurityTokenDecoder implements SecurityTokenDecoder {

  private static final int OWNER_INDEX = 0;
  private static final int VIEWER_INDEX = 1;
  private static final int APP_ID_INDEX = 2;
  private static final int CONTAINER_INDEX = 3;
  private static final int APP_URL_INDEX = 4;
  private static final int MODULE_ID_INDEX = 5;
  private static final int TOKEN_COUNT = MODULE_ID_INDEX + 1;

  /**
   * Encodes a token using the a plaintext dummy format.
   */
  public String encodeToken(SecurityToken token) {
    try {
    StringBuilder out = new StringBuilder();
      out.append(URLEncoder.encode(token.getOwnerId(), "UTF-8")).append(':')
          .append(URLEncoder.encode(token.getViewerId(), "UTF-8")).append(':')
          .append(URLEncoder.encode(token.getAppId(), "UTF-8")).append(':')
          .append(URLEncoder.encode(token.getDomain(), "UTF-8")).append(':')
          .append(URLEncoder.encode(token.getAppUrl(), "UTF-8")).append(':')
          .append(Long.toString(token.getModuleId()));
      return out.toString();
    } catch (UnsupportedEncodingException uee) {
      throw new IllegalStateException(uee);
    }
  }


  /**
   * {@inheritDoc}
   *
   * Returns a token with some faked out values.
   */
  public SecurityToken createToken(Map<String, String> parameters)
      throws SecurityTokenException {

    final String token = parameters.get(SecurityTokenDecoder.SECURITY_TOKEN_NAME);
    if (token == null || token.trim().length() == 0) {
      // No token is present, assume anonymous access
      return AnonymousSecurityToken.getInstance();
    }

    try {
      String[] tokens = token.split(":");
      if (tokens.length != TOKEN_COUNT) {
        throw new SecurityTokenException("Malformed security token");
      }
      
      return new BasicSecurityToken(
          URLDecoder.decode(tokens[OWNER_INDEX], "UTF-8"),
          URLDecoder.decode(tokens[VIEWER_INDEX], "UTF-8"),
          URLDecoder.decode(tokens[APP_ID_INDEX], "UTF-8"),
          URLDecoder.decode(tokens[CONTAINER_INDEX], "UTF-8"),
          URLDecoder.decode(tokens[APP_URL_INDEX], "UTF-8"),
          URLDecoder.decode(tokens[MODULE_ID_INDEX], "UTF-8"));
    } catch (BlobCrypterException e) {
      throw new SecurityTokenException(e);
    } catch (UnsupportedEncodingException e) {
      throw new SecurityTokenException(e);
    }
  }

  /**
   * Creates a signer with 24 hour token expiry
   */
  public BasicSecurityTokenDecoder() {
  }
}
