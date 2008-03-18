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

import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.SyndicatorConfig;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Set;

import javax.servlet.http.HttpServletResponse;

/**
 * Collection of HTTP utilities
 */
public class HttpUtil {
  public static final long START_TIME = System.currentTimeMillis();
  // 1 year.
  private static final int DEFAULT_TTL = 60 * 60 * 24 * 365;

  /**
   * Sets default caching Headers (Expires, Cache-Control, Last-Modified)
   *
   * @param response The HTTP response
   */
  public static void setCachingHeaders(HttpServletResponse response) {
    setCachingHeaders(response, DEFAULT_TTL);
  }

  /**
   * Sets HTTP headers that instruct the browser to cache indefinitely.
   * Implementations should take care to use cache-busting techniques on the
   * url.
   *
   * @param response The HTTP response
   * @param ttl The time to cache for, in seconds. If 0, then insure that
   * this object is not cached.
   */
  public static void setCachingHeaders(HttpServletResponse response, int ttl) {
    response.setDateHeader("Expires",
        System.currentTimeMillis() + (1000L * ttl));

    // IE seems to need this (10 years should be enough).
    response.setHeader("Cache-Control", "public,max-age=" +
        Integer.toString(ttl));

    // Firefox requires this for certain cases.
    response.setDateHeader("Last-Modified", START_TIME);
  }

  /**
   * Fetches js configuration for the given feature set & syndicator
   * @param config
   * @param context
   * @param features
   */
  public static JSONObject getJsConfig(SyndicatorConfig config,
      GadgetContext context, Set<String> features) {
    JSONObject syndFeatures = config.getJsonObject(context.getSyndicator(),
                                                   "gadgets.features");
    if (syndFeatures != null) {
      String[] featArray = features.toArray(new String[features.size()]);
      try {
        return new JSONObject(syndFeatures, featArray);
      } catch (JSONException e) {
        return null;
      }
    }
    return new JSONObject();
  }
}
