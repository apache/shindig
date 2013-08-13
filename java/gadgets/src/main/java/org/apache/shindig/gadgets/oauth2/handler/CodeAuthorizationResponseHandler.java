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
package org.apache.shindig.gadgets.oauth2.handler;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Provider;

import org.apache.shindig.auth.AnonymousSecurityToken;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.oauth2.OAuth2Accessor;
import org.apache.shindig.gadgets.oauth2.OAuth2Error;
import org.apache.shindig.gadgets.oauth2.OAuth2Message;
import org.apache.shindig.gadgets.oauth2.OAuth2Utils;
import org.apache.shindig.gadgets.oauth2.logger.FilteredLogger;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

/**
 *
 * See {@link AuthorizationEndpointResponseHandler}
 *
 * Handles the "code" flow
 */
public class CodeAuthorizationResponseHandler implements AuthorizationEndpointResponseHandler {
  private static final String LOG_CLASS = CodeAuthorizationResponseHandler.class.getName();
  private static final FilteredLogger LOG = FilteredLogger
          .getFilteredLogger(CodeAuthorizationResponseHandler.LOG_CLASS);

  private final List<ClientAuthenticationHandler> clientAuthenticationHandlers;
  private final HttpFetcher fetcher;
  private final Provider<OAuth2Message> oauth2MessageProvider;
  private final List<TokenEndpointResponseHandler> tokenEndpointResponseHandlers;

  @Inject
  public CodeAuthorizationResponseHandler(final Provider<OAuth2Message> oauth2MessageProvider,
          final List<ClientAuthenticationHandler> clientAuthenticationHandlers,
          final List<TokenEndpointResponseHandler> tokenEndpointResponseHandlers,
          final HttpFetcher fetcher) {
    this.oauth2MessageProvider = oauth2MessageProvider;
    this.clientAuthenticationHandlers = clientAuthenticationHandlers;
    this.tokenEndpointResponseHandlers = tokenEndpointResponseHandlers;
    this.fetcher = fetcher;

    if (CodeAuthorizationResponseHandler.LOG.isLoggable()) {
      CodeAuthorizationResponseHandler.LOG.log("this.oauth2MessageProvider = {0}",
              this.oauth2MessageProvider);
      CodeAuthorizationResponseHandler.LOG.log("this.clientAuthenticationHandlers = {0}",
              this.clientAuthenticationHandlers);
      CodeAuthorizationResponseHandler.LOG.log("this.tokenEndpointResponseHandlers = {0}",
              this.tokenEndpointResponseHandlers);
      CodeAuthorizationResponseHandler.LOG.log("this.fetcher = {0}", this.fetcher);
    }
  }

  private static String getAuthorizationBody(final OAuth2Accessor accessor,
          final String authorizationCode) throws UnsupportedEncodingException {
    final boolean isLogging = CodeAuthorizationResponseHandler.LOG.isLoggable();
    if (isLogging) {
      if (authorizationCode != null) {
        CodeAuthorizationResponseHandler.LOG.entering(CodeAuthorizationResponseHandler.LOG_CLASS,
                "getAuthorizationBody", "non-null authorizationCode");
      } else {
        CodeAuthorizationResponseHandler.LOG.entering(CodeAuthorizationResponseHandler.LOG_CLASS,
                "getAuthorizationBody", null);
      }
    }

    String ret = "";

    final Map<String, String> queryParams = Maps.newHashMapWithExpectedSize(5);
    queryParams.put(OAuth2Message.GRANT_TYPE, OAuth2Message.AUTHORIZATION_CODE);
    if (authorizationCode != null) {
      queryParams.put(OAuth2Message.AUTHORIZATION, authorizationCode);
    }
    queryParams.put(OAuth2Message.REDIRECT_URI, accessor.getRedirectUri());

    final String clientId = accessor.getClientId();
    final byte[] secretBytes = accessor.getClientSecret();
    final String secret = new String(secretBytes, "UTF-8");
    queryParams.put(OAuth2Message.CLIENT_ID, clientId);
    queryParams.put(OAuth2Message.CLIENT_SECRET, secret);

    // add any additional parameters
    for (final Map.Entry<String, String> entry : accessor.getAdditionalRequestParams().entrySet()) {
      queryParams.put(entry.getKey(), entry.getValue());
    }

    ret = OAuth2Utils.buildUrl(ret, queryParams, null);

    final char firstChar = ret.charAt(0);
    if (firstChar == '?' || firstChar == '&') {
      ret = ret.substring(1);
    }

    if (isLogging) {
      CodeAuthorizationResponseHandler.LOG.exiting(CodeAuthorizationResponseHandler.LOG_CLASS,
              "getAuthorizationBody");
    }
    return ret;
  }

