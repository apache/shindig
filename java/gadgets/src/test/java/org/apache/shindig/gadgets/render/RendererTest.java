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
import org.apache.shindig.common.ContainerConfig;
import org.apache.shindig.common.ContainerConfigException;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.GadgetSpecFactory;
import org.apache.shindig.gadgets.http.ContentFetcherFactory;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.preload.PreloaderService;
import org.apache.shindig.gadgets.preload.Preloads;
import org.apache.shindig.gadgets.spec.GadgetSpec;

import com.google.common.collect.Maps;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.Arrays;
import java.util.Map;

/**
 * Tests for Renderer
 */
public class RendererTest {
  private static final Uri SPEC_URL = Uri.parse("http://example.org/gadget.xml");
  private static final String BASIC_HTML_CONTENT = "Hello, World!";
  private static final String PROXIED_HTML_CONTENT = "Hello, Universe!";
  private static final Uri PROXIED_HTML_HREF = Uri.parse("http://example.org/proxied.php");
  private static final String GADGET =
      "<Module>" +
      " <ModulePrefs title='foo'/>" +
      " <Content view='html' type='html'>" + BASIC_HTML_CONTENT + "</Content>" +
      " <Content view='proxied' type='html' href='" + PROXIED_HTML_HREF + "'/>" +
      " <Content view='proxied-signed' authz='signed' href='" + PROXIED_HTML_HREF + "'/>" +
      " <Content view='proxied-oauth' authz='oauth' href='" + PROXIED_HTML_HREF + "'/>" +
      " <Content view='url' type='url' href='http://example.org/always/an/error.html'/>" +
      " <Content view='alias' type='html'>" + BASIC_HTML_CONTENT + "</Content>" +
      "</Module>";

  private final FakeGadgetSpecFactory specFactory = new FakeGadgetSpecFactory();
  private final FakeContentFetcherFactory fetcher = new FakeContentFetcherFactory();
  private final FakePreloaderService preloaderService = new FakePreloaderService();
  private FakeContainerConfig containerConfig;
  private Renderer renderer;

  @Before
  public void setUp() throws Exception {
    containerConfig = new FakeContainerConfig();
    renderer = new Renderer(specFactory, fetcher, preloaderService, containerConfig);
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

      @Override
      public SecurityToken getToken() {
        return new AnonymousSecurityToken();
      }
    };
  }

  @Test
  public void renderPlainTypeHtml() throws Exception {
    String content = renderer.render(makeContext("html", SPEC_URL));
    assertEquals(BASIC_HTML_CONTENT, content);
  }

  @Test
  public void renderProxiedTypeHtml() throws Exception {
    fetcher.plainResponses.put(PROXIED_HTML_HREF, new HttpResponse(PROXIED_HTML_CONTENT));
    String content = renderer.render(makeContext("proxied", SPEC_URL));
    assertEquals(PROXIED_HTML_CONTENT, content);
  }

  @Test(expected = RenderingException.class)
  public void renderTypeUrl() throws RenderingException {
    renderer.render(makeContext("url", SPEC_URL));
  }

  @Test(expected = RenderingException.class)
  public void renderInvalidUrl() throws RenderingException {
    renderer.render(makeContext("url", Uri.parse("doesnotexist")));
  }

  @Test
  public void doPreloading() throws RenderingException {
    renderer.render(makeContext("html", SPEC_URL));
    assertTrue("Preloading not performed.", preloaderService.wasPreloaded);
  }

  @Test
  public void renderProxiedSigned() throws RenderingException {
    fetcher.oauthResponses.put(PROXIED_HTML_HREF, new HttpResponse(PROXIED_HTML_CONTENT));
    String content = renderer.render(makeContext("proxied-signed", SPEC_URL));
    assertEquals(PROXIED_HTML_CONTENT, content);
  }

  @Test
  public void renderProxiedOAuth() throws RenderingException {
    fetcher.oauthResponses.put(PROXIED_HTML_HREF, new HttpResponse(PROXIED_HTML_CONTENT));
    String content = renderer.render(makeContext("proxied-oauth", SPEC_URL));
    assertEquals(PROXIED_HTML_CONTENT, content);
  }

  @Test
  public void doViewAliasing() throws Exception {
    JSONArray aliases = new JSONArray(Arrays.asList("alias"));
    containerConfig.json.put("gadgets.features/views/aliased/aliases", aliases);
    String content = renderer.render(makeContext("alias", SPEC_URL));
    assertEquals(BASIC_HTML_CONTENT, content);
  }

  @Test(expected = RenderingException.class)
  public void noSupportedViewThrows() throws RenderingException {
    renderer.render(makeContext("not-real-view", SPEC_URL));
  }

  private static class FakeGadgetSpecFactory implements GadgetSpecFactory {
    public GadgetSpec getGadgetSpec(GadgetContext context) throws GadgetException {
      return new GadgetSpec(context.getUrl(), GADGET);
    }

    public GadgetSpec getGadgetSpec(URI uri, boolean ignoreCache) {
      throw new UnsupportedOperationException();
    }
  }

  private static class FakeContentFetcherFactory extends ContentFetcherFactory {
    private final Map<Uri, HttpResponse> plainResponses = Maps.newHashMap();
    private final Map<Uri, HttpResponse> oauthResponses = Maps.newHashMap();

    public FakeContentFetcherFactory() {
      super(null, null);
    }

    @Override
    public HttpResponse fetch(HttpRequest request) throws GadgetException {
      if (request.getGadget() == null) {
        throw new GadgetException(GadgetException.Code.FAILED_TO_RETRIEVE_CONTENT,
            "No gadget associated with rendering request.");
      }

      if (request.getContainer() == null) {
        throw new GadgetException(GadgetException.Code.FAILED_TO_RETRIEVE_CONTENT,
            "No container associated with rendering request.");
      }

      if (request.getSecurityToken() == null) {
        throw new GadgetException(GadgetException.Code.FAILED_TO_RETRIEVE_CONTENT,
            "No security token associated with rendering request.");
      }

      if (request.getOAuthArguments() == null) {
        throw new GadgetException(GadgetException.Code.FAILED_TO_RETRIEVE_CONTENT,
            "No oauth arguments associated with rendering request.");
      }

      HttpResponse response;
      switch (request.getAuthType()) {
        case NONE:
          response = plainResponses.get(request.getUri());
          break;
        case SIGNED:
        case OAUTH:
          response = oauthResponses.get(request.getUri());
          break;
        default:
          response = null;
          break;
      }
      if (response == null) {
        throw new GadgetException(GadgetException.Code.FAILED_TO_RETRIEVE_CONTENT,
            "Unknown file: " + request.getUri());
      }
      return response;
    }
  }

  private static class FakePreloaderService implements PreloaderService {
    private boolean wasPreloaded;
    public Preloads preload(GadgetContext context, GadgetSpec gadget) {
      wasPreloaded = true;
      return null;
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
}
