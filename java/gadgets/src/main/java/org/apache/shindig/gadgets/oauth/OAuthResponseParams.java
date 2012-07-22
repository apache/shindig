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
package org.apache.shindig.gadgets.oauth;

import com.google.common.collect.Lists;

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.Pair;
import org.apache.shindig.common.crypto.BlobCrypter;
import org.apache.shindig.common.crypto.BlobCrypterException;
import org.apache.shindig.common.logging.i18n.MessageKeys;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Container for OAuth specific data to include in the response to the client.
 */
public class OAuthResponseParams {
  private static final Logger LOG = Logger.getLogger(OAuthResponseParams.class.getName(),MessageKeys.MESSAGES);

  // Finds the values of sensitive response params: oauth_token_secret and oauth_session_handle
  private static final Pattern REMOVE_SECRETS =
      Pattern.compile("(?<=(oauth_token_secret|oauth_session_handle)=)[^=& \t\r\n]*");

  // names for the JSON values we return to the client
  public static final String CLIENT_STATE = "oauthState";
  public static final String APPROVAL_URL = "oauthApprovalUrl";
  public static final String ERROR_CODE = "oauthError";
  public static final String ERROR_TEXT = "oauthErrorText";

  /**
   * Transient state we want to cache client side.
   */
  private final OAuthClientState newClientState;

  /**
   * Security token used to authenticate request.
   */
  private final SecurityToken securityToken;

  /**
   * Original request from client.
   */
  private final HttpRequest originalRequest;

  /**
   * Request/response pairs we sent onward.
   */
  private final List<Pair<HttpRequest, HttpResponse>> requestTrace = Lists.newArrayList();

  /**
   * Authorization URL for the client.
   */
  private String aznUrl;

  /**
   * Whether we should include the request trace in the response to the application.
   *
   * It might be nice to make this configurable based on options passed to makeRequest.  For now
   * we use some heuristics to figure it out.
   */
  private boolean sendTraceToClient;

  /**
   * Create response parameters.
   */
  public OAuthResponseParams(SecurityToken securityToken, HttpRequest originalRequest,
      BlobCrypter stateCrypter) {
    this.securityToken = securityToken;
    this.originalRequest = originalRequest;
    newClientState = new OAuthClientState(stateCrypter);
  }

  /**
   * Log a warning message that includes the details of the request.
   */
  public void logDetailedWarning(String classname, String method, String msgKey) {
    if (LOG.isLoggable(Level.FINE)) {
      LOG.log(Level.FINE,getDetails(null));
    } else if (LOG.isLoggable(Level.WARNING)) {
    	LOG.logp(Level.WARNING, classname, method, msgKey, new Object[] {getDetails(null)});
    }
  }

  /**
   * Log a warning message that includes the details of the request and the thrown exception.
   */
  public void logDetailedWarning(String classname, String method, String msgKey, Throwable e) {
    if (LOG.isLoggable(Level.FINE)) {
      LOG.log(Level.FINE, getDetails(e), e);
    } else if (LOG.isLoggable(Level.WARNING)) {
       LOG.logp(Level.WARNING, classname, method, msgKey, new Object[] {e.getMessage()});
    }
  }

  public void logDetailedInfo(String classname, String method, String msgKey, Throwable e) {
    if (LOG.isLoggable(Level.FINE)) {
      LOG.log(Level.FINE, getDetails(e), e);
    } else if (LOG.isLoggable(Level.INFO)) {
    	LOG.logp(Level.INFO, classname, method, msgKey, new Object[] {e.getMessage()});
    }
  }

  /**
   * Log a warning message that includes the details of the request.
   */
  public void logDetailedWarning(String note) {
    if (LOG.isLoggable(Level.FINE)) {
      LOG.log(Level.FINE, note + '\n' + getDetails(null));
    } else if (LOG.isLoggable(Level.WARNING)) {
      LOG.log(Level.WARNING, note);
    }
  }

