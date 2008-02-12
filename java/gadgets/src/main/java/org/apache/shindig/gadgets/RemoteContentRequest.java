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

  private final URI uri;
  public URI getUri() {
    return uri;
  }

  /**
   *
   * @param uri
   * @param headers
   * @param postBody
   */
  public RemoteContentRequest(URI uri,
                              Map<String, List<String>> headers,
                              byte[] postBody) {
    this.uri = uri;
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

  public RemoteContentRequest(URI uri, Map<String, List<String>> headers) {
    this(uri, headers, null);
  }

  public RemoteContentRequest(URI uri, byte[] postBody) {
    this(uri, null, postBody);
  }

  public RemoteContentRequest(URI uri) {
    this(uri, null, null);
  }

  @Override
  public boolean equals(Object rhs) {
    if (rhs == this) {return true;}
    if (rhs instanceof RemoteContentRequest) {
      RemoteContentRequest req = (RemoteContentRequest)rhs;
      return uri.equals(req.uri) &&
             Arrays.equals(postBody, req.postBody) &&
             headers.equals(req.headers);
    }
    return false;
  }
}
