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

import static junitx.framework.StringAssert.assertContains;
import static junitx.framework.StringAssert.assertStartsWith;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.Collections;
import java.util.Enumeration;

import javax.servlet.http.HttpServletResponse;

/**
 * Tests for MakeRequestServlet.
 *
 * Tests are trivial; real tests are in MakeRequestHandlerTest.
 */
public class MakeRequestServletTest {
  private static final String REQUEST_URL = "http://example.org/file";
  private static final String RESPONSE_BODY = "Hello, world!";
  private static final String ERROR_MESSAGE = "Broken!";
  private static final Enumeration<String> EMPTY_ENUM
      = Collections.enumeration(Collections.<String>emptyList());

  private final ServletTestFixture fixture = new ServletTestFixture();
  private final MakeRequestServlet servlet = new MakeRequestServlet();
  private final MakeRequestHandler handler = new MakeRequestHandler(fixture.contentFetcherFactory,
      fixture.securityTokenDecoder, fixture.rewriter);
  private final HttpServletResponseRecorder recorder
      = new HttpServletResponseRecorder(fixture.response);
  private final HttpRequest request = HttpRequest.getRequest(URI.create(REQUEST_URL), false);
  private final HttpResponse response = new HttpResponse(RESPONSE_BODY);

  @Before
  public void setUp() {
    servlet.setMakeRequestHandler(handler);
    expect(fixture.request.getHeaderNames()).andReturn(EMPTY_ENUM).anyTimes();
    expect(fixture.request.getParameter(MakeRequestHandler.METHOD_PARAM))
        .andReturn("GET").anyTimes();
    expect(fixture.request.getParameter(ProxyBase.URL_PARAM)).andReturn(REQUEST_URL).anyTimes();
  }

  private void setupGet() {
    expect(fixture.request.getMethod()).andReturn("GET").anyTimes();
  }

  private void setupPost() {
    expect(fixture.request.getMethod()).andReturn("POST").anyTimes();
  }

  private void assertResponseOk(int expectedStatus, String expectedBody) throws JSONException {
    if (recorder.getHttpStatusCode() == HttpServletResponse.SC_OK) {
      String body = recorder.getResponseAsString();
      assertStartsWith(MakeRequestHandler.UNPARSEABLE_CRUFT, body);
      body = body.substring(MakeRequestHandler.UNPARSEABLE_CRUFT.length());
      JSONObject object = new JSONObject(body);
      object = object.getJSONObject(REQUEST_URL);
      assertEquals(expectedStatus, object.getInt("rc"));
      assertEquals(expectedBody, object.getString("body"));
    } else {
      fail("Invalid response for request.");
    }
  }

  @Test
  public void doGetNormal() throws Exception {
    setupGet();
    expect(fixture.httpFetcher.fetch(request)).andReturn(response);
    fixture.replay();

    servlet.doGet(fixture.request, recorder);

    assertResponseOk(HttpResponse.SC_OK, RESPONSE_BODY);
  }

  @Test
  public void doGetHttpError() throws Exception {
    setupGet();
    expect(fixture.httpFetcher.fetch(request)).andReturn(HttpResponse.notFound());
    fixture.replay();

    servlet.doGet(fixture.request, recorder);

    assertResponseOk(HttpResponse.SC_NOT_FOUND, "");
  }

  @Test
  public void doGetException() throws Exception {
    setupGet();
    expect(fixture.httpFetcher.fetch(request)).andThrow(
        new GadgetException(GadgetException.Code.FAILED_TO_RETRIEVE_CONTENT, ERROR_MESSAGE));
    fixture.replay();

    servlet.doGet(fixture.request, recorder);

    assertEquals(HttpServletResponse.SC_BAD_REQUEST, recorder.getHttpStatusCode());
    assertContains(ERROR_MESSAGE, recorder.getResponseAsString());
  }

  @Test
  public void doPostNormal() throws Exception {
    setupPost();
    expect(fixture.httpFetcher.fetch(request)).andReturn(response);
    fixture.replay();

    servlet.doPost(fixture.request, recorder);

    assertResponseOk(HttpResponse.SC_OK, RESPONSE_BODY);
  }

  @Test
  public void doPostHttpError() throws Exception {
    setupPost();
    expect(fixture.httpFetcher.fetch(request)).andReturn(HttpResponse.notFound());
    fixture.replay();

    servlet.doGet(fixture.request, recorder);

    assertResponseOk(HttpResponse.SC_NOT_FOUND, "");
  }

  @Test
  public void doPostException() throws Exception {
    setupPost();
    expect(fixture.httpFetcher.fetch(request)).andThrow(
        new GadgetException(GadgetException.Code.FAILED_TO_RETRIEVE_CONTENT, ERROR_MESSAGE));
    fixture.replay();

    servlet.doPost(fixture.request, recorder);

    assertEquals(HttpServletResponse.SC_BAD_REQUEST, recorder.getHttpStatusCode());
    assertContains(ERROR_MESSAGE, recorder.getResponseAsString());
  }
}
