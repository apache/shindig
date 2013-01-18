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
import static org.easymock.EasyMock.getCurrentArguments;
import static org.easymock.EasyMock.isA;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.RenderingContext;
import org.apache.shindig.gadgets.js.AddOnloadFunctionProcessor;
import org.apache.shindig.gadgets.js.DefaultJsProcessorRegistry;
import org.apache.shindig.gadgets.js.DefaultJsServingPipeline;
import org.apache.shindig.gadgets.js.GetJsContentProcessor;
import org.apache.shindig.gadgets.js.IfModifiedSinceProcessor;
import org.apache.shindig.gadgets.js.JsException;
import org.apache.shindig.gadgets.js.JsLoadProcessor;
import org.apache.shindig.gadgets.js.JsProcessor;
import org.apache.shindig.gadgets.js.JsProcessorRegistry;
import org.apache.shindig.gadgets.js.JsRequest;
import org.apache.shindig.gadgets.js.JsRequestBuilder;
import org.apache.shindig.gadgets.js.JsResponse;
import org.apache.shindig.gadgets.js.JsResponseBuilder;
import org.apache.shindig.gadgets.js.JsServingPipeline;
import org.apache.shindig.gadgets.uri.JsUriManager;
import org.apache.shindig.gadgets.uri.JsUriManager.JsUri;
import org.apache.shindig.gadgets.uri.UriStatus;
import org.easymock.IAnswer;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletResponse;

public class JsServletTest extends ServletTestFixture {
  private static final String EXAMPLE_JS_CODE = "some javascript code";
  private static final String CONTAINER_PARAM = "im_a_container";
  private static final String ONLOAD_PARAM = "onload_me";
  private static final int REFRESH_INTERVAL_SEC = 200;
  private static final int TIMEOUT_SEC = 1000;

  private final JsServlet servlet = new JsServlet();
  private JsServlet.CachingSetter httpUtilMock;
  private GetJsContentProcessor getJsProcessorMock;
  private JsUriManager jsUriManagerMock;
  private JsLoadProcessor jsLoadProcessor;
  private DefaultJsServingPipeline jsServingPipeline;

  @Before
  public void setUp() throws Exception {
    httpUtilMock = mock(JsServlet.CachingSetter.class);
    servlet.setCachingSetter(httpUtilMock);

    jsUriManagerMock = mock(JsUriManager.class);
    servlet.setJsRequestBuilder(new JsRequestBuilder(jsUriManagerMock, null));

    getJsProcessorMock = mock(GetJsContentProcessor.class);
  }

  private void setUp(int jsloadTtlSecs) {
    jsLoadProcessor = new JsLoadProcessor(jsUriManagerMock, jsloadTtlSecs, true);
    JsProcessorRegistry jsProcessorRegistry =
        new DefaultJsProcessorRegistry(
            ImmutableList.<JsProcessor>of(new IfModifiedSinceProcessor()),
            ImmutableList.<JsProcessor>of(jsLoadProcessor, getJsProcessorMock, new AddOnloadFunctionProcessor()),
            ImmutableList.<JsProcessor>of());

    jsServingPipeline = new DefaultJsServingPipeline(jsProcessorRegistry);
    servlet.setJsServingPipeline(jsServingPipeline);

    // TODO: Abstract this out into a helper function associated with Uri class.
    expect(request.getScheme()).andReturn("http");
    expect(request.getServerPort()).andReturn(8080);
    expect(request.getServerName()).andReturn("localhost");
    expect(request.getRequestURI()).andReturn("/gadgets/js");
    expect(request.getQueryString()).andReturn(null);
  }

  private JsUri mockJsUri(String container, RenderingContext context, boolean debug,
      boolean jsload, boolean nocache, String onload, int refresh, UriStatus status,
      String... libs) {
    JsUri result = mock(JsUri.class);
    expect(result.getContainer()).andReturn(container).anyTimes();
    expect(result.getContext()).andReturn(context).anyTimes();
    expect(result.getOnload()).andReturn(onload).anyTimes();
    expect(result.getRefresh()).andReturn(refresh).anyTimes();
    expect(result.isDebug()).andReturn(debug).anyTimes();
    expect(result.isNoCache()).andReturn(nocache).anyTimes();
    expect(result.isJsload()).andReturn(jsload).anyTimes();
    expect(result.getStatus()).andReturn(status).anyTimes();
    if (libs != null) {
      expect(result.getLibs()).andReturn(Lists.newArrayList(libs)).anyTimes();
    }
    return result;
  }

