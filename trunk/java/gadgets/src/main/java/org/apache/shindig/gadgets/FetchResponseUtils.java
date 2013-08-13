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
package org.apache.shindig.gadgets;

import org.apache.shindig.gadgets.http.HttpResponse;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import java.util.Collection;
import java.util.Map;

/**
 * Handles converting HttpResponse objects to the format expected by the makeRequest javascript.
 */
public final class FetchResponseUtils {
  private FetchResponseUtils() {}
  /**
   * Convert a response to a JSON object.
   *
   * The returned JSON object contains the following values:
   * id: the id of the response
   * rc: integer response code
   * body: string response body
   * headers: object, keys are header names, values are lists of header values
   *
   * The returned object is guaranteed to be mutable.
   *
   * @param response the response body
   * @param id the response id, or null if not needed
   * @param body string to use as the body of the response.
   * @param getFullHeaders whether all response headers should be included,
   *     or only a small set
   * @return a JSONObject representation of the response body.
   */
  public static Map<String, Object> getResponseAsJson(HttpResponse response, String id,
      String body, boolean getFullHeaders) {
    Map<String, Object> resp = Maps.newHashMap();
    if (id != null) {
      resp.put("id", id);
    }
    resp.put("rc", response.getHttpStatusCode());
    resp.put("body", body);
    Map<String, Collection<String>> headers = Maps.newHashMap();
    if (getFullHeaders) {
      addAllHeaders(headers, response);
    } else {
      addHeaders(headers, response, "set-cookie");
      addHeaders(headers, response, "location");
    }
    if (!headers.isEmpty()) {
      resp.put("headers", headers);
    }
    // Merge in additional response data
    resp.putAll(response.getMetadata());

    return resp;
  }

  private static void addAllHeaders(Map<String, Collection<String>> headers,
      HttpResponse response) {
    Multimap<String, String> responseHeaders = response.getHeaders();
    for (String name : responseHeaders.keySet()) {
      headers.put(name.toLowerCase(), responseHeaders.get(name));
    }
  }

  private static void addHeaders(Map<String, Collection<String>> headers, HttpResponse response,
      String name) {
    Collection<String> values = response.getHeaders(name);
    if (!values.isEmpty()) {
      headers.put(name.toLowerCase(), values);
    }
  }

}
