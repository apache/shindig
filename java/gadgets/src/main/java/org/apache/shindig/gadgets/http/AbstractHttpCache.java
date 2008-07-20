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

/**
 * Base class for content caches. Defines cache expiration rules and
 * and restrictions on allowed content. Also enforces rewriting
 * on cacheable content.
 */
public abstract class AbstractHttpCache implements HttpCache {

  public final HttpResponse getResponse(HttpCacheKey key, HttpRequest request) {
    if (key.isCacheable()) {
      String keyString = key.toString();
      HttpResponse cached = getResponseImpl(keyString);
      if (responseStillUsable(cached)) {
        return checkRewrite(keyString, request, cached);
      }
    }
    return null;
  }

  protected abstract HttpResponse getResponseImpl(String key);

  public HttpResponse addResponse(HttpCacheKey key, HttpRequest request,
      HttpResponse response) {
    if (key.isCacheable()) {
      // If the request forces a minimum TTL for the cached content then have
      // the response honor it
      if (response != null) {
        response.setForcedCacheTTL(request.getOptions().minCacheTtl);
      }

      // !!! Note that we only rewrite cacheable content. Move this call above the if
      // to rewrite all content that passes through the cache regardless of cacheability
      rewrite(request, response);
      String keyString = key.toString();
      if (responseStillUsable(response)) {
        addResponseImpl(keyString, response);
      }
      return checkRewrite(keyString, request, response);
    }
    return response;
  }

  protected abstract void addResponseImpl(String key, HttpResponse response);

  public HttpResponse removeResponse(HttpCacheKey key) {
    String keyString = key.toString();
    HttpResponse response = getResponseImpl(keyString);
    removeResponseImpl(keyString);
    if (responseStillUsable(response)) {
      return response;
    }
    return null;
  }

  protected abstract HttpResponse removeResponseImpl(String key);

  /**
   * Utility function to verify that an entry is cacheable and not expired
   * Returns false if the content is no longer cacheable.
   *
   * @param response
   * @return true if the response can be used.
   */
  protected boolean responseStillUsable(HttpResponse response) {
    if (response == null) {
      return false;
    }
    if (response.getCacheExpiration() < System.currentTimeMillis()) {
      return false;
    }
    return true;
  }

  /**
   * Add rewritten content to the response if its not there and
   * we can add it. Re-cache if we created rewritten content.
   * Return the appropriately re-written version if requested
   */
  protected HttpResponse checkRewrite(String keyString, HttpRequest request,
      HttpResponse response) {
    if (response == null) {
      return null;
    }

    // Perform a rewrite and store the content back to the cache if the
    // content is actually rewritten
    if (rewrite(request, response)) {
      addResponseImpl(keyString, response);
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
}
