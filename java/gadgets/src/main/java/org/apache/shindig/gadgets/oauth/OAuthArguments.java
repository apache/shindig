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

import com.google.common.base.Objects;
import org.apache.shindig.common.Nullable;
import org.apache.shindig.gadgets.AuthType;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.spec.RequestAuthenticationInfo;

import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;

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
  private static final String RECEIVED_CALLBACK_PARAM = "OAUTH_RECEIVED_CALLBACK";

  // Experimental support for configuring OAuth without special parameters in the spec XML.
  public static final String PROGRAMMATIC_CONFIG_PARAM = "OAUTH_PROGRAMMATIC_CONFIG";
  public static final String REQUEST_METHOD_PARAM = "OAUTH_REQUEST_METHOD";
  public static final String PARAM_LOCATION_PARAM = "OAUTH_PARAM_LOCATION";
  public static final String REQUEST_TOKEN_URL_PARAM = "OAUTH_REQUEST_TOKEN_URL";
  public static final String ACCESS_TOKEN_URL_PARAM = "OAUTH_ACCESS_TOKEN_URL";
  public static final String AUTHORIZATION_URL_PARAM = "OAUTH_AUTHORIZATION_URL";

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

  /** Arbitrary name/value pairs associated with the request */
  private final Map<String, String> requestOptions = new TreeMap<String,String>(String.CASE_INSENSITIVE_ORDER);

  /** Whether the request is one for proxied content */
  private boolean proxiedContentRequest = false;

  /** Callback URL returned from service provider */
  private String receivedCallbackUrl = null;

  /**
   * Parse OAuthArguments from parameters to the makeRequest servlet.
   *
   * @param auth authentication type for the request
   * @param request servlet request
   * @throws GadgetException if any parameters are invalid.
   */
  public OAuthArguments(AuthType auth, HttpServletRequest request) throws GadgetException {
    useToken = parseUseToken(auth, getRequestParam(request, USE_TOKEN_PARAM, ""));
    serviceName = getRequestParam(request, SERVICE_PARAM, "");
    tokenName = getRequestParam(request, TOKEN_PARAM, "");
    requestToken = getRequestParam(request, REQUEST_TOKEN_PARAM, null);
    requestTokenSecret = getRequestParam(request, REQUEST_TOKEN_SECRET_PARAM, null);
    origClientState = getRequestParam(request, CLIENT_STATE_PARAM, null);
    bypassSpecCache = "1".equals(getRequestParam(request, BYPASS_SPEC_CACHE_PARAM, null));
    signOwner = Boolean.parseBoolean(getRequestParam(request, SIGN_OWNER_PARAM, "true"));
    signViewer = Boolean.parseBoolean(getRequestParam(request, SIGN_VIEWER_PARAM, "true"));
    receivedCallbackUrl = getRequestParam(request, RECEIVED_CALLBACK_PARAM, null);
    Enumeration<String> params = getParameterNames(request);
    while (params.hasMoreElements()) {
      String name = params.nextElement();
      requestOptions.put(name, request.getParameter(name));
    }
  }

  @SuppressWarnings("unchecked")
  private Enumeration<String> getParameterNames(HttpServletRequest request) {
    return request.getParameterNames();
  }

  /**
   * Parse OAuthArguments from parameters to Preload, proxied content rendering, and OSML tags.
   */
  public OAuthArguments(RequestAuthenticationInfo info) throws GadgetException {
    this(info.getAuthType(), info.getAttributes());

    origClientState = null;  // Client has no state for declarative calls
    bypassSpecCache = false; // too much trouble to copy nocache=1 from the request context to here.

    signOwner = info.isSignOwner();
    signViewer = info.isSignViewer();
  }

  /**
   * Parse OAuthArguments from a Map of settings
   */
  public OAuthArguments(AuthType auth,  Map<String, String> map) throws GadgetException {
    requestOptions.putAll(map);
    useToken = parseUseToken(auth, getAuthInfoParam(requestOptions, USE_TOKEN_PARAM, ""));
    serviceName = getAuthInfoParam(requestOptions, SERVICE_PARAM, "");
    tokenName = getAuthInfoParam(requestOptions, TOKEN_PARAM, "");
    requestToken = getAuthInfoParam(requestOptions, REQUEST_TOKEN_PARAM, null);
    requestTokenSecret = getAuthInfoParam(requestOptions, REQUEST_TOKEN_SECRET_PARAM, null);
    origClientState = getAuthInfoParam(requestOptions, CLIENT_STATE_PARAM, null);
    bypassSpecCache = "1".equals(getAuthInfoParam(requestOptions, BYPASS_SPEC_CACHE_PARAM, null));
    signOwner =  Boolean.parseBoolean(getAuthInfoParam(requestOptions, SIGN_OWNER_PARAM, "true"));
    signViewer = Boolean.parseBoolean(getAuthInfoParam(requestOptions, SIGN_VIEWER_PARAM, "true"));
    receivedCallbackUrl = getAuthInfoParam(requestOptions, RECEIVED_CALLBACK_PARAM, null);
  }


  /**
   * @return the named attribute from the Preload tag attributes, or default if the attribute is
   * not present.
   */
  private static String getAuthInfoParam(Map<String, String> attrs, String name, String def) {
    String val = attrs.get(name);
    if (val == null) {
      val = def;
    }
    return val;
  }

  /**
   * @return the named parameter from the request, or default if the named parameter is not present.
   */
  private static String getRequestParam(HttpServletRequest request, String name, @Nullable String def) {
    String val = request.getParameter(name);
    if (val == null) {
      val = def;
    }
    return val;
  }


  /**
   * Figure out what the client wants us to do with the OAuth access token.
   */
  private static UseToken parseUseToken(AuthType auth, String useTokenStr) throws GadgetException {
    if (useTokenStr.length() == 0) {
      if (auth == AuthType.SIGNED) {
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
        "Unknown use token value " + useTokenStr, HttpResponse.SC_BAD_REQUEST);
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
    requestOptions.putAll(orig.requestOptions);
    proxiedContentRequest = orig.proxiedContentRequest;
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

  public void setRequestOption(String name, String value) {
    requestOptions.put(name, value);
  }

  public void removeRequestOption(String name) {
    requestOptions.remove(name);
  }

  public String getRequestOption(String name) {
    return requestOptions.get(name);
  }

  public String getRequestOption(String name, String def) {
    String val = requestOptions.get(name);
    return (val != null ? val : def);
  }

  public boolean isProxiedContentRequest() {
    return proxiedContentRequest;
  }

  public void setProxiedContentRequest(boolean proxiedContentRequest) {
    this.proxiedContentRequest = proxiedContentRequest;
  }

  public boolean programmaticConfig() {
    return Boolean.parseBoolean(requestOptions.get(PROGRAMMATIC_CONFIG_PARAM));
  }

  public String getReceivedCallbackUrl() {
    return receivedCallbackUrl;
  }

  public void setReceivedCallbackUrl(String receivedCallbackUrl) {
    this.receivedCallbackUrl = receivedCallbackUrl;
  }

  @Override
  public int hashCode() {
      return Objects.hashCode(bypassSpecCache, origClientState, origClientState,
          proxiedContentRequest, requestToken, requestTokenSecret, requestTokenSecret,
          serviceName, serviceName, signOwner,
          signViewer, tokenName, useToken);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;

    if (!(obj instanceof OAuthArguments)) {
      return false;
    }

    OAuthArguments other = (OAuthArguments) obj;
    return (bypassSpecCache == other.bypassSpecCache
        && Objects.equal(origClientState, other.origClientState)
        && proxiedContentRequest == other.proxiedContentRequest
        && Objects.equal(requestToken, other.requestToken)
        && Objects.equal(requestTokenSecret, other.requestTokenSecret)
        && Objects.equal(tokenName, other.tokenName)
        && signViewer == other.signViewer
        && useToken == other.useToken);
  }
}