  /**
   * Log a warning message that includes the details of the request and the thrown exception.
   */
  public void logDetailedWarning(String note, Throwable e) {
    if (LOG.isLoggable(Level.FINE)) {
      LOG.log(Level.FINE, note + '\n' + getDetails(e), e);
    } else if (LOG.isLoggable(Level.WARNING)) {
      LOG.log(Level.WARNING, note + ": " + e.getMessage());
    }
  }

  public void logDetailedInfo(String note, Throwable e) {
    if (LOG.isLoggable(Level.FINE)) {
      LOG.log(Level.FINE, note + '\n' + getDetails(e), e);
    } else if (LOG.isLoggable(Level.INFO)) {
      LOG.log(Level.INFO, note + ": " + e.getMessage());
    }
  }

  /**
   * Add a request/response pair to our trace of actions associated with this request.
   */
  public void addRequestTrace(HttpRequest request, HttpResponse response) {
    this.requestTrace.add(Pair.of(request, response));
  }

  /**
   * @return true if the target server returned an error at some point during the request
   */
  public boolean sawErrorResponse() {
    for (Pair<HttpRequest, HttpResponse> event : requestTrace) {
      if (event.two == null || event.two.isError()) {
        return true;
      }
    }
    return false;
  }

  private String getDetails(Throwable e) {
    String error = null;

    if (null != e) {
      if(e instanceof OAuthRequestException) {
        OAuthRequestException reqException = ((OAuthRequestException) e);
        error = reqException.getError() + ", " + reqException.getErrorText();
      }
      else {
        error = e.getMessage();
      }
    }

    return "OAuth error [" + error + "] for application "
        + securityToken.getAppUrl() + ".  Request trace:" + getRequestTrace();
  }

  private String getRequestTrace() {
    StringBuilder trace = new StringBuilder();
    trace.append("\n==== Original request:\n");
    trace.append(originalRequest);
    trace.append("\n====");
    int i = 1;
    for (Pair<HttpRequest, HttpResponse> event : requestTrace) {
      trace.append("\n==== Sent request ").append(i).append(":\n");
      if (event.one != null) {
        trace.append(filterSecrets(event.one.toString()));
      }
      trace.append("\n==== Received response ").append(i).append(":\n");
      if (event.two != null) {
        trace.append(filterSecrets(event.two.toString()));
      }
      trace.append("\n====");
      ++i;
    }
    return trace.toString();
  }

  /**
   * Removes security sensitive parameters from requests and responses.
   */
  static String filterSecrets(String in) {
    Matcher m = REMOVE_SECRETS.matcher(in);
    return m.replaceAll("REMOVED");
  }

  /**
   * Update a response with additional data to be returned to the application.
   */
  public void addToResponse(HttpResponseBuilder response, OAuthRequestException e) {
    if (!newClientState.isEmpty()) {
      try {
        response.setMetadata(CLIENT_STATE, newClientState.getEncryptedState());
      } catch (BlobCrypterException cryptException) {
        // Configuration error somewhere, this should never happen.
        throw new RuntimeException(cryptException);
      }
    }
    if (aznUrl != null) {
      response.setMetadata(APPROVAL_URL, aznUrl);
    }

    if (e != null || sendTraceToClient) {
      StringBuilder verboseError = new StringBuilder();

      if (e != null) {
        response.setMetadata(ERROR_CODE, e.getError());
        verboseError.append(e.getErrorText());
      }
      if (sendTraceToClient) {
        verboseError.append('\n');
        verboseError.append(getRequestTrace());
      }

      response.setMetadata(ERROR_TEXT, verboseError.toString());
    }
  }

  /**
   * Get the state we will return to the client.
   */
  public OAuthClientState getNewClientState() {
    return newClientState;
  }

  public String getAznUrl() {
    return aznUrl;
  }

  /**
   * Set the authorization URL we will return to the client.
   */
  public void setAznUrl(String aznUrl) {
    this.aznUrl = aznUrl;
  }

  public boolean sendTraceToClient() {
    return sendTraceToClient;
  }

  public void setSendTraceToClient(boolean sendTraceToClient) {
    this.sendTraceToClient = sendTraceToClient;
  }
}
