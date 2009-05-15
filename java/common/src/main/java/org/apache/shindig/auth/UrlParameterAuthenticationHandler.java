/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.auth;

import com.google.common.collect.Maps;
import com.google.inject.Inject;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

/**
 * Produces security tokens by extracting the "st" parameter from the request url or post body.
 */
public class UrlParameterAuthenticationHandler implements AuthenticationHandler {
  private static final String TOKEN_PARAM = "st";

  private final SecurityTokenDecoder securityTokenDecoder;

  @Inject
  public UrlParameterAuthenticationHandler(SecurityTokenDecoder securityTokenDecoder) {
    this.securityTokenDecoder = securityTokenDecoder;
  }

  public String getName() {
    return AuthenticationMode.SECURITY_TOKEN_URL_PARAMETER.name();
  }

  public SecurityToken getSecurityTokenFromRequest(HttpServletRequest request)
      throws InvalidAuthenticationException {
    Map<String, String> parameters = getMappedParameters(request);
    try {
      if (parameters.get(SecurityTokenDecoder.SECURITY_TOKEN_NAME) == null) {
        return null;
      }
      return securityTokenDecoder.createToken(parameters);
    } catch (SecurityTokenException e) {
      throw new InvalidAuthenticationException("Malformed security token " +
          parameters.get(SecurityTokenDecoder.SECURITY_TOKEN_NAME), e);
    }
  }

  public String getWWWAuthenticateHeader(String realm) {
    return null;
  }

  protected SecurityTokenDecoder getSecurityTokenDecoder() {
    return this.securityTokenDecoder;
  }

  protected Map<String, String> getMappedParameters(
      final HttpServletRequest request) {
    Map<String, String> params = Maps.newHashMap();
    params.put(SecurityTokenDecoder.SECURITY_TOKEN_NAME, request.getParameter(TOKEN_PARAM));
    params.put(SecurityTokenDecoder.ACTIVE_URL_NAME, getActiveUrl(request));
    return params;
  }
  
  protected String getActiveUrl(HttpServletRequest request) {
    return request.getRequestURL().toString();
  }
}
