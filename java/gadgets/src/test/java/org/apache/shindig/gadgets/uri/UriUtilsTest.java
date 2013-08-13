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
package org.apache.shindig.gadgets.uri;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 * Tests for UriUtils.
 */
public class UriUtilsTest {
  Enumeration<String> makeEnumeration(String... args) {
    Vector<String> vector = new Vector<String>();
    if (args != null) {
      vector.addAll(Arrays.asList(args));
    }
    return vector.elements();
  }

  private void verifyMime(String requestMime, String responseMime, String expectedMime)
      throws Exception {
    String url = "http://example.org/foo";
    HttpRequest req = new HttpRequest(Uri.parse(url))
        .setRewriteMimeType(requestMime);
    HttpResponseBuilder builder = new HttpResponseBuilder()
        .setHeader("Content-Type", responseMime);

    UriUtils.maybeRewriteContentType(req, builder);
    assertEquals(expectedMime, builder.getHeader("Content-Type"));
  }

  @Test
  public void testMimeMatchPass() throws Exception {
    verifyMime("text/css", "text/css", "text/css");
  }

  @Test
  public void testMimeMatchPassWithAdditionalAttributes() throws Exception {
    verifyMime("text/css", "text/css; charset=UTF-8", "text/css");
  }

  @Test
  public void testNonMatchingMime() throws Exception {
    verifyMime("text/css", "image/png; charset=UTF-8", "text/css");
  }

  @Test
  public void testNonMatchingMimeWithSamePrefix() throws Exception {
    verifyMime("text/html", "text/plain", "text/html");
  }

  @Test
  public void testNonMatchingMimeWithWildCard() throws Exception {
    verifyMime("text/*", "image/png", "text/*");
  }

  @Test
  public void testNonMatchingMimeWithDifferentPrefix() throws Exception {
    verifyMime("text/*", "text123/html", "text/*");
  }

  @Test
  public void testMimeMatchVarySupport() throws Exception {
    verifyMime("image/*", "image/gif", "image/gif");
  }

  @Test
  public void testNullRequestMime() throws Exception {
    verifyMime(null, "image/png; charset=UTF-8", "image/png; charset=UTF-8");
  }

  @Test
  public void testEmptyRequestMime() throws Exception {
    verifyMime("", "image/png; charset=UTF-8", "image/png; charset=UTF-8");
  }

  @Test
  public void testNullResponseMime() throws Exception {
    verifyMime("text/*", null, "text/*");
  }

  @Test
  public void testCopyResponseHeadersAndStatusCode_AddHeader() throws Exception {
    HttpResponse resp = new HttpResponseBuilder()
        .setHttpStatusCode(5000)
        .addHeader("hello", "world1")
        .addHeader("hello", "world2")
        .addHeader("hello\u2297", "bad header")
        .addHeader("Content-length", "10")
        .addHeader("vary", "1")
        .create();

    HttpResponseBuilder builder = new HttpResponseBuilder();

    UriUtils.copyResponseHeadersAndStatusCode(resp, builder, false, false,
        UriUtils.DisallowedHeaders.OUTPUT_TRANSFER_DIRECTIVES,
        UriUtils.DisallowedHeaders.CACHING_DIRECTIVES);

    HttpResponse response = builder.create();

    // Date is added by HttpResponse.
    assertEquals(3, response.getHeaders().size());
    Iterator<String> headers = response.getHeaders("hello").iterator();
    assertEquals("world1", headers.next());
    assertEquals("world2", headers.next());
    assertEquals(5000, response.getHttpStatusCode());
  }

  @Test
  public void testCopyResponseHeadersAndStatusCode_SetHeaders() throws Exception {
    HttpResponse resp = new HttpResponseBuilder()
        .setHttpStatusCode(5000)
        .addHeader("hello", "world1")
        .addHeader("hello", "world2")
        .addHeader("hello\u2297", "bad header")
        .addHeader("Content-length", "10")
        .addHeader("vary", "1")
        .create();

    HttpResponseBuilder builder = new HttpResponseBuilder();

    UriUtils.copyResponseHeadersAndStatusCode(resp, builder, false, true,
        UriUtils.DisallowedHeaders.OUTPUT_TRANSFER_DIRECTIVES,
        UriUtils.DisallowedHeaders.CACHING_DIRECTIVES);

    HttpResponse response = builder.create();
    assertEquals(2, response.getHeaders().size());
    assertEquals("world2", response.getHeader("hello"));
    assertEquals(5000, response.getHttpStatusCode());
  }

