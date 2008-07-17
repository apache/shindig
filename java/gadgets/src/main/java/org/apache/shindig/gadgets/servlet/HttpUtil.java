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
package org.apache.shindig.gadgets.servlet;

import org.apache.shindig.common.ContainerConfig;
import org.apache.shindig.gadgets.GadgetContext;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;

import javax.servlet.http.HttpServletResponse;

/**
 * Collection of HTTP utilities
 */
public class HttpUtil {
  public static final long START_TIME = System.currentTimeMillis();
  // 1 year.
  public static final int DEFAULT_TTL = 60 * 60 * 24 * 365;

  /**
   * Sets HTTP headers that instruct the browser to cache content. Implementations should take care
   * to use cache-busting techniques on the url if caching for a long period of time.
   *
   * @param response The HTTP response
   */
  public static void setCachingHeaders(HttpServletResponse response) {
    setCachingHeaders(response, DEFAULT_TTL, false);
  }

  /**
   * Sets HTTP headers that instruct the browser to cache content. Implementations should take care
   * to use cache-busting techniques on the url if caching for a long period of time.
   *
   * @param response The HTTP response
   * @param noProxy True if you don't want the response to be cacheable by proxies.
   */
  public static void setCachingHeaders(HttpServletResponse response, boolean noProxy) {
    setCachingHeaders(response, DEFAULT_TTL, noProxy);
  }

  /**
   * Sets HTTP headers that instruct the browser to cache content. Implementations should take care
   * to use cache-busting techniques on the url if caching for a long period of time.
   *
   * @param response The HTTP response
   * @param ttl The time to cache for, in seconds. If 0, then insure that
   *            this object is not cached.
   */
  public static void setCachingHeaders(HttpServletResponse response, int ttl) {
    setCachingHeaders(response, ttl, false);
  }

  /**
   * Sets HTTP headers that instruct the browser to cache content. Implementations should take care
   * to use cache-busting techniques on the url if caching for a long period of time.
   *
   * @param response The HTTP response
   * @param ttl The time to cache for, in seconds. If 0, then insure that
   *            this object is not cached.
   * @param noProxy True if you don't want the response to be cacheable by proxies.
   */
  public static void setCachingHeaders(HttpServletResponse response, int ttl, boolean noProxy) {
    response.setDateHeader("Expires", System.currentTimeMillis() + (1000L * ttl));

    if (ttl == 0) {
      response.setHeader("Pragma", "no-cache");
      response.setHeader("Cache-Control", "no-cache");
    } else {
      if (noProxy) {
        response.setHeader("Cache-Control", "private,max-age=" + Integer.toString(ttl));
      } else {
        response.setHeader("Cache-Control", "public,max-age=" + Integer.toString(ttl));
      }
      // Firefox requires this for certain cases.
      response.setDateHeader("Last-Modified", START_TIME);
    }
  }

  /**
   * Fetches js configuration for the given feature set & container.
   *
   * @param config The configuration to extract js config from.
   * @param context The request context.
   * @param features A set of all features needed.
   */
  public static JSONObject getJsConfig(ContainerConfig config, GadgetContext context,
      Collection<String> features) {
    JSONObject containerFeatures = config.getJsonObject(context.getContainer(),
                                                        "gadgets.features");
    if (containerFeatures != null) {
      String[] featArray = features.toArray(new String[features.size()]);
      try {
        return new JSONObject(containerFeatures, featArray);
      } catch (JSONException e) {
        // Can't happen.
        throw new RuntimeException(e);
      }
    }
    return new JSONObject();
  }
}
