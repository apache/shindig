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
package org.apache.shindig.gadgets.oauth2;

import com.google.common.collect.Maps;
import com.google.inject.Inject;

import org.apache.shindig.auth.AnonymousSecurityToken;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.apache.shindig.gadgets.oauth2.handler.AuthorizationEndpointResponseHandler;
import org.apache.shindig.gadgets.oauth2.handler.ClientAuthenticationHandler;
import org.apache.shindig.gadgets.oauth2.handler.GrantRequestHandler;
import org.apache.shindig.gadgets.oauth2.handler.OAuth2HandlerError;
import org.apache.shindig.gadgets.oauth2.handler.ResourceRequestHandler;
import org.apache.shindig.gadgets.oauth2.handler.TokenEndpointResponseHandler;
import org.apache.shindig.gadgets.oauth2.logger.FilteredLogger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * see {@link OAuth2Request}
 *
 */
public class BasicOAuth2Request implements OAuth2Request {
  private static final String LOG_CLASS = BasicOAuth2Request.class.getName();
  private static final FilteredLogger LOG = FilteredLogger
          .getFilteredLogger(BasicOAuth2Request.LOG_CLASS);

  private static final short MAX_ATTEMPTS = 3;

  private OAuth2Accessor internalAccessor;

  private OAuth2Arguments arguments;

  private final List<AuthorizationEndpointResponseHandler> authorizationEndpointResponseHandlers;

  private final List<ClientAuthenticationHandler> clientAuthenticationHandlers;

  private final HttpFetcher fetcher;

  private final OAuth2FetcherConfig fetcherConfig;

  private final List<GrantRequestHandler> grantRequestHandlers;

  private HttpRequest realRequest;

  private final List<ResourceRequestHandler> resourceRequestHandlers;

  private OAuth2ResponseParams responseParams;

  private SecurityToken securityToken;

  private final OAuth2Store store;

  private final List<TokenEndpointResponseHandler> tokenEndpointResponseHandlers;

  private final boolean sendTraceToClient;

  private final OAuth2RequestParameterGenerator requestParameterGenerator;

  private short attemptCounter = 0;

  /**
   * @param fetcherConfig
   *          configuration options for the fetcher
   * @param fetcher
   *          fetcher to use for actually making requests
   */
  @Inject
  public BasicOAuth2Request(final OAuth2FetcherConfig fetcherConfig, final HttpFetcher fetcher,
          final List<AuthorizationEndpointResponseHandler> authorizationEndpointResponseHandlers,
          final List<ClientAuthenticationHandler> clientAuthenticationHandlers,
          final List<GrantRequestHandler> grantRequestHandlers,
          final List<ResourceRequestHandler> resourceRequestHandlers,
          final List<TokenEndpointResponseHandler> tokenEndpointResponseHandlers,
          final boolean sendTraceToClient,
          final OAuth2RequestParameterGenerator requestParameterGenerator) {
    this.fetcherConfig = fetcherConfig;
    if (this.fetcherConfig != null) {
      this.store = this.fetcherConfig.getOAuth2Store();
    } else {
      this.store = null;
    }
    this.fetcher = fetcher;
    this.authorizationEndpointResponseHandlers = authorizationEndpointResponseHandlers;
    this.clientAuthenticationHandlers = clientAuthenticationHandlers;
    this.grantRequestHandlers = grantRequestHandlers;
    this.resourceRequestHandlers = resourceRequestHandlers;
    this.tokenEndpointResponseHandlers = tokenEndpointResponseHandlers;
    this.sendTraceToClient = sendTraceToClient;
    this.requestParameterGenerator = requestParameterGenerator;

    if (BasicOAuth2Request.LOG.isLoggable()) {
      BasicOAuth2Request.LOG.log("this.fetcherConfig = {0}", this.fetcherConfig);
      BasicOAuth2Request.LOG.log("this.store = {0}", this.store);
      BasicOAuth2Request.LOG.log("this.fetcher = {0}", this.fetcher);
      BasicOAuth2Request.LOG.log("this.authorizationEndpointResponseHandlers = {0}",
              this.authorizationEndpointResponseHandlers);
      BasicOAuth2Request.LOG.log("this.clientAuthenticationHandlers = {0}",
              this.clientAuthenticationHandlers);
      BasicOAuth2Request.LOG.log("this.grantRequestHandlers = {0}", this.grantRequestHandlers);
      BasicOAuth2Request.LOG
              .log("this.resourceRequestHandlers = {0}", this.resourceRequestHandlers);
      BasicOAuth2Request.LOG.log("this.tokenEndpointResponseHandlers = {0}",
              this.tokenEndpointResponseHandlers);
      BasicOAuth2Request.LOG.log("this.sendTraceToClient = {0}", this.sendTraceToClient);
    }
  }

