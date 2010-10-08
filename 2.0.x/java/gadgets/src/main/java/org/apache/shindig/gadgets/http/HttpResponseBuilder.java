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

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.shindig.common.util.CharsetUtil;
import org.apache.shindig.common.util.DateUtil;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.parse.GadgetHtmlParser;
import org.apache.shindig.gadgets.rewrite.MutableContent;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.Map;


/**
 * Constructs HttpResponse objects.
 */
public class HttpResponseBuilder extends MutableContent {
  private int httpStatusCode = HttpResponse.SC_OK;
  private final Multimap<String, String> headers = HttpResponse.newHeaderMultimap();
  private final Map<String, String> metadata = Maps.newHashMap();
  
  // Stores the HttpResponse object, if any, from which this Builder is constructed.
  // This allows us to avoid creating a new HttpResponse in create() when no changes
  // have been made.
  private HttpResponse responseObj;
  private int responseObjNumChanges;
  
  public HttpResponseBuilder(GadgetHtmlParser parser, HttpResponse response) {
    super(parser, response);
    if (response != null) {
      httpStatusCode = response.getHttpStatusCode();
      headers.putAll(response.getHeaders());
      metadata.putAll(response.getMetadata());
    } else {
      setResponse(null);
    }
    responseObj = response;
    responseObjNumChanges = getNumChanges();
  }
  
  public HttpResponseBuilder() {
    this(unsupportedParser(), null);
  }

  public HttpResponseBuilder(HttpResponseBuilder builder) {
    this(unsupportedParser(), builder.create());
  }

  public HttpResponseBuilder(HttpResponse response) {
    this(unsupportedParser(), response);
  }

  /**
   * @return A new HttpResponse.
   */
  public HttpResponse create() {
    if (getNumChanges() != responseObjNumChanges || responseObj == null) {
      // Short-circuit the creation process: no need to create a
      // new (immutable) HttpResponse object when no modifications occurred.
      responseObj = new HttpResponse(this);
      responseObjNumChanges = getNumChanges();
    }
    return responseObj;
  }

  /**
   * @param body The response string.  Converted to UTF-8 bytes and copied when set.
   */
  public HttpResponseBuilder setResponseString(String body) {
    setContentBytes(CharsetUtil.getUtf8Bytes(body), Charsets.UTF_8);
    return this;
  }

  public HttpResponseBuilder setEncoding(Charset charset) {
    Collection<String> values = headers.get("Content-Type");
    if (!values.isEmpty()) {
      String contentType = values.iterator().next();
      StringBuilder newContentTypeBuilder = new StringBuilder("");
      // Remove previously set charset:
      String[] parts = StringUtils.split(contentType, ';');
      for (String part : parts) {
        if (!part.contains("charset=")) {
          if (newContentTypeBuilder.length() > 0) {
            newContentTypeBuilder.append("; ");
          }
          newContentTypeBuilder.append(part);
        }
      }
      if (charset != null) {
        if (newContentTypeBuilder.length() > 0) {
          newContentTypeBuilder.append("; ");
        }
        newContentTypeBuilder.append("charset=").append(charset.name());
      }
      values.clear();
      String newContentType = newContentTypeBuilder.toString();
      values.add(newContentType);
      if (!(values.size() == 1 && !contentType.equals(newContentType))) {
        incrementNumChanges();
      }
    }
    return this;
  }

  /**
   * @param responseBytes The response body. Copied when set.
   */
  public HttpResponseBuilder setResponse(byte[] responseBytes) {
    if (responseBytes == null) {
      responseBytes = ArrayUtils.EMPTY_BYTE_ARRAY;
    }
    byte[] newBytes = new byte[responseBytes.length];
    System.arraycopy(responseBytes, 0, newBytes, 0, responseBytes.length);
    setContentBytes(newBytes);
    return this;
  }

  /**
   * @param responseBytes The response body. Not copied when set.
   */
  public HttpResponseBuilder setResponseNoCopy(byte[] responseBytes) {
    if (responseBytes == null) {
      responseBytes = ArrayUtils.EMPTY_BYTE_ARRAY;
    }
    setContentBytes(responseBytes);
    return this;
  }

