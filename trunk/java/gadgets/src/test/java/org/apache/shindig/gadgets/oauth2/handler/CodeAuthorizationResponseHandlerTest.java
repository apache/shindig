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
package org.apache.shindig.gadgets.oauth2.handler;

import com.google.inject.Provider;

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.oauth2.MockUtils;
import org.apache.shindig.gadgets.oauth2.OAuth2Accessor;
import org.apache.shindig.gadgets.oauth2.OAuth2Error;
import org.apache.shindig.gadgets.oauth2.OAuth2Message;
import org.apache.shindig.gadgets.oauth2.OAuth2Store;
import org.apache.shindig.gadgets.oauth2.OAuth2Token;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public class CodeAuthorizationResponseHandlerTest extends MockUtils {

  private static CodeAuthorizationResponseHandler carh;
  private static OAuth2Store store;

  @Before
  public void setUp() throws Exception {
    CodeAuthorizationResponseHandlerTest.store = MockUtils.getDummyStore();
    CodeAuthorizationResponseHandlerTest.carh = new CodeAuthorizationResponseHandler(
        MockUtils.getDummyMessageProvider(), MockUtils.getDummyClientAuthHandlers(),
        MockUtils.getDummyTokenEndpointResponseHandlers(), MockUtils.getDummyFetcher());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testCodeAuthorizationResponseHandler_1() throws Exception {
    final Provider<OAuth2Message> oauth2MessageProvider = EasyMock.createMock(Provider.class);
    final List<ClientAuthenticationHandler> clientAuthenticationHandlers = EasyMock
        .createMock(List.class);
    final List<TokenEndpointResponseHandler> tokenEndpointResponseHandlers = EasyMock
        .createMock(List.class);
    final HttpFetcher fetcher = EasyMock.createMock(HttpFetcher.class);

    EasyMock.replay(oauth2MessageProvider);
    EasyMock.replay(clientAuthenticationHandlers);
    EasyMock.replay(tokenEndpointResponseHandlers);
    EasyMock.replay(fetcher);

    final CodeAuthorizationResponseHandler result = new CodeAuthorizationResponseHandler(
        oauth2MessageProvider, clientAuthenticationHandlers, tokenEndpointResponseHandlers, fetcher);

    EasyMock.verify(oauth2MessageProvider);
    EasyMock.verify(clientAuthenticationHandlers);
    EasyMock.verify(tokenEndpointResponseHandlers);
    EasyMock.verify(fetcher);
    Assert.assertNotNull(result);
    Assert.assertTrue(AuthorizationEndpointResponseHandler.class.isInstance(result));
  }

  @Test
  public void testHandleRequest_1() throws Exception {
    final CodeAuthorizationResponseHandler fixture = CodeAuthorizationResponseHandlerTest.carh;
    final OAuth2Accessor accessor = MockUtils.getOAuth2Accessor_Redirecting();
    final HttpServletRequest request = null;

    final OAuth2HandlerError result = fixture.handleRequest(accessor, request);

    Assert.assertNotNull(result);
    Assert.assertEquals(OAuth2Error.AUTHORIZATION_CODE_PROBLEM, result.getError());
    Assert.assertEquals("request is null", result.getContextMessage());
  }

  @Test
  public void testHandleRequest_2() throws Exception {
    final CodeAuthorizationResponseHandler fixture = CodeAuthorizationResponseHandlerTest.carh;
    final OAuth2Accessor accessor = null;
    final HttpServletRequest request = new DummyHttpServletRequest();

    final OAuth2HandlerError result = fixture.handleRequest(accessor, request);

    Assert.assertNotNull(result);
    Assert.assertEquals(OAuth2Error.AUTHORIZATION_CODE_PROBLEM, result.getError());
    Assert.assertEquals("accessor is null", result.getContextMessage());
  }

  @Test
  public void testHandleRequest_3() throws Exception {
    final CodeAuthorizationResponseHandler fixture = CodeAuthorizationResponseHandlerTest.carh;
    final OAuth2Accessor accessor = MockUtils.getOAuth2Accessor_Error();
    final HttpServletRequest request = new DummyHttpServletRequest();

    final OAuth2HandlerError result = fixture.handleRequest(accessor, request);

    Assert.assertNotNull(result);
    Assert.assertEquals(OAuth2Error.AUTHORIZATION_CODE_PROBLEM, result.getError());
    Assert.assertEquals("accessor is invalid", result.getContextMessage());
  }

  @Test
  public void testHandleRequest_4() throws Exception {
    final CodeAuthorizationResponseHandler fixture = CodeAuthorizationResponseHandlerTest.carh;
    final OAuth2Accessor accessor = MockUtils.getOAuth2Accessor_ClientCredentialsRedirecting();
    final HttpServletRequest request = new DummyHttpServletRequest();

    final OAuth2HandlerError result = fixture.handleRequest(accessor, request);

    Assert.assertNotNull(result);
    Assert.assertEquals(OAuth2Error.AUTHORIZATION_CODE_PROBLEM, result.getError());
    Assert.assertEquals("grant_type is not code", result.getContextMessage());
  }

  @Test
  public void testHandleRequest_5() throws Exception {
    final CodeAuthorizationResponseHandler fixture = CodeAuthorizationResponseHandlerTest.carh;
    final OAuth2Accessor accessor = MockUtils.getOAuth2Accessor_Redirecting();
    final HttpServletRequest request = new DummyHttpServletRequest();

    final OAuth2HandlerError result = fixture.handleRequest(accessor, request);

    Assert.assertNull(result);

    final OAuth2Token accessToken = CodeAuthorizationResponseHandlerTest.store.getToken(
        accessor.getGadgetUri(), accessor.getServiceName(), accessor.getUser(),
        accessor.getScope(), OAuth2Token.Type.ACCESS);
    Assert.assertNotNull(accessToken);
    Assert.assertEquals("xxx", new String(accessToken.getSecret(), "UTF-8"));
    Assert.assertEquals(OAuth2Message.BEARER_TOKEN_TYPE, accessToken.getTokenType());
    Assert.assertTrue(accessToken.getExpiresAt() > 1000);

    final OAuth2Token refreshToken = CodeAuthorizationResponseHandlerTest.store.getToken(
        accessor.getGadgetUri(), accessor.getServiceName(), accessor.getUser(),
        accessor.getScope(), OAuth2Token.Type.REFRESH);
    Assert.assertNotNull(refreshToken);
    Assert.assertEquals("yyy", new String(refreshToken.getSecret(), "UTF-8"));
  }

  @Test
  public void testHandleRequest_verifyAnonymousTokenOnRequest() throws Exception {
    MockUtils.DummyHttpFetcher fetcher = (MockUtils.DummyHttpFetcher)MockUtils.getDummyFetcher();
    CodeAuthorizationResponseHandler fixture = new CodeAuthorizationResponseHandler(
        MockUtils.getDummyMessageProvider(), MockUtils.getDummyClientAuthHandlers(),
        MockUtils.getDummyTokenEndpointResponseHandlers(), fetcher);
    final OAuth2Accessor accessor = MockUtils.getOAuth2Accessor_Redirecting();
    final HttpServletRequest request = new DummyHttpServletRequest();

    final OAuth2HandlerError result = fixture.handleRequest(accessor, request);

    Assert.assertNull(result);

    final OAuth2Token accessToken = CodeAuthorizationResponseHandlerTest.store.getToken(
        accessor.getGadgetUri(), accessor.getServiceName(), accessor.getUser(),
        accessor.getScope(), OAuth2Token.Type.ACCESS);
    Assert.assertNotNull(accessToken);
    Assert.assertEquals("xxx", new String(accessToken.getSecret(), "UTF-8"));
    Assert.assertEquals(OAuth2Message.BEARER_TOKEN_TYPE, accessToken.getTokenType());
    Assert.assertTrue(accessToken.getExpiresAt() > 1000);

    final OAuth2Token refreshToken = CodeAuthorizationResponseHandlerTest.store.getToken(
        accessor.getGadgetUri(), accessor.getServiceName(), accessor.getUser(),
        accessor.getScope(), OAuth2Token.Type.REFRESH);
    Assert.assertNotNull(refreshToken);
    Assert.assertEquals("yyy", new String(refreshToken.getSecret(), "UTF-8"));

    Assert.assertNotNull( fetcher.request );

    SecurityToken st = fetcher.request.getSecurityToken();
    Assert.assertNotNull( st );
    Assert.assertTrue( st.isAnonymous() );
    Assert.assertEquals( accessor.getGadgetUri(), st.getAppUrl() );
  }


  @Test
  public void testHandleResponse_1() throws Exception {
    final CodeAuthorizationResponseHandler fixture = CodeAuthorizationResponseHandlerTest.carh;
    final OAuth2Accessor accessor = MockUtils.getOAuth2Accessor_ClientCredentials();
    final HttpResponse response = new HttpResponse();
    final OAuth2HandlerError result = fixture.handleResponse(accessor, response);

    Assert.assertNotNull(result);
    Assert.assertEquals(null, result.getCause());
    Assert.assertEquals("doesn't handle responses", result.getContextMessage());
  }

  @Test
  public void testHandlesRequest_1() throws Exception {
    final CodeAuthorizationResponseHandler fixture = CodeAuthorizationResponseHandlerTest.carh;
    final OAuth2Accessor accessor = MockUtils.getOAuth2Accessor_Code();
    final HttpServletRequest request = null;

    final boolean result = fixture.handlesRequest(accessor, request);

    Assert.assertEquals(false, result);
  }

  @Test
  public void testHandlesRequest_2() throws Exception {
    final CodeAuthorizationResponseHandler fixture = CodeAuthorizationResponseHandlerTest.carh;
    final OAuth2Accessor accessor = MockUtils.getOAuth2Accessor_Redirecting();
    final HttpServletRequest request = new DummyHttpServletRequest();

    final boolean result = fixture.handlesRequest(accessor, request);

    Assert.assertTrue(result);
  }

  @Test
  public void testHandlesRequest_3() throws Exception {
    final CodeAuthorizationResponseHandler fixture = CodeAuthorizationResponseHandlerTest.carh;
    final OAuth2Accessor accessor = null;
    final HttpServletRequest request = new DummyHttpServletRequest();

    final boolean result = fixture.handlesRequest(accessor, request);

    Assert.assertEquals(false, result);
  }

  @Test
  public void testHandlesRequest_4() throws Exception {
    final CodeAuthorizationResponseHandler fixture = CodeAuthorizationResponseHandlerTest.carh;
    final OAuth2Accessor accessor = MockUtils.getOAuth2Accessor_Code();
    final HttpServletRequest request = new DummyHttpServletRequest();

    final boolean result = fixture.handlesRequest(accessor, request);

    Assert.assertEquals(false, result);
  }

  @Test
  public void testHandlesResponse_1() throws Exception {
    final CodeAuthorizationResponseHandler fixture = CodeAuthorizationResponseHandlerTest.carh;
    final OAuth2Accessor accessor = MockUtils.getOAuth2Accessor_Code();
    final HttpResponse response = new HttpResponse();

    final boolean result = fixture.handlesResponse(accessor, response);

    Assert.assertEquals(false, result);
  }

  static class DummyHttpServletRequest implements HttpServletRequest {
    private final Map<String, String> parameters;

    DummyHttpServletRequest() {
      this.parameters = new HashMap<String, String>(1);
      this.parameters.put(OAuth2Message.AUTHORIZATION, "1234");
    }

    public Object getAttribute(final String arg0) {
      return null;
    }

    @SuppressWarnings("rawtypes")
    public Enumeration getAttributeNames() {
      return null;
    }

    public String getCharacterEncoding() {
      return null;
    }

    public int getContentLength() {
      return 0;
    }

    public String getContentType() {
      return null;
    }

    public ServletInputStream getInputStream() throws IOException {
      return null;
    }

    public String getLocalAddr() {
      return null;
    }

    public String getLocalName() {
      return null;
    }

    public int getLocalPort() {
      return 0;
    }

    public Locale getLocale() {
      return null;
    }

    @SuppressWarnings("rawtypes")
    public Enumeration getLocales() {
      return null;
    }

    public String getParameter(final String arg0) {
      return this.parameters.get(arg0);
    }

    @SuppressWarnings("rawtypes")
    public Map getParameterMap() {
      return this.parameters;
    }

    @SuppressWarnings("rawtypes")
    public Enumeration getParameterNames() {
      return Collections.enumeration(this.parameters.keySet());
    }

    public String[] getParameterValues(final String arg0) {
      return null;
    }

    public String getProtocol() {
      return null;
    }

    public BufferedReader getReader() throws IOException {
      return null;
    }

    public String getRealPath(final String arg0) {
      return null;
    }

    public String getRemoteAddr() {
      return null;
    }

    public String getRemoteHost() {
      return null;
    }

    public int getRemotePort() {
      return 0;
    }

    public RequestDispatcher getRequestDispatcher(final String arg0) {
      return null;
    }

    public String getScheme() {
      return null;
    }

    public String getServerName() {
      return null;
    }

    public int getServerPort() {
      return 0;
    }

    public boolean isSecure() {
      return false;
    }

    public void removeAttribute(final String arg0) {
      // does nothing
    }

    public void setAttribute(final String arg0, final Object arg1) {
      // does nothing
    }

    public void setCharacterEncoding(final String arg0) throws UnsupportedEncodingException {
      // does nothing
    }

    public String getAuthType() {
      return null;
    }

    public String getContextPath() {
      return null;
    }

    public Cookie[] getCookies() {
      return null;
    }

    public long getDateHeader(final String arg0) {
      return 0;
    }

    public String getHeader(final String arg0) {
      return null;
    }

    @SuppressWarnings("rawtypes")
    public Enumeration getHeaderNames() {
      return null;
    }

    @SuppressWarnings("rawtypes")
    public Enumeration getHeaders(final String arg0) {
      return null;
    }

    public int getIntHeader(final String arg0) {
      return 0;
    }

    public String getMethod() {
      return null;
    }

    public String getPathInfo() {
      return null;
    }

    public String getPathTranslated() {
      return null;
    }

    public String getQueryString() {
      return null;
    }

    public String getRemoteUser() {
      return null;
    }

    public String getRequestURI() {
      return null;
    }

    public StringBuffer getRequestURL() {
      return null;
    }

    public String getRequestedSessionId() {
      return null;
    }

    public String getServletPath() {
      return null;
    }

    public HttpSession getSession() {
      return null;
    }

    public HttpSession getSession(final boolean arg0) {
      return null;
    }

    public Principal getUserPrincipal() {
      return null;
    }

    public boolean isRequestedSessionIdFromCookie() {
      return false;
    }

    public boolean isRequestedSessionIdFromURL() {
      return false;
    }

    public boolean isRequestedSessionIdFromUrl() {
      return false;
    }

    public boolean isRequestedSessionIdValid() {
      return false;
    }

    public boolean isUserInRole(final String arg0) {
      return false;
    }
  }
}
