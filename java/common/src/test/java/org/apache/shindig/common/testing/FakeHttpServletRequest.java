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
package org.apache.shindig.common.testing;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TimeZone;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * This class fakes a HttpServletRequest for unit test purposes. Currently, it
 * supports servlet API 2.4.
 *
 * <p>
 * To use this class, you specify the request info (URL, parameters) in the
 * constructors.
 *
 * <p>
 * Lots of stuff are still not implemented here. Feel free to implement them.
 */
public class FakeHttpServletRequest implements HttpServletRequest {
  protected static final String DEFAULT_HOST = "localhost";
  protected static final int DEFAULT_PORT = 80;
  private static final String COOKIE_HEADER = "Cookie";
  private static final String HOST_HEADER = "Host";
  private static final String DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";

  protected String scheme = "http";
  protected String host;
  protected int port;
  protected boolean secure = false;
  protected String method = "GET";
  protected String protocol = "HTTP/1.0";
  protected String contextPath;
  protected String servletPath;
  protected String pathInfo = null;
  protected String queryString;
  protected String ip = "127.0.0.1";
  protected String contentType;

  protected Hashtable<String, String> headers =
      new Hashtable<String, String>();

  // Use a LinkedHashMap so we can generate a query string that is in the same
  // order that we set the parameters
  protected Map<String, String[]> parameters = Maps.newLinkedHashMap();

  protected Set<String> postParameters = Sets.newHashSet();

  protected Map<String, Cookie> cookies = new Hashtable<String, Cookie>();


  // Use a Map rather than a table since the specified behavior of
  // setAttribute() allows null values.
  protected Map<String, Object> attributes = Maps.newHashMap();

  protected Locale locale = Locale.US;
  protected List<Locale> locales = null;

  // used by POST methods
  protected byte[] postData;
  protected String characterEncoding;

  // the following two booleans ensure that either getReader() or
  // getInputStream is called, but not both, to conform to specs for the
  // HttpServletRequest class.
  protected boolean getReaderCalled = false;
  protected boolean getInputStreamCalled = false;

  private HttpSession session;

  static final String METHOD_POST = "POST";

  /**
   * Example: http://www.example.com:1234/foo/bar?abc=xyz "www.example.com" is
   * the host 1234 is the port "/foo" is the contextPath "/bar" is the
   * servletPath "abc=xyz" is the queryString
   */
  public FakeHttpServletRequest(String host, int port, String contextPath,
      String servletPath, String queryString) {
    constructor(host, port, contextPath, servletPath, queryString);
  }

  public FakeHttpServletRequest(String host, String port, String contextPath,
      String servletPath, String queryString) {
    this(host, Integer.parseInt(port), contextPath, servletPath, queryString);
  }

  public FakeHttpServletRequest(String contextPath, String servletPath,
      String queryString) {
    this(DEFAULT_HOST, -1, contextPath, servletPath, queryString);
  }

  public FakeHttpServletRequest() {
    this(DEFAULT_HOST, DEFAULT_PORT, "", null, null);
  }

  public FakeHttpServletRequest(String urlStr) throws MalformedURLException {
    URL url = new URL(urlStr);
    String contextPath;
    String servletPath;
    String path = url.getPath();
    if (path.length() <= 1) {
      // path must be either empty string or "/"
      contextPath = path;
      servletPath = null;
    } else {
      // Look for the second slash which separates the servlet path from the
      // context path. e.g. "/foo/bar"
      int secondSlash = path.indexOf('/', 1);
      if (secondSlash < 0) {
        // No second slash
        contextPath = path;
        servletPath = null;
      } else {
        contextPath = path.substring(0, secondSlash);
        servletPath = path.substring(secondSlash);
      }
    }

    // Set the scheme
    scheme = url.getProtocol();
    if (scheme.equalsIgnoreCase("https")) {
      secure = true;
    }

    int port = url.getPort();

    // Call constructor() instead of this() because the later is only allowed
    // at the begining of a constructor
    constructor(url.getHost(), port, contextPath, servletPath, url.getQuery());
  }

  public FakeHttpServletRequest setLocale(Locale locale) {
    this.locale = locale;
    return this;
  }

  public FakeHttpServletRequest setLocales(List<Locale> locales) {
    this.locales = locales;
    return this;
  }

