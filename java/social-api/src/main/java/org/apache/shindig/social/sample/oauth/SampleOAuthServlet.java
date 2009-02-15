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
package org.apache.shindig.social.sample.oauth;

import com.google.inject.Inject;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthException;
import net.oauth.OAuthMessage;
import net.oauth.OAuthValidator;
import net.oauth.SimpleOAuthValidator;
import net.oauth.server.OAuthServlet;
import org.apache.shindig.common.servlet.InjectedServlet;
import org.apache.shindig.social.opensocial.oauth.OAuthEntry;
import org.apache.shindig.social.opensocial.oauth.OAuthDataStore;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This is a sample class that demonstrates how oauth tokens can be handed out and authorized.
 * This is most certainly not production code. Your server should have clear ui, require user
 * login for creating consumer secrets and authorizing request tokens, do better patch dispatching,
 * and use a non-in memory data store.
 */
public class SampleOAuthServlet extends InjectedServlet {
  public static final OAuthValidator VALIDATOR = new SimpleOAuthValidator();
  private OAuthDataStore dataStore;

  @Inject
  public void setDataStore(OAuthDataStore dataStore) {
    this.dataStore = dataStore;
  }

  @Override
  protected void doGet(HttpServletRequest servletRequest,
      HttpServletResponse servletResponse) throws ServletException, IOException {
    String path = servletRequest.getPathInfo();

    if (path.endsWith("requestToken")) {
      createRequestToken(servletRequest, servletResponse);
    } else if (path.endsWith("authorize")) {
      authorizeRequestToken(servletRequest, servletResponse);
    } else if (path.endsWith("accessToken")) {
      createAccessToken(servletRequest, servletResponse);
    }
  }

  // Hand out a request token if the consumer key and secret are valid
  private void createRequestToken(HttpServletRequest servletRequest,
      HttpServletResponse servletResponse) throws ServletException, IOException {
    OAuthMessage requestMessage = OAuthServlet.getMessage(servletRequest, null);

    String consumerKey = requestMessage.getConsumerKey();
    String consumerSecret = dataStore.getConsumerSecret(consumerKey);

    OAuthAccessor accessor = new OAuthAccessor(new OAuthConsumer(null, consumerKey,
        consumerSecret, null));
    try {
      VALIDATOR.validateMessage(requestMessage, accessor);
    } catch (OAuthException e) {
      handleException(e, servletRequest, servletResponse, true);
    } catch (URISyntaxException e) {
      handleException(e, servletRequest, servletResponse, true);
    }

    // generate request_token and secret
    OAuthEntry entry = dataStore.generateRequestToken(consumerKey);

    sendResponse(servletResponse, OAuth.newList(OAuth.OAUTH_TOKEN, entry.token,
        OAuth.OAUTH_TOKEN_SECRET, entry.tokenSecret));
  }

  private void authorizeRequestToken(HttpServletRequest servletRequest,
      HttpServletResponse servletResponse) throws ServletException, IOException {
    OAuthMessage requestMessage = OAuthServlet.getMessage(servletRequest, null);
    OAuthEntry entry = getRequestToken(servletRequest, servletResponse, requestMessage);

    // NOTE: Generally there would be a ui flow here, where the currently logged in user would be
    // asked if they want to share their data with the third party that holds the consumer key
    // We are simply going to assume that "canonical" has already granted access
    dataStore.authorizeToken(entry, "canonical");

    if (!entry.authorized) {
        
    }
    // redirect to callback param oauth_callback
    String callback = requestMessage.getParameter("oauth_callback");
    if (callback == null) {
      servletResponse.setContentType("text/plain");
      OutputStream out = servletResponse.getOutputStream();
      out.write("Token successfully authorized.".getBytes());
      out.close();
    } else {
      callback = OAuth.addParameters(callback, OAuth.OAUTH_TOKEN, entry.token);
      servletResponse.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
      servletResponse.setHeader("Location", callback);
    }
  }

  // Hand out an access token if the consumer key and secret are valid and the user authorized
  // the requestToken
  private void createAccessToken(HttpServletRequest servletRequest,
      HttpServletResponse servletResponse) throws ServletException, IOException {
    OAuthMessage requestMessage = OAuthServlet.getMessage(servletRequest, null);

    OAuthEntry entry = getRequestToken(servletRequest, servletResponse, requestMessage);
    if (!entry.authorized) {
      throw new ServletException("permission denied. request token has not been authorized.");
    }

    // turn request token into access token
    OAuthEntry accessEntry = dataStore.convertToAccessToken(entry);

    sendResponse(servletResponse, OAuth.newList(OAuth.OAUTH_TOKEN, accessEntry.token,
        OAuth.OAUTH_TOKEN_SECRET, accessEntry.tokenSecret));
  }

  private OAuthEntry getRequestToken(HttpServletRequest servletRequest,
      HttpServletResponse servletResponse, OAuthMessage requestMessage)
      throws IOException, ServletException {

    System.out.println("Getting a request Token for message: " + requestMessage);

    OAuthEntry entry = dataStore.getEntry(requestMessage.getToken());
    if (entry == null || entry.type != OAuthEntry.Type.REQUEST || entry.isExpired()) {
      throw new ServletException("permission denied. request token is invalid.");
    }

    // find consumer key, compare with supplied value, if present.
    String consumerKey = entry.consumerKey;
    if (requestMessage.getConsumerKey() != null && !consumerKey.equals(requestMessage.getConsumerKey())) {
        throw new ServletException("permission denied. consumer keys don't match.");
    }

    String consumerSecret = dataStore.getConsumerSecret(consumerKey);

    OAuthAccessor accessor = new OAuthAccessor(new OAuthConsumer(null, consumerKey,
        consumerSecret, null));
    accessor.requestToken = entry.token;
    accessor.tokenSecret = entry.tokenSecret;

    try {
      VALIDATOR.validateMessage(requestMessage, accessor);
    } catch (OAuthException e) {
      handleException(e, servletRequest, servletResponse, true);
    } catch (URISyntaxException e) {
      handleException(e, servletRequest, servletResponse, true);
    }
    return entry;
  }

  private void sendResponse(HttpServletResponse servletResponse, List<OAuth.Parameter> parameters)
      throws IOException {
    servletResponse.setContentType("text/plain");
    OutputStream out = servletResponse.getOutputStream();
    OAuth.formEncode(parameters, out);
    out.close();
  }

  private static void handleException(Exception e, HttpServletRequest request,
      HttpServletResponse response, boolean sendBody)
      throws IOException, ServletException {
    String realm = (request.isSecure()) ? "https://" : "http://";
    realm += request.getLocalName();
    OAuthServlet.handleException(response, e, realm, sendBody);
  }

}
