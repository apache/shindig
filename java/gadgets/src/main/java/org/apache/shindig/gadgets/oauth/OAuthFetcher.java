/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.shindig.gadgets.oauth;

import org.apache.shindig.common.SecurityToken;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.ChainedContentFetcher;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpCacheKey;
import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthException;
import net.oauth.OAuthMessage;
import net.oauth.OAuthProblemException;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Implements the OAuth dance (http://oauth.net/core/1.0/) for gadgets.
 *
 * Reading the example in the appendix to the OAuth spec will be helpful to
 * those reading this code.
 *
 * This class is not thread-safe; create a new one for each request that
 * requires OAuth signing.
 */
public class OAuthFetcher extends ChainedContentFetcher {

  // Maximum number of attempts at the protocol before giving up.
  private static final int MAX_ATTEMPTS = 2;

  // names of additional OAuth parameters we include in outgoing requests
  public static final String XOAUTH_APP_URL = "xoauth_app_url";

  /**
   * Per request configuration for this OAuth fetch.
   */
  protected final OAuthFetchParams fetchParams;

  /**
   * Configuration options for the fetcher.
   */
  protected final OAuthFetcherConfig fetcherConfig;

  /**
   * OAuth specific stuff to include in the response.
   */
  protected final OAuthResponseParams responseParams;

  /**
   * The accessor we use for signing messages. This also holds metadata about
   * the service provider, such as their URLs and the keys we use to access
   * those URLs.
   */
  private OAuthStore.AccessorInfo accessorInfo;

  /**
   * The request the client really wants to make.
   */
  private HttpRequest realRequest;

  /**
   * @param fetcherConfig configuration options for the fetcher
   * @param nextFetcher fetcher to use for actually making requests
   * @param authToken user's gadget security token
   * @param params OAuth fetch parameters sent from makeRequest
   */
  public OAuthFetcher(
      OAuthFetcherConfig fetcherConfig,
      HttpFetcher nextFetcher,
      SecurityToken authToken,
      OAuthArguments params) {
    super(nextFetcher);
    this.fetcherConfig = fetcherConfig;
    this.fetchParams = new OAuthFetchParams(
        params,
        new OAuthClientState(fetcherConfig.getStateCrypter(), params.getOrigClientState()),
        authToken);
    this.responseParams = new OAuthResponseParams(fetcherConfig.getStateCrypter());
  }

  /**
   * Retrieves metadata from our persistent store.
   *
   * TODO(beaton): can we fix this so it avoids hitting the persistent data
   * store when a client makes multiple requests with an approved access token?
   *
   * @throws GadgetException
   */
  private void lookupOAuthMetadata() throws GadgetException {
    OAuthStore.TokenKey tokenKey = buildTokenKey();
    accessorInfo = fetcherConfig.getTokenStore().getOAuthAccessor(
        tokenKey, fetchParams.getArguments().getBypassSpecCache());

    // The persistent data store may be out of sync with reality; we trust
    // the state we stored on the client to be accurate.
    OAuthAccessor accessor = accessorInfo.getAccessor();
    if (fetchParams.getClientState().getRequestToken() != null) {
      accessor.requestToken = fetchParams.getClientState().getRequestToken();
      accessor.tokenSecret = fetchParams.getClientState().getRequestTokenSecret();
    } else if (fetchParams.getClientState().getAccessToken() != null) {
      accessor.accessToken = fetchParams.getClientState().getAccessToken();
      accessor.tokenSecret = fetchParams.getClientState().getAccessTokenSecret();
    } else if (accessor.accessToken == null &&
               fetchParams.getArguments().getRequestToken() != null) {
      // We don't have an access token yet, but the client sent us a
      // (hopefully) preapproved request token.
      accessor.requestToken = fetchParams.getArguments().getRequestToken();
      accessor.tokenSecret = fetchParams.getArguments().getRequestTokenSecret();
    }
  }

  private OAuthStore.TokenKey buildTokenKey() {
    OAuthStore.TokenKey tokenKey = new OAuthStore.TokenKey();
    tokenKey.setGadgetUri(fetchParams.getAuthToken().getAppUrl());
    tokenKey.setModuleId(fetchParams.getAuthToken().getModuleId());
    tokenKey.setServiceName(fetchParams.getArguments().getServiceName());
    tokenKey.setTokenName(fetchParams.getArguments().getTokenName());
    // At some point we might want to let gadgets specify whether to use OAuth
    // for the owner, the viewer, or someone else. For now always using the
    // owner identity seems reasonable.
    tokenKey.setUserId(fetchParams.getAuthToken().getOwnerId());
    return tokenKey;
  }

  public HttpResponse fetch(HttpRequest request) throws GadgetException {
    HttpCacheKey cacheKey = makeCacheKey(request);
    HttpResponse response = fetcherConfig.getHttpCache().getResponse(cacheKey, request);
    if (response != null) {
      return response;
    }

    try {
      lookupOAuthMetadata();
    } catch (GadgetException e) {
      responseParams.setError(OAuthError.BAD_OAUTH_CONFIGURATION);
      return buildErrorResponse(e);
    }

    this.realRequest = request;

    int attempts = 0;
    boolean retry;
    do {
      retry = false;
      ++attempts;
      try {
        response = attemptFetch();
      } catch (OAuthProtocolException pe) {
        retry = handleProtocolException(pe, attempts);
        if (!retry) {
          response = pe.getResponseForGadget();
        }
      }
    } while (retry);

    if (response == null) {
      throw new GadgetException(
          GadgetException.Code.INTERNAL_SERVER_ERROR,
          "No response for OAuth fetch to " + realRequest.getUri());
    }
    return fetcherConfig.getHttpCache().addResponse(cacheKey, request, response);
  }

  // Builds up a cache key based on the same key that we use into the OAuth
  // token storage, which should identify precisely which data to return for the
  // response.  Using the OAuth access token as the cache key is another
  // possibility.
  private HttpCacheKey makeCacheKey(HttpRequest request) {
    HttpCacheKey key = new HttpCacheKey(request);
    key.set("authentication", "oauth");
    OAuthStore.TokenKey tokenKey = buildTokenKey();
    key.set("user", tokenKey.getUserId());
    key.set("gadget", tokenKey.getGadgetUri());
    key.set("instance", Long.toString(tokenKey.getModuleId()));
    key.set("service", tokenKey.getServiceName());
    key.set("token", tokenKey.getTokenName());
    return key;
  }

  private HttpResponse buildErrorResponse(GadgetException e) {
    if (responseParams.getError() == null) {
      responseParams.setError(OAuthError.UNKNOWN_PROBLEM);
    }
    // Take a giant leap of faith and assume that the exception message
    // will be useful to a gadget developer.  Also include the exception
    // stack trace, in case the problem report makes it to someone who knows
    // enough to do something useful with the stack.
    // TODO(beaton): This seemed like a good idea at the time, but dumping an entire stack trace to
    // the client is a little much.  Remove this.
    StringWriter errorBuf = new StringWriter();
    errorBuf.append(e.getMessage());
    errorBuf.append("\n\n");
    PrintWriter printer = new PrintWriter(errorBuf);
    e.printStackTrace(printer);
    printer.flush();
    responseParams.setErrorText(errorBuf.toString());
    return buildNonDataResponse(403);
  }

  private boolean handleProtocolException(
      OAuthProtocolException pe, int attempts) throws OAuthStoreException {
    if (pe.startFromScratch()) {
      OAuthStore.TokenKey tokenKey = buildTokenKey();
      fetcherConfig.getTokenStore().removeToken(tokenKey);

      accessorInfo.accessor.accessToken = null;
      accessorInfo.accessor.requestToken = null;
      accessorInfo.accessor.tokenSecret = null;
    }
    return (attempts < MAX_ATTEMPTS && pe.canRetry());
  }

  private HttpResponse attemptFetch()
      throws GadgetException, OAuthProtocolException {
    if (needApproval()) {
      // This is section 6.1 of the OAuth spec.
      checkCanApprove();
      fetchRequestToken();
      // This is section 6.2 of the OAuth spec.
      buildClientApprovalState();
      buildAznUrl();
      // break out of the content fetching chain, we need permission from
      // the user to do this
      return buildOAuthApprovalResponse();
    } else if (needAccessToken()) {
      // This is section 6.3 of the OAuth spec
      checkCanApprove();
      exchangeRequestToken();
      saveAccessToken();
      buildClientAccessState();
    }
    return fetchData();
  }

  /**
   * Do we need to get the user's approval to access the data?
   */
  private boolean needApproval() {
    return (accessorInfo.getAccessor().requestToken == null
            && accessorInfo.getAccessor().accessToken == null);
  }

  /**
   * Make sure the user is authorized to approve access tokens.  At the moment
   * we restrict this to page owner's viewing their own pages.
   *
   * @throws GadgetException
   */
  private void checkCanApprove() throws GadgetException {
    String pageOwner = fetchParams.getAuthToken().getOwnerId();
    String pageViewer = fetchParams.getAuthToken().getViewerId();
    String stateOwner = fetchParams.getClientState().getOwner();
    if (!pageOwner.equals(pageViewer)) {
      throw new GadgetException(GadgetException.Code.INTERNAL_SERVER_ERROR,
          "Only page owners can grant OAuth approval");
    }
    if (stateOwner != null && !stateOwner.equals(pageOwner)) {
      throw new GadgetException(GadgetException.Code.INTERNAL_SERVER_ERROR,
          "Client state belongs to a different person.");
    }
  }

  private void fetchRequestToken()
      throws GadgetException, OAuthProtocolException {
    try {
      OAuthAccessor accessor = accessorInfo.getAccessor();
      String url = accessor.consumer.serviceProvider.requestTokenURL;
      List<OAuth.Parameter> msgParams = new ArrayList<OAuth.Parameter>();
      msgParams.add(new OAuth.Parameter(XOAUTH_APP_URL, fetchParams.getAuthToken().getAppUrl()));
      OAuthMessage request = newRequestMessage(url, msgParams);
      OAuthMessage reply = sendOAuthMessage(request);
      reply.requireParameters(OAuth.OAUTH_TOKEN, OAuth.OAUTH_TOKEN_SECRET);
      accessor.requestToken = reply.getParameter(OAuth.OAUTH_TOKEN);
      accessor.tokenSecret = reply.getParameter(OAuth.OAUTH_TOKEN_SECRET);
    } catch (OAuthException e) {
      throw new GadgetException(GadgetException.Code.INTERNAL_SERVER_ERROR, e);
    } catch (IOException e) {
      throw new GadgetException(GadgetException.Code.INTERNAL_SERVER_ERROR, e);
    } catch (URISyntaxException e) {
      throw new GadgetException(GadgetException.Code.INTERNAL_SERVER_ERROR, e);
    }
  }

  private OAuthMessage newRequestMessage(String method,
      String url, List<OAuth.Parameter> params)
      throws OAuthException, IOException, URISyntaxException {

    if (params == null) {
      throw new IllegalArgumentException("params was null in " +
          "newRequestMessage(String, String, List<OAuth.Parameter> " +
          "Use newRequesMessage(String) if you don't have a params to pass");
    }

    switch (accessorInfo.signatureType) {
      case RSA_SHA1:
        params.add(new OAuth.Parameter(OAuth.OAUTH_SIGNATURE_METHOD,
                                       OAuth.RSA_SHA1));
        break;

      case PLAINTEXT:
        params.add(new OAuth.Parameter(OAuth.OAUTH_SIGNATURE_METHOD,
                                       "PLAINTEXT"));
        break;

      default:
        params.add(new OAuth.Parameter(OAuth.OAUTH_SIGNATURE_METHOD,
                                       OAuth.HMAC_SHA1));
    }

    OAuthAccessor accessor = accessorInfo.getAccessor();

    return accessor.newRequestMessage(method, url, params);
  }

  private OAuthMessage newRequestMessage(String url,
      List<OAuth.Parameter> params)
      throws OAuthException, IOException, URISyntaxException {
    String method = "POST";
    if (accessorInfo.getHttpMethod() == OAuthStore.HttpMethod.GET) {
      method = "GET";
    }
    return newRequestMessage(method, url, params);
  }

  private String getAuthorizationHeader(
      List<Map.Entry<String, String>> oauthParams) {
    StringBuilder result = new StringBuilder("OAuth ");

    boolean first = true;
    for (Map.Entry<String, String> parameter : oauthParams) {
      if (!first) {
        result.append(", ");
      } else {
        first = false;
      }
      result.append(OAuth.percentEncode(parameter.getKey()))
            .append("=\"")
            .append(OAuth.percentEncode(parameter.getValue()))
            .append('"');
    }
    return result.toString();
  }

  private HttpRequest createHttpRequest(HttpRequest base,
      List<Map.Entry<String, String>> oauthParams) throws IOException, GadgetException {

    OAuthStore.OAuthParamLocation paramLocation = accessorInfo.getParamLocation();

    // paramLocation could be overriden by a run-time parameter to fetchRequest

    HttpRequest result = new HttpRequest(base);

    switch (paramLocation) {
      case AUTH_HEADER:
        result.addHeader("Authorization", getAuthorizationHeader(oauthParams));
        break;

      case POST_BODY:
        if (!OAuth.isFormEncoded(result.getContentType())) {
          throw new GadgetException(GadgetException.Code.INVALID_PARAMETER,
              "OAuth param location can only be post_body if post body if of " +
              "type x-www-form-urlencoded");
        }
        String oauthData = OAuth.formEncode(oauthParams);
        if (result.getPostBodyLength() == 0) {
          result.setPostBody(oauthData.getBytes("UTF-8"));
        } else {
          result.setPostBody((result.getPostBodyAsString() + '&' + oauthData).getBytes());
        }
        break;

      case URI_QUERY:
        result.setUri(Uri.parse(OAuth.addParameters(result.getUri().toString(), oauthParams)));
        break;
    }

    return result;
  }

  /**
   * Sends OAuth request token and access token messages.
   */
  private OAuthMessage sendOAuthMessage(OAuthMessage request)
      throws IOException, OAuthProtocolException, GadgetException {

    HttpRequest req = new HttpRequest(Uri.parse(request.URL))
        .setMethod(request.method)
        .setIgnoreCache(true);

    HttpRequest oauthRequest = createHttpRequest(req, filterOAuthParams(request));

    HttpResponse response = nextFetcher.fetch(oauthRequest);
    checkForProtocolProblem(response);
    OAuthMessage reply = new OAuthMessage(null, null, null);

    reply.addParameters(OAuth.decodeForm(response.getResponseAsString()));
    reply = parseAuthHeader(reply, response);
    return reply;
  }

  /**
   * Parse OAuth WWW-Authenticate header and either add them to an existing
   * message or create a new message.
   *
   * @param msg
   * @param resp
   * @return the updated message.
   */
  private OAuthMessage parseAuthHeader(OAuthMessage msg, HttpResponse resp) {
    if (msg == null) {
      msg = new OAuthMessage(null, null, null);
    }
    List<String> authHeaders = resp.getHeaders("WWW-Authenticate");
    if (authHeaders != null) {
      for (String auth : authHeaders) {
        msg.addParameters(OAuthMessage.decodeAuthorization(auth));
      }
    }
    return msg;
  }

  /**
   * Builds the data we'll cache on the client while we wait for approval.
   */
  private void buildClientApprovalState() {
    OAuthAccessor accessor = accessorInfo.getAccessor();
    responseParams.getNewClientState().setRequestToken(accessor.requestToken);
    responseParams.getNewClientState().setRequestTokenSecret(accessor.tokenSecret);
    responseParams.getNewClientState().setOwner(fetchParams.getAuthToken().getOwnerId());
  }

  /**
   * Builds the URL the client needs to visit to approve access.
   */
  private void buildAznUrl() {
    // At some point we can be clever and use a callback URL to improve
    // the user experience, but that's too complex for now.
    OAuthAccessor accessor = accessorInfo.getAccessor();
    StringBuilder azn = new StringBuilder(
        accessor.consumer.serviceProvider.userAuthorizationURL);
    if (azn.indexOf("?") == -1) {
      azn.append('?');
    } else {
      azn.append('&');
    }
    azn.append(OAuth.OAUTH_TOKEN);
    azn.append('=');
    azn.append(OAuth.percentEncode(accessor.requestToken));
    responseParams.setAznUrl(azn.toString());
  }

  private HttpResponse buildOAuthApprovalResponse() {
    return buildNonDataResponse(200);
  }

  private HttpResponse buildNonDataResponse(int status) {
    HttpResponse response = new HttpResponse(status, null, null);
    responseParams.addToResponse(response);
    response.setNoCache();
    return response;
  }

  /**
   * Do we need to exchange a request token for an access token?
   */
  private boolean needAccessToken() {
    return (accessorInfo.getAccessor().requestToken != null
            && accessorInfo.getAccessor().accessToken == null);
  }

  /**
   * Implements section 6.3 of the OAuth spec.
   * @throws OAuthProtocolException
   */
  private void exchangeRequestToken()
      throws GadgetException, OAuthProtocolException {
    try {
      OAuthAccessor accessor = accessorInfo.getAccessor();
      String url = accessor.consumer.serviceProvider.accessTokenURL;
      List<OAuth.Parameter> msgParams = new ArrayList<OAuth.Parameter>();
      msgParams.add(new OAuth.Parameter(XOAUTH_APP_URL, fetchParams.getAuthToken().getAppUrl()));
      msgParams.add(
          new OAuth.Parameter(OAuth.OAUTH_TOKEN, accessor.requestToken));
      OAuthMessage request = newRequestMessage(url, msgParams);
      OAuthMessage reply = sendOAuthMessage(request);
      reply.requireParameters(OAuth.OAUTH_TOKEN, OAuth.OAUTH_TOKEN_SECRET);
      accessor.accessToken = reply.getParameter(OAuth.OAUTH_TOKEN);
      accessor.tokenSecret = reply.getParameter(OAuth.OAUTH_TOKEN_SECRET);
    } catch (OAuthException e) {
      throw new GadgetException(GadgetException.Code.INTERNAL_SERVER_ERROR, e);
    } catch (IOException e) {
      throw new GadgetException(GadgetException.Code.INTERNAL_SERVER_ERROR, e);
    } catch (URISyntaxException e) {
      throw new GadgetException(GadgetException.Code.INTERNAL_SERVER_ERROR, e);
    }
  }

  /**
   * Save off our new token and secret to the persistent store.
   *
   * @throws GadgetException
   */
  private void saveAccessToken() throws GadgetException {
    OAuthAccessor accessor = accessorInfo.getAccessor();
    OAuthStore.TokenKey tokenKey = buildTokenKey();
    OAuthStore.TokenInfo tokenInfo = new OAuthStore.TokenInfo(
        accessor.accessToken, accessor.tokenSecret);
    fetcherConfig.getTokenStore().storeTokenKeyAndSecret(tokenKey, tokenInfo);
  }

  /**
   * Builds the data we'll cache on the client while we make requests.
   */
  private void buildClientAccessState() {
    OAuthAccessor accessor = accessorInfo.getAccessor();
    responseParams.getNewClientState().setAccessToken(accessor.accessToken);
    responseParams.getNewClientState().setAccessTokenSecret(accessor.tokenSecret);
    responseParams.getNewClientState().setOwner(fetchParams.getAuthToken().getOwnerId());
  }

  /**
   * Get honest-to-goodness user data.
   *
   * @throws OAuthProtocolException if the service provider returns an OAuth
   * related error instead of user data.
   */
  private HttpResponse fetchData()
      throws GadgetException, OAuthProtocolException {
    try {
      List<OAuth.Parameter> msgParams =
        OAuth.isFormEncoded(realRequest.getContentType())
        ? OAuth.decodeForm(realRequest.getPostBodyAsString())
        : new ArrayList<OAuth.Parameter>();

      String method = realRequest.getMethod();

      msgParams.add(new OAuth.Parameter(XOAUTH_APP_URL, fetchParams.getAuthToken().getAppUrl()));

      // Build and sign the message.
      OAuthMessage oauthRequest = newRequestMessage(
          method, realRequest.getUri().toString(), msgParams);

      HttpRequest oauthHttpRequest = createHttpRequest(
          realRequest,
          filterOAuthParams(oauthRequest));

      // Not externally cacheable.
      oauthHttpRequest.setIgnoreCache(true);

      HttpResponse response = nextFetcher.fetch(oauthHttpRequest);

      checkForProtocolProblem(response);

      // Track metadata on the response
      responseParams.addToResponse(response);
      return response;
    } catch (UnsupportedEncodingException e) {
      throw new GadgetException(GadgetException.Code.INTERNAL_SERVER_ERROR, e);
    } catch (IOException e) {
      throw new GadgetException(GadgetException.Code.INTERNAL_SERVER_ERROR, e);
    } catch (URISyntaxException e) {
      throw new GadgetException(GadgetException.Code.INTERNAL_SERVER_ERROR, e);
    } catch (OAuthException e) {
      throw new GadgetException(GadgetException.Code.INTERNAL_SERVER_ERROR, e);
    }
  }

  private void checkForProtocolProblem(HttpResponse response)
      throws OAuthProtocolException, IOException {
    int status = response.getHttpStatusCode();
    if (status >= 400 && status < 500) {
      OAuthMessage message = parseAuthHeader(null, response);
      if (message.getParameter(OAuthProblemException.OAUTH_PROBLEM) != null) {
        // SP reported extended error information
        throw new OAuthProtocolException(message);
      }
      // No extended information, guess based on HTTP response code.
      throw new OAuthProtocolException(status);
    }
  }

  /**
   * Extracts only those parameters from an OAuthMessage that are OAuth-related.
   * An OAuthMessage may hold a whole bunch of non-OAuth-related parameters
   * because they were all needed for signing. But when constructing a request
   * we need to be able to extract just the OAuth-related parameters because
   * they, and only they, may have to be put into an Authorization: header or
   * some such thing.
   *
   * @param message the OAuthMessage object, which holds non-OAuth parameters
   * such as foo=bar (which may have been in the original URI query part, or
   * perhaps in the POST body), as well as OAuth-related parameters (such as
   * oauth_timestamp or oauth_signature).
   *
   * @return a list that contains only the oauth_related parameters.
   *
   * @throws IOException
   */
  private List<Map.Entry<String, String>>
      filterOAuthParams(OAuthMessage message) throws IOException {
    List<Map.Entry<String, String>> result =
        new ArrayList<Map.Entry<String, String>>();
    for (Map.Entry<String, String> param : message.getParameters()) {
      if (param.getKey().toLowerCase().startsWith("oauth")
          || param.getKey().toLowerCase().startsWith("xoauth")) {
        result.add(param);
      }
    }
    return result;
  }
}
