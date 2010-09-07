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

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.apache.commons.lang.StringUtils;
import org.apache.shindig.common.servlet.HttpUtil;
import org.apache.shindig.common.util.DateUtil;
import org.apache.shindig.common.util.TimeSource;
import org.apache.shindig.gadgets.encoding.EncodingDetector;

import java.io.ByteArrayInputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Represents the results of an HTTP content retrieval operation.
 *
 * HttpResponse objects are immutable in order to allow them to be safely used in concurrent
 * caches and by multiple threads without worrying about concurrent modification.
 */
public final class HttpResponse implements Externalizable {
  private static final long serialVersionUID = 7526471155622776147L;

  public static final int SC_CONTINUE = 100;
  public static final int SC_SWITCHING_PROTOCOLS = 101;

  public static final int SC_OK = 200;
  public static final int SC_CREATED = 201;
  public static final int SC_ACCEPTED = 202;
  public static final int SC_NON_AUTHORITATIVE_INFORMATION = 203;
  public static final int SC_NO_CONTENT = 204;
  public static final int SC_RESET_CONTENT = 205;
  public static final int SC_PARTIAL_CONTENT = 206;

  public static final int SC_MULTIPLE_CHOICES = 300;
  public static final int SC_MOVED_PERMANENTLY = 301;
  public static final int SC_FOUND = 302;
  public static final int SC_SEE_OTHER = 303;
  public static final int SC_NOT_MODIFIED = 304;
  public static final int SC_USE_PROXY = 305;
  public static final int SC_TEMPORARY_REDIRECT = 307;

  public static final int SC_BAD_REQUEST = 400;
  public static final int SC_UNAUTHORIZED = 401;
  public static final int SC_PAYMENT_REQUIRED = 402;
  public static final int SC_FORBIDDEN = 403;
  public static final int SC_NOT_FOUND = 404;
  public static final int SC_METHOD_NOT_ALLOWED = 405;
  public static final int SC_NOT_ACCEPTABLE = 406;
  public static final int SC_PROXY_AUTHENTICATION_REQUIRED = 407;
  public static final int SC_REQUEST_TIMEOUT = 408;
  public static final int SC_CONFLICT = 409;
  public static final int SC_GONE = 410;
  public static final int SC_LENGTH_REQUIRED = 411;
  public static final int SC_PRECONDITION_FAILED = 412;
  public static final int SC_REQUEST_ENTITY_TOO_LARGE = 413;
  public static final int SC_REQUEST_URI_TOO_LONG = 414;
  public static final int SC_UNSUPPORTED_MEDIA_TYPE = 415;
  public static final int SC_REQUESTED_RANGE_NOT_SATISFIABLE = 416;
  public static final int SC_EXPECTATION_FAILED = 417;

  public static final int SC_INTERNAL_SERVER_ERROR = 500;
  public static final int SC_NOT_IMPLEMENTED = 501;
  public static final int SC_BAD_GATEWAY = 502;
  public static final int SC_SERVICE_UNAVAILABLE = 503;
  public static final int SC_GATEWAY_TIMEOUT = 504;
  public static final int SC_HTTP_VERSION_NOT_SUPPORTED = 505;

  // These content types can always skip encoding detection.
  private static final Set<String> BINARY_CONTENT_TYPES = ImmutableSet.of(
      "image/jpeg", "image/png", "image/gif", "image/jpg", "application/x-shockwave-flash",
      "application/octet-stream", "application/ogg", "application/zip", "audio/mpeg",
      "audio/x-ms-wma", "audio/vnd.rn-realaudio", "audio/x-wav", "video/mpeg", "video/mp4",
      "video/quicktime", "video/x-ms-wmv", "video/x-flv", "video/flv",
      "video/x-ms-asf", "application/pdf", "image/x-icon"
  );

  // These HTTP status codes should always honor the HTTP status returned by the remote host. All
  // other error codes are treated as errors and will use the negativeCacheTtl value.
  private static final Set<Integer> NEGATIVE_CACHING_EXEMPT_STATUS
      = ImmutableSet.of(SC_UNAUTHORIZED, SC_FORBIDDEN);

