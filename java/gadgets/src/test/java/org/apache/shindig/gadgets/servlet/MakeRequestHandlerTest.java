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

import static junitx.framework.StringAssert.assertStartsWith;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.apache.shindig.auth.AuthInfo;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.servlet.HttpUtilTest;
import org.apache.shindig.common.testing.FakeGadgetToken;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.gadgets.AuthType;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.apache.shindig.gadgets.uri.UriCommon.Param;
import org.easymock.Capture;
import org.easymock.IAnswer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

/**
 * Tests for MakeRequestHandler.
 */
public class MakeRequestHandlerTest extends ServletTestFixture {
  private static final Uri REQUEST_URL = Uri.parse("http://example.org/file");
  private static final String REQUEST_BODY = "I+am+the+request+body!foo=baz%20la";
  private static final String RESPONSE_BODY = "makeRequest response body";
  private static final SecurityToken DUMMY_TOKEN = new FakeGadgetToken();

  private final MakeRequestHandler handler
      = new MakeRequestHandler(pipeline, rewriterRegistry, feedProcessorProvider);

  private void expectGetAndReturnBody(String response) throws Exception {
    expectGetAndReturnBody(AuthType.NONE, response);
  }

  private void expectGetAndReturnBody(AuthType authType, String response) throws Exception {
    HttpRequest request = new HttpRequest(REQUEST_URL).setAuthType(authType);
    expect(pipeline.execute(request)).andReturn(new HttpResponse(response));
  }

  private void expectPostAndReturnBody(String postData, String response) throws Exception {
    expectPostAndReturnBody(AuthType.NONE, postData, response);
  }

  private void expectPostAndReturnBody(AuthType authType, String postData, String response)
      throws Exception {
    HttpRequest req = new HttpRequest(REQUEST_URL).setMethod("POST")
        .setPostBody(REQUEST_BODY.getBytes("UTF-8"))
        .setAuthType(authType)
        .addHeader("Content-Type", "application/x-www-form-urlencoded");
    expect(pipeline.execute(req)).andReturn(new HttpResponse(response));
    expect(request.getParameter(MakeRequestHandler.METHOD_PARAM)).andReturn("POST");
    expect(request.getParameter(MakeRequestHandler.POST_DATA_PARAM))
        .andReturn(REQUEST_BODY);
  }

  private JSONObject extractJsonFromResponse() throws JSONException {
    String body = recorder.getResponseAsString();
    assertStartsWith(MakeRequestHandler.UNPARSEABLE_CRUFT, body);
    body = body.substring(MakeRequestHandler.UNPARSEABLE_CRUFT.length());
    return new JSONObject(body).getJSONObject(REQUEST_URL.toString());
  }

  @Before
  public void setUp() {
    expect(request.getMethod()).andReturn("POST").anyTimes();
    expect(request.getParameter(Param.URL.getKey()))
        .andReturn(REQUEST_URL.toString()).anyTimes();
  }

  @Test
  public void testGetRequest() throws Exception {
    expectGetAndReturnBody(RESPONSE_BODY);
    replay();

    handler.fetch(request, recorder);

    JSONObject results = extractJsonFromResponse();
    assertEquals(HttpResponse.SC_OK, results.getInt("rc"));
    assertEquals(RESPONSE_BODY, results.get("body"));
    assertTrue(rewriter.responseWasRewritten());
  }

  @Test
  public void testGetRequestWithUncommonStatusCode() throws Exception {
    HttpRequest req = new HttpRequest(REQUEST_URL);
    HttpResponse response = new HttpResponseBuilder()
        .setHttpStatusCode(HttpResponse.SC_CREATED)
        .setResponseString(RESPONSE_BODY)
        .create();
    expect(pipeline.execute(req)).andReturn(response);
    replay();

    handler.fetch(request, recorder);

    JSONObject results = extractJsonFromResponse();
    assertEquals(HttpResponse.SC_CREATED, results.getInt("rc"));
    assertEquals(RESPONSE_BODY, results.get("body"));
    assertTrue(rewriter.responseWasRewritten());
  }

