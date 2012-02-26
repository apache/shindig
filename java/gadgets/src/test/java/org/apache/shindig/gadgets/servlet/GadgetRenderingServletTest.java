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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.apache.shindig.common.servlet.HttpServletResponseRecorder;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.render.Renderer;
import org.apache.shindig.gadgets.render.RenderingResults;
import org.apache.shindig.gadgets.uri.IframeUriManager;
import org.apache.shindig.gadgets.uri.UriStatus;
import org.apache.shindig.gadgets.uri.UriCommon.Param;

import org.easymock.IMocksControl;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class GadgetRenderingServletTest {
  private static final String NON_ASCII_STRING
      = "Games, HQ, Mang\u00E1, Anime e tudo que um bom nerd ama";

  private final IMocksControl control = EasyMock.createNiceControl();
  private final HttpServletRequest request = makeRequestMock(this);
  private final HttpServletResponse response = control.createMock(HttpServletResponse.class);
  private final Renderer renderer = control.createMock(Renderer.class);
  public final HttpServletResponseRecorder recorder = new HttpServletResponseRecorder(response);
  private final GadgetRenderingServlet servlet = new GadgetRenderingServlet();
  private final IframeUriManager iframeUriManager = control.createMock(IframeUriManager.class);

  @Before
  public void setUpUrlGenerator() {
    expect(iframeUriManager.validateRenderingUri(isA(Uri.class))).andReturn(UriStatus.VALID_UNVERSIONED);
    expect(request.getRequestURL()).andReturn(new StringBuffer("http://foo.com"));
    expect(request.getQueryString()).andReturn("?q=a");
    servlet.setIframeUriManager(iframeUriManager);
  }

  @Test
  public void dosHeaderRejected() throws Exception {
    expect(request.getHeader(HttpRequest.DOS_PREVENTION_HEADER)).andReturn("foo");
    control.replay();
    servlet.doGet(request, recorder);

    assertEquals(HttpServletResponse.SC_FORBIDDEN, recorder.getHttpStatusCode());
  }

  @Test
  public void renderWithTtl() throws Exception {
    servlet.setRenderer(renderer);
    expect(renderer.render(isA(GadgetContext.class)))
        .andReturn(RenderingResults.ok("working"));
    expect(request.getParameter(Param.REFRESH.getKey())).andReturn("120");
    control.replay();
    servlet.doGet(request, recorder);
    assertEquals("private,max-age=120", recorder.getHeader("Cache-Control"));
  }

  @Test
  public void renderWithBadTtl() throws Exception {
    servlet.setRenderer(renderer);
    expect(renderer.render(isA(GadgetContext.class)))
        .andReturn(RenderingResults.ok("working"));
    expect(request.getParameter(Param.REFRESH.getKey())).andReturn("");
    control.replay();
    servlet.doGet(request, recorder);
    assertEquals("private,max-age=300", recorder.getHeader("Cache-Control"));
  }

  @Test
  public void normalResponse() throws Exception {
    servlet.setRenderer(renderer);
    expect(renderer.render(isA(GadgetContext.class)))
        .andReturn(RenderingResults.ok("working"));
    control.replay();

    servlet.doGet(request, recorder);

    assertEquals(HttpServletResponse.SC_OK, recorder.getHttpStatusCode());
    assertEquals("private,max-age=" + GadgetRenderingServlet.DEFAULT_CACHE_TTL,
        recorder.getHeader("Cache-Control"));
    assertEquals("working", recorder.getResponseAsString());
  }

  @Test
  public void errorsPassedThrough() throws Exception {
    servlet.setRenderer(renderer);
    expect(renderer.render(isA(GadgetContext.class)))
        .andReturn(RenderingResults.error("busted", HttpServletResponse.SC_INTERNAL_SERVER_ERROR));
    control.replay();

    servlet.doGet(request, recorder);

    assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, recorder.getHttpStatusCode());
    assertNull("Cache-Control header passed where it should not be.",
        recorder.getHeader("Cache-Control"));
    assertEquals("busted", recorder.getResponseAsString());

  }

  @Test
  public void errorsAreEscaped() throws Exception {
    servlet.setRenderer(renderer);
    expect(renderer.render(isA(GadgetContext.class)))
        .andReturn(RenderingResults.error("busted<script>alert(document.domain)</script>",
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR));
    control.replay();

    servlet.doGet(request, recorder);

    assertEquals("busted&lt;script&gt;alert(document.domain)&lt;/script&gt;",
        recorder.getResponseAsString());

    assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, recorder.getHttpStatusCode());
  }

  @Test
  public void outputEncodingIsUtf8() throws Exception {
    servlet.setRenderer(renderer);
    expect(renderer.render(isA(GadgetContext.class)))
        .andReturn(RenderingResults.ok(NON_ASCII_STRING));
    control.replay();

    servlet.doGet(request, recorder);


    assertEquals("UTF-8", recorder.getCharacterEncoding());
    assertEquals("text/html", recorder.getContentType());
    assertEquals(NON_ASCII_STRING, recorder.getResponseAsString());
  }

  @Test
  public void refreshParameter_specified() throws Exception {
    servlet.setRenderer(renderer);
    expect(request.getParameter("refresh")).andReturn("1000");
    expect(renderer.render(isA(GadgetContext.class)))
        .andReturn(RenderingResults.ok("working"));
    control.replay();
    servlet.doGet(request, recorder);
    assertEquals("private,max-age=1000", recorder.getHeader("Cache-Control"));
  }

  @Test
  public void refreshParameter_default() throws Exception {
    servlet.setRenderer(renderer);
    expect(renderer.render(isA(GadgetContext.class)))
        .andReturn(RenderingResults.ok("working"));
    control.replay();
    servlet.doGet(request, recorder);
    assertEquals("private,max-age=300", recorder.getHeader("Cache-Control"));
  }

  private static HttpServletRequest makeRequestMock(GadgetRenderingServletTest testcase) {
    HttpServletRequest req = testcase.control.createMock(HttpServletRequest.class);
    expect(req.getScheme()).andReturn("http").anyTimes();
    expect(req.getServerPort()).andReturn(80).anyTimes();
    expect(req.getServerName()).andReturn("example.com").anyTimes();
    expect(req.getRequestURI()).andReturn("/path").anyTimes();
    return req;
  }
}
