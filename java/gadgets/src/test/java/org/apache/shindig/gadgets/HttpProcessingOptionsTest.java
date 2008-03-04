/**
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

package org.apache.shindig.gadgets;

import org.apache.shindig.gadgets.http.HttpProcessingOptions;
import org.junit.Assert;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;
import javax.servlet.ServletInputStream;
import javax.servlet.RequestDispatcher;
import java.util.Enumeration;
import java.util.Map;
import java.util.Locale;
import java.security.Principal;
import java.io.UnsupportedEncodingException;
import java.io.IOException;
import java.io.BufferedReader;

import junit.framework.TestCase;

/**
 */
public class HttpProcessingOptionsTest extends TestCase {

  public void testGetIgnoreCache_ParamEqualsIntMax() throws Exception {
    Assert.assertTrue("Should ignore nocache", new HttpProcessingOptions(makeFakeHttpRequest(Integer.toString(Integer.MAX_VALUE))).getIgnoreCache());
  }

  public void testGetIgnoreCache_ParamEquals1() throws Exception {
    Assert.assertTrue("Should ignore nocache", new HttpProcessingOptions(makeFakeHttpRequest("1")).getIgnoreCache());
  }

  public void testGetIgnoreCache_ParamEqualsEmptyString() throws Exception {
    Assert.assertTrue("Should ignore nocache", new HttpProcessingOptions(makeFakeHttpRequest("")).getIgnoreCache());
  }

  public void testGetIgnoreCache_ParamEqualsZero() throws Exception {
    Assert.assertFalse("Should not ignore nocache", new HttpProcessingOptions(makeFakeHttpRequest("0")).getIgnoreCache());
  }

  public void testGetIgnoreCache_ParamEqualsNull() throws Exception {
    Assert.assertFalse("Should not ignore nocache", new HttpProcessingOptions(makeFakeHttpRequest(null)).getIgnoreCache());
  }

  /**
   * Makes a Fake Servlet request that can change it's return for the call getParamter("nocache")
   * @param cachingValue   What value to return for getparameter call
   * @return HttpServletRequest faked request object
   */

  private HttpServletRequest makeFakeHttpRequest(final String cachingValue) {
    return new HttpServletRequest() {

      public String getParameter(String s) {
        if (s != null && s.equals("nocache")) {
          return cachingValue;
        }
        return null;
      }

      public Object getAttribute(String s) {
        return null;
      }

      public Enumeration getAttributeNames() {
        return null;
      }

      public String getCharacterEncoding() {
        return null;
      }

      public void setCharacterEncoding(String s) throws UnsupportedEncodingException {

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

      public Enumeration getParameterNames() {
        return null;
      }

      public String[] getParameterValues(String s) {
        return new String[0];
      }

      public Map getParameterMap() {
        return null;
      }

      public String getProtocol() {
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

      public BufferedReader getReader() throws IOException {
        return null;
      }

      public String getRemoteAddr() {
        return null;
      }

      public String getRemoteHost() {
        return null;
      }

      public void setAttribute(String s, Object o) {

      }

      public void removeAttribute(String s) {

      }

      public Locale getLocale() {
        return null;
      }

      public Enumeration getLocales() {
        return null;
      }

      public boolean isSecure() {
        return false;
      }

      public RequestDispatcher getRequestDispatcher(String s) {
        return null;
      }

      public String getRealPath(String s) {
        return null;
      }

      public int getRemotePort() {
        return 0;
      }

      public String getLocalName() {
        return null;
      }

      public String getLocalAddr() {
        return null;
      }

      public int getLocalPort() {
        return 0;
      }

      public String getAuthType() {
        return null;
      }

      public Cookie[] getCookies() {
        return new Cookie[0];
      }

      public long getDateHeader(String s) {
        return 0;
      }

      public String getHeader(String s) {
        return null;
      }

      public Enumeration getHeaders(String s) {
        return null;
      }

      public Enumeration getHeaderNames() {
        return null;
      }

      public int getIntHeader(String s) {
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

      public String getContextPath() {
        return null;
      }

      public String getQueryString() {
        return null;
      }

      public String getRemoteUser() {
        return null;
      }

      public boolean isUserInRole(String s) {
        return false;
      }

      public Principal getUserPrincipal() {
        return null;
      }

      public String getRequestedSessionId() {
        return null;
      }

      public String getRequestURI() {
        return null;
      }

      public StringBuffer getRequestURL() {
        return null;
      }

      public String getServletPath() {
        return null;
      }

      public HttpSession getSession(boolean b) {
        return null;
      }

      public HttpSession getSession() {
        return null;
      }

      public boolean isRequestedSessionIdValid() {
        return false;
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
    };
  }


}