  @Test
  public void testGetRequestWithRefresh() throws Exception {
    expect(request.getParameter(Param.REFRESH.getKey())).andReturn("120").anyTimes();

    Capture<HttpRequest> requestCapture = new Capture<HttpRequest>();
    expect(pipeline.execute(capture(requestCapture))).andReturn(new HttpResponse(RESPONSE_BODY));

    replay();

    handler.fetch(request, recorder);

    HttpRequest httpRequest = requestCapture.getValue();
    assertEquals("public,max-age=120", recorder.getHeader("Cache-Control"));
    assertEquals(120, httpRequest.getCacheTtl());
  }

  @Test
  public void testGetRequestWithBadTtl() throws Exception {
    expect(request.getParameter(Param.REFRESH.getKey())).andReturn("foo").anyTimes();

    Capture<HttpRequest> requestCapture = new Capture<HttpRequest>();
    expect(pipeline.execute(capture(requestCapture))).andReturn(new HttpResponse(RESPONSE_BODY));

    replay();

    try {
      handler.fetch(request, recorder);
    } catch (GadgetException e) {
      // Expected - catch now occurs at the MakeRequestServlet level.
    }

    HttpRequest httpRequest = requestCapture.getValue();
    assertEquals(null, recorder.getHeader("Cache-Control"));
    assertEquals(-1, httpRequest.getCacheTtl());
  }


  @Test
  public void testExplicitHeaders() throws Exception {
    String headerString = "X-Foo=bar&X-Bar=baz%20foo";

    HttpRequest expected = new HttpRequest(REQUEST_URL)
        .addHeader("X-Foo", "bar")
        .addHeader("X-Bar", "baz foo");
    expect(pipeline.execute(expected)).andReturn(new HttpResponse(RESPONSE_BODY));
    expect(request.getParameter(MakeRequestHandler.HEADERS_PARAM)).andReturn(headerString);
    replay();

    handler.fetch(request, recorder);
    verify();

    JSONObject results = extractJsonFromResponse();
    assertEquals(HttpResponse.SC_OK, results.getInt("rc"));
    assertEquals(RESPONSE_BODY, results.get("body"));
    assertTrue(rewriter.responseWasRewritten());
  }

  @Test
  public void testPostRequest() throws Exception {
    expect(request.getParameter(MakeRequestHandler.METHOD_PARAM)).andReturn("POST");
    expectPostAndReturnBody(REQUEST_BODY, RESPONSE_BODY);
    replay();

    handler.fetch(request, recorder);
    JSONObject results = extractJsonFromResponse();

    assertEquals(HttpResponse.SC_OK, results.getInt("rc"));
    assertEquals(RESPONSE_BODY, results.get("body"));
    assertTrue(rewriter.responseWasRewritten());
  }

  @Test
  public void testFetchContentTypeFeed() throws Exception {
    String entryTitle = "Feed title";
    String entryLink = "http://example.org/entry/0/1";
    String entrySummary = "This is the summary";
    String rss = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                 "<rss version=\"2.0\"><channel>" +
                 "<title>dummy</title>" +
                 "<link>http://example.org/</link>" +
                 "<item>" +
                 "<title>" + entryTitle + "</title>" +
                 "<link>" + entryLink + "</link>" +
                 "<description>" + entrySummary + "</description>" +
                 "</item>" +
                 "</channel></rss>";

    expectGetAndReturnBody(rss);
    expect(request.getParameter(MakeRequestHandler.CONTENT_TYPE_PARAM)).andReturn("FEED");
    replay();

    handler.fetch(request, recorder);
    JSONObject results = extractJsonFromResponse();

    JSONObject feed = new JSONObject(results.getString("body"));
    JSONObject entry = feed.getJSONArray("Entry").getJSONObject(0);

    assertEquals(entryTitle, entry.getString("Title"));
    assertEquals(entryLink, entry.getString("Link"));
    assertNull("getSummaries has the wrong default value (should be false).",
        entry.optString("Summary", null));
    assertTrue(rewriter.responseWasRewritten());
  }

