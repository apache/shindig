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

import static junitx.framework.ComparableAssert.assertGreater;
import static junitx.framework.ComparableAssert.assertLesser;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.commons.lang3.StringUtils;
import org.apache.shindig.common.servlet.HttpUtil;
import org.apache.shindig.common.util.DateUtil;
import org.apache.shindig.common.util.FakeTimeSource;
import org.easymock.EasyMock;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

public class HttpUtilTest {

  public static final FakeTimeSource timeSource = new FakeTimeSource();
  public static final long testStartTime = timeSource.currentTimeMillis();

  static {
    HttpUtil.setTimeSource(timeSource);
  }

  private HttpServletResponse mockResponse = EasyMock.createMock(HttpServletResponse.class);
  private HttpServletResponseRecorder recorder = new HttpServletResponseRecorder(mockResponse);

  @Test
  public void testSetCachingHeaders() {
    HttpUtil.setCachingHeaders(recorder);
    checkCacheControlHeaders(testStartTime, recorder, HttpUtil.getDefaultTtl(), false);
  }

  @Test
  public void testSetCachingHeadersNoProxy() {
    HttpUtil.setCachingHeaders(recorder, true);

    checkCacheControlHeaders(testStartTime, recorder, HttpUtil.getDefaultTtl(), true);
  }

  @Test
  public void testSetCachingHeadersAllowProxy() {
    HttpUtil.setCachingHeaders(recorder, false);
    checkCacheControlHeaders(testStartTime, recorder, HttpUtil.getDefaultTtl(), false);
  }

  @Test
  public void testSetCachingHeadersFixedTtl() {
    int ttl = 10;
    HttpUtil.setCachingHeaders(recorder, ttl);
    checkCacheControlHeaders(testStartTime, recorder, ttl, false);
  }

  @Test
  public void testSetCachingHeadersWithTtlAndNoProxy() {
    int ttl = 20;
    HttpUtil.setCachingHeaders(recorder, ttl, true);
    checkCacheControlHeaders(testStartTime, recorder, ttl, true);
  }

  @Test
  public void testSetCachingHeadersNoCache() {
    HttpUtil.setCachingHeaders(recorder, 0);
    checkCacheControlHeaders(testStartTime, recorder, 0, true);
  }

  @Test
  public void testSetNoCche() {
    HttpUtil.setNoCache(recorder);
    checkCacheControlHeaders(testStartTime, recorder, 0, true);
  }

  @Test
  public void testCORSstar() {
    HttpUtil.setCORSheader(recorder, Collections.singleton("*"));
    assertEquals(recorder.getHeader(HttpUtil.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER), "*");
  }

  @Test
  public void testCORSnull() {
     HttpUtil.setCORSheader(recorder, null);
     assertEquals(recorder.getHeader(HttpUtil.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER), null);
   }

   @Test
   @Ignore("HttpServletResponseRecorder doesn't support multiple headers")
   public void testCORSmultiple() {
     HttpUtil.setCORSheader(recorder, Arrays.asList("http://foo.example.com", "http://bar.example.com"));
     // TODO fix HttpServletResponseRecorder and add multi-header test here
   }

  public static void checkCacheControlHeaders(long testStartTime,
      HttpServletResponseRecorder response, int ttl, boolean noProxy) {

    long expires = DateUtil.parseRfc1123Date(response.getHeader("Expires")).getTime();

    long lowerBound = testStartTime + (1000L * (ttl - 1));
    long upperBound = lowerBound + 2000L;

    assertGreater("Expires should be at least " + ttl + " seconds more than start time.",
        lowerBound, expires);

    assertLesser("Expires should be within 2 seconds of the requested value.",
        upperBound, expires);

    if (ttl == 0) {
      assertEquals("no-cache", response.getHeader("Pragma"));
      assertEquals("no-cache", response.getHeader("Cache-Control"));
    } else {
      List<String> directives
          = Arrays.asList(StringUtils.split(response.getHeader("Cache-Control"), ','));

      assertTrue("Incorrect max-age set.", directives.contains("max-age=" + ttl));
      if (noProxy) {
        assertTrue("No private Cache-Control directive was set.", directives.contains("private"));
      } else {
        assertTrue("No public Cache-Control directive was set.", directives.contains("public"));
      }
    }
  }

}
