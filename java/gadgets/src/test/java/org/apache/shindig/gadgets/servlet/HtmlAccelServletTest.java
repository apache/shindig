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

import com.google.caja.util.Maps;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.UrlGenerator;
import org.apache.shindig.gadgets.UrlValidationStatus;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.apache.shindig.gadgets.render.Renderer;
import org.apache.shindig.gadgets.render.RenderingResults;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.Map;

public class HtmlAccelServletTest extends ServletTestFixture {

  private static final String REWRITE_CONTENT = "working rewrite";
  
  private HtmlAccelServlet servlet;
  private Renderer renderer;

  @Before
  public void setUp() throws Exception {
    servlet = new HtmlAccelServlet();
    servlet.setRequestPipeLine(pipeline);
    servlet.setUrlGenerator(new FakeUrlGenerator());
    renderer = mock(Renderer.class);
    servlet.setRenderer(renderer);
  }

  @Test
  public void testHtmlAccelNoData() throws Exception {
    String url = "http://example.org/data.html";
    
    HttpRequest req = new HttpRequest(Uri.parse(url));
    expect(pipeline.execute(req)).andReturn(null).once();
    expect(request.getParameter("url")).andReturn(url).once();
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
    expect(request.getParameter("url")).andReturn(url).once();
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
    expect(request.getParameter("url")).andReturn(url).once();
    expect(request.getRequestURL()).andReturn(new StringBuffer("gmodules.com/gadgets/accel")).once();
    expect(request.getQueryString()).andReturn("url=" + url).once();
    expect(renderer.render(isA(GadgetContext.class)))
        .andReturn(RenderingResults.ok(REWRITE_CONTENT));
    replay();
    
    servlet.doGet(request, recorder);
    verify();
    assertEquals(REWRITE_CONTENT, recorder.getResponseAsString());
    assertEquals(200, recorder.getHttpStatusCode());
  }

  @Test
  public void testHtmlAccelParams() throws Exception {

    Renderer newRenderer = new Renderer(null, null, null, lockedDomainService) {
      @Override
      public RenderingResults render(GadgetContext context) {
        assertTrue(HtmlAccelServlet.ACCEL_GADGET_PARAM_VALUE == 
          context.getParameter(HtmlAccelServlet.ACCEL_GADGET_PARAM_NAME));
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
    expect(request.getParameter("url")).andReturn(url).once();
    expect(request.getRequestURL()).andReturn(new StringBuffer("gmodules.com/gadgets/accel")).once();
    expect(request.getQueryString()).andReturn("url=" + url).once();
    replay();
   
    servlet.doGet(request, recorder);
    verify();
  }
  
  private static class FakeUrlGenerator implements UrlGenerator {

    public UrlValidationStatus validateJsUrl(String url) {
      throw new UnsupportedOperationException();
    }

    public String getIframeUrl(Gadget gadget) {
      throw new UnsupportedOperationException();
    }

    public UrlValidationStatus validateIframeUrl(String url) {
      return UrlValidationStatus.VALID_UNVERSIONED;
    }

    public String getBundledJsUrl(Collection<String> features, GadgetContext context) {
      throw new UnsupportedOperationException();
    }

    public String getGadgetDomainOAuthCallback(String container, String gadgetHost) {
      throw new UnsupportedOperationException();
    }
  }


}
