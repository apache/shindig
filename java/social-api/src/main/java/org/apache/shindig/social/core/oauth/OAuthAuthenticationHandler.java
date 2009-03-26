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
import com.google.inject.name.Named;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthException;
import net.oauth.OAuthMessage;
import net.oauth.OAuthServiceProvider;
import net.oauth.SimpleOAuthValidator;
import net.oauth.server.OAuthServlet;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.shindig.auth.AuthenticationHandler;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.util.CharsetUtil;
import org.apache.shindig.social.opensocial.oauth.OAuthDataStore;
import org.apache.shindig.social.opensocial.oauth.OAuthEntry;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;

/**
 * Handle both 2-legged consumer and full 3-legged OAuth requests
 */
public class OAuthAuthenticationHandler implements AuthenticationHandler {

  public static final String REQUESTOR_ID_PARAM = "xoauth_requestor_id";
  public static final String OAUTH_BODY_HASH = "oauth_body_hash";

  private final OAuthDataStore store;

  @Deprecated
  private final boolean allowLegacyBodySigning;

  @Inject
  public OAuthAuthenticationHandler(OAuthDataStore store,
      @Named("shindig.oauth.legacy-body-signing") boolean allowLegacyBodySigning) {
    this.store = store;
    this.allowLegacyBodySigning = allowLegacyBodySigning;
  }

  public String getName() {
    return "OAuth";
  }

  public String getWWWAuthenticateHeader(String realm) {
    return String.format("OAuth realm=\"%s\"", realm);
  }

  public SecurityToken getSecurityTokenFromRequest(HttpServletRequest request)
      throws InvalidAuthenticationException {
    OAuthMessage message = OAuthServlet.getMessage(request, null);
    if (StringUtils.isEmpty(getParameter(message, OAuth.OAUTH_SIGNATURE))) {
      // Is not an oauth request
      return null;
    }
    String bodyHash = getParameter(message, OAUTH_BODY_HASH);
    if (!StringUtils.isEmpty(bodyHash)) {
      verifyBodyHash(request, bodyHash);
    }
    try {
      return verifyMessage(message);
    } catch (InvalidAuthenticationException iae) {
      // Legacy body signing is intended for backwards compatability with opensocial clients
      // that assumed they could use the raw request body as a pseudo query param to get
      // body signing. This assumption was born out of the limitations of the OAuth 1.0 spec which
      // states that request bodies are only signed if they are form-encoded. This lead many clients
      // to force a content type of application/x-www-form-urlencoded for xml/json bodies and then
      // hope that recevier decoding of the body didnt have encoding issues. This didnt work out
      // to well so now these clients are required to specify the correct content type. This code
      // lets clients which sign using the old technique to work if they specify the correct content
      // type. This support is deprecated and should be removed later.
      if (allowLegacyBodySigning && requestHasBody(request) &&
          (StringUtils.isEmpty(request.getContentType())  ||
          !request.getContentType().contains(OAuth.FORM_ENCODED))) {
        try {
          message.addParameter(readBodyString(request), "");
          return verifyMessage(message);
        } catch (IOException ioe) {
          throw iae;
        }
      }
      throw iae;
    }
  }

  protected SecurityToken verifyMessage(OAuthMessage message)
      throws InvalidAuthenticationException {
    OAuthEntry entry = getOAuthEntry(message);
    OAuthConsumer authConsumer = getConsumer(message);

    OAuthServiceProvider provider = new OAuthServiceProvider(null, null, null);
    OAuthAccessor accessor = new OAuthAccessor(new OAuthConsumer(null, authConsumer.consumerKey,
        authConsumer.consumerSecret, provider));

    if (entry != null) {
      accessor.tokenSecret = entry.tokenSecret;
      accessor.accessToken = entry.token;
    }

    try {
      message.validateMessage(accessor, new SimpleOAuthValidator());
    } catch (OAuthException e) {
      throw new InvalidAuthenticationException("Unable to verify OAuth request", e);
    } catch (IOException e) {
      throw new InvalidAuthenticationException("Unable to verify OAuth request", e);
    } catch (URISyntaxException e) {
      throw new InvalidAuthenticationException("Unable to verify OAuth request", e);
    }
    return getTokenFromVerifiedRequest(message, entry, authConsumer);
  }

