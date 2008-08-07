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
import org.apache.shindig.social.opensocial.oauth.AuthenticationHandler;
import org.apache.shindig.social.opensocial.oauth.OAuthLookupService;

import com.google.inject.Inject;
import net.oauth.OAuth;
import net.oauth.OAuthMessage;
import net.oauth.server.OAuthServlet;
import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * This class only handles "two-legged" OAuth (aka Consumer Request) OAuth requests. The request
 * must include a xoauth_requestor_id parameter, which will be the userId of the person the
 * container is requesting information on behalf of.
 */
public class OAuthConsumerRequestAuthenticationHandler implements AuthenticationHandler {
  public static final String AUTH_OAUTH_CONSUMER_REQUEST = "OAuth-ConsumerRequest";
  public static final String REQUESTOR_ID_PARAM = "xoauth_requestor_id";
  private final OAuthLookupService service;

  @Inject
  public OAuthConsumerRequestAuthenticationHandler(OAuthLookupService service) {
    this.service = service;
  }

  public String getName() {
    return AUTH_OAUTH_CONSUMER_REQUEST;
  }

  public SecurityToken getSecurityTokenFromRequest(HttpServletRequest request) {
    OAuthMessage requestMessage = OAuthServlet.getMessage(request, null);

    String containerKey = getParameter(requestMessage, OAuth.OAUTH_CONSUMER_KEY);
    String containerSignature = getParameter(requestMessage, OAuth.OAUTH_SIGNATURE);
    String userId = StringUtils.trim(request.getParameter(REQUESTOR_ID_PARAM));

    if (containerKey == null || containerSignature == null || StringUtils.isBlank(userId)) {
      // This isn't a proper OAuth request
      return null;
    }

    if (service.thirdPartyHasAccessToUser(requestMessage, containerKey, userId)) {
      return service.getSecurityToken(containerKey, userId);
    } else {
      return null;
    }
  }

  private String getParameter(OAuthMessage requestMessage, String key) {
    try {
      return requestMessage.getParameter(key);
    } catch (IOException e) {
      return null;
    }
  }

}