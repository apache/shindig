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
import org.apache.shindig.common.JsonAssert;
import org.apache.shindig.common.JsonSerializer;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.uri.UriBuilder;
import org.apache.shindig.common.xml.XmlUtil;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.AbstractHttpCache;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.RequestPipeline;
import org.apache.shindig.gadgets.preload.PipelineExecutor;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.PipelinedData;
import org.apache.shindig.gadgets.spec.View;
import org.json.JSONObject;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

/**
 * Tests for ProxyRenderer.
 */
public class ProxyRendererTest {
  private static final Uri SPEC_URL = Uri.parse("http://example.org/gadget.xml");
  private static final String PROXIED_HTML_CONTENT = "Hello, Universe!";
  private static final Uri PROXIED_HTML_HREF = Uri.parse("http://example.org/proxied.php");
  private static final Uri EXPECTED_PROXIED_HTML_HREF
      = Uri.parse("http://example.org/proxied.php?lang=all&country=ALL");
  private static final String USER_AGENT = "TestUserAgent/1.0";
  private static final String USER_AGENT_SET = "TestUserAgent/1.0 Shindig";
  private static final GadgetContext CONTEXT = new GadgetContext() {
    @Override
    public SecurityToken getToken() {
      return new AnonymousSecurityToken();
    }

    @Override
    public String getUserAgent() {
      return USER_AGENT;
    }
  };

  private final FakeHttpCache cache = new FakeHttpCache();
  private final FakeRequestPipeline pipeline = new FakeRequestPipeline();
  private final FakePipelineExecutor pipelineExecutor = new FakePipelineExecutor();
  private final ProxyRenderer proxyRenderer = new ProxyRenderer(pipeline,
      cache, pipelineExecutor);

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
  public void renderProxied() throws Exception {
    HttpRequest request = new HttpRequest(EXPECTED_PROXIED_HTML_HREF);
    request.setHeader("User-Agent", USER_AGENT_SET);
    HttpResponse response = new HttpResponse(PROXIED_HTML_CONTENT);
    pipeline.plainResponses.put(EXPECTED_PROXIED_HTML_HREF, response);

    String content = proxyRenderer.render(makeHrefGadget("none"));
    assertEquals(PROXIED_HTML_CONTENT, content);
    assertEquals(response, cache.getResponse(request));
  }

  @Test
  public void renderProxiedRelative() throws Exception {
    Uri base = EXPECTED_PROXIED_HTML_HREF;
    final Uri relative = Uri.parse("/some/path?foo=bar");
    Uri resolved = new UriBuilder(base.resolve(relative))
      .addQueryParameter("lang", GadgetSpec.DEFAULT_LOCALE.getLanguage())
      .addQueryParameter("country", GadgetSpec.DEFAULT_LOCALE.getCountry())
      .toUri();

    HttpRequest request = new HttpRequest(resolved);
    request.setHeader("User-Agent", USER_AGENT_SET);
    HttpResponse response = new HttpResponse(PROXIED_HTML_CONTENT);

    pipeline.plainResponses.put(resolved, response);

    Gadget gadget = makeHrefGadget("none");
    gadget.setContext(new GadgetContext(gadget.getContext()) {
      @Override
      public String getParameter(String name) {
        return name.equals(HtmlRenderer.PATH_PARAM) ? relative.toString() : null;
      }
    });

    String content = proxyRenderer.render(gadget);
    assertEquals(PROXIED_HTML_CONTENT, content);
    assertEquals(response, cache.getResponse(request));
  }

  @Test
  public void renderProxiedRelativeBadPath() throws Exception {
    HttpRequest request = new HttpRequest(EXPECTED_PROXIED_HTML_HREF);
    request.setHeader("User-Agent", USER_AGENT_SET);
    HttpResponse response = new HttpResponse(PROXIED_HTML_CONTENT);
    pipeline.plainResponses.put(EXPECTED_PROXIED_HTML_HREF, response);

    Gadget gadget = makeHrefGadget("none");
    gadget.setContext(new GadgetContext(gadget.getContext()) {
      @Override
      public String getParameter(String name) {
        return name.equals(HtmlRenderer.PATH_PARAM) ? "$(^)$" : null;
      }
    });

    String content = proxyRenderer.render(gadget);

    assertEquals(PROXIED_HTML_CONTENT, content);
    assertEquals(response, cache.getResponse(request));
  }