  public HttpResponse fetch(final HttpRequest request) {
    final boolean isLogging = BasicOAuth2Request.LOG.isLoggable();
    if (isLogging) {
      BasicOAuth2Request.LOG.entering(BasicOAuth2Request.LOG_CLASS, "fetch", request);
    }

    OAuth2Accessor accessor = null;

    HttpResponse response = null;

    try {
      // First step is to get an OAuth2Accessor for this request
      if (request == null || request.getSecurityToken() == null) {
        // Any errors before we have an accessor are special cases
        response = this.sendErrorResponse(null, OAuth2Error.MISSING_FETCH_PARAMS,
                "no request or security token");
      } else {
        this.realRequest = request;
        this.securityToken = request.getSecurityToken();
        this.responseParams = new OAuth2ResponseParams();
        this.arguments = this.realRequest.getOAuth2Arguments();

        if (BasicOAuth2Request.LOG.isLoggable()) {
          BasicOAuth2Request.LOG.log("this.realRequest = {0}", this.realRequest);
          BasicOAuth2Request.LOG.log("this.securityToken = {0}", this.securityToken);
          BasicOAuth2Request.LOG.log("this.responseParams = {0}", this.responseParams);
          BasicOAuth2Request.LOG.log("this.arguments = {0}", this.arguments);
        }

        if (this.responseParams == null || this.arguments == null) {
          // Any errors before we have an accessor are special cases
          return this.sendErrorResponse(null, OAuth2Error.FETCH_INIT_PROBLEM,
                  "no responseParams or arguments");
        }

        accessor = this.getAccessor();

        if (BasicOAuth2Request.LOG.isLoggable()) {
          BasicOAuth2Request.LOG.log("accessor", accessor);
        }

        if (accessor == null) {
          // Any errors before we have an accessor are special cases
          response = this.sendErrorResponse(null, OAuth2Error.FETCH_INIT_PROBLEM,
                  "accessor is null");
        } else {
          accessor.setRedirecting(false);

          final Map<String, String> requestParams = this.requestParameterGenerator
                  .generateParams(this.realRequest);
          accessor.setAdditionalRequestParams(requestParams);

          HttpResponseBuilder responseBuilder = null;
          if (!accessor.isErrorResponse()) {
            responseBuilder = this.attemptFetch(accessor);
          }

          response = this.processResponse(accessor, responseBuilder);
        }
      }
    } catch (final Throwable t) {
      BasicOAuth2Request.LOG.log(Level.SEVERE, "exception occurred during fetch", t);
      if (accessor == null) {
        accessor = new BasicOAuth2Accessor(t, OAuth2Error.FETCH_PROBLEM,
                "exception occurred during fetch", "");
      } else {
        accessor.setErrorResponse(t, OAuth2Error.FETCH_PROBLEM, "exception occurred during fetch",
                "");
      }
      response = this.processResponse(accessor, this.getErrorResponseBuilder(t,
              OAuth2Error.FETCH_PROBLEM, "exception occurred during fetch"));
    } finally {
      if (accessor != null) {
        if (!accessor.isRedirecting()) {
          if (BasicOAuth2Request.LOG.isLoggable()) {
            BasicOAuth2Request.LOG.log("accessor is not redirecting, remove it", accessor);
          }
          accessor.invalidate();
          this.store.removeOAuth2Accessor(accessor);
          this.internalAccessor = null;
        } else {
          if (!accessor.isValid()) {
            if (BasicOAuth2Request.LOG.isLoggable()) {
              BasicOAuth2Request.LOG.log("accesssor is not valid", accessor);
            }
          } else if (accessor.isErrorResponse()) {
            if (BasicOAuth2Request.LOG.isLoggable()) {
              BasicOAuth2Request.LOG.log("accessor isErrorResponse",
                      accessor.getErrorContextMessage());
            }
          }
          this.store.storeOAuth2Accessor(accessor);
        }
      }
    }

    if (isLogging) {
      BasicOAuth2Request.LOG.exiting(BasicOAuth2Request.LOG_CLASS, "fetch", response);
    }

    return response;
  }

