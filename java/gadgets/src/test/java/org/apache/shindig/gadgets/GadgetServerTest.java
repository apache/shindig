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

import org.apache.shindig.common.BasicSecurityToken;
import org.apache.shindig.common.SecurityToken;
import org.apache.shindig.common.crypto.BlobCrypterException;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.spec.GadgetSpec;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class GadgetServerTest extends GadgetTestFixture {

  private final static URI SPEC_URL = URI.create("http://example.org/g.xml");
  private final static HttpRequest SPEC_REQUEST
      = new HttpRequest(SPEC_URL);
  private final static URI BUNDLE_URL = URI.create("http://example.org/m.xml");
  private final static HttpRequest BUNDLE_REQUEST
      = new HttpRequest(BUNDLE_URL);
  private final static GadgetContext BASIC_CONTEXT = new GadgetContext() {
    @Override
    public URI getUrl() {
      return SPEC_URL;
    }

    @Override
    @SuppressWarnings("unused")
    public SecurityToken getToken() throws GadgetException {
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
  private final static String BASIC_SPEC_XML
      = "<Module>" +
        "  <ModulePrefs title=\"GadgetServerTest\"/>" +
        "  <Content type=\"html\">Hello, world!</Content>" +
        "</Module>";

  private final static String BASIC_BUNDLE_XML
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

    GadgetContext context = new GadgetContext() {
      @Override
      public URI getUrl() {
        return SPEC_URL;
      }
    };

    expect(fetcher.fetch(SPEC_REQUEST)).andReturn(spec);
    replay();

    Gadget gadget = gadgetServer.processGadget(BASIC_CONTEXT);

    // No verification because the cache store ops happen on separate threads
    // and easy mock doesn't handle that properly.

    assertEquals("TITLE", gadget.getSpec().getModulePrefs().getTitle());
    assertEquals("BODY",
        gadget.getSpec().getView(GadgetSpec.DEFAULT_VIEW).getContent());
  }

  public void testPreloadsFetched() throws Exception {
    String preloadUrl = "http://example.org/preload.txt";
    String preloadData = "Preload Data";
    HttpRequest preloadRequest
        = new HttpRequest(URI.create(preloadUrl));

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
  }

  public void testPreloadViewMatch() throws Exception {
    String preloadUrl = "http://example.org/preload.txt";
    String preloadData = "Preload Data";
    HttpRequest preloadRequest
        = new HttpRequest(URI.create(preloadUrl));

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
        return SPEC_URL;
      }

      @Override
      public String getView() {
        return "v1";
      }
    };

    Gadget gadget = gadgetServer.processGadget(context);

    assertTrue(gadget.getPreloadMap().size() == 1);
  }

  public void testPreloadAntiMatch() throws Exception {
    String preloadUrl = "http://example.org/preload.txt";
    String preloadData = "Preload Data";
    HttpRequest preloadRequest
        = new HttpRequest(URI.create(preloadUrl));

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
        return SPEC_URL;
      }

      @Override
      public String getView() {
        return "v2";
      }
    };

    Gadget gadget = gadgetServer.processGadget(context);
    assertTrue(gadget.getPreloadMap().isEmpty());
  }

  public void testNoSignedPreloadWithoutToken() throws Exception {
    String preloadUrl = "http://example.org/preload.txt";
    String preloadData = "Preload Data";
    HttpRequest preloadRequest
        = new HttpRequest(URI.create(preloadUrl));

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
        return SPEC_URL;
      }
    };

    Gadget gadget = gadgetServer.processGadget(context);
    assertTrue(gadget.getPreloadMap().isEmpty());
  }

  public void testSignedPreloadWithToken() throws Exception {
    String preloadUrl = "http://example.org/preload.txt";
    String preloadData = "Preload Data";
    HttpRequest preloadRequest
        = new HttpRequest(URI.create(preloadUrl));

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
  }


  public void testBlacklistedGadget() throws Exception {
    expect(blacklist.isBlacklisted(eq(SPEC_URL))).andReturn(true);
    replay();

    try {
      gadgetServer.processGadget(BASIC_CONTEXT);
      fail("No exception thrown when a gadget is black listed!");
    } catch (GadgetException e) {
      assertEquals(GadgetException.Code.BLACKLISTED_GADGET, e.getCode());
    }
    verify();
  }
}
