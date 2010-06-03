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

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;

import com.google.common.collect.Maps;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.apache.shindig.gadgets.render.Renderer;
import org.apache.shindig.gadgets.render.RenderingResults;
import org.apache.shindig.gadgets.uri.IframeUriManager;
import org.apache.shindig.gadgets.uri.UriStatus;
import org.easymock.Capture;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

public class HtmlAccelServletTest extends ServletTestFixture {

  private static final String REWRITE_CONTENT = "working rewrite";
  private static final String SERVLET = "/gadgets/accel";
  private HtmlAccelServlet servlet;
  private Renderer renderer;

  @Before
  public void setUp() throws Exception {
    servlet = new HtmlAccelServlet();
    servlet.setRequestPipeline(pipeline);
    servlet.setIframeUriManager(new FakeIframeUriManager());
    renderer = mock(Renderer.class);
    servlet.setRenderer(renderer);
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
    
    HttpRequest req = new HttpRequest(Uri.parse(url));
    HttpResponse resp = new HttpResponseBuilder()
        .setResponse(data.getBytes())
        .setHeader("Content-Type", "text/html")
        .setHttpStatusCode(200)
        .create();
    expect(pipeline.execute(req)).andReturn(resp).once();
    expectRequest("", url);
    expect(renderer.render(isA(GadgetContext.class)))
        .andReturn(RenderingResults.ok(REWRITE_CONTENT));
    replay();
    
    servlet.doGet(request, recorder);
    verify();
    assertEquals(REWRITE_CONTENT, recorder.getResponseAsString());
    assertEquals(200, recorder.getHttpStatusCode());
  }

  @Test
  public void testHtmlAccelRewriteChain() throws Exception {
    String url = "http://example.org/data.html?id=1";
    String data = "<html><body>Hello World</body></html>";
    
    Capture<HttpRequest> reqCapture = new Capture<HttpRequest>();
    HttpResponse resp = new HttpResponseBuilder()
        .setResponse(data.getBytes())
        .setHeader("Content-Type", "text/html")
        .setCacheTtl(567)
        .setHttpStatusCode(200)
        .create();
    expect(pipeline.execute(capture(reqCapture))).andReturn(resp).once();
    expectRequest("//" + url, null);
    expect(renderer.render(isA(GadgetContext.class)))
        .andReturn(RenderingResults.ok(REWRITE_CONTENT));
    replay();
    
    servlet.doGet(request, recorder);
    verify();
    HttpRequest req = reqCapture.getValue();
    assertEquals(url, req.getUri().toString());
    assertEquals("accel", req.getContainer());
    assertEquals(REWRITE_CONTENT, recorder.getResponseAsString());
    assertEquals(200, recorder.getHttpStatusCode());
    assertTrue(recorder.getHeader("Cache-Control").equals("private,max-age=566") 
        || recorder.getHeader("Cache-Control").equals("private,max-age=567"));
    // Note: due to rounding (MS to S conversion), ttl is down by 1
  }

  @Test
  public void testHtmlAccelRewriteChainParams() throws Exception {
    String url = "http://example.org/data.html?id=1";
    String data = "<html><body>Hello World</body></html>";
    
    HttpResponse resp = new HttpResponseBuilder()
        .setResponse(data.getBytes())
        .setHeader("Content-Type", "text/html")
        .setHttpStatusCode(200)
        .create();
    Capture<HttpRequest> reqCapture = new Capture<HttpRequest>();
    expect(pipeline.execute(capture(reqCapture))).andReturn(resp).once();
    expectRequest("/container=open&refresh=3600/" + url, null);
    expect(renderer.render(isA(GadgetContext.class)))
        .andReturn(RenderingResults.ok(REWRITE_CONTENT));
    replay();
    
    servlet.doGet(request, recorder);
    verify();
    HttpRequest req = reqCapture.getValue();
    assertEquals(url, req.getUri().toString());
    assertEquals("open", req.getContainer());
    assertEquals(REWRITE_CONTENT, recorder.getResponseAsString());
    assertEquals(200, recorder.getHttpStatusCode());
    assertEquals("private,max-age=3600", recorder.getHeader("Cache-Control"));
  }

  @Test
  public void testHtmlAccelRewriteErrorCode() throws Exception {
    String url = "http://example.org/data.html";
    String data = "<html><body>This is error page</body></html>";
    
    HttpRequest req = new HttpRequest(Uri.parse(url));
    HttpResponse resp = new HttpResponseBuilder()
        .setResponse(data.getBytes())
        .setHeader("Content-Type", "text/html")
        .setHttpStatusCode(404)
        .create();
    expect(pipeline.execute(req)).andReturn(resp).once();
    expectRequest("", url);
    expect(renderer.render(isA(GadgetContext.class)))
        .andReturn(RenderingResults.ok(REWRITE_CONTENT));
    replay();
    
    servlet.doGet(request, recorder);
    verify();
    assertEquals(REWRITE_CONTENT, recorder.getResponseAsString());
    assertEquals(404, recorder.getHttpStatusCode());
  }

  @Test
  public void testHtmlAccelRewriteInternalError() throws Exception {
    String url = "http://example.org/data.html";
    String data = "<html><body>This is error page</body></html>";
    
    HttpRequest req = new HttpRequest(Uri.parse(url));
    HttpResponse resp = new HttpResponseBuilder()
        .setResponse(data.getBytes())
        .setHeader("Content-Type", "text/html")
        .setHttpStatusCode(500)
        .create();
    expect(pipeline.execute(req)).andReturn(resp).once();
    expectRequest("", url);
    expect(renderer.render(isA(GadgetContext.class)))
        .andReturn(RenderingResults.ok(REWRITE_CONTENT));
    replay();
    
    servlet.doGet(request, recorder);
    verify();
    assertEquals(REWRITE_CONTENT, recorder.getResponseAsString());
    assertEquals(502, recorder.getHttpStatusCode());
  }

  @Test
  public void testHtmlAccelParams() throws Exception {

    Renderer newRenderer = new Renderer(null, null, null, lockedDomainService) {
      @Override
      public RenderingResults render(GadgetContext context) {
        assertTrue(HtmlAccelServlet.isAccel(context));
        assertEquals("accel", context.getParameter("container"));
        return RenderingResults.ok(REWRITE_CONTENT);
      }
    };
    servlet.setRenderer(newRenderer);
    Map<String,String> paramMap = Maps.newHashMap();
    paramMap.put("container","accel");
    servlet.setAddedServletParams(paramMap);
    
    String url = "http://example.org/data.html";
    
    HttpRequest req = new HttpRequest(Uri.parse(url));
    HttpResponse resp = new HttpResponseBuilder()
        .setHeader("Content-Type", "text/html")
        .setHttpStatusCode(200)
        .create();
    expect(pipeline.execute(req)).andReturn(resp).once();
    expectRequest("", url);
    replay();
   
    servlet.doGet(request, recorder);
    verify();
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
    String queryParams = (url == null ? "" : "url=" + url);
    expect(request.getQueryString()).andReturn(queryParams).anyTimes();
  }

  private static class FakeIframeUriManager implements IframeUriManager {
    public UriStatus validateRenderingUri(Uri uri) {
      return UriStatus.VALID_UNVERSIONED;
    }

    public Uri makeRenderingUri(Gadget gadget) {
      throw new UnsupportedOperationException();
    }
  }


}
