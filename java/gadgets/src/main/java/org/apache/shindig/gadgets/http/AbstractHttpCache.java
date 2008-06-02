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

import java.net.URI;

/**
 * Base class for content caches. Defines cache expiration rules and
 * and restrictions on allowed content. Also enforces rewriting
 * on cacheable content.
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
    if (response == null) {
      return null;
    }
    if (response.getCacheExpiration() < System.currentTimeMillis()) {
      return null;
    }
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
}
