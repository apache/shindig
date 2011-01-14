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

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;

import com.google.caja.util.Lists;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.RenderingContext;
import org.apache.shindig.gadgets.uri.JsUriManager;
import org.apache.shindig.gadgets.uri.JsUriManager.JsUri;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletResponse;

public class JsServletTest extends ServletTestFixture {
  private static final String CONTAINER_PARAM = "im_a_container";
  private static final String ONLOAD_PARAM = "onload_me";
  private static final int REFRESH_INTERVAL_SEC = 200;
  private static final int TIMEOUT_SEC = 1000;

  private final JsServlet servlet = new JsServlet();
  private JsServlet.CachingSetter httpUtilMock;
  private JsHandler jsHandlerMock;
  private JsUriManager jsUriManagerMock;

  @Before
  public void setUp() throws Exception {
    httpUtilMock = mock(JsServlet.CachingSetter.class);
    servlet.setCachingSetter(httpUtilMock);

    jsHandlerMock = mock(JsHandler.class);
    servlet.setJsHandler(jsHandlerMock);

    jsUriManagerMock = mock(JsUriManager.class);
    servlet.setUrlGenerator(jsUriManagerMock);

    // TODO: Abstract this out into a helper function associated with Uri class.
    expect(request.getScheme()).andReturn("http");
    expect(request.getServerPort()).andReturn(8080);
    expect(request.getServerName()).andReturn("localhost");
    expect(request.getRequestURI()).andReturn("/gadgets/js");
    expect(request.getQueryString()).andReturn(null);
  }

  private JsUri mockJsUri(String container, RenderingContext context, boolean debug,
      boolean jsload, boolean nocache, String onload, int refresh, String... libs) {
    JsUri result = mock(JsUri.class);
    expect(result.getContainer()).andReturn(container).anyTimes();
    expect(result.getContext()).andReturn(context).anyTimes();
    expect(result.getOnload()).andReturn(onload).anyTimes();
    expect(result.getRefresh()).andReturn(refresh).anyTimes();
    expect(result.isDebug()).andReturn(debug).anyTimes();
    expect(result.isNoCache()).andReturn(nocache).anyTimes();
    expect(result.isJsload()).andReturn(jsload).anyTimes();
    if (libs != null) {
      expect(result.getLibs()).andReturn(Lists.newArrayList(libs)).anyTimes();
    }
    return result;
  }

  @Test
  public void testDoJsloadNormal() throws Exception {
    String url = "http://localhost/gadgets/js/feature.js?v=abc&nocache=0&onload=" + ONLOAD_PARAM;
    JsUri jsUri = mockJsUri(CONTAINER_PARAM, RenderingContext.CONTAINER, true, true, false,
        ONLOAD_PARAM, REFRESH_INTERVAL_SEC);

    expect(jsUriManagerMock.processExternJsUri(isA(Uri.class))).andReturn(jsUri);
    expect(jsUriManagerMock.makeExternJsUri(isA(JsUri.class)))
        .andReturn(Uri.parse(url + "&jsload=1"));
    httpUtilMock.setCachingHeaders(recorder, REFRESH_INTERVAL_SEC, false);
    replay();

    servlet.doGet(request, recorder);
    assertEquals(HttpServletResponse.SC_OK, recorder.getHttpStatusCode());
    assertEquals(String.format(JsServlet.JSLOAD_JS_TPL, url), recorder.getResponseAsString());
    verify();
  }

  @Test
  public void testDoJsloadWithJsLoadTimeout() throws Exception {
    String url = "http://localhost/gadgets/js/feature.js?v=abc&nocache=0&onload=" + ONLOAD_PARAM;
    JsUri jsUri = mockJsUri(CONTAINER_PARAM, RenderingContext.CONTAINER, true, true,
        false, ONLOAD_PARAM, -1); // Disable refresh interval.

    expect(jsUriManagerMock.processExternJsUri(isA(Uri.class))).andReturn(jsUri);
    expect(jsUriManagerMock.makeExternJsUri(isA(JsUri.class)))
        .andReturn(Uri.parse(url + "&jsload=1"));
    servlet.setJsloadTtlSecs(TIMEOUT_SEC); // Enable JS load timeout.
    httpUtilMock.setCachingHeaders(recorder, TIMEOUT_SEC, false);
    replay();

    servlet.doGet(request, recorder);
    assertEquals(HttpServletResponse.SC_OK, recorder.getHttpStatusCode());
    assertEquals(String.format(JsServlet.JSLOAD_JS_TPL, url), recorder.getResponseAsString());
    verify();
  }

  @Test
  public void testDoJsloadNoOnload() throws Exception {
    JsUri jsUri = mockJsUri(CONTAINER_PARAM, RenderingContext.CONTAINER, true, true, false,
        null, // lacks &onload=
        REFRESH_INTERVAL_SEC);
    expect(jsUriManagerMock.processExternJsUri(isA(Uri.class))).andReturn(jsUri);
    replay();

    servlet.doGet(request, recorder);
    assertEquals(HttpServletResponse.SC_BAD_REQUEST, recorder.getHttpStatusCode());
    assertEquals(JsServlet.JSLOAD_ONLOAD_ERROR, recorder.getResponseAsString());
    verify();
  }

  @Test
  public void testDoJsloadNoCache() throws Exception {
    String url = "http://localhost/gadgets/js/feature.js?v=abc&nocache=1&onload=" + ONLOAD_PARAM;
    JsUri jsUri = mockJsUri(CONTAINER_PARAM, RenderingContext.CONTAINER, true, true,
        true, // Set to no cache.
        ONLOAD_PARAM, REFRESH_INTERVAL_SEC);

    expect(jsUriManagerMock.processExternJsUri(isA(Uri.class))).andReturn(jsUri);
    expect(jsUriManagerMock.makeExternJsUri(isA(JsUri.class)))
        .andReturn(Uri.parse(url + "&jsload=1"));
    httpUtilMock.setCachingHeaders(recorder, 0, false); // TTL of 0 is set.
    replay();

    servlet.doGet(request, recorder);
    assertEquals(HttpServletResponse.SC_OK, recorder.getHttpStatusCode());
    assertEquals(String.format(JsServlet.JSLOAD_JS_TPL, url), recorder.getResponseAsString());
    verify();
  }
}
