/*
pro * Licensed to the Apache Software Foundation (ASF) under one
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

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.LockedDomainService;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.uri.ProxyUriManager;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletResponse;

/**
 * Tests for ProxyServlet.
 *
 * Tests are trivial; real tests are in ProxyHandlerTest.
 */
public class ProxyServletTest extends ServletTestFixture {
  private static final Uri REQUEST_URL = Uri.parse("http://example.org/file");
  private static final String BASIC_SYNTAX_URL
      = "http://opensocial.org/proxy?foo=bar&url=" + REQUEST_URL;
  private static final String RESPONSE_BODY = "Hello, world!";
  private static final String ERROR_MESSAGE = "Broken!";

  private final ProxyUriManager proxyUriManager = mock(ProxyUriManager.class);
  private final LockedDomainService lockedDomainService = mock(LockedDomainService.class);
  private final ProxyHandler proxyHandler = mock(ProxyHandler.class);
  private final ProxyServlet servlet = new ProxyServlet();
  private final ProxyUriManager.ProxyUri proxyUri = mock(ProxyUriManager.ProxyUri.class);

  @Before
  public void setUp() throws Exception {
    servlet.setProxyHandler(proxyHandler);
    servlet.setProxyUriManager(proxyUriManager);
    servlet.setLockedDomainService(lockedDomainService);
  }

  private void setupRequest(String str) throws Exception {
    setupRequest(str, true);
  }

  private void setupRequest(String str, boolean ldSafe) throws Exception {
    Uri uri = Uri.parse(str);
    expect(request.getScheme()).andReturn(uri.getScheme());
    expect(request.getServerName()).andReturn(uri.getAuthority());
    expect(request.getServerPort()).andReturn(80);
    expect(request.getRequestURI()).andReturn(uri.getPath());
    expect(request.getQueryString()).andReturn(uri.getQuery());
    expect(request.getHeader("Host")).andReturn(uri.getAuthority());
    expect(proxyUriManager.process(uri)).andReturn(proxyUri);
    expect(lockedDomainService.isSafeForOpenProxy(uri.getAuthority())).andReturn(ldSafe);
  }

  private void assertResponseOk(int expectedStatus, String expectedBody) {
    assertEquals(expectedStatus, recorder.getHttpStatusCode());
    assertEquals(expectedBody, recorder.getResponseAsString());
  }

  @Test
  public void testIfModifiedSinceAlwaysReturnsEarly() throws Exception {
    expect(request.getHeader("If-Modified-Since")).andReturn("Yes, this is an invalid header");
   
    replay();
    servlet.doGet(request, recorder);
    verify();

    assertEquals(HttpServletResponse.SC_NOT_MODIFIED, recorder.getHttpStatusCode());
    assertFalse(rewriter.responseWasRewritten());
  }

  @Test
  public void testDoGetNormal() throws Exception {
    setupRequest(BASIC_SYNTAX_URL);
    expect(proxyHandler.fetch(proxyUri)).andReturn(new HttpResponse(RESPONSE_BODY));
    
    replay();
    servlet.doGet(request, recorder);
    verify();

    assertResponseOk(HttpResponse.SC_OK, RESPONSE_BODY);
  }

  @Test
  public void testDoGetHttpError() throws Exception {
    setupRequest(BASIC_SYNTAX_URL);
    expect(proxyHandler.fetch(proxyUri)).andReturn(HttpResponse.notFound());
    
    replay();
    servlet.doGet(request, recorder);
    verify();

    assertResponseOk(HttpResponse.SC_NOT_FOUND, "");
  }

  @Test
  public void testDoGetException() throws Exception {
    setupRequest(BASIC_SYNTAX_URL);
    expect(proxyHandler.fetch(proxyUri)).andThrow(
        new GadgetException(GadgetException.Code.FAILED_TO_RETRIEVE_CONTENT, ERROR_MESSAGE));
   
    replay();
    servlet.doGet(request, recorder);
    verify();

    assertEquals(HttpServletResponse.SC_BAD_REQUEST, recorder.getHttpStatusCode());
    assertContains(ERROR_MESSAGE, recorder.getResponseAsString());
  }
  
  @Test
  public void testDoGetNormalWithLockedDomainUnsafe() throws Exception {
    setupRequest(BASIC_SYNTAX_URL, false);
    
    replay();
    servlet.doGet(request, recorder);
    verify();
    
    assertEquals(HttpServletResponse.SC_BAD_REQUEST, recorder.getHttpStatusCode());
    assertContains("wrong domain", recorder.getResponseAsString());
  }
}
