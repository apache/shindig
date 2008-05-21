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

import org.apache.shindig.gadgets.servlet.HttpUtil;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents the results of an HTTP content retrieval operation.
 */
public class HttpResponse {
  // Replicate HTTP status codes here.
  public final static int SC_OK = 200;
  public final static int SC_NOT_FOUND = 404;
  public final static int SC_INTERNAL_SERVER_ERROR = 500;
  public final static int SC_TIMEOUT = 504;

  private final int httpStatusCode;
  private static final String DEFAULT_ENCODING = "UTF-8";
  private final String encoding;

  public static final HttpResponse ERROR
      = new HttpResponse(SC_INTERNAL_SERVER_ERROR);
  public static final HttpResponse TIMEOUT = new HttpResponse(SC_TIMEOUT);
  public static final HttpResponse NOT_FOUND = new HttpResponse(SC_NOT_FOUND);

  // Used to lazily convert to a string representation of the input.
  private String responseString = null;
  private final byte[] responseBytes;
  private final Map<String, List<String>> headers;
  private final Map<String, String> metadata;

  private HttpResponse rewritten;

  // Holds character sets for fast conversion
  private static ConcurrentHashMap<String,Charset> encodingToCharset= new ConcurrentHashMap<String,Charset>();

  /**
   * Create a dummy empty map. Access via HttpResponse.ERROR
   */
  private HttpResponse(int statusCode) {
    this.httpStatusCode = statusCode;
    this.responseBytes = new byte[0];
    this.encoding = DEFAULT_ENCODING;
    this.headers = Collections.emptyMap();
    this.metadata = new HashMap<String, String>();
  }

  /**
   * @param httpStatusCode
   * @param responseBytes
   * @param headers May be null.
   */
  public HttpResponse(int httpStatusCode, byte[] responseBytes,
                       Map<String, List<String>> headers) {
    this.httpStatusCode = httpStatusCode;
    if (responseBytes == null) {
      this.responseBytes = new byte[0];
    } else {
      this.responseBytes = new byte[responseBytes.length];
      System.arraycopy(
          responseBytes, 0, this.responseBytes, 0, responseBytes.length);
    }

    if (headers == null) {
      this.headers = Collections.emptyMap();
    } else {
      Map<String, List<String>> tmpHeaders
          = new HashMap<String, List<String>>();
      for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
        List<String> newList = new ArrayList<String>(entry.getValue());
        tmpHeaders.put(entry.getKey(), Collections.unmodifiableList(newList));
      }
      this.headers = tmpHeaders;
    }
    this.metadata = new HashMap<String, String>();
    this.encoding = detectEncoding();
  }

  /**
   * Simple constructor for setting a basic response from a string. Mostly used
   * for testing.
   *
   * @param body
   */
  public HttpResponse(String body) {
    this(SC_OK, body.getBytes(), null);
  }

  /**
   * Attempts to determine the encoding of the body. If it can't be determined,
   * we use DEFAULT_ENCODING instead.
   * @return The detected encoding or DEFAULT_ENCODING.
   */
  private String detectEncoding() {
    String contentType = getHeader("Content-Type");
    if (contentType != null) {
      String[] parts = contentType.split(";");
      if (parts.length == 2) {
        int offset = parts[1].indexOf("charset=");
        if (offset != -1) {
          return parts[1].substring(offset + 8).toUpperCase();
        }
      }
    }
    return DEFAULT_ENCODING;
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
   * Attempts to convert the response body to a string using the Content-Type
   * header. If no Content-Type header is specified (or it doesn't include an
   * encoding), we will assume it is UTF-8.
   *
   * @return The body as a string.
   */
  public String getResponseAsString() {
    if (responseString == null) {
      Charset charset = encodingToCharset.get(encoding);
      if (charset == null) {
        charset = Charset.forName(encoding);
        encodingToCharset.put(encoding,charset);
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
   * @param name
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
   * @param name
   * @return The first set header with the given name or null if not set. If
   *         you need multiple values for the header, use getHeaders().
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
   * @return A rewritten HttpResponse
   */
  public HttpResponse getRewritten() {
    return rewritten;
  }

  /**
   * Set the rewritten version of this content
   * @param rewritten
   */
  public void setRewritten(HttpResponse rewritten) {
    this.rewritten = rewritten;
  }

  /**
   * @return consolidated cache expiration time or -1
   */
  public long getCacheExpiration() {
    if (isStrictNoCache()) return -1;
    long maxAgeExpiration = getCacheControlMaxAge() + System.currentTimeMillis();
    long expiration = getExpiration();
    if (expiration == -1) {
      return maxAgeExpiration;
    }
    return expiration;
  }

  /**
   * @return the expiration time from the Expires header or -1 if not set
   */
  public long getExpiration() {
    String expires = getHeader("Expires");
    if (expires != null) {
      Date expiresDate = HttpUtil.parseDate(expires);
      if (expiresDate != null) {
        return expiresDate.getTime();
      }
    }
    return -1;
  }

  /**
   * @return max-age value or -1 if invalid or not set
   */
  public long getCacheControlMaxAge() {
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
}
