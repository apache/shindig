/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.shindig.gadgets;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Represents the results of an HTTP content retrieval operation.
 */
public class RemoteContent {
  private final int httpStatusCode;
  private static final String DEFAULT_ENCODING = "UTF-8";
  private final String encoding;

  // Used to lazily convert to a string representation of the input.
  private String responseString = null;
  private final byte[] responseBytes;
  private final Map<String, List<String>> headers;

  /**
   * @param httpStatusCode
   * @param resultBody
   * @param headers May be null.
   */
  public RemoteContent(int httpStatusCode, byte[] responseBytes,
                       Map<String, List<String>> headers) {
    this.httpStatusCode = httpStatusCode;
    if (responseBytes == null) {
      this.responseBytes = new byte[0];
    } else {
      this.responseBytes = responseBytes;
    }

    Map<String, List<String>> tempHeaders = new HashMap<String, List<String>>();

    if (headers != null) {
      for (Map.Entry<String, List<String>> header : headers.entrySet()) {
        List<String> values = new LinkedList<String>();
        for (String value : header.getValue()) {
          values.add(value);
        }
        tempHeaders.put(header.getKey(), values);
      }
    }

    this.headers = Collections.unmodifiableMap(tempHeaders);
    this.encoding = detectEncoding();
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
          return parts[1].substring(offset + 8);
        }
      }
    }
    return DEFAULT_ENCODING;
  }

  public int getHttpStatusCode() {
    return httpStatusCode;
  }

  public byte[] getByteArray() {
    return responseBytes;
  }

  public String getEncoding() {
    return encoding;
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
      try {
        String response = new String(responseBytes, encoding);
        // Strip BOM.
        if (response.length() > 0 && response.codePointAt(0) == 0xFEFF) {
          responseString = response.substring(1);
        } else {
          responseString = response;
        }
      } catch (UnsupportedEncodingException e) {
        responseString = "Unable to convert from encoding: " + encoding;
      }
    }
    return responseString;
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
      return Collections.unmodifiableList(ret);
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
}
