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
package org.apache.shindig.gadgets.oauth.testing;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.util.CharsetUtil;
import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.oauth.OAuthArguments;
import org.apache.shindig.gadgets.oauth.OAuthFetcherConfig;
import org.apache.shindig.gadgets.oauth.OAuthRequest;
import org.apache.shindig.gadgets.oauth.OAuthArguments.UseToken;

import net.oauth.OAuth.Parameter;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Test utility to emulate the requests sent via gadgets.io.makeRequest.  The simulation starts
 * at what arrives at OAuthRequest.  Code above OAuthRequest (MakeRequestHandler, preloads) are not
 * exercised here.
 */
public class MakeRequestClient {

  private final SecurityToken securityToken;
  private final OAuthFetcherConfig fetcherConfig;
  private final FakeOAuthServiceProvider serviceProvider;
  private final String serviceName;
  private OAuthArguments baseArgs;
  private String oauthState;
  private String approvalUrl;
  private String receivedCallbackUrl;
  private boolean ignoreCache;
  private Map<String, String> trustedParams = Maps.newHashMap();
  private HttpFetcher nextFetcher;

  /**
   * Create a make request client with the given security token, sending requests through an
   * OAuth fetcher to an OAuth service provider.
   *
   * @param securityToken identity of the user.
   * @param fetcherConfig configuration for the OAuthRequest
   * @param serviceProvider service provider being targeted.
   * @param serviceName nickname for the service being accessed.
   */
  public MakeRequestClient(SecurityToken securityToken, OAuthFetcherConfig fetcherConfig,
      FakeOAuthServiceProvider serviceProvider, String serviceName) {
    this.securityToken = securityToken;
    this.fetcherConfig = fetcherConfig;
    this.serviceProvider = serviceProvider;
    this.serviceName = serviceName;
    this.baseArgs = makeNonSocialOAuthArguments();
    this.ignoreCache = false;
  }

  /**
   * Set the arguments to the OAuth fetch.
   */
  public void setBaseArgs(OAuthArguments baseArgs) {
    this.baseArgs = baseArgs;
  }

  public OAuthArguments getBaseArgs() {
    return baseArgs;
  }

  public void setIgnoreCache(boolean ignoreCache) {
    this.ignoreCache = ignoreCache;
  }

  public void setNextFetcher(HttpFetcher nextFetcher) {
    this.nextFetcher = nextFetcher;
  }

  public void setTrustedParam(String name, String value) {
    trustedParams.put(name, value);
  }

  private OAuthRequest createRequest() {
    HttpFetcher dest = serviceProvider;
    if (nextFetcher != null) {
      dest = nextFetcher;
    }
    if (trustedParams != null) {
      List<Parameter> trusted = Lists.newArrayList();
      for (Entry<String, String> e : trustedParams.entrySet()) {
        trusted.add(new Parameter(e.getKey(), e.getValue()));
      }
      return new OAuthRequest(fetcherConfig, dest, trusted);
    }
    return new OAuthRequest(fetcherConfig, dest);
  }

  /**
   * Send an OAuth GET request to the given URL.
   */
  public HttpResponse sendGet(String target) throws Exception {
    HttpRequest request = new HttpRequest(Uri.parse(target));
    request.setOAuthArguments(recallState());
    OAuthRequest dest = createRequest();
    request.setIgnoreCache(ignoreCache);
    request.setSecurityToken(securityToken);
    HttpResponse response = dest.fetch(request);
    saveState(response);
    return response;
  }

  // Yes, this is really allowed by the HTTP spec and supported by real servers.
  public HttpResponse sendGetWithBody(String target, String type, byte[] body) {
    HttpRequest request = new HttpRequest(Uri.parse(target));
    request.setOAuthArguments(recallState());
    OAuthRequest dest = createRequest();
    if (type != null) {
      request.setHeader("Content-Type", type);
    }
    request.setPostBody(body);
    request.setSecurityToken(securityToken);
    HttpResponse response = dest.fetch(request);
    saveState(response);
    return response;
  }

  /**
   * Send an OAuth POST request to the given URL.
   */
  public HttpResponse sendFormPost(String target, String body) throws Exception {
    HttpRequest request = new HttpRequest(Uri.parse(target));
    request.setOAuthArguments(recallState());
    OAuthRequest dest = createRequest();
    request.setMethod("POST");
    request.setPostBody(CharsetUtil.getUtf8Bytes(body));
    request.setHeader("content-type", "application/x-www-form-urlencoded");
    request.setSecurityToken(securityToken);
    HttpResponse response = dest.fetch(request);
    saveState(response);
    return response;
  }

  /**
   * Send an OAuth POST with binary data in the binary.
   */
  public HttpResponse sendRawPost(String target, String type, byte[] body) throws Exception {
    HttpRequest request = new HttpRequest(Uri.parse(target));
    request.setOAuthArguments(recallState());
    OAuthRequest dest = createRequest();
    request.setMethod("POST");
    if (type != null) {
      request.setHeader("Content-Type", type);
    }
    request.setPostBody(body);
    request.setSecurityToken(securityToken);
    HttpResponse response = dest.fetch(request);
    saveState(response);
    return response;
  }

  /**
   * Create arguments simulating authz=OAUTH.
   */
  public OAuthArguments makeNonSocialOAuthArguments() {
    OAuthArguments params = new OAuthArguments();
    params.setUseToken(UseToken.ALWAYS);
    params.setServiceName(serviceName);
    params.setSignOwner(false);
    params.setSignViewer(false);
    return params;
  }

  /**
   * Create arguments simulating authz=SIGNED.
   */
  public OAuthArguments makeSignedFetchArguments() {
    OAuthArguments params = new OAuthArguments();
    params.setUseToken(UseToken.NEVER);
    params.setSignOwner(true);
    params.setSignViewer(true);
    return params;
  }

  /**
   * Track state (see gadgets.io.makeRequest handling of the oauthState and received callback
   * parameters.
   */
  private OAuthArguments recallState() {
    OAuthArguments params = new OAuthArguments(baseArgs);
    params.setOrigClientState(oauthState);
    params.setReceivedCallbackUrl(receivedCallbackUrl);
    receivedCallbackUrl = null;
    return params;
  }

  /**
   * Track state (see gadgets.io.makeRequest handling of the oauthState parameter).
   */
  private void saveState(HttpResponse response) {
    approvalUrl = null;
    if (response.getMetadata() != null) {
      if (response.getMetadata().containsKey("oauthState")) {
        oauthState = response.getMetadata().get("oauthState");
      }
      approvalUrl = response.getMetadata().get("oauthApprovalUrl");
    }
  }

  /**
   * Simulate the user visiting the service provider and approved access to their data.
   */
  public void approveToken(String params) throws Exception {
    // This will throw if approvalUrl looks wrong.
    receivedCallbackUrl = serviceProvider.browserVisit(approvalUrl + '&' + params);
  }

  public void setReceivedCallbackUrl(String receivedCallbackUrl) {
    this.receivedCallbackUrl = receivedCallbackUrl;
  }

  public void clearState() {
    this.oauthState = null;
  }
}
