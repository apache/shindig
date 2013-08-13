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
package org.apache.shindig.gadgets.http;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Tests for HttpResponseBuilder.
 *
 * This test case compliments HttpResponseTest; not all tests are duplicated here.
 */
public class HttpResponseBuilderTest {

  @Test
  public void copyConstructor() {
    HttpResponseBuilder builder = new HttpResponseBuilder()
        .setHttpStatusCode(HttpResponse.SC_NOT_FOUND)
        .setMetadata("foo", "bar")
        .addHeader("Foo-bar", "baz");

    HttpResponseBuilder builder2 = new HttpResponseBuilder(builder);
    assertEquals(builder.create(), builder2.create());
  }

  @Test
  public void addHeader() {
    HttpResponseBuilder builder = new HttpResponseBuilder()
        .addHeader("Foo-bar", "baz");

    assertEquals("baz", builder.getHeaders().get("Foo-bar").iterator().next());
  }

  @Test
  public void addHeadersMap() {
    Map<String, String> headers = ImmutableMap.of("foo", "bar", "blah", "blah");

    HttpResponseBuilder builder = new HttpResponseBuilder()
        .addHeaders(headers);

    assertEquals(Arrays.asList("bar"), Lists.newArrayList(builder.getHeaders().get("foo")));
    assertEquals(Arrays.asList("blah"), Lists.newArrayList(builder.getHeaders().get("blah")));
  }

  @Test
  public void addAllHeaders() {
    Map<String, List<String>> headers = Maps.newHashMap();

    List<String> foo = Lists.newArrayList("bar", "blah");
    List<String> bar = Lists.newArrayList("baz");
    headers.put("foo", foo);
    headers.put("bar", bar);


    HttpResponseBuilder builder = new HttpResponseBuilder()
        .addAllHeaders(headers);

    assertTrue(builder.getHeaders().get("foo").containsAll(foo));
    assertTrue(builder.getHeaders().get("bar").containsAll(bar));
  }

  @Test
  public void setExpirationTime() {
    HttpResponseBuilder builder = new HttpResponseBuilder()
        .addHeader("Pragma", "no-cache")
        .addHeader("Cache-Control", "public,max-age=100")
        .setExpirationTime(100);

    Multimap<String, String> headers = builder.getHeaders();
    assertTrue("No Expires header added.", headers.containsKey("Expires"));
    assertFalse("Pragma header not removed", headers.containsKey("Pragma"));
    assertFalse("Cache-Control header not removed", headers.containsKey("Cache-Control"));
  }

  @Test
  public void setCacheControlMaxAge() {
    HttpResponseBuilder builder = new HttpResponseBuilder()
        .addHeader("Cache-Control", "public,max-age=100")
        .setCacheControlMaxAge(12345);

    Multimap<String, String> headers = builder.getHeaders();
    assertEquals("public,max-age=12345", headers.get("Cache-Control").iterator().next());
  }

  @Test
  public void setCacheControlMaxAgeWithSpacesInCacheControlHeader() {
    HttpResponseBuilder builder = new HttpResponseBuilder()
        .addHeader("Cache-Control", "public, max-age=123, no-transform ")
        .setCacheControlMaxAge(12345);

    Multimap<String, String> headers = builder.getHeaders();
    assertEquals("public,no-transform,max-age=12345",
                 headers.get("Cache-Control").iterator().next());
  }

  @Test
  public void setCacheControlMaxAgeWithBadMaxAgeFormat() {
    HttpResponseBuilder builder = new HttpResponseBuilder()
        .addHeader("Cache-Control", "public, max-age=12=ab")
        .setCacheControlMaxAge(12345);

    Multimap<String, String> headers = builder.getHeaders();
    assertEquals("public,max-age=12=ab,max-age=12345",
                 headers.get("Cache-Control").iterator().next());
  }

  @Test
  public void setCacheControlMaxAgeWithNoInitialMaxAge() {
    HttpResponseBuilder builder = new HttpResponseBuilder()
        .addHeader("Cache-Control", "private")
        .setCacheControlMaxAge(10000);

    Multimap<String, String> headers = builder.getHeaders();
    assertEquals("private,max-age=10000", headers.get("Cache-Control").iterator().next());
  }

  @Test
  public void setCacheControlMaxAgeWithNoCacheControlHeader() {
    HttpResponseBuilder builder = new HttpResponseBuilder()
        .setCacheControlMaxAge(86400);

    Multimap<String, String> headers = builder.getHeaders();
    assertEquals("max-age=86400", headers.get("Cache-Control").iterator().next());
  }

