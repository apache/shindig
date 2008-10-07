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
import org.apache.shindig.common.uri.UriBuilder;
import org.apache.shindig.common.xml.XmlUtil;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.ContentFetcherFactory;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.preload.PreloaderService;
import org.apache.shindig.gadgets.preload.Preloads;
import org.apache.shindig.gadgets.rewrite.ContentRewriterRegistry;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.View;

import com.google.common.collect.Maps;

import org.junit.Test;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

/**
 * Tests for HtmlRenderer
 */
public class HtmlRendererTest {
  private static final Uri SPEC_URL = Uri.parse("http://example.org/gadget.xml");
  private static final String BASIC_HTML_CONTENT = "Hello, World!";
  private static final String PROXIED_HTML_CONTENT = "Hello, Universe!";
  private static final Uri PROXIED_HTML_HREF = Uri.parse("http://example.org/proxied.php");
  private static final Uri EXPECTED_PROXIED_HTML_HREF
      = Uri.parse("http://example.org/proxied.php?lang=all&country=ALL");
  private static final GadgetContext CONTEXT = new GadgetContext() {
    @Override
    public SecurityToken getToken() {
      return new AnonymousSecurityToken();
    }
  };

  private final FakeContentFetcherFactory fetcher = new FakeContentFetcherFactory();
  private final FakePreloaderService preloaderService = new FakePreloaderService();
  private final FakeContentRewriterRegistry rewriter = new FakeContentRewriterRegistry();
  private final HtmlRenderer renderer = new HtmlRenderer(fetcher, preloaderService, rewriter);

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

  @Test
  public void renderPlainTypeHtml() throws Exception {
    String content = renderer.render(makeGadget(BASIC_HTML_CONTENT));
    assertEquals(BASIC_HTML_CONTENT, content);
  }

  @Test
  public void renderProxied() throws Exception {
    fetcher.plainResponses.put(EXPECTED_PROXIED_HTML_HREF, new HttpResponse(PROXIED_HTML_CONTENT));
    String content = renderer.render(makeHrefGadget("none"));
    assertEquals(PROXIED_HTML_CONTENT, content);
  }

  @Test
  public void renderProxiedSigned() throws Exception {
    fetcher.signedResponses.put(EXPECTED_PROXIED_HTML_HREF, new HttpResponse(PROXIED_HTML_CONTENT));
    String content = renderer.render(makeHrefGadget("signed"));
    assertEquals(PROXIED_HTML_CONTENT, content);
  }

  @Test
  public void renderProxiedOAuth() throws Exception {
    // TODO: We need to disambiguate between oauth and signed.
    fetcher.oauthResponses.put(EXPECTED_PROXIED_HTML_HREF, new HttpResponse(PROXIED_HTML_CONTENT));
    String content = renderer.render(makeHrefGadget("oauth"));
    assertEquals(PROXIED_HTML_CONTENT, content);
  }

  @Test
  public void renderProxiedCustomLocale() throws Exception {
    UriBuilder uri = new UriBuilder(PROXIED_HTML_HREF);
    uri.putQueryParameter("lang", "foo");
    uri.putQueryParameter("country", "BAR");
    Gadget gadget = makeHrefGadget("none");
    gadget.setContext(new GadgetContext() {
      @Override
      public Locale getLocale() {
        return new Locale("foo", "BAR");
      }

      @Override
      public SecurityToken getToken() {
        return new AnonymousSecurityToken();
      }
    });

    fetcher.plainResponses.put(uri.toUri(), new HttpResponse(PROXIED_HTML_CONTENT));
    String content = renderer.render(gadget);
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
    assertTrue("Rewriting not performed.", rewriter.wasRewritten);
  }

  private static class FakeContentFetcherFactory extends ContentFetcherFactory {
    private final Map<Uri, HttpResponse> plainResponses = Maps.newHashMap();
    private final Map<Uri, HttpResponse> signedResponses = Maps.newHashMap();
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
          response = signedResponses.get(request.getUri());
          break;
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

  private static class FakeContentRewriterRegistry implements ContentRewriterRegistry {
    private boolean wasRewritten = false;

    public String rewriteGadget(Gadget gadget, String content) {
      wasRewritten = true;
      return content;
    }

    public HttpResponse rewriteHttpResponse(HttpRequest req, HttpResponse resp) {
      throw new UnsupportedOperationException();
    }
  }
}
