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

import org.apache.shindig.common.cache.CacheProvider;
import org.apache.shindig.common.cache.LruCacheProvider;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.util.DateUtil;

import junit.framework.TestCase;

import java.util.Date;

/**
 * Tests for basic content cache
 */
public class DefaultHttpCacheTest extends TestCase {

  private HttpCache cache;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    cache = new DefaultHttpCache(getCacheProvider());
  }


  @Override
  protected void tearDown() throws Exception {
    cache = null;
    super.tearDown();
  }

  /**
   * gets the cache provider to use for the set of tests
   * @return default cache provider
   */
  protected CacheProvider getCacheProvider() {
    return new LruCacheProvider(5);
  }

  private static HttpRequest createRequest(String method) {
    return new HttpRequest(Uri.parse("http://www.example.org")).setMethod(method);
  }

  private static HttpResponse createResponse(int statusCode, String header, String headerValue) {
   return new HttpResponseBuilder()
        .setHttpStatusCode(statusCode)
        .addHeader(header, headerValue)
        .create();
  }

  private HttpResponse createExpiresResponse(int statusCode, long expiration) {
    Date newExpiry = new Date(expiration);
    return createResponse(statusCode, "Expires", DateUtil.formatDate(newExpiry));
  }

  private HttpResponse createMaxAgeResponse(int statusCode, long age) {
    return createResponse(statusCode, "Cache-Control", "max-age=" + age);
  }

  public void testEmptyCache() {
    HttpRequest req = createRequest("GET");
    HttpCacheKey key = new HttpCacheKey(req);
    assertNull(cache.getResponse(key, req));
  }

  public void testCacheable() {
    HttpRequest req = createRequest("GET");
    HttpResponse resp = createResponse(200, null, null);
    HttpCacheKey key = new HttpCacheKey(req);
    cache.addResponse(key, req, resp);
    assertEquals(resp, cache.getResponse(key, req));
  }

  public void testNotCacheableForPost() {
    HttpRequest req = createRequest("POST");
    HttpResponse resp = createResponse(200, null, null);
    HttpCacheKey key = new HttpCacheKey(req);
    cache.addResponse(key, req, resp);
    assertNull(cache.getResponse(key, req));
  }

  public void testCacheableForErr() {
    HttpRequest req = createRequest("GET");
    HttpResponse resp = createResponse(500, null, null);
    HttpCacheKey key = new HttpCacheKey(req);
    cache.addResponse(key, req, resp);
    assertEquals(resp, cache.getResponse(key, req));
  }

  public void testCacheableForFutureExpires() {
    HttpRequest req = createRequest("GET");
    HttpResponse resp = createExpiresResponse(200,
        System.currentTimeMillis() + 10000L);
    HttpCacheKey key = new HttpCacheKey(req);
    cache.addResponse(key, req, resp);
    assertEquals(resp, cache.getResponse(key, req));
  }

  public void testNotCacheableForPastExpires() {
    HttpRequest req = createRequest("GET");
    HttpResponse resp = createExpiresResponse(200,
        System.currentTimeMillis() - 10000L);
    HttpCacheKey key = new HttpCacheKey(req);
    cache.addResponse(key, req, resp);
    assertNull(cache.getResponse(key, req));
  }

  public void testCacheableForFutureMaxAge() {
    HttpRequest req = createRequest("GET");
    HttpResponse resp = createMaxAgeResponse(200,
        10000L);
    HttpCacheKey key = new HttpCacheKey(req);
    cache.addResponse(key, req, resp);
    assertEquals(resp, cache.getResponse(key, req));
  }

  public void testNotCacheableForNoCache() {
    HttpRequest req = createRequest("GET");
    HttpResponse resp = createResponse(200, "Cache-Control", "no-cache");
    HttpCacheKey key = new HttpCacheKey(req);
    cache.addResponse(key, req, resp);
    assertNull(cache.getResponse(key, req));
  }

  public void testCacheableForExpiresWithWait() {
    HttpRequest req = createRequest("GET");
    HttpResponse resp = createExpiresResponse(200,
        System.currentTimeMillis() + 5000L);
    HttpCacheKey key = new HttpCacheKey(req);
    cache.addResponse(key, req, resp);
    try {
      synchronized (cache) {
        cache.wait(500L);
      }
    } catch (InterruptedException ie) {
      fail("Failed to wait for cache");
    }
    assertEquals(cache.getResponse(key, req), resp);
  }


  public void testNotCacheableForExpiresWithWait() {
    HttpRequest req = createRequest("GET");
    HttpResponse resp = createExpiresResponse(200,
        System.currentTimeMillis() + 1000L);
    HttpCacheKey key = new HttpCacheKey(req);
    cache.addResponse(key, req, resp);
    try {
      synchronized (cache) {
        cache.wait(1001L);
      }
    } catch (InterruptedException ie) {
      fail("Failed to wait for cache");
    }
    assertNull(cache.getResponse(key, req));
  }

  public void testCacheableWithForcedMinTTL() {
    HttpRequest req = createRequest("GET");
    // in seconds
    req.setCacheTtl(5);
    HttpResponse resp = createExpiresResponse(200, System.currentTimeMillis());
    HttpCacheKey key = new HttpCacheKey(req);
    cache.addResponse(key, req, resp);
    try {
      synchronized (cache) {
        cache.wait(2000L);
      }
    } catch (InterruptedException ie) {
      fail("Failed to wait for cache");
    }
    assertNotNull(cache.getResponse(key, req));
  }

}
