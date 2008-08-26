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
package org.apache.shindig.gadgets;

import static org.junit.Assert.assertEquals;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.spec.GadgetSpec;

import com.google.common.collect.Maps;

import org.junit.Test;

import java.net.URI;
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
      " <Content view='url' type='url' href='http://example.org/always/an/error.html'/>" +
      "</Module>";

  private final FakeHttpFetcher httpFetcher = new FakeHttpFetcher();

  private final Renderer renderer = new Renderer(new FakeGadgetSpecFactory(), httpFetcher);

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
  public void renderPlainTypeHtml() throws Exception {
    String content = renderer.render(makeContext("html", SPEC_URL));
    assertEquals(BASIC_HTML_CONTENT, content);
  }

  @Test
  public void renderProxiedTypeHtml() throws Exception {
    httpFetcher.responses.put(PROXIED_HTML_HREF, new HttpResponse(PROXIED_HTML_CONTENT));
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

  private static class FakeGadgetSpecFactory implements GadgetSpecFactory {
    public GadgetSpec getGadgetSpec(GadgetContext context) throws GadgetException {
      return new GadgetSpec(context.getUrl(), GADGET);
    }

    public GadgetSpec getGadgetSpec(URI uri, boolean ignoreCache) {
      throw new UnsupportedOperationException();
    }
  }

  private static class FakeHttpFetcher implements HttpFetcher {
    private final Map<Uri, HttpResponse> responses = Maps.newHashMap();

    public HttpResponse fetch(HttpRequest request) throws GadgetException {
      HttpResponse response = responses.get(request.getUri());
      if (response == null) {
        throw new GadgetException(GadgetException.Code.FAILED_TO_RETRIEVE_CONTENT,
            "Unknown gadget: " + request.getUri());
      }
      return response;
    }
  }
}