  @Test
  public void testFetchFeedWithParameters() throws Exception {
    String entryTitle = "Feed title";
    String entryLink = "http://example.org/entry/0/1";
    String entrySummary = "This is the summary";
    String rss = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                 "<rss version=\"2.0\"><channel>" +
                 "<title>dummy</title>" +
                 "<link>http://example.org/</link>" +
                 "<item>" +
                 "<title>" + entryTitle + "</title>" +
                 "<link>" + entryLink + "</link>" +
                 "<description>" + entrySummary + "</description>" +
                 "</item>" +
                 "<item>" +
                 "<title>" + entryTitle + "</title>" +
                 "<link>" + entryLink + "</link>" +
                 "<description>" + entrySummary + "</description>" +
                 "</item>" +
                 "<item>" +
                 "<title>" + entryTitle + "</title>" +
                 "<link>" + entryLink + "</link>" +
                 "<description>" + entrySummary + "</description>" +
                 "</item>" +
                 "</channel></rss>";

    expectGetAndReturnBody(rss);
    expect(request.getParameter(MakeRequestHandler.GET_SUMMARIES_PARAM)).andReturn("true");
    expect(request.getParameter(MakeRequestHandler.NUM_ENTRIES_PARAM)).andReturn("2");
    expect(request.getParameter(MakeRequestHandler.CONTENT_TYPE_PARAM)).andReturn("FEED");
    replay();

    handler.fetch(request, recorder);
    JSONObject results = extractJsonFromResponse();

    JSONObject feed = new JSONObject(results.getString("body"));
    JSONArray feeds = feed.getJSONArray("Entry");

    assertEquals("numEntries not parsed correctly.", 2, feeds.length());

    JSONObject entry = feeds.getJSONObject(1);
    assertEquals(entryTitle, entry.getString("Title"));
    assertEquals(entryLink, entry.getString("Link"));
    assertTrue("getSummaries not parsed correctly.", entry.has("Summary"));
    assertEquals(entrySummary, entry.getString("Summary"));
    assertTrue(rewriter.responseWasRewritten());
  }

  @Test
  public void testFetchEmptyDocument() throws Exception {
    expectGetAndReturnBody("");
    replay();

    handler.fetch(request, recorder);
    JSONObject results = extractJsonFromResponse();

    assertEquals(HttpResponse.SC_OK, results.getInt("rc"));
    assertEquals("", results.get("body"));
    assertTrue(rewriter.responseWasRewritten());
  }

  private void expectParameters(HttpServletRequest request, String... params) {
    final List<String> v = Lists.newArrayList(params);

    expect(request.getParameterNames()).andStubAnswer(new IAnswer<Enumeration<String>>() {
      public Enumeration<String> answer() throws Throwable {
        return Collections.enumeration(v);
      }
    });
  }

  @Test
  public void testSignedGetRequest() throws Exception {

    expect(request.getAttribute(AuthInfo.Attribute.SECURITY_TOKEN.getId()))
        .andReturn(DUMMY_TOKEN).atLeastOnce();
    expect(request.getParameter(MakeRequestHandler.AUTHZ_PARAM))
        .andReturn(AuthType.SIGNED.toString()).atLeastOnce();
    HttpRequest expected = new HttpRequest(REQUEST_URL)
        .setAuthType(AuthType.SIGNED);
    expect(pipeline.execute(expected))
        .andReturn(new HttpResponse(RESPONSE_BODY));
    expectParameters(request, MakeRequestHandler.AUTHZ_PARAM);
    replay();

    handler.fetch(request, recorder);
    JSONObject results = extractJsonFromResponse();

    assertEquals(RESPONSE_BODY, results.get("body"));
    assertTrue(rewriter.responseWasRewritten());
  }

