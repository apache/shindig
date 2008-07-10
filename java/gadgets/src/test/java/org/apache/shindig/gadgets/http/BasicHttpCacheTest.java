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
import org.apache.commons.lang.ArrayUtils;

import junit.framework.TestCase;

import java.net.URI;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests for basic content cache
 */
public class BasicHttpCacheTest extends TestCase {

  private HttpCache cache;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    cache = new BasicHttpCache(10);
  }

  @Override
  protected void tearDown() throws Exception {
    cache = null;
    super.tearDown();
  }

  private HttpRequest createRequest(String method) {
    HttpRequest req = new HttpRequest(method,
        URI.create("http://www.here.com"), new HashMap<String, List<String>>(),
        ArrayUtils.EMPTY_BYTE_ARRAY, new HttpRequest.Options());
    return req;
  }

  private HttpResponse createResponse(int statusCode, String header,
      String headerValue) {
    Map<String, List<String>> headers = new HashMap<String, List<String>>();
    if (header != null) {
      headers.put(header, Arrays.asList(headerValue));
    }
    HttpResponse resp = new HttpResponse(statusCode, ArrayUtils.EMPTY_BYTE_ARRAY, headers);
    return resp;
  }

  private HttpResponse createExpiresResponse(int statusCode, long expiration) {
    Date newExpiry = new Date(expiration);
    return createResponse(statusCode, "Expires", DateUtil.formatDate(newExpiry));
  }

  private HttpResponse createMaxAgeResponse(int statusCode, long age) {
    return createResponse(statusCode, "Cache-Control", "max-age=" + age);
  }

  public void testEmptyCache() {
    assertNull(cache.getResponse(createRequest("GET")));
  }

  public void testCacheable() {
    HttpRequest req = createRequest("GET");
    HttpResponse resp = createResponse(200, null, null);
    cache.addResponse(req, resp);
    assertEquals(cache.getResponse(req), resp);
  }

  public void testNotCacheableForPost() {
    HttpRequest req = createRequest("POST");
    HttpResponse resp = createResponse(200, null, null);
    cache.addResponse(req, resp);
    assertNull(cache.getResponse(req));
  }

  public void testCacheableForErr() {
    HttpRequest req = createRequest("GET");
    HttpResponse resp = createResponse(500, null, null);
    cache.addResponse(req, resp);
    assertEquals(resp, cache.getResponse(req));
  }

  public void testCacheableForFutureExpires() {
    HttpRequest req = createRequest("GET");
    HttpResponse resp = createExpiresResponse(200,
        System.currentTimeMillis() + 10000L);
    cache.addResponse(req, resp);
    assertEquals(cache.getResponse(req), resp);
  }

  public void testNotCacheableForPastExpires() {
    HttpRequest req = createRequest("GET");
    HttpResponse resp = createExpiresResponse(200,
        System.currentTimeMillis() - 10000L);
    cache.addResponse(req, resp);
    assertNull(cache.getResponse(req));
  }

  public void testCacheableForFutureMaxAge() {
    HttpRequest req = createRequest("GET");
    HttpResponse resp = createMaxAgeResponse(200,
        10000L);
    cache.addResponse(req, resp);
    assertEquals(cache.getResponse(req), resp);
  }

  public void testNotCacheableForNoCache() {
    HttpRequest req = createRequest("GET");
    HttpResponse resp = createResponse(200, "Cache-Control", "no-cache");
    cache.addResponse(req, resp);
    assertNull(cache.getResponse(req));
  }

  public void testCacheableForExpiresWithWait() {
    HttpRequest req = createRequest("GET");
    HttpResponse resp = createExpiresResponse(200,
        System.currentTimeMillis() + 5000L);
    cache.addResponse(req, resp);
    try {
      synchronized (cache) {
        cache.wait(500L);
      }
    } catch (InterruptedException ie) {
      fail("Failed to wait for cache");
    }
    assertEquals(cache.getResponse(req), resp);
  }


  public void testNotCacheableForExpiresWithWait() {
    HttpRequest req = createRequest("GET");
    HttpResponse resp = createExpiresResponse(200,
        System.currentTimeMillis() + 1000L);
    cache.addResponse(req, resp);
    try {
      synchronized (cache) {
        cache.wait(1001L);
      }
    } catch (InterruptedException ie) {
      fail("Failed to wait for cache");
    }
    assertNull(cache.getResponse(req));
  }

}
