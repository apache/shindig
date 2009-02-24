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

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthException;
import net.oauth.OAuthMessage;
import net.oauth.SimpleOAuthValidator;
import net.oauth.server.OAuthServlet;

import org.apache.commons.lang.StringUtils;
import org.apache.shindig.auth.AuthenticationHandler;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.social.opensocial.oauth.OAuthDataStore;

import java.io.IOException;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletRequest;

import com.google.inject.Inject;

/**
 * This class only handles "two-legged" OAuth (aka Consumer Request) OAuth requests. The request
 * must include a xoauth_requestor_id parameter, which will be the userId of the person the
 * container is requesting information on behalf of.
 */
public class OAuthConsumerRequestAuthenticationHandler implements AuthenticationHandler {
  public static final String AUTH_OAUTH_CONSUMER_REQUEST = "OAuth-ConsumerRequest";
  public static final String REQUESTOR_ID_PARAM = "xoauth_requestor_id";
  private OAuthDataStore store;

  @Inject
  public OAuthConsumerRequestAuthenticationHandler(OAuthDataStore store) {
    this.store = store;
  }

  public String getName() {
    return AUTH_OAUTH_CONSUMER_REQUEST;
  }

  public String getWWWAuthenticateHeader(String realm) {
    return String.format("OAuth realm=\"%s\"", realm);
  }

  public SecurityToken getSecurityTokenFromRequest(HttpServletRequest request) {
    OAuthMessage requestMessage = OAuthServlet.getMessage(request, null);
    String token = getParameter(requestMessage, OAuth.OAUTH_TOKEN);

    if (StringUtils.isBlank(token) || !isValidOAuthRequest(requestMessage)) {
      return null;
    }

    String userId = StringUtils.trim(request.getParameter(REQUESTOR_ID_PARAM));
    try {
      return store.getSecurityTokenForConsumerRequest(requestMessage.getToken(), userId);
    } catch (IOException e) {
      throw new InvalidAuthenticationException(e.getMessage(), e);
    }
  }

  private boolean isValidOAuthRequest(OAuthMessage requestMessage) {
    String consumerKey = getParameter(requestMessage, OAuth.OAUTH_CONSUMER_KEY);
    OAuthConsumer consumer = store.getConsumer(consumerKey);
    OAuthAccessor accessor = new OAuthAccessor(consumer);

    SimpleOAuthValidator validator = new SimpleOAuthValidator();
    try {
      validator.validateMessage(requestMessage, accessor);
      return true;
    } catch (IOException e) {
      return false;
    } catch (URISyntaxException e) {
      return false;
    } catch (OAuthException e) {
      return false;
    }
  }

  private String getParameter(OAuthMessage requestMessage, String key) {
    try {
      return StringUtils.trim(requestMessage.getParameter(key));
    } catch (IOException e) {
      return null;
    }
  }

}
