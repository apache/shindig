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

import org.apache.commons.codec.binary.Base64;
import org.apache.shindig.common.servlet.HttpServletResponseRecorder;
import org.apache.shindig.common.servlet.HttpUtil;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.util.DateUtil;
import org.apache.shindig.common.util.FakeTimeSource;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.easymock.EasyMock;
import org.json.JSONObject;
import org.junit.Test;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Vector;

import static junitx.framework.ComparableAssert.assertGreater;
import static junitx.framework.ComparableAssert.assertLesser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ServletUtilTest {
  @Test
  public void testValidateUrlNoPath() throws Exception {
    Uri url = ServletUtil.validateUrl(Uri.parse("http://www.example.com"));
    assertEquals("http", url.getScheme());
    assertEquals("www.example.com", url.getAuthority());
    assertEquals("/", url.getPath());
    assertNull(url.getQuery());
    assertNull(url.getFragment());
  }

  @Test
  public void testValidateUrlHttps() throws Exception {
    Uri url = ServletUtil.validateUrl(Uri.parse("https://www.example.com"));
    assertEquals("https", url.getScheme());
    assertEquals("www.example.com", url.getAuthority());
    assertEquals("/", url.getPath());
    assertNull(url.getQuery());
    assertNull(url.getFragment());
  }

  @Test
  public void testValidateUrlWithPath() throws Exception {
    Uri url = ServletUtil.validateUrl(Uri.parse("http://www.example.com/foo"));
    assertEquals("http", url.getScheme());
    assertEquals("www.example.com", url.getAuthority());
    assertEquals("/foo", url.getPath());
    assertNull(url.getQuery());
    assertNull(url.getFragment());
  }

  @Test
  public void testValidateUrlWithPort() throws Exception {
    Uri url = ServletUtil.validateUrl(Uri.parse("http://www.example.com:8080/foo"));
    assertEquals("http", url.getScheme());
    assertEquals("www.example.com:8080", url.getAuthority());
    assertEquals("/foo", url.getPath());
    assertNull(url.getQuery());
    assertNull(url.getFragment());
  }

  @Test
  public void testValidateUrlWithEncodedPath() throws Exception {
    Uri url = ServletUtil.validateUrl(Uri.parse("http://www.example.com/foo%20bar"));
    assertEquals("http", url.getScheme());
    assertEquals("www.example.com", url.getAuthority());
    assertEquals("/foo%20bar", url.getPath());
    assertNull(url.getQuery());
    assertNull(url.getFragment());
  }

  @Test
  public void testValidateUrlWithEncodedQuery() throws Exception {
    Uri url = ServletUtil.validateUrl(Uri.parse("http://www.example.com/foo?q=with%20space"));
    assertEquals("http", url.getScheme());
    assertEquals("www.example.com", url.getAuthority());
    assertEquals("/foo", url.getPath());
    assertEquals("q=with%20space", url.getQuery());
    assertEquals("with space", url.getQueryParameter("q"));
    assertNull(url.getFragment());
  }

  @Test
  public void testValidateUrlWithNoPathAndEncodedQuery() throws Exception {
    Uri url = ServletUtil.validateUrl(Uri.parse("http://www.example.com?q=with%20space"));
    assertEquals("http", url.getScheme());
    assertEquals("www.example.com", url.getAuthority());
    assertEquals("/", url.getPath());
    assertEquals("q=with%20space", url.getQuery());
    assertNull(url.getFragment());
  }

  @Test(expected = GadgetException.class)
  public void testValidateUrlNullInput() throws Exception {
    ServletUtil.validateUrl(null);
  }

  @Test
  public void testOutputDataUri() throws Exception {
    checkOutputDataUri("text/foo", "text/foo", "UTF-8");
  }

  @Test
  public void testOutputDataUriWithCharset() throws Exception {
    checkOutputDataUri("text/bar; charset=ISO-8859-1", "text/bar", "ISO-8859-1");
  }

  @Test
  public void testOutputDataUriWithEmptyCharset() throws Exception {
    checkOutputDataUri("text/bar; charset=", "text/bar", "UTF-8");
  }

  private void checkOutputDataUri(String contentType, String expectedType,
      String expectedEncoding) throws Exception {
    String theData = "this is the data";
    String mk1 = "meta1", mv1 = "val1";
    String mk2 = "'\"}key", mv2 = "val{\"'";
    HttpResponse response = new HttpResponseBuilder()
      .setResponseString(theData)
      .addHeader("Content-Type", contentType)
      .setMetadata(mk1, mv1)
      .setMetadata(mk2, mv2)
      .setMetadata(ServletUtil.DATA_URI_KEY, "foo")  // Should be ignored.
      .create();

    HttpResponse jsonified = ServletUtil.convertToJsonResponse(response);

    assertEquals("application/json; charset=UTF-8", jsonified.getHeader("Content-Type"));

    String emitted = jsonified.getResponseAsString();
    JSONObject js = new JSONObject(emitted);
    assertEquals(mv1, js.getString(mk1));
    assertEquals(mv2, js.getString(mk2));
    String content64 = getBase64(theData);
    assertEquals("data:" + expectedType + ";base64;charset=" + expectedEncoding + "," + content64,
        js.getString(ServletUtil.DATA_URI_KEY));
  }

  private String getBase64(String input) throws Exception {
    return new String(Base64.encodeBase64(input.getBytes("UTF8")), "UTF8");
  }

  @Test
  public void testFromHttpServletRequest() throws Exception {
    HttpServletRequest original = EasyMock.createMock(HttpServletRequest.class);
    EasyMock.expect(original.getScheme()).andReturn("https");
    EasyMock.expect(original.getServerName()).andReturn("www.example.org");
    EasyMock.expect(original.getServerPort()).andReturn(444);
    EasyMock.expect(original.getRequestURI()).andReturn("/path/foo");
    EasyMock.expect(original.getQueryString()).andReturn("one=two&three=four");
    Vector<String> headerNames = new Vector<String>();
    headerNames.add("Header1");
    headerNames.add("Header2");
    EasyMock.expect(original.getHeaderNames()).andReturn(headerNames.elements());
    EasyMock.expect(original.getHeaders("Header1"))
        .andReturn(makeEnumeration("HVal1", "HVal3"));
    EasyMock.expect(original.getHeaders("Header2"))
        .andReturn(makeEnumeration("HVal2", "HVal4"));
    EasyMock.expect(original.getMethod()).andReturn("post");
    final ByteArrayInputStream bais = new ByteArrayInputStream("post body".getBytes());
    ServletInputStream sis = new ServletInputStream() {
      @Override
      public int read() throws IOException {
        return bais.read();
      }
    };
    EasyMock.expect(original.getInputStream()).andReturn(sis);
    EasyMock.expect(original.getRemoteAddr()).andReturn("1.2.3.4");

    EasyMock.replay(original);
    HttpRequest request = ServletUtil.fromHttpServletRequest(original);
    EasyMock.verify(original);

    assertEquals(Uri.parse("https://www.example.org:444/path/foo?one=two&three=four"),
        request.getUri());
    assertEquals(3, request.getHeaders().size());
    assertEquals("HVal1", request.getHeaders("Header1").get(0));
    assertEquals("HVal3", request.getHeaders("Header1").get(1));
    assertEquals("HVal2", request.getHeaders("Header2").get(0));
    assertEquals("HVal4", request.getHeaders("Header2").get(1));
    assertEquals("post", request.getMethod());
    assertEquals("post body", request.getPostBodyAsString());
    assertEquals("1.2.3.4", request.getParam(ServletUtil.REMOTE_ADDR_KEY));
  }

  @Test
  public void testCopyToServletResponseAndOverrideCacheHeadersForPublic() throws Exception {
    FakeTimeSource fakeTime = new FakeTimeSource();
    HttpUtil.setTimeSource(fakeTime);

    HttpResponse response = new HttpResponseBuilder()
        .setResponseString("response string").setHttpStatusCode(200).addHeader("h1", "v1")
        .addHeader("h2", "v2").setCacheTtl(1000).create();

    HttpServletResponse servletResponse = EasyMock.createMock(HttpServletResponse.class);
    HttpServletResponseRecorder recorder = new HttpServletResponseRecorder(servletResponse);

    ServletUtil.copyToServletResponseAndOverrideCacheHeaders(response, recorder);

    assertEquals(200, recorder.getHttpStatusCode());
    assertEquals("response string", recorder.getResponseAsString());
    assertEquals("v1", recorder.getHeader("h1"));
    assertEquals("v2", recorder.getHeader("h2"));
    assertEquals("public,max-age=1000", recorder.getHeader("Cache-Control"));
  }

  @Test
  public void testCopyToServletResponse() throws Exception {
    HttpResponse response = new HttpResponseBuilder()
        .setResponseString("response string").setHttpStatusCode(200).addHeader("h1", "v1")
        .addHeader("h2", "v2").addHeader("Cache-Control", "private,no-store,max-age=10")
        .addHeader("Expires", "123").create();

    HttpServletResponse servletResponse = EasyMock.createMock(HttpServletResponse.class);
    HttpServletResponseRecorder recorder = new HttpServletResponseRecorder(servletResponse);

    ServletUtil.copyToServletResponse(response, recorder);

    assertEquals(200, recorder.getHttpStatusCode());
    assertEquals("response string", recorder.getResponseAsString());
    assertEquals("v1", recorder.getHeader("h1"));
    assertEquals("v2", recorder.getHeader("h2"));
    assertEquals("private,no-store,max-age=10", recorder.getHeader("Cache-Control"));
    assertEquals("123", recorder.getHeader("Expires"));
  }

  @Test
  public void testCopyToServletResponseAndOverrideCacheHeadersForPrivate()
      throws Exception {
    FakeTimeSource fakeTime = new FakeTimeSource();
    HttpUtil.setTimeSource(fakeTime);

    HttpResponse response = new HttpResponseBuilder()
        .setResponseString("response string").setHttpStatusCode(200).addHeader("h1", "v1")
        .addHeader("h2", "v2").addHeader("Cache-Control", "private,no-store,max-age=10")
        .addHeader("Expires", "123").create();

    HttpServletResponse servletResponse = EasyMock.createMock(HttpServletResponse.class);
    HttpServletResponseRecorder recorder = new HttpServletResponseRecorder(servletResponse);
    long testStartTime = fakeTime.currentTimeMillis();
    ServletUtil.copyToServletResponseAndOverrideCacheHeaders(response, recorder);
    assertEquals(200, recorder.getHttpStatusCode());
    assertEquals("response string", recorder.getResponseAsString());
    assertEquals("v1", recorder.getHeader("h1"));
    assertEquals("v2", recorder.getHeader("h2"));
    assertEquals("no-cache", recorder.getHeader("Cache-Control"));
    long expires = DateUtil.parseRfc1123Date(recorder.getHeader("Expires")).getTime();
    assertGreater(testStartTime - 2000L, expires);
    assertLesser(testStartTime + 2000L, expires);
  }

  @Test
  public void testCopyToServletResponseAndOverrideCacheHeadersForStrictNoCache()
      throws Exception {
    HttpResponse response = new HttpResponseBuilder()
        .setResponseString("response string").setHttpStatusCode(200).addHeader("h1", "v1")
        .addHeader("h2", "v2").setStrictNoCache().create();

    HttpServletResponse servletResponse = EasyMock.createMock(HttpServletResponse.class);
    HttpServletResponseRecorder recorder = new HttpServletResponseRecorder(servletResponse);

    FakeTimeSource fakeTime = new FakeTimeSource();
    HttpUtil.setTimeSource(fakeTime);
    ServletUtil.copyToServletResponseAndOverrideCacheHeaders(response, recorder);

    assertEquals(200, recorder.getHttpStatusCode());
    assertEquals("response string", recorder.getResponseAsString());
    assertEquals("v1", recorder.getHeader("h1"));
    assertEquals("v2", recorder.getHeader("h2"));
    assertEquals("no-cache", recorder.getHeader("Pragma"));
    assertEquals("no-cache", recorder.getHeader("Cache-Control"));
  }

  Enumeration<String> makeEnumeration(String... args) {
    Vector<String> vector = new Vector<String>();
    vector.addAll(Arrays.asList(args));
    return vector.elements();
  }
}
