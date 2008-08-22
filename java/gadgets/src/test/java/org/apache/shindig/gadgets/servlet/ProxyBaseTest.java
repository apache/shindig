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

import com.google.common.collect.Maps;
import org.apache.shindig.common.ContainerConfig;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import static org.easymock.EasyMock.expect;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Tests for ProxyBase.
 */
public class ProxyBaseTest extends ServletTestFixture {

  private final ProxyBase proxy = new ProxyBase() {
    @Override
    public void fetch(HttpServletRequest request, HttpServletResponse response) {
      // Nothing.
    }
  };

  public void testValidateUrlNoPath() throws Exception {
    Uri url = proxy.validateUrl("http://www.example.com");
    assertEquals("http", url.getScheme());
    assertEquals("www.example.com", url.getAuthority());
    assertEquals("/", url.getPath());
    assertNull(url.getQuery());
    assertNull(url.getFragment());
  }

  public void testValidateUrlHttps() throws Exception {
    Uri url = proxy.validateUrl("https://www.example.com");
    assertEquals("https", url.getScheme());
    assertEquals("www.example.com", url.getAuthority());
    assertEquals("/", url.getPath());
    assertNull(url.getQuery());
    assertNull(url.getFragment());
  }

  public void testValidateUrlWithPath() throws Exception {
    Uri url = proxy.validateUrl("http://www.example.com/foo");
    assertEquals("http", url.getScheme());
    assertEquals("www.example.com", url.getAuthority());
    assertEquals("/foo", url.getPath());
    assertNull(url.getQuery());
    assertNull(url.getFragment());
  }

  public void testValidateUrlWithPort() throws Exception {
    Uri url = proxy.validateUrl("http://www.example.com:8080/foo");
    assertEquals("http", url.getScheme());
    assertEquals("www.example.com:8080", url.getAuthority());
    assertEquals("/foo", url.getPath());
    assertNull(url.getQuery());
    assertNull(url.getFragment());
  }

  public void testValidateUrlWithEncodedPath() throws Exception {
    Uri url = proxy.validateUrl("http://www.example.com/foo%20bar");
    assertEquals("http", url.getScheme());
    assertEquals("www.example.com", url.getAuthority());
    assertEquals("/foo%20bar", url.getPath());
    assertNull(url.getQuery());
    assertNull(url.getFragment());
  }

  public void testValidateUrlWithEncodedQuery() throws Exception {
    Uri url = proxy.validateUrl("http://www.example.com/foo?q=with%20space");
    assertEquals("http", url.getScheme());
    assertEquals("www.example.com", url.getAuthority());
    assertEquals("/foo", url.getPath());
    assertEquals("q=with%20space", url.getQuery());
    assertEquals("with space", url.getQueryParameter("q"));
    assertNull(url.getFragment());
  }

  public void testValidateUrlWithNoPathAndEncodedQuery() throws Exception {
    Uri url = proxy.validateUrl("http://www.example.com?q=with%20space");
    assertEquals("http", url.getScheme());
    assertEquals("www.example.com", url.getAuthority());
    assertEquals("/", url.getPath());
    assertEquals("q=with%20space", url.getQuery());
    assertNull(url.getFragment());
  }

  public void testValidateUrlNullInput() {
    try {
      proxy.validateUrl(null);
      fail("Should have thrown");
    } catch (GadgetException e) {
      // good
    }
  }

  public void testValidateUrlBadInput() {
    try {
    proxy.validateUrl("%$#%#$%#$%");
    } catch (GadgetException e) {
      // good
    }
  }

  public void testValidateUrlBadProtocol() {
    try {
    proxy.validateUrl("gopher://foo");
  } catch (GadgetException e) {
    // good
  }
  }

  public void testSetResponseHeaders() {
    HttpResponse results = new HttpResponseBuilder().create();
    replay();

    proxy.setResponseHeaders(request, recorder, results);

    // Just verify that they were set. Specific values are configurable.
    assertNotNull("Expires header not set", recorder.getHeader("Expires"));
    assertNotNull("Cache-Control header not set", recorder.getHeader("Cache-Control"));
    assertEquals("attachment;filename=p.txt", recorder.getHeader("Content-Disposition"));
  }

  public void testSetResponseHeadersNoCache() {
    Map<String, List<String>> headers = Maps.newTreeMap(String.CASE_INSENSITIVE_ORDER);
    headers.put("Pragma", Arrays.asList("no-cache"));
    HttpResponse results = new HttpResponseBuilder()
        .addHeader("Pragma", "no-cache")
        .create();
    replay();

    proxy.setResponseHeaders(request, recorder, results);

    // Just verify that they were set. Specific values are configurable.
    assertNotNull("Expires header not set", recorder.getHeader("Expires"));
    assertEquals("no-cache", recorder.getHeader("Pragma"));
    assertEquals("no-cache", recorder.getHeader("Cache-Control"));
    assertEquals("attachment;filename=p.txt", recorder.getHeader("Content-Disposition"));
  }

  public void testSetResponseHeadersForceParam() {
    HttpResponse results = new HttpResponseBuilder().create();
    expect(request.getParameter(ProxyBase.REFRESH_PARAM)).andReturn("30").anyTimes();
    replay();

    proxy.setResponseHeaders(request, recorder, results);

    checkCacheControlHeaders(30, false);
    assertEquals("attachment;filename=p.txt", recorder.getHeader("Content-Disposition"));
  }

  public void testGetParameter() {
    expect(request.getParameter("foo")).andReturn("bar");
    replay();

    assertEquals("bar", proxy.getParameter(request, "foo", "not foo"));
  }

  public void testGetParameterWithNullValue() {
    expect(request.getParameter("foo")).andReturn(null);
    replay();

    assertEquals("not foo", proxy.getParameter(request, "foo", "not foo"));
  }

  public void testGetContainerWithContainer() {
    expect(request.getParameter(ProxyBase.CONTAINER_PARAM)).andReturn("bar");
    replay();

    assertEquals("bar", proxy.getContainer(request));
  }

  public void testGetContainerWithSynd() {
    expect(request.getParameter(ProxyBase.CONTAINER_PARAM)).andReturn(null);
    expect(request.getParameter(ProxyBase.SYND_PARAM)).andReturn("syndtainer");
    replay();

    assertEquals("syndtainer", proxy.getContainer(request));
  }

  public void testGetContainerNoParam() {
    expect(request.getParameter(ProxyBase.CONTAINER_PARAM)).andReturn(null);
    expect(request.getParameter(ProxyBase.SYND_PARAM)).andReturn(null);
    replay();

    assertEquals(ContainerConfig.DEFAULT_CONTAINER, proxy.getContainer(request));
  }
}
