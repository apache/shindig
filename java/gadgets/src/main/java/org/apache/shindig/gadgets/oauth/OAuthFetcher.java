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

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthMessage;

import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.GadgetToken;
import org.apache.shindig.gadgets.RemoteContent;
import org.apache.shindig.gadgets.RemoteContentFetcher;
import org.apache.shindig.gadgets.RemoteContentRequest;
import org.apache.shindig.util.BlobCrypter;
import org.apache.shindig.util.BlobCrypterException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
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
public class OAuthFetcher extends RemoteContentFetcher {

  // We store some blobs of data on the client for later reuse; the blobs
  // contain key/value pairs, and these are the key names.
  private static final String REQ_TOKEN_KEY = "r";
  private static final String REQ_TOKEN_SECRET_KEY = "rs";
  private static final String ACCESS_TOKEN_KEY = "a";
  private static final String ACCESS_TOKEN_SECRET_KEY = "as";
  private static final String OWNER_KEY = "o";
  
  // names for the JSON values we return to the client
  public static final String CLIENT_STATE = "oauthState";
  public static final String APPROVAL_URL = "approvalUrl";

  /**
   * Maximum age for our client state; if this is exceeded we start over. One
   * hour is a fairly arbitrary time limit here.
   */
  private static final int CLIENT_STATE_MAX_AGE_SECS = 3600;

  /**
   * The gadget security token, with info about owner/viewer/gadget.
   */
  protected final GadgetToken authToken;

  /**
   * The gadget's nickname for the service provider.
   */
  protected final String serviceName;

  /**
   * The gadget's nickname for the token.
   */
  protected final String tokenName;

  /**
   * Reference to our persistent store for OAuth metadata.
   */
  protected GadgetOAuthTokenStore tokenStore;

  /**
   * The accessor we use for signing messages. This also holds metadata about
   * the service provider, such as their URLs and the keys we use to access
   * those URLs.
   */
  private OAuthStore.AccessorInfo accessorInfo;

  /**
   * We use this to encrypt and sign the state we cache on the client.
   */
  private BlobCrypter oauthCrypter;

  /**
   * State the client sent with their request.
   */
  private Map<String, String> origClientState;

  /**
   * The request the client really wants to make.
   */
  private RemoteContentRequest realRequest;

  /**
   * State to cache on the client.
   */
  private String newClientState;

  /**
   * Authorization URL for the client
   */
  private String aznUrl;

  /**
   *
   * @param oauthCrypter used to encrypt transient information we store on the
   *        client.
   * @param nextFetcher fetcher to use for actually making requests
   * @param authToken user's gadget security token
   * @param serviceName gadget's nickname for the service being accessed
   * @param tokenName gadget's nickname for the token to be used
   * @param origClientState state sent in from the client
   * @param tokenStore storage for long lived tokens.
   */
  public OAuthFetcher(
      GadgetOAuthTokenStore tokenStore,
      BlobCrypter oauthCrypter,
      RemoteContentFetcher nextFetcher,
      GadgetToken authToken,
      OAuthRequestParams params) {
    super(nextFetcher);
    this.oauthCrypter = oauthCrypter;
    this.authToken = authToken;
    this.serviceName = params.getServiceName();
    this.tokenName = params.getTokenName();
    this.newClientState = null;
    this.aznUrl = null;
    String origClientState = params.getOrigClientState();
    if (origClientState != null && origClientState.length() > 0) {
      try {
        this.origClientState =
          oauthCrypter.unwrap(origClientState, CLIENT_STATE_MAX_AGE_SECS);
      } catch (BlobCrypterException e) {
        // Probably too old, pretend we never saw it at all.
      }
    }
    if (this.origClientState == null) {
      this.origClientState = new HashMap<String, String>();
    }
    this.tokenStore = tokenStore;
  }

  public void init() throws GadgetException {
    lookupOAuthMetadata();
  }

  /**
   * Retrieves metadata from our persistent store.
   *
   * TODO(beaton): can we fix this so it avoids hitting the persistent data
   * store when a client makes multiple requests with an approved access token?
   *
   * @throws GadgetException
   */
  protected void lookupOAuthMetadata() throws GadgetException {
    OAuthStore.TokenKey tokenKey = buildTokenKey();
    accessorInfo = tokenStore.getOAuthAccessor(tokenKey);

    // The persistent data store may be out of sync with reality; we trust
    // the state we stored on the client to be accurate.
    OAuthAccessor accessor = accessorInfo.getAccessor();
    if (origClientState.containsKey(REQ_TOKEN_KEY)) {
      accessor.requestToken = origClientState.get(REQ_TOKEN_KEY);
      accessor.tokenSecret = origClientState.get(REQ_TOKEN_SECRET_KEY);
    } else if (origClientState.containsKey(ACCESS_TOKEN_KEY)) {
      accessor.accessToken = origClientState.get(ACCESS_TOKEN_KEY);
      accessor.tokenSecret = origClientState.get(ACCESS_TOKEN_SECRET_KEY);
    }
  }

