/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.gadgets.http;

import org.apache.shindig.common.util.DateUtil;

import junit.framework.TestCase;

import org.apache.commons.io.IOUtils;

import java.util.Arrays;

public class HttpResponseTest extends TestCase {
  private static final byte[] UTF8_DATA = new byte[] {
    (byte)0xEF, (byte)0xBB, (byte)0xBF, 'h', 'e', 'l', 'l', 'o'
  };
  private static final String UTF8_STRING = "hello";

  // A large string is needed for accurate charset detection.
  private static final byte[] LATIN1_DATA = new byte[] {
    'G', 'a', 'm', 'e', 's', ',', ' ', 'H', 'Q', ',', ' ', 'M', 'a', 'n', 'g', (byte)0xE1, ',', ' ',
    'A', 'n', 'i', 'm', 'e', ' ', 'e', ' ', 't', 'u', 'd', 'o', ' ', 'q', 'u', 'e', ' ', 'u', 'm',
    ' ', 'b', 'o', 'm', ' ', 'n', 'e', 'r', 'd', ' ', 'a', 'm', 'a'
  };
  private static final String LATIN1_STRING
      = "Games, HQ, Mang\u00E1, Anime e tudo que um bom nerd ama";

  private static final byte[] BIG5_DATA = new byte[] {
    (byte)0xa7, (byte)0x41, (byte)0xa6, (byte)0x6e
  };

  private static final String BIG5_STRING = "\u4F60\u597D";


  private static int roundToSeconds(long ts) {
    return (int)(ts / 1000);
  }

  public void testGetEncoding() throws Exception {
    HttpResponse response = new HttpResponseBuilder()
        .addHeader("Content-Type", "text/plain; charset=TEST-CHARACTER-SET")
        .create();
    assertEquals("TEST-CHARACTER-SET", response.getEncoding());
  }

  public void testEncodingDetectionUtf8WithBom() throws Exception {
     HttpResponse response = new HttpResponseBuilder()
         .addHeader("Content-Type", "text/plain; charset=UTF-8")
         .setResponse(UTF8_DATA)
         .create();
    assertEquals(UTF8_STRING, response.getResponseAsString());
  }

  public void testEncodingDetectionLatin1() throws Exception {
    // Input is a basic latin-1 string with 1 non-UTF8 compatible char.
    HttpResponse response = new HttpResponseBuilder()
        .addHeader("Content-Type", "text/plain; charset=iso-8859-1")
        .setResponse(LATIN1_DATA)
        .create();
    assertEquals(LATIN1_STRING, response.getResponseAsString());
  }

  public void testEncodingDetectionBig5() throws Exception {
    HttpResponse response = new HttpResponseBuilder()
        .addHeader("Content-Type", "text/plain; charset=BIG5")
        .setResponse(BIG5_DATA)
        .create();
    assertEquals(BIG5_STRING, response.getResponseAsString());
  }

  public void testEncodingDetectionUtf8WithBomNoCharsetSpecified() throws Exception {
    HttpResponse response = new HttpResponseBuilder()
        .addHeader("Content-Type", "text/plain")
        .setResponse(UTF8_DATA)
        .create();
    assertEquals("UTF-8", response.getEncoding().toUpperCase());
    assertEquals(UTF8_STRING, response.getResponseAsString());
  }

  public void testEncodingDetectionLatin1NoCharsetSpecified() throws Exception {
    HttpResponse response = new HttpResponseBuilder()
        .addHeader("Content-Type", "text/plain;")
        .setResponse(LATIN1_DATA)
        .create();
    assertEquals("ISO-8859-1", response.getEncoding().toUpperCase());
    assertEquals(LATIN1_STRING, response.getResponseAsString());
  }

  public void testEncodingDetectionUtf8WithBomNoContentHeader() throws Exception {
    HttpResponse response = new HttpResponseBuilder()
        .setResponse(UTF8_DATA)
        .create();
    assertEquals("UTF-8", response.getEncoding().toUpperCase());
    assertEquals(UTF8_STRING, response.getResponseAsString());
  }

