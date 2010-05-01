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
 * and restrictions on allowed content.
 */
public abstract class AbstractHttpCache implements HttpCache {

  // Implement these methods to create a concrete HttpCache class.
  protected abstract HttpResponse getResponseImpl(String key);
  protected abstract void addResponseImpl(String key, HttpResponse response);
  protected abstract HttpResponse removeResponseImpl(String key);
  
  public final HttpResponse getResponse(HttpCacheKey key, HttpRequest request) {
    if (key.isCacheable()) {
      String keyString = key.toString();
      HttpResponse cached = getResponseImpl(keyString);
      if (responseStillUsable(cached)) {
        return cached;
      }
    }
    return null;
  }

  public HttpResponse addResponse(HttpCacheKey key, HttpRequest request, HttpResponse response) {
    if (key.isCacheable() && response != null) {
      // !!! Note that we only rewrite cacheable content. Move this call above the if
      // to rewrite all content that passes through the cache regardless of cacheability.
      HttpResponseBuilder responseBuilder = new HttpResponseBuilder(response);
      int forcedTtl = request.getCacheTtl();
      if (forcedTtl != -1) {
        responseBuilder.setCacheTtl(forcedTtl);
      }

      response = responseBuilder.create();
      addResponseImpl(key.toString(), response);
    }
    
    return response;
  }

  public HttpResponse removeResponse(HttpCacheKey key) {
    String keyString = key.toString();
    HttpResponse response = getResponseImpl(keyString);
    removeResponseImpl(keyString);
    if (responseStillUsable(response)) {
      return response;
    }
    return null;
  }

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
}
