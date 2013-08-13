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
package org.apache.shindig.common.servlet;

import com.google.common.base.Preconditions;
import org.apache.shindig.common.Pair;
import org.apache.shindig.common.util.DateUtil;
import org.apache.shindig.common.util.TimeSource;

import com.google.common.collect.Lists;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Collection of HTTP utilities
 */
public final class HttpUtil {
  private HttpUtil() {}

  // 1 year.
  private static int defaultTtl = 60 * 60 * 24 * 365;

  private static TimeSource timeSource;

  static {
    setTimeSource(new TimeSource());
  }

  public static void setTimeSource(TimeSource timeSource) {
    HttpUtil.timeSource = timeSource;
  }

  public static TimeSource getTimeSource() {
    return timeSource;
  }

  /**
   * Sets HTTP headers that instruct the browser to cache content. Implementations should take care
   * to use cache-busting techniques on the url if caching for a long period of time.
   *
   * @param response The HTTP response
   */
  public static void setCachingHeaders(HttpServletResponse response) {
    setCachingHeaders(response, defaultTtl, false);
  }

  /**
   * Sets HTTP headers that instruct the browser to cache content. Implementations should take care
   * to use cache-busting techniques on the url if caching for a long period of time.
   *
   * @param response The HTTP response
   * @param noProxy True if you don't want the response to be cacheable by proxies.
   */
  public static void setCachingHeaders(HttpServletResponse response, boolean noProxy) {
    setCachingHeaders(response, defaultTtl, noProxy);
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

  public static void setNoCache(HttpServletResponse response) {
    setCachingHeaders(response, 0, false);
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
    for (Pair<String, String> header : getCachingHeadersToSet(ttl, noProxy)) {
      response.setHeader(header.one, header.two);
    }
  }

  public static List<Pair<String, String>> getCachingHeadersToSet(int ttl, boolean noProxy) {
    return getCachingHeadersToSet(ttl, null, null, noProxy);
  }

  public static List<Pair<String, String>> getCachingHeadersToSet(int ttl, String cacheControl, String pragma, boolean noProxy) {
    List<Pair<String, String>> cachingHeaders = Lists.newArrayListWithExpectedSize(3);
    cachingHeaders.add(Pair.of("Expires",
        DateUtil.formatRfc1123Date(timeSource.currentTimeMillis() + (1000L * ttl))));

    if (ttl <= 0) {
      cachingHeaders.add(Pair.of("Pragma", pragma == null ? "no-cache" : pragma));
      cachingHeaders.add(Pair.of("Cache-Control", cacheControl == null ? "no-cache" : cacheControl));
    } else {
      if (noProxy) {
        cachingHeaders.add(Pair.of("Cache-Control", "private,max-age=" + Integer.toString(ttl)));
      } else {
        cachingHeaders.add(Pair.of("Cache-Control", "public,max-age=" + Integer.toString(ttl)));
      }
    }

    return cachingHeaders;
  }

  public static int getDefaultTtl() {
    return defaultTtl;
  }

  public static void setDefaultTtl(int defaultTtl) {
    HttpUtil.defaultTtl = defaultTtl;
  }


  static final Pattern GET_REQUEST_CALLBACK_PATTERN = Pattern.compile("[A-Za-z_][A-Za-z0-9_\\.]+");

  public static boolean isJSONP(HttpServletRequest request) throws IllegalArgumentException {
    String callback = request.getParameter("callback");

    // Must be a GET
    if (!"GET".equals(request.getMethod()))
      return false;

    // No callback specified
    if (callback == null) return false;

    Preconditions.checkArgument(GET_REQUEST_CALLBACK_PATTERN.matcher(callback).matches(),
        "Wrong format for parameter 'callback' specified. Must match: " +
            GET_REQUEST_CALLBACK_PATTERN.toString());

    return true;
  }


  public static final String ACCESS_CONTROL_ALLOW_ORIGIN_HEADER = "Access-Control-Allow-Origin";

  /**
   * Set the header for Cross-Site Resource Sharing.
   * @param resp HttpServletResponse to modify
   * @param validOrigins a space separated list of Origins as defined by the html5 spec
   * @see <a href="http://dev.w3.org/html5/spec/browsers.html#origin-0">html 5 spec, section 5.3</a>
   */
  public static void setCORSheader(HttpServletResponse resp, Collection<String> validOrigins) {
    if (validOrigins == null) {
      return;
    }
    for (String origin : validOrigins) {
      resp.addHeader(ACCESS_CONTROL_ALLOW_ORIGIN_HEADER, origin);
    }
  }
}