  @Test
  public void testCopyResponseHeadersAndStatusCode_RemapTrue() throws Exception {
    HttpResponse resp = new HttpResponseBuilder()
        .setHttpStatusCode(500)
        .addHeader("hello", "world1")
        .addHeader("hello", "world2")
        .addHeader("hello\u2297", "bad header")
        .addHeader("Content-length", "10")
        .addHeader("vary", "1")
        .create();

    HttpResponseBuilder builder = new HttpResponseBuilder();

    UriUtils.copyResponseHeadersAndStatusCode(resp, builder, true, true,
        UriUtils.DisallowedHeaders.OUTPUT_TRANSFER_DIRECTIVES,
        UriUtils.DisallowedHeaders.CACHING_DIRECTIVES);

    HttpResponse response = builder.create();
    assertEquals(2, response.getHeaders().size());
    assertEquals("world2", response.getHeader("hello"));
    assertEquals(502, response.getHttpStatusCode());
  }

  @Test
  public void testCopyResponseHeadersAndStatusCode_RemapFalse() throws Exception {
    HttpResponse resp = new HttpResponseBuilder()
        .setHttpStatusCode(500)
        .addHeader("hello", "world1")
        .addHeader("hello", "world2")
        .addHeader("hello\u2297", "bad header")
        .addHeader("Content-length", "10")
        .addHeader("vary", "1")
        .create();

    HttpResponseBuilder builder = new HttpResponseBuilder();

    UriUtils.copyResponseHeadersAndStatusCode(resp, builder, false, true,
        UriUtils.DisallowedHeaders.OUTPUT_TRANSFER_DIRECTIVES,
        UriUtils.DisallowedHeaders.CACHING_DIRECTIVES);

    HttpResponse response = builder.create();
    assertEquals(2, response.getHeaders().size());
    assertEquals("world2", response.getHeader("hello"));
    assertEquals(500, response.getHttpStatusCode());
  }

  @Test
  public void testCopyRequestHeaders() throws Exception {
    HttpRequest origRequest = new HttpRequest(Uri.parse("http://www.example.org/data.html"));

    Map<String, List<String>> addedHeaders =
        ImmutableMap.<String, List<String>>builder()
          .put("h1", ImmutableList.of("v1", "v2"))
          .put("h2", ImmutableList.of("v3", "v4"))
          .put("hello\u2297", ImmutableList.of("v5", "v6"))
          .put("unchanged_header", ImmutableList.<String>of())
          .put("Content-Length", ImmutableList.of("50", "100"))
          .build();

    origRequest.addAllHeaders(addedHeaders);

    HttpRequest req = new HttpRequest(Uri.parse(
        "http://www.example.org/data.html"));
    req.removeHeader(HttpRequest.DOS_PREVENTION_HEADER);
    req.addHeader("h1", "hello");
    req.addHeader("Content-Length", "10");
    req.addHeader("unchanged_header", "original_value");

    UriUtils.copyRequestHeaders(origRequest, req,
        UriUtils.DisallowedHeaders.POST_INCOMPATIBLE_DIRECTIVES);

    Map<String, List<String>> headers =
        ImmutableMap.<String, List<String>>builder()
        .put("h1", ImmutableList.of("v1", "v2"))
        .put("h2", ImmutableList.of("v3", "v4"))
        .put("unchanged_header", ImmutableList.of("original_value"))
        .put("Content-Length", ImmutableList.of("10"))
        .put(HttpRequest.DOS_PREVENTION_HEADER, ImmutableList.of("on"))
        .build();

    assertEquals(headers, req.getHeaders());
  }

  @Test
  public void testCopyRequestData() throws Exception {
    HttpRequest origRequest = new HttpRequest(Uri.parse("http://www.example.com"));
    origRequest.setMethod("Post");

    String data = "hello world";
    origRequest.setPostBody(data.getBytes());

    HttpRequest req = new HttpRequest(Uri.parse(
        "http://www.example.org/data.html"));

    UriUtils.copyRequestData(origRequest, req);

    assertEquals(data, req.getPostBodyAsString());
  }

  @Test
  public void testGetContentTypeWithoutCharset() {
    assertEquals("text/html",
                 UriUtils.getContentTypeWithoutCharset("text/html"));
    assertEquals("text/html;",
                 UriUtils.getContentTypeWithoutCharset("text/html;"));
    assertEquals("text/html",
                 UriUtils.getContentTypeWithoutCharset("text/html; charset=hello"));
    assertEquals("text/html; hello=world",
                 UriUtils.getContentTypeWithoutCharset("text/html; charset=hello; hello=world"));
    assertEquals("text/html; pharset=hello; hello=world",
                 UriUtils.getContentTypeWithoutCharset("text/html; pharset=hello; hello=world"));
    assertEquals("text/html; charsett=utf; hello=world",
                 UriUtils.getContentTypeWithoutCharset("text/html; charsett=utf; ; hello=world"));
  }
}