  public FakeHttpServletRequest setProtocol(String prot) {
    this.protocol = prot;
    return this;
  }

  public FakeHttpServletRequest setSecure(boolean secure) {
    this.secure = secure;
    return this;
  }

  /*
   * Set a header on this request. Note that if the header implies other
   * attributes of the request I will set them accordingly. Specifically:
   *
   * If the header is "Cookie:" then I will automatically call setCookie on all
   * of the name-value pairs found therein.
   *
   * This makes the object easier to use because you can just feed it headers
   * and the object will remain consistent with the behavior you'd expect from a
   * request.
   */
  public FakeHttpServletRequest setHeader(String name, String value) {
    if (name.equals(COOKIE_HEADER)) {
      String[] pairs = splitAndTrim(value, ";");
      for (String pair : pairs) {
        int equalsPos = pair.indexOf('=');
        if (equalsPos != -1) {
          String cookieName = pair.substring(0, equalsPos);
          String cookieValue = pair.substring(equalsPos + 1);
          addToCookieMap(new Cookie(cookieName, cookieValue));
        }
      }
      setCookieHeader();
      return this;
    }

    addToHeaderMap(name, value);

    if (name.equals(HOST_HEADER)) {
      host = value;
    }
    return this;
  }

  private void addToHeaderMap(String name, String value) {
    headers.put(name.toLowerCase(), value);
  }

  /**
   * Associates a set of cookies with this fake request.
   *
   * @param cookies the cookies associated with this request.
   */
  public FakeHttpServletRequest setCookies(Cookie... cookies) {
    for (Cookie cookie : cookies) {
      addToCookieMap(cookie);
    }
    setCookieHeader();
    return this;
  }

  /**
   * Sets a single cookie associated with this fake request. Cookies are
   * cumulative, but ones with the same name will overwrite one another.
   *
   * @param c the cookie to associate with this request.
   */
  public FakeHttpServletRequest setCookie(Cookie c) {
    addToCookieMap(c);
    setCookieHeader();
    return this;
  }

  private void addToCookieMap(Cookie c) {
    cookies.put(c.getName(), c);
  }

  /**
   * Sets the "Cookie" HTTP header based on the current cookies.
   */
  private void setCookieHeader() {
    StringBuilder sb = new StringBuilder();
    boolean isFirst = true;
    for (Cookie c : cookies.values()) {
      if (!isFirst) {
        sb.append("; ");
      }
      sb.append(c.getName());
      sb.append('=');
      sb.append(c.getValue());
      isFirst = false;
    }

    // We cannot use setHeader() here, because setHeader() calls this method
    addToHeaderMap(COOKIE_HEADER, sb.toString());
  }

  /**
   * Sets the a parameter in this fake request.
   *
   * @param name the string key
   * @param values the string array value
   * @param isPost if the paramenter comes in the post body.
   */
  public FakeHttpServletRequest setParameter(String name, boolean isPost, String... values) {
    if (isPost) {
      postParameters.add(name);
    }
    parameters.put(name, values);
    // Old query string no longer matches up, so set it to null so it can be
    // regenerated on the next call of getQueryString()
    queryString = null;
    return this;
  }

  /**
   * Sets the a parameter in this fake request.
   *
   * @param name the string key
   * @param values the string array value
   */
  public FakeHttpServletRequest setParameter(String name, String... values) {
    setParameter(name, false, values);
    return this;
  }


  /** Set the path info field. */
  public FakeHttpServletRequest setPathInfo(String path) {
    pathInfo = path;
    return this;
  }

  /**
   * Specify the mock POST data.
   *
   * @param postString the mock post data
   * @param encoding format with which to encode mock post data
   */
  public FakeHttpServletRequest setPostData(String postString, String encoding)
      throws UnsupportedEncodingException {
    setPostData(postString.getBytes(encoding));
    characterEncoding = encoding;
    return this;
  }

  /**
   * Specify the mock POST data in raw binary format.
   *
   * This implicitly sets character encoding to not specified.
   *
   * @param data the mock post data; this is owned by the caller, so
   *        modifications made after this call will show up when the post data
   *        is read
   */
  public FakeHttpServletRequest setPostData(byte[] data) {
    postData = data;
    characterEncoding = null;
    method = METHOD_POST;
    return this;
  }

