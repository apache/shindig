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
import com.google.inject.Inject;
import com.google.inject.name.Named;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;

import org.apache.commons.lang.ArrayUtils;
import org.apache.shindig.common.util.DateUtil;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents the results of an HTTP content retrieval operation.
 */
public class HttpResponse {

  // Replicate HTTP status codes here.
  public final static int SC_OK = 200;

  public final static int SC_UNAUTHORIZED = 401;

  public final static int SC_FORBIDDEN = 403;

  public final static int SC_NOT_FOUND = 404;

  public final static int SC_INTERNAL_SERVER_ERROR = 500;

  public final static int SC_TIMEOUT = 504;

  private final static Set<String> BINARY_CONTENT_TYPES = new HashSet<String>(Arrays.asList(
      "image/jpeg", "image/png", "image/gif", "image/jpg", "application/x-shockwave-flash"
  ));

  private final static Set<Integer> CACHE_CONTROL_OK_STATUS_CODES = new HashSet<Integer>(
      Arrays.asList(SC_OK, SC_UNAUTHORIZED, SC_FORBIDDEN));

  // TTL to use when an error response is fetched. This should be non-zero to
  // avoid high rates of requests to bad urls in high-traffic situations.
  protected final static long NEGATIVE_CACHE_TTL = 30 * 1000;

  /**
   * Default TTL for an entry in the cache that does not have any cache controlling headers.
   */
  protected static final long DEFAULT_TTL = 5L * 60L * 1000L;

  public static final String DEFAULT_ENCODING = "UTF-8";

  private final int httpStatusCode;

  // Derivation of encoding from content is EXPENSIVE using icu4j so be careful
  // how you construct, copy and store response objects.
  private final String encoding;

  // Used to lazily convert to a string representation of the input.
  private String responseString = null;

  private final byte[] responseBytes;

  private final Map<String, List<String>> headers;

  private final Map<String, String> metadata;

  private final long date;

  private HttpResponse rewritten;

  @Inject
  @Named("http.cache.negativeCacheTtl")
  private static long negativeCacheTtl = NEGATIVE_CACHE_TTL;

  @Inject
  @Named("http.cache.defaultTtl")
  private static long defaultTtl = DEFAULT_TTL;

  // Holds character sets for fast conversion
  private static ConcurrentHashMap<String, Charset> encodingToCharset
      = new ConcurrentHashMap<String, Charset>();

  /**
   * Create a dummy empty map. Access via HttpResponse.ERROR
   */
  public HttpResponse(int statusCode) {
    this(statusCode, ArrayUtils.EMPTY_BYTE_ARRAY, null, Charset.defaultCharset().name());
  }

  /**
   * @param headers May be null.
   */
  public HttpResponse(int httpStatusCode, byte[] responseBytes,
      Map<String, List<String>> headers) {
    this(httpStatusCode, responseBytes, headers, null);
  }

