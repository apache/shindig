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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;

import org.apache.shindig.common.util.DateUtil;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Represents the results of an HTTP content retrieval operation.
 *
 * HttpResponse objects are immutable in order to allow them to be safely used in concurrent
 * caches and by multiple threads without worrying about concurrent modification.
 */
public final class HttpResponse {
  public static final int SC_OK = 200;
  public static final int SC_MOVED_TEMPORARILY = 301;
  public static final int SC_MOVED_PERMANENTLY = 302;
  public static final int SC_UNAUTHORIZED = 401;
  public static final int SC_FORBIDDEN = 403;
  public static final int SC_NOT_FOUND = 404;
  public static final int SC_INTERNAL_SERVER_ERROR = 500;
  public static final int SC_TIMEOUT = 504;

  // These content types can always skip encoding detection.
  private static final Collection<String> BINARY_CONTENT_TYPES = Sets.newHashSet(
      "image/jpeg", "image/png", "image/gif", "image/jpg", "application/x-shockwave-flash",
      "application/octet-stream", "application/ogg", "application/zip", "audio/mpeg",
      "audio/x-ms-wma", "audio/vnd.rn-realaudio", "audio/x-wav", "video/mpeg", "video/mp4",
      "video/quicktime", "video/x-ms-wmv", "video/x-flv", "video/flv",
      "video/x-ms-asf", "application/pdf"
  );

  // These HTTP status codes should always honor the HTTP status returned by the remote host. All
  // other status codes are treated as errors and will use the negativeCacheTtl value.
  private static final Collection<Integer> CACHE_CONTROL_OK_STATUS_CODES
      = Sets.newHashSet(SC_OK, SC_UNAUTHORIZED, SC_FORBIDDEN);

  // TTL to use when an error response is fetched. This should be non-zero to
  // avoid high rates of requests to bad urls in high-traffic situations.
  static final long DEFAULT_NEGATIVE_CACHE_TTL = 30 * 1000;

  // Default TTL for an entry in the cache that does not have any cache control headers.
  static final long DEFAULT_TTL = 5L * 60L * 1000L;

  static final String DEFAULT_ENCODING = "UTF-8";

  @Inject @Named("shindig.http.cache.negativeCacheTtl")
  private static long negativeCacheTtl = DEFAULT_NEGATIVE_CACHE_TTL;

  @Inject @Named("shindig.http.cache.defaultTtl")
  private static long defaultTtl = DEFAULT_TTL;

  // Holds character sets for fast conversion
  private static final Map<String, Charset> encodingToCharset = Maps.newConcurrentHashMap();

  private final int httpStatusCode;
  private final Map<String, List<String>> headers;
  private final byte[] responseBytes;
  private String responseString;
  private final Map<String, String> metadata;

  private final long date;
  private final String encoding;

  /**
   * Construct an HttpResponse from a builder (called by HttpResponseBuilder.create).
   */
  HttpResponse(HttpResponseBuilder builder) {
    httpStatusCode = builder.getHttpStatusCode();
    Map<String, List<String>> headerCopy = Maps.newTreeMap(String.CASE_INSENSITIVE_ORDER);
    headerCopy.putAll(builder.getHeaders());

    // Always safe, HttpResponseBuilder won't modify the body.
    responseBytes = builder.getResponse();

    Map<String, String> metadataCopy = Maps.newHashMap(builder.getMetadata());
    metadata = Collections.unmodifiableMap(metadataCopy);

    // We want to modify the headers to ensure that the proper Content-Type and Date headers
    // have been set. This allows us to avoid these expensive calculations from the cache.
    date = getAndUpdateDate(headerCopy);
    encoding = getAndUpdateEncoding(headerCopy, responseBytes);
    headers = Collections.unmodifiableMap(headerCopy);
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

  public static HttpResponse timeout() {
    return new HttpResponse(SC_TIMEOUT, "");
  }

  public static HttpResponse notFound() {
    return new HttpResponse(SC_NOT_FOUND, "");
  }

  public int getHttpStatusCode() {
    return httpStatusCode;
  }

  /**
   * @return The encoding of the response body, if we're able to determine it.
   */
  public String getEncoding() {
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
      Charset charset = encodingToCharset.get(encoding);
      if (charset == null) {
        charset = Charset.forName(encoding);
        encodingToCharset.put(encoding, charset);
      }
      responseString = charset.decode(ByteBuffer.wrap(responseBytes)).toString();

      // Strip BOM if present
      if (responseString.length() > 0 && responseString.codePointAt(0) == 0xFEFF) {
        responseString = responseString.substring(1);
      }
    }
    return responseString;
  }

