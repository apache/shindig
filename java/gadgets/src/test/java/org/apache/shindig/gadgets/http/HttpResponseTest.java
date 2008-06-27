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

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class HttpResponseTest extends TestCase {
  private final static byte[] UTF8_DATA = new byte[] {
    (byte)0xEF, (byte)0xBB, (byte)0xBF, 'h', 'e', 'l', 'l', 'o'
  };
  private final static String UTF8_STRING = "hello";

  // A large string is needed for accurate charset detection.
  private final static byte[] LATIN1_DATA = new byte[] {
    'G', 'a', 'm', 'e', 's', ',', ' ', 'H', 'Q', ',', ' ', 'M', 'a', 'n', 'g', (byte)0xE1, ',', ' ',
    'A', 'n', 'i', 'm', 'e', ' ', 'e', ' ', 't', 'u', 'd', 'o', ' ', 'q', 'u', 'e', ' ', 'u', 'm',
    ' ', 'b', 'o', 'm', ' ', 'n', 'e', 'r', 'd', ' ', 'a', 'm', 'a'
  };
  private final static String LATIN1_STRING
      = "Games, HQ, Mang\u00E1, Anime e tudo que um bom nerd ama";

  private final static byte[] BIG5_DATA = new byte[] {
    (byte)0xa7, (byte)0x41, (byte)0xa6, (byte)0x6e
  };
  private final static String BIG5_STRING = "\u4F60\u597D";

  private Map<String, List<String>> headers;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    headers = new HashMap<String, List<String>>();
  }

  private void addHeader(String name, String value) {
    java.util.List<String> existing = headers.get(name);
    if (existing == null) {
      existing = new LinkedList<String>();
      headers.put(name, existing);
    }
    existing.add(value);
  }

  private int roundToSeconds(long ts) {
    return (int)(ts / 1000);
  }

  public void testGetEncoding() throws Exception {
    addHeader("Content-Type", "text/plain; charset=TEST-CHARACTER-SET");
    HttpResponse response = new HttpResponse(200, null, headers);
    assertEquals("TEST-CHARACTER-SET", response.getEncoding());
  }

  public void testEncodingDetectionUtf8WithBom() throws Exception {
    // Input is UTF-8 with BOM.
    addHeader("Content-Type", "text/plain; charset=UTF-8");
    HttpResponse response = new HttpResponse(200, UTF8_DATA, headers);
    assertEquals(UTF8_STRING, response.getResponseAsString());
  }

  public void testEncodingDetectionLatin1() throws Exception {
    // Input is a basic latin-1 string with 1 non-UTF8 compatible char.
    addHeader("Content-Type", "text/plain; charset=iso-8859-1");
    HttpResponse response = new HttpResponse(200, LATIN1_DATA, headers);
    assertEquals(LATIN1_STRING, response.getResponseAsString());
  }

  public void testEncodingDetectionBig5() throws Exception {
    addHeader("Content-Type", "text/plain; charset=BIG5");
    HttpResponse response = new HttpResponse(200, BIG5_DATA, headers);
    assertEquals(BIG5_STRING, response.getResponseAsString());
  }

  public void testEncodingDetectionUtf8WithBomNoCharsetSpecified() throws Exception {
    addHeader("Content-Type", "text/plain");
    HttpResponse response = new HttpResponse(200, UTF8_DATA, headers);
    assertEquals("UTF-8", response.getEncoding().toUpperCase());
    assertEquals(UTF8_STRING, response.getResponseAsString());
  }

  public void testEncodingDetectionLatin1NoCharsetSpecified() throws Exception {
    addHeader("Content-Type", "text/plain;");
    HttpResponse response = new HttpResponse(200, LATIN1_DATA, headers);
    assertEquals("ISO-8859-1", response.getEncoding().toUpperCase());
    assertEquals(LATIN1_STRING, response.getResponseAsString());
  }

  public void testEncodingDetectionUtf8WithBomNoContentHeader() throws Exception {
    HttpResponse response = new HttpResponse(200, UTF8_DATA, headers);
    assertEquals("UTF-8", response.getEncoding().toUpperCase());
    assertEquals(UTF8_STRING, response.getResponseAsString());
  }

  public void testEncodingDetectionLatin1NoContentHeader() throws Exception {
    HttpResponse response = new HttpResponse(200, LATIN1_DATA, headers);
    assertEquals("ISO-8859-1", response.getEncoding().toUpperCase());
    assertEquals(LATIN1_STRING, response.getResponseAsString());
  }

  public void testGetEncodingForImageContentType() throws Exception {
    addHeader("Content-Type", "image/png; charset=iso-8859-1");
    HttpResponse response = new HttpResponse(200, LATIN1_DATA, headers);
    assertEquals("UTF-8", response.getEncoding().toUpperCase());
  }

  public void testGetEncodingForFlashContentType() throws Exception {
    addHeader("Content-Type", "application/x-shockwave-flash; charset=iso-8859-1");
    HttpResponse response = new HttpResponse(200, LATIN1_DATA, headers);
    assertEquals("UTF-8", response.getEncoding().toUpperCase());
  }

  public void testPreserveBinaryData() throws Exception {
    byte[] data = new byte[] {
        (byte)0x00, (byte)0xDE, (byte)0xEA, (byte)0xDB, (byte)0xEE, (byte)0xF0
    };
    addHeader("Content-Type", "application/octet-stream");
    HttpResponse response = new HttpResponse(200, data, headers);

    byte[] out = IOUtils.toByteArray(response.getResponse());
    assertEquals(data.length, response.getContentLength());
    assertTrue(Arrays.equals(data, out));

    out = response.getResponseAsBytes();
    assertTrue(Arrays.equals(data, out));
  }

  public void testStrictCacheControlNoCache() throws Exception {
    addHeader("Cache-Control", "no-cache");
    HttpResponse response = new HttpResponse(200, null, headers);
    assertTrue(response.isStrictNoCache());
    assertEquals(-1, response.getCacheExpiration());
    assertEquals(-1, response.getCacheTtl());
  }

  public void testStrictPragmaNoCache() throws Exception {
    addHeader("Pragma", "no-cache");
    HttpResponse response = new HttpResponse(200, null, headers);
    assertTrue(response.isStrictNoCache());
    assertEquals(-1, response.getCacheExpiration());
    assertEquals(-1, response.getCacheTtl());
  }

  public void testStrictPragmaJunk() throws Exception {
    addHeader("Pragma", "junk");
    HttpResponse response = new HttpResponse(200, null, headers);
    assertFalse(response.isStrictNoCache());
    int expected = roundToSeconds(System.currentTimeMillis() + HttpResponse.DEFAULT_TTL);
    int expires = roundToSeconds(response.getCacheExpiration());
    assertEquals(expected, expires);
    assertTrue(response.getCacheTtl() <= HttpResponse.DEFAULT_TTL && response.getCacheTtl() > 0);
  }

  public void testExpires() throws Exception {
    int ttl = 10;
    int time = roundToSeconds(System.currentTimeMillis()) + ttl;
    addHeader("Expires", DateUtil.formatDate(1000L * time));
    HttpResponse response = new HttpResponse(200, null, headers);
    assertEquals(time, roundToSeconds(response.getCacheExpiration()));
    // 9 because of rounding.
    assertEquals(9, roundToSeconds(response.getCacheTtl()));
  }

  public void testMaxAge() throws Exception {
    int maxAge = 10;
    int expected = roundToSeconds(System.currentTimeMillis()) + maxAge;
    addHeader("Cache-Control", "public, max-age=" + maxAge);
    HttpResponse response = new HttpResponse(200, null, headers);
    int expiration = roundToSeconds(response.getCacheExpiration());
    assertEquals(expected, expiration);
    assertEquals(maxAge * 1000, response.getCacheTtl());
  }

  public void testFixedDate() throws Exception {
    int time = roundToSeconds(System.currentTimeMillis());
    addHeader("Date", DateUtil.formatDate(1000L * time));
    HttpResponse response = new HttpResponse(200, null, headers);
    assertEquals(time, roundToSeconds(response.getDate()));
    assertEquals(time + roundToSeconds(HttpResponse.DEFAULT_TTL),
        roundToSeconds(response.getCacheExpiration()));
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

  public void testNullHeaderNamesStripped() {
    addHeader(null, "dummy");
    HttpResponse response = new HttpResponse(200, null, headers);
    for (String key : response.getAllHeaders().keySet()) {
      assertNotNull("Null header not removed.", key);
    }
  }
}
