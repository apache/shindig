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

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthException;
import net.oauth.OAuthMessage;
import net.oauth.OAuth.Parameter;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map.Entry;

/**
 * Wrapper for the OAuth.net utility functions.  Some of them are declared as throwing IOException
 * for cases that are extremely unlikely to happen.  We turn those IOExceptions in to
 * RuntimeExceptions since the caller can't do anything about them anyway.
 */
public final class OAuthUtil {
  private OAuthUtil() {}

  public static String getParameter(OAuthMessage message, String name) {
    try {
      return message.getParameter(name);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static List<Entry<String, String>> getParameters(OAuthMessage message) {
    try {
      return message.getParameters();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static String formEncode(Iterable<? extends Entry<String, String>> parameters) {
    try {
      return OAuth.formEncode(parameters);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static String addParameters(String url, List<Entry<String, String>> parameters) {
    try {
      return OAuth.addParameters(url, parameters);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static OAuthMessage newRequestMessage(OAuthAccessor accessor, String method, String url,
      List<Parameter> parameters) throws OAuthException {
    try {
      return accessor.newRequestMessage(method, url, parameters);
    } catch (IOException e) {
      throw new RuntimeException(e);
    } catch (URISyntaxException e) {
      throw new OAuthException(e);
    }
  }

  public static enum SignatureType {
    URL_ONLY,
    URL_AND_FORM_PARAMS,
    URL_AND_BODY_HASH,
  }

  /**
   * @param tokenEndpoint true if this is a request token or access token request.  We don't check
   * oauth_body_hash on those.
   */
  public static SignatureType getSignatureType(boolean tokenEndpoint, String contentType) {
    if (OAuth.isFormEncoded(contentType)) {
      return SignatureType.URL_AND_FORM_PARAMS;
    }
    if (tokenEndpoint) {
      return SignatureType.URL_ONLY;
    }
    return SignatureType.URL_AND_BODY_HASH;
  }
}