  private HttpResponseBuilder attemptFetch(final OAuth2Accessor accessor) {
    final boolean isLogging = BasicOAuth2Request.LOG.isLoggable();
    if (isLogging) {
      BasicOAuth2Request.LOG.entering(BasicOAuth2Request.LOG_CLASS, "attemptFetch",
              new Object[] { accessor });
      BasicOAuth2Request.LOG.log("BasicOAuth2Request.haveAccessToken(accessor) = {0}",
              BasicOAuth2Request.haveAccessToken(accessor) != null);
      BasicOAuth2Request.LOG.log("BasicOAuth2Request.haveRefreshToken(accessor) = {0}",
              BasicOAuth2Request.haveRefreshToken(accessor) != null);
    }

    if (this.attemptCounter > BasicOAuth2Request.MAX_ATTEMPTS) {
      if (isLogging) {
        BasicOAuth2Request.LOG.log("MAX_ATTEMPTS exceeded {0}", this.attemptCounter);
        // This can be useful to diagnose the recursion
        final StackTraceElement[] stackElements = Thread.currentThread().getStackTrace();
        String stack = "";
        for (final StackTraceElement element : stackElements) {
          stack = stack + element.toString() + "\n";
        }
        BasicOAuth2Request.LOG.log("MAX_ATTEMPTS stack = {0}", stack);
      }
      return this.fetchData(accessor, true);
    }

    this.attemptCounter++;

    if (isLogging) {
      BasicOAuth2Request.LOG.log("attempt number {0}", this.attemptCounter);
    }

    HttpResponseBuilder ret = null;

    if (accessor.isErrorResponse()) {
      // If there's an error in the accessor don't continue.
      return this.getErrorResponseBuilder(accessor.getErrorException(), accessor.getError(),
              accessor.getErrorContextMessage(), accessor.getErrorUri(), accessor.getErrorContextMessage());
    } else {
      if (BasicOAuth2Request.haveAccessToken(accessor) != null) {
        // We have an access_token, use it and stop!
        // Don't try more than three times
        ret = this.fetchData(accessor, this.attemptCounter > BasicOAuth2Request.MAX_ATTEMPTS);
      } else {
        // We don't have an access token, we need to try and get one.
        // First step see if we have a refresh token
        if (BasicOAuth2Request.haveRefreshToken(accessor) != null) {
          if (BasicOAuth2Request.checkCanRefresh()) {
            boolean attempt = false;
            final String internedAccessor = getAccessorKey(accessor).intern();
            if (isLogging) {
              BasicOAuth2Request.LOG.log("about to synchronize on {0}", internedAccessor);
            }
            // This syncrhonized block is less than ideal.
            // It is needed because if a gadget has multiple makeRequests that triggers
            // multiple refreshes they can end up clobbering each other, and cause
            // temporary failures until the gadget is refreshed.
            // Syncrhonizing on the internedAccessor helps.  It is not cluster safe
            // and could be problematic having so much code synchd.
            // TODO : https://issues.apache.org/jira/browse/SHINDIG-1871
            synchronized (internedAccessor) {
              final OAuth2Accessor acc = this.getAccessorInternal();
              if (isLogging) {
                BasicOAuth2Request.LOG.log("acc = {0}", acc);
                BasicOAuth2Request.LOG.log("BasicOAuth2Request.haveAccessToken(acc) = {0}",
                        BasicOAuth2Request.haveAccessToken(acc) == null);
                BasicOAuth2Request.LOG.log("BasicOAuth2Request.haveRefreshToken(acc) = {0}",
                        BasicOAuth2Request.haveRefreshToken(acc) == null);
              }
              if (BasicOAuth2Request.haveAccessToken(acc) != null) {
                // Another refresh must have won
                if (isLogging) {
                  BasicOAuth2Request.LOG.log("found an access token from another refresh",
                          new Object[] {});
                }
                attempt = true;
              } else {
                final OAuth2HandlerError handlerError = this.refreshToken(accessor);
                if (handlerError == null) {
                  // No errors refreshing, attempt the fetch again.
                  attempt = true;
                  if (isLogging) {
                    BasicOAuth2Request.LOG.log("no refresh errors reported", new Object[] {});
                  }
                } else {
                  if (isLogging) {
                    BasicOAuth2Request.LOG.log("refresh errors reported", new Object[] {});
                  }
                  // There was an error refreshing, stop.
                  final OAuth2Error error = handlerError.getError();
                  ret = this.getErrorResponseBuilder(handlerError.getCause(), error,
                          handlerError.getContextMessage(), handlerError.getUri(),
                          handlerError.getDescription());
                }
              }
            }
            if (attempt) {
              if (isLogging) {
                BasicOAuth2Request.LOG.log("going to re-attempt with a clean accesor",
                        new Object[] {});
              }
              this.store.removeOAuth2Accessor(this.internalAccessor);
              this.internalAccessor = null;
              ret = this.attemptFetch(this.getAccessor());
            }
          } else {
            // User cannot refresh, they'll have to try to authorize again.
            accessor.setAccessToken(null);
            accessor.setRefreshToken(null);
            ret = this.attemptFetch(accessor);
          }
        } else {
          // We have no access token and no refresh token.
          // User needs to authorize again.
          if (!accessor.isRedirecting() && this.checkCanAuthorize(accessor)) {
            final String completeAuthUrl = this.authorize(accessor);
            if (completeAuthUrl != null) {
              // Send a response to redirect to the authorization url
              this.responseParams.setAuthorizationUrl(completeAuthUrl);
              accessor.setRedirecting(true);
            } else {
              // This wasn't a redirect type of authorization. try again
              ret = this.attemptFetch(accessor);
            }
          }
        }
      }

      if (ret == null) {
        if (accessor.isRedirecting()) {
          // Send redirect response to client
          ret = new HttpResponseBuilder().setHttpStatusCode(HttpResponse.SC_OK).setStrictNoCache();
        } else {
          accessor.setAccessToken(null);
          ret = this.attemptFetch(accessor);
        }
      }
    }

    if (isLogging) {
      BasicOAuth2Request.LOG.exiting(BasicOAuth2Request.LOG_CLASS, "attemptFetch", ret);
    }

    return ret;
  }