  private OAuthStore.TokenKey buildTokenKey() {
    OAuthStore.TokenKey tokenKey = new OAuthStore.TokenKey();
    tokenKey.setGadgetUri(authToken.getAppUrl());
    tokenKey.setModuleId(authToken.getModuleId());
    tokenKey.setServiceName(serviceName);
    tokenKey.setTokenName(tokenName);
    // At some point we might want to let gadgets specify whether to use OAuth
    // for the owner, the viewer, or someone else. For now always using the
    // owner identity seems reasonable.
    tokenKey.setUserId(authToken.getOwnerId());
    return tokenKey;
  }

  @Override
  public RemoteContent fetch(RemoteContentRequest request)
      throws GadgetException {
    this.realRequest = request;
    if (needApproval()) {
      // This is section 6.1 of the OAuth spec.
      checkCanApprove();
      fetchRequestToken();
      // This is section 6.2 of the OAuth spec.
      buildClientApprovalState();
      buildAznUrl();
      // break out of the content fetching chain, we need permission from
      // the user to do this
      return null;
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
    String pageOwner = authToken.getOwnerId();
    String pageViewer = authToken.getViewerId();
    String stateOwner = origClientState.get(OWNER_KEY);
    if (!pageOwner.equals(pageViewer)) {
      throw new GadgetException(GadgetException.Code.INTERNAL_SERVER_ERROR,
          "Only page owners can grant OAuth approval");
    }
    if (stateOwner != null && !stateOwner.equals(pageOwner)) {
      throw new GadgetException(GadgetException.Code.INTERNAL_SERVER_ERROR,
          "Client state belongs to a different person.");
    }
  }

  private void fetchRequestToken() throws GadgetException {
    try {
      OAuthAccessor accessor = accessorInfo.getAccessor();
      String url = accessor.consumer.serviceProvider.requestTokenURL;
      OAuthMessage request = newRequestMessage(url);
      OAuthMessage reply = sendOAuthMessage(request);
      reply.requireParameters(OAuth.OAUTH_TOKEN, OAuth.OAUTH_TOKEN_SECRET);
      accessor.requestToken = reply.getParameter(OAuth.OAUTH_TOKEN);
      accessor.tokenSecret = reply.getParameter(OAuth.OAUTH_TOKEN_SECRET);
    } catch (Exception e) {
      // It's unfortunate the OAuth libraries throw a generic Exception.
      throw new GadgetException(GadgetException.Code.INTERNAL_SERVER_ERROR, e);
    }
  }

  private OAuthMessage newRequestMessage(String method,
      String url, List<OAuth.Parameter> params) throws Exception {

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

  private OAuthMessage newRequestMessage(String url) throws Exception {
    ArrayList<OAuth.Parameter> params = new ArrayList<OAuth.Parameter>();
    return newRequestMessage(url, params);
  }

  private OAuthMessage newRequestMessage(String url,
      List<OAuth.Parameter> params) throws Exception {
    String method = "POST";
    if (accessorInfo.getHttpMethod() == OAuthStore.HttpMethod.GET) {
      method = "GET";
    }
    return newRequestMessage(method, url, params);
  }

  /**
   * Sends OAuth request token and access token messages.
   */
  private OAuthMessage sendOAuthMessage(OAuthMessage request)
      throws IOException, URISyntaxException, GadgetException {
    String params = "";
    String url = request.URL;
    if (accessorInfo.getHttpMethod() == OAuthStore.HttpMethod.GET) {
      url = OAuth.addParameters(url, request.getParameters());
    } else {
      params = OAuth.formEncode(request.getParameters());
    }

    RemoteContentRequest rcr =
        new RemoteContentRequest(request.method, new URI(url), null,
            params.getBytes(), RemoteContentRequest.DEFAULT_OPTIONS);
    RemoteContent content = nextFetcher.fetch(rcr);
    OAuthMessage reply = new OAuthMessage(null, null, null);
    reply.addParameters(OAuth.decodeForm(content.getResponseAsString()));
    return reply;
  }

  /**
   * Builds the data we'll cache on the client while we wait for approval.
   */
  private void buildClientApprovalState() throws GadgetException {
    try {
      OAuthAccessor accessor = accessorInfo.getAccessor();
      Map<String, String> oauthState = new HashMap<String, String>();
      oauthState.put(REQ_TOKEN_KEY, accessor.requestToken);
      oauthState.put(REQ_TOKEN_SECRET_KEY, accessor.tokenSecret);
      oauthState.put(OWNER_KEY, authToken.getOwnerId());
      newClientState = oauthCrypter.wrap(oauthState);
    } catch (BlobCrypterException e) {
      throw new GadgetException(GadgetException.Code.INTERNAL_SERVER_ERROR, e);
    }
  }

  /**
   * Builds the URL the client needs to visit to approve access.
   */
  private void buildAznUrl() {
    // At some point we can be clever and use a callback URL to improve
    // the user experience, but that's too complex for now.
    OAuthAccessor accessor = accessorInfo.getAccessor();
    StringBuffer azn = new StringBuffer(
        accessor.consumer.serviceProvider.userAuthorizationURL);
    if (azn.indexOf("?") == -1) {
      azn.append("?");
    } else {
      azn.append("&");
    }
    azn.append(OAuth.OAUTH_TOKEN);
    azn.append("=");
    azn.append(OAuth.percentEncode(accessor.requestToken));
    aznUrl = azn.toString();
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
   */
  private void exchangeRequestToken() throws GadgetException {
    try {
      OAuthAccessor accessor = accessorInfo.getAccessor();
      String url = accessor.consumer.serviceProvider.accessTokenURL;
      List<OAuth.Parameter> msgParams = new ArrayList<OAuth.Parameter>();
      msgParams.add(
          new OAuth.Parameter(OAuth.OAUTH_TOKEN, accessor.requestToken));
      OAuthMessage request = newRequestMessage(url, msgParams);
      OAuthMessage reply = sendOAuthMessage(request);
      reply.requireParameters(OAuth.OAUTH_TOKEN, OAuth.OAUTH_TOKEN_SECRET);
      accessor.accessToken = reply.getParameter(OAuth.OAUTH_TOKEN);
      accessor.tokenSecret = reply.getParameter(OAuth.OAUTH_TOKEN_SECRET);
    } catch (Exception e) {
      // It's unfortunate the OAuth libraries throw a generic Exception.
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
    tokenStore.storeTokenKeyAndSecret(tokenKey, tokenInfo);
  }

  /**
   * Builds the data we'll cache on the client while we make requests.
   */
  private void buildClientAccessState() throws GadgetException {
    try {
      Map<String, String> oauthState = new HashMap<String, String>();
      OAuthAccessor accessor = accessorInfo.getAccessor();
      oauthState.put(ACCESS_TOKEN_KEY, accessor.accessToken);
      oauthState.put(ACCESS_TOKEN_SECRET_KEY, accessor.tokenSecret);
      oauthState.put(OWNER_KEY, authToken.getOwnerId());
      newClientState = oauthCrypter.wrap(oauthState);
    } catch (BlobCrypterException e) {
      throw new GadgetException(GadgetException.Code.INTERNAL_SERVER_ERROR, e);
    }
  }

  /**
   * Get honest-to-goodness user data.
   */
  private RemoteContent fetchData() throws GadgetException {
    try {
      List<OAuth.Parameter> msgParams =
          OAuth.decodeForm(realRequest.getPostBodyAsString());

      String method = realRequest.getMethod();

      // Build and sign the message.
      OAuthMessage oauthRequest = newRequestMessage(
          method, realRequest.getUri().toASCIIString(), msgParams);

      // Convert the signed message to a RemoteContentRequest
      String url = oauthRequest.URL;
      byte postBytes[] = null;
      if (method.equals("POST")) {
        postBytes = OAuth.formEncode(oauthRequest.getParameters()).getBytes();
      } else {
        url = OAuth.addParameters(url, oauthRequest.getParameters());
      }

      RemoteContentRequest rcr = new RemoteContentRequest(
          realRequest.getMethod(),
          new URI(url),
          realRequest.getAllHeaders(),
          postBytes,
          realRequest.getOptions());
      return nextFetcher.fetch(rcr);
    } catch (UnsupportedEncodingException e) {
      throw new GadgetException(GadgetException.Code.INTERNAL_SERVER_ERROR, e);
    } catch (IOException e) {
      throw new GadgetException(GadgetException.Code.INTERNAL_SERVER_ERROR, e);
    } catch (URISyntaxException e) {
      throw new GadgetException(GadgetException.Code.INTERNAL_SERVER_ERROR, e);
    } catch (Exception e) {
      throw new GadgetException(GadgetException.Code.INTERNAL_SERVER_ERROR, e);
    }
  }
  
  @Override
  public Map<String, String> getResponseMetadata() {
    Map<String, String> extra = new HashMap<String, String>();
    if (newClientState != null) {
      extra.put(CLIENT_STATE, newClientState);
    }
    if (aznUrl != null) {
      extra.put(APPROVAL_URL, aznUrl);
    }
    return extra;
  }
}
