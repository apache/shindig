/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.apache.shindig.gadgets;

import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Test utility to intercept remote content requests for inspection.
 */
public class InterceptingContentFetcher extends ChainedContentFetcher {

  protected InterceptingContentFetcher() {
    super(null);
  }

  public HttpRequest interceptedRequest;

  public HttpResponse fetch(HttpRequest request) {
    interceptedRequest = request;
    try {
      JSONObject resp = new JSONObject();
      resp.put("url", request.getUri().toString());
      resp.put("method", request.getMethod());
      resp.put("body", request.getPostBodyAsString());
      return new HttpResponse(resp.toString());
    } catch (JSONException e) {
      return new HttpResponse(e.toString());
    }
  }

}