  @Test
  public void setCacheTtl() {
    HttpResponseBuilder builder = new HttpResponseBuilder()
        .addHeader("Pragma", "no-cache")
        .addHeader("Expires", "some time stamp normally goes here")
        .addHeader("Cache-Control", "no-cache")
        .setCacheTtl(100);

    Multimap<String, String> headers = builder.getHeaders();
    assertFalse("Expires header not removed.", headers.containsKey("Expires"));
    assertFalse("Pragma header not removed", headers.containsKey("Pragma"));
    assertEquals("public,max-age=100", headers.get("Cache-Control").iterator().next());
  }

  @Test
  public void setStrictNoCache() {
    HttpResponseBuilder builder = new HttpResponseBuilder()
        .addHeader("Cache-Control", "public,max-age=100")
        .addHeader("Expires", "some time stamp normally goes here")
        .setStrictNoCache();

    Multimap<String, String> headers = builder.getHeaders();
    assertFalse("Expires header not removed.", headers.containsKey("Expires"));
    assertEquals("no-cache", headers.get("Cache-Control").iterator().next());
    assertEquals("no-cache", headers.get("Pragma").iterator().next());
  }

  @Test
  public void setEncoding() {
    HttpResponseBuilder builder = new HttpResponseBuilder()
        .addHeader("Content-Type", "text/html; charset=Big5")
        .setEncoding(Charsets.UTF_8);

    Multimap<String, String> headers = builder.getHeaders();
    assertEquals("text/html; charset=UTF-8", headers.get("Content-Type").iterator().next());
  }

  @Test
  public void setEncodingEmpty() {
    HttpResponseBuilder builder = new HttpResponseBuilder()
        .addHeader("Content-Type", "text/html")
        .setEncoding(Charsets.UTF_8);

    Multimap<String, String> headers = builder.getHeaders();
    assertEquals("text/html; charset=UTF-8", headers.get("Content-Type").iterator().next());
  }

  @Test
  public void setResponseString() {
    HttpResponse resp = new HttpResponseBuilder()
        .setResponseString("foo")
        .create();
    assertEquals("foo", resp.getResponseAsString());
  }

  @Test
  public void setResponseStringWithContentType() {
    HttpResponse resp = new HttpResponseBuilder()
        .addHeader("Content-Type", "text/html")
        .setResponseString("foo")
        .create();
    Multimap<String, String> headers = resp.getHeaders();
    assertEquals("text/html; charset=UTF-8", headers.get("Content-Type").iterator().next());
    assertEquals("foo", resp.getResponseAsString());
  }

  @Test
  public void setResponse() {
    byte[] someData = "some data".getBytes();
    HttpResponse resp = new HttpResponseBuilder()
        .setResponse(someData)
        .create();

    assertNotSame(someData, resp.getResponseAsBytes());
    assertArrayEquals(someData, resp.getResponseAsBytes());
  }

  @Test
  public void setResponseNoCopy() {
    byte[] someData = "some data".getBytes();
    HttpResponse resp = new HttpResponseBuilder()
        .setResponseNoCopy(someData)
        .create();

    assertSame(someData, resp.getResponseAsBytes());
  }

  @Test
  public void headerOrdering() {
    ImmutableList<String> soupList = ImmutableList.of("Tomato", "Potato", "Lentil", "Onion");
    HttpResponseBuilder b = new HttpResponseBuilder();
    for (String soup : soupList) {
      b.addHeader("Soup", soup);
    }
    HttpResponse resp = b.create();

    // Insure that headers are stored in the order they are added
    assertEquals(Joiner.on(",").join(resp.getHeaders("Soup")), Joiner.on(",").join(soupList));
  }

  @Test
  public void noModsReturnsSameResponse() {
    HttpResponseBuilder builder = new HttpResponseBuilder();
    builder.setHttpStatusCode(HttpResponse.SC_BAD_GATEWAY);
    builder.setResponseString("foo");
    HttpResponse response = builder.create();
    assertSame(response, builder.create());
  }

  @Test
  public void noModsReturnsSameResponseBuilderCtor() {
    HttpResponseBuilder builder = new HttpResponseBuilder();
    builder.setHttpStatusCode(HttpResponse.SC_OK);
    HttpResponseBuilder nextBuilder = new HttpResponseBuilder(builder);
    assertSame(builder.create(), nextBuilder.create());
  }

  @Test
  public void noModsReturnsSameResponseBaseCtor() {
    HttpResponse response = new HttpResponse("foo");
    HttpResponseBuilder builder = new HttpResponseBuilder(response);
    assertSame(response, builder.create());
    builder.setHttpStatusCode(HttpResponse.SC_BAD_GATEWAY);
    HttpResponse newResponse = builder.create();
    assertNotSame(response, newResponse);
    assertSame(newResponse, builder.create());
  }
}
