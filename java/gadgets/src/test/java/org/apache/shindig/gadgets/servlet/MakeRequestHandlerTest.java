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
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;

import org.apache.shindig.auth.AuthInfo;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.testing.FakeGadgetToken;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.apache.shindig.gadgets.spec.Auth;

import com.google.common.collect.Lists;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

/**
 * Tests for MakeRequestHandler.
 */
public class MakeRequestHandlerTest extends ServletTestFixture {
  private static final Uri REQUEST_URL = Uri.parse("http://example.org/file");
  private static final String REQUEST_BODY = "I+am+the+request+body!foo=baz%20la";
  private static final String RESPONSE_BODY = "makeRequest response body";
  private static final SecurityToken DUMMY_TOKEN = new FakeGadgetToken();

  private final MakeRequestHandler handler = new MakeRequestHandler(contentFetcherFactory);

  private void expectGetAndReturnBody(String response) throws Exception {
    expectGetAndReturnBody(fetcher, response);
  }

  private void expectGetAndReturnBody(HttpFetcher fetcher, String response) throws Exception {
    HttpRequest request = new HttpRequest(REQUEST_URL);
    expect(fetcher.fetch(request)).andReturn(new HttpResponse(response));
  }

  private void expectPostAndReturnBody(String postData, String response) throws Exception {
    expectPostAndReturnBody(fetcher, postData, response);
  }

  private void expectPostAndReturnBody(HttpFetcher fetcher, String postData, String response)
      throws Exception {
    HttpRequest req = new HttpRequest(REQUEST_URL).setMethod("POST")
        .setPostBody(REQUEST_BODY.getBytes("UTF-8"));
    expect(fetcher.fetch(req)).andReturn(new HttpResponse(response));
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

  @Override
  public void setUp() {
    expect(request.getMethod()).andReturn("POST").anyTimes();
    expect(request.getParameter(MakeRequestHandler.URL_PARAM))
        .andReturn(REQUEST_URL.toString()).anyTimes();
  }

  public void testGetRequest() throws Exception {
    expectGetAndReturnBody(RESPONSE_BODY);
    replay();

    handler.fetch(request, recorder);

    JSONObject results = extractJsonFromResponse();
    assertEquals(HttpResponse.SC_OK, results.getInt("rc"));
    assertEquals(RESPONSE_BODY, results.get("body"));
  }

  public void testExplicitHeaders() throws Exception {
    String headerString = "X-Foo=bar&X-Bar=baz%20foo";

    final List<HttpRequest> requests = Lists.newArrayList();
    HttpFetcher fakeFetcher = new HttpFetcher() {
      public HttpResponse fetch(HttpRequest request) {
        requests.add(request);
        return new HttpResponse(RESPONSE_BODY);
      }
    };

    reset();
    setUp();
    expect(contentFetcherFactory.get()).andReturn(fakeFetcher);
    expect(request.getParameter(MakeRequestHandler.HEADERS_PARAM)).andReturn(headerString);
    replay();

    handler.fetch(request, recorder);
    verify();

    assertEquals("bar", requests.get(0).getHeader("X-Foo"));
    assertEquals("baz foo", requests.get(0).getHeader("X-Bar"));

    JSONObject results = extractJsonFromResponse();
    assertEquals(HttpResponse.SC_OK, results.getInt("rc"));
    assertEquals(RESPONSE_BODY, results.get("body"));
  }

  public void testPostRequest() throws Exception {
    expect(request.getParameter(MakeRequestHandler.METHOD_PARAM)).andReturn("POST");
    expectPostAndReturnBody(REQUEST_BODY, RESPONSE_BODY);
    replay();

    handler.fetch(request, recorder);
    JSONObject results = extractJsonFromResponse();

    assertEquals(HttpResponse.SC_OK, results.getInt("rc"));
    assertEquals(RESPONSE_BODY, results.get("body"));
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
  }

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
  }

  public void testFetchEmptyDocument() throws Exception {
    expectGetAndReturnBody("");
    replay();

    handler.fetch(request, recorder);
    JSONObject results = extractJsonFromResponse();

    assertEquals(HttpResponse.SC_OK, results.getInt("rc"));
    assertEquals("", results.get("body"));
  }

  public void testSignedGetRequest() throws Exception {

    expect(request.getAttribute(AuthInfo.Attribute.SECURITY_TOKEN.getId()))
        .andReturn(DUMMY_TOKEN).atLeastOnce();
    expect(request.getParameter(MakeRequestHandler.AUTHZ_PARAM))
        .andReturn(Auth.SIGNED.toString()).atLeastOnce();
    expect(signingFetcher.fetch(isA(HttpRequest.class)))
        .andReturn(new HttpResponse(RESPONSE_BODY));
    replay();

    handler.fetch(request, recorder);
    JSONObject results = extractJsonFromResponse();

    assertEquals(RESPONSE_BODY, results.get("body"));
  }

