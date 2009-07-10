/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.gadgets.http;

import org.apache.shindig.common.util.CharsetUtil;
import org.apache.shindig.common.util.DateUtil;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import org.apache.commons.lang.ArrayUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;


/**
 * Constructs HttpResponse objects.
 */
public class HttpResponseBuilder {
  private int httpStatusCode = HttpResponse.SC_OK;
  private final Multimap<String, String> headers = HttpResponse.newHeaderMultimap();
  private byte[] responseBytes = ArrayUtils.EMPTY_BYTE_ARRAY;
  private final Map<String, String> metadata = Maps.newHashMap();

  public HttpResponseBuilder() {}

  public HttpResponseBuilder(HttpResponseBuilder builder) {
    httpStatusCode = builder.httpStatusCode;
    headers.putAll(builder.headers);
    metadata.putAll(builder.metadata);
    responseBytes = builder.responseBytes;
  }

  public HttpResponseBuilder(HttpResponse response) {
    httpStatusCode = response.getHttpStatusCode();

    headers.putAll(response.getHeaders());

    metadata.putAll(response.getMetadata());
    responseBytes = response.getResponseAsBytes();
  }

  /**
   * @return A new HttpResponse.
   */
  public HttpResponse create() {
    return new HttpResponse(this);
  }

  /**
   * @param body The response string.  Converted to UTF-8 bytes and copied when set.
   */
  public HttpResponseBuilder setResponseString(String body) {
    responseBytes = CharsetUtil.getUtf8Bytes(body);
    return this;
  }

  /**
   * @param responseBytes The response body. Copied when set.
   */
  public HttpResponseBuilder setResponse(byte[] responseBytes) {
    if (responseBytes == null) {
      responseBytes = ArrayUtils.EMPTY_BYTE_ARRAY;
    }
    this.responseBytes = new byte[responseBytes.length];
    System.arraycopy(responseBytes, 0, this.responseBytes, 0, responseBytes.length);
    return this;
  }

  /**
   * @param responseBytes The response body. Not copied when set.
   */
  public HttpResponseBuilder setResponseNoCopy(byte[] responseBytes) {
    if (responseBytes == null) {
      responseBytes = ArrayUtils.EMPTY_BYTE_ARRAY;
    }
    this.responseBytes = responseBytes;
    return this;
  }

  /**
   * @param httpStatusCode The HTTP response status, defined on HttpResponse.
   */
  public HttpResponseBuilder setHttpStatusCode(int httpStatusCode) {
    this.httpStatusCode = httpStatusCode;
    return this;
  }

  /**
   * Add a single header to the response. If a value for the given name is already set, a second
   * value is added. If you wish to overwrite any possible values for a header, use
   * {@link #setHeader(String, String)}.
   */
  public HttpResponseBuilder addHeader(String name, String value) {
    if (name != null) {
      headers.put(name, value);
    }
    return this;
  }

  /**
   * Sets a single header value, overwriting any previously set headers with the same name.
   */
  public HttpResponseBuilder setHeader(String name, String value) {
    if (name != null) {
      headers.replaceValues(name, Lists.newArrayList(value));
    }
    return this;
  }

  /**
   * Adds an entire map of headers to the response.
   */
  public HttpResponseBuilder addHeaders(Map<String, String> headers) {
    for (Map.Entry<String,String> entry : headers.entrySet()) {
      this.headers.put(entry.getKey(), entry.getValue());
    }
    return this;
  }

  /**
   * Adds all headers in the provided multimap to the response.
   */
  public HttpResponseBuilder addAllHeaders(Map<String, ? extends List<String>> headers) {
    for (Map.Entry<String,? extends List<String>> entry : headers.entrySet()) {
      this.headers.putAll(entry.getKey(), entry.getValue());
    }
    return this;
  }

  /**
   * Remove all headers with the given name from the response.
   *
   * @return Any values that were removed from the response.
   */
  public Collection<String> removeHeader(String name) {
    return headers.removeAll(name);
  }

  /**
   * @param cacheTtl The time to live for this response, in seconds.
   */
  public HttpResponseBuilder setCacheTtl(int cacheTtl) {
    headers.removeAll("Pragma");
    headers.removeAll("Expires");
    headers.replaceValues("Cache-Control", ImmutableList.of("public,max-age=" + cacheTtl));
    return this;
  }

  /**
   * @param expirationTime The expiration time for this response, in
   * milliseconds since the Unix epoch.
   */
  public HttpResponseBuilder setExpirationTime(long expirationTime) {
    headers.removeAll("Cache-Control");
    headers.removeAll("Pragma");
    headers.put("Expires", DateUtil.formatRfc1123Date(expirationTime));
    return this;
  }

  /**
   * Sets cache-control headers indicating the response is not cacheable.
   */
  private final List<String> NO_CACHE_HEADER = ImmutableList.of("no-cache");
  public HttpResponseBuilder setStrictNoCache() {
    headers.replaceValues("Cache-Control", NO_CACHE_HEADER);
    headers.replaceValues("Pragma", NO_CACHE_HEADER);
    headers.removeAll("Expires");
    return this;
  }

  /**
   * Adds a new piece of metadata to the response.
   */
  public HttpResponseBuilder setMetadata(String key, String value) {
    metadata.put(key, value);
    return this;
  }

  /**
   * Merges the given Map of metadata into the existing metadata.
   */
  public HttpResponseBuilder setMetadata(Map<String, String> metadata) {
    this.metadata.putAll(metadata);
    return this;
  }

  Multimap<String, String> getHeaders() {
    return headers;
  }

  Map<String, String> getMetadata() {
    return metadata;
  }

  byte[] getResponse() {
    return responseBytes;
  }

  public int getHttpStatusCode() {
    return httpStatusCode;
  }
}