  // TTL to use when an error response is fetched. This should be non-zero to
  // avoid high rates of requests to bad urls in high-traffic situations.
  static final long DEFAULT_NEGATIVE_CACHE_TTL = 30 * 1000;

  // Default TTL for an entry in the cache that does not have any cache control headers.
  static final long DEFAULT_TTL = 5L * 60L * 1000L;

  static final Charset DEFAULT_ENCODING = Charset.forName("UTF-8");

  // At what point you don't trust remote server date stamp on response (in milliseconds)
  // (Should be less then DEFAULT_TTL)
  static final long DEFAULT_DRIFT_LIMIT_MS = 3L * 60L * 1000L;

  @Inject(optional = true) @Named("shindig.cache.http.negativeCacheTtl")
  private static long negativeCacheTtl = DEFAULT_NEGATIVE_CACHE_TTL;

  @Inject(optional = true) @Named("shindig.cache.http.defaultTtl")
  private static long defaultTtl = DEFAULT_TTL;

  @Inject(optional = true) @Named("shindig.http.fast-encoding-detection")
  private static boolean fastEncodingDetection = true;

  // Support injection of smarter encoding detection
  @Inject(optional = true)
  private static EncodingDetector.FallbackEncodingDetector customEncodingDetector =
      new EncodingDetector.FallbackEncodingDetector();

  @Inject(optional = true) @Named("shindig.http.date-drift-limit-ms")
  private static long responseDateDriftLimit = DEFAULT_DRIFT_LIMIT_MS;

  public static void setTimeSource(TimeSource timeSource) {
    HttpUtil.setTimeSource(timeSource);
  }

  public static TimeSource getTimeSource() {
    return HttpUtil.getTimeSource();
  }

  // Holds character sets for fast conversion
  private static final Map<String, Charset> encodingToCharset = new MapMaker().makeMap();

  private String responseString;
  private long date;
  private Charset encoding;
  private Map<String, String> metadata;

  private int httpStatusCode;
  private Multimap<String, String> headers;
  private byte[] responseBytes;

  /**
   * Needed for serialization. Do not use this for any other purpose.
   */
  public HttpResponse() {}

  /**
   * Construct an HttpResponse from a builder (called by HttpResponseBuilder.create).
   */
  HttpResponse(HttpResponseBuilder builder) {
    httpStatusCode = builder.getHttpStatusCode();
    Multimap<String, String> headerCopy = HttpResponse.newHeaderMultimap();

    // Always safe, HttpResponseBuilder won't modify the body.
    responseBytes = builder.getResponse();

    // Copy headers after builder.getResponse(), since that can modify Content-Type.
    headerCopy.putAll(builder.getHeaders());

    Map<String, String> metadataCopy = Maps.newHashMap(builder.getMetadata());
    metadata = Collections.unmodifiableMap(metadataCopy);

    // We want to modify the headers to ensure that the proper Content-Type and Date headers
    // have been set. This allows us to avoid these expensive calculations from the cache.
    date = getAndUpdateDate(headerCopy);
    encoding = getAndUpdateEncoding(headerCopy, responseBytes);
    headers = Multimaps.unmodifiableMultimap(headerCopy);
  }

  private HttpResponse(int httpStatusCode, String body) {
    this(new HttpResponseBuilder()
      .setHttpStatusCode(httpStatusCode)
      .setResponseString(body));
  }

  public HttpResponse(String body) {
    this(SC_OK, body);
  }

  public static HttpResponse error() {
    return new HttpResponse(SC_INTERNAL_SERVER_ERROR, "");
  }

  public static HttpResponse badrequest(String msg) {
    return new HttpResponse(SC_BAD_REQUEST, msg);
  }

  public static HttpResponse timeout() {
    return new HttpResponse(SC_GATEWAY_TIMEOUT, "");
  }

  public static HttpResponse notFound() {
    return new HttpResponse(SC_NOT_FOUND, "");
  }

  public int getHttpStatusCode() {
    return httpStatusCode;
  }

  /**
   * @return True if the status code is considered to be an error.
   */
  public boolean isError() {
    return httpStatusCode >= 400;
  }

  /**
   * @return The encoding of the response body, if we're able to determine it.
   */
  public String getEncoding() {
    return encoding.name();
  }