  /**
   * @param headers  May be null.
   * @param encoding May be null.
   */
  public HttpResponse(int httpStatusCode, byte[] responseBytes,
      Map<String, List<String>> headers,
      String encoding) {
    this.httpStatusCode = httpStatusCode;
    if (responseBytes == null) {
      this.responseBytes = ArrayUtils.EMPTY_BYTE_ARRAY;
    } else {
      this.responseBytes = new byte[responseBytes.length];
      System.arraycopy(
          responseBytes, 0, this.responseBytes, 0, responseBytes.length);
    }

    Map<String, List<String>> tmpHeaders =
        new TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER);
    if (headers != null) {
      for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
        if (entry.getKey() != null && entry.getValue() != null) {
          List<String> newList = new ArrayList<String>(entry.getValue());
          tmpHeaders.put(entry.getKey(), Collections.unmodifiableList(newList));
        }
      }
    }

    date = getValidDate(tmpHeaders);

    this.headers = tmpHeaders;

    this.metadata = new HashMap<String, String>();
    if (encoding == null) {
      if (responseBytes != null && responseBytes.length > 0) {
        this.encoding = detectEncoding();
      } else {
        this.encoding = Charset.defaultCharset().name();
      }
    } else {
      this.encoding = encoding;
    }
  }

  /**
   * Simple constructor for setting a basic response from a string. Mostly used for testing.
   */
  public HttpResponse(String body) {
    this(SC_OK, body.getBytes(), null, Charset.defaultCharset().name());
  }

  public static HttpResponse error() {
    return new HttpResponse(SC_INTERNAL_SERVER_ERROR);
  }

  public static HttpResponse timeout() {
    return new HttpResponse(SC_TIMEOUT);
  }

  public static HttpResponse notFound() {
    return new HttpResponse(SC_NOT_FOUND);
  }

  /**
   * Tries to find a valid date from the input headers. If one can't be found, the current time is
   * used.
   *
   * @param headers Input headers. If the Date header is missing or invalid, it will be set with the
   *                current time.
   * @return The value of the date header, in milliseconds.
   */
  private long getValidDate(Map<String, List<String>> headers) {
    // Validate the Date header. Must conform to the HTTP date format.
    long timestamp = -1;
    String dateStr = headers.get("Date") == null ? null : headers.get("Date").get(0);
    if (dateStr != null) {
      Date d = DateUtil.parseDate(dateStr);
      if (d != null) {
        timestamp = d.getTime();
      }
    }
    if (timestamp == -1) {
      timestamp = System.currentTimeMillis();
      headers.put("Date", Arrays.asList(DateUtil.formatDate(timestamp)));
    }
    return timestamp;
  }

  /**
   * Attempts to determine the encoding of the body. If it can't be determined, we use
   * DEFAULT_ENCODING instead.
   *
   * @return The detected encoding or DEFAULT_ENCODING.
   */
  private String detectEncoding() {
    String contentType = getHeader("Content-Type");
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

    // If the header doesn't specify the charset, try to determine it by examining the content.
    CharsetDetector detector = new CharsetDetector();
    detector.setText(responseBytes);
    CharsetMatch match = detector.detect();

    if (contentType != null) {
      // Record the charset in the content-type header so that its value can be cached
      // and re-used. This is a BIG performance win.
      this.headers.put("Content-Type",
          Lists.newArrayList(contentType + "; charset=" + match.getName().toUpperCase()));
    }
    return match.getName().toUpperCase();
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
   * UTF-8.
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
   * @return The response as a byte array
   */
  public byte[] getResponseAsBytes() {
    return responseBytes;
  }

  /**
   * @return All headers for this object.
   */
  public Map<String, List<String>> getAllHeaders() {
    return headers;
  }

  /**
   * @return All headers with the given name.
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
    return this.metadata;
  }

  /**
   * Get the rewritten version of this content
   *
   * @return A rewritten HttpResponse
   */
  public HttpResponse getRewritten() {
    return rewritten;
  }

  /**
   * Set the rewritten version of this content
   */
  public void setRewritten(HttpResponse rewritten) {
    this.rewritten = rewritten;
  }

  /**
   * Set the externally forced minimum cache min-TTL This is derived from the "refresh" param on
   * OpenProxy request Value is in seconds
   */
  public void setForcedCacheTTL(int forcedCacheTtl) {
    if (forcedCacheTtl > 0) {
      this.headers.remove("Expires");
      this.headers.remove("Pragma");
      this.headers.put("Cache-Control", Lists.newArrayList("public,max-age=" + forcedCacheTtl));
    }
  }

  /**
   * Sets cache-control headers indicating the response is not cacheable.
   */
  public void setNoCache() {
    this.headers.put("Cache-Control", Lists.newArrayList("no-cache"));
    this.headers.put("Pragma", Lists.newArrayList("no-cache"));
    this.headers.remove("Expires");
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
      return getDate() + negativeCacheTtl;
    }
    if (isStrictNoCache()) {
      return -1;
    }
    long maxAge = getCacheControlMaxAge();
    if (maxAge != -1) {
      return getDate() + maxAge;
    }
    long expiration = getExpiration();
    if (expiration != -1) {
      return expiration;
    }
    return getDate() + defaultTtl;
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

    List<String> pragmas = getHeaders("Pragma");
    if (pragmas != null) {
      for (String pragma : pragmas) {
        if ("no-cache".equalsIgnoreCase(pragma)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * @return The value of the HTTP Date header.
   */
  protected long getDate() {
    return date;
  }

  /**
   * @return the expiration time from the Expires header or -1 if not set
   */
  protected long getExpiration() {
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
  protected long getCacheControlMaxAge() {
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
            } catch (NumberFormatException e) {
              return -1;
            }
          }
        }
      }
    }
    return -1;
  }
}
