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

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.uri.UriBuilder;
import org.apache.shindig.common.util.CharsetUtil;
import org.apache.shindig.gadgets.ChainedContentFetcher;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.RequestSigningException;
import org.apache.shindig.gadgets.http.BasicHttpFetcher;
import org.apache.shindig.gadgets.http.HttpCache;
import org.apache.shindig.gadgets.http.HttpCacheKey;
import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.apache.shindig.gadgets.oauth.AccessorInfo.HttpMethod;
import org.apache.shindig.gadgets.oauth.AccessorInfo.OAuthParamLocation;
import org.apache.shindig.gadgets.oauth.OAuthStore.TokenInfo;

import com.google.common.collect.Maps;

import org.apache.commons.io.IOUtils;

import net.oauth.OAuth;
import net.oauth.OAuth.Parameter;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthException;
import net.oauth.OAuthMessage;
import net.oauth.OAuthProblemException;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Implements both signed fetch and full OAuth for gadgets, as well as a combination of the two that
 * is necessary to build OAuth enabled gadgets for social sites.
 * 
 * Signed fetch sticks identity information in the query string, signed either with the container's
 * private key, or else with a secret shared between the container and the gadget.
 * 
 * Full OAuth redirects the user to the OAuth service provider site to obtain the user's permission
 * to access their data.  Read the example in the appendix to the OAuth spec for a summary of how
 * this works (The spec is at http://oauth.net/core/1.0/).
 * 
 * The combination protocol works by sending identity information in all requests, and allows the
 * OAuth dance to happen as well when owner == viewer.  This lets OAuth service providers build up
 * an identity mapping from ids on social network sites to their own local ids.
 */
public class OAuthFetcher extends ChainedContentFetcher {

  // Logger
  private static final Logger logger = Logger.getLogger(OAuthFetcher.class.getName());
  
  // Maximum number of attempts at the protocol before giving up.
  private static final int MAX_ATTEMPTS = 2;

  // names of additional OAuth parameters we include in outgoing requests
  // TODO(beaton): can we do away with this bit in favor of the opensocial param?
  public static final String XOAUTH_APP_URL = "xoauth_app_url";
  
  protected static final String OPENSOCIAL_OWNERID = "opensocial_owner_id";

  protected static final String OPENSOCIAL_VIEWERID = "opensocial_viewer_id";

  protected static final String OPENSOCIAL_APPID = "opensocial_app_id";
  
  // TODO(beaton): figure out if this is the name in the 0.8 spec.
  protected static final String OPENSOCIAL_APPURL = "opensocial_app_url";

  protected static final String XOAUTH_PUBLIC_KEY = "xoauth_signature_publickey";
  
  protected static final Pattern ALLOWED_PARAM_NAME = Pattern.compile("[-:\\w~!@$*()_\\[\\]:,./]+");
  
  private static final String OAUTH_SESSION_HANDLE = "oauth_session_handle";
  
  private static final String OAUTH_EXPIRES_IN = "oauth_expires_in";
  
  private static final long ACCESS_TOKEN_EXPIRE_UNKNOWN = 0;
  private static final long ACCESS_TOKEN_FORCE_EXPIRE = -1;

  /**
   * State information from client
   */
  protected final OAuthClientState clientState;

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
  private AccessorInfo accessorInfo;

  /**
   * The request the client really wants to make.
   */
  private HttpRequest realRequest;

  /**
   * @param fetcherConfig configuration options for the fetcher
   * @param nextFetcher fetcher to use for actually making requests
   * @param request request that will be sent through the fetcher
   */
  public OAuthFetcher(
      OAuthFetcherConfig fetcherConfig,
      HttpFetcher nextFetcher,
      HttpRequest request) {
    super(nextFetcher);
    this.fetcherConfig = fetcherConfig;
    this.clientState = new OAuthClientState(
        fetcherConfig.getStateCrypter(),
        request.getOAuthArguments().getOrigClientState());
    this.responseParams = new OAuthResponseParams(fetcherConfig.getStateCrypter());
  }

  /**
   * Retrieves metadata from our persistent store.
   */
  private void lookupOAuthMetadata() throws GadgetException {
    accessorInfo = fetcherConfig.getTokenStore().getOAuthAccessor(
        realRequest.getSecurityToken(), realRequest.getOAuthArguments(), clientState);
  }

  public HttpResponse fetch(HttpRequest request) throws GadgetException {
    this.realRequest = request;
    HttpCacheKey cacheKey = makeCacheKey();
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
      } catch (UserVisibleOAuthException e) {
        responseParams.setError(e.getOAuthErrorCode());
        return buildErrorResponse(e);
      }
    } while (retry);

    if (response == null) {
      throw new GadgetException(
          GadgetException.Code.INTERNAL_SERVER_ERROR,
          "No response for OAuth fetch to " + realRequest.getUri());
    }
    return fetcherConfig.getHttpCache().addResponse(cacheKey, request, response);
  }

  // Builds up a cache key.  Full OAuth and signed fetch have slightly different cache semantics
  // that both need to be accounted for here.  For signed fetch, we need to remember what identity
  // information we passed along (owner only?  viewer only?  both?).  For OAuth, we need to
  // remember whose OAuth token we used.  We only use the OAuth token when owner == viewer, and
  // it's possible we won't do it even then.
  private HttpCacheKey makeCacheKey() {
    HttpCacheKey key = new HttpCacheKey(realRequest);
    SecurityToken st = realRequest.getSecurityToken();
    key.set("authentication", "oauth");
    if (realRequest.getOAuthArguments().getSignOwner()) {
      key.set("owner", st.getOwnerId());
    }
    if (realRequest.getOAuthArguments().getSignViewer()) {
      key.set("viewer", st.getViewerId());
    }
    if (st.getOwnerId() != null
        && st.getOwnerId().equals(st.getViewerId())
        && realRequest.getOAuthArguments().mayUseToken()) {
      key.set("tokenOwner", st.getOwnerId());
    }
    key.set("gadget", st.getAppUrl());
    key.set("instance", Long.toString(st.getModuleId()));
    key.set("service", realRequest.getOAuthArguments().getServiceName());
    key.set("token", realRequest.getOAuthArguments().getTokenName());
    return key;
  }

  private HttpResponse buildErrorResponse(GadgetException e) {
    if (responseParams.getError() == null) {
      responseParams.setError(OAuthError.UNKNOWN_PROBLEM);
    }
    if (responseParams.getErrorText() == null && (e instanceof UserVisibleOAuthException)) {
      responseParams.setErrorText(e.getMessage());
    }
    logger.log(Level.WARNING, "OAuth error", e);
    return buildNonDataResponse(403);
  }

  private boolean handleProtocolException(
      OAuthProtocolException pe, int attempts) throws GadgetException {
    if (pe.canExtend()) {
      accessorInfo.setTokenExpireMillis(ACCESS_TOKEN_FORCE_EXPIRE);
    } else if (pe.startFromScratch()) {
      fetcherConfig.getTokenStore().removeToken(realRequest.getSecurityToken(),
          accessorInfo.getConsumer(), realRequest.getOAuthArguments());
      accessorInfo.getAccessor().accessToken = null;
      accessorInfo.getAccessor().requestToken = null;
      accessorInfo.getAccessor().tokenSecret = null;
      accessorInfo.setSessionHandle(null);
      accessorInfo.setTokenExpireMillis(ACCESS_TOKEN_EXPIRE_UNKNOWN);
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
    return (realRequest.getOAuthArguments().mustUseToken()
            && accessorInfo.getAccessor().requestToken == null
            && accessorInfo.getAccessor().accessToken == null);
  }
  
  /**
   * Make sure the user is authorized to approve access tokens.  At the moment
   * we restrict this to page owner's viewing their own pages.
   *
   * @throws GadgetException
   */
  private void checkCanApprove() throws GadgetException {
    String pageOwner = realRequest.getSecurityToken().getOwnerId();
    String pageViewer = realRequest.getSecurityToken().getViewerId();
    String stateOwner = clientState.getOwner();
    if (pageOwner == null) {
      throw new UserVisibleOAuthException(OAuthError.UNAUTHENTICATED, "Unauthenticated");
    }
    if (!pageOwner.equals(pageViewer)) {
      throw new UserVisibleOAuthException(OAuthError.NOT_OWNER,
          "Only page owners can grant OAuth approval");
    }
    if (stateOwner != null && !stateOwner.equals(pageOwner)) {
      throw new GadgetException(GadgetException.Code.INTERNAL_SERVER_ERROR,
          "Client state belongs to a different person.");
    }
  }

  private void fetchRequestToken() throws GadgetException, OAuthProtocolException {
    try {
      OAuthAccessor accessor = accessorInfo.getAccessor();
      HttpRequest request = new HttpRequest(
          Uri.parse(accessor.consumer.serviceProvider.requestTokenURL));
      request.setMethod(accessorInfo.getHttpMethod().toString());
      if (accessorInfo.getHttpMethod() == HttpMethod.POST) {
        request.setHeader("Content-Type", OAuth.FORM_ENCODED);
      }
      
      HttpRequest signed = sanitizeAndSign(request, null);
 
      OAuthMessage reply = sendOAuthMessage(signed);
      
      accessor.requestToken = OAuthUtil.getParameter(reply, OAuth.OAUTH_TOKEN);
      accessor.tokenSecret = OAuthUtil.getParameter(reply, OAuth.OAUTH_TOKEN_SECRET);
    } catch (OAuthException e) {
      throw new UserVisibleOAuthException(e.getMessage(), e);
    }
  }

  /**
   * Strip out any owner or viewer identity information passed by the client.
   * 
   * @throws RequestSigningException
   */
  private List<Parameter> sanitize(List<Parameter> params)
      throws RequestSigningException {
    ArrayList<Parameter> list = new ArrayList<Parameter>();
    for (Parameter p : params) {
      String name = p.getKey();
      if (allowParam(name)) {
        list.add(p);
      } else {
        throw new RequestSigningException("invalid parameter name " + name);
      }
    }
    return list;
  }
  
  private boolean allowParam(String paramName) {
    String canonParamName = paramName.toLowerCase();
    return (!(canonParamName.startsWith("oauth") ||
        canonParamName.startsWith("xoauth") ||
        canonParamName.startsWith("opensocial")) &&
        ALLOWED_PARAM_NAME.matcher(canonParamName).matches());
  }
   
  /**
   * Add identity information, such as owner/viewer/gadget.
   */
  private void addIdentityParams(List<Parameter> params) {
    String owner = realRequest.getSecurityToken().getOwnerId();
    if (owner != null && realRequest.getOAuthArguments().getSignOwner()) {
      params.add(new Parameter(OPENSOCIAL_OWNERID, owner));
    }

    String viewer = realRequest.getSecurityToken().getViewerId();
    if (viewer != null && realRequest.getOAuthArguments().getSignViewer()) {
      params.add(new Parameter(OPENSOCIAL_VIEWERID, viewer));
    }

    String app = realRequest.getSecurityToken().getAppId();
    if (app != null) {
      params.add(new Parameter(OPENSOCIAL_APPID, app));
    }
    
    String appUrl = realRequest.getSecurityToken().getAppUrl();
    if (appUrl != null) {
      params.add(new Parameter(OPENSOCIAL_APPURL, appUrl));
    }
    
    if (accessorInfo.getConsumer().getConsumer().consumerKey == null) {
      params.add(
          new Parameter(OAuth.OAUTH_CONSUMER_KEY, realRequest.getSecurityToken().getDomain()));
    }
  }
  
  /**
   * Add signature type to the message.
   */
  private void addSignatureParams(List<Parameter> params) {
    if (accessorInfo.getConsumer().getKeyName() != null) {
      params.add(new Parameter(XOAUTH_PUBLIC_KEY, accessorInfo.getConsumer().getKeyName()));
    }
    params.add(new Parameter(OAuth.OAUTH_VERSION, OAuth.VERSION_1_0));
    params.add(new Parameter(OAuth.OAUTH_TIMESTAMP,
        Long.toString(fetcherConfig.getClock().currentTimeMillis() / 1000)));
  }

  private static String getAuthorizationHeader(
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

  
  /*
    Start with an HttpRequest.
    Throw if there are any attacks in the query.
    Throw if there are any attacks in the post body.
    Build up OAuth parameter list
    Sign it.
    Add OAuth parameters to new request
    Send it.
  */
  public HttpRequest sanitizeAndSign(HttpRequest base, List<Parameter> params)
      throws GadgetException {
    if (params == null) {
      params = new ArrayList<Parameter>();
    }
    UriBuilder target = new UriBuilder(base.getUri());
    String query = target.getQuery();
    target.setQuery(null);
    params.addAll(sanitize(OAuth.decodeForm(query)));
    if (OAuth.isFormEncoded(base.getHeader("Content-Type"))) {
      params.addAll(sanitize(OAuth.decodeForm(base.getPostBodyAsString())));
    }

    addIdentityParams(params);
    
    addSignatureParams(params);

    try {
      OAuthMessage signed = OAuthUtil.newRequestMessage(accessorInfo.getAccessor(), 
          base.getMethod(), target.toString(), params);
      HttpRequest oauthHttpRequest = createHttpRequest(base, selectOAuthParams(signed));
      // Following 302s on OAuth responses is unlikely to be productive.
      oauthHttpRequest.setFollowRedirects(false);
      return oauthHttpRequest;
    } catch (OAuthException e) {
      throw new GadgetException(GadgetException.Code.INTERNAL_SERVER_ERROR, e);
    }
  }

  private HttpRequest createHttpRequest(HttpRequest base,
      List<Map.Entry<String, String>> oauthParams) throws GadgetException {

    OAuthParamLocation paramLocation = accessorInfo.getParamLocation();

    // paramLocation could be overriden by a run-time parameter to fetchRequest

    HttpRequest result = new HttpRequest(base);
    
    // If someone specifies that OAuth parameters go in the body, but then sends a request for
    // data using GET, we've got a choice.  We can throw some type of error, since a GET request
    // can't have a body, or we can stick the parameters somewhere else, like, say, the header.
    // We opt to put them in the header, since that stands some chance of working with some
    // OAuth service providers.
    if (paramLocation == OAuthParamLocation.POST_BODY &&
        !result.getMethod().equals("POST")) {
      paramLocation = OAuthParamLocation.AUTH_HEADER;
    }

    switch (paramLocation) {
      case AUTH_HEADER:
        result.addHeader("Authorization", getAuthorizationHeader(oauthParams));
        break;

      case POST_BODY:
        String contentType = result.getHeader("Content-Type");
        if (!OAuth.isFormEncoded(contentType)) {
          throw new UserVisibleOAuthException(
              "OAuth param location can only be post_body if post body if of " +
              "type x-www-form-urlencoded");
        }
        String oauthData = OAuthUtil.formEncode(oauthParams);
        if (result.getPostBodyLength() == 0) {
          result.setPostBody(CharsetUtil.getUtf8Bytes(oauthData));
        } else {
          result.setPostBody((result.getPostBodyAsString() + '&' + oauthData).getBytes());
        }
        break;

      case URI_QUERY:
        result.setUri(Uri.parse(OAuthUtil.addParameters(result.getUri().toString(), oauthParams)));
        break;
    }

    return result;
  }

  /**
   * Sends OAuth request token and access token messages.
   */
  private OAuthMessage sendOAuthMessage(HttpRequest request)
      throws GadgetException, OAuthProtocolException, OAuthProblemException {
    HttpResponse response = nextFetcher.fetch(request);
    boolean done = false;
    try {
      checkForProtocolProblem(response);
      OAuthMessage reply = new OAuthMessage(null, null, null);

      reply.addParameters(OAuth.decodeForm(response.getResponseAsString()));
      reply = parseAuthHeader(reply, response);
      OAuthUtil.requireParameters(reply, OAuth.OAUTH_TOKEN, OAuth.OAUTH_TOKEN_SECRET);
      done = true;
      return reply;
    } finally {
      if (!done) {
        logServiceProviderError(request, response);
      }
    }
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

    for (String auth : resp.getHeaders("WWW-Authenticate")) {
      msg.addParameters(OAuthMessage.decodeAuthorization(auth));
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
    responseParams.getNewClientState().setOwner(realRequest.getSecurityToken().getOwnerId());
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
    HttpResponseBuilder response = new HttpResponseBuilder().setHttpStatusCode(status);
    responseParams.addToResponse(response);
    response.setStrictNoCache();
    return response.create();
  }

  /**
   * Do we need to exchange a request token for an access token?
   */
  private boolean needAccessToken() {
    if (realRequest.getOAuthArguments().mustUseToken()
        && accessorInfo.getAccessor().requestToken != null
        && accessorInfo.getAccessor().accessToken == null) {
      return true;
    }
    if (realRequest.getOAuthArguments().mayUseToken() && accessTokenExpired()) {
      return true;
    }
    return false;
  }

  private boolean accessTokenExpired() {
    return (accessorInfo.getTokenExpireMillis() != ACCESS_TOKEN_EXPIRE_UNKNOWN
        && accessorInfo.getTokenExpireMillis() < fetcherConfig.getClock().currentTimeMillis());
  }
  
  /**
   * Implements section 6.3 of the OAuth spec.
   * @throws OAuthProtocolException
   */
  private void exchangeRequestToken()
      throws GadgetException, OAuthProtocolException {
    try {
      if (accessorInfo.getAccessor().accessToken != null) {
        // session extension per
        // http://oauth.googlecode.com/svn/spec/ext/session/1.0/drafts/1/spec.html
        accessorInfo.getAccessor().requestToken = accessorInfo.getAccessor().accessToken;
        accessorInfo.getAccessor().accessToken = null;
      }
      OAuthAccessor accessor = accessorInfo.getAccessor();
      HttpRequest request = new HttpRequest(
          Uri.parse(accessor.consumer.serviceProvider.accessTokenURL));
      request.setMethod(accessorInfo.getHttpMethod().toString());
      if (accessorInfo.getHttpMethod() == HttpMethod.POST) {
        request.setHeader("Content-Type", OAuth.FORM_ENCODED);
      }
      
      List<Parameter> msgParams = new ArrayList<Parameter>();
      msgParams.add(new Parameter(OAuth.OAUTH_TOKEN, accessor.requestToken));
      if (accessorInfo.getSessionHandle() != null) {
        msgParams.add(new Parameter(OAUTH_SESSION_HANDLE, accessorInfo.getSessionHandle()));
      }
      
      HttpRequest signed = sanitizeAndSign(request, msgParams);
      
      OAuthMessage reply = sendOAuthMessage(signed);
      
      accessor.accessToken = OAuthUtil.getParameter(reply, OAuth.OAUTH_TOKEN);
      accessor.tokenSecret = OAuthUtil.getParameter(reply, OAuth.OAUTH_TOKEN_SECRET);
      accessorInfo.setSessionHandle(OAuthUtil.getParameter(reply, OAUTH_SESSION_HANDLE));
      accessorInfo.setTokenExpireMillis(ACCESS_TOKEN_EXPIRE_UNKNOWN);
      if (OAuthUtil.getParameter(reply, OAUTH_EXPIRES_IN) != null) {
        try {
          int expireSecs = Integer.parseInt(OAuthUtil.getParameter(reply, OAUTH_EXPIRES_IN));
          long expireMillis = fetcherConfig.getClock().currentTimeMillis() + expireSecs * 1000;
          accessorInfo.setTokenExpireMillis(expireMillis);
        } catch (NumberFormatException e) {
          // Hrm.  Bogus server.  We can safely ignore this, we'll just wait for the server to
          // tell us when the access token has expired.
          logger.log(Level.WARNING, "server returned bogus expiration: " + reply);
        }
      }
    } catch (OAuthException e) {
      throw new UserVisibleOAuthException(e.getMessage(), e);
    }
  }

  /**
   * Save off our new token and secret to the persistent store.
   *
   * @throws GadgetException
   */
  private void saveAccessToken() throws GadgetException {
    OAuthAccessor accessor = accessorInfo.getAccessor();
    TokenInfo tokenInfo = new TokenInfo(accessor.accessToken, accessor.tokenSecret,
        accessorInfo.getSessionHandle(), accessorInfo.getTokenExpireMillis());
    fetcherConfig.getTokenStore().storeTokenKeyAndSecret(realRequest.getSecurityToken(),
        accessorInfo.getConsumer(), realRequest.getOAuthArguments(), tokenInfo);
  }

  /**
   * Builds the data we'll cache on the client while we make requests.
   */
  private void buildClientAccessState() {
    OAuthAccessor accessor = accessorInfo.getAccessor();
    responseParams.getNewClientState().setAccessToken(accessor.accessToken);
    responseParams.getNewClientState().setAccessTokenSecret(accessor.tokenSecret);
    responseParams.getNewClientState().setOwner(realRequest.getSecurityToken().getOwnerId());
    responseParams.getNewClientState().setSessionHandle(accessorInfo.getSessionHandle());
    responseParams.getNewClientState().setTokenExpireMillis(accessorInfo.getTokenExpireMillis());
  }

  /**
   * Get honest-to-goodness user data.
   *
   * @throws OAuthProtocolException if the service provider returns an OAuth
   * related error instead of user data.
   */
  private HttpResponse fetchData() throws GadgetException, OAuthProtocolException {
    HttpRequest signed = sanitizeAndSign(realRequest, null);

    HttpResponse response = nextFetcher.fetch(signed);

    try {
      checkForProtocolProblem(response);
    } catch (OAuthProtocolException e) {
      logServiceProviderError(signed, response);
      throw e;
    }

    // Track metadata on the response
    HttpResponseBuilder builder = new HttpResponseBuilder(response);
    responseParams.addToResponse(builder);
    return builder.create();
  }

  /**
   * Look for an OAuth protocol problem.  For cases where no access token is in play 
   * @param response
   * @throws OAuthProtocolException
   */
  private void checkForProtocolProblem(HttpResponse response) throws OAuthProtocolException {
    if (isFullOAuthError(response)) {
      OAuthMessage message = parseAuthHeader(null, response);
      if (OAuthUtil.getParameter(message, OAuthProblemException.OAUTH_PROBLEM) != null) {
        // SP reported extended error information
        throw new OAuthProtocolException(message);
      }
      // No extended information, guess based on HTTP response code.
      throw new OAuthProtocolException(response.getHttpStatusCode());
    }
  }
  
  /**
   * Check if a response might be due to an OAuth protocol error.  We don't want to intercept
   * errors for signed fetch, we only care about places where we are dealing with OAuth request
   * and/or access tokens.
   */
  private boolean isFullOAuthError(HttpResponse response) {
    // 400, 401 and 403 are likely to be authentication errors.
    if (response.getHttpStatusCode() != 400 && response.getHttpStatusCode() != 401 &&
        response.getHttpStatusCode() != 403) {
      return false;
    }
    // If the client forced us to use full OAuth, this might be OAuth related.
    if (realRequest.getOAuthArguments().mustUseToken()) {
      return true;
    }
    // If we're using an access token, this might be OAuth related.
    if (accessorInfo.getAccessor().accessToken != null) {
      return true;
    }
    // Not OAuth related.
    return false;
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
   */
  private static List<Map.Entry<String, String>>
      selectOAuthParams(OAuthMessage message) {
    List<Map.Entry<String, String>> result =
        new ArrayList<Map.Entry<String, String>>();
    for (Map.Entry<String, String> param : OAuthUtil.getParameters(message)) {
      if (isContainerInjectedParameter(param.getKey())) {
        result.add(param);
      }
    }
    return result;
  }

  private static boolean isContainerInjectedParameter(String key) {
    key = key.toLowerCase();
    return key.startsWith("oauth") || key.startsWith("xoauth") || key.startsWith("opensocial");
  }
  
  
  /** Logging for errors that service providers return to us, useful for integration problems */
  private void logServiceProviderError(HttpRequest request, HttpResponse response) {
    logger.log(Level.INFO, "OAuth request failed:\n" + request + "\nresponse:\n" + response);
  }

  /**
   *  Run a simple OAuth fetcher to execute a variety of OAuth fetches and output
   *  the result
   *
   *  Arguments
   *  --consumerKey <oauth_consumer_key>
   *  --consumerSecret <oauth_consumer_secret>
   *  --requestorId <xoauth_requestor_id>
   *  --accessToken <oauth_access_token>
   *  --method <GET | POST>
   *  --url <url>
   *  --contentType <contentType>
   *  --postBody <encoded post body>
   *  --postFile <file path of post body contents>
   *  --paramLocation <URI_QUERY | POST_BODY | AUTH_HEADER>
   *
   */
  public static void main(String[] argv) throws Exception {
    Map<String, String> params = Maps.newHashMap();
    for (int i = 0; i < argv.length; i+=2) {
      params.put(argv[i], argv[i+1]);
    }
    final String consumerKey = params.get("--consumerKey");
    final String consumerSecret = params.get("--consumerSecret");
    final String xOauthRequestor = params.get("--requestorId");
    final String accessToken = params.get("--accessToken");
    final String method = params.get("--method") == null ? "GET" :params.get("--method");
    String url = params.get("--url");
    String contentType = params.get("--contentType");
    String postBody = params.get("--postBody");
    String postFile = params.get("--postFile");
    String paramLocation = params.get("--paramLocation");

    HttpRequest request = new HttpRequest(Uri.parse(url));
    if (contentType != null) {
      request.setHeader("Content-Type", contentType);
    } else {
      request.setHeader("Content-Type", OAuth.FORM_ENCODED);
    }
    if (postBody != null) {
      request.setPostBody(postBody.getBytes());
    }
    if (postFile != null) {
      request.setPostBody(IOUtils.toByteArray(new FileInputStream(postFile)));
    }

    OAuthParamLocation paramLocationEnum = OAuthParamLocation.URI_QUERY;
    if (paramLocation != null) {
      paramLocationEnum = OAuthParamLocation.valueOf(paramLocation);
    }


    List<OAuth.Parameter> oauthParams = new ArrayList<OAuth.Parameter>();
    UriBuilder target = new UriBuilder(Uri.parse(url));
    String query = target.getQuery();
    target.setQuery(null);
    oauthParams.addAll(OAuth.decodeForm(query));
    if (OAuth.isFormEncoded(contentType) && request.getPostBodyAsString() != null) {
      oauthParams.addAll(OAuth.decodeForm(request.getPostBodyAsString()));
    }
    if (consumerKey != null) {
      oauthParams.add(new OAuth.Parameter(OAuth.OAUTH_CONSUMER_KEY, consumerKey));
    }
    if (xOauthRequestor != null) {
      oauthParams.add(new OAuth.Parameter("xoauth_requestor_id", xOauthRequestor));
    }

    OAuthConsumer consumer = new OAuthConsumer(null, consumerKey, consumerSecret, null);
    OAuthAccessor accessor = new OAuthAccessor(consumer);
    accessor.accessToken = accessToken;
    OAuthMessage message = accessor.newRequestMessage(method, target.toString(), oauthParams);

    List<Map.Entry<String, String>> entryList = selectOAuthParams(message);

    switch (paramLocationEnum) {
      case AUTH_HEADER:
        request.addHeader("Authorization", getAuthorizationHeader(entryList));
        break;

      case POST_BODY:
        if (!OAuth.isFormEncoded(contentType)) {
          throw new UserVisibleOAuthException(
              "OAuth param location can only be post_body if post body if of " +
                  "type x-www-form-urlencoded");
        }
        String oauthData = OAuthUtil.formEncode(oauthParams);
        if (request.getPostBodyLength() == 0) {
          request.setPostBody(CharsetUtil.getUtf8Bytes(oauthData));
        } else {
          request.setPostBody((request.getPostBodyAsString() + '&' + oauthData).getBytes());
        }
        break;

      case URI_QUERY:
        request.setUri(Uri.parse(OAuthUtil.addParameters(request.getUri().toString(),
            entryList)));
        break;
    }
    request.setMethod(method);

    HttpCache nullCache = new HttpCache() {
      public HttpResponse getResponse(HttpCacheKey key, HttpRequest request) {
        return null;
      }

      public HttpResponse addResponse(HttpCacheKey key, HttpRequest request,
          HttpResponse response) {
        return response;
      }

      public HttpResponse removeResponse(HttpCacheKey key) {
        return null;
      }
    };
    HttpFetcher fetcher = new BasicHttpFetcher(nullCache);
    HttpResponse response = fetcher.fetch(request);

    System.out.println("Request ------------------------------");
    System.out.println(request.toString());
    System.out.println("Response -----------------------------");
    System.out.println(response.toString());
  }
}
