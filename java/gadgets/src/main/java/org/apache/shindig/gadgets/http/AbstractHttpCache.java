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

import org.apache.shindig.common.util.DateUtil;

import java.net.URI;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Base class for content caches. Defines cache expiration rules and
 * and restrictions on allowed content. Also enforces rewriting
 * on cacheable content.
 *
 * TODO: Move cache checking code into HttpUtil
 */
public abstract class AbstractHttpCache implements HttpCache {

  public final HttpResponse getResponse(HttpRequest request) {
    if (canCacheRequest(request)) {
      HttpResponse response = getResponse(request.getUri());
      return checkRewrite(request, response);
    }
    return null;
  }

  public final HttpResponse getResponse(URI uri) {
    if (uri == null) return null;
    return checkResponse(getResponseImpl(uri));
  }

  protected abstract HttpResponse getResponseImpl(URI uri);

  public HttpResponse addResponse(HttpRequest request, HttpResponse response) {
    if (canCacheRequest(request)) {
      // !!! Note that we only rewrite cacheable content. Move this call above the if
      // to rewrite all content that passes through the cache regardless of cacheability
      rewrite(request, response);
      addResponse(request.getUri(), response);
      return checkRewrite(request, response);
    }
    return response;
  }

  public HttpResponse addResponse(URI uri, HttpResponse response) {
    if (uri == null || response == null) return response;
    response = checkResponse(response);
    // Clone the URI to prevent outside references from preventing collection
    addResponseImpl(URI.create(uri.toString()), response);
    return response;
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
      Date expiresDate = DateUtil.parseDate(expires);
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
                  .put("Expires", Arrays.asList(DateUtil.formatDate(newExpiry)));
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
        .put("Expires", Arrays.asList(DateUtil.formatDate(newExpiry)));
    return response;
  }

  /**
   * Add rewritten content to the response if its not there and
   * we can add it. Re-cache if we created rewritten content.
   * Return the appropriately re-written version if requested
   */
  protected HttpResponse checkRewrite(HttpRequest request, HttpResponse response) {
    if (response == null) return null;

    // Perform a rewrite and store the content back to the cache if the
    // content is actually rewritten
    if (rewrite(request, response)) {
      addResponseImpl(request.getUri(), response);
    }

    // Return the rewritten version if requested
    if (request.getOptions() != null &&
        !request.getOptions().ignoreCache &&
        request.getOptions().rewriter != null &&
        response.getRewritten() != null &&
        response.getRewritten().getResponseAsBytes().length > 0) {
      return response.getRewritten();
    }
    return response;
  }

  /**
   * Rewrite the content
   * @return true if rewritten content was generated
   */
  protected boolean rewrite(HttpRequest request, HttpResponse response) {
    if (response == null) return false;
    // TODO - Make this sensitive to custom rewriting rules
    if (response.getRewritten() == null &&
        request.getOptions() != null &&
        request.getOptions().rewriter != null) {
      response.setRewritten(request.getOptions().rewriter.rewrite(request, response));
      if (response.getRewritten() != null) {
        return true;
      }
    }
    return false;
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
