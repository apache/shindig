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

import org.apache.shindig.common.ContainerConfig;
import org.apache.shindig.common.ContainerConfigException;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.GadgetSpecFactory;
import org.apache.shindig.gadgets.spec.GadgetSpec;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.Arrays;

/**
 * Tests for RenderDispatcher.
 */
public class RenderDispatcherTest {
  private static final Uri SPEC_URL = Uri.parse("http://example.org/gadget.xml");
  private static final Uri TYPE_URL_HREF = Uri.parse("http://example.org/gadget.php");
  private static final String BASIC_HTML_CONTENT = "Hello, World!";
  private static final String GADGET =
      "<Module>" +
      " <ModulePrefs title='foo'/>" +
      " <Content view='html' type='html'>" + BASIC_HTML_CONTENT + "</Content>" +
      " <Content view='url' type='url' href='" + TYPE_URL_HREF + "'/>" +
      " <Content view='alias' type='html'>" + BASIC_HTML_CONTENT + "</Content>" +
      "</Module>";

  private final FakeHtmlRenderer htmlRenderer = new FakeHtmlRenderer();
  private final FakeGadgetSpecFactory gadgetSpecFactory = new FakeGadgetSpecFactory();
  private FakeContainerConfig containerConfig;
  private RenderDispatcher renderer;

  @Before
  public void setUp() throws Exception {
    containerConfig = new FakeContainerConfig();
    renderer = new RenderDispatcher(htmlRenderer, gadgetSpecFactory, containerConfig);
  }

  private GadgetContext makeContext(final String view, final Uri specUrl) {
    return new GadgetContext() {
      @Override
      public URI getUrl() {
        return specUrl.toJavaUri();
      }

      @Override
      public String getView() {
        return view;
      }
    };
  }

  @Test
  public void renderTypeHtml() {
    RenderingResults results = renderer.render(makeContext("html", SPEC_URL));
    assertEquals(RenderingResults.Status.OK, results.getStatus());
    assertEquals(BASIC_HTML_CONTENT, results.getContent());
  }

  @Test
  public void renderTypeUrl() {
    RenderingResults results = renderer.render(makeContext("url", SPEC_URL));
    assertEquals(RenderingResults.Status.MUST_REDIRECT, results.getStatus());
    assertEquals(TYPE_URL_HREF, results.getRedirect());
  }

  @Test
  public void renderInvalidUrl() {
    RenderingResults results = renderer.render(makeContext("url", Uri.parse("doesnotexist")));
    assertEquals(RenderingResults.Status.ERROR, results.getStatus());
    assertNotNull("No error message provided for invalid url.", results.getErrorMessage());
  }

  @Test
  public void doViewAliasing() throws Exception {
    JSONArray aliases = new JSONArray(Arrays.asList("some-alias", "alias"));
    containerConfig.json.put("gadgets.features/views/aliased/aliases", aliases);
    RenderingResults results = renderer.render(makeContext("aliased", SPEC_URL));
    assertEquals(RenderingResults.Status.OK, results.getStatus());
    assertEquals(BASIC_HTML_CONTENT, results.getContent());
  }

  @Test
  public void noSupportedViewThrows() {
    RenderingResults results = renderer.render(makeContext("not-real-view", SPEC_URL));
    assertEquals(RenderingResults.Status.ERROR, results.getStatus());
    assertNotNull("No error message provided for invalid view.", results.getErrorMessage());
  }

  @Test
  public void handlesGadgetExceptionGracefully() {
    gadgetSpecFactory.exception = new GadgetException(GadgetException.Code.INVALID_PATH, "foo");
    RenderingResults results = renderer.render(makeContext("does-not-matter", SPEC_URL));
    assertEquals(RenderingResults.Status.ERROR, results.getStatus());
    assertEquals("foo", results.getErrorMessage());
  }

  @Test
  public void handlesRenderingExceptionGracefully() {
    htmlRenderer.exception = new RenderingException("oh no!");
    RenderingResults results = renderer.render(makeContext("html", SPEC_URL));
    assertEquals(RenderingResults.Status.ERROR, results.getStatus());
    assertEquals("oh no!", results.getErrorMessage());
  }

  @Test
  public void validateParent() throws Exception {
    final String parent = "http://example.org/foo";
    GadgetContext context = new GadgetContext() {
      @Override
      public URI getUrl() {
        return SPEC_URL.toJavaUri();
      }

      @Override
      public String getView() {
        return "html";
      }

      @Override
      public String getParameter(String name) {
        if (name.equals("parent")) {
          return parent;
        }
        return null;
      }
    };

    containerConfig.json.put("gadgets.parent",
        new JSONArray(Arrays.asList("http:\\/\\/example\\.org\\/[a-z]+", "localhost")));

    RenderingResults results = renderer.render(context);
    assertEquals(RenderingResults.Status.OK, results.getStatus());
  }

  @Test
  public void validateBadParent() throws Exception {
    final String parent = "http://example.org/foo";
    GadgetContext context = new GadgetContext() {
      @Override
      public URI getUrl() {
        return SPEC_URL.toJavaUri();
      }

      @Override
      public String getParameter(String name) {
        if (name.equals("parent")) {
          return parent;
        }
        return null;
      }
    };

    containerConfig.json.put("gadgets.parent",
        new JSONArray(Arrays.asList("http:\\/\\/example\\.com\\/[a-z]+", "localhost")));

    RenderingResults results = renderer.render(context);
    assertEquals(RenderingResults.Status.ERROR, results.getStatus());
    assertNotNull("No error message provided for bad parent.", results.getErrorMessage());
  }

  private static class FakeGadgetSpecFactory implements GadgetSpecFactory {
    private GadgetException exception;
    public GadgetSpec getGadgetSpec(GadgetContext context) throws GadgetException {
      if (exception != null) {
        throw exception;
      }
      return new GadgetSpec(context.getUrl(), GADGET);
    }

    public GadgetSpec getGadgetSpec(URI uri, boolean ignoreCache) {
      throw new UnsupportedOperationException();
    }
  }

  private static class FakeContainerConfig extends ContainerConfig {
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
      super(null, null);
    }

    @Override
    public String render(Gadget gadget) throws RenderingException {
      if (exception != null) {
        throw exception;
      }
      return gadget.getCurrentView().getContent();
    }
  }
}
