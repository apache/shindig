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

import java.util.Map;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;

import org.apache.shindig.gadgets.spec.Preload;

/**
 * Arguments to an OAuth fetch sent by the client.
 */
public class OAuthArguments {
  public static final String SERVICE_PARAM = "OAUTH_SERVICE_NAME";
  public static final String TOKEN_PARAM = "OAUTH_TOKEN_NAME";
  public static final String REQUEST_TOKEN_PARAM = "OAUTH_REQUEST_TOKEN";
  public static final String REQUEST_TOKEN_SECRET_PARAM =
      "OAUTH_REQUEST_TOKEN_SECRET";
  public static final String CLIENT_STATE_PARAM = "oauthState";
  public static final String BYPASS_SPEC_CACHE_PARAM = "bypassSpecCache";

  protected String serviceName;
  protected String tokenName;
  protected String requestToken;
  protected String requestTokenSecret;
  protected String origClientState;
  protected boolean bypassSpecCache;

  @SuppressWarnings("unchecked")
  public OAuthArguments(HttpServletRequest request) {
    Map<String, String> params = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
    Map<String, String[]> reqParams = request.getParameterMap();
    for (String name : reqParams.keySet()) {
      params.put(name, reqParams.get(name)[0]);
    }
    init(params);
  }
  
  public OAuthArguments(Preload preload) {
    Map<String, String> attrs = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
    attrs.putAll(preload.getAttributes());
    init(attrs);
  }
  
  private void init(Map<String, String> attrs) {
    serviceName = getParam(attrs, SERVICE_PARAM, "");
    tokenName = getParam(attrs, TOKEN_PARAM, "");
    requestToken = getParam(attrs, REQUEST_TOKEN_PARAM, null);
    requestTokenSecret = getParam(attrs, REQUEST_TOKEN_SECRET_PARAM, null);
    origClientState = getParam(attrs, CLIENT_STATE_PARAM, null);
    bypassSpecCache = "1".equals(getParam(attrs, BYPASS_SPEC_CACHE_PARAM, null));
  }
  
  private String getParam(Map<String, String> attrs, String name, String def) {
    String val = attrs.get(name);
    if (val == null) {
      val = def;
    }
    return val;
  }

  // Testing only
  public OAuthArguments(String serviceName, String tokenName,
      String origClientState, boolean bypassSpecCache) {
    this(serviceName, tokenName, origClientState, bypassSpecCache, null, null);
  }

  // Testing only
  public OAuthArguments(String serviceName, String tokenName,
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
}
