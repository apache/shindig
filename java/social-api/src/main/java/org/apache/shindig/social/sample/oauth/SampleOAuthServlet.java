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
package org.apache.shindig.social.sample.oauth;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.shindig.common.servlet.HttpUtil;
import org.apache.shindig.common.servlet.InjectedServlet;
import org.apache.shindig.social.opensocial.oauth.OAuthDataStore;
import org.apache.shindig.social.opensocial.oauth.OAuthEntry;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthException;
import net.oauth.OAuthMessage;
import net.oauth.OAuthProblemException;
import net.oauth.OAuthValidator;
import net.oauth.OAuth.Parameter;
import net.oauth.server.OAuthServlet;

/**
 * This is a sample class that demonstrates how oauth tokens can be handed out and authorized.
 * This is most certainly not production code. Your server should have clear ui, require user
 * login for creating consumer secrets and authorizing request tokens, do better patch dispatching,
 * and use a non-in memory data store.
 */
public class SampleOAuthServlet extends InjectedServlet {
  private OAuthValidator validator;
  private OAuthDataStore dataStore;
  private String oauthAuthorizeAction;

  @Inject
  public void setValidator(OAuthValidator validator) {
    this.validator = validator;
  }

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
    HttpUtil.setNoCache(servletResponse);
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
        servletResponse.sendError(HttpServletResponse.SC_NOT_FOUND, "unknown Url");
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
      OAuthProblemException e = new OAuthProblemException(OAuth.Problems.PARAMETER_ABSENT);
      e.setParameter(OAuth.Problems.OAUTH_PARAMETERS_ABSENT, OAuth.OAUTH_CONSUMER_KEY);
      throw e;
    }
    OAuthConsumer consumer = dataStore.getConsumer(consumerKey);

    if (consumer == null)
      throw new OAuthProblemException(OAuth.Problems.CONSUMER_KEY_UNKNOWN);

    OAuthAccessor accessor = new OAuthAccessor(consumer);
    validator.validateMessage(requestMessage, accessor);

    String callback = requestMessage.getParameter(OAuth.OAUTH_CALLBACK);

    if (callback == null) {
      // see if the consumer has a callback
      callback = consumer.callbackURL;
    }
    if (callback == null) {
      callback = "oob";
    }

    // generate request_token and secret
    OAuthEntry entry = dataStore.generateRequestToken(consumerKey,
                                                      requestMessage.getParameter(OAuth.OAUTH_VERSION), callback);

    List<Parameter> responseParams = OAuth.newList(OAuth.OAUTH_TOKEN, entry.getToken(),
                                                   OAuth.OAUTH_TOKEN_SECRET, entry.getTokenSecret());
    if (callback != null) {
      responseParams.add(new Parameter(OAuth.OAUTH_CALLBACK_CONFIRMED, "true"));
    }
    sendResponse(servletResponse, responseParams);
  }


  /////////////////////
  // deal with authorization request
  private void authorizeRequestToken(HttpServletRequest servletRequest,
                                     HttpServletResponse servletResponse) throws ServletException, IOException, OAuthException, URISyntaxException {

    OAuthMessage requestMessage = OAuthServlet.getMessage(servletRequest, null);

    if (requestMessage.getToken() == null) {
      // MALFORMED REQUEST
      servletResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, "Authentication token not found");
      return;
    }
    OAuthEntry entry = dataStore.getEntry(requestMessage.getToken());

    if (entry == null) {
      servletResponse.sendError(HttpServletResponse.SC_NOT_FOUND, "OAuth Entry not found");
      return;
    }

    OAuthConsumer consumer = dataStore.getConsumer(entry.getConsumerKey());

    // Extremely rare case where consumer dissappears
    if (consumer == null) {
      servletResponse.sendError(HttpServletResponse.SC_NOT_FOUND, "consumer for entry not found");
      return;
    }

    // The token is disabled if you try to convert to an access token prior to authorization
    if (entry.getType() == OAuthEntry.Type.DISABLED) {
      servletResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "This token is disabled, please reinitate login");
      return;
    }

    String callback = entry.getCallbackUrl();

    // Redirect to a UI flow if the token is not authorized
    if (!entry.isAuthorized()) {
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

        servletRequest.setAttribute("TOKEN", entry.getToken());
        servletRequest.setAttribute("CONSUMER", consumer);

        servletRequest.getRequestDispatcher(oauthAuthorizeAction).forward(servletRequest,servletResponse);
      }
      return;
    }

    // If we're here then the entry has been authorized

    // redirect to callback
    if (callback == null || "oob".equals(callback)) {
      // consumer did not specify a callback
      servletResponse.setContentType("text/plain");
      PrintWriter out = servletResponse.getWriter();
      out.write("Token successfully authorized.\n");
      if (entry.getCallbackToken() != null) {
        // Usability fail.
        out.write("Please enter code " + entry.getCallbackToken() + " at the consumer.");
      }
    } else {
      callback = OAuth.addParameters(callback, OAuth.OAUTH_TOKEN, entry.getToken());
      // Add user_id to the callback
      callback = OAuth.addParameters(callback, "user_id", entry.getUserId());
      if (entry.getCallbackToken() != null) {
        callback = OAuth.addParameters(callback, OAuth.OAUTH_VERIFIER,
                                       entry.getCallbackToken());
      }

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
      throw new OAuthProblemException(OAuth.Problems.TOKEN_REJECTED);

    if (entry.getCallbackToken() != null) {
      // We're using the fixed protocol
      String clientCallbackToken = requestMessage.getParameter(OAuth.OAUTH_VERIFIER);
      if (!entry.getCallbackToken().equals(clientCallbackToken)) {
        dataStore.disableToken(entry);
        servletResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "This token is not authorized");
        return;
      }
    } else if (!entry.isAuthorized()) {
      // Old protocol.  Catch consumers trying to convert a token to one that's not authorized
      dataStore.disableToken(entry);
      servletResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "This token is not authorized");
      return;
    }

    // turn request token into access token
    OAuthEntry accessEntry = dataStore.convertToAccessToken(entry);

    sendResponse(servletResponse, OAuth.newList(
                   OAuth.OAUTH_TOKEN, accessEntry.getToken(),
                   OAuth.OAUTH_TOKEN_SECRET, accessEntry.getTokenSecret(),
                   "user_id", entry.getUserId()));
  }


  private OAuthEntry getValidatedEntry(OAuthMessage requestMessage)
    throws IOException, ServletException, OAuthException, URISyntaxException {

    OAuthEntry entry = dataStore.getEntry(requestMessage.getToken());
    if (entry == null)
      throw new OAuthProblemException(OAuth.Problems.TOKEN_REJECTED);

    if (entry.getType() != OAuthEntry.Type.REQUEST)
      throw new OAuthProblemException(OAuth.Problems.TOKEN_USED);

    if (entry.isExpired())
      throw new OAuthProblemException(OAuth.Problems.TOKEN_EXPIRED);

    // find consumer key, compare with supplied value, if present.

    if  (requestMessage.getConsumerKey() == null) {
      OAuthProblemException e = new OAuthProblemException(OAuth.Problems.PARAMETER_ABSENT);
      e.setParameter(OAuth.Problems.OAUTH_PARAMETERS_ABSENT, OAuth.OAUTH_CONSUMER_KEY);
      throw e;
    }

    String consumerKey = entry.getConsumerKey();
    if (!consumerKey.equals(requestMessage.getConsumerKey()))
      throw new OAuthProblemException(OAuth.Problems.CONSUMER_KEY_REFUSED);

    OAuthConsumer consumer = dataStore.getConsumer(consumerKey);
    if (consumer == null)
      throw new OAuthProblemException(OAuth.Problems.CONSUMER_KEY_UNKNOWN);

    OAuthAccessor accessor = new OAuthAccessor(consumer);
    accessor.requestToken = entry.getToken();
    accessor.tokenSecret = entry.getTokenSecret();

    validator.validateMessage(requestMessage, accessor);

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

    if (request.getHeader("Host") != null) {
      realm += request.getHeader("Host");
    } else {
      realm += request.getLocalName();
    }
    OAuthServlet.handleException(response, e, realm, sendBody);
  }

}