  public void testSignedPostRequest() throws Exception {
    // Doesn't actually sign since it returns the standard fetcher.
    // Signing tests are in SigningFetcherTest
    expectPostAndReturnBody(signingFetcher, REQUEST_BODY, RESPONSE_BODY);
    expect(request.getAttribute(AuthInfo.Attribute.SECURITY_TOKEN.getId()))
        .andReturn(DUMMY_TOKEN).atLeastOnce();
    expect(request.getParameter(MakeRequestHandler.AUTHZ_PARAM))
        .andReturn(Auth.SIGNED.toString()).atLeastOnce();
    replay();

    handler.fetch(request, recorder);
    JSONObject results = extractJsonFromResponse();

    assertEquals(RESPONSE_BODY, results.get("body"));
    assertFalse("A security token was returned when it was not requested.",
        results.has("st"));
  }

  public void testChangeSecurityToken() throws Exception {
    // Doesn't actually sign since it returns the standard fetcher.
    // Signing tests are in SigningFetcherTest
    expectGetAndReturnBody(signingFetcher, RESPONSE_BODY);
    FakeGadgetToken authToken = new FakeGadgetToken().setUpdatedToken("updated");
    expect(request.getAttribute(AuthInfo.Attribute.SECURITY_TOKEN.getId()))
        .andReturn(authToken).atLeastOnce();
    expect(request.getParameter(MakeRequestHandler.AUTHZ_PARAM))
        .andReturn(Auth.SIGNED.toString()).atLeastOnce();
    replay();

    handler.fetch(request, recorder);
    JSONObject results = extractJsonFromResponse();

    assertEquals(RESPONSE_BODY, results.get("body"));
    assertEquals("updated", results.getString("st"));
  }

  public void testDoOAuthRequest() throws Exception {
    // Doesn't actually do oauth dance since it returns the standard fetcher.
    // OAuth tests are in OAuthFetcherTest
    expectGetAndReturnBody(oauthFetcher, RESPONSE_BODY);
    FakeGadgetToken authToken = new FakeGadgetToken().setUpdatedToken("updated");
    expect(request.getAttribute(AuthInfo.Attribute.SECURITY_TOKEN.getId()))
        .andReturn(authToken).atLeastOnce();
    expect(request.getParameter(MakeRequestHandler.AUTHZ_PARAM))
        .andReturn(Auth.OAUTH.toString()).atLeastOnce();
    // This isn't terribly accurate, but is close enough for this test.
    expect(request.getParameterMap()).andStubReturn(Collections.EMPTY_MAP);
    replay();

    handler.fetch(request, recorder);
    JSONObject results = extractJsonFromResponse();

    assertEquals(HttpResponse.SC_OK, results.getInt("rc"));
    assertEquals(RESPONSE_BODY, results.get("body"));
  }

  public void testInvalidSigningTypeTreatedAsNone() throws Exception {
    expectGetAndReturnBody(RESPONSE_BODY);
    expect(request.getParameter(MakeRequestHandler.AUTHZ_PARAM)).andReturn("garbage");
    replay();

    handler.fetch(request, recorder);
    JSONObject results = extractJsonFromResponse();

    assertEquals(HttpResponse.SC_OK, results.getInt("rc"));
    assertEquals(RESPONSE_BODY, results.get("body"));
  }

  public void testBadHttpResponseIsPropagated() throws Exception {
    HttpRequest internalRequest = new HttpRequest(REQUEST_URL);
    expect(fetcher.fetch(internalRequest)).andReturn(HttpResponse.error());
    replay();

    handler.fetch(request, recorder);
    JSONObject results = extractJsonFromResponse();

    assertEquals(HttpResponse.SC_INTERNAL_SERVER_ERROR, results.getInt("rc"));
  }

  public void testBadSecurityTokenThrows() throws Exception {
    expect(request.getAttribute(AuthInfo.Attribute.SECURITY_TOKEN.getId()))
        .andReturn(null).atLeastOnce();
    expect(request.getParameter(MakeRequestHandler.AUTHZ_PARAM))
        .andReturn(Auth.SIGNED.toString()).atLeastOnce();
    replay();

    try {
      handler.fetch(request, recorder);
      fail("Should have thrown");
    } catch (GadgetException e) {
      // good.
    }
  }

  public void testMetadataCopied() throws Exception {
    HttpRequest internalRequest = new HttpRequest(REQUEST_URL);
    HttpResponse response = new HttpResponseBuilder()
        .setResponse("foo".getBytes("UTF-8"))
        .setMetadata("foo", RESPONSE_BODY)
        .create();

    expect(fetcher.fetch(internalRequest)).andReturn(response);
    replay();

    handler.fetch(request, recorder);
    JSONObject results = extractJsonFromResponse();

    assertEquals(RESPONSE_BODY, results.getString("foo"));
  }
}