  private static String getCompleteTokenUrl(final String accessTokenUrl) {
    return OAuth2Utils.buildUrl(accessTokenUrl, null, null);
  }

  public OAuth2HandlerError handleRequest(final OAuth2Accessor accessor,
          final HttpServletRequest request) {
    final boolean isLogging = CodeAuthorizationResponseHandler.LOG.isLoggable();
    if (isLogging) {
      CodeAuthorizationResponseHandler.LOG.entering(CodeAuthorizationResponseHandler.LOG_CLASS,
              "handleRequest", new Object[] { accessor, request != null });
    }

    OAuth2HandlerError ret = null;

    if (accessor == null) {
      ret = new OAuth2HandlerError(OAuth2Error.AUTHORIZATION_CODE_PROBLEM, "accessor is null", null);
    } else if (request == null) {
      ret = new OAuth2HandlerError(OAuth2Error.AUTHORIZATION_CODE_PROBLEM, "request is null", null);
    } else if (!accessor.isValid() || accessor.isErrorResponse() || !accessor.isRedirecting()) {
      ret = new OAuth2HandlerError(OAuth2Error.AUTHORIZATION_CODE_PROBLEM, "accessor is invalid",
              null);
    } else if (!accessor.getGrantType().equalsIgnoreCase(OAuth2Message.AUTHORIZATION)) {
      ret = new OAuth2HandlerError(OAuth2Error.AUTHORIZATION_CODE_PROBLEM,
              "grant_type is not code", null);
    }

    if (ret == null) {
      try {
        final OAuth2Message msg = this.oauth2MessageProvider.get();
        msg.parseRequest(request);
        if (msg.getError() != null) {
          ret = new OAuth2HandlerError(msg.getError(), "error parsing authorization response",
                  null, msg.getErrorUri(), msg.getErrorDescription());
        } else {
          ret = this.setAuthorizationCode(msg.getAuthorization(), accessor);
        }
      } catch (final Exception e) {
        if (CodeAuthorizationResponseHandler.LOG.isLoggable()) {
          CodeAuthorizationResponseHandler.LOG.log(
                  "Exception exchanging authorization code for access_token", e);
        }
        ret = new OAuth2HandlerError(OAuth2Error.AUTHORIZATION_CODE_PROBLEM,
                "Exception exchanging authorization code for access_token", e);
      }
    }

    if (isLogging) {
      CodeAuthorizationResponseHandler.LOG.exiting(CodeAuthorizationResponseHandler.LOG_CLASS,
              "handleRequest", ret);
    }

    return ret;
  }

  public OAuth2HandlerError handleResponse(final OAuth2Accessor accessor,
          final HttpResponse response) {
    return new OAuth2HandlerError(OAuth2Error.AUTHORIZATION_CODE_PROBLEM,
            "doesn't handle responses", null);
  }

  public boolean handlesRequest(final OAuth2Accessor accessor, final HttpServletRequest request) {

    if (accessor == null) {
      return false;
    } else if (request == null) {
      return false;
    } else if (!accessor.isValid() || accessor.isErrorResponse() || !accessor.isRedirecting()) {
      return false;
    } else if (!accessor.getGrantType().equalsIgnoreCase(OAuth2Message.AUTHORIZATION)) {
      return false;
    }

    return true;
  }

  public boolean handlesResponse(final OAuth2Accessor accessor, final HttpResponse response) {
    return false;
  }

