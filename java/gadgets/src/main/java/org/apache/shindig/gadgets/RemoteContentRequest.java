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

package org.apache.shindig.gadgets;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds request data for passing to a RemoteContentFetcher.
 * Instances of this object are immutable.
 *
 * TODO: We should probably just stick the method in here. Having separate
 * calls for POST vs. get seems unnecessary.
 *
 * TODO: This naming seems really ridiculous now. Why don't we just call it
 * what it is -- an HTTP request?
 */
public class RemoteContentRequest {
  private final byte[] postBody;

  /**
   * @return An input stream that can be used to read the post body.
   */
  public InputStream getPostBody() {
    return new ByteArrayInputStream(postBody);
  }

  /**
   * Retrieves the total length of the post body.
   *
   * @return The length of the post body.
   */
  public int getPostBodyLength() {
    return postBody.length;
  }

  private final String contentType;
  private final static String DEFAULT_CONTENT_TYPE
      = "application/x-www-form-urlencoded; charset=utf-8";

  /**
   * @return The content type of the request (determined from request headers)
   */
  public String getContentType() {
    return contentType;
  }

  private final Map<String, List<String>> headers;

  /**
   * @return All headers set in this request.
   */
  public Map<String, List<String>> getAllHeaders() {
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
   * @param name
   * @return The first set header with the given name or null if not set. If
   *         you need multiple values for the header, use getHeaders().
   */
  public String getHeader(String name) {
    List<String> headerList = getHeaders(name);
    if (headerList.size() == 0) {
      return null;
    } else {
      return headerList.get(0);
    }
  }

  private final String method;
  public String getMethod() {
    return method;
  }

  private final URI uri;
  public URI getUri() {
    return uri;
  }

  private final Options options;
  public Options getOptions() {
    return options;
  }

  /**
   *
   * @param method
   * @param uri
   * @param headers
   * @param postBody
   * @param options
   */
  public RemoteContentRequest(String method,
                              URI uri,
                              Map<String, List<String>> headers,
                              byte[] postBody,
                              Options options) {
    this.method = method;
    this.uri = uri;
    this.options = options;
    // Copy the headers
    if (headers == null) {
      this.headers = Collections.emptyMap();
    } else {
      Map<String, List<String>> tmpHeaders
          = new HashMap<String, List<String>>();
      for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
        List<String> newList = new ArrayList<String>(entry.getValue());
        tmpHeaders.put(entry.getKey(), Collections.unmodifiableList(newList));
      }
      this.headers = Collections.unmodifiableMap(tmpHeaders);
    }
    if (postBody == null) {
      this.postBody = new byte[0];
    } else {
      this.postBody = new byte[postBody.length];
      System.arraycopy(postBody, 0, this.postBody, 0, postBody.length);
    }

    // Calculate content type.
    String type = getHeader("Content-Type");
    if (type == null) {
      contentType = DEFAULT_CONTENT_TYPE;
    } else {
      contentType = type;
    }
  }

  /**
   * Basic GET request.
   *
   * @param uri
   */
  public RemoteContentRequest(URI uri) {
    this("GET", uri, null, null, DEFAULT_OPTIONS);
  }

  /**
   * GET with options
   *
   * @param uri
   * @param options
   */
  public RemoteContentRequest(URI uri, Options options) {
    this("GET", uri, null, null, options);
  }

  /**
   * GET request with custom headers and default options
   * @param uri
   * @param headers
   */
  public RemoteContentRequest(URI uri, Map<String, List<String>> headers) {
    this("GET", uri, headers, null, DEFAULT_OPTIONS);
  }

  /**
   * GET request with custom headers + options
   * @param uri
   * @param headers
   * @param options
   */
  public RemoteContentRequest(URI uri, Map<String, List<String>> headers,
      Options options) {
    this("GET", uri, headers, null, options);
  }

  /**
   * Basic POST request
   * @param uri
   * @param postBody
   */
  public RemoteContentRequest(URI uri, byte[] postBody) {
    this("POST", uri, null, postBody, DEFAULT_OPTIONS);
  }

  /**
   * POST request with options
   * @param uri
   * @param postBody
   * @param options
   */
  public RemoteContentRequest(URI uri, byte[] postBody, Options options) {
    this("POST", uri, null, postBody, options);
  }

  /**
   * POST request with headers
   * @param uri
   * @param headers
   * @param postBody
   */
  public RemoteContentRequest(URI uri, Map<String, List<String>> headers,
      byte[] postBody) {
    this("POST", uri, headers, postBody, DEFAULT_OPTIONS);
  }

  /**
   * POST request with options + headers
   * @param uri
   * @param headers
   * @param postBody
   * @param options
   */
  public RemoteContentRequest(URI uri, Map<String, List<String>> headers,
      byte[] postBody, Options options) {
    this("POST", uri, headers, postBody, options);
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();
    buf.append(method).append(" ").append(uri.getPath()).append(" HTTP/1.1\r\n")
       .append("Host: ").append(uri.getHost())
       .append(uri.getPort() == 80 ? "" : ":" + uri.getPort())
       .append("\r\n");

    for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
      buf.append(entry.getKey()).append(": ");
      boolean first = false;
      for (String header : entry.getValue()) {
        if (!first) {
          first = true;
        } else {
          buf.append(", ");
        }
        buf.append(header);
      }
      buf.append("\r\n");
    }
   buf.append("\r\n");
   buf.append(new String(postBody));
   return buf.toString();
  }

  @Override
  public boolean equals(Object rhs) {
    if (rhs == this) {return true;}
    if (rhs instanceof RemoteContentRequest) {
      RemoteContentRequest req = (RemoteContentRequest)rhs;
      return method.equals(req.method) &&
             uri.equals(req.uri) &&
             Arrays.equals(postBody, req.postBody) &&
             headers.equals(req.headers);
    }
    return false;
  }

  public static final Options DEFAULT_OPTIONS = new Options();

  /**
   * Bag of options for making a request.
   *
   * This object is mutable to keep us sane. Don't mess with it once you've
   * sent it to RemoteContentRequest or bad things might happen.
   */
  public static class Options {
    public boolean ignoreCache = false;
  }
}
