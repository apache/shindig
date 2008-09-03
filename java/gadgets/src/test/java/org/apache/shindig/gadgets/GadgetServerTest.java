/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.gadgets;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;

import org.apache.shindig.auth.BasicSecurityToken;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.crypto.BlobCrypterException;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.oauth.OAuthArguments;
import org.apache.shindig.gadgets.spec.GadgetSpec;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class GadgetServerTest extends GadgetTestFixture {

  private static final Uri SPEC_URL = Uri.parse("http://example.org/g.xml");
  private static final HttpRequest SPEC_REQUEST = new HttpRequest(SPEC_URL);
  private static final Uri BUNDLE_URL = Uri.parse("http://example.org/m.xml");
  private static final HttpRequest BUNDLE_REQUEST = new HttpRequest(BUNDLE_URL);
  private static final GadgetContext BASIC_CONTEXT = new GadgetContext() {
    @Override
    public URI getUrl() {
      return SPEC_URL.toJavaUri();
    }

    @Override
    public SecurityToken getToken() {
      try {
        return new BasicSecurityToken("o", "v", "a", "d", "u", "m");
      } catch (BlobCrypterException bce) {
        throw new RuntimeException(bce);
      }
    }

    @Override
    public String getView() {
      return "v2";
    }

    @Override
    public UserPrefs getUserPrefs() {
      Map<String, String> map = new HashMap<String, String>();
      map.put("body", "BODY");
      return new UserPrefs(map);
    }
  };
  private static final String BASIC_SPEC_XML
      = "<Module>" +
        "  <ModulePrefs title=\"GadgetServerTest\"/>" +
        "  <Content type=\"html\">Hello, world!</Content>" +
        "</Module>";

  private static final String BASIC_BUNDLE_XML
      = "<messagebundle>" +
        "  <msg name=\"title\">TITLE</msg>" +
       "</messagebundle>";

  public void testGadgetSpecLookup() throws Exception {
    HttpRequest req = new HttpRequest(SPEC_URL);
    HttpResponse resp = new HttpResponse(BASIC_SPEC_XML);

    expect(fetcher.fetch(req)).andReturn(resp);
    replay();

    Gadget gadget = gadgetServer.processGadget(BASIC_CONTEXT);
    verify();
    assertEquals("GadgetServerTest",
        gadget.getSpec().getModulePrefs().getTitle());
    assertTrue(rewriter.viewWasRewritten());
  }

  public void testGadgetSpecLookupWithFetcherFailure() throws Exception {
    HttpResponse resp = HttpResponse.notFound();

    expect(fetcher.fetch(SPEC_REQUEST)).andReturn(resp);
    replay();

    try {
      gadgetServer.processGadget(BASIC_CONTEXT);
      fail("Expected a GadgetException for a failed http fetch.");
    } catch (GadgetException e) {
      // Expected for a bad gadget spec URI.
    }

    verify();
    assertFalse(rewriter.viewWasRewritten());
  }

  public void testSubstitutionsDone() throws Exception {
    String gadgetXml
        = "<Module>" +
          "  <ModulePrefs title=\"__MSG_title__\">" +
          "    <Locale messages=\"" + BUNDLE_URL.toString() + "\"/>" +
          "  </ModulePrefs>" +
          "  <UserPref name=\"body\" datatype=\"string\"/>" +
          "  <Content type=\"html\">__UP_body__</Content>" +
          "</Module>";

    HttpResponse spec = new HttpResponse(gadgetXml);
    HttpResponse bundle = new HttpResponse(BASIC_BUNDLE_XML);

    expect(fetcher.fetch(SPEC_REQUEST)).andReturn(spec);
    expect(fetcher.fetch(BUNDLE_REQUEST)).andReturn(bundle);
    replay();

    Gadget gadget = gadgetServer.processGadget(BASIC_CONTEXT);

    // No verification because the cache store ops happen on separate threads
    // and easy mock doesn't handle that properly.

    assertEquals("TITLE", gadget.getSpec().getModulePrefs().getTitle());
    assertEquals("BODY",
        gadget.getSpec().getView(GadgetSpec.DEFAULT_VIEW).getContent());
    assertTrue(rewriter.viewWasRewritten());
  }

  public void testBundledSubstitutionsDone() throws Exception {
    String gadgetXml
        = "<Module>" +
          "  <ModulePrefs title=\"__MSG_title__\">" +
          "    <Locale>" +
          "      <msg name='title'>TITLE</msg>" +
          "      <msg name='body'>BODY</msg>" +
          "    </Locale>" +
          "  </ModulePrefs>" +
          "  <UserPref name=\"body\" datatype=\"string\"/>" +
          "  <Content type=\"html\">__MSG_body__</Content>" +
          "</Module>";

    HttpResponse spec = new HttpResponse(gadgetXml);

    expect(fetcher.fetch(SPEC_REQUEST)).andReturn(spec);
    replay();

    Gadget gadget = gadgetServer.processGadget(BASIC_CONTEXT);

    // No verification because the cache store ops happen on separate threads
    // and easy mock doesn't handle that properly.

    assertEquals("TITLE", gadget.getSpec().getModulePrefs().getTitle());
    assertEquals("BODY",
        gadget.getSpec().getView(GadgetSpec.DEFAULT_VIEW).getContent());
    assertTrue(rewriter.viewWasRewritten());
  }

  public void testPreloadsFetched() throws Exception {
    String preloadUrl = "http://example.org/preload.txt";
    String preloadData = "Preload Data";
    HttpRequest preloadRequest = new HttpRequest(Uri.parse(preloadUrl));

    String gadgetXml
        = "<Module>" +
          "  <ModulePrefs title=\"foo\">" +
          "    <Preload href=\"" + preloadUrl + "\"/>" +
          "  </ModulePrefs>" +
          "  <Content type=\"html\">dummy</Content>" +
          "</Module>";
    expect(fetcherFactory.get()).andReturn(fetcher);
    expect(fetcher.fetch(SPEC_REQUEST))
         .andReturn(new HttpResponse(gadgetXml));
    expect(fetcher.fetch(preloadRequest))
        .andReturn(new HttpResponse(preloadData));
    replay();

    Gadget gadget = gadgetServer.processGadget(BASIC_CONTEXT);

    assertEquals(preloadData, gadget.getPreloadMap().values().iterator().next()
                                    .get().getResponseAsString());
    assertTrue(rewriter.viewWasRewritten());
  }

  public void testPreloadViewMatch() throws Exception {
    String preloadUrl = "http://example.org/preload.txt";
    String preloadData = "Preload Data";
    HttpRequest preloadRequest = new HttpRequest(Uri.parse(preloadUrl));

    String gadgetXml
        = "<Module>" +
          "  <ModulePrefs title=\"foo\">" +
          "    <Preload href=\"" + preloadUrl + "\" views=\"v1\"/>" +
          "  </ModulePrefs>" +
          "  <Content type=\"html\" view=\"v1,v2\">dummy</Content>" +
          "</Module>";
    expect(fetcherFactory.get()).andReturn(fetcher);
    expect(fetcher.fetch(SPEC_REQUEST))
         .andReturn(new HttpResponse(gadgetXml));
    expect(fetcher.fetch(preloadRequest))
        .andReturn(new HttpResponse(preloadData));
    replay();

    GadgetContext context = new GadgetContext() {
      @Override
      public URI getUrl() {
        return SPEC_URL.toJavaUri();
      }

      @Override
      public String getView() {
        return "v1";
      }
    };

    Gadget gadget = gadgetServer.processGadget(context);

    assertTrue(gadget.getPreloadMap().size() == 1);
    assertTrue(rewriter.viewWasRewritten());
  }

  public void testPreloadAntiMatch() throws Exception {
    String preloadUrl = "http://example.org/preload.txt";
    String preloadData = "Preload Data";
    HttpRequest preloadRequest
        = new HttpRequest(Uri.parse(preloadUrl));

    String gadgetXml
        = "<Module>" +
        "  <ModulePrefs title=\"foo\">" +
        "    <Preload href=\"" + preloadUrl + "\" views=\"v1,v3\"/>" +
        "  </ModulePrefs>" +
        "  <Content type=\"html\" view=\"v1,v2\">dummy</Content>" +
        "</Module>";
    expect(fetcherFactory.get()).andReturn(fetcher);
    expect(fetcher.fetch(SPEC_REQUEST))
        .andReturn(new HttpResponse(gadgetXml));
    expect(fetcher.fetch(preloadRequest))
        .andReturn(new HttpResponse(preloadData));
    replay();

    GadgetContext context = new GadgetContext() {
      @Override
      public URI getUrl() {
        return SPEC_URL.toJavaUri();
      }

      @Override
      public String getView() {
        return "v2";
      }
    };

    Gadget gadget = gadgetServer.processGadget(context);
    assertTrue(gadget.getPreloadMap().isEmpty());
    assertTrue(rewriter.viewWasRewritten());
  }

  public void testNoSignedPreloadWithoutToken() throws Exception {
    String preloadUrl = "http://example.org/preload.txt";

    String gadgetXml
        = "<Module>" +
        "  <ModulePrefs title=\"foo\">" +
        "    <Preload href=\"" + preloadUrl + "\" authz=\"signed\"/>" +
        "  </ModulePrefs>" +
        "  <Content type=\"html\" view=\"v1,v2\">dummy</Content>" +
        "</Module>";
    expect(fetcher.fetch(SPEC_REQUEST))
        .andReturn(new HttpResponse(gadgetXml));
    replay();

    GadgetContext context = new GadgetContext() {
      @Override
      public URI getUrl() {
        return SPEC_URL.toJavaUri();
      }
    };

    Gadget gadget = gadgetServer.processGadget(context);
    assertTrue(gadget.getPreloadMap().isEmpty());
  }

  public void testSignedPreloadWithToken() throws Exception {
    String preloadUrl = "http://example.org/preload.txt";
    String preloadData = "Preload Data";
    HttpRequest preloadRequest = new HttpRequest(Uri.parse(preloadUrl));

    String gadgetXml
        = "<Module>" +
        "  <ModulePrefs title=\"foo\">" +
        "    <Preload href=\"" + preloadUrl + "\" authz=\"signed\"/>" +
        "  </ModulePrefs>" +
        "  <Content type=\"html\" view=\"v1,v2\">dummy</Content>" +
        "</Module>";
    expect(fetcher.fetch(SPEC_REQUEST))
        .andReturn(new HttpResponse(gadgetXml));
    expect(fetcherFactory.getSigningFetcher(BASIC_CONTEXT.getToken()))
        .andReturn(fetcher);
    expect(fetcher.fetch(preloadRequest))
        .andReturn(new HttpResponse(preloadData));
    replay();

    Gadget gadget = gadgetServer.processGadget(BASIC_CONTEXT);
    assertTrue(gadget.getPreloadMap().size() == 1);
    assertTrue(rewriter.viewWasRewritten());
  }

  public void testOAuthPreload() throws Exception {
    String preloadUrl = "http://example.org/preload.txt";
    String preloadData = "Preload Data";
    HttpRequest preloadRequest = new HttpRequest(Uri.parse(preloadUrl));

    String gadgetXml
        = "<Module>" +
        "  <ModulePrefs title=\"foo\">" +
        "    <Preload href=\"" + preloadUrl + "\" authz=\"oauth\" " +
        		    "oauth_service_name='service'/>" +
        "  </ModulePrefs>" +
        "  <Content type=\"html\" view=\"v1,v2\">dummy</Content>" +
        "</Module>";
    expect(fetcher.fetch(SPEC_REQUEST))
        .andReturn(new HttpResponse(gadgetXml));
    expect(fetcherFactory.getOAuthFetcher(
        isA(SecurityToken.class), isA(OAuthArguments.class)))
        .andReturn(fetcher);
    expect(fetcher.fetch(preloadRequest))
        .andReturn(new HttpResponse(preloadData));
    replay();

    Gadget gadget = gadgetServer.processGadget(BASIC_CONTEXT);
    assertTrue(gadget.getPreloadMap().size() == 1);
    assertTrue(rewriter.viewWasRewritten());
  }

  public void testBlacklistedGadget() throws Exception {
    URI test = SPEC_URL.toJavaUri();
    expect(blacklist.isBlacklisted(eq(test))).andReturn(true);
    replay();

    try {
      gadgetServer.processGadget(BASIC_CONTEXT);
      fail("No exception thrown when a gadget is black listed!");
    } catch (GadgetException e) {
      assertEquals(GadgetException.Code.BLACKLISTED_GADGET, e.getCode());
    }
    verify();
    assertFalse(rewriter.viewWasRewritten());
  }

  public void testViewContentFetching() throws Exception {
    Uri viewUri = Uri.parse("http://example.org/content.htm");
    String gadgetXml
        = "<Module>" +
          "  <ModulePrefs title=\"foo\">" +
          "  </ModulePrefs>" +
          "  <Content type=\"html\" href=\"" + viewUri +"\" view=\"bar\" />" +
          "</Module>";
    String content ="<h2>foo</h2>";

    HttpResponse spec = new HttpResponse(gadgetXml);
    expect(fetcher.fetch(SPEC_REQUEST)).andReturn(spec);

    HttpRequest viewContentRequest = new HttpRequest(viewUri);
    HttpResponse viewContentResponse = new HttpResponse(content);
    expect(fetcher.fetch(viewContentRequest)).andReturn(viewContentResponse);

    replay();

    Gadget gadget = gadgetServer.processGadget(BASIC_CONTEXT);

    verify();

    assertNull(gadget.getSpec().getView("bar").getHref());
    assertEquals(content, gadget.getSpec().getView("bar").getContent());
  }

  public void testViewContentFetchingWithBadHref() throws Exception {
    Uri viewUri = Uri.parse("http://example.org/nonexistantcontent.htm");
    String gadgetXml
        = "<Module>" +
          "  <ModulePrefs title=\"foo\">" +
          "  </ModulePrefs>" +
          "  <Content type=\"html\" href=\"" + viewUri +"\" view=\"bar\" />" +
          "</Module>";

    HttpResponse spec = new HttpResponse(gadgetXml);
    expect(fetcher.fetch(SPEC_REQUEST)).andReturn(spec);

    HttpRequest viewContentRequest = new HttpRequest(viewUri);
    HttpResponse viewContentResponse = HttpResponse.notFound();
    expect(fetcher.fetch(viewContentRequest)).andReturn(viewContentResponse);

    replay();

    try {
      gadgetServer.processGadget(BASIC_CONTEXT);
      fail("Expected a GadgetException for a failed http fetch of remote gadget content.");
    } catch (GadgetException e) {
      // Expected for a bad content href URI.
    }

    verify();
  }
}
