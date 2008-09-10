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
package org.apache.shindig.gadgets.oauth;

import com.google.inject.Inject;

import net.oauth.OAuthServiceProvider;

import org.apache.commons.lang.StringUtils;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.GadgetSpecFactory;
import org.apache.shindig.gadgets.oauth.AccessorInfo.HttpMethod;
import org.apache.shindig.gadgets.oauth.AccessorInfo.OAuthParamLocation;
import org.apache.shindig.gadgets.oauth.OAuthStore.ConsumerInfo;
import org.apache.shindig.gadgets.oauth.OAuthStore.TokenInfo;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.OAuthService;
import org.apache.shindig.gadgets.spec.OAuthSpec;
import org.apache.shindig.gadgets.spec.OAuthService.Location;
import org.apache.shindig.gadgets.spec.OAuthService.Method;

import java.net.URI;
import java.net.URISyntaxException;

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
   * 
   * Note that most of that work gets skipped for signed fetch, we just look up the consumer key
   * and secret for that.  Signed fetch always sticks the parameters in the query string.
   */
  public AccessorInfo getOAuthAccessor(SecurityToken securityToken,
      OAuthArguments arguments, OAuthClientState clientState) throws GadgetException {
    
    AccessorInfoBuilder accessorBuilder = new AccessorInfoBuilder();

    // Does the gadget spec tell us any details about the service provider, like where to put the
    // OAuth parameters and what methods to use for their URLs?
    OAuthServiceProvider provider = null;
    if (arguments.mayUseToken()) {
      provider = lookupSpecInfo(securityToken, arguments, accessorBuilder);
    } else {
      // This is plain old signed fetch.
      accessorBuilder.setParameterLocation(AccessorInfo.OAuthParamLocation.URI_QUERY);
    }
    
    // What consumer key/secret should we use?
    ConsumerInfo consumer = store.getConsumerKeyAndSecret(
        securityToken, arguments.getServiceName(), provider);
    accessorBuilder.setConsumer(consumer);

    // Should we use the OAuth access token?  We never do this unless the client allows it, and
    // if owner == viewer.
    if (arguments.mayUseToken()
        && securityToken.getOwnerId() != null
        && securityToken.getViewerId().equals(securityToken.getOwnerId())) {  
      lookupToken(securityToken, consumer, arguments, clientState, accessorBuilder);
    }
   
    return accessorBuilder.create();
  }
  
  /**
   * Lookup information contained in the gadget spec.
   */
  private OAuthServiceProvider lookupSpecInfo(SecurityToken securityToken, OAuthArguments arguments,
      AccessorInfoBuilder accessorBuilder) throws GadgetException {
    GadgetSpec spec = findSpec(securityToken, arguments);
    OAuthSpec oauthSpec = spec.getModulePrefs().getOAuthSpec();
    if (oauthSpec == null) {
      throw oauthNotFoundEx(securityToken);
    }
    OAuthService service = oauthSpec.getServices().get(arguments.getServiceName());
    if (service == null) {
      throw serviceNotFoundEx(securityToken, oauthSpec, arguments.getServiceName());
    }
    // In theory some one could specify different parameter locations for request token and
    // access token requests, but that's probably not useful.  We just use the request token
    // rules for everything.
    accessorBuilder.setParameterLocation(getStoreLocation(service.getRequestUrl().location));
    accessorBuilder.setMethod(getStoreMethod(service.getRequestUrl().method));
    OAuthServiceProvider provider = new OAuthServiceProvider(
        service.getRequestUrl().url.toASCIIString(),
        service.getAuthorizationUrl().toASCIIString(),
        service.getAccessUrl().url.toASCIIString());
    return provider;
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
   * @throws GadgetException 
   */
  private void lookupToken(SecurityToken securityToken, ConsumerInfo consumerInfo,
      OAuthArguments arguments, OAuthClientState clientState, AccessorInfoBuilder accessorBuilder)
      throws GadgetException {
    if (clientState.getRequestToken() != null) {
      // We cached the request token on the client.
      accessorBuilder.setRequestToken(clientState.getRequestToken());
      accessorBuilder.setTokenSecret(clientState.getRequestTokenSecret());
    } else if (clientState.getAccessToken() != null) {
      // We cached the access token on the client
      accessorBuilder.setAccessToken(clientState.getAccessToken());
      accessorBuilder.setTokenSecret(clientState.getAccessTokenSecret());
    } else {
      // No useful client-side state, check persistent storage
      TokenInfo tokenInfo = store.getTokenInfo(securityToken, consumerInfo,
          arguments.getServiceName(), arguments.getTokenName());
      if (tokenInfo != null && tokenInfo.getAccessToken() != null) {
        // We have an access token in persistent storage, use that.
        accessorBuilder.setAccessToken(tokenInfo.getAccessToken());
        accessorBuilder.setTokenSecret(tokenInfo.getTokenSecret());
      } else {
        // We don't have an access token yet, but the client sent us a (hopefully) preapproved
        // request token.
        accessorBuilder.setRequestToken(arguments.getRequestToken());
        accessorBuilder.setTokenSecret(arguments.getRequestTokenSecret());
      }
    }
  }

  private OAuthParamLocation getStoreLocation(Location location) throws GadgetException {
    switch(location) {
    case HEADER:
      return OAuthParamLocation.AUTH_HEADER;
    case URL:
      return OAuthParamLocation.URI_QUERY;
    case BODY:
      return OAuthParamLocation.POST_BODY;
    }
    throw new GadgetException(GadgetException.Code.INTERNAL_SERVER_ERROR,
        "Unknown parameter location " + location);
  }
  
  private HttpMethod getStoreMethod(Method method) throws GadgetException {
    switch(method) {
    case GET:
      return HttpMethod.GET;
    case POST:
      return HttpMethod.POST;
    }
    throw new GadgetException(GadgetException.Code.INTERNAL_SERVER_ERROR,
        "Unknown method " + method);
  }

  private GadgetSpec findSpec(SecurityToken securityToken, OAuthArguments arguments)
      throws GadgetException {
    try {
      return specFactory.getGadgetSpec(
          new URI(securityToken.getAppUrl()),
          arguments.getBypassSpecCache());
    } catch (URISyntaxException e) {
      throw new OAuthStoreException("could not fetch gadget spec, gadget URI invalid", e);
    }
  }
  
  private GadgetException serviceNotFoundEx(SecurityToken securityToken, OAuthSpec oauthSpec,
      String serviceName) {
    StringBuilder message = new StringBuilder()
        .append("Spec for gadget ")
        .append(securityToken.getAppUrl())
        .append(" does not contain OAuth service ")
        .append(serviceName)
        .append(".  Known services: ")
        .append(StringUtils.join(oauthSpec.getServices().keySet(), ','));
    return new GadgetException(GadgetException.Code.INVALID_PARAMETER,
        message.toString());
  }

  private GadgetException oauthNotFoundEx(SecurityToken securityToken) {
    StringBuilder message = new StringBuilder()
        .append("Spec for gadget ")
        .append(securityToken.getAppUrl())
        .append(" does not contain OAuth element.");
    return new GadgetException(GadgetException.Code.INVALID_PARAMETER,
        message.toString());
  }
  
  /**
   * Store an access token for the given user/gadget/service/token name
   */
  public void storeTokenKeyAndSecret(SecurityToken securityToken, ConsumerInfo consumerInfo,
      OAuthArguments arguments, TokenInfo tokenInfo) throws GadgetException {
    store.setTokenInfo(securityToken, consumerInfo, arguments.getServiceName(),
        arguments.getTokenName(), tokenInfo);
  }

  /**
   * Remove an access token for the given user/gadget/service/token name
   */
  public void removeToken(SecurityToken securityToken, ConsumerInfo consumerInfo,
      OAuthArguments arguments) throws GadgetException {
    store.removeToken(securityToken, consumerInfo, arguments.getServiceName(),
        arguments.getTokenName());
  }
}
