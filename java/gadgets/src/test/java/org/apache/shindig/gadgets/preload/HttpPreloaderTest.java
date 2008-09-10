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
package org.apache.shindig.gadgets.preload;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.testing.FakeGadgetToken;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.http.ContentFetcherFactory;
import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.apache.shindig.gadgets.oauth.OAuthArguments;
import org.apache.shindig.gadgets.spec.GadgetSpec;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.easymock.IMocksControl;
import org.easymock.classextension.EasyMock;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Tests for HttpPreloader.
 */
public class HttpPreloaderTest {
  private static final String PRELOAD_HREF = "http://www.example.org/file";
  private static final String PRELOAD_HREF2 = "http://www.example.org/file";
  private static final String PRELOAD_CONTENT = "Preloaded data";
  private static final String CONTAINER = "some-container";
  private static final URI GADGET_URL = URI.create("http://example.org/gadget.xml");
  private static final Map<String, String> PRELOAD_METADATA = Maps.immutableMap("foo", "bar");

  private final IMocksControl control = EasyMock.createNiceControl();
  private final ContentFetcherFactory fetchers = control.createMock(ContentFetcherFactory.class);
  private final RecordingHttpFetcher plainFetcher = new RecordingHttpFetcher();
  private final RecordingHttpFetcher oauthFetcher = new RecordingHttpFetcher();

  private final GadgetContext context = new GadgetContext() {
    @Override
    public SecurityToken getToken() {
      return new FakeGadgetToken();
    }

    @Override
    public String getContainer() {
      return CONTAINER;
    }

    @Override
    public URI getUrl() {
      return GADGET_URL;
    }
  };

  @Before
  public void setUp() throws Exception {
    expect(fetchers.get())
        .andReturn(plainFetcher).anyTimes();
    expect(fetchers.getOAuthFetcher(isA(HttpRequest.class))).andReturn(oauthFetcher).anyTimes();
    control.replay();
  }

  private void checkRequest(HttpRequest request) {
    assertEquals(context.getContainer(), request.getContainer());
    assertEquals(GADGET_URL.toString(), request.getGadget().toString());
    assertEquals(context.getToken().getAppId(), request.getSecurityToken().getAppId());
  }

  private static void checkResults(Map<String, String> results) {
    assertEquals(PRELOAD_CONTENT, results.get("body"));
    assertEquals(HttpResponse.SC_OK, Integer.parseInt(results.get("rc")));
    assertTrue("Metadata values not copied to output.",
        results.entrySet().containsAll(PRELOAD_METADATA.entrySet()));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void normalPreloads() throws Exception {
    String xml =
        "<Module><ModulePrefs title=''>" +
        " <Preload href='" + PRELOAD_HREF + "'/>" +
        "</ModulePrefs><Content/></Module>";
    GadgetSpec gadget = new GadgetSpec(GADGET_URL, xml);
    Preloader preloader = new HttpPreloader(fetchers);

    Map<String, Callable<PreloadedData>> preloaded = preloader.createPreloadTasks(context, gadget);

    PreloadedData data = preloaded.get(PRELOAD_HREF).call();

    checkRequest(plainFetcher.requests.get(0));
    checkResults((Map<String, String>)data.toJson());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void signedPreloads() throws Exception {
    String xml =
        "<Module><ModulePrefs title=''>" +
        " <Preload href='" + PRELOAD_HREF + "' authz='signed' sign_viewer='false'/>" +
        "</ModulePrefs><Content/></Module>";
    GadgetSpec gadget = new GadgetSpec(GADGET_URL, xml);
    Preloader preloader = new HttpPreloader(fetchers);

    Map<String, Callable<PreloadedData>> preloaded = preloader.createPreloadTasks(context, gadget);

    PreloadedData data = preloaded.get(PRELOAD_HREF).call();

    HttpRequest request = oauthFetcher.requests.get(0);
    checkRequest(request);
    assertTrue(request.getOAuthArguments().getSignOwner());
    assertFalse(request.getOAuthArguments().getSignViewer());
    checkResults((Map<String, String>)data.toJson());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void oauthPreloads() throws Exception {
    String xml =
        "<Module><ModulePrefs title=''>" +
        // This is kind of a bogus test since oauth params aren't set.
        " <Preload href='" + PRELOAD_HREF + "' authz='oauth'/>" +
        "</ModulePrefs><Content/></Module>";
    GadgetSpec gadget = new GadgetSpec(GADGET_URL, xml);
    Preloader preloader = new HttpPreloader(fetchers);

    Map<String, Callable<PreloadedData>> preloaded = preloader.createPreloadTasks(context, gadget);

    PreloadedData data = preloaded.get(PRELOAD_HREF).call();

    HttpRequest request = oauthFetcher.requests.get(0);
    checkRequest(request);
    checkResults((Map<String, String>)data.toJson());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void multiplePreloads() throws Exception {
    String xml =
        "<Module><ModulePrefs title=''>" +
        " <Preload href='" + PRELOAD_HREF + "'/>" +
        " <Preload href='" + PRELOAD_HREF2 + "'/>" +
        "</ModulePrefs><Content/></Module>";
    GadgetSpec gadget = new GadgetSpec(GADGET_URL, xml);
    Preloader preloader = new HttpPreloader(fetchers);

    Map<String, Callable<PreloadedData>> preloaded = preloader.createPreloadTasks(context, gadget);

    PreloadedData data = preloaded.get(PRELOAD_HREF).call();
    checkRequest(plainFetcher.requests.get(0));
    checkResults((Map<String, String>)data.toJson());

    data = preloaded.get(PRELOAD_HREF2).call();
    checkRequest(plainFetcher.requests.get(1));
    checkResults((Map<String, String>)data.toJson());
  }

  private static class RecordingHttpFetcher implements HttpFetcher {
    private List<HttpRequest> requests = Lists.newArrayList();

    public HttpResponse fetch(HttpRequest request) {
      requests.add(request);
      return new HttpResponseBuilder()
          .setMetadata(PRELOAD_METADATA)
          .setResponseString(PRELOAD_CONTENT)
          .create();
    }
  }
}
