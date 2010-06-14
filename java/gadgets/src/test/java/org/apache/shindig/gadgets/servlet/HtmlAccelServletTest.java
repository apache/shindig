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

import com.google.common.collect.Maps;

import org.apache.commons.lang.StringUtils;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.config.AbstractContainerConfig;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.apache.shindig.gadgets.rewrite.CaptureRewriter;
import org.apache.shindig.gadgets.rewrite.DefaultResponseRewriterRegistry;
import org.apache.shindig.gadgets.rewrite.ResponseRewriter;
import org.apache.shindig.gadgets.uri.AccelUriManager;
import static org.easymock.EasyMock.expect;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;

public class HtmlAccelServletTest extends ServletTestFixture {

  private static class FakeContainerConfig extends AbstractContainerConfig {
    protected final Map<String, Object> data = Maps.newHashMap();

    @Override
    public Object getProperty(String container, String name) {
      return data.get(name);
    }
  }

  private class FakeCaptureRewriter extends CaptureRewriter {
    String contentToRewrite;

    public void setContentToRewrite(String s) {
      contentToRewrite = s;
    }
    @Override
    public void rewrite(HttpRequest request, HttpResponseBuilder original) {
      super.rewrite(request, original);
      if (!StringUtils.isEmpty(contentToRewrite)) {
        original.setResponse(contentToRewrite.getBytes());
      }
    }
  }

  private static final String REWRITE_CONTENT = "working rewrite";
  private static final String SERVLET = "/gadgets/accel";
  private HtmlAccelServlet servlet;

  @Before
  public void setUp() throws Exception {
    servlet = new HtmlAccelServlet();
    AccelUriManager accelUriManager = new AccelUriManager(
        new FakeContainerConfig(), null);

    rewriter = new FakeCaptureRewriter();
    rewriterRegistry = new DefaultResponseRewriterRegistry(
        Arrays.<ResponseRewriter>asList(rewriter), null);
    servlet.setHandler(new AccelHandler(pipeline, rewriterRegistry,
                                        accelUriManager));
  }

  @Test
  public void testHtmlAccelNoData() throws Exception {
    String url = "http://example.org/data.html";

    HttpRequest req = new HttpRequest(Uri.parse(url));
    expect(pipeline.execute(req)).andReturn(null).once();
    expectRequest("", url);
    replay();

    servlet.doGet(request, recorder);
    verify();
    assertEquals("Error fetching data", recorder.getResponseAsString());
    assertEquals(400, recorder.getHttpStatusCode());
  }

  @Test
  public void testHtmlAccelNoHtml() throws Exception {
    String url = "http://example.org/data.xml";
    String data = "<html><body>Hello World</body></html>";

    HttpRequest req = new HttpRequest(Uri.parse(url));
    HttpResponse resp = new HttpResponseBuilder()
        .setResponse(data.getBytes())
        .setHeader("Content-Type", "text/xml")
        .setHttpStatusCode(200)
        .create();
    expect(pipeline.execute(req)).andReturn(resp).once();
    expectRequest("", url);
    replay();

    servlet.doGet(request, recorder);
    verify();
    assertEquals(data, recorder.getResponseAsString());
  }

  @Test
  public void testHtmlAccelRewriteSimple() throws Exception {
    String url = "http://example.org/data.html";
    String data = "<html><body>Hello World</body></html>";

    ((FakeCaptureRewriter) rewriter).setContentToRewrite(REWRITE_CONTENT);
    HttpRequest req = new HttpRequest(Uri.parse(url));
    HttpResponse resp = new HttpResponseBuilder()
        .setResponse(data.getBytes())
        .setHeader("Content-Type", "text/html")
        .setHttpStatusCode(200)
        .create();
    expect(pipeline.execute(req)).andReturn(resp).once();
    expectRequest("", url);
    replay();

    servlet.doGet(request, recorder);
    verify();
    assertEquals(REWRITE_CONTENT, recorder.getResponseAsString());
    assertEquals(200, recorder.getHttpStatusCode());
    assertTrue(rewriter.responseWasRewritten());
  }

  @Test
  public void testHtmlAccelRewriteErrorCode() throws Exception {
    String url = "http://example.org/data.html";
    String data = "<html><body>This is error page</body></html>";

    ((FakeCaptureRewriter) rewriter).setContentToRewrite(REWRITE_CONTENT);
    HttpRequest req = new HttpRequest(Uri.parse(url));
    HttpResponse resp = new HttpResponseBuilder()
        .setResponse(data.getBytes())
        .setHeader("Content-Type", "text/html")
        .setHttpStatusCode(404)
        .create();
    expect(pipeline.execute(req)).andReturn(resp).once();
    expectRequest("", url);
    replay();

    servlet.doGet(request, recorder);
    verify();
    assertEquals(AccelHandler.ERROR_FETCHING_DATA, recorder.getResponseAsString());
    assertEquals(404, recorder.getHttpStatusCode());
    assertFalse(rewriter.responseWasRewritten());
  }

  @Test
  public void testHtmlAccelRewriteInternalError() throws Exception {
    String url = "http://example.org/data.html";
    String data = "<html><body>This is error page</body></html>";

    ((FakeCaptureRewriter) rewriter).setContentToRewrite(data);
    HttpRequest req = new HttpRequest(Uri.parse(url));
    HttpResponse resp = new HttpResponseBuilder()
        .setResponse(data.getBytes())
        .setHeader("Content-Type", "text/html")
        .setHttpStatusCode(500)
        .create();
    expect(pipeline.execute(req)).andReturn(resp).once();
    expectRequest("", url);
    replay();

    servlet.doGet(request, recorder);
    verify();
    assertEquals(AccelHandler.ERROR_FETCHING_DATA, recorder.getResponseAsString());
    assertEquals(502, recorder.getHttpStatusCode());
    assertFalse(rewriter.responseWasRewritten());
  }

  private void expectRequest(String extraPath, String url) {
    expect(request.getServletPath()).andReturn(SERVLET).anyTimes();
    expect(request.getScheme()).andReturn("http").anyTimes();
    expect(request.getServerName()).andReturn("apache.org").anyTimes();
    expect(request.getServerPort()).andReturn(-1).anyTimes();
    expect(request.getRequestURI()).andReturn(SERVLET + extraPath).anyTimes();
    expect(request.getRequestURL())
        .andReturn(new StringBuffer("apache.org" + SERVLET + extraPath))
        .anyTimes();
    String queryParams = (url == null ? "" : "url=" + url + "&container=accel");
    expect(request.getQueryString()).andReturn(queryParams).anyTimes();
  }
}