  private String authorize(final OAuth2Accessor accessor) {
    final boolean isLogging = BasicOAuth2Request.LOG.isLoggable();
    if (isLogging) {
      BasicOAuth2Request.LOG.entering(BasicOAuth2Request.LOG_CLASS, "authorize", accessor);
    }

    String ret = null;

    final String grantType = accessor.getGrantType();

    GrantRequestHandler grantRequestHandlerUsed = null;
    for (final GrantRequestHandler grantRequestHandler : this.grantRequestHandlers) {
      if (grantRequestHandler.getGrantType().equalsIgnoreCase(grantType)) {
        grantRequestHandlerUsed = grantRequestHandler;
        break;
      }
    }

    if (grantRequestHandlerUsed == null) {
      accessor.setErrorResponse(null, OAuth2Error.AUTHENTICATION_PROBLEM,
              "no grantRequestHandler found for " + grantType, "");
    } else {
      String completeAuthUrl = null;
      try {
        completeAuthUrl = grantRequestHandlerUsed.getCompleteUrl(accessor);
      } catch (final OAuth2RequestException e) {
        if (isLogging) {
          BasicOAuth2Request.LOG.log("error getting complete url", e);
        }
      }
      if (grantRequestHandlerUsed.isRedirectRequired()) {
        ret = completeAuthUrl;
      } else {
        final OAuth2HandlerError error = this.authorize(accessor, grantRequestHandlerUsed,
                completeAuthUrl);
        if (error != null) {
          accessor.setErrorResponse(error.getCause(), OAuth2Error.AUTHENTICATION_PROBLEM,
                  error.getContextMessage() + " , " + error.getDescription(), error.getUri());
        }
      }
    }

    if (isLogging) {
      BasicOAuth2Request.LOG.exiting(BasicOAuth2Request.LOG_CLASS, "authorize", ret);
    }

    return ret;
  }

  private OAuth2HandlerError authorize(final OAuth2Accessor accessor,
          final GrantRequestHandler grantRequestHandler, final String completeAuthUrl) {

    final boolean isLogging = BasicOAuth2Request.LOG.isLoggable();
    if (isLogging) {
      BasicOAuth2Request.LOG.entering(BasicOAuth2Request.LOG_CLASS, "authorize", new Object[] {
              accessor, grantRequestHandler, completeAuthUrl });
    }

    OAuth2HandlerError ret = null;

    HttpRequest authorizationRequest;
    try {
      authorizationRequest = grantRequestHandler.getAuthorizationRequest(accessor, completeAuthUrl);
    } catch (final OAuth2RequestException e) {
      authorizationRequest = null;
      ret = new OAuth2HandlerError(e.getError(), e.getErrorText(), e);
    }

    if (isLogging) {
      BasicOAuth2Request.LOG.log("authorizationRequest = {0}", authorizationRequest);
    }

    if (authorizationRequest != null) {
      HttpResponse authorizationResponse;
      try {
        authorizationResponse = this.fetcher.fetch(authorizationRequest);
      } catch (final GadgetException e) {
        if (isLogging) {
          BasicOAuth2Request.LOG.log("authorize()", e);
        }
        authorizationResponse = null;
        ret = new OAuth2HandlerError(OAuth2Error.AUTHORIZE_PROBLEM,
                "exception thrown fetching authorization", e);
      }

      if (isLogging) {
        BasicOAuth2Request.LOG.log("authorizationResponse = {0}", authorizationResponse);
      }

      if (authorizationResponse != null) {
        if (grantRequestHandler.isAuthorizationEndpointResponse()) {
          for (final AuthorizationEndpointResponseHandler authorizationEndpointResponseHandler : this.authorizationEndpointResponseHandlers) {
            if (authorizationEndpointResponseHandler.handlesResponse(accessor,
                    authorizationResponse)) {
              if (isLogging) {
                BasicOAuth2Request.LOG.log("using AuthorizationEndpointResponseHandler = {0}",
                        authorizationEndpointResponseHandler);
              }
              ret = authorizationEndpointResponseHandler.handleResponse(accessor,
                      authorizationResponse);
              if (ret != null) {
                // error occurred stop processing
                break;
              }
            }
          }
        }

        if (ret == null) {
          if (grantRequestHandler.isTokenEndpointResponse()) {
            for (final TokenEndpointResponseHandler tokenEndpointResponseHandler : this.tokenEndpointResponseHandlers) {
              if (tokenEndpointResponseHandler.handlesResponse(accessor, authorizationResponse)) {
                if (isLogging) {
                  BasicOAuth2Request.LOG.log("using TokenEndpointResponseHandler = {0}",
                          tokenEndpointResponseHandler);
                }
                ret = tokenEndpointResponseHandler.handleResponse(accessor, authorizationResponse);
                if (ret != null) {
                  // error occurred stop processing
                  break;
                }
              }
            }
          }
        }
      }
    }

    if (isLogging) {
      BasicOAuth2Request.LOG.exiting(BasicOAuth2Request.LOG_CLASS, "authorize", ret);
    }

    return ret;
  }