  @Test
  public void testJsServletGivesErrorWhenUriManagerThrowsException() throws Exception {
    setUp(0);
    expect(jsUriManagerMock.processExternJsUri(isA(Uri.class))).andThrow(new GadgetException(null));
    replay();

    servlet.doGet(request, recorder);
    assertEquals(HttpServletResponse.SC_BAD_REQUEST, recorder.getHttpStatusCode());
    verify();
  }

  @Test
  public void testWithIfModifiedSinceHeaderPresentAndVersionReturnsNotModified() throws Exception {
    setUp(0);
    JsUri jsUri = mockJsUri(CONTAINER_PARAM, RenderingContext.CONTAINER, false, false, false,
        null, REFRESH_INTERVAL_SEC, UriStatus.VALID_VERSIONED);
    expect(jsUriManagerMock.processExternJsUri(isA(Uri.class))).andReturn(jsUri);
    expect(request.getHeader("If-Modified-Since")).andReturn("12345");
    replay();

    servlet.doGet(request, recorder);
    assertEquals(HttpServletResponse.SC_NOT_MODIFIED, recorder.getHttpStatusCode());
    verify();
  }

  @Test
  public void testWithIfModifiedSinceHeaderPresentButNoVersionActsNormal() throws Exception {
    setUp(0);
    JsUri jsUri = mockJsUri(CONTAINER_PARAM, RenderingContext.CONTAINER, false, false, false,
        null, REFRESH_INTERVAL_SEC, UriStatus.VALID_UNVERSIONED);
    expect(jsUriManagerMock.processExternJsUri(isA(Uri.class))).andReturn(jsUri);
    expect(request.getHeader("If-Modified-Since")).andReturn("12345");
    final JsResponse response = new JsResponseBuilder().appendJs(EXAMPLE_JS_CODE, "js").build();
    expect(request.getHeader("Host")).andReturn("localhost");
    expect(getJsProcessorMock.process(isA(JsRequest.class), isA(JsResponseBuilder.class))).andAnswer(
        new IAnswer<Boolean>() {
          public Boolean answer() throws Throwable {
            JsResponseBuilder builder = (JsResponseBuilder)getCurrentArguments()[1];
            builder.appendAllJs(response.getAllJsContent());
            return true;
          }
        });
    replay();

    servlet.doGet(request, recorder);
    assertEquals(HttpServletResponse.SC_OK, recorder.getHttpStatusCode());
    assertEquals(EXAMPLE_JS_CODE, recorder.getResponseAsString());
    verify();
  }

  @Test
  public void testDoJsloadNormal() throws Exception {
    setUp(0);
    String url = "http://localhost/gadgets/js/feature.js?v=abc&nocache=0&onload=" + ONLOAD_PARAM;
    JsUri jsUri = mockJsUri(CONTAINER_PARAM, RenderingContext.CONTAINER, true, true, false,
        ONLOAD_PARAM, REFRESH_INTERVAL_SEC, UriStatus.VALID_VERSIONED);

    expect(jsUriManagerMock.processExternJsUri(isA(Uri.class))).andReturn(jsUri);
    expect(jsUriManagerMock.makeExternJsUri(isA(JsUri.class)))
        .andReturn(Uri.parse(url));
    httpUtilMock.setCachingHeaders(recorder, REFRESH_INTERVAL_SEC, false);
    replay();

    servlet.doGet(request, recorder);
    assertEquals(HttpServletResponse.SC_OK, recorder.getHttpStatusCode());
    assertEquals(String.format(JsLoadProcessor.JSLOAD_JS_TPL, url + "&jsload=0"),
        recorder.getResponseAsString());
    verify();
  }