  public void testEncodingDetectionLatin1NoContentHeader() throws Exception {
     HttpResponse response = new HttpResponseBuilder()
        .setResponse(LATIN1_DATA)
        .create();
    assertEquals("ISO-8859-1", response.getEncoding().toUpperCase());
    assertEquals(LATIN1_STRING, response.getResponseAsString());
  }

  public void testGetEncodingForImageContentType() throws Exception {
    HttpResponse response = new HttpResponseBuilder()
        .setResponse(LATIN1_DATA)
        .addHeader("Content-Type", "image/png; charset=iso-8859-1")
        .create();
    assertEquals(HttpResponse.DEFAULT_ENCODING, response.getEncoding().toUpperCase());
  }

  public void testGetEncodingForFlashContentType() throws Exception {
    HttpResponse response = new HttpResponseBuilder()
        .setResponse(LATIN1_DATA)
        .addHeader("Content-Type", "application/x-shockwave-flash; charset=iso-8859-1")
        .create();
    assertEquals(HttpResponse.DEFAULT_ENCODING, response.getEncoding().toUpperCase());
  }

  public void testPreserveBinaryData() throws Exception {
    byte[] data = new byte[] {
        (byte)0x00, (byte)0xDE, (byte)0xEA, (byte)0xDB, (byte)0xEE, (byte)0xF0
    };
    HttpResponse response = new HttpResponseBuilder()
        .addHeader("Content-Type", "application/octet-stream")
        .setResponse(data)
        .create();

    byte[] out = IOUtils.toByteArray(response.getResponse());
    assertEquals(data.length, response.getContentLength());
    assertTrue(Arrays.equals(data, out));

    out = IOUtils.toByteArray(response.getResponse());
    assertTrue(Arrays.equals(data, out));
  }

  public void testStrictCacheControlNoCache() throws Exception {
    HttpResponse response = new HttpResponseBuilder()
        .addHeader("Cache-Control", "no-cache")
        .create();
    assertTrue(response.isStrictNoCache());
    assertEquals(-1, response.getCacheExpiration());
    assertEquals(-1, response.getCacheTtl());
  }

  public void testStrictPragmaNoCache() throws Exception {
    HttpResponse response = new HttpResponseBuilder()
        .addHeader("Pragma", "no-cache")
        .create();
    assertTrue(response.isStrictNoCache());
    assertEquals(-1, response.getCacheExpiration());
    assertEquals(-1, response.getCacheTtl());
  }

  public void testStrictPragmaJunk() throws Exception {
    HttpResponse response = new HttpResponseBuilder()
        .addHeader("Pragma", "junk")
        .create();
    assertFalse(response.isStrictNoCache());
    int expected = roundToSeconds(System.currentTimeMillis() + HttpResponse.DEFAULT_TTL);
    int expires = roundToSeconds(response.getCacheExpiration());
    assertEquals(expected, expires);
    assertTrue(response.getCacheTtl() <= HttpResponse.DEFAULT_TTL && response.getCacheTtl() > 0);
  }

  /**
   * Verifies that the cache TTL is within acceptable ranges.
   * This always rounds down due to timing, so actual verification will be against maxAge - 1.
   */
  private static void assertTtlOk(int maxAge, HttpResponse response) {
    assertEquals(maxAge - 1, roundToSeconds(response.getCacheTtl() - 1));
  }

  public void testExpires() throws Exception {
    int maxAge = 10;
    int time = roundToSeconds(System.currentTimeMillis()) + maxAge;
    HttpResponse response = new HttpResponseBuilder()
        .addHeader("Expires", DateUtil.formatDate(1000L * time))
        .create();
    assertEquals(time, roundToSeconds(response.getCacheExpiration()));
    // Second rounding makes this n-1.
    assertTtlOk(maxAge, response);
  }

  public void testMaxAgeNoDate() throws Exception {
    int maxAge = 10;
    // Guess time.
    int expected = roundToSeconds(System.currentTimeMillis()) + maxAge;
    HttpResponse response = new HttpResponseBuilder()
        .addHeader("Cache-Control", "public, max-age=" + maxAge)
        .create();
    int expiration = roundToSeconds(response.getCacheExpiration());

    assertEquals(expected, expiration);
    assertTtlOk(maxAge, response);
  }

