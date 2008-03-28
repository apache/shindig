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

import static org.easymock.EasyMock.expect;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.net.URI;

import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.GadgetTestFixture;
import org.apache.shindig.gadgets.RemoteContent;
import org.apache.shindig.gadgets.RemoteContentRequest;
import org.easymock.EasyMock;
import org.easymock.IArgumentMatcher;
import org.json.JSONObject;

public class ProxyHandlerTest extends GadgetTestFixture {

  private final static String URL_ONE = "http://www.example.com/test.html";
  private final static String DATA_ONE = "hello world";

  final ByteArrayOutputStream baos = new ByteArrayOutputStream();
  final PrintWriter writer = new PrintWriter(baos);

  private void expectGetAndReturnData(String url, byte[] data) throws Exception {
    RemoteContentRequest req = new RemoteContentRequest(
        "GET", new URI(url), null, null, new RemoteContentRequest.Options());
    RemoteContent resp = new RemoteContent(200, data, null);
    expect(fetcher.fetch(req)).andReturn(resp);
  }

  private void expectPostAndReturnData(String url, byte[] body, byte[] data) throws Exception {
    RemoteContentRequest req = new RemoteContentRequest(
        "POST", new URI(url), null, body, new RemoteContentRequest.Options());
    RemoteContent resp = new RemoteContent(200, data, null);
    expect(fetcher.fetch(req)).andReturn(resp);
  }

  private void setupPostRequestMock(String url, String body) throws Exception {
    setupGenericRequestMock("POST", url);
    expect(request.getParameter("postData")).andReturn(body).atLeastOnce();
  }

  private void setupGetRequestMock(String url) throws Exception {
    setupGenericRequestMock("GET", url);
  }

  private void setupGenericRequestMock(String method, String url) throws Exception {
    expect(request.getMethod()).andReturn("POST").atLeastOnce();
    expect(request.getParameter("httpMethod")).andReturn(method).atLeastOnce();
    expect(request.getParameter("url")).andReturn(url).atLeastOnce();
    expect(response.getWriter()).andReturn(writer).atLeastOnce();
  }

  private JSONObject readJSONResponse(String body) throws Exception {
    String json = body.substring("throw 1; < don't be evil' >".length(), body.length());
    return new JSONObject(json);
  }

  public void testFetchJson() throws Exception {
    setupGetRequestMock(URL_ONE);
    expectGetAndReturnData(URL_ONE, DATA_ONE.getBytes());
    replay();
    proxyHandler.fetchJson(request, response, state);
    verify();
    writer.close();
    JSONObject json = readJSONResponse(baos.toString());
    JSONObject info = json.getJSONObject(URL_ONE);
    assertEquals(200, info.getInt("rc"));
    assertEquals(DATA_ONE, info.get("body"));
  }

  public void testFetchDecodedUrl() throws Exception {
    String origUrl = "http://www.example.com";
    String cleanedUrl = "http://www.example.com/";
    setupGetRequestMock(origUrl);
    expectGetAndReturnData(cleanedUrl, DATA_ONE.getBytes());
    replay();
    proxyHandler.fetchJson(request, response, state);
    verify();
    writer.close();
    JSONObject json = readJSONResponse(baos.toString());
    JSONObject info = json.getJSONObject(origUrl);
    assertEquals(200, info.getInt("rc"));
    assertEquals(DATA_ONE, info.get("body"));
  }

  public void testEmptyDocument() throws Exception {
    setupGetRequestMock(URL_ONE);
    expectGetAndReturnData(URL_ONE, "".getBytes());
    replay();
    proxyHandler.fetchJson(request, response, state);
    verify();
    writer.close();
    JSONObject json = readJSONResponse(baos.toString());
    JSONObject info = json.getJSONObject(URL_ONE);
    assertEquals(200, info.getInt("rc"));
    assertEquals("", info.get("body"));
  }

  public void testPostRequest() throws Exception {
    String body = "abc";
    setupPostRequestMock(URL_ONE, body);
    expectPostAndReturnData(URL_ONE, body.getBytes(), DATA_ONE.getBytes());
    replay();
    proxyHandler.fetchJson(request, response, state);
    verify();
    writer.close();
    JSONObject json = readJSONResponse(baos.toString());
    JSONObject info = json.getJSONObject(URL_ONE);
    assertEquals(200, info.getInt("rc"));
    assertEquals(DATA_ONE, info.get("body"));
  }

  public void testSignedGetRequest() throws Exception {
    setupGetRequestMock(URL_ONE);
    expect(request.getParameter("st")).andReturn("fake-token").atLeastOnce();
    expect(request.getParameter("authz")).andReturn("signed").atLeastOnce();
    RemoteContent resp = new RemoteContent(200, DATA_ONE.getBytes(), null);
    expect(fetcher.fetch(looksLikeSignedFetch(URL_ONE))).andReturn(resp);
    replay();
    proxyHandler.fetchJson(request, response, state);
    verify();
    writer.close();
  }

  public void testSignedPostRequest() throws Exception {
    String postBody = "foo=bar%20baz";
    setupPostRequestMock(URL_ONE, postBody);
    expect(request.getParameter("st")).andReturn("fake-token").atLeastOnce();
    expect(request.getParameter("authz")).andReturn("signed").atLeastOnce();
    RemoteContent resp = new RemoteContent(200, DATA_ONE.getBytes(), null);
    expect(fetcher.fetch(
        looksLikeSignedFetch(URL_ONE, postBody.getBytes()))).andReturn(resp);
    replay();
    proxyHandler.fetchJson(request, response, state);
    verify();
    writer.close();
  }
  