  @Test
  public void testDoJsloadWithJsLoadTimeout() throws Exception {
    setUp(TIMEOUT_SEC); // Enable JS load timeout.

    String url = "http://localhost/gadgets/js/feature.js?v=abc&nocache=0&onload=" + ONLOAD_PARAM;
    JsUri jsUri = mockJsUri(CONTAINER_PARAM, RenderingContext.CONTAINER, true, true,
        false, ONLOAD_PARAM, -1, UriStatus.VALID_VERSIONED); // Disable refresh interval.

    expect(jsUriManagerMock.processExternJsUri(isA(Uri.class))).andReturn(jsUri);
    expect(jsUriManagerMock.makeExternJsUri(isA(JsUri.class)))
        .andReturn(Uri.parse(url));
    httpUtilMock.setCachingHeaders(recorder, TIMEOUT_SEC, false);
    replay();

    servlet.doGet(request, recorder);
    assertEquals(HttpServletResponse.SC_OK, recorder.getHttpStatusCode());
    assertEquals(String.format(JsLoadProcessor.JSLOAD_JS_TPL, url + "&jsload=0"),
        recorder.getResponseAsString());
    verify();
  }

  @Test
  public void testDoJsloadNoOnload() throws Exception {
    setUp(0);
    JsUri jsUri = mockJsUri(CONTAINER_PARAM, RenderingContext.CONTAINER, true, true, false,
        null, // lacks &onload=
        REFRESH_INTERVAL_SEC, UriStatus.VALID_VERSIONED);
    expect(jsUriManagerMock.processExternJsUri(isA(Uri.class))).andReturn(jsUri);
    replay();

    servlet.doGet(request, recorder);
    assertEquals(HttpServletResponse.SC_BAD_REQUEST, recorder.getHttpStatusCode());
    assertEquals(JsLoadProcessor.JSLOAD_ONLOAD_ERROR, recorder.getResponseAsString());
    verify();
  }

  @Test
  public void testDoJsloadNoCache() throws Exception {
    setUp(0);
    String url = "http://localhost/gadgets/js/feature.js?v=abc&nocache=1&onload=" + ONLOAD_PARAM;
    JsUri jsUri = mockJsUri(CONTAINER_PARAM, RenderingContext.CONTAINER, true, true,
        true, // Set to no cache.
        ONLOAD_PARAM, REFRESH_INTERVAL_SEC, UriStatus.VALID_VERSIONED);

    expect(jsUriManagerMock.processExternJsUri(isA(Uri.class))).andReturn(jsUri);
    expect(jsUriManagerMock.makeExternJsUri(isA(JsUri.class)))
        .andReturn(Uri.parse(url));
    httpUtilMock.setCachingHeaders(recorder, 0, false); // TTL of 0 is set.
    replay();

    servlet.doGet(request, recorder);
    assertEquals(HttpServletResponse.SC_OK, recorder.getHttpStatusCode());
    assertEquals(String.format(JsLoadProcessor.JSLOAD_JS_TPL, url + "&jsload=0"),
        recorder.getResponseAsString());
    verify();
  }

  @Test
  public void testJsServletGivesErrorWhenJsResponseHasError() throws Exception {
    setUp(0);
    JsProcessor errorProcessor = new JsProcessor(){
      public boolean process(JsRequest jsRequest, JsResponseBuilder builder) throws JsException {
        builder.setStatusCode(HttpServletResponse.SC_NOT_FOUND);
        builder.addError("Something bad happened");
        return false;
      }};
    JsProcessorRegistry jsProcessorRegistry = new DefaultJsProcessorRegistry(ImmutableList.<JsProcessor> of(),
            ImmutableList.<JsProcessor> of(errorProcessor), ImmutableList.<JsProcessor> of());

    JsServingPipeline pipeline = new DefaultJsServingPipeline(jsProcessorRegistry);
    servlet.setJsServingPipeline(pipeline);
    replay();

    servlet.doGet(request, recorder);
    assertEquals(HttpServletResponse.SC_NOT_FOUND, recorder.getHttpStatusCode());
    assertEquals("Something bad happened", recorder.getResponseAsString());
    verify();
  }
}