  protected OAuthEntry getOAuthEntry(OAuthMessage message) throws InvalidAuthenticationException {
    OAuthEntry entry = null;
    String token = getParameter(message, OAuth.OAUTH_TOKEN);
    if (!StringUtils.isEmpty(token))  {
      entry = store.getEntry(token);
      if (entry == null) {
        throw new InvalidAuthenticationException("No oauth entry for token: " + token, null);
      } else if (entry.type != OAuthEntry.Type.ACCESS) {
        throw new InvalidAuthenticationException("token is not an access token.", null);
      } else if (entry.isExpired()) {
        throw new InvalidAuthenticationException("access token has expired.", null);
      }
    }
    return entry;
  }

  protected OAuthConsumer getConsumer(OAuthMessage message) throws InvalidAuthenticationException {
    String consumerKey = getParameter(message, OAuth.OAUTH_CONSUMER_KEY);
    OAuthConsumer authConsumer = store.getConsumer(consumerKey);
    if (authConsumer == null) {
      throw new InvalidAuthenticationException("No consumer registered for key " + consumerKey,
          null);
    }
    return authConsumer;
  }

  protected SecurityToken getTokenFromVerifiedRequest(OAuthMessage message, OAuthEntry entry,
      OAuthConsumer authConsumer) {
    if (entry != null) {
      return new OAuthSecurityToken(entry.userId, entry.callbackUrl, entry.appId,
          entry.domain, entry.container);
    } else {
      String userId = getParameter(message, REQUESTOR_ID_PARAM);
      return store.getSecurityTokenForConsumerRequest(authConsumer.consumerKey, userId);
    }
  }

  public static byte[] readBody(HttpServletRequest request) throws IOException {
    if (request.getAttribute(AuthenticationHandler.STASHED_BODY) != null) {
      return (byte[])request.getAttribute(AuthenticationHandler.STASHED_BODY);
    }
    byte[] rawBody = IOUtils.toByteArray(request.getInputStream());
    request.setAttribute(AuthenticationHandler.STASHED_BODY, rawBody);
    return rawBody;
  }

  public static String readBodyString(HttpServletRequest request) throws IOException {
    byte[] rawBody = readBody(request);
    return IOUtils.toString(new ByteArrayInputStream(rawBody), request.getCharacterEncoding());
  }

  public static void verifyBodyHash(HttpServletRequest request, String oauthBodyHash)
      throws InvalidAuthenticationException {
    // we are doing body hash signing which is not permitted for form-encoded data
    if (request.getContentType().contains(OAuth.FORM_ENCODED)) {
      throw new AuthenticationHandler.InvalidAuthenticationException(
          "Cannot use oauth_body_hash with a Content-Type of application/x-www-form-urlencoded",
          null);
    } else if (!requestHasBody(request)) {
      throw new AuthenticationHandler.InvalidAuthenticationException(
          "Cannot use oauth_body_hash with a GET or HEAD request",null);
    } else {
      try {
        byte[] rawBody = readBody(request);
        byte[] received = Base64.decodeBase64(CharsetUtil.getUtf8Bytes(oauthBodyHash));
        byte[] expected = DigestUtils.sha(rawBody);
        if (!Arrays.equals(received, expected)) {
          throw new AuthenticationHandler.InvalidAuthenticationException(
            "oauth_body_hash failed verification", null);
        }
      } catch (IOException ioe) {
        throw new AuthenticationHandler.InvalidAuthenticationException(
          "Unable to read content body for oauth_body_hash verification", null);
      }
    }
  }

  public static String getParameter(OAuthMessage requestMessage, String key) {
    try {
      return StringUtils.trim(requestMessage.getParameter(key));
    } catch (IOException e) {
      return null;
    }
  }

  public static boolean requestHasBody(HttpServletRequest request) {
    return !("GET".equals(request.getMethod()) || "HEAD".equals(request.getMethod()));
  }
}
