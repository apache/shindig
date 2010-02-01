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

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.internal.Preconditions;
import com.google.inject.name.Named;

import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.HttpVersion;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.conn.ProxySelectorRoutePlanner;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.ProxySelector;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * A simple HTTP fetcher implementation based on Apache httpclient. Not recommended for production deployments until
 * the following issues are addressed:
 * <p/>
 * 1. This class potentially allows access to resources behind an organization's firewall.
 * 2. This class does not enforce any limits on what is fetched from remote hosts.
 */
@Singleton
public class BasicHttpFetcher implements HttpFetcher {
  private static final int DEFAULT_CONNECT_TIMEOUT_MS = 5000;
  private static final int DEFAULT_READ_TIMEOUT_MS = 5000;
  private static final int DEFAULT_MAX_OBJECT_SIZE = 0;  // no limit
  private static final long DEFAULT_SLOW_RESPONSE_WARNING = 10000;

  protected final HttpClient FETCHER;

  // mutable fields must be volatile
  private volatile int maxObjSize;
  private volatile long slowResponseWarning;

  private static final Logger LOG = Logger.getLogger(BasicHttpFetcher.class.getName());

  private final Set<Class<?>> TIMEOUT_EXCEPTIONS = ImmutableSet.<Class<?>>of(ConnectionPoolTimeoutException.class,
      SocketTimeoutException.class, SocketException.class, HttpHostConnectException.class, NoHttpResponseException.class,
      InterruptedException.class, UnknownHostException.class);

  /**
   * Creates a new fetcher using the default maximum object size and timeout --
   * no limit and 5 seconds.
   */
  public BasicHttpFetcher() {
    this(DEFAULT_MAX_OBJECT_SIZE, DEFAULT_CONNECT_TIMEOUT_MS, DEFAULT_READ_TIMEOUT_MS);
  }

  @Deprecated
  public BasicHttpFetcher(int maxObjSize, int connectionTimeoutMs) {
    this(maxObjSize, connectionTimeoutMs, DEFAULT_READ_TIMEOUT_MS);
  }

