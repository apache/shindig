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
import com.google.inject.name.Named;

import net.oauth.*;
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
  private String oauthAuthorizeAction;

  @Inject
  public void setDataStore(OAuthDataStore dataStore) {
    this.dataStore = dataStore;
  }

  @Inject void setAuthorizeAction(@Named("shindig.oauth.authorize-action") String authorizeAction) {
     this.oauthAuthorizeAction = authorizeAction;
  }

  @Override
  protected void doPost(HttpServletRequest servletRequest,
      HttpServletResponse servletResponse) throws ServletException, IOException {

    doGet(servletRequest, servletResponse);
  }

  @Override
  protected void doGet(HttpServletRequest servletRequest,
      HttpServletResponse servletResponse) throws ServletException, IOException {
    String path = servletRequest.getPathInfo();

    try {
      // dispatch
      if (path.endsWith("requestToken")) {
        createRequestToken(servletRequest, servletResponse);
      } else if (path.endsWith("authorize")) {
        authorizeRequestToken(servletRequest, servletResponse);
      } else if (path.endsWith("accessToken")) {
        createAccessToken(servletRequest, servletResponse);
      } else {
        servletResponse.sendError(servletResponse.SC_NOT_FOUND, "unknown Url");
      }
    } catch (OAuthException e) {
      handleException(e, servletRequest, servletResponse, true);
    } catch (URISyntaxException e) {
      handleException(e, servletRequest, servletResponse, true);
    }
  }

  // Hand out a request token if the consumer key and secret are valid
  private void createRequestToken(HttpServletRequest servletRequest,
      HttpServletResponse servletResponse) throws IOException, OAuthException, URISyntaxException {
    OAuthMessage requestMessage = OAuthServlet.getMessage(servletRequest, null);

    String consumerKey = requestMessage.getConsumerKey();
    if (consumerKey == null) {
      OAuthProblemException e = new OAuthProblemException("parameter_absent");
      e.setParameter("oauth_paramaeters_absent", "oauth_consumer_key");
      throw e;
    }
    OAuthConsumer consumer = dataStore.getConsumer(consumerKey);

    if (consumer == null)
      throw new OAuthProblemException("consumer_key_unknown");

    OAuthAccessor accessor = new OAuthAccessor(consumer);
    VALIDATOR.validateMessage(requestMessage, accessor);

    // generate request_token and secret
    OAuthEntry entry = dataStore.generateRequestToken(consumerKey);

    sendResponse(servletResponse, OAuth.newList(OAuth.OAUTH_TOKEN, entry.token,
        OAuth.OAUTH_TOKEN_SECRET, entry.tokenSecret));
  }


  /////////////////////
  // deal with authorization request
  private void authorizeRequestToken(HttpServletRequest servletRequest,
      HttpServletResponse servletResponse) throws ServletException, IOException, OAuthException, URISyntaxException {

    OAuthMessage requestMessage = OAuthServlet.getMessage(servletRequest, null);
    OAuthEntry entry = dataStore.getEntry(requestMessage.getToken());

    if (entry == null) {
      servletResponse.sendError(servletResponse.SC_NOT_FOUND, "Authentication Token not found");
    }

    OAuthConsumer consumer = dataStore.getConsumer(entry.consumerKey);
    String callback = requestMessage.getParameter("oauth_callback");
    if (callback == null) {
      // see if the consumer has a callback
      callback = consumer.callbackURL;
    }

    // Redirect to a UI flow if the token is not authorized
    if (!entry.authorized) {
      // TBD -- need to decode encrypted payload somehow..
      if (this.oauthAuthorizeAction.startsWith("http")) {
        // Redirect to authorization page with params
        // Supply standard set of params
        // TBD
      } else {
        // Use internal forward to a jsp page
        servletRequest.setAttribute("OAUTH_DATASTORE",  dataStore);
        
        servletRequest.setAttribute("OAUTH_ENTRY",  entry);
        servletRequest.setAttribute("CALLBACK", callback);

        servletRequest.setAttribute("TOKEN", entry.token);
        servletRequest.setAttribute("CONSUMER", consumer);
        
        servletRequest.getRequestDispatcher(oauthAuthorizeAction).forward(servletRequest,servletResponse);
      }
      return;
    }

    // If we're here then the entry has been authorized out of band.

    // redirect to callback param oauth_callback
    if (callback == null) {
      servletResponse.setContentType("text/plain");
      OutputStream out = servletResponse.getOutputStream();
      out.write("Token successfully authorized.".getBytes());
      out.close();
    } else {
      callback = OAuth.addParameters(callback, OAuth.OAUTH_TOKEN, entry.token);
      callback = OAuth.addParameters(callback, "user_id", entry.userId);

      servletResponse.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
      servletResponse.setHeader("Location", callback);
    }
  }

  // Hand out an access token if the consumer key and secret are valid and the user authorized
  // the requestToken
  private void createAccessToken(HttpServletRequest servletRequest,
      HttpServletResponse servletResponse) throws ServletException, IOException, OAuthException, URISyntaxException {
    OAuthMessage requestMessage = OAuthServlet.getMessage(servletRequest, null);

    OAuthEntry entry = getValidatedEntry(requestMessage);
    if (entry == null)
      throw new OAuthProblemException("token_rejected");

    if (!entry.authorized) {
      throw new ServletException("additional_authorization_required");
    }

    // turn request token into access token
    OAuthEntry accessEntry = dataStore.convertToAccessToken(entry);

    sendResponse(servletResponse, OAuth.newList(
        OAuth.OAUTH_TOKEN, accessEntry.token,
        OAuth.OAUTH_TOKEN_SECRET, accessEntry.tokenSecret,
        "user_id", entry.userId));
  }


  private OAuthEntry getValidatedEntry(OAuthMessage requestMessage)
      throws IOException, ServletException, OAuthException, URISyntaxException {

    OAuthEntry entry = dataStore.getEntry(requestMessage.getToken());
    if (entry == null)
      throw new OAuthProblemException("token_rejected");

    if (entry.type != OAuthEntry.Type.REQUEST)
      throw new OAuthProblemException("token_used");

    if (entry.isExpired())
      throw new OAuthProblemException("token_expired");

    // find consumer key, compare with supplied value, if present.

    if  (requestMessage.getConsumerKey() == null) {
      OAuthProblemException e = new OAuthProblemException("parameter_absent");
      e.setParameter("oauth_paramaeters_absent", "oauth_consumer");
      throw e;
    }

    String consumerKey = entry.consumerKey;
    if (!consumerKey.equals(requestMessage.getConsumerKey()))
        throw new OAuthProblemException("consumer_key_refused");

    OAuthConsumer consumer = dataStore.getConsumer(consumerKey);

    if (consumer == null)
      throw new OAuthProblemException("consumer_key_unknown");
    
    OAuthAccessor accessor = new OAuthAccessor(consumer);

    accessor.requestToken = entry.token;
    accessor.tokenSecret = entry.tokenSecret;

    VALIDATOR.validateMessage(requestMessage, accessor);

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