  @Test
  public void testSignedPostRequest() throws Exception {
    // Doesn't actually sign since it returns the standard fetcher.
    // Signing tests are in SigningFetcherTest
    expectPostAndReturnBody(AuthType.SIGNED, REQUEST_BODY, RESPONSE_BODY);
    expect(request.getAttribute(AuthInfo.Attribute.SECURITY_TOKEN.getId()))
        .andReturn(DUMMY_TOKEN).atLeastOnce();
    expect(request.getParameter(MakeRequestHandler.AUTHZ_PARAM))
        .andReturn(AuthType.SIGNED.toString()).atLeastOnce();
    expectParameters(request, MakeRequestHandler.METHOD_PARAM, MakeRequestHandler.POST_DATA_PARAM,
        MakeRequestHandler.AUTHZ_PARAM);
    replay();

    handler.fetch(request, recorder);
    JSONObject results = extractJsonFromResponse();

    assertEquals(RESPONSE_BODY, results.get("body"));
    assertFalse("A security token was returned when it was not requested.",
        results.has("st"));
    assertTrue(rewriter.responseWasRewritten());
  }

  @Test
  public void testChangeSecurityToken() throws Exception {
    // Doesn't actually sign since it returns the standard fetcher.
    // Signing tests are in SigningFetcherTest
    expectGetAndReturnBody(AuthType.SIGNED, RESPONSE_BODY);
    FakeGadgetToken authToken = new FakeGadgetToken().setUpdatedToken("updated");
    expect(request.getAttribute(AuthInfo.Attribute.SECURITY_TOKEN.getId()))
        .andReturn(authToken).atLeastOnce();
    expect(request.getParameter(MakeRequestHandler.AUTHZ_PARAM))
        .andReturn(AuthType.SIGNED.toString()).atLeastOnce();
    expectParameters(request, MakeRequestHandler.AUTHZ_PARAM);
    replay();

    handler.fetch(request, recorder);
    JSONObject results = extractJsonFromResponse();

    assertEquals(RESPONSE_BODY, results.get("body"));
    assertEquals("updated", results.getString("st"));
    assertTrue(rewriter.responseWasRewritten());
  }

  @Test
  public void testDoOAuthRequest() throws Exception {
    // Doesn't actually do oauth dance since it returns the standard fetcher.
    // OAuth tests are in OAuthRequestTest
    expectGetAndReturnBody(AuthType.OAUTH, RESPONSE_BODY);
    FakeGadgetToken authToken = new FakeGadgetToken().setUpdatedToken("updated");
    expect(request.getAttribute(AuthInfo.Attribute.SECURITY_TOKEN.getId()))
        .andReturn(authToken).atLeastOnce();
    expect(request.getParameter(MakeRequestHandler.AUTHZ_PARAM))
        .andReturn(AuthType.OAUTH.toString()).atLeastOnce();
    // This isn't terribly accurate, but is close enough for this test.
    expect(request.getParameterMap()).andStubReturn(Collections.emptyMap());
    expectParameters(request);
    replay();

    handler.fetch(request, recorder);
    JSONObject results = extractJsonFromResponse();

    assertEquals(HttpResponse.SC_OK, results.getInt("rc"));
    assertEquals(RESPONSE_BODY, results.get("body"));
    assertTrue(rewriter.responseWasRewritten());
  }

  @Test
  public void testInvalidSigningTypeTreatedAsNone() throws Exception {
    expectGetAndReturnBody(RESPONSE_BODY);
    expect(request.getParameter(MakeRequestHandler.AUTHZ_PARAM)).andReturn("garbage");
    replay();

    handler.fetch(request, recorder);
    JSONObject results = extractJsonFromResponse();

    assertEquals(HttpResponse.SC_OK, results.getInt("rc"));
    assertEquals(RESPONSE_BODY, results.get("body"));
    assertTrue(rewriter.responseWasRewritten());
  }

