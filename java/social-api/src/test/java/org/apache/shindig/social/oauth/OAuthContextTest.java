/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.social.oauth;

import junit.framework.TestCase;

import org.apache.shindig.social.oauth.OAuthContext.AuthMethod;

import java.io.BufferedReader;
import java.security.Principal;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public class OAuthContextTest extends TestCase {

  public void testGettersAndSetters() throws Exception {
    OAuthContext context = new OAuthContext();

    // first, make sure it's constructed in the right state
    assertEquals(OAuthContext.AuthMethod.NONE, context.getAuthMethod());
    assertNull(context.getConsumerKey());
    assertNull(context.getOAuthToken());

    // then, test the getters and setters
    context.setAuthMethod(AuthMethod.OAUTH);
    assertEquals(OAuthContext.AuthMethod.OAUTH, context.getAuthMethod());

    context.setConsumerKey("consumer");
    assertEquals("consumer", context.getConsumerKey());

    context.setOAuthToken("token");
    assertEquals("token", context.getOAuthToken());
  }

  public void testCreationAndOverriding() throws Exception {

    HttpServletRequest request = new FakeHttpServletRequest();

    // make sure that we always get a OAuthContext object
    OAuthContext context = OAuthContext.fromRequest(request);

    assertNotNull(context);

    // make sure that we can override existing contexts
    OAuthContext context2 = OAuthContext.newContextForRequest(request);

    assertNotSame(context, context2);
    assertEquals(OAuthContext.AuthMethod.NONE, context2.getAuthMethod());

    OAuthContext context3 = OAuthContext.fromRequest(request);
    assertSame(context2, context3);
  }

  public static class FakeHttpServletRequest implements HttpServletRequest {

    private HashMap<String, Object> attributes = new HashMap<String, Object>();

    public String getAuthType() {
      return null;
    }

    public String getContextPath() {
      return null;
    }

    public Cookie[] getCookies() {
      return null;
    }

    public long getDateHeader(String name) {
      return 0;
    }

    public String getHeader(String name) {
      return null;
    }

    @SuppressWarnings("unchecked")
    public Enumeration getHeaderNames() {
      return null;
    }

    @SuppressWarnings("unchecked")
    public Enumeration getHeaders(String name) {
      return null;
    }

    public int getIntHeader(String name) {
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
      return new StringBuffer("http://foo.com/bar");
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

    public HttpSession getSession(boolean create) {
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

    public boolean isUserInRole(String role) {
      return false;
    }

    public Object getAttribute(String name) {
      return attributes.get(name);
    }

    @SuppressWarnings("unchecked")
    public Enumeration getAttributeNames() {
      return Collections.enumeration(attributes.keySet());
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

    public ServletInputStream getInputStream() {
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

    @SuppressWarnings("unchecked")
    public Enumeration getLocales() {
      return null;
    }

    public String getParameter(String name) {
      return null;
    }

    @SuppressWarnings("unchecked")
    public Map getParameterMap() {
      return new HashMap();
    }

    @SuppressWarnings("unchecked")
    public Enumeration getParameterNames() {
      return null;
    }

    public String[] getParameterValues(String name) {
      return new String[0];
    }

    public String getProtocol() {
      return null;
    }

    public BufferedReader getReader() {
      return null;
    }

    public String getRealPath(String path) {
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

    public RequestDispatcher getRequestDispatcher(String path) {
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

    public void removeAttribute(String name) {
      attributes.remove(name);
    }

    public void setAttribute(String name, Object o) {
      attributes.put(name, o);
    }

    public void setCharacterEncoding(String env) {
    }
  }
}
