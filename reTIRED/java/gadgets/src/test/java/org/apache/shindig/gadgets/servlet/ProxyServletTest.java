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
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;

import org.junit.Before;
import org.junit.Test;

import java.net.URI;

import javax.servlet.http.HttpServletResponse;

/**
 * Tests for ProxyServlet.
 *
 * Tests are trivial; real tests are in ProxyHandlerTest.
 */
public class ProxyServletTest {
  private static final String REQUEST_DOMAIN = "example.org";
  private static final String REQUEST_URL = "http://example.org/file";
  private static final String BASIC_SYNTAX_URL
      = "http://opensocial.org/proxy?foo=bar&url=" + REQUEST_URL;
  private static final String ALT_SYNTAX_URL
      = "http://opensocial.org/proxy/foo=bar/" + REQUEST_URL;
  private static final String RESPONSE_BODY = "Hello, world!";
  private static final String ERROR_MESSAGE = "Broken!";

  private final ServletTestFixture fixture = new ServletTestFixture();
  private final ProxyHandler proxyHandler
      = new ProxyHandler(fixture.httpFetcher, fixture.lockedDomainService, fixture.rewriter);
  private final ProxyServlet servlet = new ProxyServlet();
  private final HttpRequest request = HttpRequest.getRequest(URI.create(REQUEST_URL), false);
  private final HttpResponse response = new HttpResponse(RESPONSE_BODY);

  @Before
  public void setUp() {
    servlet.setProxyHandler(proxyHandler);
    expect(fixture.request.getParameter(ProxyBase.URL_PARAM))
        .andReturn(REQUEST_URL).anyTimes();
    expect(fixture.request.getHeader("Host")).andReturn(REQUEST_DOMAIN).anyTimes();
    expect(fixture.lockedDomainService.embedCanRender(REQUEST_DOMAIN))
        .andReturn(true).anyTimes();
  }

  private void setupBasic() {
    expect(fixture.request.getRequestURI()).andReturn(BASIC_SYNTAX_URL);
  }

  private void setupAltSyntax() {
    expect(fixture.request.getRequestURI()).andReturn(ALT_SYNTAX_URL);
  }

  private void assertResponseOk(int expectedStatus, String expectedBody) {
      assertEquals(expectedStatus, fixture.recorder.getHttpStatusCode());
      assertEquals(expectedBody, fixture.recorder.getResponseAsString());
  }

  @Test
  public void doGetNormal() throws Exception {
    setupBasic();
    expect(fixture.httpFetcher.fetch(request)).andReturn(response);
    fixture.replay();

    servlet.doGet(fixture.request, fixture.recorder);

    assertResponseOk(HttpResponse.SC_OK, RESPONSE_BODY);
  }

  @Test
  public void doGetHttpError() throws Exception {
    setupBasic();
    expect(fixture.httpFetcher.fetch(request)).andReturn(HttpResponse.notFound());
    fixture.replay();

    servlet.doGet(fixture.request, fixture.recorder);

    assertResponseOk(HttpResponse.SC_NOT_FOUND, "");
  }

  @Test
  public void doGetException() throws Exception {
    setupBasic();
    expect(fixture.httpFetcher.fetch(request)).andThrow(
        new GadgetException(GadgetException.Code.FAILED_TO_RETRIEVE_CONTENT, ERROR_MESSAGE));
    fixture.replay();

    servlet.doGet(fixture.request, fixture.recorder);

    assertEquals(HttpServletResponse.SC_BAD_REQUEST, fixture.recorder.getHttpStatusCode());
    assertContains(ERROR_MESSAGE, fixture.recorder.getResponseAsString());
  }

  @Test
  public void doGetAlternateSyntax() throws Exception {
    setupAltSyntax();
    expect(fixture.request.getRequestURI()).andReturn(ALT_SYNTAX_URL);
    expect(fixture.httpFetcher.fetch(request)).andReturn(response);
    fixture.replay();

    servlet.doGet(fixture.request, fixture.recorder);

    assertResponseOk(HttpResponse.SC_OK, RESPONSE_BODY);
  }
}
