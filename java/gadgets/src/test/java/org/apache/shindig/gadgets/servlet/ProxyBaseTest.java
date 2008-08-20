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
package org.apache.shindig.gadgets.servlet;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpResponse;

import com.google.common.collect.Maps;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Tests for ProxyBase.
 */
public class ProxyBaseTest {

  private final ServletTestFixture fixture = new ServletTestFixture();

  private final ProxyBase proxy = new ProxyBase() {
    @Override
    public void fetch(HttpServletRequest request, HttpServletResponse response) {
      // Nothing.
    }
  };

  @Test
  public void validateUrlNoPath() throws Exception {
    Uri url = proxy.validateUrl("http://www.example.com");
    assertEquals("http", url.getScheme());
    assertEquals("www.example.com", url.getAuthority());
    assertEquals("/", url.getPath());
    assertNull(url.getQuery());
    assertNull(url.getFragment());
  }

  @Test
  public void validateUrlHttps() throws Exception {
    Uri url = proxy.validateUrl("https://www.example.com");
    assertEquals("https", url.getScheme());
    assertEquals("www.example.com", url.getAuthority());
    assertEquals("/", url.getPath());
    assertNull(url.getQuery());
    assertNull(url.getFragment());
  }

  @Test
  public void validateUrlWithPath() throws Exception {
    Uri url = proxy.validateUrl("http://www.example.com/foo");
    assertEquals("http", url.getScheme());
    assertEquals("www.example.com", url.getAuthority());
    assertEquals("/foo", url.getPath());
    assertNull(url.getQuery());
    assertNull(url.getFragment());
  }

  @Test
  public void validateUrlWithPort() throws Exception {
    Uri url = proxy.validateUrl("http://www.example.com:8080/foo");
    assertEquals("http", url.getScheme());
    assertEquals("www.example.com:8080", url.getAuthority());
    assertEquals("/foo", url.getPath());
    assertNull(url.getQuery());
    assertNull(url.getFragment());
  }

  @Test
  public void validateUrlWithEncodedPath() throws Exception {
    Uri url = proxy.validateUrl("http://www.example.com/foo%20bar");
    assertEquals("http", url.getScheme());
    assertEquals("www.example.com", url.getAuthority());
    assertEquals("/foo%20bar", url.getPath());
    assertNull(url.getQuery());
    assertNull(url.getFragment());
  }

  @Test
  public void validateUrlWithEncodedQuery() throws Exception {
    Uri url = proxy.validateUrl("http://www.example.com/foo?q=with%20space");
    assertEquals("http", url.getScheme());
    assertEquals("www.example.com", url.getAuthority());
    assertEquals("/foo", url.getPath());
    assertEquals("q=with%20space", url.getQuery());
    assertEquals("with space", url.getQueryParameter("q"));
    assertNull(url.getFragment());
  }

  @Test
  public void validateUrlWithNoPathAndEncodedQuery() throws Exception {
    Uri url = proxy.validateUrl("http://www.example.com?q=with%20space");
    assertEquals("http", url.getScheme());
    assertEquals("www.example.com", url.getAuthority());
    assertEquals("/", url.getPath());
    assertEquals("q=with%20space", url.getQuery());
    assertNull(url.getFragment());
  }

  @Test(expected = GadgetException.class)
  public void validateUrlNullInput() throws GadgetException {
    proxy.validateUrl(null);
  }

  @Test(expected = GadgetException.class)
  public void validateUrlBadInput() throws GadgetException {
    proxy.validateUrl("%$#%#$%#$%");
  }

  @Test(expected = GadgetException.class)
  public void validateUrlBadProtocol() throws GadgetException {
    proxy.validateUrl("gopher://foo");
  }

  @Test
  public void setResponseHeaders() {
    HttpResponse results = new HttpResponse(HttpResponse.SC_OK);
    fixture.replay();

    proxy.setResponseHeaders(fixture.request, fixture.recorder, results);

    // Just verify that they were set. Specific values are configurable.
    assertNotNull("Expires header not set", fixture.recorder.getHeader("Expires"));
    assertNotNull("Cache-Control header not set", fixture.recorder.getHeader("Cache-Control"));
    assertEquals("attachment;filename=p.txt", fixture.recorder.getHeader("Content-Disposition"));
  }

  @Test
  public void setResponseHeadersNoCache() {
    Map<String, List<String>> headers = Maps.newTreeMap(String.CASE_INSENSITIVE_ORDER);
    headers.put("Pragma", Arrays.asList("no-cache"));
    HttpResponse results = new HttpResponse(HttpResponse.SC_OK, new byte[0], headers);
    fixture.replay();

    proxy.setResponseHeaders(fixture.request, fixture.recorder, results);

    // Just verify that they were set. Specific values are configurable.
    assertNotNull("Expires header not set", fixture.recorder.getHeader("Expires"));
    assertEquals("no-cache", fixture.recorder.getHeader("Pragma"));
    assertEquals("no-cache", fixture.recorder.getHeader("Cache-Control"));
    assertEquals("attachment;filename=p.txt", fixture.recorder.getHeader("Content-Disposition"));
  }

  @Test
  public void setResponseHeadersForceParam() {
    HttpResponse results = new HttpResponse(HttpResponse.SC_OK);
    expect(fixture.request.getParameter(ProxyBase.REFRESH_PARAM)).andReturn("30").anyTimes();
    fixture.replay();

    proxy.setResponseHeaders(fixture.request, fixture.recorder, results);

    fixture.checkCacheControlHeaders(30, false);
    assertEquals("attachment;filename=p.txt", fixture.recorder.getHeader("Content-Disposition"));
  }

  @Test
  public void getParameter() {
    expect(fixture.request.getParameter("foo")).andReturn("bar");
    fixture.replay();

    assertEquals("bar", proxy.getParameter(fixture.request, "foo", "not foo"));
  }

  @Test
  public void getParameterWithNullValue() {
    expect(fixture.request.getParameter("foo")).andReturn(null);
    fixture.replay();

    assertEquals("not foo", proxy.getParameter(fixture.request, "foo", "not foo"));
  }
}
