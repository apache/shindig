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

import com.google.common.collect.Maps;

import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.spec.Auth;
import org.apache.shindig.gadgets.spec.Preload;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

/**
 * Arguments to an OAuth fetch sent by the client.
 */
public class OAuthArguments {
  private static final String SERVICE_PARAM = "OAUTH_SERVICE_NAME";
  private static final String TOKEN_PARAM = "OAUTH_TOKEN_NAME";
  private static final String REQUEST_TOKEN_PARAM = "OAUTH_REQUEST_TOKEN";
  private static final String REQUEST_TOKEN_SECRET_PARAM = "OAUTH_REQUEST_TOKEN_SECRET";
  private static final String USE_TOKEN_PARAM = "OAUTH_USE_TOKEN";
  private static final String CLIENT_STATE_PARAM = "oauthState";
  private static final String BYPASS_SPEC_CACHE_PARAM = "bypassSpecCache";
  private static final String SIGN_OWNER_PARAM = "signOwner";
  private static final String SIGN_VIEWER_PARAM = "signViewer";
 
  /**
   * Should the OAuth access token be used?
   */
  public static enum UseToken {
    /** Do not use the OAuth access token */
    NEVER,
    /** Use the access token if it exists already, but don't prompt for permission */
    IF_AVAILABLE,
    /** Use the access token if it exists, and prompt if it doesn't */
    ALWAYS,
  }
  
  /** Should we attempt to use an access token for the request */
  private UseToken useToken = UseToken.ALWAYS;
  
  /** OAuth service nickname.  Signed fetch uses the empty string */
  private String serviceName = "";
  
  /** OAuth token nickname.  Signed fetch uses the empty string */
  private String tokenName = "";
  
  /** Request token the client wants us to use, may be null */
  private String requestToken = null;
  
  /** Token secret that goes with the request token */
  private String requestTokenSecret = null;
  
  /** Encrypted state blob stored on the client */
  private String origClientState = null;
  
  /** Whether we should bypass the gadget spec cache */
  private boolean bypassSpecCache = false;
  
  /** Include information about the owner? */
  private boolean signOwner = false;
  
  /** Include information about the viewer? */
  private boolean signViewer = false;

  /**
   * Parse OAuthArguments from parameters to the makeRequest servlet.
   * 
   * @param auth authentication type for the request
   * @param request servlet request
   * @throws GadgetException if any parameters are invalid.
   */
  public OAuthArguments(Auth auth, HttpServletRequest request) throws GadgetException {
    useToken = parseUseToken(auth, getRequestParam(request, USE_TOKEN_PARAM, ""));
    serviceName = getRequestParam(request, SERVICE_PARAM, "");
    tokenName = getRequestParam(request, TOKEN_PARAM, "");
    requestToken = getRequestParam(request, REQUEST_TOKEN_PARAM, null);
    requestTokenSecret = getRequestParam(request, REQUEST_TOKEN_SECRET_PARAM, null);
    origClientState = getRequestParam(request, CLIENT_STATE_PARAM, null);
    bypassSpecCache = "1".equals(getRequestParam(request, BYPASS_SPEC_CACHE_PARAM, null));
    signOwner = Boolean.parseBoolean(getRequestParam(request, SIGN_OWNER_PARAM, "true"));
    signViewer = Boolean.parseBoolean(getRequestParam(request, SIGN_VIEWER_PARAM, "true"));
  }
  
  public OAuthArguments(Preload preload) throws GadgetException {
    Map<String, String> attrs = Maps.newTreeMap(String.CASE_INSENSITIVE_ORDER);
    attrs.putAll(preload.getAttributes());
    useToken = parseUseToken(preload.getAuthType(), getPreloadParam(attrs, USE_TOKEN_PARAM, ""));
    serviceName = getPreloadParam(attrs, SERVICE_PARAM, "");
    tokenName = getPreloadParam(attrs, TOKEN_PARAM, "");
    requestToken = getPreloadParam(attrs, REQUEST_TOKEN_PARAM, null);
    requestTokenSecret = getPreloadParam(attrs, REQUEST_TOKEN_SECRET_PARAM, null);
    origClientState = null;
    bypassSpecCache = false;
    signOwner = preload.isSignOwner();
    signViewer = preload.isSignViewer();
  }
  
