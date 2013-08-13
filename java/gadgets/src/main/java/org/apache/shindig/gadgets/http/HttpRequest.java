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
package org.apache.shindig.gadgets.http;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.util.CharsetUtil;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.gadgets.AuthType;
import org.apache.shindig.gadgets.admin.BasicGadgetAdminStore;
import org.apache.shindig.gadgets.oauth.OAuthArguments;
import org.apache.shindig.gadgets.oauth2.OAuth2Arguments;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Creates HttpRequests. A new HttpRequest should be created for every unique HttpRequest
 * being constructed.
 */
public class HttpRequest {
  private static final Logger LOG = Logger.getLogger(HttpRequest.class.getName());

  /** Automatically added to every request so that we know that the request came from our server. */
  public static final String DOS_PREVENTION_HEADER = "X-shindig-dos";
  static final String DEFAULT_CONTENT_TYPE = "application/x-www-form-urlencoded; charset=utf-8";

  private String method = "GET";
  private Uri uri;
  private final Map<String, List<String>> headers = new TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER);

  // Internal parameters which serve as extra information to pass along the
  // chain of HttpRequest processing.
  // NOTE: These are not get/post parameter equivalent of HttpServletRequest.
  private final Map<String, String> params = Maps.newHashMap();

  private byte[] postBody = ArrayUtils.EMPTY_BYTE_ARRAY;

  // TODO: It might be useful to refactor these into a simple map of objects and use sub classes
  // for more detailed data.

  // Cache control.
  private boolean ignoreCache;
  private int cacheTtl = -1;

  // Sanitization
  private boolean sanitizationRequested;

  // Caja
  private boolean cajaRequested;

  // Whether to follow redirects
  private boolean followRedirects = true;

  // Context for the request.
  private Uri gadget;
  private String container = ContainerConfig.DEFAULT_CONTAINER;

  // For signed fetch & OAuth
  private SecurityToken securityToken;

  // TODO: Move this into OAuthRequest.
  private OAuthArguments oauthArguments;
  private OAuth2Arguments oauth2Arguments;
  private AuthType authType;

  private String rewriteMimeType;
  private boolean internalRequest;

  /**
   * Construct a new request for the given uri.
   */
  public HttpRequest(Uri uri) {
    this.uri = uri;
    authType = AuthType.NONE;
    addHeader(DOS_PREVENTION_HEADER, "on");
  }

  /**
   * Clone an existing HttpRequest.
   */
  public HttpRequest(HttpRequest request) {
    method = request.method;
    uri = request.uri;
    headers.putAll(request.headers);
    postBody = request.postBody;
    ignoreCache = request.ignoreCache;
    cacheTtl = request.cacheTtl;
    gadget = request.gadget;
    container = request.container;
    securityToken = request.securityToken;
    if (request.oauthArguments != null) {
      oauthArguments = new OAuthArguments(request.oauthArguments);
    }
    if (request.oauth2Arguments != null) {
      oauth2Arguments = new OAuth2Arguments(request.oauth2Arguments);
    }
    authType = request.authType;
    rewriteMimeType = request.rewriteMimeType;
    followRedirects = request.followRedirects;
    internalRequest = request.internalRequest;
  }

  public HttpRequest setMethod(String method) {
    this.method = method;
    return this;
  }

  public HttpRequest setUri(Uri uri) {
    this.uri = uri;
    return this;
  }

  /**
   * Add a single header to the request. If a value for the given name is already set, a second
   * value is added. If you wish to overwrite any possible values for a header, use
   * {@link #setHeader(String, String)}.
   */
  public HttpRequest addHeader(String name, String value) {
    List<String> values = headers.get(name);
    if (values == null) {
      values = Lists.newArrayList();
      headers.put(name, values);
    }
    values.add(value);
    return this;
  }

  /**
   * Sets a single header value, overwriting any previously set headers with the same name.
   */
  public HttpRequest setHeader(String name, String value) {
    headers.put(name, Lists.newArrayList(value));
    return this;
  }

  /**
   * Adds an entire map of headers to the request.
   */
  public HttpRequest addHeaders(Map<String, String> headers) {
    for (Map.Entry<String, String> entry : headers.entrySet()) {
      addHeader(entry.getKey(), entry.getValue());
    }
    return this;
  }

  /**
   * Adds all headers in the provided map to the request.
   */
  public HttpRequest addAllHeaders(Map<String, ? extends List<String>> headers) {
    this.headers.putAll(headers);
    return this;
  }

  /**
   * Remove all headers with the given name from the request.
   *
   * @return Any values that were removed from the request.
   */
  public List<String> removeHeader(String name) {
    return headers.remove(name);
  }

  /**
   * Assigns the specified body to the request, copying all input bytes.
   */
  public HttpRequest setPostBody(byte[] postBody) {
    try {
      setPostBody(postBody == null ? null : new ByteArrayInputStream(postBody));
    } catch (IOException e){
      if (LOG.isLoggable(Level.WARNING)) {
        LOG.log(Level.WARNING, e.getMessage(), e);  // Shouldn't ever happen.
      }
    }
    return this;
  }

  /**
   * Fills in the request body from an InputStream.
   * @throws IOException
   */
  public HttpRequest setPostBody(InputStream is) throws IOException {
    if (postBody == null) {
      this.postBody = ArrayUtils.EMPTY_BYTE_ARRAY;
    } else {
      postBody = IOUtils.toByteArray(is);
    }
    return this;
  }

  /**
   * @param ignoreCache Whether to ignore all caching for this request.
   */
  public HttpRequest setIgnoreCache(boolean ignoreCache) {
    this.ignoreCache = ignoreCache;
    if (ignoreCache) {
      // Bypass any proxy caches as well.
      headers.put("Pragma", Lists.newArrayList("no-cache"));
    }
    return this;
  }

  /**
   * Should content fetched in response to this request
   * be sanitized based on the specified mime-type
   */
  public boolean isSanitizationRequested() {
    return sanitizationRequested;
  }

  public void setSanitizationRequested(boolean sanitizationRequested) {
    this.sanitizationRequested = sanitizationRequested;
  }

    /**
   * Should content fetched in response to this request
   * be sanitized based on the specified mime-type
   */
  public boolean isCajaRequested() {
    return cajaRequested;
  }

  public void setCajaRequested(boolean cajaRequested) {
    this.cajaRequested = cajaRequested;
  }

  /**
   * @param cacheTtl The amount of time to cache the result object for, in seconds. If set to -1,
   * HTTP cache control headers will be honored. Otherwise objects will be cached for the time
   * specified.
   */
  public HttpRequest setCacheTtl(int cacheTtl) {
    this.cacheTtl = cacheTtl;
    return this;
  }

  /**
   * @param gadget The gadget that caused this HTTP request to be necessary. May be null if the
   * request was not initiated by the actions of a gadget.
   */
  public HttpRequest setGadget(Uri gadget) {
    this.gadget = gadget;
    return this;
  }

  /**
   * @param container The container that this request originated from.
   */
  public HttpRequest setContainer(String container) {
    this.container = container;
    return this;
  }

  /**
   * Assign the security token to use for making any form of authenticated request.
   */
  public HttpRequest setSecurityToken(SecurityToken securityToken) {
    this.securityToken = securityToken;
    return this;
  }

  /**
   * @param oauthArguments arguments for OAuth/signed fetched
   */
  public HttpRequest setOAuthArguments(OAuthArguments oauthArguments) {
    this.oauthArguments = oauthArguments;
    return this;
  }

  /**
   * @param oauth2Arguments arguments for OAuth2/signed fetched
   */
  public HttpRequest setOAuth2Arguments(OAuth2Arguments oauth2Arguments) {
    this.oauth2Arguments = oauth2Arguments;
    return this;
  }

  /**
   * @param followRedirects whether this request should automatically follow redirects.
   */
  public HttpRequest setFollowRedirects(boolean followRedirects) {
    this.followRedirects = followRedirects;
    return this;
  }

  /**
   * @param authType The type of authentication being used for this request.
   */
  public HttpRequest setAuthType(AuthType authType) {
    this.authType = authType;
    return this;
  }

  /**
   * @param rewriteMimeType The assumed content type of the response to be rewritten. Overrides
   * any values set in the Content-Type response header.
   *
   * TODO: Move this to new rewriting facility.
   */
  public HttpRequest setRewriteMimeType(String rewriteMimeType) {
    this.rewriteMimeType = rewriteMimeType;
    return this;
  }

  public String getMethod() {
    return method;
  }

  public Uri getUri() {
    return uri;
  }

  /**
   * @return All headers to be sent in this request.
   */
  public Map<String, List<String>> getHeaders() {
    return headers;
  }

  /**
   * @param name The header to fetch
   * @return A list of headers with that name (may be empty).
   */
  public List<String> getHeaders(String name) {
    List<String> match = headers.get(name);
    if (match == null) {
      return Collections.emptyList();
    } else {
      return match;
    }
  }

  /**
   * @return The first set header with the given name or null if not set. If
   *         you need multiple values for the header, use getHeaders().
   */
  public String getHeader(String name) {
    List<String> headerList = getHeaders(name);
    if (headerList.isEmpty()) {
      return null;
    } else {
      return headerList.get(0);
    }
  }

  /**
   * @return The content type of the request (determined from request headers)
   */
  public String getContentType() {
    String type = getHeader("Content-Type");
    if (type == null) {
      return DEFAULT_CONTENT_TYPE;
    }
    return type;
  }

  /**
   * @return An input stream that can be used to read the post body.
   */
  public InputStream getPostBody() {
    return new ByteArrayInputStream(postBody);
  }

  /**
   * @return The post body as a string, assuming UTF-8 encoding.
   * TODO: We should probably tolerate other encodings, based on the
   *     Content-Type header.
   */
  public String getPostBodyAsString() {
    return CharsetUtil.newUtf8String(postBody);
  }

  /**
   * Retrieves the total length of the post body.
   *
   * @return The length of the post body.
   */
  public int getPostBodyLength() {
    return postBody.length;
  }

  /**
   * @return True if caching should be ignored for this request.
   */
  public boolean getIgnoreCache() {
    return ignoreCache;
  }

  /**
   * @return The amount of time to cache any response objects for, in seconds.
   */
  public int getCacheTtl() {
    return cacheTtl;
  }

  /**
   * @return The uri of gadget responsible for making this request.
   */
  public Uri getGadget() {
    return gadget;
  }

  public String getParam(String paramName) {
    return params.get(paramName);
  }

  public Integer getParamAsInteger(String paramName) {
    String value = params.get(paramName);
    if (value == null) {
      return null;
    }
    return NumberUtils.createInteger(value);
  }

  public <T> void setParam(String paramName, T paramValue) {
    params.put(paramName,  (paramValue == null) ? null : String.valueOf(paramValue));
  }

  public Map<String, String> getParams() {
    return params;
  }

  /**
   * @return The container responsible for making this request.
   */
  public String getContainer() {
    return container;
  }

  /**
   * @return The security token used to make this request.
   */
  public SecurityToken getSecurityToken() {
    return securityToken;
  }

  /**
   * @return arguments for OAuth and signed fetch
   */
  public OAuthArguments getOAuthArguments() {
    return oauthArguments;
  }

  /**
   * @return arguments for OAuth2 and signed fetch
   */
  public OAuth2Arguments getOAuth2Arguments() {
    return oauth2Arguments;
  }


  /**
   * @return true if redirects should be followed.
   */
  public boolean getFollowRedirects() {
    return followRedirects;
  }

  /**
   * @return The type of authentication being used for this request.
   */
  public AuthType getAuthType() {
    return authType;
  }

  /**
   * @return The content type to assume when rewriting.
   *
   * TODO: Move this to new rewriting facility.
   */
  public String getRewriteMimeType() {
    return rewriteMimeType;
  }

  /**
   * @return true if this is an internal request, false otherwise
   */
  public boolean isInternalRequest() {
    return internalRequest;
  }

  /**
   * An internal request is one created by the server to satisfy global server requirements.
   * Examples are retrieving the RPC methods, loading features, or rewriting requests pulling in
   * external content (that are driven back through the proxy to be completed).  SecurityTokens would typically
   * refer to a gadget as the source of the request, whereas the server initiated requests are occurring on behalf
   * of the server, and not on behalf of a specific gadget.
   * @param internalRequest Marks the request object as internal.
   * @return HttpRequest A self-reference
   */
  public HttpRequest setInternalRequest(boolean internalRequest) {
    this.internalRequest = internalRequest;
    return this;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(method);
    buf.append(' ').append(uri.getPath())
       .append(uri.getQuery() == null ? "" : '?' + uri.getQuery()).append("\n\n");
    buf.append("Host: ").append(uri.getAuthority()).append('\n');
    buf.append("X-Shindig-AuthType: ").append(authType).append('\n');
    for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
      String name = entry.getKey();
      for (String value : entry.getValue()) {
        buf.append(name).append(": ").append(value).append('\n');
      }
    }
    buf.append('\n');
    buf.append(getPostBodyAsString());

    return buf.toString();
  }

  @Override
  public int hashCode() {
    return method.hashCode()
      ^ uri.hashCode()
      ^ authType.hashCode()
      ^ Arrays.hashCode(postBody)
      ^ headers.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof HttpRequest)) {
      return false;
    }
    HttpRequest req = (HttpRequest)obj;
    return method.equals(req.method) &&
            uri.equals(req.uri) &&
            authType == req.authType &&
            Arrays.equals(postBody, req.postBody) &&
            headers.equals(req.headers);
    // TODO: Verify that other fields aren't meaningful. Especially important to check for oauth args.
  }
}

