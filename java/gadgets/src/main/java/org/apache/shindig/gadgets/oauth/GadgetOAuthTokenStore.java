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

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.GadgetSpecFactory;
import org.apache.shindig.gadgets.oauth.AccessorInfo.HttpMethod;
import org.apache.shindig.gadgets.oauth.AccessorInfo.OAuthParamLocation;
import org.apache.shindig.gadgets.oauth.OAuthStore.ConsumerInfo;
import org.apache.shindig.gadgets.oauth.OAuthStore.TokenInfo;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.OAuthService;
import org.apache.shindig.gadgets.spec.OAuthSpec;
import org.apache.shindig.gadgets.spec.SpecParserException;
import org.apache.shindig.gadgets.spec.BaseOAuthService.Location;
import org.apache.shindig.gadgets.spec.BaseOAuthService.Method;

import com.google.inject.Inject;
import com.google.common.base.Joiner;

import net.oauth.OAuthServiceProvider;

/**
 * Higher-level interface that allows callers to store and retrieve
 * OAuth-related data directly from {@code GadgetSpec}s, {@code GadgetContext}s,
 * etc. See {@link OAuthStore} for a more detailed explanation of the OAuth
 * Data Store.
 */
public class GadgetOAuthTokenStore {

  private final OAuthStore store;
  private final GadgetSpecFactory specFactory;

  /**
   * Public constructor.
   *
   * @param store an {@link OAuthStore} that can store and retrieve OAuth
   *              tokens, as well as information about service providers.
   */
  @Inject
  public GadgetOAuthTokenStore(OAuthStore store, GadgetSpecFactory specFactory) {
    this.store = store;
    this.specFactory = specFactory;
  }

  /**
   * Retrieve an AccessorInfo and OAuthAccessor that are ready for signing OAuthMessages.  To do
   * this, we need to figure out:
   *
   * - what consumer key/secret to use for signing.
   * - if an access token should be used for the request, and if so what it is.   *
   * - the OAuth request/authorization/access URLs.
   * - what HTTP method to use for request token and access token requests
   * - where the OAuth parameters are located.
   * - Information from the OAuth Fetcher config to determine if owner pages are secure
   *
   * Note that most of that work gets skipped for signed fetch, we just look up the consumer key
   * and secret for that.  Signed fetch always sticks the parameters in the query string.
   */
  public AccessorInfo getOAuthAccessor(SecurityToken securityToken,
      OAuthArguments arguments, OAuthClientState clientState, OAuthResponseParams responseParams,
      OAuthFetcherConfig fetcherConfig)
      throws OAuthRequestException {

    AccessorInfoBuilder accessorBuilder = new AccessorInfoBuilder();

    // Pick up any additional information needed about the format of the request, either from
    // options to makeRequest, or options from the spec, or from sensible defaults.  This is how
    // we figure out where to put the OAuth parameters and what methods to use for the OAuth
    // URLs.
    OAuthServiceProvider provider = null;
    if (arguments.programmaticConfig()) {
      provider = loadProgrammaticConfig(arguments, accessorBuilder, responseParams);
    } else if (arguments.mayUseToken()) {
      provider = lookupSpecInfo(securityToken, arguments, accessorBuilder, responseParams);
    } else {
      // This is plain old signed fetch.
      accessorBuilder.setParameterLocation(AccessorInfo.OAuthParamLocation.URI_QUERY);
    }

    // What consumer key/secret should we use?
    ConsumerInfo consumer;
    try {
      consumer = store.getConsumerKeyAndSecret(
          securityToken, arguments.getServiceName(), provider);
      accessorBuilder.setConsumer(consumer);
    } catch (GadgetException e) {
      throw new OAuthRequestException(OAuthError.UNKNOWN_PROBLEM,
          "Unable to retrieve consumer key", e);
    }

    // Should we use the OAuth access token?  We never do this unless the client allows it, and
    // if owner == viewer or owner pages are secure.
    if (arguments.mayUseToken() && securityToken.getViewerId() != null) {
      if ((fetcherConfig != null && fetcherConfig.isViewerAccessTokensEnabled()) ||
          securityToken.getViewerId().equals(securityToken.getOwnerId())) {
        lookupToken(securityToken, consumer, arguments, clientState, accessorBuilder, responseParams);
      }
    }

    return accessorBuilder.create(responseParams);
  }

