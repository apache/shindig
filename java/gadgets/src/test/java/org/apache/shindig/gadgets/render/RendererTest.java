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
package org.apache.shindig.gadgets.render;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.config.BasicContainerConfig;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.LockedDomainService;
import org.apache.shindig.gadgets.process.ProcessingException;
import org.apache.shindig.gadgets.process.Processor;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.View;

import com.google.common.collect.Maps;

import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Map;

/**
 * Tests for Renderer.
 */
public class RendererTest {
  protected static final Uri SPEC_URL = Uri.parse("http://example.org/gadget.xml");
  private static final Uri TYPE_URL_HREF = Uri.parse("http://example.org/gadget.php");
  private static final String BASIC_HTML_CONTENT = "Hello, World!";
  protected static final String GADGET =
      "<Module>" +
      " <ModulePrefs title='foo'/>" +
      " <Content view='html' type='html'>" + BASIC_HTML_CONTENT + "</Content>" +
      " <Content view='url' type='url' href='" + TYPE_URL_HREF + "'/>" +
      "</Module>";
  protected static final String GADGET_CAJA =
    "<Module>" +
    " <ModulePrefs title='foo'>" +
    "   <Require feature='caja'/>" +
    " </ModulePrefs>" +
    " <Content view='html' type='html'>" + BASIC_HTML_CONTENT + "</Content>" +
    " <Content view='url' type='url' href='" + TYPE_URL_HREF + "'/>" +
    "</Module>";

  private final FakeHtmlRenderer htmlRenderer = new FakeHtmlRenderer();
  private final FakeProcessor processor = new FakeProcessor();
  private final FakeLockedDomainService lockedDomainService =  new FakeLockedDomainService();
  private FakeContainerConfig containerConfig;
  private Renderer renderer;

  @Before
  public void setUp() throws Exception {
    containerConfig = new FakeContainerConfig();
    renderer = new Renderer(processor, htmlRenderer, containerConfig, lockedDomainService);
  }

  private GadgetContext makeContext(final String view) {
    return makeContext(view, null, null);
  }

  private GadgetContext makeContext(final String view, final String sanitize, final String caja) {
    return new GadgetContext() {
      @Override
      public String getView() {
        return view;
      }

      @Override
      public String getParameter(String name) {
        if (name.equals("parent")) {
          return "http://example.org/foo";
        } else if (name.equals("sanitize")) {
          return sanitize;
        } else if (name.equals("caja")) {
          return caja;
        }
        return null;
      }
    };
  }

  @Test
  public void renderTypeHtml() {
    RenderingResults results = renderer.render(makeContext("html"));
    assertEquals(RenderingResults.Status.OK, results.getStatus());
    assertEquals(BASIC_HTML_CONTENT, results.getContent());
  }

  @Test
  public void renderTypeUrl() {
    RenderingResults results = renderer.render(makeContext("url"));
    assertEquals(RenderingResults.Status.MUST_REDIRECT, results.getStatus());
    assertEquals(TYPE_URL_HREF, results.getRedirect());
  }

  @Test
  public void renderTypeUrlRequiresCajaIncompatible() {
    processor.setGadgetData(GADGET_CAJA);
    RenderingResults results = renderer.render(makeContext("url"));
    assertEquals(RenderingResults.Status.ERROR, results.getStatus());
    assertEquals(HttpServletResponse.SC_BAD_REQUEST, results.getHttpStatusCode());
  }

  @Test
  public void renderTypeUrlCajaParamIncompatible() {
    RenderingResults results = renderer.render(makeContext("url", null, "1"));
    assertEquals(RenderingResults.Status.ERROR, results.getStatus());
    assertEquals(HttpServletResponse.SC_BAD_REQUEST, results.getHttpStatusCode());
  }

  @Test
  public void renderTypeUrlSanitizedIncompatible() {
    RenderingResults results = renderer.render(makeContext("url", "1", null));
    assertEquals(RenderingResults.Status.ERROR, results.getStatus());
    assertEquals(HttpServletResponse.SC_BAD_REQUEST, results.getHttpStatusCode());
  }

  @Test
  public void handlesProcessingExceptionGracefully() {
    processor.exception = new ProcessingException("foo", HttpServletResponse.SC_FORBIDDEN);
    RenderingResults results = renderer.render(makeContext("html"));
    assertEquals(RenderingResults.Status.ERROR, results.getStatus());
    assertEquals("foo", results.getErrorMessage());
    assertEquals(HttpServletResponse.SC_FORBIDDEN, results.getHttpStatusCode());
  }