  private static String buildRefreshTokenUrl(final OAuth2Accessor accessor) {
    final boolean isLogging = BasicOAuth2Request.LOG.isLoggable();
    if (isLogging) {
      BasicOAuth2Request.LOG.entering(BasicOAuth2Request.LOG_CLASS, "buildRefreshTokenUrl",
              accessor);
    }

    String ret = null;

    final String refreshUrl = accessor.getTokenUrl();
    if (refreshUrl != null) {
      ret = BasicOAuth2Request.getCompleteRefreshUrl(refreshUrl);
    }

    if (isLogging) {
      BasicOAuth2Request.LOG.exiting(BasicOAuth2Request.LOG_CLASS, "buildRefreshTokenUrl", ret);
    }

    return ret;
  }

  private boolean checkCanAuthorize(final OAuth2Accessor accessor) {
    final boolean isLogging = BasicOAuth2Request.LOG.isLoggable();
    if (isLogging) {
      BasicOAuth2Request.LOG.entering(BasicOAuth2Request.LOG_CLASS, "checkCanAuthorize", accessor);
    }

    boolean ret = true;

    if (BasicOAuth2Request.LOG.isLoggable()) {
      BasicOAuth2Request.LOG.log("securityToken = {0}", this.securityToken);
    }

    final String pageOwner = this.securityToken.getOwnerId();
    final String pageViewer = this.securityToken.getViewerId();

    if (BasicOAuth2Request.LOG.isLoggable()) {
      BasicOAuth2Request.LOG.log("pageOwner = {0}", pageOwner);
      BasicOAuth2Request.LOG.log("pageViewer = {0}", pageViewer);
    }

    if (pageOwner == null || pageViewer == null) {
      accessor.setErrorResponse(null, OAuth2Error.AUTHORIZE_PROBLEM,
              "pageOwner or pageViewer is null", "");
      ret = false;
    } else if (!this.fetcherConfig.isViewerAccessTokensEnabled() && !pageOwner.equals(pageViewer)) {
      accessor.setErrorResponse(null, OAuth2Error.AUTHORIZE_PROBLEM, "pageViewer is not pageOwner",
              "");
      ret = false;
    }

    if (isLogging) {
      BasicOAuth2Request.LOG.exiting(BasicOAuth2Request.LOG_CLASS, "checkCanAuthorize", ret);
    }

    return ret;
  }

  private static boolean checkCanRefresh() {
    // Everyone can try to refresh???
    return true;
  }

  private HttpResponseBuilder fetchData(final OAuth2Accessor accessor, final boolean lastAttempt) {
    final boolean isLogging = BasicOAuth2Request.LOG.isLoggable();
    if (isLogging) {
      BasicOAuth2Request.LOG.entering(BasicOAuth2Request.LOG_CLASS, "fetchData", accessor);
    }

    HttpResponseBuilder ret = null;

    try {
      final HttpResponse response = this.fetchFromServer(accessor, this.realRequest, lastAttempt);
      if (response != null) {
        ret = new HttpResponseBuilder(response);

        if (response.getHttpStatusCode() != HttpResponse.SC_OK && this.sendTraceToClient) {
          this.responseParams.addRequestTrace(this.realRequest, response);
        }
      }
    } catch (final OAuth2RequestException e) {
      ret = this.getErrorResponseBuilder(e, e.getError(), e.getErrorText(), e.getErrorUri(),
              e.getErrorDescription());
    }

    if (isLogging) {
      BasicOAuth2Request.LOG.exiting(BasicOAuth2Request.LOG_CLASS, "fetchData", ret);
    }

    return ret;
  }

