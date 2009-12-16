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

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;

public class HttpResponseTest extends Assert {
  private static final byte[] UTF8_DATA = {
    (byte)0xEF, (byte)0xBB, (byte)0xBF, 'h', 'e', 'l', 'l', 'o'
  };
  private static final String UTF8_STRING = "hello";

  // A large string is needed for accurate charset detection.
  private static final byte[] LATIN1_DATA = {
    'G', 'a', 'm', 'e', 's', ',', ' ', 'H', 'Q', ',', ' ', 'M', 'a', 'n', 'g', (byte)0xE1, ',', ' ',
    'A', 'n', 'i', 'm', 'e', ' ', 'e', ' ', 't', 'u', 'd', 'o', ' ', 'q', 'u', 'e', ' ', 'u', 'm',
    ' ', 'b', 'o', 'm', ' ', 'n', 'e', 'r', 'd', ' ', 'a', 'm', 'a'
  };
  private static final String LATIN1_STRING
      = "Games, HQ, Mang\u00E1, Anime e tudo que um bom nerd ama";

  private static final byte[] BIG5_DATA = {
    (byte)0xa7, (byte)0x41, (byte)0xa6, (byte)0x6e
  };

  private static final String BIG5_STRING = "\u4F60\u597D";


  private static int roundToSeconds(long ts) {
    return (int)(ts / 1000);
  }

  @Test
  public void testEncodingDetectionUtf8WithBom() throws Exception {
     HttpResponse response = new HttpResponseBuilder()
         .addHeader("Content-Type", "text/plain; charset=UTF-8")
         .setResponse(UTF8_DATA)
         .create();
    assertEquals(UTF8_STRING, response.getResponseAsString());
    assertEquals("UTF-8", response.getEncoding());
  }

  @Test
  public void testEncodingDetectionUtf8WithBomCaseInsensitiveKey() throws Exception {
    HttpResponse response = new HttpResponseBuilder()
        .addHeader("Content-Type", "text/plain; Charset=utf-8")
        // Legitimate data, should be ignored in favor of explicit charset.
        .setResponse(LATIN1_DATA)
        .create();
    assertEquals("UTF-8", response.getEncoding());
  }

  @Test
  public void testEncodingDetectionLatin1() throws Exception {
    // Input is a basic latin-1 string with 1 non-UTF8 compatible char.
    HttpResponse response = new HttpResponseBuilder()
        .addHeader("Content-Type", "text/plain; charset=iso-8859-1")
        .setResponse(LATIN1_DATA)
        .create();
    assertEquals(LATIN1_STRING, response.getResponseAsString());
  }

  @Test
  public void testEncodingDetectionLatin1withIncorrectCharset() throws Exception {
    // Input is a basic latin-1 string with 1 non-UTF8 compatible char.
    HttpResponse response = new HttpResponseBuilder()
        .addHeader("Content-Type", "text/plain; charset=iso-88859-1")
        .setResponse(LATIN1_DATA)
        .create();
    assertEquals(LATIN1_STRING, response.getResponseAsString());
    assertEquals("ISO-8859-1", response.getEncoding());
  }

  @Test
  public void testEncodingDetectionUtf8WithBomAndIncorrectCharset() throws Exception {
     HttpResponse response = new HttpResponseBuilder()
         .addHeader("Content-Type", "text/plain; charset=UTTFF-88")
         .setResponse(UTF8_DATA)
         .create();
    assertEquals(UTF8_STRING, response.getResponseAsString());
    assertEquals("UTF-8", response.getEncoding());
  }

  @Test
  public void testEncodingDetectionUtf8WithBomAndInvalidCharset() throws Exception {
     HttpResponse response = new HttpResponseBuilder()
         // Use a charset that will generate an IllegalCharsetNameException
         .addHeader("Content-Type", "text/plain; charset=.UTF-8")
         .setResponse(UTF8_DATA)
         .create();
    assertEquals(UTF8_STRING, response.getResponseAsString());
    assertEquals("UTF-8", response.getEncoding());
  }

  @Test
  public void testEncodingDetectionBig5() throws Exception {
    HttpResponse response = new HttpResponseBuilder()
        .addHeader("Content-Type", "text/plain; charset=BIG5")
        .setResponse(BIG5_DATA)
        .create();
    assertEquals(BIG5_STRING, response.getResponseAsString());
  }

  @Test
  public void testEncodingDetectionBig5WithQuotes() throws Exception {
    HttpResponse response = new HttpResponseBuilder()
        .addHeader("Content-Type", "text/plain; charset=\"BIG5\"")
        .setResponse(BIG5_DATA)
        .create();
    assertEquals(BIG5_STRING, response.getResponseAsString());
  }

  @Test
  public void testEncodingDetectionUtf8WithBomNoCharsetSpecified() throws Exception {
    HttpResponse response = new HttpResponseBuilder()
        .addHeader("Content-Type", "text/plain")
        .setResponse(UTF8_DATA)
        .create();
    assertEquals("UTF-8", response.getEncoding().toUpperCase());
    assertEquals(UTF8_STRING, response.getResponseAsString());
  }

