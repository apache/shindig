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

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.AuthType;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.LockedDomainService;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.oauth.OAuthArguments;
import org.apache.shindig.gadgets.oauth2.OAuth2Arguments;
import org.apache.shindig.gadgets.uri.ProxyUriManager;
import org.apache.shindig.gadgets.uri.ProxyUriManager.ProxyUri;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletResponse;

/**
 * Tests for ProxyServlet.
 *
 * Tests are trivial; real tests are in ProxyHandlerTest.
 */
public class ProxyServletTest extends ServletTestFixture {
  private static final Uri REQUEST_URL = Uri.parse("http://example.org/file");
  private static final String BASIC_SYNTAX_URL = "http://opensocial.org/proxy?foo=bar&url="
          + REQUEST_URL;
  private static final String RESPONSE_BODY = "Hello, world!";
  private static final String ERROR_MESSAGE = "Broken!";
  private static final String POST_CONTENT = "my post stuff";
  private static final String POST_METHOD = "POST";

  private ServletInputStream postContentStream = new ServletInputStream() {
    InputStream is = new ByteArrayInputStream(POST_CONTENT.getBytes());
    @Override
    public int read() throws IOException {
      return is.read();
    }

    @Override
    public void close() throws IOException {
      is.close();
    }

  };

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

  @Test
  public void testDoPostNormal() throws Exception {
    setupRequest(BASIC_SYNTAX_URL);
    expect(request.getInputStream()).andReturn(postContentStream);
    expect(request.getMethod()).andReturn(POST_METHOD);
    expect(proxyHandler.fetch(proxyUri, POST_CONTENT)).andReturn(new HttpResponse(RESPONSE_BODY));

    replay();
    servlet.doPost(request, recorder);
    verify();

    assertResponseOk(HttpResponse.SC_OK, RESPONSE_BODY);
  }

  @Test
  public void testDoPostHttpError() throws Exception {
    setupRequest(BASIC_SYNTAX_URL);
    expect(proxyHandler.fetch(proxyUri, POST_CONTENT)).andReturn(HttpResponse.notFound());
    expect(request.getMethod()).andReturn(POST_METHOD);
    expect(request.getInputStream()).andReturn(postContentStream);

    replay();
    servlet.doPost(request, recorder);
    verify();

    assertResponseOk(HttpResponse.SC_NOT_FOUND, "");
  }

  @Test
  public void testDoPostException() throws Exception {
    setupRequest(BASIC_SYNTAX_URL);
    expect(request.getInputStream()).andReturn(postContentStream);
    expect(request.getMethod()).andReturn(POST_METHOD);
    expect(proxyHandler.fetch(proxyUri, POST_CONTENT)).andThrow(
            new GadgetException(GadgetException.Code.FAILED_TO_RETRIEVE_CONTENT, ERROR_MESSAGE));

    replay();
    servlet.doPost(request, recorder);
    verify();

    assertEquals(HttpServletResponse.SC_BAD_REQUEST, recorder.getHttpStatusCode());
    assertContains(ERROR_MESSAGE, recorder.getResponseAsString());
  }

  @Test
  public void testDoPostNormalWithLockedDomainUnsafe() throws Exception {
    setupRequest(BASIC_SYNTAX_URL, false);

    replay();
    servlet.doGet(request, recorder);
    verify();

    assertEquals(HttpServletResponse.SC_BAD_REQUEST, recorder.getHttpStatusCode());
    assertContains("wrong domain", recorder.getResponseAsString());
  }

  @Test
  public void testDoGetWithOAuth2() throws Exception {
    Map<String, String> options = new HashMap<String, String>();
    options.put("OAUTH_SERVICE_NAME", "example");
    ProxyUriManager.ProxyUri proxyUri = new ProxyUri(-1, false, true, "default", "http://example.org/gadget.xml", REQUEST_URL);
    proxyUri.setAuthType(AuthType.OAUTH2);

    Uri uri = Uri.parse(BASIC_SYNTAX_URL + "&authz=oauth2&OAUTH_SERVICE_NAME=example&container=default&gadget=http://example.org/gadget.xml");
    expect(proxyUriManager.process(uri)).andReturn(proxyUri);
    expect(request.getScheme()).andReturn(uri.getScheme());
    expect(request.getServerName()).andReturn(uri.getAuthority());
    expect(request.getServerPort()).andReturn(80);
    expect(request.getRequestURI()).andReturn(uri.getPath());
    expect(request.getQueryString()).andReturn(uri.getQuery());
    expect(request.getHeader("Host")).andReturn(uri.getAuthority());
    expect(request.getParameter("OAUTH_SERVICE_NAME")).andReturn("example");
    expect(request.getParameterNames()).andReturn(Collections.enumeration(options.keySet()));
    expect(lockedDomainService.isSafeForOpenProxy(uri.getAuthority())).andReturn(true);

    ProxyUriManager.ProxyUri pUri = new ProxyUri(-1, false, true, "default", "http://example.org/gadget.xml", REQUEST_URL);
    pUri.setAuthType(AuthType.OAUTH2);
    pUri.setOAuth2Arguments(new OAuth2Arguments(AuthType.OAUTH2, options));

    expect(proxyHandler.fetch(pUri)).andReturn(new HttpResponse(RESPONSE_BODY));
    replay();
    servlet.doGet(request, recorder);
    verify();
    assertResponseOk(HttpResponse.SC_OK, RESPONSE_BODY);
  }

  @Test
  public void testDoGetWithOAuth() throws Exception {
    Map<String, String> options = new HashMap<String, String>();
    options.put("OAUTH_SERVICE_NAME", "example");
    ProxyUriManager.ProxyUri proxyUri = new ProxyUri(-1, false, true, "default", "http://example.org/gadget.xml", REQUEST_URL);
    proxyUri.setAuthType(AuthType.OAUTH);

    Uri uri = Uri.parse(BASIC_SYNTAX_URL + "&authz=oauth&OAUTH_SERVICE_NAME=example&container=default&gadget=http://example.org/gadget.xml");
    expect(proxyUriManager.process(uri)).andReturn(proxyUri);
    expect(request.getScheme()).andReturn(uri.getScheme());
    expect(request.getServerName()).andReturn(uri.getAuthority());
    expect(request.getServerPort()).andReturn(80);
    expect(request.getRequestURI()).andReturn(uri.getPath());
    expect(request.getQueryString()).andReturn(uri.getQuery());
    expect(request.getHeader("Host")).andReturn(uri.getAuthority());
    expect(request.getParameter("OAUTH_SERVICE_NAME")).andReturn("example");
    expect(request.getParameterNames()).andReturn(Collections.enumeration(options.keySet()));
    expect(lockedDomainService.isSafeForOpenProxy(uri.getAuthority())).andReturn(true);

    ProxyUriManager.ProxyUri pUri = new ProxyUri(-1, false, true, "default", "http://example.org/gadget.xml", REQUEST_URL);
    pUri.setAuthType(AuthType.OAUTH);
    pUri.setOAuthArguments(new OAuthArguments(AuthType.OAUTH, options));

    expect(proxyHandler.fetch(pUri)).andReturn(new HttpResponse(RESPONSE_BODY));
    replay();
    servlet.doGet(request, recorder);
    verify();
    assertResponseOk(HttpResponse.SC_OK, RESPONSE_BODY);
  }
}
