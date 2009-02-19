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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

/**
 * Handles converting HttpResponse objects to the format expected by the makeRequest javascript.
 */
public class FetchResponseUtils {

  /**
   * Convert a response to a JSON object.
   * 
   * The returned JSON object contains the following values:
   * id: the id of the response
   * rc: integer response code
   * body: string response body
   * headers: object, keys are header names, values are lists of header values
   * 
   * @param response the response body
   * @param id the response id, or null if not needed
   * @param body string to use as the body of the response.
   * @return a JSONObject representation of the response body.
   */
  public static JSONObject getResponseAsJson(HttpResponse response, String id, String body)
      throws JSONException {
    JSONObject resp = new JSONObject();
    if (id != null) {
      resp.put("id", id);
    }
    resp.put("rc", response.getHttpStatusCode());
    resp.put("body", body);
    JSONObject headers = new JSONObject();
    addHeaders(headers, response, "set-cookie");
    addHeaders(headers, response, "location");
    resp.put("headers", headers);
    // Merge in additional response data
    for (Map.Entry<String, String> entry : response.getMetadata().entrySet()) {
      resp.put(entry.getKey(), entry.getValue());
    }
    return resp;
  }
  
  private static void addHeaders(JSONObject headers, HttpResponse response, String headerName)
      throws JSONException {
    List<String> values = response.getHeaders(headerName);
    if (!values.isEmpty()) {
      headers.put(headerName.toLowerCase(), new JSONArray(values));
    }
  }
  
}