  @Test
  public void renderProxiedFromCache() throws Exception {
    HttpRequest request = new HttpRequest(EXPECTED_PROXIED_HTML_HREF);
    request.setHeader("User-Agent", USER_AGENT_SET);
    HttpResponse response = new HttpResponse(PROXIED_HTML_CONTENT);
    cache.addResponse(request, response);
    String content = proxyRenderer.render(makeHrefGadget("none"));
    assertEquals(PROXIED_HTML_CONTENT, content);
  }

  @Test
  public void renderProxiedSigned() throws Exception {
    pipeline.signedResponses.put(EXPECTED_PROXIED_HTML_HREF, new HttpResponse(PROXIED_HTML_CONTENT));
    String content = proxyRenderer.render(makeHrefGadget("signed"));
    assertEquals(PROXIED_HTML_CONTENT, content);
  }

  @Test
  public void renderProxiedOAuth() throws Exception {
    // TODO: We need to disambiguate between oauth and signed.
    pipeline.oauthResponses.put(EXPECTED_PROXIED_HTML_HREF, new HttpResponse(PROXIED_HTML_CONTENT));
    String content = proxyRenderer.render(makeHrefGadget("oauth"));
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

    pipeline.plainResponses.put(uri.toUri(), new HttpResponse(PROXIED_HTML_CONTENT));
    String content = proxyRenderer.render(gadget);
    assertEquals(PROXIED_HTML_CONTENT, content);
  }

  @Test
  public void renderProxiedWithPreload() throws Exception {
    List<JSONObject> prefetchedJson = ImmutableList.of(new JSONObject("{id: 'foo', data: 'bar'}"));

    pipelineExecutor.results = new PipelineExecutor.Results(null, prefetchedJson, null);

    pipeline.plainResponses.put(EXPECTED_PROXIED_HTML_HREF, new HttpResponse(PROXIED_HTML_CONTENT));

    String content = proxyRenderer.render(makeHrefGadget("none"));
    assertEquals(PROXIED_HTML_CONTENT, content);

    HttpRequest lastHttpRequest = pipeline.getLastHttpRequest();
    assertEquals("POST", lastHttpRequest.getMethod());
    assertEquals("application/json;charset=utf-8", lastHttpRequest.getHeader("Content-Type"));
    String postBody = lastHttpRequest.getPostBodyAsString();

    JsonAssert.assertJsonEquals(JsonSerializer.serialize(prefetchedJson), postBody);
    assertTrue(pipelineExecutor.wasPreloaded);
  }

  @Test
  public void appendUserAgent() throws Exception {
    String expectedUA = USER_AGENT + " Shindig";
    HttpResponse response = new HttpResponse(PROXIED_HTML_CONTENT);
    pipeline.plainResponses.put(EXPECTED_PROXIED_HTML_HREF, response);

    proxyRenderer.render(makeHrefGadget("none"));
    String actualUA = pipeline.lastHttpRequest.getHeader("User-Agent");
    assertEquals(expectedUA, actualUA);
  }

  private static class FakeHttpCache extends AbstractHttpCache {
    private final Map<String, HttpResponse> map = Maps.newHashMap();

    protected FakeHttpCache() {
    }

    @Override
    protected void addResponseImpl(String key, HttpResponse response) {
      map.put(key, response);
    }

    @Override
    protected HttpResponse getResponseImpl(String key) {
      return map.get(key);
    }

    @Override
    protected void removeResponseImpl(String key) {
      map.remove(key);
    }
  }

  private static class FakeRequestPipeline implements RequestPipeline {
    protected final Map<Uri, HttpResponse> plainResponses = Maps.newHashMap();
    protected final Map<Uri, HttpResponse> signedResponses = Maps.newHashMap();
    protected final Map<Uri, HttpResponse> oauthResponses = Maps.newHashMap();
    private HttpRequest lastHttpRequest;

    protected FakeRequestPipeline() {
    }

    public HttpResponse execute(HttpRequest request) throws GadgetException {
      lastHttpRequest = request;

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

      assertTrue(request.getOAuthArguments().isProxiedContentRequest());

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

    public HttpRequest getLastHttpRequest() {
      return lastHttpRequest;
    }
  }

  private static class FakePipelineExecutor extends PipelineExecutor {
    protected boolean wasPreloaded;
    protected Results results;

    public FakePipelineExecutor() {
      super(null, null, null);
    }

    @Override
    public Results execute(GadgetContext context, Collection<PipelinedData> pipelines) {
      wasPreloaded = true;
      return results;
    }
  }
}
