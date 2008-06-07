/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.gadgets.http;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * Implementation of a {@code RemoteObjectFetcher} using standard java.net
 * classes. Only one instance of this should be present at any time, so we
 * annotate it as a Singleton to resolve Guice injection limitations.
 */
@Singleton
public class BasicHttpFetcher implements HttpFetcher {
  private static final int CONNECT_TIMEOUT_MS = 5000;
  private static final int DEFAULT_MAX_OBJECT_SIZE = 1024 * 1024;

  private final int maxObjSize;
  private final HttpCache cache;

  /**
   * Creates a new fetcher capable of retrieving objects {@code maxObjSize}
   * bytes or smaller in size.
   * @param maxObjSize Maximum size, in bytes, of object to fetch
   */
  public BasicHttpFetcher(HttpCache cache, int maxObjSize) {
    this.maxObjSize = maxObjSize;
    this.cache = cache;
  }

  /**
   * Creates a new fetcher using the default maximum object size.
   */
  @Inject
  public BasicHttpFetcher(HttpCache cache) {
    this(cache, DEFAULT_MAX_OBJECT_SIZE);
  }

  /**
   * Initializes the connection.
   *
   * @param request
   * @return The opened connection
   * @throws IOException
   */
  private HttpURLConnection getConnection(HttpRequest request)
      throws IOException {
    HttpURLConnection fetcher =
        (HttpURLConnection)request.getUri().toURL().openConnection();
    fetcher.setConnectTimeout(CONNECT_TIMEOUT_MS);
    fetcher.setRequestProperty("Accept-Encoding", "gzip, deflate");
    fetcher.setInstanceFollowRedirects(true);
    Map<String, List<String>> reqHeaders = request.getAllHeaders();
    for (Map.Entry<String, List<String>> entry : reqHeaders.entrySet()) {
      List<String> value = entry.getValue();
      if (value.size() == 1) {
        fetcher.setRequestProperty(entry.getKey(), value.get(0));
      } else {
        StringBuilder headerList = new StringBuilder();
        boolean first = false;
        for (String val : value) {
          if (!first) {
            first = true;
          } else {
            headerList.append(',');
          }
          headerList.append(val);
        }
        fetcher.setRequestProperty(entry.getKey(), headerList.toString());
      }
    }
    fetcher.setDefaultUseCaches(!request.getOptions().ignoreCache);
    return fetcher;
  }

  /**
   * @param fetcher
   * @return A HttpResponse object made by consuming the response of the
   *     given HttpURLConnection.
   */
  private HttpResponse makeResponse(HttpURLConnection fetcher)
      throws IOException {
    Map<String, List<String>> headers = fetcher.getHeaderFields();
    int responseCode = fetcher.getResponseCode();
    // Find the response stream - the error stream may be valid in cases
    // where the input stream is not.
    InputStream baseIs = null;
    try {
      baseIs = fetcher.getInputStream();
    } catch (IOException e) {
      // normal for 401, 403 and 404 responses, for example...
    }
    if (baseIs == null) {
      // Try for an error input stream
      baseIs = fetcher.getErrorStream();
    }
    if (baseIs == null) {
      // Fall back to zero length response.
      baseIs = new ByteArrayInputStream(new byte[0]);
    }
    
    String encoding = fetcher.getContentEncoding();
    // Create the appropriate stream wrapper based on the encoding type.
    InputStream is = null;
    if (encoding == null) {
      is = baseIs;
    } else if (encoding.equalsIgnoreCase("gzip")) {
      is = new GZIPInputStream(baseIs);
    } else if (encoding.equalsIgnoreCase("deflate")) {
      Inflater inflater = new Inflater(true);
      is = new InflaterInputStream(baseIs, inflater);
    }

    byte[] body = IOUtils.toByteArray(is);
    return new HttpResponse(responseCode, body, headers);
  }

  /** {@inheritDoc} */
  public HttpResponse fetch(HttpRequest request) {
    HttpResponse response = cache.getResponse(request);
    if (response != null) {
      return response;
    }
    try {
      HttpURLConnection fetcher = getConnection(request);
      fetcher.setRequestMethod(request.getMethod());
      if (!"GET".equals(request.getMethod())) {
        fetcher.setUseCaches(false);
      }
      if (request.getPostBodyLength() > 0) {
        fetcher.setDoOutput(true);
        fetcher.setRequestProperty("Content-Length",
            String.valueOf(request.getPostBodyLength()));
        IOUtils.copy(request.getPostBody(), fetcher.getOutputStream());
      }
      response = makeResponse(fetcher);
      return cache.addResponse(request, response);
    } catch (IOException e) {
      if (e instanceof java.net.SocketTimeoutException ||
          e instanceof java.net.SocketException) {
        return HttpResponse.timeout();
      }
      return HttpResponse.error();
    }
  }
}