  private OAuth2HandlerError setAuthorizationCode(final String authorizationCode,
          final OAuth2Accessor accessor) {

    final boolean isLogging = CodeAuthorizationResponseHandler.LOG.isLoggable();
    if (isLogging) {
      if (authorizationCode != null) {
        CodeAuthorizationResponseHandler.LOG.entering(CodeAuthorizationResponseHandler.LOG_CLASS,
                "setAuthorizationCode", new Object[] { "non-null authorizationCode", accessor });
      } else {
        CodeAuthorizationResponseHandler.LOG.entering(CodeAuthorizationResponseHandler.LOG_CLASS,
                "setAuthorizationCode", new Object[] { null, accessor });
      }
    }

    OAuth2HandlerError ret = null;

    final String tokenUrl = CodeAuthorizationResponseHandler.getCompleteTokenUrl(accessor
            .getTokenUrl());

    final HttpRequest request = new HttpRequest(Uri.parse(tokenUrl));
    request.setMethod("POST");
    request.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
    request.setSecurityToken(new AnonymousSecurityToken("", 0L, accessor.getGadgetUri()));

    if (!OAuth2Utils.isUriAllowed(request.getUri(), accessor.getAllowedDomains())) {
      ret = new OAuth2HandlerError(OAuth2Error.AUTHORIZATION_CODE_PROBLEM,
              "Exception exchanging authorization code for access_token - domain not allowed", null);
    }

    if (ret == null) {
      for (final ClientAuthenticationHandler clientAuthenticationHandler : this.clientAuthenticationHandlers) {
        if (clientAuthenticationHandler.geClientAuthenticationType().equalsIgnoreCase(
                accessor.getClientAuthenticationType())) {
          final OAuth2HandlerError error = clientAuthenticationHandler.addOAuth2Authentication(
                  request, accessor);
          if (error != null) {
            ret = error;
          }
        }
      }
    }

    if (ret == null) {
      try {
        final byte[] body = CodeAuthorizationResponseHandler.getAuthorizationBody(accessor,
                authorizationCode).getBytes("UTF-8");
        request.setPostBody(body);
      } catch (final UnsupportedEncodingException e) {
        if (CodeAuthorizationResponseHandler.LOG.isLoggable()) {
          CodeAuthorizationResponseHandler.LOG.log(
                  "UnsupportedEncodingException getting authorization body", e);
        }
        ret = new OAuth2HandlerError(OAuth2Error.AUTHORIZATION_CODE_PROBLEM,
                "error getting authorization body", e);
      }

      HttpResponse response = null;
      try {
        response = this.fetcher.fetch(request);
      } catch (final GadgetException e) {
        if (CodeAuthorizationResponseHandler.LOG.isLoggable()) {
          CodeAuthorizationResponseHandler.LOG.log("error exchanging code for access_token", e);
        }
        ret = new OAuth2HandlerError(OAuth2Error.AUTHORIZATION_CODE_PROBLEM,
                "error exchanging code for access_token", e);
      }

      if (ret == null && response != null) {
        if (response.getHttpStatusCode() != HttpResponse.SC_OK) {
          final OAuth2Message msg = this.oauth2MessageProvider.get();
          msg.parseJSON(response.getResponseAsString());
          if (msg.getError() != null) {
            ret = new OAuth2HandlerError(msg.getError(), "error exchanging code for access_token",
                    null, msg.getErrorUri(), msg.getErrorDescription());
          }
        }

        if (ret == null) {
          for (final TokenEndpointResponseHandler tokenEndpointResponseHandler : this.tokenEndpointResponseHandlers) {
            if (tokenEndpointResponseHandler.handlesResponse(accessor, response)) {
              ret = tokenEndpointResponseHandler.handleResponse(accessor, response);
              if (ret != null) {
                // error occurred stop processing
                break;
              }
            }
          }
        }
      }
    }

    if (isLogging) {
      CodeAuthorizationResponseHandler.LOG.exiting(CodeAuthorizationResponseHandler.LOG_CLASS,
              "setAuthorizationCode", ret);
    }

    return ret;
  }
}