  /**
   * @return The Charset of the response body's encoding, if we were able to determine it.
   */
  public Charset getEncodingCharset() {
    return encoding;
  }

  /**
   * @return the content length
   */
  public int getContentLength() {
    return responseBytes.length;
  }

  /**
   * @return An input stream suitable for reading the entirety of the response.
   */
  public InputStream getResponse() {
    return new ByteArrayInputStream(responseBytes);
  }

  /**
   * Attempts to convert the response body to a string using the Content-Type header. If no
   * Content-Type header is specified (or it doesn't include an encoding), we will assume it is
   * DEFAULT_ENCODING.
   *
   * @return The body as a string.
   */
  public String getResponseAsString() {
    if (responseString == null) {
      responseString = encoding.decode(ByteBuffer.wrap(responseBytes)).toString();

      // Strip BOM if present.
      if (responseString.length() > 0 && responseString.codePointAt(0) == 0xFEFF) {
        responseString = responseString.substring(1);
      }
    }
    return responseString;
  }

  /**
   * @return All headers for this object.
   */
  public Multimap<String, String> getHeaders() {
    return headers;
  }

  /**
   * @return All headers with the given name. If no headers are set for the given name, an empty
   * collection will be returned.
   */
  public Collection<String> getHeaders(String name) {
    return headers.get(name);
  }

  /**
   * @return The first set header with the given name or null if not set. If you need multiple
   *         values for the header, use getHeaders().
   */
  public String getHeader(String name) {
    Collection<String> headerList = getHeaders(name);
    if (headerList.isEmpty()) {
      return null;
    } else {
      return headerList.iterator().next();
    }
  }

  /**
   * @return additional data to embed in responses sent from the JSON proxy.
   */
  public Map<String, String> getMetadata() {
    return metadata;
  }

  /**
   * Calculate the Cache Expiration for this response.
   *
   *
   * For errors (rc >=400) we intentionally ignore cache-control headers for most HTTP error responses, because if
   * we don't we end up hammering sites that have gone down with lots of requests. Certain classes
   * of client errors (authentication) have more severe behavioral implications if we cache them.
   *
   * For errors if the server provides a Retry-After header we use that.
   *
   * We technically shouldn't be caching certain 300 class status codes either, such as 302, but
   * in practice this is a better option for performance.
   *
   * @return consolidated cache expiration time or -1
   */
  public long getCacheExpiration() {
    if (isError() && !NEGATIVE_CACHING_EXEMPT_STATUS.contains(httpStatusCode)) {
      // If the server provides a Retry-After header use that as the cacheTtl
      String retryAfter = this.getHeader("Retry-After");
      if (retryAfter != null) {
        if (StringUtils.isNumeric(retryAfter)) {
          return date + Integer.parseInt(retryAfter) * 1000L;
        } else {
          Date expiresDate = DateUtil.parseRfc1123Date(retryAfter);
          if (expiresDate != null)
            return expiresDate.getTime();
        }
      }
      // default value
      return date + negativeCacheTtl;
    }

    if (isStrictNoCache()) {
      return -1;
    }
    long maxAge = getCacheControlMaxAge();
    if (maxAge != -1) {
      return date + maxAge;
    }
    long expiration = getExpiresTime();
    if (expiration != -1) {
      return expiration;
    }
    return date + defaultTtl;
  }

  /**
   * @return Consolidated ttl in milliseconds or -1.
   */
  public long getCacheTtl() {
    long expiration = getCacheExpiration();
    if (expiration != -1) {
      return expiration - getTimeSource().currentTimeMillis();
    }
    return -1;
  }

  /**
   * @return True if this result is stale.
   */
  public boolean isStale() {
    return getCacheTtl() <= 0;
  }

  /**
   * @return true if a strict no-cache header is set in Cache-Control or Pragma
   */
  public boolean isStrictNoCache() {
    if (isError() && !NEGATIVE_CACHING_EXEMPT_STATUS.contains(httpStatusCode)) {
      return false;
    }
    String cacheControl = getHeader("Cache-Control");
    if (cacheControl != null) {
      String[] directives = StringUtils.split(cacheControl, ',');
      for (String directive : directives) {
        directive = directive.trim();
        if (directive.equalsIgnoreCase("no-cache")
            || directive.equalsIgnoreCase("no-store")
            || directive.equalsIgnoreCase("private")) {
          return true;
        }
      }
    }

    for (String pragma : getHeaders("Pragma")) {
      if ("no-cache".equalsIgnoreCase(pragma)) {
        return true;
      }
    }
    return false;
  }

