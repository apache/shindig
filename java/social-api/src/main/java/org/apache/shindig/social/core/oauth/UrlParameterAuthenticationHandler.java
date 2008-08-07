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
package org.apache.shindig.social.core.oauth;

import org.apache.shindig.common.SecurityToken;
import org.apache.shindig.common.SecurityTokenDecoder;
import org.apache.shindig.common.SecurityTokenException;
import org.apache.shindig.common.servlet.ParameterFetcher;
import org.apache.shindig.social.opensocial.oauth.AuthenticationHandler;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import javax.servlet.http.HttpServletRequest;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UrlParameterAuthenticationHandler implements AuthenticationHandler {
  public static final String AUTH_URL_PARAMETER = "SecurityTokenUrlParameter";

  private static final Logger logger = Logger.getLogger(
      UrlParameterAuthenticationHandler.class.getName());

  private final SecurityTokenDecoder securityTokenDecoder;
  private final ParameterFetcher parameterFetcher;

  @Inject
  public UrlParameterAuthenticationHandler(SecurityTokenDecoder securityTokenDecoder,
      @Named("DataServiceServlet")ParameterFetcher parameterFetcher) {
    this.securityTokenDecoder = securityTokenDecoder;
    this.parameterFetcher = parameterFetcher;
  }

  public String getName() {
    return AUTH_URL_PARAMETER;
  }

  public SecurityToken getSecurityTokenFromRequest(HttpServletRequest request) {
    try {
      return securityTokenDecoder.createToken(parameterFetcher.fetch(request));
    } catch (SecurityTokenException e) {
      logger.log(Level.INFO, "Valid security token not found.", e);
      return null;
    }
  }
}