  /**
   * Creates a new fetcher for fetching HTTP objects.  Not really suitable
   * for production use. Use of an HTTP proxy for security is also necessary
   * for production deployment.
   *
   * @param maxObjSize          Maximum size, in bytes, of the object we will fetch, 0 if no limit..
   * @param connectionTimeoutMs timeout, in milliseconds, for connecting to hosts.
   * @param readTimeoutMs       timeout, in millseconds, for unresponsive connections
   */
  public BasicHttpFetcher(int maxObjSize, int connectionTimeoutMs, int readTimeoutMs) {
    // Create and initialize HTTP parameters
    setMaxObjectSizeBytes(maxObjSize);
    setSlowResponseWarning(DEFAULT_SLOW_RESPONSE_WARNING);

    HttpParams params = new BasicHttpParams();

    ConnManagerParams.setTimeout(params, connectionTimeoutMs);

    // These are probably overkill for most sites.
    ConnManagerParams.setMaxTotalConnections(params, 1152);
    ConnManagerParams.setMaxConnectionsPerRoute(params, new ConnPerRouteBean(256));

    HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
    HttpProtocolParams.setUserAgent(params, "Apache Shindig");
    HttpProtocolParams.setContentCharset(params, "UTF-8");

    HttpConnectionParams.setConnectionTimeout(params, connectionTimeoutMs);
    HttpConnectionParams.setSoTimeout(params, readTimeoutMs);
    HttpConnectionParams.setStaleCheckingEnabled(params, true);

    HttpClientParams.setRedirecting(params, true);
    HttpClientParams.setAuthenticating(params, false);

    // Create and initialize scheme registry
    SchemeRegistry schemeRegistry = new SchemeRegistry();
    schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
    schemeRegistry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));

    ClientConnectionManager cm = new ThreadSafeClientConnManager(params, schemeRegistry);

    DefaultHttpClient client = new DefaultHttpClient(cm, params);

    // try resending the request once
    client.setHttpRequestRetryHandler(new DefaultHttpRequestRetryHandler(1, true));

    // Add hooks for gzip/deflate
    client.addRequestInterceptor(new HttpRequestInterceptor() {
      public void process(
          final org.apache.http.HttpRequest request,
          final HttpContext context) throws HttpException, IOException {
        if (!request.containsHeader("Accept-Encoding")) {
          request.addHeader("Accept-Encoding", "gzip, deflate");
        }
      }
    });
    client.addResponseInterceptor(new HttpResponseInterceptor() {
      public void process(
          final org.apache.http.HttpResponse response,
          final HttpContext context) throws HttpException, IOException {
        HttpEntity entity = response.getEntity();
        if (entity != null) {
          Header ceheader = entity.getContentEncoding();
          if (ceheader != null) {
            for (HeaderElement codec : ceheader.getElements()) {
              String codecname = codec.getName();
              if ("gzip".equalsIgnoreCase(codecname)) {
                response.setEntity(
                    new GzipDecompressingEntity(response.getEntity()));
                return;
              } else if ("deflate".equals(codecname)) {
                response.setEntity(new DeflateDecompressingEntity(response.getEntity()));
                return;
              }
            }
          }
        }
      }
    });
    client.setHttpRequestRetryHandler(new DefaultHttpRequestRetryHandler() );

    // Use Java's built-in proxy logic
    ProxySelectorRoutePlanner routePlanner = new ProxySelectorRoutePlanner(
            client.getConnectionManager().getSchemeRegistry(),
            ProxySelector.getDefault());
    client.setRoutePlanner(routePlanner);

    FETCHER = client;
  }

  static class GzipDecompressingEntity extends HttpEntityWrapper {
    public GzipDecompressingEntity(final HttpEntity entity) {
      super(entity);
    }

    public InputStream getContent() throws IOException, IllegalStateException {
      // the wrapped entity's getContent() decides about repeatability
      InputStream wrappedin = wrappedEntity.getContent();

      return new GZIPInputStream(wrappedin);
    }

    public long getContentLength() {
      // length of ungzipped content is not known
      return -1;
    }
  }

  static class DeflateDecompressingEntity extends HttpEntityWrapper {
    public DeflateDecompressingEntity(final HttpEntity entity) {
      super(entity);
    }

    public InputStream getContent()
        throws IOException, IllegalStateException {

      // the wrapped entity's getContent() decides about repeatability
      InputStream wrappedin = wrappedEntity.getContent();

      return new InflaterInputStream(wrappedin, new Inflater(true));
    }

    public long getContentLength() {
      // length of ungzipped content is not known
      return -1;
    }
  }

  public HttpResponse fetch(org.apache.shindig.gadgets.http.HttpRequest request) {
    HttpUriRequest httpMethod = null;
    Preconditions.checkNotNull(request);
    final String methodType = request.getMethod();
    final String requestUri = request.getUri().toString();

    final org.apache.http.HttpResponse response;
    final long started = System.currentTimeMillis();

    try {
      if ("POST".equals(methodType) || "PUT".equals(methodType)) {
        HttpEntityEnclosingRequestBase enclosingMethod = ("POST".equals(methodType))
          ? new HttpPost(requestUri)
          : new HttpPut(requestUri);

        if (request.getPostBodyLength() > 0) {
          enclosingMethod.setEntity(new InputStreamEntity(request.getPostBody(), request.getPostBodyLength()));
        }
        httpMethod = enclosingMethod;
      } else if ("GET".equals(methodType)) {
        httpMethod = new HttpGet(requestUri);
      } else if ("HEAD".equals(methodType)) {
        httpMethod = new HttpHead(requestUri);
      } else if ("DELETE".equals(methodType)) {
        httpMethod = new HttpDelete(requestUri);
      }
      for (Map.Entry<String, List<String>> entry : request.getHeaders().entrySet()) {
        httpMethod.addHeader(entry.getKey(), StringUtils.join(entry.getValue(), ','));
      }

      if (!request.getFollowRedirects())
        httpMethod.getParams().setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, false);
      
      response = FETCHER.execute(httpMethod);

      if (response == null)
        throw new IOException("Unknown problem with request");

      if (response.getEntity() == null) {
        throw new IOException("Cannot retrieve " + request.getUri() + " reason " + response.getStatusLine().getReasonPhrase());
      }

      long now = System.currentTimeMillis();
      if (now - started > slowResponseWarning) {
        slowResponseWarning(request, started, now);
      }

      return makeResponse(response);

    } catch (Exception e) {
      long now = System.currentTimeMillis();

      // Find timeout exceptions, respond accordingly
      if (TIMEOUT_EXCEPTIONS.contains(e.getClass())) {
        LOG.warning("Timeout for " + request.getUri() + " Exception: " + e.getClass().getName() + " - " + e.getMessage() + " - " + (now - started) + "ms");
        return HttpResponse.timeout();
      }

      LOG.log(Level.WARNING, "Got Exception fetching " + request.getUri() + " - " + (now - started) + "ms", e);

      return HttpResponse.error();
    } finally {
      // cleanup any outstanding resources..
      if (httpMethod != null) try {
        httpMethod.abort();
      } catch (Exception e) {
        // ignore
      }
    }
  }

  /**
   * Called when a request takes too long.   Consider subclassing this if you want to do something other than logging
   * a warning .
   *
   * @param request the request that generated the slowrequest
   * @param started  the time the request started, in milliseconds.
   * @param finished the time the request finished, in milliseconds.
   */
  protected void slowResponseWarning(HttpRequest request, long started, long finished) {
    LOG.warning("Slow response from " + request.getUri() + ' ' + (finished - started) + "ms");
  }

  /**
   * Change the global maximum fetch size (in bytes) for all fetches.
   *
   * @param maxObjectSizeBytes value for maximum number of bytes, or 0 for no limit
   */
  @Inject(optional = true)
  public void setMaxObjectSizeBytes(@Named("shindig.http.client.max-object-size-bytes") int maxObjectSizeBytes) {
    this.maxObjSize = maxObjectSizeBytes;
  }

  /**
   * Change the global threshold for warning about slow responses
   *
   * @param slowResponseWarning time in milliseconds after we issue a warning
   */

  @Inject(optional = true)
  public void setSlowResponseWarning(@Named("shindig.http.client.slow-response-warning") long slowResponseWarning) {
    this.slowResponseWarning = slowResponseWarning;
  }

  /**
   * Change the global connection timeout for all new fetchs.
   *
   * @param connectionTimeoutMs new connection timeout in milliseconds
   */
  @Inject(optional = true)
  public void setConnectionTimeoutMs(@Named("shindig.http.client.connection-timeout-ms") int connectionTimeoutMs) {
    Preconditions.checkArgument(connectionTimeoutMs > 0, "connection-timeout-ms must be greater than 0");
    FETCHER.getParams().setIntParameter(HttpConnectionParams.CONNECTION_TIMEOUT, connectionTimeoutMs);
  }

  /**
   * Change the global read timeout for all new fetchs.
   *
   * @param connectionTimeoutMs new connection timeout in milliseconds
   */
  @Inject(optional = true)
  public void setReadTimeoutMs(@Named("shindig.http.client.read-timeout-ms") int connectionTimeoutMs) {
    Preconditions.checkArgument(connectionTimeoutMs > 0, "connection-timeout-ms must be greater than 0");
    FETCHER.getParams().setIntParameter(HttpConnectionParams.SO_TIMEOUT, connectionTimeoutMs);
  }


  /**
   * @param response The response to parse
   * @return A HttpResponse object made by consuming the response of the
   *         given HttpMethod.
   * @throws IOException when problems occur processing the body content
   */
  private HttpResponse makeResponse(org.apache.http.HttpResponse response) throws IOException {
    HttpResponseBuilder builder = new HttpResponseBuilder();

    if (response.getAllHeaders() != null) {
      for (Header h : response.getAllHeaders()) {
        if (h.getName() != null)
          builder.addHeader(h.getName(), h.getValue());
      }
    }

    HttpEntity entity = Preconditions.checkNotNull(response.getEntity());

    if (maxObjSize > 0 && entity.getContentLength() > maxObjSize) {
      return HttpResponse.badrequest("Exceeded maximum number of bytes - " + maxObjSize);
    }

    return builder
        .setHttpStatusCode(response.getStatusLine().getStatusCode())
        .setResponse(EntityUtils.toByteArray(entity))
        .create();
  }
}