  @Test
  public void handlesRenderingExceptionGracefully() {
    htmlRenderer.exception = new RenderingException("four-oh-four", HttpServletResponse.SC_NOT_FOUND);
    RenderingResults results = renderer.render(makeContext("html"));
    assertEquals(RenderingResults.Status.ERROR, results.getStatus());
    assertEquals("four-oh-four", results.getErrorMessage());
    assertEquals(HttpServletResponse.SC_NOT_FOUND, results.getHttpStatusCode());
  }

  @Test
  public void handlesRuntimeWrappedGadgetExceptionGracefully() {
    htmlRenderer.runtimeException = new RuntimeException(
        new GadgetException(GadgetException.Code.FAILED_TO_RETRIEVE_CONTENT, "oh no!"));
    RenderingResults results = renderer.render(makeContext("html"));
    assertEquals(RenderingResults.Status.ERROR, results.getStatus());
    assertEquals("oh no!", results.getErrorMessage());
  }

  @Test(expected = RuntimeException.class)
  public void otherRuntimeExceptionsThrow() {
    htmlRenderer.runtimeException = new RuntimeException("Help!");
    renderer.render(makeContext("html"));
  }

  @Test
  public void validateParent() throws Exception {
    containerConfig.data.put("gadgets.parent",
        Arrays.asList("http:\\/\\/example\\.org\\/[a-z]+", "localhost"));

    RenderingResults results = renderer.render(makeContext("html"));
    assertEquals(RenderingResults.Status.OK, results.getStatus());
  }

  @Test
  public void validateBadParent() throws Exception {
    containerConfig.data.put("gadgets.parent",
        Arrays.asList("http:\\/\\/example\\.com\\/[a-z]+", "localhost"));
    RenderingResults results = renderer.render(makeContext("html"));
    assertEquals(RenderingResults.Status.ERROR, results.getStatus());
    assertNotNull("No error message provided for bad parent.", results.getErrorMessage());
  }

  @Test
  public void handlesNoCurrentViewGracefully() throws Exception {
    RenderingResults results = renderer.render(makeContext("bad-view-name"));
    assertEquals(RenderingResults.Status.ERROR, results.getStatus());
    assertNotNull("No error message for missing current view", results.getErrorMessage());
  }

  @Test
  public void verifyLockedDomain() throws Exception {
    renderer.render(makeContext("html"));
    assertTrue("Locked domain not verified", lockedDomainService.wasChecked);
  }

  @Test
  public void wrongDomainFails() throws Exception {
    lockedDomainService.canRender = false;
    RenderingResults results = renderer.render(makeContext("html"));
    assertEquals(RenderingResults.Status.ERROR, results.getStatus());
  }

  private static class FakeContainerConfig extends BasicContainerConfig {
    protected final Map<String, Object> data = Maps.newHashMap();

    @Override
    public Object getProperty(String container, String name) {
      return data.get(name);
    }
  }

  private static class FakeHtmlRenderer extends HtmlRenderer {
    protected RenderingException exception;
    protected RuntimeException runtimeException;

    public FakeHtmlRenderer() {
      super(null, null, null, null);
    }

    @Override
    public String render(Gadget gadget) throws RenderingException {
      if (exception != null) {
        throw exception;
      }
      if (runtimeException != null) {
        throw runtimeException;
      }
      return gadget.getCurrentView().getContent();
    }
  }

  private static class FakeProcessor extends Processor {
    protected ProcessingException exception;
    private String gadgetData;

    public FakeProcessor() {
      super(null, null, null, null, null);
      this.gadgetData = GADGET;
    }

    public void setGadgetData(String gadgetData) {
      this.gadgetData = gadgetData;
    }

    @Override
    public Gadget process(GadgetContext context) throws ProcessingException {
      if (exception != null) {
        throw exception;
      }
      try {
        GadgetSpec spec = new GadgetSpec(SPEC_URL, gadgetData);
        View view = spec.getView(context.getView());
        return new Gadget()
            .setContext(context)
            .setSpec(spec)
            .setCurrentView(view);
      } catch (GadgetException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private static class FakeLockedDomainService implements LockedDomainService {
    protected boolean wasChecked = false;
    protected boolean canRender = true;

    protected FakeLockedDomainService() {
    }

    public boolean isGadgetValidForHost(String host, Gadget gadget, String container) {
      wasChecked = true;
      return canRender;
    }

    public String getLockedDomainForGadget(Gadget gadget, String container) {
      return null;
    }

    public boolean isSafeForOpenProxy(String host) {
      return false;
    }

    public boolean isEnabled() {
      return false;
    }

    public boolean isHostUsingLockedDomain(String host) {
      return false;
    }

    public boolean isRefererCheckEnabled() {
        return false;
    }
  }
}
