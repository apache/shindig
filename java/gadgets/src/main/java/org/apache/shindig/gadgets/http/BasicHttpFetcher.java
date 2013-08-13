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

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
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
import org.apache.http.client.protocol.RequestAddCookies;
import org.apache.http.client.protocol.ResponseProcessCookies;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.params.ConnRouteParams;
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
import org.apache.http.util.ByteArrayBuffer;
import org.apache.shindig.common.logging.i18n.MessageKeys;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.GadgetException;

import java.io.EOFException;
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

// Temporary replacement of javax.annotation.Nullable
import org.apache.shindig.common.Nullable;
import javax.servlet.http.HttpServletResponse;

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

  //class name for logging purpose
  private static final String classname = BasicHttpFetcher.class.getName();
  private static final Logger LOG = Logger.getLogger(classname,MessageKeys.MESSAGES);

  private final Set<Class<?>> TIMEOUT_EXCEPTIONS = ImmutableSet.<Class<?>>of(ConnectionPoolTimeoutException.class,
      SocketTimeoutException.class, SocketException.class, HttpHostConnectException.class, NoHttpResponseException.class,
      InterruptedException.class, UnknownHostException.class);

  /**
   * Creates a new fetcher using the default maximum object size and timeout --
   * no limit and 5 seconds.
   * @param basicHttpFetcherProxy The http proxy to use.
   */
  @Inject
  public BasicHttpFetcher(@Nullable @Named("org.apache.shindig.gadgets.http.basicHttpFetcherProxy")
                          String basicHttpFetcherProxy) {
    this(DEFAULT_MAX_OBJECT_SIZE, DEFAULT_CONNECT_TIMEOUT_MS, DEFAULT_READ_TIMEOUT_MS,
         basicHttpFetcherProxy);
  }

  /**
   * Creates a new fetcher for fetching HTTP objects.  Not really suitable
   * for production use. Use of an HTTP proxy for security is also necessary
   * for production deployment.
   *
   * @param maxObjSize          Maximum size, in bytes, of the object we will fetch, 0 if no limit..
   * @param connectionTimeoutMs timeout, in milliseconds, for connecting to hosts.
   * @param readTimeoutMs       timeout, in millseconds, for unresponsive connections
   * @param basicHttpFetcherProxy The http proxy to use.
   */
  public BasicHttpFetcher(int maxObjSize, int connectionTimeoutMs, int readTimeoutMs,
                          String basicHttpFetcherProxy) {
    // Create and initialize HTTP parameters
    setMaxObjectSizeBytes(maxObjSize);
    setSlowResponseWarning(DEFAULT_SLOW_RESPONSE_WARNING);

    HttpParams params = new BasicHttpParams();

    HttpConnectionParams.setConnectionTimeout(params, connectionTimeoutMs);

    HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
    HttpProtocolParams.setUserAgent(params, "Apache Shindig");
    HttpProtocolParams.setContentCharset(params, "UTF-8");

    HttpConnectionParams.setConnectionTimeout(params, connectionTimeoutMs);
    HttpConnectionParams.setSoTimeout(params, readTimeoutMs);
    HttpConnectionParams.setStaleCheckingEnabled(params, true);
    HttpConnectionParams.setSoReuseaddr(params, true);

    HttpClientParams.setRedirecting(params, true);
    HttpClientParams.setAuthenticating(params, false);

    // Create and initialize scheme registry
    SchemeRegistry schemeRegistry = new SchemeRegistry();
    schemeRegistry.register(new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));
    schemeRegistry.register(new Scheme("https", 443, SSLSocketFactory.getSocketFactory()));

    ThreadSafeClientConnManager cm = new ThreadSafeClientConnManager(schemeRegistry);
    // These are probably overkill for most sites.
    cm.setMaxTotal(1152);
    cm.setDefaultMaxPerRoute(256);

    DefaultHttpClient client = new DefaultHttpClient(cm, params);

    // Set proxy if set via guice.
    if (!Strings.isNullOrEmpty(basicHttpFetcherProxy)) {
      String[] splits = StringUtils.split(basicHttpFetcherProxy, ':');
      ConnRouteParams.setDefaultProxy(
          client.getParams(), new HttpHost(splits[0], Integer.parseInt(splits[1]), "http"));
    }

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

    // Disable automatic storage and sending of cookies (see SHINDIG-1382)
    client.removeRequestInterceptorByClass(RequestAddCookies.class);
    client.removeResponseInterceptorByClass(ResponseProcessCookies.class);

    // Use Java's built-in proxy logic in case no proxy set via guice.
    if (Strings.isNullOrEmpty(basicHttpFetcherProxy)) {
      ProxySelectorRoutePlanner routePlanner = new ProxySelectorRoutePlanner(
          client.getConnectionManager().getSchemeRegistry(),
          ProxySelector.getDefault());
      client.setRoutePlanner(routePlanner);
    }

    FETCHER = client;
  }

  static class GzipDecompressingEntity extends HttpEntityWrapper {
    public GzipDecompressingEntity(final HttpEntity entity) {
      super(entity);
    }

    @Override
    public InputStream getContent() throws IOException, IllegalStateException {
      // the wrapped entity's getContent() decides about repeatability
      InputStream wrappedin = wrappedEntity.getContent();

      return new GZIPInputStream(wrappedin);
    }

    @Override
    public long getContentLength() {
      // length of ungzipped content is not known
      return -1;
    }
  }

  static class DeflateDecompressingEntity extends HttpEntityWrapper {
    public DeflateDecompressingEntity(final HttpEntity entity) {
      super(entity);
    }

    @Override
    public InputStream getContent()
        throws IOException, IllegalStateException {

      // the wrapped entity's getContent() decides about repeatability
      InputStream wrappedin = wrappedEntity.getContent();

      return new InflaterInputStream(wrappedin, new Inflater(true));
    }

    @Override
    public long getContentLength() {
      // length of ungzipped content is not known
      return -1;
    }
  }

  public HttpResponse fetch(org.apache.shindig.gadgets.http.HttpRequest request)
      throws GadgetException {
    HttpUriRequest httpMethod = null;
    Preconditions.checkNotNull(request);
    final String methodType = request.getMethod();

    final org.apache.http.HttpResponse response;
    final long started = System.currentTimeMillis();

    // Break the request Uri to its components:
    Uri uri = request.getUri();
    if (Strings.isNullOrEmpty(uri.getAuthority())) {
      throw new GadgetException(GadgetException.Code.INVALID_USER_DATA,
          "Missing domain name for request: " + uri,
          HttpServletResponse.SC_BAD_REQUEST);
    }
    if (Strings.isNullOrEmpty(uri.getScheme())) {
      throw new GadgetException(GadgetException.Code.INVALID_USER_DATA,
          "Missing schema for request: " + uri,
          HttpServletResponse.SC_BAD_REQUEST);
    }
    String[] hostparts = StringUtils.splitPreserveAllTokens(uri.getAuthority(),':');
    int port = -1; // default port
    if (hostparts.length > 2) {
      throw new GadgetException(GadgetException.Code.INVALID_USER_DATA,
          "Bad host name in request: " + uri.getAuthority(),
          HttpServletResponse.SC_BAD_REQUEST);
    }
    if (hostparts.length == 2) {
      try {
        port = Integer.parseInt(hostparts[1]);
      } catch (NumberFormatException e) {
        throw new GadgetException(GadgetException.Code.INVALID_USER_DATA,
            "Bad port number in request: " + uri.getAuthority(),
            HttpServletResponse.SC_BAD_REQUEST);
      }
    }

    String requestUri = uri.getPath();
    // Treat path as / if set as null.
    if (uri.getPath() == null) {
      requestUri = "/";
    }
    if (uri.getQuery() != null) {
      requestUri += '?' + uri.getQuery();
    }

    // Get the http host to connect to.
    HttpHost host = new HttpHost(hostparts[0], port, uri.getScheme());

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
        httpMethod.addHeader(entry.getKey(), Joiner.on(',').join(entry.getValue()));
      }

      // Disable following redirects.
      if (!request.getFollowRedirects()) {
        httpMethod.getParams().setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, false);
      }

      // HttpClient doesn't handle all cases when breaking url (specifically '_' in domain)
      // So lets pass it the url parsed:
      response = FETCHER.execute(host, httpMethod);

      if (response == null) {
        throw new IOException("Unknown problem with request");
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
        if (LOG.isLoggable(Level.INFO)) {
          LOG.logp(Level.INFO, classname, "fetch", MessageKeys.TIMEOUT_EXCEPTION, new Object[] {request.getUri(),classname,e.getMessage(),now-started});
        }
        return HttpResponse.timeout();
      }
      if (LOG.isLoggable(Level.INFO)) {
          LOG.logp(Level.INFO, classname, "fetch", MessageKeys.EXCEPTION_OCCURRED, new Object[] {request.getUri(),now-started});
          LOG.logp(Level.INFO, classname, "fetch", "", e);
      }
      // Separate shindig error from external error
      throw new GadgetException(GadgetException.Code.INTERNAL_SERVER_ERROR, e,
          HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    } finally {
      // cleanup any outstanding resources..
      if (httpMethod != null) try {
        httpMethod.abort();
      } catch (UnsupportedOperationException e) {
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
    if (LOG.isLoggable(Level.WARNING)) {
      LOG.logp(Level.WARNING, classname, "slowResponseWarning", MessageKeys.SLOW_RESPONSE, new Object[] {request.getUri(),finished-started});
    }
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
   * @param readTimeoutMs new connection timeout in milliseconds
   */
  @Inject(optional = true)
  public void setReadTimeoutMs(@Named("shindig.http.client.read-timeout-ms") int readTimeoutMs) {
    Preconditions.checkArgument(readTimeoutMs > 0, "read-timeout-ms must be greater than 0");
    FETCHER.getParams().setIntParameter(HttpConnectionParams.SO_TIMEOUT, readTimeoutMs);
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

    HttpEntity entity = response.getEntity();

    if (maxObjSize > 0 && entity != null && entity.getContentLength() > maxObjSize) {
      return HttpResponse.badrequest("Exceeded maximum number of bytes - " + maxObjSize);
    }

    byte[] responseBytes = (entity == null) ? null : toByteArraySafe(entity);

    return builder
        .setHttpStatusCode(response.getStatusLine().getStatusCode())
        .setResponse(responseBytes)
        .create();
  }

  /**
   * This method is Safe replica version of org.apache.http.util.EntityUtils.toByteArray.
   * The try block embedding 'instream.read' has a corresponding catch block for 'EOFException'
   * (that's Ignored) and all other IOExceptions are let pass.
   *
   * @param entity
   * @return byte array containing the entity content. May be empty/null.
   * @throws IOException if an error occurs reading the input stream
   */
  public byte[] toByteArraySafe(final HttpEntity entity) throws IOException {
    if (entity == null) {
      return null;
    }

    InputStream instream = entity.getContent();
    if (instream == null) {
      return ArrayUtils.EMPTY_BYTE_ARRAY;
    }
    Preconditions.checkArgument(entity.getContentLength() < Integer.MAX_VALUE, "HTTP entity too large to be buffered in memory");

    // The raw data stream (inside JDK) is read in a buffer of size '512'. The original code
    // org.apache.http.util.EntityUtils.toByteArray reads the unzipped data in a buffer of
    // 4096 byte. For any data stream that has a compression ratio lesser than 1/8, this may
    // result in the buffer/array overflow. Increasing the buffer size to '16384'. It's highly
    // unlikely to get data compression ratios lesser than 1/32 (3%).
    final int bufferLength = 16384;
    int i = (int)entity.getContentLength();
    if (i < 0) {
      i = bufferLength;
    }
    ByteArrayBuffer buffer = new ByteArrayBuffer(i);
    try {
      byte[] tmp = new byte[bufferLength];
      int l;
      while((l = instream.read(tmp)) != -1) {
        buffer.append(tmp, 0, l);
      }
    } catch (EOFException eofe) {
      /**
       * Ref: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4040920
       * Due to a bug in JDK ZLIB (InflaterInputStream), unexpected EOF error can occur.
       * In such cases, even if the input stream is finished reading, the
       * 'Inflater.finished()' call erroneously returns 'false' and
       * 'java.util.zip.InflaterInputStream.fill' throws the 'EOFException'.
       * So for such case, ignore the Exception in case Exception Cause is
       * 'Unexpected end of ZLIB input stream'.
       *
       * Also, ignore this exception in case the exception has no message
       * body as this is the case where {@link GZIPInputStream#readUByte}
       * throws EOFException with empty message. A bug has been filed with Sun
       * and will be mentioned here once it is accepted.
       */
      if (instream.available() == 0 &&
          (eofe.getMessage() == null ||
           eofe.getMessage().equals("Unexpected end of ZLIB input stream"))) {
        LOG.log(Level.FINE, "EOFException: ", eofe);
      } else {
        throw eofe;
      }
    }
    finally {
      instream.close();
    }
    return buffer.toByteArray();
  }
}
