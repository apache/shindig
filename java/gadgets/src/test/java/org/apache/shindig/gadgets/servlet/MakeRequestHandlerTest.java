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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.apache.shindig.common.SecurityToken;
import org.apache.shindig.common.SecurityTokenException;
import org.apache.shindig.common.testing.FakeGadgetToken;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.spec.Auth;
import org.apache.shindig.gadgets.spec.Preload;

import com.google.common.collect.Lists;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

/**
 * Tests for MakeRequestHandler.
 */
public class MakeRequestHandlerTest {
  private static final String REQUEST_URL = "http://example.org/file";
  private static final String REQUEST_BODY = "I+am+the+request+body!foo=baz%20la";
  private static final String RESPONSE_BODY = "makeRequest response body";
  private static final SecurityToken DUMMY_TOKEN = new FakeGadgetToken();

  private final ServletTestFixture fixture = new ServletTestFixture();
  private final MakeRequestHandler handler = new MakeRequestHandler(fixture.contentFetcherFactory,
      fixture.securityTokenDecoder, fixture.rewriter);
  private final HttpServletResponseRecorder recorder
      = new HttpServletResponseRecorder(fixture.response);

  private void expectGetAndReturnBody(String response) throws Exception {
    expectGetAndReturnBody(fixture.httpFetcher, response);
  }

  private void expectGetAndReturnBody(HttpFetcher fetcher, String response) throws Exception {
    HttpRequest request = HttpRequest.getRequest(URI.create(REQUEST_URL), false);
    expect(fetcher.fetch(request)).andReturn(new HttpResponse(response));
  }

  private void expectPostAndReturnBody(String postData, String response) throws Exception {
    expectPostAndReturnBody(fixture.httpFetcher, postData, response);
  }

  private void expectPostAndReturnBody(HttpFetcher fetcher, String postData, String response)
      throws Exception {
    HttpRequest request = new HttpRequest(URI.create(REQUEST_URL), REQUEST_BODY.getBytes("UTF-8"));
    expect(fixture.request.getParameter(MakeRequestHandler.METHOD_PARAM)).andReturn("POST");
    expect(fixture.request.getParameter(MakeRequestHandler.POST_DATA_PARAM))
        .andReturn(REQUEST_BODY);
    expect(fetcher.fetch(request)).andReturn(new HttpResponse(response));
  }

  private JSONObject extractJsonFromResponse() throws JSONException {
    String body = recorder.getResponseAsString();
    assertStartsWith(MakeRequestHandler.UNPARSEABLE_CRUFT, body);
    body = body.substring(MakeRequestHandler.UNPARSEABLE_CRUFT.length());
    return new JSONObject(body).getJSONObject(REQUEST_URL);
  }

  @Before
  public void setUp() {
    expect(fixture.request.getMethod()).andReturn("POST").anyTimes();
    expect(fixture.request.getParameter(MakeRequestHandler.URL_PARAM))
        .andReturn(REQUEST_URL).anyTimes();
  }

  @Test
  public void getRequest() throws Exception {
    expectGetAndReturnBody(RESPONSE_BODY);
    fixture.replay();

    handler.fetch(fixture.request, recorder);

    JSONObject results = extractJsonFromResponse();
    assertEquals(HttpResponse.SC_OK, results.getInt("rc"));
    assertEquals(RESPONSE_BODY, results.get("body"));
  }

  @Test
  public void explicitHeaders() throws Exception {
    String headerString = "X-Foo=bar&X-Bar=baz%20foo";

    final List<HttpRequest> requests = Lists.newArrayList();
    HttpFetcher fakeFetcher = new HttpFetcher() {
      public HttpResponse fetch(HttpRequest request) {
        requests.add(request);
        return new HttpResponse(RESPONSE_BODY);
      }
    };

    fixture.reset();
    setUp();
    expect(fixture.contentFetcherFactory.get()).andReturn(fakeFetcher);
    expect(fixture.request.getParameter(MakeRequestHandler.HEADERS_PARAM)).andReturn(headerString);
    fixture.replay();

    handler.fetch(fixture.request, recorder);
    fixture.verify();

    assertEquals("bar", requests.get(0).getHeader("X-Foo"));
    assertEquals("baz foo", requests.get(0).getHeader("X-Bar"));

    JSONObject results = extractJsonFromResponse();
    assertEquals(HttpResponse.SC_OK, results.getInt("rc"));
    assertEquals(RESPONSE_BODY, results.get("body"));
  }

