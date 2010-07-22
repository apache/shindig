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
import net.oauth.OAuth;

import java.util.Enumeration;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

/**
 * Produces security tokens by extracting the "st" parameter from the request url or post body.
 */
public class UrlParameterAuthenticationHandler implements AuthenticationHandler {
  private static final String SECURITY_TOKEN_PARAM = "st";

  private final SecurityTokenCodec securityTokenCodec;
  private static final Pattern COMMAWHITESPACE = Pattern.compile("\\s*,\\s*");

  @Inject
  public UrlParameterAuthenticationHandler(SecurityTokenCodec securityTokenCodec) {
    this.securityTokenCodec = securityTokenCodec;
  }

  public String getName() {
    return AuthenticationMode.SECURITY_TOKEN_URL_PARAMETER.name();
  }

  public SecurityToken getSecurityTokenFromRequest(HttpServletRequest request)
      throws InvalidAuthenticationException {
    Map<String, String> parameters = getMappedParameters(request);
    try {
      if (parameters.get(SecurityTokenCodec.SECURITY_TOKEN_NAME) == null) {
        return null;
      }
      return securityTokenCodec.createToken(parameters);
    } catch (SecurityTokenException e) {
      throw new InvalidAuthenticationException("Malformed security token " +
          parameters.get(SecurityTokenCodec.SECURITY_TOKEN_NAME), e);
    }
  }

  public String getWWWAuthenticateHeader(String realm) {
    return null;
  }

  protected SecurityTokenCodec getSecurityTokenCodec() {
    return this.securityTokenCodec;
  }

  // From OAuthMessage
  private static final Pattern AUTHORIZATION = Pattern.compile("\\s*(\\w*)\\s+(.*)");
  private static final Pattern NVP = Pattern.compile("(\\S*)\\s*\\=\\s*\"([^\"]*)\"");

  protected Map<String, String> getMappedParameters(final HttpServletRequest request) {
    Map<String, String> params = Maps.newHashMap();
    String token = null;

    // old style security token
    if (token == null) {
      token = request.getParameter(SECURITY_TOKEN_PARAM);
    }

    // OAuth2 token as a param
    // NOTE: if oauth_signature_method is present then we have a OAuth 1.0 request
    if (token == null && request.isSecure() && request.getParameter(OAuth.OAUTH_SIGNATURE_METHOD) == null) {
      token = request.getParameter(OAuth.OAUTH_TOKEN);
    }

    // token in authorization header
    if (token == null) {
      for (Enumeration<String> headers = request.getHeaders("Authorization"); headers != null && headers.hasMoreElements();) {
        Matcher m = AUTHORIZATION.matcher(headers.nextElement());
        if (m.matches() && "Token".equalsIgnoreCase(m.group(1))) {
          for (String nvp : COMMAWHITESPACE.split(m.group(2))) {
            m = NVP.matcher(nvp);
            if (m.matches() && "token".equals(m.group(1))) {
              token = OAuth.decodePercent(m.group(2));
            }
          }
        }
      }
    }

    params.put(SecurityTokenCodec.SECURITY_TOKEN_NAME, token);
    params.put(SecurityTokenCodec.ACTIVE_URL_NAME, getActiveUrl(request));
    return params;
  }
  
  protected String getActiveUrl(HttpServletRequest request) {
    return request.getRequestURL().toString();
  }
}
