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

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.ContainerConfig;
import org.apache.shindig.common.uri.Uri;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Creates HttpRequests. A new HttpRequest should be created for every unique HttpRequest
 * being constructed.
 */
public class HttpRequest {
  static final String DEFAULT_CONTENT_TYPE = "application/x-www-form-urlencoded; charset=utf-8";

  private String method = "GET";
  private Uri uri;
  private final Map<String, List<String>> headers = Maps.newTreeMap(String.CASE_INSENSITIVE_ORDER);
  private byte[] postBody = ArrayUtils.EMPTY_BYTE_ARRAY;

  // TODO: It might be useful to refactor these into a simple map of objects and use sub classes
  // for more detailed data.

  // Cache control.
  private boolean ignoreCache;
  private int cacheTtl = -1;

  // Context for the request.
  private Uri gadget;
  private String container = ContainerConfig.DEFAULT_CONTAINER;

  // For signed fetch & OAuth
  private SecurityToken securityToken;
  private boolean signOwner = true;
  private boolean signViewer = true;

  private String rewriteMimeType;

  /**
   * Construct a new request for the given uri.
   */
  public HttpRequest(Uri uri) {
    this.uri = uri;
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
    signOwner = request.signOwner;
    signViewer = request.signViewer;
    rewriteMimeType = request.rewriteMimeType;
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
    if (postBody == null) {
      this.postBody = ArrayUtils.EMPTY_BYTE_ARRAY;
    } else {
      this.postBody = new byte[postBody.length];
      System.arraycopy(postBody, 0, this.postBody, 0, postBody.length);
    }
    return this;
  }

  /**
   * Fills in the request body from an InputStream.
   */
  public HttpRequest setPostBody(InputStream is) throws IOException {
    postBody = IOUtils.toByteArray(is);
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
   * @param cacheTtl The amount of time to cache the result object for, in milliseconds. If set to
   * -1, HTTP cache control headers will be honored. Otherwise objects will be cached for the time
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
   * @param signOwner Whether to include the owner id when making authenticated requests. Defaults
   * to true.
   */
  public HttpRequest setSignOwner(boolean signOwner) {
    this.signOwner = signOwner;
    return this;
  }

  /**
   * @param signViewer Whether to include the viewer id when making authenticated requests. Defaults
   * to true.
   */
  public HttpRequest setSignViewer(boolean signViewer) {
    this.signViewer = signViewer;
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
    try {
      return new String(postBody, "UTF-8");
    } catch (UnsupportedEncodingException ignore) {
      return "";
    }
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
   * @return The amount of time to cache any response objects for, in milliseconds.
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
   * @return True if the owner id should be passed in the request parameters.
   */
  public boolean getSignOwner() {
    return signOwner;
  }

  /**
   * @return True if the viewer id should be passed in the request parameters.
   */
  public boolean getSignViewer() {
    return signViewer;
  }

  /**
   * @return The content type to assume when rewriting.
   *
   * TODO: Move this to new rewriting facility.
   */
  public String getRewriteMimeType() {
    return rewriteMimeType;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(method);
    buf.append(' ').append(uri.getPath())
       .append(uri.getQuery() == null ? "" : uri.getQuery()).append("\n\n");
    buf.append("Host: ").append(uri.getAuthority()).append('\n');
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
  public boolean equals(Object obj) {
    if (obj == this) {return true;}
    if (obj instanceof HttpRequest) {
      HttpRequest req = (HttpRequest)obj;
      return method.equals(req.method) &&
             uri.equals(req.uri) &&
             Arrays.equals(postBody, req.postBody) &&
             headers.equals(req.headers);
             // TODO: Verify that other fields aren't meaningful.
    }
    return false;
  }

}