  public void testMaxAgeInvalidDate() throws Exception {
    int maxAge = 10;
    // Guess time.
    int expected = roundToSeconds(System.currentTimeMillis()) + maxAge;

    HttpResponse response = new HttpResponseBuilder()
        .addHeader("Date", "Wed, 09 Jul 2008 19:18:33 EDT")
        .addHeader("Cache-Control", "public, max-age=" + maxAge)
        .create();
    int expiration = roundToSeconds(response.getCacheExpiration());

    assertEquals(expected, expiration);
    assertTtlOk(maxAge, response);
  }

  public void testMaxAgeWithDate() throws Exception {
    int maxAge = 10;
    int now = roundToSeconds(System.currentTimeMillis());
    HttpResponse response = new HttpResponseBuilder()
        .addHeader("Date", DateUtil.formatDate(1000L * now))
        .addHeader("Cache-Control", "public, max-age=" + maxAge)
        .create();

    assertEquals(now + maxAge, roundToSeconds(response.getCacheExpiration()));
    assertTtlOk(maxAge, response);
  }

  public void testFixedDate() throws Exception {
    int time = roundToSeconds(System.currentTimeMillis());
    HttpResponse response = new HttpResponseBuilder()
        .addHeader("Date", DateUtil.formatDate(1000L * time))
        .create();
    assertEquals(time + roundToSeconds(HttpResponse.DEFAULT_TTL),
        roundToSeconds(response.getCacheExpiration()));
    assertTtlOk(roundToSeconds(HttpResponse.DEFAULT_TTL), response);
  }

  public void testNegativeCaching() {
    assertTrue("Bad HTTP responses must be cacheable!",
        HttpResponse.error().getCacheExpiration() > System.currentTimeMillis());
    assertTrue("Bad HTTP responses must be cacheable!",
        HttpResponse.notFound().getCacheExpiration() > System.currentTimeMillis());
    assertTrue("Bad HTTP responses must be cacheable!",
        HttpResponse.timeout().getCacheExpiration() > System.currentTimeMillis());
    long ttl = HttpResponse.error().getCacheTtl();
    assertTrue(ttl <= HttpResponse.DEFAULT_TTL && ttl > 0);
  }

  private static void assertDoesNotAllowNegativeCaching(int status)  {
    HttpResponse response = new HttpResponseBuilder()
        .setHttpStatusCode(status)
        .setResponse(UTF8_DATA)
        .setStrictNoCache()
        .create();
    assertEquals(-1, response.getCacheTtl());
  }

  private static void assertAllowsNegativeCaching(int status) {
    HttpResponse response = new HttpResponseBuilder()
        .setHttpStatusCode(status)
        .setResponse(UTF8_DATA)
        .setStrictNoCache()
        .create();
    long ttl = response.getCacheTtl();
    assertTrue(ttl <= HttpResponse.DEFAULT_TTL && ttl > 0);
  }

  public void testStrictNoCacheAndNegativeCaching() {
    assertDoesNotAllowNegativeCaching(HttpResponse.SC_UNAUTHORIZED);
    assertDoesNotAllowNegativeCaching(HttpResponse.SC_FORBIDDEN);
    assertDoesNotAllowNegativeCaching(HttpResponse.SC_OK);
    assertAllowsNegativeCaching(HttpResponse.SC_NOT_FOUND);
    assertAllowsNegativeCaching(HttpResponse.SC_INTERNAL_SERVER_ERROR);
    assertAllowsNegativeCaching(HttpResponse.SC_GATEWAY_TIMEOUT);
  }

  public void testSetNoCache() {
    int time = roundToSeconds(System.currentTimeMillis());
    HttpResponse response = new HttpResponseBuilder()
        .addHeader("Expires", DateUtil.formatDate(1000L * time))
        .setStrictNoCache()
        .create();
    assertNull(response.getHeader("Expires"));
    assertEquals("no-cache", response.getHeader("Pragma"));
    assertEquals("no-cache", response.getHeader("Cache-Control"));
  }

  public void testNullHeaderNamesStripped() {
    HttpResponse response = new HttpResponseBuilder()
        .addHeader(null, "dummy")
        .create();
    for (String key : response.getHeaders().keySet()) {
      assertNotNull("Null header not removed.", key);
    }
  }
}