  @Test
  public void testBadHttpResponseIsPropagated() throws Exception {
    HttpRequest internalRequest = new HttpRequest(REQUEST_URL);
    expect(pipeline.execute(internalRequest)).andReturn(HttpResponse.error());
    replay();

    handler.fetch(request, recorder);
    JSONObject results = extractJsonFromResponse();

    assertEquals(HttpResponse.SC_INTERNAL_SERVER_ERROR, results.getInt("rc"));
    assertTrue(rewriter.responseWasRewritten());
  }

  @Test(expected=GadgetException.class)
  public void testBadSecurityTokenThrows() throws Exception {
    expect(request.getAttribute(AuthInfo.Attribute.SECURITY_TOKEN.getId()))
        .andReturn(null).atLeastOnce();
    expect(request.getParameter(MakeRequestHandler.AUTHZ_PARAM))
        .andReturn(AuthType.SIGNED.toString()).atLeastOnce();
    replay();

    handler.fetch(request, recorder);
  }

  @Test
  public void testMetadataCopied() throws Exception {
    HttpRequest internalRequest = new HttpRequest(REQUEST_URL);
    HttpResponse response = new HttpResponseBuilder()
        .setResponse("foo".getBytes("UTF-8"))
        .setMetadata("foo", RESPONSE_BODY)
        .create();

    expect(pipeline.execute(internalRequest)).andReturn(response);
    replay();

    handler.fetch(request, recorder);
    JSONObject results = extractJsonFromResponse();

    assertEquals(RESPONSE_BODY, results.getString("foo"));
    assertTrue(rewriter.responseWasRewritten());
  }

  @Test
  public void testSetCookiesReturned() throws Exception {
    HttpRequest internalRequest = new HttpRequest(REQUEST_URL);
    HttpResponse response = new HttpResponseBuilder()
        .setResponse("foo".getBytes("UTF-8"))
        .addHeader("Set-Cookie", "foo=bar; Secure")
        .addHeader("Set-Cookie", "name=value")
        .create();

    expect(pipeline.execute(internalRequest)).andReturn(response);
    replay();

    handler.fetch(request, recorder);
    JSONObject results = extractJsonFromResponse();
    JSONObject headers = results.getJSONObject("headers");
    assertNotNull(headers);
    JSONArray cookies = headers.getJSONArray("set-cookie");
    assertNotNull(cookies);
    assertEquals(2, cookies.length());
    assertEquals("foo=bar; Secure", cookies.get(0));
    assertEquals("name=value", cookies.get(1));
  }

  @Test
  public void testLocationReturned() throws Exception {
    HttpRequest internalRequest = new HttpRequest(REQUEST_URL);
    HttpResponse response = new HttpResponseBuilder()
        .setResponse("foo".getBytes("UTF-8"))
        .addHeader("Location", "somewhere else")
        .create();

    expect(pipeline.execute(internalRequest)).andReturn(response);
    replay();

    handler.fetch(request, recorder);
    JSONObject results = extractJsonFromResponse();
    JSONObject headers = results.getJSONObject("headers");
    assertNotNull(headers);
    JSONArray locations = headers.getJSONArray("location");
    assertNotNull(locations);
    assertEquals(1, locations.length());
    assertEquals("somewhere else", locations.get(0));
  }

  @Test
  public void testSetResponseHeaders() throws Exception {
    HttpResponse results = new HttpResponseBuilder().create();
    replay();

    MakeRequestHandler.setResponseHeaders(request, recorder, results);

    // Just verify that they were set. Specific values are configurable.
    assertNotNull("Expires header not set", recorder.getHeader("Expires"));
    assertNotNull("Cache-Control header not set", recorder.getHeader("Cache-Control"));
    assertEquals("attachment;filename=p.txt", recorder.getHeader("Content-Disposition"));
  }

