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

import com.google.inject.Inject;

import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthException;
import net.oauth.OAuthMessage;
import net.oauth.OAuthServiceProvider;
import net.oauth.SimpleOAuthValidator;
import net.oauth.server.OAuthServlet;
import org.apache.shindig.auth.AuthenticationHandler;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.social.opensocial.oauth.OAuthDataStore;
import org.apache.shindig.social.opensocial.oauth.OAuthEntry;

import java.io.IOException;
import java.net.URISyntaxException;
import javax.servlet.http.HttpServletRequest;

/**
 * Normal three legged OAuth handler
 */
public class OAuthAuthenticationHandler implements AuthenticationHandler {
  private static String AUTH_OAUTH_REQUEST = "OAuth";
  private OAuthDataStore store;

  @Inject
  public OAuthAuthenticationHandler(OAuthDataStore store) {
    this.store = store;
  }

  public String getName() {
    return AUTH_OAUTH_REQUEST;
  }

  public String getWWWAuthenticateHeader(String realm) {
    return String.format("OAuth realm=\"%s\"", realm);
  }

  public SecurityToken getSecurityTokenFromRequest(HttpServletRequest request) {
    OAuthMessage message = OAuthServlet.getMessage(request, null);
    OAuthEntry entry;


    try {
      // no token available...
      if (message.getToken() == null) return null;

      entry = store.getEntry(message.getToken());
    } catch (IOException e) {
      return null;
    }

    if (!isValidOAuthRequest(message, entry)) {
      return null;
    }

    return new OAuthSecurityToken(entry.userId, entry.callbackUrl, entry.appId,
        entry.domain, entry.container);
  }

  private boolean isValidOAuthRequest(OAuthMessage message, OAuthEntry entry) {
    if (entry == null || entry.type != OAuthEntry.Type.ACCESS || entry.isExpired()) {
      throw new InvalidAuthenticationException("access token is invalid.", null);
    }

    OAuthServiceProvider provider = new OAuthServiceProvider(null, null, null);
    OAuthAccessor accessor = new OAuthAccessor(new OAuthConsumer(null, entry.consumerKey,
        entry.consumerSecret, provider));

    accessor.tokenSecret = entry.tokenSecret;
    accessor.accessToken = entry.token;

    try {
      message.validateMessage(accessor, new SimpleOAuthValidator());
    } catch (OAuthException e) {
      throw new InvalidAuthenticationException(e.getMessage(), e);
    } catch (IOException e) {
      return false;
    } catch (URISyntaxException e) {
      return false;
    }

    return true;
  }
}