  /**
   * @return the named attribute from the Preload tag attributes, or default if the attribute is
   * not present.
   */
  private String getPreloadParam(Map<String, String> attrs, String name, String def) {
    String val = attrs.get(name);
    if (val == null) {
      val = def;
    }
    return val;
  }

  /**
   * @return the named parameter from the request, or default if the named parameter is not present.
   */
  private static String getRequestParam(HttpServletRequest request, String name, String def) {
    String val = request.getParameter(name);
    if (val == null) {
      val = def;
    }
    return val;
  }


  /**
   * Figure out what the client wants us to do with the OAuth access token.
   */
  private static UseToken parseUseToken(Auth auth, String useTokenStr) throws GadgetException {
    if (useTokenStr.length() == 0) {
      if (auth == Auth.SIGNED) {
        // signed fetch defaults to not using the token
        return UseToken.NEVER;
      } else {
        // OAuth defaults to always using it.
        return UseToken.ALWAYS;
      }
    }
    useTokenStr = useTokenStr.toLowerCase();
    if ("always".equals(useTokenStr)) {
      return UseToken.ALWAYS;
    }
    if ("if_available".equals(useTokenStr)) {
      return UseToken.IF_AVAILABLE;
    }
    if ("never".equals(useTokenStr)) {
      return UseToken.NEVER;
    }
    throw new GadgetException(GadgetException.Code.INVALID_PARAMETER,
        "Unknown use token value " + useTokenStr);
  }
  
  /**
   * Create an OAuthArguments object with all default values.  The details can be filled in later
   * using the setters.
   * 
   * Be careful using this in anything except test code.  If you find yourself wanting to use this
   * method in real code, consider writing a new constructor instead.
   */
  public OAuthArguments() {
  }
  
  
  /**
   * Copy constructor.
   */
  public OAuthArguments(OAuthArguments orig) {
    useToken = orig.useToken;
    serviceName = orig.serviceName;
    tokenName = orig.tokenName;
    requestToken = orig.requestToken;
    requestTokenSecret = orig.requestTokenSecret;
    origClientState = orig.origClientState;
    bypassSpecCache = orig.bypassSpecCache;
    signOwner = orig.signOwner;
    signViewer = orig.signViewer;
  }

  public boolean mustUseToken() {
    return (useToken == UseToken.ALWAYS);
  }
  
  public boolean mayUseToken() {
    return (useToken == UseToken.IF_AVAILABLE || useToken == UseToken.ALWAYS);
  }

  public UseToken getUseToken() {
    return useToken;
  }

  public void setUseToken(UseToken useToken) {
    this.useToken = useToken;
  }

  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  public String getTokenName() {
    return tokenName;
  }

  public void setTokenName(String tokenName) {
    this.tokenName = tokenName;
  }

  public String getRequestToken() {
    return requestToken;
  }

  public void setRequestToken(String requestToken) {
    this.requestToken = requestToken;
  }

  public String getRequestTokenSecret() {
    return requestTokenSecret;
  }

  public void setRequestTokenSecret(String requestTokenSecret) {
    this.requestTokenSecret = requestTokenSecret;
  }

  public String getOrigClientState() {
    return origClientState;
  }

  public void setOrigClientState(String origClientState) {
    this.origClientState = origClientState;
  }

  public boolean getBypassSpecCache() {
    return bypassSpecCache;
  }

  public void setBypassSpecCache(boolean bypassSpecCache) {
    this.bypassSpecCache = bypassSpecCache;
  }

  public boolean getSignOwner() {
    return signOwner;
  }

  public void setSignOwner(boolean signOwner) {
    this.signOwner = signOwner;
  }

  public boolean getSignViewer() {
    return signViewer;
  }

  public void setSignViewer(boolean signViewer) {
    this.signViewer = signViewer;
  }
}
