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

import junitx.framework.ArrayAssert;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.uri.UriBuilder;
import org.apache.shindig.gadgets.GadgetException;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Holds test cases that all HttpFetcher implementations should pass.  This
 * starts up an HTTP server and runs tests against it.
 */
public abstract class AbstractHttpFetcherTest {
  private static final int ECHO_PORT = 9003;
  protected static final Uri BASE_URL = Uri.parse("http://localhost:9003/");
  private static EchoServer server;
  protected HttpFetcher fetcher = null;

  @BeforeClass
  public static void setUpOnce() throws Exception {
    server = new EchoServer();
    server.start(ECHO_PORT);
  }

  @AfterClass
  public static void tearDownOnce() throws Exception {
    if (server != null) {
      server.stop();
    }
  }

  @Test public void testHttpBadHost() throws Exception {
    Uri uri = Uri.parse("http://a:b:c/");
    HttpRequest request = new HttpRequest(uri);
    try {
      fetcher.fetch(request);
      fail("Expected GadgetException");
    } catch (GadgetException e) {
      assertEquals(400, e.getHttpStatusCode());
      assertTrue(e.getMessage().contains("Bad host name in request"));
    }
  }

  @Test public void testHttpBadPort() throws Exception {
    Uri uri = Uri.parse("http://a:b/");
    HttpRequest request = new HttpRequest(uri);
    try {
      fetcher.fetch(request);
      fail("Expected GadgetException");
    } catch (GadgetException e) {
      assertEquals(400, e.getHttpStatusCode());
      assertTrue(e.getMessage().contains("Bad port number in request"));
    }
  }

  @Test public void testHttpBadUrl() throws Exception {
    Uri uri = Uri.parse("host/data");
    HttpRequest request = new HttpRequest(uri);
    try {
      fetcher.fetch(request);
      fail("Expected GadgetException");
    } catch (GadgetException e) {
      assertEquals(400, e.getHttpStatusCode());
      assertTrue(e.getMessage().contains("Missing domain name for request"));
    }
  }

  @Test public void testHttpNoSchema() throws Exception {
    Uri uri = Uri.parse("//host/data");
    HttpRequest request = new HttpRequest(uri);
    try {
      fetcher.fetch(request);
      fail("Expected GadgetException");
    } catch (GadgetException e) {
      assertEquals(400, e.getHttpStatusCode());
      assertTrue(e.getMessage().contains("Missing schema for request"));
    }
  }

  @Test public void testHttpUnderscore() throws Exception {
    Uri uri = Uri.parse("http://0.test_host.com/data");
    HttpRequest request = new HttpRequest(uri);
    HttpResponse response = fetcher.fetch(request);
    assertEquals(504, response.getHttpStatusCode()); //timeout
  }

  @Test public void testHttpFetch() throws Exception {
    String content = "Hello, world!";
    Uri uri = new UriBuilder(BASE_URL).addQueryParameter("body", content).toUri();
    HttpRequest request = new HttpRequest(uri);
    HttpResponse response = fetcher.fetch(request);
    assertEquals(200, response.getHttpStatusCode());
    assertEquals(content, response.getResponseAsString());
  }

  @Test public void testHttp404() throws Exception {
    String content = "Hello, world!";
    Uri uri = new UriBuilder(BASE_URL)
        .addQueryParameter("body", content)
        .addQueryParameter("status", "404")
        .toUri();
    HttpRequest request = new HttpRequest(uri);
    HttpResponse response = fetcher.fetch(request);
    assertEquals(404, response.getHttpStatusCode());
    assertEquals(content, response.getResponseAsString());
  }

  @Test public void testHttp403() throws Exception {
    String content = "Hello, world!";
    Uri uri = new UriBuilder(BASE_URL)
        .addQueryParameter("body", content)
        .addQueryParameter("status", "403")
        .addQueryParameter("header", "WWW-Authenticate=some auth data")
        .toUri();
    HttpRequest request = new HttpRequest(uri);
    HttpResponse response = fetcher.fetch(request);
    assertEquals(403, response.getHttpStatusCode());
    assertEquals(content, response.getResponseAsString());
    assertEquals("some auth data", response.getHeader("WWW-Authenticate"));
  }

  @Test public void testHttp403NoBody() throws Exception {
    String content = "";
    Uri uri = new UriBuilder(BASE_URL)
        .addQueryParameter("body", content)
        .addQueryParameter("status", "403")
        .addQueryParameter("header", "WWW-Authenticate=some auth data")
        .toUri();
    HttpRequest request = new HttpRequest(uri);
    HttpResponse response = fetcher.fetch(request);
    assertEquals(403, response.getHttpStatusCode());
    assertEquals(content, response.getResponseAsString());
    assertEquals("some auth data", response.getHeader("WWW-Authenticate"));
  }

