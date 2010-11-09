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
import static org.junit.Assert.assertTrue;

import org.apache.shindig.auth.AnonymousSecurityToken;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.xml.XmlUtil;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.preload.PreloadedData;
import org.apache.shindig.gadgets.preload.PreloaderService;
import org.apache.shindig.gadgets.rewrite.CaptureRewriter;
import org.apache.shindig.gadgets.rewrite.GadgetRewriter;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.View;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.Callable;

import com.google.common.collect.ImmutableList;

/**
 * Tests for HtmlRenderer
 */
public class HtmlRendererTest {
  private static final Uri SPEC_URL = Uri.parse("http://example.org/gadget.xml");
  private static final String BASIC_HTML_CONTENT = "Hello, World!";
  private static final String PROXIED_HTML_CONTENT = "Hello, Universe!";
  private static final Uri PROXIED_HTML_HREF = Uri.parse("http://example.org/proxied.php");
  private static final GadgetContext CONTEXT = new GadgetContext() {
    @Override
    public SecurityToken getToken() {
      return new AnonymousSecurityToken();
    }
  };

  private final FakePreloaderService preloaderService = new FakePreloaderService();
  private final FakeProxyRenderer proxyRenderer = new FakeProxyRenderer();
  private final CaptureRewriter captureRewriter = new CaptureRewriter();
  private HtmlRenderer renderer;

  private Gadget makeGadget(String content) throws GadgetException {
    GadgetSpec spec = new GadgetSpec(SPEC_URL,
        "<Module><ModulePrefs title=''/><Content><![CDATA[" + content + "]]></Content></Module>");

    return new Gadget()
        .setSpec(spec)
        .setContext(CONTEXT)
        .setCurrentView(spec.getView("default"));
  }

  private Gadget makeHrefGadget(String authz) throws Exception {
    Gadget gadget = makeGadget("");
    String doc = "<Content href='" + PROXIED_HTML_HREF + "' authz='" + authz + "'/>";
    View view = new View("proxied", Arrays.asList(XmlUtil.parse(doc)), SPEC_URL);
    gadget.setCurrentView(view);
    return gadget;
  }

  @Before
  public void setUp() throws Exception {
    renderer = new HtmlRenderer(preloaderService, proxyRenderer,
        new GadgetRewritersProvider(ImmutableList.of((GadgetRewriter) captureRewriter)),
        null);

  }

  @Test
  public void renderPlainTypeHtml() throws Exception {
    String content = renderer.render(makeGadget(BASIC_HTML_CONTENT));
    assertEquals(BASIC_HTML_CONTENT, content);
  }

  @Test
  public void renderProxied() throws Exception {
    String content = renderer.render(makeHrefGadget("none"));
    assertEquals(PROXIED_HTML_CONTENT, content);
  }

  @Test
  public void doPreloading() throws Exception {
    renderer.render(makeGadget(BASIC_HTML_CONTENT));
    assertTrue("Preloading not performed.", preloaderService.wasPreloaded);
  }

  @Test
  public void doRewriting() throws Exception {
    renderer.render(makeGadget(BASIC_HTML_CONTENT));
    assertTrue("Rewriting not performed.", captureRewriter.viewWasRewritten());
  }

  private static class FakeProxyRenderer extends ProxyRenderer {
    public FakeProxyRenderer() {
      super(null, null, null);
    }

    @Override
    public String render(Gadget gadget) throws RenderingException, GadgetException {
      return PROXIED_HTML_CONTENT;
    }
  }

  private static class FakePreloaderService implements PreloaderService {
    protected boolean wasPreloaded;
    protected Collection<PreloadedData> preloads;

    protected FakePreloaderService() {
    }

    public Collection<PreloadedData> preload(Gadget gadget) {
      wasPreloaded = true;
      return preloads;
    }

    public Collection<PreloadedData> preload(Collection<Callable<PreloadedData>> tasks) {
      wasPreloaded = true;
      return preloads;
    }
  }

}