  @Test
  public void postRequest() throws Exception {
    expectPostAndReturnBody(REQUEST_BODY, RESPONSE_BODY);
    expect(fixture.request.getParameter(MakeRequestHandler.METHOD_PARAM)).andReturn("POST");
    fixture.replay();

    handler.fetch(fixture.request, recorder);
    JSONObject results = extractJsonFromResponse();

    assertEquals(HttpResponse.SC_OK, results.getInt("rc"));
    assertEquals(RESPONSE_BODY, results.get("body"));
  }

  @Test
  public void fetchContentTypeFeed() throws Exception {
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
    expect(fixture.request.getParameter(MakeRequestHandler.CONTENT_TYPE_PARAM)).andReturn("FEED");
    fixture.replay();

    handler.fetch(fixture.request, recorder);
    JSONObject results = extractJsonFromResponse();

    JSONObject feed = new JSONObject(results.getString("body"));
    JSONObject entry = feed.getJSONArray("Entry").getJSONObject(0);

    assertEquals(entryTitle, entry.getString("Title"));
    assertEquals(entryLink, entry.getString("Link"));
    assertNull("getSummaries has the wrong default value (should be false).",
        entry.optString("Summary", null));
  }

  @Test
  public void fetchFeedWithParameters() throws Exception {
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
    expect(fixture.request.getParameter(MakeRequestHandler.GET_SUMMARIES_PARAM)).andReturn("true");
    expect(fixture.request.getParameter(MakeRequestHandler.NUM_ENTRIES_PARAM)).andReturn("2");
    expect(fixture.request.getParameter(MakeRequestHandler.CONTENT_TYPE_PARAM)).andReturn("FEED");
    fixture.replay();

    handler.fetch(fixture.request, recorder);
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

  @Test
  public void fetchEmptyDocument() throws Exception {
    expectGetAndReturnBody("");
    fixture.replay();

    handler.fetch(fixture.request, recorder);
    JSONObject results = extractJsonFromResponse();

    assertEquals(HttpResponse.SC_OK, results.getInt("rc"));
    assertEquals("", results.get("body"));
  }

  @Test
  public void signedGetRequest() throws Exception {
    // Doesn't actually sign since it returns the standard fetcher.
    // Signing tests are in SigningFetcherTest
    expect(fixture.securityTokenDecoder.createToken("fake-token")).andReturn(DUMMY_TOKEN);
    expect(fixture.request.getParameter(MakeRequestHandler.SECURITY_TOKEN_PARAM))
        .andReturn("fake-token").atLeastOnce();
    expect(fixture.request.getParameter(Preload.AUTHZ_ATTR))
        .andReturn(Auth.SIGNED.toString()).atLeastOnce();
    expect(fixture.signingFetcher.fetch(isA(HttpRequest.class)))
        .andReturn(new HttpResponse(RESPONSE_BODY));
    fixture.replay();

    handler.fetch(fixture.request, recorder);
    JSONObject results = extractJsonFromResponse();

    assertEquals(RESPONSE_BODY, results.get("body"));
  }

  @Test
  public void signedPostRequest() throws Exception {
    // Doesn't actually sign since it returns the standard fetcher.
    // Signing tests are in SigningFetcherTest
    expectPostAndReturnBody(fixture.signingFetcher, REQUEST_BODY, RESPONSE_BODY);
    expect(fixture.securityTokenDecoder.createToken("fake-token")).andReturn(DUMMY_TOKEN);
    expect(fixture.request.getParameter(MakeRequestHandler.SECURITY_TOKEN_PARAM))
        .andReturn("fake-token").atLeastOnce();
    expect(fixture.request.getParameter(Preload.AUTHZ_ATTR))
        .andReturn(Auth.SIGNED.toString()).atLeastOnce();
    fixture.replay();

    handler.fetch(fixture.request, recorder);
    JSONObject results = extractJsonFromResponse();

    assertEquals(RESPONSE_BODY, results.get("body"));
    assertFalse("A security token was returned when it was not requested.",
        results.has("st"));
  }

  @Test
  public void changeSecurityToken() throws Exception {
    // Doesn't actually sign since it returns the standard fetcher.
    // Signing tests are in SigningFetcherTest
    expectGetAndReturnBody(fixture.signingFetcher, RESPONSE_BODY);
    FakeGadgetToken authToken = new FakeGadgetToken("updated");
    expect(fixture.securityTokenDecoder.createToken("fake-token")).andReturn(authToken);
    expect(fixture.request.getParameter(MakeRequestHandler.SECURITY_TOKEN_PARAM))
        .andReturn("fake-token").atLeastOnce();
    expect(fixture.request.getParameter(Preload.AUTHZ_ATTR))
        .andReturn(Auth.SIGNED.toString()).atLeastOnce();
    fixture.replay();

    handler.fetch(fixture.request, recorder);
    JSONObject results = extractJsonFromResponse();

    assertEquals(RESPONSE_BODY, results.get("body"));
    assertEquals("updated", results.getString("st"));
  }

  @Test
  public void doOAuthRequest() throws Exception {
    // Doesn't actually do oauth dance since it returns the standard fetcher.
    // OAuth tests are in OAuthFetcherTest
    expectGetAndReturnBody(fixture.oauthFetcher, RESPONSE_BODY);
    FakeGadgetToken authToken = new FakeGadgetToken("updated");
    expect(fixture.securityTokenDecoder.createToken("fake-token")).andReturn(authToken);
    expect(fixture.request.getParameter(MakeRequestHandler.SECURITY_TOKEN_PARAM))
        .andReturn("fake-token").atLeastOnce();
    expect(fixture.request.getParameter(Preload.AUTHZ_ATTR))
        .andReturn(Auth.AUTHENTICATED.toString()).atLeastOnce();
    fixture.replay();

    handler.fetch(fixture.request, recorder);
    JSONObject results = extractJsonFromResponse();

    assertEquals(HttpResponse.SC_OK, results.getInt("rc"));
    assertEquals(RESPONSE_BODY, results.get("body"));
  }

  @Test
  public void invalidSigningTypeTreatedAsNone() throws Exception {
    expectGetAndReturnBody(RESPONSE_BODY);
    expect(fixture.request.getParameter(Preload.AUTHZ_ATTR)).andReturn("garbage");
    fixture.replay();

    handler.fetch(fixture.request, recorder);
    JSONObject results = extractJsonFromResponse();

    assertEquals(HttpResponse.SC_OK, results.getInt("rc"));
    assertEquals(RESPONSE_BODY, results.get("body"));
  }

  @Test
  public void badHttpResponseIsPropagated() throws Exception {
    HttpRequest request = HttpRequest.getRequest(URI.create(REQUEST_URL), false);
    expect(fixture.httpFetcher.fetch(request)).andReturn(HttpResponse.error());
    fixture.replay();

    handler.fetch(fixture.request, recorder);
    JSONObject results = extractJsonFromResponse();

    assertEquals(HttpResponse.SC_INTERNAL_SERVER_ERROR, results.getInt("rc"));
  }

  @Test(expected = GadgetException.class)
  public void badSecurityTokenThrows() throws Exception {
    expect(fixture.request.getParameter(MakeRequestHandler.SECURITY_TOKEN_PARAM))
        .andReturn("fake-token").atLeastOnce();
    expect(fixture.request.getParameter(Preload.AUTHZ_ATTR))
        .andReturn(Auth.SIGNED.toString()).atLeastOnce();
    expect(fixture.securityTokenDecoder.createToken("fake-token"))
        .andThrow(new SecurityTokenException("No!"));
    fixture.replay();

    handler.fetch(fixture.request, recorder);

    assertTrue("Response for bad tokens should not be SC_OK",
        HttpServletResponse.SC_OK != recorder.getHttpStatusCode());
  }

  @Test
  public void metadataCopied() throws Exception {
    HttpRequest request = HttpRequest.getRequest(URI.create(REQUEST_URL), false);
    HttpResponse response = new HttpResponse("foo");
    response.getMetadata().put("foo", RESPONSE_BODY);
    expect(fixture.httpFetcher.fetch(request)).andReturn(response);
    fixture.replay();

    handler.fetch(fixture.request, recorder);
    JSONObject results = extractJsonFromResponse();

    assertEquals(RESPONSE_BODY, results.getString("foo"));
  }
}