  private HttpResponse fetchFromServer(final OAuth2Accessor accessor, final HttpRequest request,
          final boolean lastAttempt) throws OAuth2RequestException {
    final boolean isLogging = BasicOAuth2Request.LOG.isLoggable();
    if (isLogging) {
      BasicOAuth2Request.LOG.entering(BasicOAuth2Request.LOG_CLASS, "fetchFromServer",
              new Object[] { accessor, "only log request once", lastAttempt });
    }

    HttpResponse ret;

    final long currentTime = System.currentTimeMillis();

    OAuth2Token accessToken = accessor.getAccessToken();
    if (accessToken != null) {
      final long expiresAt = accessToken.getExpiresAt();
      if (expiresAt != 0) {
        if (currentTime >= expiresAt) {
          if (BasicOAuth2Request.LOG.isLoggable()) {
            BasicOAuth2Request.LOG.log("accessToken has expired at {0}", expiresAt);
          }
          try {
            this.store.removeToken(accessToken);
          } catch (final GadgetException e) {
            throw new OAuth2RequestException(OAuth2Error.MISSING_SERVER_RESPONSE,
                    "error removing access_token", null);
          }
          accessToken = null;
          accessor.setAccessToken(null);
          if (!lastAttempt) {
            return null;
          }
        }
      }
    }

    OAuth2Token refreshToken = accessor.getRefreshToken();
    if (refreshToken != null) {
      final long expiresAt = refreshToken.getExpiresAt();
      if (expiresAt != 0) {
        if (currentTime >= expiresAt) {
          if (BasicOAuth2Request.LOG.isLoggable()) {
            BasicOAuth2Request.LOG.log("refreshToken has expired at {0}", expiresAt);
          }
          try {
            this.store.removeToken(refreshToken);
          } catch (final GadgetException e) {
            throw new OAuth2RequestException(OAuth2Error.MISSING_SERVER_RESPONSE,
                    "error removing refresh_token", null);
          }
          refreshToken = null;
          accessor.setRefreshToken(null);
          if (!lastAttempt) {
            return null;
          }
        }
      }
    }

    if (BasicOAuth2Request.LOG.isLoggable()) {
      BasicOAuth2Request.LOG.log("accessToken = {0}", accessToken);
      BasicOAuth2Request.LOG.log("refreshToken = {0}", refreshToken);
    }

    if (accessToken != null) {
      final boolean isAllowed = OAuth2Utils.isUriAllowed(request.getUri(), accessor.getAllowedDomains());
      if (isAllowed) {
        String tokenType = accessToken.getTokenType();
        if (tokenType == null || tokenType.length() == 0) {
          tokenType = OAuth2Message.BEARER_TOKEN_TYPE;
        }

        for (final ResourceRequestHandler resourceRequestHandler : this.resourceRequestHandlers) {
          if (tokenType.equalsIgnoreCase(resourceRequestHandler.getTokenType())) {
            resourceRequestHandler.addOAuth2Params(accessor, request);
          }
        }
      } else {
        BasicOAuth2Request.LOG.log(Level.WARNING,
                "Gadget {0} attempted to send OAuth2 Token to an unauthorized domain: {1}.",
                new Object[] { accessor.getGadgetUri(), request.getUri() });
      }
    }

    try {
      ret = this.fetcher.fetch(request);
    } catch (final GadgetException e) {
      throw new OAuth2RequestException(OAuth2Error.MISSING_SERVER_RESPONSE,
              "GadgetException fetchFromServer", e);
    }

    if (ret == null) {
      throw new OAuth2RequestException(OAuth2Error.MISSING_SERVER_RESPONSE, "response is null",
              null);
    }

    final int responseCode = ret.getHttpStatusCode();

    if (isLogging) {
      BasicOAuth2Request.LOG.log("responseCode = {0}", responseCode);
    }

    if (responseCode == HttpResponse.SC_UNAUTHORIZED) {
      if (accessToken != null) {
        try {
          this.store.removeToken(accessToken);
        } catch (final GadgetException e) {
          throw new OAuth2RequestException(OAuth2Error.MISSING_SERVER_RESPONSE,
                  "error removing access_token", null);
        }
        accessor.setAccessToken(null);
      }

      if (!lastAttempt) {
        ret = null;
      }
    }

    if (isLogging) {
      BasicOAuth2Request.LOG.exiting(BasicOAuth2Request.LOG_CLASS, "fetchFromServer", ret);
    }

    return ret;
  }

  private OAuth2Accessor getAccessorInternal() {
    OAuth2Accessor ret = null;
    if (this.fetcherConfig != null) {
      final GadgetOAuth2TokenStore tokenStore = this.fetcherConfig.getTokenStore();
      if (tokenStore != null) {
        ret = tokenStore.getOAuth2Accessor(this.securityToken, this.arguments,
                this.realRequest.getGadget());
      }
    }
    return ret;
  }

  private OAuth2Accessor getAccessor() {
    if (this.internalAccessor == null || !this.internalAccessor.isValid()) {
      this.internalAccessor = this.getAccessorInternal();
    }

    return this.internalAccessor;
  }

  private static String getCompleteRefreshUrl(final String refreshUrl) {
    return OAuth2Utils.buildUrl(refreshUrl, null, null);
  }

  private HttpResponseBuilder getErrorResponseBuilder(final Throwable t, final OAuth2Error error,
          final String contextMessage) {
    return this.getErrorResponseBuilder(t, error, contextMessage, null, null);
  }

  private HttpResponseBuilder getErrorResponseBuilder(final Throwable t, final OAuth2Error error,
          final String contextMessage, final String errorUri, final String errorDescription) {

    final boolean isLogging = BasicOAuth2Request.LOG.isLoggable();
    if (isLogging) {
      BasicOAuth2Request.LOG.entering(BasicOAuth2Request.LOG_CLASS, "getErrorResponseBuilder",
              new Object[] { t, error, contextMessage, errorUri, errorDescription });
    }

    final HttpResponseBuilder ret = new HttpResponseBuilder().setHttpStatusCode(
            HttpResponse.SC_FORBIDDEN).setStrictNoCache();

    if (t != null && this.sendTraceToClient) {
      final StringWriter sw = new StringWriter();
      t.printStackTrace(new PrintWriter(sw));
      final String message = sw.toString();
      this.responseParams.addDebug(message);
    }

    if (this.sendTraceToClient) {
      this.responseParams.addToResponse(ret, error.getErrorCode(),
              error.getErrorDescription(contextMessage) + " , " + errorDescription, errorUri,
              error.getErrorExplanation());
    } else {
      this.responseParams.addToResponse(ret, error.getErrorCode(), "", "",
              error.getErrorExplanation());
    }

    if (isLogging) {
      BasicOAuth2Request.LOG.exiting(BasicOAuth2Request.LOG_CLASS, "getErrorResponseBuilder", ret);
    }

    return ret;
  }