  @Test
  public void testSetContentTypeHeader() throws Exception {
    HttpResponse results = new HttpResponseBuilder()
        .create();
    replay();
    MakeRequestHandler.setResponseHeaders(request, recorder, results);

    assertEquals("application/octet-stream", recorder.getHeader("Content-Type"));
  }

  @Test
  public void testSetResponseHeadersNoCache() throws Exception {
    Map<String, List<String>> headers = Maps.newTreeMap(String.CASE_INSENSITIVE_ORDER);
    headers.put("Pragma", Arrays.asList("no-cache"));
    HttpResponse results = new HttpResponseBuilder()
        .addHeader("Pragma", "no-cache")
        .create();
    replay();

    MakeRequestHandler.setResponseHeaders(request, recorder, results);

    // Just verify that they were set. Specific values are configurable.
    assertNotNull("Expires header not set", recorder.getHeader("Expires"));
    assertEquals("no-cache", recorder.getHeader("Pragma"));
    assertEquals("no-cache", recorder.getHeader("Cache-Control"));
    assertEquals("attachment;filename=p.txt", recorder.getHeader("Content-Disposition"));
  }

  @Test
  public void testSetResponseHeadersForceParam() throws Exception {
    HttpResponse results = new HttpResponseBuilder().create();
    expect(request.getParameter(Param.REFRESH.getKey())).andReturn("30").anyTimes();
    replay();

    // not sure why but the following line seems to help this test past deterministically 
    System.out.println("request started at " + HttpUtilTest.testStartTime);
    MakeRequestHandler.setResponseHeaders(request, recorder, results);
    HttpUtilTest.checkCacheControlHeaders(HttpUtilTest.testStartTime, recorder, 30, false);
    assertEquals("attachment;filename=p.txt", recorder.getHeader("Content-Disposition"));
  }

  @Test
  public void testSetResponseHeadersForceParamInvalid() throws Exception {
    HttpResponse results = new HttpResponseBuilder().create();
    expect(request.getParameter(Param.REFRESH.getKey())).andReturn("foo").anyTimes();
    replay();

    try {
      MakeRequestHandler.setResponseHeaders(request, recorder, results);
    } catch (GadgetException e) {
      assertEquals(GadgetException.Code.INVALID_PARAMETER, e.getCode());
    }
  }

  @Test
  public void testGetParameter() {
    expect(request.getParameter("foo")).andReturn("bar");
    replay();

    assertEquals("bar", MakeRequestHandler.getParameter(request, "foo", "not foo"));
  }

  @Test
  public void testGetParameterWithNullValue() {
    expect(request.getParameter("foo")).andReturn(null);
    replay();

    assertEquals("not foo", MakeRequestHandler.getParameter(request, "foo", "not foo"));
  }

  @Test
  public void testGetContainerWithContainer() {
    expect(request.getParameter(Param.CONTAINER.getKey())).andReturn("bar");
    replay();

    assertEquals("bar", MakeRequestHandler.getContainer(request));
  }

  @SuppressWarnings("deprecation")
  @Test
  public void testGetContainerWithSynd() {
    expect(request.getParameter(Param.CONTAINER.getKey())).andReturn(null);
    expect(request.getParameter(Param.SYND.getKey())).andReturn("syndtainer");
    replay();

    assertEquals("syndtainer", MakeRequestHandler.getContainer(request));
  }

  @SuppressWarnings("deprecation")
  @Test
  public void testGetContainerNoParam() {
    expect(request.getParameter(Param.CONTAINER.getKey())).andReturn(null);
    expect(request.getParameter(Param.SYND.getKey())).andReturn(null);
    replay();

    assertEquals(ContainerConfig.DEFAULT_CONTAINER, MakeRequestHandler.getContainer(request));
  }
}
