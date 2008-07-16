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

import javax.servlet.http.HttpServletRequest;

/**
 * Bundles information about a proxy request that requires OAuth
 */
public class OAuthRequestParams {
  public static final String SERVICE_PARAM = "OAUTH_SERVICE_NAME";
  public static final String TOKEN_PARAM = "OAUTH_TOKEN_NAME";
  public static final String REQUEST_TOKEN_PARAM = "OAUTH_REQUEST_TOKEN";
  public static final String REQUEST_TOKEN_SECRET_PARAM =
      "OAUTH_REQUEST_TOKEN_SECRET";
  public static final String CLIENT_STATE_PARAM = "oauthState";
  public static final String BYPASS_SPEC_CACHE_PARAM = "bypassSpecCache";

  protected final String serviceName;
  protected final String tokenName;
  protected final String requestToken;
  protected final String requestTokenSecret;
  protected final String origClientState;
  protected final boolean bypassSpecCache;

  public OAuthRequestParams(HttpServletRequest request) {
    serviceName = getParam(request, SERVICE_PARAM, "");
    tokenName = getParam(request, TOKEN_PARAM, "");
    requestToken = getParam(request, REQUEST_TOKEN_PARAM, null);
    requestTokenSecret = getParam(request, REQUEST_TOKEN_SECRET_PARAM, null);
    origClientState = getParam(request, CLIENT_STATE_PARAM, null);
    bypassSpecCache = parseBypassSpecCacheParam(request);
  }

  // Testing only
  public OAuthRequestParams(String serviceName, String tokenName,
      String origClientState, boolean bypassSpecCache) {
    this(serviceName, tokenName, origClientState, bypassSpecCache, null, null);
  }

  // Testing only
  public OAuthRequestParams(String serviceName, String tokenName,
      String origClientState, boolean bypassSpecCache, String requestToken,
      String requestTokenSecret) {
    this.serviceName = serviceName;
    this.tokenName = tokenName;
    this.requestToken = requestToken;
    this.requestTokenSecret = requestTokenSecret;
    this.origClientState = origClientState;
    this.bypassSpecCache = bypassSpecCache;
  }

  public String getServiceName() {
    return serviceName;
  }

  public String getTokenName() {
    return tokenName;
  }

  public String getOrigClientState() {
    return origClientState;
  }

  public boolean getBypassSpecCache() {
    return bypassSpecCache;
  }
  
  public String getRequestToken() {
    return requestToken;
  }
  
  public String getRequestTokenSecret() {
    return requestTokenSecret;
  }

  public static boolean parseBypassSpecCacheParam(HttpServletRequest request) {
    return "1".equals(request.getParameter(BYPASS_SPEC_CACHE_PARAM));
  }
  
  private String getParam(HttpServletRequest request, String name, String def) {
    String val = request.getParameter(name);
    if (val == null) {
      val = def;
    }
    return val;
  }
}