  private static String getRefreshBody(final OAuth2Accessor accessor) {
    final boolean isLogging = BasicOAuth2Request.LOG.isLoggable();
    if (isLogging) {
      BasicOAuth2Request.LOG.entering(BasicOAuth2Request.LOG_CLASS, "getRefreshBody", accessor);
    }

    String ret = "";

    Map<String, String> queryParams;
    try {
      queryParams = Maps.newHashMap();
      queryParams.put(OAuth2Message.GRANT_TYPE, OAuth2Message.REFRESH_TOKEN);
      queryParams.put(OAuth2Message.REFRESH_TOKEN, new String(accessor.getRefreshToken()
              .getSecret(), "UTF-8"));
      if (accessor.getScope() != null && accessor.getScope().length() > 0) {
        queryParams.put(OAuth2Message.SCOPE, accessor.getScope());
      }

      final String clientId = accessor.getClientId();
      final byte[] secret = accessor.getClientSecret();
      queryParams.put(OAuth2Message.CLIENT_ID, clientId);
      queryParams.put(OAuth2Message.CLIENT_SECRET, new String(secret, "UTF-8"));

      ret = OAuth2Utils.buildUrl(ret, queryParams, null);

      final char firstChar = ret.charAt(0);
      if (firstChar == '?' || firstChar == '&') {
        ret = ret.substring(1);
      }

      if (isLogging) {
        BasicOAuth2Request.LOG.exiting(BasicOAuth2Request.LOG_CLASS, "getRefreshBody", ret);
      }
    } catch (final UnsupportedEncodingException e) {
      if (isLogging) {
        BasicOAuth2Request.LOG.log("error generating refresh body", e);
        ret = null;
      }
    }

    return ret;
  }

  private HttpResponse processResponse(final OAuth2Accessor accessor,
          final HttpResponseBuilder responseBuilder) {

    final boolean isLogging = BasicOAuth2Request.LOG.isLoggable();
    if (isLogging) {
      BasicOAuth2Request.LOG.entering(BasicOAuth2Request.LOG_CLASS, "processResponse",
              new Object[] { accessor, responseBuilder == null });
    }

    if (accessor.isErrorResponse() || responseBuilder == null) {
      return this.sendErrorResponse(accessor.getErrorException(), accessor.getError(),
              accessor.getErrorContextMessage(), accessor.getErrorUri(), "");
    }

    if (this.responseParams.getAuthorizationUrl() != null) {
      responseBuilder.setMetadata(OAuth2ResponseParams.APPROVAL_URL,
              this.responseParams.getAuthorizationUrl());
      accessor.setRedirecting(true);
    } else {
      accessor.setRedirecting(false);
    }

    final HttpResponse ret = responseBuilder.create();
    if (isLogging) {
      BasicOAuth2Request.LOG.exiting(BasicOAuth2Request.LOG_CLASS, "processResponse",
              "response logged in fetch()");
    }

    return ret;
  }

