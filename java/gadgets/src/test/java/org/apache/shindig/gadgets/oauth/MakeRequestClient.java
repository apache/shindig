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
import org.apache.shindig.common.util.CharsetUtil;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.oauth.OAuthArguments.UseToken;

/**
 * 
 */
public class MakeRequestClient {
  
  private final SecurityToken securityToken;
  private final OAuthFetcherConfig fetcherConfig;
  private final FakeOAuthServiceProvider serviceProvider;
  private final String serviceName;
  private OAuthArguments baseArgs;
  private String oauthState;
  private String approvalUrl;
  private boolean ignoreCache;

  public MakeRequestClient(SecurityToken securityToken, OAuthFetcherConfig fetcherConfig,
      FakeOAuthServiceProvider serviceProvider, String serviceName) {
    this.securityToken = securityToken;
    this.fetcherConfig = fetcherConfig;
    this.serviceProvider = serviceProvider;
    this.serviceName = serviceName;
    this.baseArgs = makeNonSocialOAuthArguments();
    this.ignoreCache = false;
  }
  
  public void setBaseArgs(OAuthArguments baseArgs) {
    this.baseArgs = baseArgs;
  }
  
  public OAuthArguments getBaseArgs() {
    return baseArgs;
  }
  
  public void setIgnoreCache(boolean ignoreCache) {
    this.ignoreCache = ignoreCache;
  }
  
  public HttpResponse sendGet(String target) throws Exception {
    HttpRequest request = new HttpRequest(Uri.parse(target));
    request.setOAuthArguments(recallState());
    OAuthFetcher dest = new OAuthFetcher(fetcherConfig, serviceProvider, request);
    request.setIgnoreCache(ignoreCache);
    request.setSecurityToken(securityToken);
    HttpResponse response = dest.fetch(request);
    saveState(response);
    return response;
  }
  
  public HttpResponse sendFormPost(String target, String body) throws Exception {
    HttpRequest request = new HttpRequest(Uri.parse(target));
    request.setOAuthArguments(recallState());
    OAuthFetcher dest = new OAuthFetcher(fetcherConfig, serviceProvider, request);
    request.setMethod("POST");
    request.setPostBody(CharsetUtil.getUtf8Bytes(body));
    request.setHeader("content-type", "application/x-www-form-urlencoded");
    request.setSecurityToken(securityToken);
    HttpResponse response = dest.fetch(request);
    saveState(response);
    return response;
  }
  
  public OAuthArguments makeNonSocialOAuthArguments() {
    OAuthArguments params = new OAuthArguments();
    params.setUseToken(UseToken.ALWAYS);
    params.setServiceName(serviceName);
    params.setSignOwner(false);
    params.setSignViewer(false);
    return params;
  }
  
  public OAuthArguments makeSignedFetchArguments() {
    OAuthArguments params = new OAuthArguments();
    params.setUseToken(UseToken.NEVER);
    params.setSignOwner(true);
    params.setSignViewer(true);
    return params;
  }
    
  private OAuthArguments recallState() {
    OAuthArguments params = new OAuthArguments(baseArgs);
    params.setOrigClientState(oauthState);
    return params;
  }
  
  private void saveState(HttpResponse response) {
    approvalUrl = null;
    if (response.getMetadata() != null) {
      if (response.getMetadata().containsKey("oauthState")) {
        oauthState = response.getMetadata().get("oauthState");
      }
      approvalUrl = response.getMetadata().get("oauthApprovalUrl");
    }
  }
  
  public void approveToken(String params) throws Exception {
    // This will throw if approvalUrl looks wrong.
    serviceProvider.browserVisit(approvalUrl + "&" + params);
  }
}
