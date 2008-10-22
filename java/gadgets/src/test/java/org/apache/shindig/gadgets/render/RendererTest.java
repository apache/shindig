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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.apache.shindig.common.ContainerConfigException;
import org.apache.shindig.common.JsonContainerConfig;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.LockedDomainService;
import org.apache.shindig.gadgets.process.ProcessingException;
import org.apache.shindig.gadgets.process.Processor;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.View;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

/**
 * Tests for Renderer.
 */
public class RendererTest {
  private static final Uri SPEC_URL = Uri.parse("http://example.org/gadget.xml");
  private static final Uri TYPE_URL_HREF = Uri.parse("http://example.org/gadget.php");
  private static final String BASIC_HTML_CONTENT = "Hello, World!";
  private static final String GADGET =
      "<Module>" +
      " <ModulePrefs title='foo'/>" +
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
    return new GadgetContext() {
      @Override
      public String getView() {
        return view;
      }

      @Override
      public String getParameter(String name) {
        if (name.equals("parent")) {
          return "http://example.org/foo";
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
  public void handlesProcessingExceptionGracefully() {
    processor.exception = new ProcessingException("foo");
    RenderingResults results = renderer.render(makeContext("html"));
    assertEquals(RenderingResults.Status.ERROR, results.getStatus());
    assertEquals("foo", results.getErrorMessage());
  }

  @Test
  public void handlesRenderingExceptionGracefully() {
    htmlRenderer.exception = new RenderingException("oh no!");
    RenderingResults results = renderer.render(makeContext("html"));
    assertEquals(RenderingResults.Status.ERROR, results.getStatus());
    assertEquals("oh no!", results.getErrorMessage());
  }

  @Test
  public void validateParent() throws Exception {
    containerConfig.json.put("gadgets.parent",
        new JSONArray(Arrays.asList("http:\\/\\/example\\.org\\/[a-z]+", "localhost")));

    RenderingResults results = renderer.render(makeContext("html"));
    assertEquals(RenderingResults.Status.OK, results.getStatus());
  }

  @Test
  public void validateBadParent() throws Exception {
    containerConfig.json.put("gadgets.parent",
        new JSONArray(Arrays.asList("http:\\/\\/example\\.com\\/[a-z]+", "localhost")));
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
  public void wrongDomainRedirects() throws Exception {
    lockedDomainService.canRender = false;
    RenderingResults results = renderer.render(makeContext("html"));
    assertEquals(RenderingResults.Status.MUST_REDIRECT, results.getStatus());
    // TODO: Verify the real url for redirection.
    assertNull(results.getRedirect());
  }

  private static class FakeContainerConfig extends JsonContainerConfig {
    private final JSONObject json = new JSONObject();

    public FakeContainerConfig() throws ContainerConfigException {
      super(null);
    }

    @Override
    public Object getJson(String container, String parameter) {
      return json.opt(parameter);
    }
  }

  private static class FakeHtmlRenderer extends HtmlRenderer {
    private RenderingException exception;

    public FakeHtmlRenderer() {
      super(null, null, null);
    }

    @Override
    public String render(Gadget gadget) throws RenderingException {
      if (exception != null) {
        throw exception;
      }
      return gadget.getCurrentView().getContent();
    }
  }

  private static class FakeProcessor extends Processor {
    private ProcessingException exception;

    public FakeProcessor() {
      super(null, null, null, null);
    }

    @Override
    public Gadget process(GadgetContext context) throws ProcessingException {
      if (exception != null) {
        throw exception;
      }

      try {
        GadgetSpec spec = new GadgetSpec(SPEC_URL, GADGET);
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
    private boolean wasChecked = false;
    private boolean canRender = true;
    public boolean gadgetCanRender(String host, GadgetSpec gadget, String container) {
      wasChecked = true;
      return canRender;
    }

    public String getLockedDomainForGadget(GadgetSpec gadget, String container) {
      return null;
    }

    public boolean isSafeForOpenProxy(String host) {
      return false;
    }
  }
}