  /**
   * @return All headers for this object.
   */
  public Map<String, List<String>> getHeaders() {
    return headers;
  }

  /**
   * @return All headers with the given name. If no headers are set for the given name, an empty
   * collection will be returned.
   */
  public List<String> getHeaders(String name) {
    List<String> ret = headers.get(name);
    if (ret == null) {
      return Collections.emptyList();
    } else {
      return ret;
    }
  }

  /**
   * @return The first set header with the given name or null if not set. If you need multiple
   *         values for the header, use getHeaders().
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
   * @return additional data to embed in responses sent from the JSON proxy.
   */
  public Map<String, String> getMetadata() {
    return metadata;
  }

  /**
   * @return consolidated cache expiration time or -1
   */
  public long getCacheExpiration() {
    // We intentionally ignore cache-control headers for most HTTP error responses, because if
    // we don't we end up hammering sites that have gone down with lots of requests.  Proper
    // support for caching of OAuth responses is more complex, for that we have to respect
    // cache-control headers for 401s and 403s.
    if (!CACHE_CONTROL_OK_STATUS_CODES.contains(httpStatusCode)) {
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
   * @return Consolidated ttl or -1.
   */
  public long getCacheTtl() {
    long expiration = getCacheExpiration();
    if (expiration != -1) {
      return expiration - System.currentTimeMillis();
    }
    return -1;
  }

  /**
   * @return true if a strict no-cache header is set in Cache-Control or Pragma
   */
  public boolean isStrictNoCache() {
    String cacheControl = getHeader("Cache-Control");
    if (cacheControl != null) {
      String[] directives = cacheControl.split(",");
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
      Date expiresDate = DateUtil.parseDate(expires);
      if (expiresDate != null) {
        return expiresDate.getTime();
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
      String[] directives = cacheControl.split(",");
      for (String directive : directives) {
        directive = directive.trim();
        if (directive.startsWith("max-age")) {
          String[] parts = directive.split("=");
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
  private static long getAndUpdateDate(Map<String, List<String>> headers) {
    // Validate the Date header. Must conform to the HTTP date format.
    long timestamp = -1;
    List<String> dates = headers.get("Date");
    String dateStr = dates == null ? null : dates.isEmpty() ? null : dates.get(0);
    if (dateStr != null) {
      Date d = DateUtil.parseDate(dateStr);
      if (d != null) {
        timestamp = d.getTime();
      }
    }
    if (timestamp == -1) {
      timestamp = System.currentTimeMillis();
      headers.put("Date", Lists.newArrayList(DateUtil.formatDate(timestamp)));
    }
    return timestamp;
  }

  /**
   * Attempts to determine the encoding of the body. If it can't be determined, we use
   * DEFAULT_ENCODING instead.
   *
   * @return The detected encoding or DEFAULT_ENCODING.
   */
  private static String getAndUpdateEncoding(Map<String, List<String>> headers, byte[] body) {
    List<String> values = headers.get("Content-Type");
    String contentType = values == null ? null : values.isEmpty() ? null : values.get(0);
    if (contentType != null) {
      String[] parts = contentType.split(";");
      if (BINARY_CONTENT_TYPES.contains(parts[0])) {
        return DEFAULT_ENCODING;
      }
      if (parts.length == 2) {
        int offset = parts[1].indexOf("charset=");
        if (offset != -1) {
          return parts[1].substring(offset + 8).toUpperCase();
        }
      }
    }

    if (body == null || body.length == 0) {
      return DEFAULT_ENCODING;
    }

    // If the header doesn't specify the charset, try to determine it by examining the content.
    CharsetDetector detector = new CharsetDetector();
    detector.setText(body);
    CharsetMatch match = detector.detect();

    if (contentType != null) {
      // Record the charset in the content-type header so that its value can be cached
      // and re-used. This is a BIG performance win.
      headers.put("Content-Type",
          Lists.newArrayList(contentType + "; charset=" + match.getName().toUpperCase()));
    }
    return match.getName().toUpperCase();
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
    for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
      String name = entry.getKey();
      for (String value : entry.getValue()) {
        buf.append(name).append(": ").append(value).append('\n');
      }
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
}