  /**
   * Lookup information contained in the gadget spec.
   */
  private OAuthServiceProvider lookupSpecInfo(SecurityToken securityToken, OAuthArguments arguments,
      AccessorInfoBuilder accessorBuilder, OAuthResponseParams responseParams)
      throws OAuthRequestException {
    GadgetSpec spec = findSpec(securityToken, arguments, responseParams);
    OAuthSpec oauthSpec = spec.getModulePrefs().getOAuthSpec();
    if (oauthSpec == null) {
      throw new OAuthRequestException(OAuthError.BAD_OAUTH_CONFIGURATION,
          "Failed to retrieve OAuth URLs, spec for gadget " +
          securityToken.getAppUrl() + " does not contain OAuth element.");
    }
    OAuthService service = oauthSpec.getServices().get(arguments.getServiceName());
    if (service == null) {
      throw new OAuthRequestException(OAuthError.BAD_OAUTH_CONFIGURATION,
          "Failed to retrieve OAuth URLs, spec for gadget does not contain OAuth service " +
          arguments.getServiceName() + ".  Known services: " +
          Joiner.on(',').join(oauthSpec.getServices().keySet()) + '.');
    }
    // In theory some one could specify different parameter locations for request token and
    // access token requests, but that's probably not useful.  We just use the request token
    // rules for everything.
    accessorBuilder.setParameterLocation(
        getStoreLocation(service.getRequestUrl().location, responseParams));
    accessorBuilder.setMethod(getStoreMethod(service.getRequestUrl().method, responseParams));
    return new OAuthServiceProvider(
        service.getRequestUrl().url.toJavaUri().toASCIIString(),
        service.getAuthorizationUrl().toJavaUri().toASCIIString(),
        service.getAccessUrl().url.toJavaUri().toASCIIString());
  }

  private OAuthServiceProvider loadProgrammaticConfig(OAuthArguments arguments,
      AccessorInfoBuilder accessorBuilder, OAuthResponseParams responseParams)
      throws OAuthRequestException {
    try {
      String paramLocationStr = arguments.getRequestOption(OAuthArguments.PARAM_LOCATION_PARAM, "");
      Location l = Location.parse(paramLocationStr);
      accessorBuilder.setParameterLocation(getStoreLocation(l, responseParams));

      String requestMethod = arguments.getRequestOption(OAuthArguments.REQUEST_METHOD_PARAM, "GET");
      Method m = Method.parse(requestMethod);
      accessorBuilder.setMethod(getStoreMethod(m, responseParams));

      String requestTokenUrl = arguments.getRequestOption(OAuthArguments.REQUEST_TOKEN_URL_PARAM);
      verifyUrl(requestTokenUrl, responseParams);
      String accessTokenUrl = arguments.getRequestOption(OAuthArguments.ACCESS_TOKEN_URL_PARAM);
      verifyUrl(accessTokenUrl, responseParams);

      String authorizationUrl = arguments.getRequestOption(OAuthArguments.AUTHORIZATION_URL_PARAM);
      verifyUrl(authorizationUrl, responseParams);
      return new OAuthServiceProvider(requestTokenUrl, authorizationUrl, accessTokenUrl);
    } catch (SpecParserException e) {
      // these exceptions have decent programmer readable messages
      throw new OAuthRequestException(OAuthError.BAD_OAUTH_CONFIGURATION,
          e.getMessage());
    }
  }

  private void verifyUrl(String url, OAuthResponseParams responseParams)
      throws OAuthRequestException {
    if (url == null) {
      return;
    }
    Uri uri;
    try {
      uri = Uri.parse(url);
    } catch (Throwable t) {
      throw new OAuthRequestException(OAuthError.INVALID_URL, url);
    }
    if (!uri.isAbsolute()) {
      throw new OAuthRequestException(OAuthError.INVALID_URL, url);
    }
  }

