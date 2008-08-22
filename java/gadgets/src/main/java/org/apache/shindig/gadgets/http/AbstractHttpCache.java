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

  public HttpResponse addResponse(HttpCacheKey key, HttpRequest request, HttpResponse response) {
    if (key.isCacheable() && response != null) {
      // !!! Note that we only rewrite cacheable content. Move this call above the if
      // to rewrite all content that passes through the cache regardless of cacheability
      HttpResponse rewritten = rewrite(request, response);

      HttpResponseBuilder responseBuilder = new HttpResponseBuilder(response);
      responseBuilder.setRewritten(rewritten);
      int forcedTtl = request.getCacheTtl();
      if (forcedTtl != -1) {
        responseBuilder.setCacheTtl(request.getCacheTtl());
      }

      response = responseBuilder.create();

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
   * @return true If the response can be used.
   */
  protected boolean responseStillUsable(HttpResponse response) {
    if (response == null) {
      return false;
    }    
    return response.getCacheExpiration() > System.currentTimeMillis();
  }

  /**
   * Add rewritten content to the response if its not there and
   * we can add it. Re-cache if we created rewritten content.
   * Return the appropriately re-written version if requested
   */
  protected HttpResponse checkRewrite(String key, HttpRequest request, HttpResponse response) {
    if (response == null) {
      return null;
    }

    // Perform a rewrite and store the content back to the cache if the
    // content is actually rewritten
    HttpResponse rewritten = rewrite(request, response);

    if (rewritten != null) {
      // TODO: Remove this when new rewriting mechanism is ready.
      response = new HttpResponseBuilder(response).setRewritten(rewritten).create();
      addResponseImpl(key, response);
    }

    // Return the rewritten version if requested
    if (!request.getIgnoreCache() &&
        request.getContentRewriter() != null &&
        response.getRewritten() != null &&
        response.getRewritten().getContentLength() > 0) {
      return response.getRewritten();
    }
    return response;
  }

  /**
   * Rewrite the content
   * @return true if rewritten content was generated
   */
  protected HttpResponse rewrite(HttpRequest request, HttpResponse response) {
    // TODO - Make this sensitive to custom rewriting rules
    if (response.getRewritten() == null && request.getContentRewriter() != null) {
      return request.getContentRewriter().rewrite(request, response);
    }
    return null;
  }
}
