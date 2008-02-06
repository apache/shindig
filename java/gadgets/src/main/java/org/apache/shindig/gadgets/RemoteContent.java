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

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Represents the results of an HTTP content retrieval operation.
 */
public class RemoteContent {
  private int httpStatusCode = -1;
  private byte[] resultBody = new byte[0];
  private Map<String, List<String>> headers;

  /**
   * @param httpStatusCode
   * @param resultBody
   * @param headers May be null.
   */
  public RemoteContent(int httpStatusCode, byte[] resultBody,
                       Map<String, List<String>> headers) {
    this.httpStatusCode = httpStatusCode;
    if (resultBody != null) {
      this.resultBody = resultBody;
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
  }

  public int getHttpStatusCode() {
    return httpStatusCode;
  }

  public byte[] getByteArray() {
    return resultBody;
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