  public void testInvalidSigningType() throws Exception {
    setupGetRequestMock(URL_ONE);
    expect(request.getParameter("st")).andReturn("fake-token").atLeastOnce();
    expect(request.getParameter("authz")).andReturn("garbage").atLeastOnce();
    replay();
    try {
      proxyHandler.fetchJson(request, response, state);
      fail("proxyHandler accepted invalid authz type");
    } catch (GadgetException e) {
      assertEquals(GadgetException.Code.UNSUPPORTED_FEATURE, e.getCode());
    }
  }
  
  private RemoteContentRequest looksLikeSignedFetch(String url) {
    return looksLikeSignedFetch(url, null);
  }
  
  private RemoteContentRequest looksLikeSignedFetch(String url, byte[] postBody) {
    EasyMock.reportMatcher(new SignedFetchArgumentMatcher(url, postBody));
    return null;
  }

  private class SignedFetchArgumentMatcher implements IArgumentMatcher {

    private String expectedUrl;
    private byte[] postBody;

    public SignedFetchArgumentMatcher(String expectedUrl) {
      this(expectedUrl, null);
    }
    
    public SignedFetchArgumentMatcher(String expectedUrl, byte[] postBody) {
      this.expectedUrl = expectedUrl;
      this.postBody = postBody;
    }

    public void appendTo(StringBuffer sb) {
      sb.append("SignedFetchArgumentMatcher(");
      sb.append(expectedUrl);
      sb.append(')');
    }

    public boolean matches(Object arg0) {
      RemoteContentRequest request = (RemoteContentRequest)arg0;
      String url = request.getUri().toASCIIString();
      if (url.startsWith(expectedUrl) &&
          url.contains("opensocial_owner_id") && 
          url.contains("opensocial_viewer_id") &&
          url.contains("opensocial_app_id") &&
          byteArrayEquals(postBody, request.getPostBodyBytes())) {
        return true;
      }
      return false;
    }

    private boolean byteArrayEquals(byte[] expected, byte[] actual) {
      if (expected == null) {
        expected = new byte[0];
      }
      if (actual == null) {
        actual = new byte[0];
      }
      if (expected.length != actual.length) {
        return false;
      }
      for (int i=0; i < expected.length; i++) {
        if (expected[i] != actual[i]) {
          return false;
        }
      }
      return true;
    }
  }
  
  public void testValidateUrlNoPath() throws Exception {
    URI url = proxyHandler.validateUrl("http://www.example.com");
    assertEquals("http", url.getScheme());
    assertEquals("www.example.com", url.getHost());
    assertEquals(-1, url.getPort());
    assertEquals("/", url.getPath());
    assertNull(url.getQuery());
    assertNull(url.getFragment());
  }
  
  public void testValidateUrlWithPath() throws Exception {
    URI url = proxyHandler.validateUrl("http://www.example.com/foo");
    assertEquals("http", url.getScheme());
    assertEquals("www.example.com", url.getHost());
    assertEquals(-1, url.getPort());
    assertEquals("/foo", url.getPath());
    assertNull(url.getQuery());
    assertNull(url.getFragment());
  }
  
  public void testValidateUrlWithPort() throws Exception {
    URI url = proxyHandler.validateUrl("http://www.example.com:8080/foo");
    assertEquals("http", url.getScheme());
    assertEquals("www.example.com", url.getHost());
    assertEquals(8080, url.getPort());
    assertEquals("/foo", url.getPath());
    assertNull(url.getQuery());
    assertNull(url.getFragment());
  }
  
  public void testValidateUrlWithEncodedPath() throws Exception {
    URI url = proxyHandler.validateUrl("http://www.example.com:8080/foo%20bar");
    assertEquals("http", url.getScheme());
    assertEquals("www.example.com", url.getHost());
    assertEquals(8080, url.getPort());
    assertEquals("/foo%20bar", url.getRawPath());
    assertEquals("/foo bar", url.getPath());
    assertNull(url.getQuery());
    assertNull(url.getFragment());    
  }
  
  public void testValidateUrlWithEncodedQuery() throws Exception {
    URI url = proxyHandler.validateUrl("http://www.example.com:8080/foo?q=with%20space");
    assertEquals("http", url.getScheme());
    assertEquals("www.example.com", url.getHost());
    assertEquals(8080, url.getPort());
    assertEquals("/foo", url.getPath());
    assertEquals("q=with%20space", url.getRawQuery());
    assertEquals("q=with space", url.getQuery());
    assertNull(url.getFragment());    
  }
  
  public void testValidateUrlWithNoPathAndEncodedQuery() throws Exception {
    URI url = proxyHandler.validateUrl("http://www.example.com?q=with%20space");
    assertEquals("http", url.getScheme());
    assertEquals("www.example.com", url.getHost());
    assertEquals(-1, url.getPort());
    assertEquals("/", url.getPath());
    assertEquals("q=with%20space", url.getRawQuery());
    assertEquals("q=with space", url.getQuery());
    assertNull(url.getFragment());        
  }
}