  private OAuth2HandlerError refreshToken(final OAuth2Accessor accessor) {
    final boolean isLogging = BasicOAuth2Request.LOG.isLoggable();
    if (isLogging) {
      BasicOAuth2Request.LOG.entering(BasicOAuth2Request.LOG_CLASS, "refreshToken",
              new Object[] { accessor });
    }

    OAuth2HandlerError ret = null;

    String refershTokenUrl;

    refershTokenUrl = BasicOAuth2Request.buildRefreshTokenUrl(accessor);

    if (isLogging) {
      BasicOAuth2Request.LOG.log("refershTokenUrl = {0}", refershTokenUrl);
    }

    if (refershTokenUrl != null) {
      HttpResponse response = null;
      final HttpRequest request = new HttpRequest(Uri.parse(refershTokenUrl));
      request.setSecurityToken(new AnonymousSecurityToken("", 0L, accessor.getGadgetUri()));
      request.setMethod("POST");
      request.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");

      for (final ClientAuthenticationHandler clientAuthenticationHandler : this.clientAuthenticationHandlers) {
        if (clientAuthenticationHandler.geClientAuthenticationType().equalsIgnoreCase(
                accessor.getClientAuthenticationType())) {
          clientAuthenticationHandler.addOAuth2Authentication(request, accessor);
        }
      }

      try {
        final byte[] body = BasicOAuth2Request.getRefreshBody(accessor).getBytes("UTF-8");
        request.setPostBody(body);
      } catch (final Exception e) {
        if (isLogging) {
          BasicOAuth2Request.LOG.log("refreshToken()", e);
        }
        ret = new OAuth2HandlerError(OAuth2Error.REFRESH_TOKEN_PROBLEM,
                "error generating refresh body", e);
      }

      if (!OAuth2Utils.isUriAllowed(request.getUri(), accessor.getAllowedDomains())) {
        ret = new OAuth2HandlerError(OAuth2Error.REFRESH_TOKEN_PROBLEM,
                "error fetching refresh token - domain not allowed", null);
      }

      if (ret == null) {
        try {
          response = this.fetcher.fetch(request);
        } catch (final GadgetException e) {
          if (isLogging) {
            BasicOAuth2Request.LOG.log("refreshToken()", e);
          }
          ret = new OAuth2HandlerError(OAuth2Error.REFRESH_TOKEN_PROBLEM,
                  "error fetching refresh token", e);
        }

        if (isLogging) {
          BasicOAuth2Request.LOG.log("response = {0}", response);
        }

        if (response == null) {
          ret = new OAuth2HandlerError(OAuth2Error.REFRESH_TOKEN_PROBLEM, "response is null", null);
        }

        if (ret == null) {
          // response is not null..
          final int statusCode = response.getHttpStatusCode();
          if (statusCode == HttpResponse.SC_UNAUTHORIZED
                  || statusCode == HttpResponse.SC_BAD_REQUEST) {
            try {
              this.store.removeToken(accessor.getRefreshToken());
            } catch (final GadgetException e) {
              ret = new OAuth2HandlerError(OAuth2Error.REFRESH_TOKEN_PROBLEM,
                      "failed to remove refresh token", e);
            }
            accessor.setRefreshToken(null);
            if (isLogging) {
              BasicOAuth2Request.LOG.log(Level.FINEST,
                      "received {0} from provider, removed refresh token.  response = {1}",
                      new Object[] { statusCode, response.getResponseAsString() });
            }
            return null;
          } else if (statusCode != HttpResponse.SC_OK) {
            ret = new OAuth2HandlerError(OAuth2Error.REFRESH_TOKEN_PROBLEM,
                    "bad response from server : " + statusCode, null, "",
                    response.getResponseAsString());
          }

          if (ret == null) {
            for (final TokenEndpointResponseHandler tokenEndpointResponseHandler : this.tokenEndpointResponseHandlers) {
              if (tokenEndpointResponseHandler.handlesResponse(accessor, response)) {
                final OAuth2HandlerError error = tokenEndpointResponseHandler.handleResponse(
                        accessor, response);
                if (error != null) {
                  try {
                    this.store.removeToken(accessor.getRefreshToken());
                  } catch (final GadgetException e) {
                    ret = new OAuth2HandlerError(OAuth2Error.REFRESH_TOKEN_PROBLEM,
                            error.getContextMessage(), e, error.getUri(), error.getDescription());
                  }
                  accessor.setRefreshToken(null);
                  return error;
                }
              }
            }
          }
        }
      }
    }

    if (isLogging) {
      BasicOAuth2Request.LOG.exiting(BasicOAuth2Request.LOG_CLASS, "refreshToken", ret);
    }

    return ret;
  }

  private HttpResponse sendErrorResponse(final Throwable t, final OAuth2Error error,
          final String contextMessage) {
    final HttpResponseBuilder responseBuilder = this.getErrorResponseBuilder(t, error,
            contextMessage);
    return responseBuilder.create();
  }

  private HttpResponse sendErrorResponse(final Throwable t, final OAuth2Error error,
          final String contextMessage, final String errorUri, final String errorDescription) {
    final HttpResponseBuilder responseBuilder = this.getErrorResponseBuilder(t, error,
            contextMessage, errorUri, errorDescription);
    return responseBuilder.create();
  }

  private static OAuth2Token haveAccessToken(final OAuth2Accessor accessor) {
    OAuth2Token ret = accessor.getAccessToken();
    if (ret != null) {
      if (!BasicOAuth2Request.validateAccessToken(ret)) {
        ret = null;
      }
    }
    return ret;
  }

  private static OAuth2Token haveRefreshToken(final OAuth2Accessor accessor) {
    OAuth2Token ret = accessor.getRefreshToken();
    if (ret != null) {
      if (!BasicOAuth2Request.validateRefreshToken(ret)) {
        ret = null;
      }
    }
    return ret;
  }

  private static boolean validateAccessToken(final OAuth2Token accessToken) {
    return accessToken != null;
  }

  private static boolean validateRefreshToken(final OAuth2Token refreshToken) {
    return refreshToken != null;
  }

  private static String getAccessorKey(final OAuth2Accessor accessor) {
    if (accessor != null) {
      return "accessor:" + accessor.getGadgetUri() + ':' + accessor.getServiceName() + ':'
              + accessor.getUser() + ':' + accessor.getScope();
    }

    return null;
  }
}