  public HttpResponseBuilder setHttpStatusCode(int httpStatusCode) {
    if (this.httpStatusCode != httpStatusCode) {
      this.httpStatusCode = httpStatusCode;
      incrementNumChanges();
    }
    return this;
  }
  
  public HttpResponseBuilder clearAllHeaders() {
    incrementNumChanges();
    headers.clear();
    return this;
  }

  public HttpResponseBuilder addHeader(String name, String value) {
    if (name != null) {
      headers.put(name, value);
      incrementNumChanges();
    }
    return this;
  }

  public HttpResponseBuilder setHeader(String name, String value) {
    if (name != null) {
      headers.replaceValues(name, Lists.newArrayList(value));
      incrementNumChanges();
    }
    return this;
  }

  public String getHeader(String name) {
    if (name != null && headers.containsKey(name)) {
      return headers.get(name).iterator().next();
    }
    return null;
  }

  public HttpResponseBuilder addHeaders(Map<String, String> headers) {
    for (Map.Entry<String,String> entry : headers.entrySet()) {
      this.headers.put(entry.getKey(), entry.getValue());
      incrementNumChanges();
    }
    return this;
  }

  public HttpResponseBuilder addAllHeaders(Map<String, ? extends List<String>> headers) {
    for (Map.Entry<String,? extends List<String>> entry : headers.entrySet()) {
      this.headers.putAll(entry.getKey(), entry.getValue());
      incrementNumChanges();
    }
    return this;
  }

  public Collection<String> removeHeader(String name) {
    Collection<String> ret = headers.removeAll(name);
    if (ret != null) {
      incrementNumChanges();
    }
    return ret;
  }

  public HttpResponseBuilder setCacheTtl(int cacheTtl) {
    headers.removeAll("Pragma");
    headers.removeAll("Expires");
    headers.replaceValues("Cache-Control", ImmutableList.of("public,max-age=" + cacheTtl));
    incrementNumChanges();
    return this;
  }

  public HttpResponseBuilder setExpirationTime(long expirationTime) {
    headers.removeAll("Cache-Control");
    headers.removeAll("Pragma");
    headers.put("Expires", DateUtil.formatRfc1123Date(expirationTime));
    incrementNumChanges();
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
    incrementNumChanges();
    return this;
  }

  public HttpResponseBuilder setMetadata(String key, String value) {
    metadata.put(key, value);
    incrementNumChanges();
    return this;
  }

  public HttpResponseBuilder setMetadata(Map<String, String> metadata) {
    this.metadata.putAll(metadata);
    incrementNumChanges();
    return this;
  }
  
  public int getContentLength() {
    return getResponse().length;
  }

  Multimap<String, String> getHeaders() {
    return headers;
  }

  Map<String, String> getMetadata() {
    return metadata;
  }
  
  byte[] getResponse() {
    // Supported to avoid copying data unnecessarily.
    return getRawContentBytes();
  }

  public int getHttpStatusCode() {
    return httpStatusCode;
  }
  
  /**
   * Ensures that, when setting content bytes, the bytes' encoding is reflected
   * in the current Content-Type header.
   * Note, this method does NOT override existing Content-Type values if newEncoding is null.
   * This allows charset to be set by header only, along with a byte array -- a very typical,
   * and important, pattern when creating an HttpResponse in an HttpFetcher.
   */
  @Override
  protected void setContentBytesState(byte[] newBytes, Charset newEncoding) {
    super.setContentBytesState(newBytes, newEncoding);
    
    // Set the new encoding of the raw bytes, in order to ensure that
    // Content-Type headers are in sync w/ the content's encoding.
    if (newEncoding != null) setEncoding(newEncoding);
  }
  
  private static GadgetHtmlParser unsupportedParser() {
    return new GadgetHtmlParser(null) {
      @Override
      protected Document parseDomImpl(String source) throws GadgetException {
        throw new UnsupportedOperationException("Using HttpResponseBuilder in non-rewriting context");
      }

      @Override
      protected DocumentFragment parseFragmentImpl(String source)
          throws GadgetException {
        throw new UnsupportedOperationException("Using HttpResponseBuilder in non-rewriting context");
      }
    };
  }
}