  /**
   * @return the expiration time from the Expires header or -1 if not set
   */
  private long getExpiresTime() {
    String expires = getHeader("Expires");
    if (expires != null) {
      Date expiresDate = DateUtil.parseRfc1123Date(expires);
      if (expiresDate != null) {
        return expiresDate.getTime();
      } else {
        // Per RFC2616, 14.21 (http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.21):
        // "HTTP/1.1 clients and caches MUST treat other invalid date formats,
        // especially including the value "0", as in the past (i.e., "already
        // expired")."
        return 0;
      }
    }
    return -1;
  }

  /**
   * @return max-age value or -1 if invalid or not set
   */
  private long getCacheControlMaxAge() {
    String cacheControl = getHeader("Cache-Control");
    if (cacheControl != null) {
      String[] directives = StringUtils.split(cacheControl, ',');
      for (String directive : directives) {
        directive = directive.trim();
        if (directive.startsWith("max-age")) {
          String[] parts = StringUtils.split(directive, '=');
          if (parts.length == 2) {
            try {
              return Long.parseLong(parts[1]) * 1000;
            } catch (NumberFormatException ignore) {
              return -1;
            }
          }
        }
      }
    }
    return -1;
  }

  /**
   * Tries to find a valid date from the input headers.
   *
   * @return The value of the date header, in milliseconds, or -1 if no Date could be determined.
   */
  private static long getAndUpdateDate(Multimap<String, String> headers) {
    // Validate the Date header. Must conform to the HTTP date format.
    long timestamp = -1;
    long currentTime = getTimeSource().currentTimeMillis();
    Collection<String> dates = headers.get("Date");

    if (!dates.isEmpty()) {
      Date d = DateUtil.parseRfc1123Date(dates.iterator().next());
      if (d != null) {
        timestamp = d.getTime();
        if (Math.abs(currentTime - timestamp) > responseDateDriftLimit) {
          // Do not trust the date from response if it is too old (server time out of sync)
          timestamp = -1;
        }
      }
    }
    if (timestamp == -1) {
      timestamp = currentTime;
      headers.replaceValues("Date", ImmutableList.of(DateUtil.formatRfc1123Date(timestamp)));
    }
    return timestamp;
  }

  /**
   * returns the default TTL for responses.  Used mainly by tests because Guice static injects TTL values.
   *
   * @return milliseconds of the ttl
   */
  public long getDefaultTtl() {
    return defaultTtl;
  }

  /**
   * Attempts to determine the encoding of the body. If it can't be determined, we use
   * DEFAULT_ENCODING instead.
   *
   * @return The detected encoding or DEFAULT_ENCODING.
   */
  private static Charset getAndUpdateEncoding(Multimap<String, String> headers, byte[] body) {
    if (body == null || body.length == 0) {
      return DEFAULT_ENCODING;
    }

    Collection<String> values = headers.get("Content-Type");
    if (!values.isEmpty()) {
      String contentType = values.iterator().next();
      String[] parts = StringUtils.split(contentType, ';');
      if (parts == null
          || parts.length == 0
          || BINARY_CONTENT_TYPES.contains(parts[0])) {
        return DEFAULT_ENCODING;
      }
      if (parts.length == 2) {
        int offset = parts[1].toLowerCase().indexOf("charset=");
        if (offset != -1) {
          String charset = parts[1].substring(offset + 8).toUpperCase();
          // Some servers include quotes around the charset:
          //   Content-Type: text/html; charset="UTF-8"
          if (charset.length() >= 2 && charset.startsWith("\"") && charset.endsWith("\"")) {
            charset = charset.substring(1, charset.length() - 1);
          }

          try {
            return charsetForName(charset);
          } catch (IllegalArgumentException e) {
            // fall through to detection
          }
        }
      }
      Charset encoding = EncodingDetector.detectEncoding(body, fastEncodingDetection,
          customEncodingDetector);
      // Record the charset in the content-type header so that its value can be cached
      // and re-used. This is a BIG performance win.
      values.clear();
      values.add(contentType + "; charset=" + encoding.name());

      return encoding;
    } else {
      // If no content type was specified, we'll assume an unknown binary type.
      return DEFAULT_ENCODING;
    }
  }

