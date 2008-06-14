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
  public static final String SERVICE_PARAM = "oauthService";
  public static final String TOKEN_PARAM = "oauthToken";
  public static final String CLIENT_STATE_PARAM = "oauthState";
  public static final String BYPASS_SPEC_CACHE_PARAM = "bypassSpecCache";

  protected final String serviceName;
  protected final String tokenName;
  protected final String origClientState;
  protected final boolean bypassSpecCache;

  public OAuthRequestParams(HttpServletRequest request) {
    serviceName = request.getParameter(SERVICE_PARAM);
    tokenName = request.getParameter(TOKEN_PARAM);
    origClientState = request.getParameter(CLIENT_STATE_PARAM);
    bypassSpecCache = parseBypassSpecCacheParam(request);
  }

  // Really only use this for testing, please
  public OAuthRequestParams(String serviceName, String tokenName,
      String origClientState, boolean bypassSpecCache) {
    this.serviceName = serviceName;
    this.tokenName = tokenName;
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

  public static boolean parseBypassSpecCacheParam(HttpServletRequest request) {
    return "1".equals(request.getParameter(BYPASS_SPEC_CACHE_PARAM));
  }
}