  /**
   * Set a new value for the query string. The query string will be parsed and
   * all parameters reset.
   *
   * @param queryString representing the new value. i.e.: "bug=1&id=23"
   */
  public FakeHttpServletRequest setQueryString(String queryString) {
    this.queryString = queryString;
    parameters.clear();
    decodeQueryString(queryString, parameters);
    return this;
  }

  /**
   * Sets the session for this request.
   *
   * @param session the new session
   */
  public FakeHttpServletRequest setSession(HttpSession session) {
    this.session = session;
    return this;
  }

  /**
   * Sets the content type.
   *
   * @param contentType of the request.
   */
  public FakeHttpServletRequest setContentType(String contentType) {
    this.contentType = contentType;
    return this;
  }

  // ///////////////////////////////////////////////////////////////////////////
  // Implements methods from HttpServletRequest
  // ///////////////////////////////////////////////////////////////////////////

  public String getAuthType() {
    throw new UnsupportedOperationException();
  }

  public java.lang.String getContextPath() {
    return contextPath;
  }

  public Cookie[] getCookies() {
    if (cookies.isEmpty()) {
      // API promises null return if no cookies
      return null;
    }
    return cookies.values().toArray(new Cookie[cookies.size()]);
  }

  public long getDateHeader(String name) {
    String value = getHeader(name);
    if (value == null) return -1;

    SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT, Locale.US);
    format.setTimeZone(TimeZone.getTimeZone("GMT"));
    try {
      return format.parse(value).getTime();
    } catch (ParseException e) {
      throw new IllegalArgumentException("Cannot parse number from header "
          + name + ':' + value, e);
    }
  }

  public FakeHttpServletRequest setDateHeader(String name, long value) {
    SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT, Locale.US);
    format.setTimeZone(TimeZone.getTimeZone("GMT"));
    setHeader(name, format.format(new Date(value)));
    return this;
  }

  public String getHeader(String name) {
    return headers.get(name.toLowerCase());
  }

  public Enumeration<String> getHeaderNames() {
    return headers.keys();
  }

  public Enumeration<?> getHeaders(String name) {
    List<String> values = Lists.newArrayList();
    for (Map.Entry<String, String> entry : headers.entrySet()) {
      if (name.equalsIgnoreCase(entry.getKey())) {
        values.add(entry.getValue());
      }
    }
    return Collections.enumeration(values);
  }

  public int getIntHeader(String name) {
    return Integer.parseInt(getHeader(name));
  }

  public String getMethod() {
    return method;
  }

  public FakeHttpServletRequest setMethod(String method) {
    this.method = method;
    return this;
  }

  public String getPathInfo() {
    return pathInfo;
  }

  public String getPathTranslated() {
    throw new UnsupportedOperationException();
  }

  public String getQueryString() {
    try {
      if (queryString == null && !parameters.isEmpty()) {
        boolean hasPrevious = false;
        StringBuilder queryString = new StringBuilder();
        for (Map.Entry<String, String[]> entry : parameters.entrySet()) {
          // We're not interested in blank keys
          if (StringUtils.isBlank(entry.getKey()) || postParameters.contains(entry.getKey())) {
            continue;
          }
          if (hasPrevious) {
            queryString.append('&');
          }

          String[] values = entry.getValue();
          // Append the parameters to the query string
          if (values.length == 0) {
            queryString.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
          } else {
            for (int i = 0; i < values.length; i++) {
              queryString.append(URLEncoder.encode(entry.getKey(), "UTF-8")).append('=').append(
                      URLEncoder.encode(values[i], "UTF-8"));
              if (i < values.length - 1) {
                queryString.append('&');
              }
            }
          }
          hasPrevious = true;

        }
        this.queryString = queryString.toString();
      }
      return queryString;
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("Should always support UTF-8", e);
    }
  }

  public String getRemoteUser() {
    throw new UnsupportedOperationException();
  }

  public String getRequestedSessionId() {
    throw new UnsupportedOperationException();
  }

  public String getRequestURI() {
    StringBuilder buf = new StringBuilder();
    if (StringUtils.isNotBlank(contextPath)) {
      buf.append(contextPath);
    }

    if (servletPath != null && !"".equals(servletPath)) {
      buf.append(servletPath);
    }

    if (buf.length() == 0) {
      buf.append('/');
    }

    return buf.toString();
  }

  public StringBuffer getRequestURL() {
    StringBuffer buf =
        secure ? new StringBuffer("https://") : new StringBuffer("http://");
    buf.append(host);
    if (port >= 0) {
      buf.append(':');
      buf.append(port);
    }
    buf.append(getRequestURI()); // always begins with '/'
    return buf;
  }

  public String getServletPath() {
    return servletPath;
  }

  public FakeHttpServletRequest setServletPath(String servletPath) {
    this.servletPath = servletPath;
    return this;
  }

  public HttpSession getSession() {
    return getSession(true);
  }

  public HttpSession getSession(boolean create) {
    // TODO return fake session if create && session == null
    return session;
  }

  public java.security.Principal getUserPrincipal() {
    throw new UnsupportedOperationException();
  }

  public boolean isRequestedSessionIdFromCookie() {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public boolean isRequestedSessionIdFromUrl() {
    throw new UnsupportedOperationException("This method is deprecated");
  }

  public boolean isRequestedSessionIdFromURL() {
    throw new UnsupportedOperationException();
  }

  public boolean isRequestedSessionIdValid() {
    throw new UnsupportedOperationException();
  }

  public boolean isUserInRole(String role) {
    throw new UnsupportedOperationException();
  }

  // Implements methods from ServletRequest ///////////////////////////////////

  public Object getAttribute(String name) {
    return attributes.get(name);
  }

  public Enumeration<?> getAttributeNames() {
    return Collections.enumeration(attributes.keySet());
  }

  public String getCharacterEncoding() {
    return characterEncoding;
  }

  public int getContentLength() {
    return (postData == null) ? 0 : postData.length;
  }

  public String getContentType() {
    return contentType;
  }

  /**
   * Get the body of the request (i.e. the POST data) as a binary stream. As per
   * Java docs, this OR getReader() may be called, but not both (attempting that
   * will result in an IllegalStateException)
   *
   */
  public ServletInputStream getInputStream() {
    if (getReaderCalled) {
      throw new IllegalStateException(
          "getInputStream() called after getReader()");
    }
    getInputStreamCalled = true; // so that getReader() can no longer be called

    final InputStream in = new ByteArrayInputStream(postData);
    return new ServletInputStream() {
      @Override public int read() throws IOException {
        return in.read();
      }
    };
  }

  public Locale getLocale() {
    return locale;
  }

  public Enumeration<?> getLocales() {
    return Collections.enumeration(locales);
  }

  public String getParameter(String name) {
    String[] parameters = getParameterValues(name);
    if (parameters == null || parameters.length < 1) {
      return null;
    } else {
      return parameters[0];
    }
  }

  public Map<String, String[]> getParameterMap() {
    return parameters;
  }

  public Enumeration<String> getParameterNames() {
    return Collections.enumeration(parameters.keySet());
  }

  public String[] getParameterValues(String name) {
    return parameters.get(name);
  }

  public String getProtocol() {
    return protocol;
  }

  public BufferedReader getReader() throws IOException {
    if (getInputStreamCalled) {
      throw new IllegalStateException(
          "getReader() called after getInputStream()");
    }

    getReaderCalled = true;
    BufferedReader br = null;
    ByteArrayInputStream bais = new ByteArrayInputStream(postData);
    InputStreamReader isr;
    if (characterEncoding != null) {
      isr = new InputStreamReader(bais, characterEncoding);
    } else {
      isr = new InputStreamReader(bais);
    }
    br = new BufferedReader(isr);
    return br;
  }

  @Deprecated
  public String getRealPath(String path) {
    throw new UnsupportedOperationException("This method is deprecated");
  }

  public String getRemoteAddr() {
    return ip;
  }

  /**
   * Sets the remote IP address for this {@code FakeHttpServletRequest}.
   *
   * @param ip the IP to set
   * @return this {@code FakeHttpServletRequest} object
   */
  public FakeHttpServletRequest setRemoteAddr(String ip) {
    this.ip = ip;
    return this;
  }

  public String getRemoteHost() {
    return "localhost";
  }


  /*
   * (non-Javadoc)
   *
   * New Servlet 2.4 method
   *
   * @see javax.servlet.ServletRequest#getLocalPort()
   */
  public int getLocalPort() {
    return 8080;
  }

  /*
   * (non-Javadoc)
   *
   * New Servlet 2.4 method
   *
   * @see javax.servlet.ServletRequest#getLocalAddr()
   */
  public String getLocalAddr() {
    return "127.0.0.1";
  }

  /*
   * (non-Javadoc)
   *
   * New Servlet 2.4 method
   *
   * @see javax.servlet.ServletRequest#getLocalName()
   */
  public String getLocalName() {
    return "localhost";
  }

  /*
   * (non-Javadoc)
   *
   * New Servlet 2.4 method
   *
   * @see javax.servlet.ServletRequest#getRemotePort()
   */
  public int getRemotePort() {
    throw new UnsupportedOperationException();
  }


  public RequestDispatcher getRequestDispatcher(String path) {
    throw new UnsupportedOperationException();
  }

  public String getScheme() {
    return scheme;
  }

  public String getServerName() {
    return host;
  }

  public int getServerPort() {
    return (port < 0) ? DEFAULT_PORT : port;
  }

  public boolean isSecure() {
    return secure;
  }

  public void removeAttribute(String name) {
    attributes.remove(name);
  }

  public void setAttribute(String name, Object value) {
    attributes.put(name, value);
  }

  /**
   * @inheritDoc
   *
   * For POST requests, this affects interpretation of POST body.
   *
   * For non-POST requests (original author's comment): Do nothing - all request
   * components were created as unicode Strings, so this can't affect how
   * they're interpreted anyway.
   */
  public void setCharacterEncoding(String env) {
    if (method.equals(METHOD_POST)) {
      characterEncoding = env;
    }
  }

  // Helper methods ///////////////////////////////////////////////////////////

  /**
   * This method serves as the central constructor of this class. The reason it
   * is not an actual constructor is that Java doesn't allow calling another
   * constructor at the end of a constructor. e.g.
   *
   * <pre>
   * public FakeHttpServletRequest(String foo) {
   *   // Do something here
   *   this(foo, bar); // calling another constructor here is not allowed
   * }
   * </pre>
   */
  protected void constructor(String host, int port, String contextPath,
      String servletPath, String queryString) {
    setHeader(HOST_HEADER, host);
    this.port = port;
    this.contextPath = contextPath;
    this.servletPath = servletPath;
    this.queryString = queryString;
    if (queryString != null) {
      decodeQueryString(queryString, parameters);
    }
  }

  protected void decodeQueryString(String queryString,
      Map<String, String[]> parameters) {
    for (String param : queryString.split("&")) {
      // The first '=' separates the name and value
      int sepPos = param.indexOf('=');
      String name, value;
      if (sepPos < 0) {
        // if no equal is present, assume a blank value
        name = param;
        value = "";
      } else {
        name = param.substring(0, sepPos);
        value = param.substring(sepPos + 1);
      }

      addParameter(parameters, decodeParameterPart(name),
          decodeParameterPart(value));
    }
  }

  private String decodeParameterPart(String str) {
    // borrowed from FormUrlDecoder
    try {
      // we could infer proper encoding from headers, but setCharacterEncoding
      // is a noop.
      return URLDecoder.decode(str, "UTF-8");
    } catch (IllegalArgumentException iae) {
      // According to the javadoc of URLDecoder, when the input string is
      // illegal, it could either leave the illegal characters alone or throw
      // an IllegalArgumentException! To deal with both consistently, we
      // ignore IllegalArgumentException and just return the original string.
      return str;
    } catch (UnsupportedEncodingException e) {
      return str;
    }
  }

  protected void addParameter(Map<String, String[]> parameters, String name,
      String value) {
    if (parameters.containsKey(name)) {
      String[] existingParamValues = parameters.get(name);
      String[] newParamValues = new String[existingParamValues.length + 1];
      System.arraycopy(existingParamValues, 0, newParamValues, 0,
          existingParamValues.length);
      newParamValues[newParamValues.length - 1] = value;
      parameters.put(name, newParamValues);
    } else {
      String[] paramValues = {value,};
      parameters.put(name, paramValues);
    }
  }

  private static String[] splitAndTrim(String str, String delims) {
    StringTokenizer tokenizer = new StringTokenizer(str, delims);
    int n = tokenizer.countTokens();
    String[] list = new String[n];
    for (int i = 0; i < n; i++) {
      list[i] = tokenizer.nextToken().trim();
    }
    return list;
  }
}