  /**
   * Cover for Charset.forName() that caches results.
   * @return the charset
   * @throws IllegalArgumentException if the encoding is invalid
   */
  private static Charset charsetForName(String encoding) {
    Charset charset = encodingToCharset.get(encoding);
    if (charset == null) {
      charset = Charset.forName(encoding);
      encodingToCharset.put(encoding, charset);
    }

    return charset;
  }

  @Override
  public int hashCode() {
    return httpStatusCode
      ^ headers.hashCode()
      ^ Arrays.hashCode(responseBytes);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) { return true; }
    if (!(obj instanceof HttpResponse)) { return false; }

    HttpResponse response = (HttpResponse)obj;

    return httpStatusCode == response.httpStatusCode &&
           headers.equals(response.headers) &&
           Arrays.equals(responseBytes, response.responseBytes);
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder("HTTP/1.1 ").append(httpStatusCode).append("\r\n\r\n");
    for (Map.Entry<String,String> entry : headers.entries()) {
      buf.append(entry.getKey()).append(": ").append(entry.getValue()).append("\r\n");
    }
    buf.append("\r\n").append(getResponseAsString()).append("\r\n");
    return buf.toString();
  }

  /**
   * @return The response as a byte array. Only visible to the package to avoid copying when
   * making a new HttpResponseBuilder.
   */
  byte[] getResponseAsBytes() {
    return responseBytes;
  }

  /**
   * Expected layout:
   *
   * int - status code
   * Map<String, List<String>> - headers
   * int - length of body
   * byte array - body, of previously specified length
   */
  @SuppressWarnings("unchecked")
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    httpStatusCode = in.readInt();

    // We store the multimap as a Map<String,List<String>> to insulate us from google-collections API churn
    // And to remain backwards compatible

    Map<String, List<String>> headerCopyMap = (Map<String, List<String>>)in.readObject();
    Multimap headerCopy = newHeaderMultimap();

    for (Map.Entry<String,List<String>> entry : headerCopyMap.entrySet()) {
      headerCopy.putAll(entry.getKey(), entry.getValue());
    }

    int bodyLength = in.readInt();
    responseBytes = new byte[bodyLength];
    int cnt, offset = 0;
    while ((cnt = in.read(responseBytes, offset, bodyLength)) > 0) {
      offset += cnt;
      bodyLength -= cnt;
    }
    if (offset != responseBytes.length) {
      throw new IOException("Invalid body! Expected length = " + responseBytes.length + ", bytes readed = " + offset + '.');
    }

    date = getAndUpdateDate(headerCopy);
    encoding = getAndUpdateEncoding(headerCopy, responseBytes);
    headers = Multimaps.unmodifiableMultimap(headerCopy);
    metadata = Collections.emptyMap();
  }

  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeInt(httpStatusCode);
    // Write out multimap as a map (see above)
    Map<String,List<String>> map = Maps.newHashMap();
    for (String key : headers.keySet()) {
      map.put(key, Lists.newArrayList(headers.get(key)));
    }
    out.writeObject(Maps.newHashMap(map));
    out.writeInt(responseBytes.length);
    out.write(responseBytes);
  }


  private static final Supplier<Collection<String>> HEADER_COLLECTION_SUPPLIER = new HeaderCollectionSupplier();

  private static class HeaderCollectionSupplier implements Supplier<Collection<String>> {
    public Collection<String> get() {
      return new LinkedList<String>();  //To change body of implemented methods use File | Settings | File Templates.
    }
  }
  public static Multimap<String,String> newHeaderMultimap() {
    TreeMap<String,Collection<String>> map = Maps.newTreeMap(String.CASE_INSENSITIVE_ORDER);
    return Multimaps.newMultimap(map, HEADER_COLLECTION_SUPPLIER);
  }
}