  @Test
  public void testEncodingDetectionLatin1NoCharsetSpecified() throws Exception {
    HttpResponse response = new HttpResponseBuilder()
        .addHeader("Content-Type", "text/plain;")
        .setResponse(LATIN1_DATA)
        .create();
    assertEquals("ISO-8859-1", response.getEncoding().toUpperCase());
    assertEquals(LATIN1_STRING, response.getResponseAsString());
  }

  @Test
  public void testEncodingDetectionUtf8WithBomNoContentHeader() throws Exception {
    HttpResponse response = new HttpResponseBuilder()
        .setResponse(UTF8_DATA)
        .create();
    assertEquals("UTF-8", response.getEncoding().toUpperCase());
    assertEquals(UTF8_STRING, response.getResponseAsString());
  }

  @Test
  public void testEncodingDetectionLatin1NoContentHeader() throws Exception {
     HttpResponse response = new HttpResponseBuilder()
        .setResponse(LATIN1_DATA)
        .create();
    assertEquals(HttpResponse.DEFAULT_ENCODING.name(), response.getEncoding());
  }

  @Test
  public void testGetEncodingForImageContentType() throws Exception {
    HttpResponse response = new HttpResponseBuilder()
        .setResponse(LATIN1_DATA)
        .addHeader("Content-Type", "image/png; charset=iso-8859-1")
        .create();
    assertEquals(HttpResponse.DEFAULT_ENCODING.name(), response.getEncoding().toUpperCase());
  }

  @Test
  public void testGetEncodingForFlashContentType() throws Exception {
    HttpResponse response = new HttpResponseBuilder()
        .setResponse(LATIN1_DATA)
        .addHeader("Content-Type", "application/x-shockwave-flash; charset=iso-8859-1")
        .create();
    assertEquals(HttpResponse.DEFAULT_ENCODING.name(), response.getEncoding().toUpperCase());
  }