  /**
   * Figure out the OAuth token that should be used with this request.  We check for this in three
   * places.  In order of priority:
   *
   * 1) From information we cached on the client.
   *    We encrypt the token and cache on the client for performance.
   *
   * 2) From information we have in our persistent state.
   *    We persist the token server-side so we can look it up if necessary.
   *
   * 3) From information the gadget developer tells us to use (a preapproved request token.)
   *    Gadgets can be initialized with preapproved request tokens.  If the user tells the service
   *    provider they want to add a gadget to a gadget container site, the service provider can
   *    create a preapproved request token for that site and pass it to the gadget as a user
   *    preference.
   */
  private void lookupToken(SecurityToken securityToken, ConsumerInfo consumerInfo,
      OAuthArguments arguments, OAuthClientState clientState, AccessorInfoBuilder accessorBuilder,
      OAuthResponseParams responseParams)
      throws OAuthRequestException {
    if (clientState.getRequestToken() != null) {
      // We cached the request token on the client.
      accessorBuilder.setRequestToken(clientState.getRequestToken());
      accessorBuilder.setTokenSecret(clientState.getRequestTokenSecret());
    } else if (clientState.getAccessToken() != null) {
      // We cached the access token on the client
      accessorBuilder.setAccessToken(clientState.getAccessToken());
      accessorBuilder.setTokenSecret(clientState.getAccessTokenSecret());
      accessorBuilder.setSessionHandle(clientState.getSessionHandle());
      accessorBuilder.setTokenExpireMillis(clientState.getTokenExpireMillis());
    } else {
      // No useful client-side state, check persistent storage
      TokenInfo tokenInfo;
      try {
        tokenInfo = store.getTokenInfo(securityToken, consumerInfo,
            arguments.getServiceName(), arguments.getTokenName());
      } catch (GadgetException e) {
        throw new OAuthRequestException(OAuthError.UNKNOWN_PROBLEM,
            "Unable to retrieve access token", e);
      }
      if (tokenInfo != null && tokenInfo.getAccessToken() != null) {
        // We have an access token in persistent storage, use that.
        accessorBuilder.setAccessToken(tokenInfo.getAccessToken());
        accessorBuilder.setTokenSecret(tokenInfo.getTokenSecret());
        accessorBuilder.setSessionHandle(tokenInfo.getSessionHandle());
        accessorBuilder.setTokenExpireMillis(tokenInfo.getTokenExpireMillis());
      } else {
        // We don't have an access token yet, but the client sent us a (hopefully) preapproved
        // request token.
        accessorBuilder.setRequestToken(arguments.getRequestToken());
        accessorBuilder.setTokenSecret(arguments.getRequestTokenSecret());
      }
    }
  }

  private OAuthParamLocation getStoreLocation(Location location,
      OAuthResponseParams responseParams) throws OAuthRequestException {
    switch(location) {
    case HEADER:
      return OAuthParamLocation.AUTH_HEADER;
    case URL:
      return OAuthParamLocation.URI_QUERY;
    case BODY:
      return OAuthParamLocation.POST_BODY;
    }
    throw new OAuthRequestException(OAuthError.UNKNOWN_PARAMETER_LOCATION);
  }

  private HttpMethod getStoreMethod(Method method, OAuthResponseParams responseParams)
      throws OAuthRequestException {
    switch(method) {
    case GET:
      return HttpMethod.GET;
    case POST:
      return HttpMethod.POST;
    }
    throw new OAuthRequestException(OAuthError.UNSUPPORTED_HTTP_METHOD, method.toString());
  }

  private GadgetSpec findSpec(final SecurityToken securityToken, final OAuthArguments arguments,
      OAuthResponseParams responseParams) throws OAuthRequestException {
    try {
      GadgetContext context = new OAuthGadgetContext(securityToken, arguments);
      return specFactory.getGadgetSpec(context);
    } catch (IllegalArgumentException e) {
      throw new OAuthRequestException(OAuthError.UNKNOWN_PROBLEM,
          "Could not fetch gadget spec, gadget URI invalid.", e);
    } catch (GadgetException e) {
      throw new OAuthRequestException(OAuthError.UNKNOWN_PROBLEM,
          "Could not fetch gadget spec", e);
    }
  }

  /**
   * Store an access token for the given user/gadget/service/token name
   */
  public void storeTokenKeyAndSecret(SecurityToken securityToken, ConsumerInfo consumerInfo,
      OAuthArguments arguments, TokenInfo tokenInfo, OAuthResponseParams responseParams)
      throws OAuthRequestException {
    try {
      store.setTokenInfo(securityToken, consumerInfo, arguments.getServiceName(),
          arguments.getTokenName(), tokenInfo);
    } catch (GadgetException e) {
      throw new OAuthRequestException(OAuthError.UNKNOWN_PROBLEM,
          "Unable to store access token", e);
    }
  }

  /**
   * Remove an access token for the given user/gadget/service/token name
   */
  public void removeToken(SecurityToken securityToken, ConsumerInfo consumerInfo,
      OAuthArguments arguments, OAuthResponseParams responseParams) throws OAuthRequestException {
    try {
      store.removeToken(securityToken, consumerInfo, arguments.getServiceName(),
          arguments.getTokenName());
    } catch (GadgetException e) {
      throw new OAuthRequestException(OAuthError.UNKNOWN_PROBLEM,
          "Unable to remove access token", e);
    }
  }
}
