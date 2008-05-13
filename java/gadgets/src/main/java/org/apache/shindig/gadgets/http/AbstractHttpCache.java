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

import java.net.URI;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Base class for content caches. Defines cache expiration rules and
 * and restrictions on allowed content.
 *
 * TODO: Move cache checking code into HttpUtil
 */
public abstract class AbstractHttpCache implements HttpCache {

  public final HttpResponse getResponse(HttpRequest request) {
    if (canCacheRequest(request)) {
      return getResponse(request.getUri());
    }
    return null;
  }

  public final HttpResponse getResponse(URI uri) {
    if (uri == null) return null;
    return checkResponse(getResponseImpl(uri));
  }

  protected abstract HttpResponse getResponseImpl(URI uri);

  public void addResponse(HttpRequest request, HttpResponse response) {
    if (canCacheRequest(request)) {
      addResponse(request.getUri(), response);
    }
  }

  public void addResponse(URI uri, HttpResponse response) {
    response = checkResponse(response);
    if (uri == null || response == null) return;
    // Clone the URI to prevent outside references from preventing collection
    addResponseImpl(URI.create(uri.toString()), response);
  }

  protected abstract void addResponseImpl(URI uri, HttpResponse response);

  public HttpResponse removeResponse(HttpRequest request) {
    return removeResponse(request.getUri());
  }

  public HttpResponse removeResponse(URI uri) {
    if (uri == null) return null;
    HttpResponse response = getResponseImpl(uri);
    removeResponseImpl(uri);
    return checkResponse(response);
  }

  protected abstract HttpResponse removeResponseImpl(URI uri);

  /**
   * Utility function to verify that an entry is cacheable and not expired
   * Returns null if the content is no longer cacheable.
   *
   * @param request
   * @return content or null
   */
  protected boolean canCacheRequest(HttpRequest request) {
    return ("GET".equals(request.getMethod()) &&
        !request.getOptions().ignoreCache);
  }

  /**
   * Utility function to verify that an entry is cacheable and not expired
   * Returns null if the content is no longer cacheable.
   *
   * @param response
   * @return content or null
   */
  protected HttpResponse checkResponse(HttpResponse response) {
    if (response == null) return null;

    if (response.getHttpStatusCode() != 200) return null;

    long now = System.currentTimeMillis();

    String expires = response.getHeader("Expires");
    if (expires != null) {
      Date expiresDate = HttpUtil.parseDate(expires);
      if (expiresDate == null) {
        // parse problem
        return null;
      }
      long expiresMs = expiresDate.getTime();
      if (expiresMs > now) {
        return response;
      } else {
        return null;
      }
    }

    // Cache-Control headers may be an explicit max-age, or no-cache, which
    // means we use a default expiration time.
    String cacheControl = response.getHeader("Cache-Control");
    if (cacheControl != null) {
      String[] directives = cacheControl.split(",");
      for (String directive : directives) {
        directive = directive.trim();
        // boolean params
        if (directive.equals("no-cache")) {
          return null;
        }
        if (directive.startsWith("max-age")) {
          String[] parts = directive.split("=");
          if (parts.length == 2) {
            try {
              // Record the max-age and store it in the content as an
              // absolute expiration
              long maxAgeMs = Long.parseLong(parts[1]) * 1000;
              Date newExpiry = new Date(now + maxAgeMs);
              response.getAllHeaders()
                  .put("Expires", Arrays.asList(HttpUtil.formatDate(newExpiry)));
              return response;
            } catch (NumberFormatException e) {
              return null;
            }
          }
        }
      }
    }

    // Look for Pragma: no-cache. If present, return null.
    List<String> pragmas = response.getHeaders("Pragma");
    if (pragmas != null) {
      for (String pragma : pragmas) {
        if ("no-cache".equals(pragma)) {
          return null;
        }
      }
    }

    // Assume the content is cacheable for the default TTL
    // if no other directives exist
    Date newExpiry = new Date(now + getDefaultTTL());
    response.getAllHeaders()
        .put("Expires", Arrays.asList(HttpUtil.formatDate(newExpiry)));
    return response;
  }

  /**
   * Default TTL for an entry in the cache that does not have any
   * cache controlling headers
   * @return default TTL for cache entries
   */
  protected long getDefaultTTL() {
    // 5 mins
    return 5L * 60L * 1000L;
  }
}