  @Test
  public void testPreserveBinaryData() throws Exception {
    byte[] data = {
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

  @Test
  public void testStrictCacheControlNoCache() throws Exception {
    HttpResponse response = new HttpResponseBuilder()
        .addHeader("Cache-Control", "no-cache")
        .create();
    assertTrue(response.isStrictNoCache());
    assertEquals(-1, response.getCacheExpiration());
    assertEquals(-1, response.getCacheTtl());
  }

  @Test
  public void testStrictPragmaNoCache() throws Exception {
    HttpResponse response = new HttpResponseBuilder()
        .addHeader("Pragma", "no-cache")
        .create();
    assertTrue(response.isStrictNoCache());
    assertEquals(-1, response.getCacheExpiration());
    assertEquals(-1, response.getCacheTtl());
  }

  @Test
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
  
  @Test
  public void testCachingHeadersIgnoredOnError() throws Exception {
    HttpResponse response = new HttpResponseBuilder()
        .addHeader("Cache-Control", "no-cache")
        .setHttpStatusCode(404)
        .create();
    assertFalse(response.isStrictNoCache());

    response = new HttpResponseBuilder()
        .addHeader("Cache-Control", "no-cache")
        .setHttpStatusCode(403)
        .create();
    assertTrue(response.isStrictNoCache());

    response = new HttpResponseBuilder()
        .addHeader("Cache-Control", "no-cache")
        .setHttpStatusCode(401)
        .create();
    assertTrue(response.isStrictNoCache());
  }

  /**
   * Verifies that the cache TTL is within acceptable ranges.
   * This always rounds down due to timing, so actual verification will be against maxAge - 1.
   */
  private static void assertTtlOk(int maxAge, HttpResponse response) {
    assertEquals(maxAge - 1, roundToSeconds(response.getCacheTtl() - 1));
  }

  @Test
  public void testExpires() throws Exception {
    int maxAge = 10;
    int time = roundToSeconds(System.currentTimeMillis()) + maxAge;
    HttpResponse response = new HttpResponseBuilder()
        .addHeader("Expires", DateUtil.formatRfc1123Date(1000L * time))
        .create();
    assertEquals(time, roundToSeconds(response.getCacheExpiration()));
    // Second rounding makes this n-1.
    assertTtlOk(maxAge, response);
  }

  @Test
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

  @Test
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

  @Test
  public void testMaxAgeWithDate() throws Exception {
    int maxAge = 10;
    int now = roundToSeconds(System.currentTimeMillis());
    HttpResponse response = new HttpResponseBuilder()
        .addHeader("Date", DateUtil.formatRfc1123Date(1000L * now))
        .addHeader("Cache-Control", "public, max-age=" + maxAge)
        .create();

    assertEquals(now + maxAge, roundToSeconds(response.getCacheExpiration()));
    assertTtlOk(maxAge, response);
  }

  @Test
  public void testFixedDate() throws Exception {
    int time = roundToSeconds(System.currentTimeMillis());
    HttpResponse response = new HttpResponseBuilder()
        .addHeader("Date", DateUtil.formatRfc1123Date(1000L * time))
        .create();
    assertEquals(time + roundToSeconds(HttpResponse.DEFAULT_TTL),
        roundToSeconds(response.getCacheExpiration()));
    assertTtlOk(roundToSeconds(HttpResponse.DEFAULT_TTL), response);
  }

  @Test
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

  @Test
  public void testStrictNoCacheAndNegativeCaching() {
    assertDoesNotAllowNegativeCaching(HttpResponse.SC_UNAUTHORIZED);
    assertDoesNotAllowNegativeCaching(HttpResponse.SC_FORBIDDEN);
    assertDoesNotAllowNegativeCaching(HttpResponse.SC_OK);
    assertAllowsNegativeCaching(HttpResponse.SC_NOT_FOUND);
    assertAllowsNegativeCaching(HttpResponse.SC_INTERNAL_SERVER_ERROR);
    assertAllowsNegativeCaching(HttpResponse.SC_GATEWAY_TIMEOUT);
  }

  @Test
  public void testRetryAfter() {
    HttpResponse response;
    String nowPlus60 = DateUtil.formatRfc1123Date(System.currentTimeMillis() + 60 * 1000L);
    for (String date : Arrays.asList("60", nowPlus60)) {
      for (int rc : Arrays.asList(HttpResponse.SC_INTERNAL_SERVER_ERROR, HttpResponse.SC_GATEWAY_TIMEOUT, HttpResponse.SC_BAD_REQUEST)) {
        response = new HttpResponseBuilder()
            .setHttpStatusCode(rc)
            .setHeader("Retry-After","60")
            .create();
        long ttl = response.getCacheTtl();
        assertTrue(ttl <= 60 * 1000L && ttl > 0);
      }
    }
  }

  @Test
  public void testSetNoCache() {
    int time = roundToSeconds(System.currentTimeMillis());
    HttpResponse response = new HttpResponseBuilder()
        .addHeader("Expires", DateUtil.formatRfc1123Date(1000L * time))
        .setStrictNoCache()
        .create();
    assertNull(response.getHeader("Expires"));
    assertEquals("no-cache", response.getHeader("Pragma"));
    assertEquals("no-cache", response.getHeader("Cache-Control"));
  }

  @Test
  public void testNullHeaderNamesStripped() {
    HttpResponse response = new HttpResponseBuilder()
        .addHeader(null, "dummy")
        .create();
    for (String key : response.getHeaders().keySet()) {
      assertNotNull("Null header not removed.", key);
    }
  }

  @Test
  public void testIsError() {
    // These aren't all valid status codes, but they're reserved in these blocks. Changes
    // would be required to the HTTP standard anyway before this test would be invalid.
    for (int i = 100; i < 400; i += 100) {
      for (int j = 0; j < 10; ++j) {
        HttpResponse response = new HttpResponseBuilder().setHttpStatusCode(i).create();
        assertFalse("Status below 400 considered to be an error", response.isError());
      }
    }

    for (int i = 400; i < 600; i += 100) {
      for (int j = 0; j < 10; ++j) {
        HttpResponse response = new HttpResponseBuilder().setHttpStatusCode(i).create();
        assertTrue("Status above 400 considered to be an error", response.isError());
      }
    }
  }

  @Test
  public void testSerialization() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream out = new ObjectOutputStream(baos);

    HttpResponse response = new HttpResponseBuilder()
        .addHeader("Foo", "bar")
        .addHeader("Foo", "baz")
        .addHeader("Blah", "blah")
        .setHttpStatusCode(204)
        .setResponseString("This is the response string")
        .create();

    out.writeObject(response);

    ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
    ObjectInputStream in = new ObjectInputStream(bais);

    HttpResponse deserialized = (HttpResponse)in.readObject();

    assertEquals(response, deserialized);
  }

  @Test
  public void testSerializationWithTransientFields() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream out = new ObjectOutputStream(baos);

    long now = System.currentTimeMillis();

    HttpResponse response = new HttpResponseBuilder()
        .addHeader("Foo", "bar")
        .addHeader("Foo", "baz")
        .addHeader("Blah", "blah")
        .addHeader("Date", DateUtil.formatRfc1123Date(now))
        .setHttpStatusCode(204)
        .setResponseString("This is the response string")
        .setMetadata("foo", "bar")
        .create();

    out.writeObject(response);

    ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
    ObjectInputStream in = new ObjectInputStream(bais);

    HttpResponse deserialized = (HttpResponse)in.readObject();

    HttpResponse expectedResponse = new HttpResponseBuilder()
        .addHeader("Foo", "bar")
        .addHeader("Foo", "baz")
        .addHeader("Blah", "blah")
        .addHeader("Date", DateUtil.formatRfc1123Date(now))
        .setHttpStatusCode(204)
        .setResponseString("This is the response string")
        .create();

    assertEquals(expectedResponse, deserialized);
  }
}
