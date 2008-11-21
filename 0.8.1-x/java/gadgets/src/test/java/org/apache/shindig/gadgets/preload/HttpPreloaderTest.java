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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.testing.FakeGadgetToken;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.AuthType;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.http.ContentFetcherFactory;
import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.apache.shindig.gadgets.spec.GadgetSpec;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

/**
 * Tests for HttpPreloader.
 */
public class HttpPreloaderTest {
  private static final String PRELOAD_HREF = "http://www.example.org/file";
  private static final String PRELOAD_HREF2 = "http://www.example.org/file-two";
  private static final String PRELOAD_CONTENT = "Preloaded data";
  private static final String CONTAINER = "some-container";
  private static final Uri GADGET_URL = Uri.parse("http://example.org/gadget.xml");
  private static final Map<String, String> PRELOAD_METADATA = Maps.immutableMap("foo", "bar");
  private final RecordingHttpFetcher plainFetcher = new RecordingHttpFetcher();
  private final RecordingHttpFetcher oauthFetcher = new RecordingHttpFetcher();

  private final ContentFetcherFactory fetchers = new ContentFetcherFactory(null, null) {
    @Override
    public HttpResponse fetch(HttpRequest request) {
      if (request.getAuthType() == AuthType.NONE) {
        return plainFetcher.fetch(request);
      }
      return oauthFetcher.fetch(request);
    }
  };

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
      return GADGET_URL.toJavaUri();
    }
  };

  private void checkRequest(HttpRequest request) {
    assertEquals(context.getContainer(), request.getContainer());
    assertEquals(GADGET_URL.toString(), request.getGadget().toString());
    assertEquals(context.getToken().getAppId(), request.getSecurityToken().getAppId());
  }

  private static void checkResults(JSONObject results) throws JSONException {
    assertEquals(PRELOAD_CONTENT, results.get("body"));
    assertEquals(HttpResponse.SC_OK, results.getInt("rc"));
    assertEquals("yo=momma", results.getJSONObject("headers").getJSONArray("set-cookie").get(0));

    for (Entry<String, String> entry : PRELOAD_METADATA.entrySet()) {
      assertEquals("Metadata values not copied to output.",
          entry.getValue(), results.get(entry.getKey()));
    }
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
    checkResults((JSONObject)data.toJson());
  }

  @Test
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
    checkResults((JSONObject) data.toJson());
  }

  @Test
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
    checkResults((JSONObject) data.toJson());
  }

  @Test
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
    checkResults((JSONObject) data.toJson());

    data = preloaded.get(PRELOAD_HREF2).call();
    checkRequest(plainFetcher.requests.get(1));
    checkResults((JSONObject) data.toJson());
  }

  @Test
  public void onlyPreloadForCorrectView() throws Exception {
    String xml =
        "<Module><ModulePrefs title=''>" +
        " <Preload href='" + PRELOAD_HREF + "' views='foo,bar,baz'/>" +
        " <Preload href='" + PRELOAD_HREF2 + "' views='bar'/>" +
        "</ModulePrefs><Content/></Module>";
    GadgetSpec gadget = new GadgetSpec(GADGET_URL, xml);
    Preloader preloader = new HttpPreloader(fetchers);

    GadgetContext fooViewContext = new GadgetContext() {
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
        return GADGET_URL.toJavaUri();
      }

      @Override
      public String getView() {
        return "foo";
      }
    };

    Map<String, Callable<PreloadedData>> preloaded
        = preloader.createPreloadTasks(fooViewContext, gadget);

    PreloadedData data = preloaded.get(PRELOAD_HREF).call();
    checkRequest(plainFetcher.requests.get(0));
    checkResults((JSONObject) data.toJson());

    assertNull("Preloaded an item that should not have been.", preloaded.get(PRELOAD_HREF2));
  }

  private static class RecordingHttpFetcher implements HttpFetcher {
    private final List<HttpRequest> requests = Lists.newArrayList();

    public HttpResponse fetch(HttpRequest request) {
      requests.add(request);
      return new HttpResponseBuilder()
          .setMetadata(PRELOAD_METADATA)
          .setResponseString(PRELOAD_CONTENT)
          .addHeader("Set-Cookie", "yo=momma")
          .create();
    }
  }
}