  @Test public void testHttp401NoBody() throws Exception {
    String content = "";
    Uri uri = new UriBuilder(BASE_URL)
        .addQueryParameter("body", content)
        .addQueryParameter("status", "401")
        .addQueryParameter("header", "WWW-Authenticate=some auth data")
        .toUri();
    HttpRequest request = new HttpRequest(uri);
    HttpResponse response = fetcher.fetch(request);
    assertEquals(401, response.getHttpStatusCode());
    assertEquals(content, response.getResponseAsString());
    assertEquals("some auth data", response.getHeader("WWW-Authenticate"));
  }

  @Test public void testDelete() throws Exception {
    HttpRequest request = new HttpRequest(BASE_URL).setMethod("DELETE");
    HttpResponse response = fetcher.fetch(request);
    assertEquals("DELETE", response.getHeader("x-method"));
  }

  @Test public void testPost_noBody() throws Exception {
    HttpRequest request = new HttpRequest(BASE_URL).setMethod("POST");
    HttpResponse response = fetcher.fetch(request);
    assertEquals("POST", response.getHeader("x-method"));
    assertEquals("", response.getResponseAsString());
  }

  @Test public void testPost_withBody() throws Exception {
    byte[] body = new byte[5000];
    for (int i=0; i < body.length; ++i) {
      body[i] = (byte)(i % 255);
    }
    HttpRequest request = new HttpRequest(BASE_URL)
        .setMethod("POST")
        .setPostBody(body)
        .addHeader("content-type", "application/octet-stream");
    HttpResponse response = fetcher.fetch(request);
    assertEquals("POST", response.getHeader("x-method"));
    ArrayAssert.assertEquals(body, response.getResponseAsBytes());
  }

  @Test public void testPut_noBody() throws Exception {
    HttpRequest request = new HttpRequest(BASE_URL).setMethod("PUT");
    HttpResponse response = fetcher.fetch(request);
    assertEquals("PUT", response.getHeader("x-method"));
    assertEquals("", response.getResponseAsString());
  }

  @Test public void testPut_withBody() throws Exception {
    byte[] body = new byte[5000];
    for (int i=0; i < body.length; ++i) {
      body[i] = (byte)i;
    }
    HttpRequest request = new HttpRequest(BASE_URL)
        .setMethod("PUT")
        .setPostBody(body)
        .addHeader("content-type", "application/octet-stream");
    HttpResponse response = fetcher.fetch(request);
    assertEquals("PUT", response.getHeader("x-method"));
    ArrayAssert.assertEquals(body, response.getResponseAsBytes());
  }

  @Test public void testHugeBody() throws Exception {
    byte[] body = new byte[1024*1024]; // 1 MB
    for (int i=0; i < body.length; ++i) {
      body[i] = (byte)i;
    }
    HttpRequest request = new HttpRequest(BASE_URL)
        .setMethod("POST")
        .setPostBody(body)
        .addHeader("content-type", "application/octet-stream");
    HttpResponse response = fetcher.fetch(request);
    assertEquals("POST", response.getHeader("x-method"));
    ArrayAssert.assertEquals(body, response.getResponseAsBytes());
  }

  @Test public void testFollowRedirects() throws Exception {
    String content = "";
    Uri uri = new UriBuilder(BASE_URL)
        .addQueryParameter("body", content)
        .addQueryParameter("status", "302")
        .addQueryParameter("header", "Location=" + BASE_URL.toString() + "?body=redirected")
        .toUri();
    HttpRequest request = new HttpRequest(uri);
    HttpResponse response = fetcher.fetch(request);
    assertEquals(200, response.getHttpStatusCode());
    assertEquals("redirected", response.getResponseAsString());
  }

  @Test public void testFollowRelativeRedirects() throws Exception {
    String content = "";
    Uri uri = new UriBuilder(BASE_URL)
        .addQueryParameter("body", content)
        .addQueryParameter("status", "302")
        .addQueryParameter("header", "Location=/?body=redirected")
        .toUri();
    HttpRequest request = new HttpRequest(uri);
    HttpResponse response = fetcher.fetch(request);
    assertEquals(200, response.getHttpStatusCode());
    assertEquals("redirected", response.getResponseAsString());
  }

  @Test public void testNoFollowRedirects() throws Exception {
    String content = "";
    Uri uri = new UriBuilder(BASE_URL)
        .addQueryParameter("body", content)
        .addQueryParameter("status", "302")
        .addQueryParameter("header", "Location=" + BASE_URL.toString() + "?body=redirected")
        .toUri();
    HttpRequest request = new HttpRequest(uri)
        .setFollowRedirects(false);
    HttpResponse response = fetcher.fetch(request);
    assertEquals(302, response.getHttpStatusCode());
    assertEquals(content, response.getResponseAsString());
    assertEquals(BASE_URL.toString() + "?body=redirected", response.getHeader("Location"));
  }
}
