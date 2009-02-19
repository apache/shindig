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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * A very primitive HTTP fetcher implementation. Not recommended for production deployments until
 * the following issues are addressed:
 *
 * 1. This class potentially allows access to resources behind an organization's firewall.
 * 2. This class does not handle most advanced HTTP functionality correctly (SSL, gzip, etc.)
 * 3. This class does not enforce any limits on what is fetched from remote hosts.
 *
 * It is highly likely that this will be replaced by an apache commons HttpClient in the future.
 *
 * TODO: Replace with commons HttpClient.
 */
@Singleton
public class BasicHttpFetcher implements HttpFetcher {
  private static final int CONNECT_TIMEOUT_MS = 5000;
  private static final int DEFAULT_MAX_OBJECT_SIZE = 1024 * 1024;

  private Provider<Proxy> proxyProvider;

  /**
   * Creates a new fetcher for fetching HTTP objects.  Not really suitable
   * for production use.  Someone should probably go and implement maxObjSize,
   * for one thing.  Use of an HTTP proxy for security is also necessary
   * for production deployment.
   *
   * @param maxObjSize Maximum size, in bytes, of object to fetch.  Except this
   * isn't actually implemented.
   */
  public BasicHttpFetcher(int maxObjSize) {
  }

  /**
   * Creates a new fetcher using the default maximum object size.
   */
  @Inject
  public BasicHttpFetcher() {
    this(DEFAULT_MAX_OBJECT_SIZE);
  }

  // TODO Re-add Inject annotation once shindig is upgraded to guice 2.0, because at the moment this causes problems
  // when running shindig behind a proxy as guice still injects a proxy provider even though optional is set to true.
  // See issue http://code.google.com/p/google-guice/issues/detail?id=107 for more details.
  // @Inject(optional=true)
  public void setProxyProvider(Provider<Proxy> proxyProvider) {
    this.proxyProvider = proxyProvider;
  }

  /**
   * Initializes the connection.
   *
   * @param request
   * @return The opened connection
   * @throws IOException
   */
  private HttpURLConnection getConnection(HttpRequest request) throws IOException {
    URL url = new URL(request.getUri().toString());
    HttpURLConnection fetcher = (HttpURLConnection) ( proxyProvider == null ?
        url.openConnection() : url.openConnection(proxyProvider.get()));
    fetcher.setConnectTimeout(CONNECT_TIMEOUT_MS);
    fetcher.setRequestProperty("Accept-Encoding", "gzip, deflate");
    fetcher.setInstanceFollowRedirects(request.getFollowRedirects());
    for (Map.Entry<String, List<String>> entry : request.getHeaders().entrySet()) {
      fetcher.setRequestProperty(entry.getKey(), StringUtils.join(entry.getValue(), ','));
    }
    fetcher.setDefaultUseCaches(!request.getIgnoreCache());
    return fetcher;
  }

  /**
   * @param fetcher
   * @return A HttpResponse object made by consuming the response of the
   *     given HttpURLConnection.
   */
  private HttpResponse makeResponse(HttpURLConnection fetcher) throws IOException {
    Map<String, List<String>> headers = Maps.newHashMap(fetcher.getHeaderFields());
    // The first header is always null here to provide the response body.
    headers.remove(null);
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
      baseIs = new ByteArrayInputStream(ArrayUtils.EMPTY_BYTE_ARRAY);
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
    return new HttpResponseBuilder()
        .setHttpStatusCode(responseCode)
        .setResponse(body)
        .addAllHeaders(headers)
        .create();
  }

  /** {@inheritDoc} */
  public HttpResponse fetch(HttpRequest request) {
    try {
      HttpURLConnection fetcher = getConnection(request);
      fetcher.setRequestMethod(request.getMethod());
      if (!"GET".equals(request.getMethod())) {
        fetcher.setUseCaches(false);
      }
      fetcher.setRequestProperty("Content-Length",
          String.valueOf(request.getPostBodyLength()));
      if (request.getPostBodyLength() > 0) {
        fetcher.setDoOutput(true);
        IOUtils.copy(request.getPostBody(), fetcher.getOutputStream());
      }
      return makeResponse(fetcher);
    } catch (IOException e) {
      if (e instanceof java.net.SocketTimeoutException ||
          e instanceof java.net.SocketException) {
        return HttpResponse.timeout();
      }
      return HttpResponse.error();
    }
  }
}
