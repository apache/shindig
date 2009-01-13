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
import static org.junit.Assert.assertTrue;

import org.apache.shindig.gadgets.AuthType;
import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.apache.shindig.gadgets.http.RequestPipeline;
import org.apache.shindig.gadgets.spec.GadgetSpec;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

/**
 * Tests for HttpPreloader.
 */
public class HttpPreloaderTest extends PreloaderTestFixture {
  private static final String PRELOAD_HREF = "http://www.example.org/file";
  private static final String PRELOAD_HREF2 = "http://www.example.org/file-two";
  private static final String PRELOAD_CONTENT = "Preloaded data";
  protected static final Map<String, String> PRELOAD_METADATA = ImmutableMap.of("foo", "bar");
  protected final RecordingHttpFetcher plainFetcher = new RecordingHttpFetcher();
  protected final RecordingHttpFetcher oauthFetcher = new RecordingHttpFetcher();

  private final RequestPipeline requestPipeline = new RequestPipeline() {
    public HttpResponse execute(HttpRequest request) {
      if (request.getAuthType() == AuthType.NONE) {
        return plainFetcher.fetch(request);
      }
      return oauthFetcher.fetch(request);
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
  public void normalPreloads() throws Exception {
    String xml =
        "<Module><ModulePrefs title=''>" +
        " <Preload href='" + PRELOAD_HREF + "'/>" +
        "</ModulePrefs><Content/></Module>";
    GadgetSpec gadget = new GadgetSpec(GADGET_URL, xml);
    Preloader preloader = new HttpPreloader(requestPipeline);

    Collection<Callable<PreloadedData>> preloaded =
        preloader.createPreloadTasks(context, gadget, PreloaderService.PreloadPhase.HTML_RENDER);

    assertEquals(1, preloaded.size());
    PreloadedData data = preloaded.iterator().next().call();

    checkRequest(plainFetcher.requests.get(0));
    checkResults((JSONObject) data.toJson().get(PRELOAD_HREF));
  }

  @Test
  public void signedPreloads() throws Exception {
    String xml =
        "<Module><ModulePrefs title=''>" +
        " <Preload href='" + PRELOAD_HREF + "' authz='signed' sign_viewer='false'/>" +
        "</ModulePrefs><Content/></Module>";
    GadgetSpec gadget = new GadgetSpec(GADGET_URL, xml);
    Preloader preloader = new HttpPreloader(requestPipeline);

    Collection<Callable<PreloadedData>> preloaded =
        preloader.createPreloadTasks(context, gadget, PreloaderService.PreloadPhase.HTML_RENDER);

    assertEquals(1, preloaded.size());
    PreloadedData data = preloaded.iterator().next().call();

    HttpRequest request = oauthFetcher.requests.get(0);
    checkRequest(request);
    assertTrue(request.getOAuthArguments().getSignOwner());
    assertFalse(request.getOAuthArguments().getSignViewer());
    checkResults((JSONObject) data.toJson().get(PRELOAD_HREF));
  }

  @Test
  public void oauthPreloads() throws Exception {
    String xml =
        "<Module><ModulePrefs title=''>" +
        // This is kind of a bogus test since oauth params aren't set.
        " <Preload href='" + PRELOAD_HREF + "' authz='oauth'/>" +
        "</ModulePrefs><Content/></Module>";
    GadgetSpec gadget = new GadgetSpec(GADGET_URL, xml);
    Preloader preloader = new HttpPreloader(requestPipeline);

    Collection<Callable<PreloadedData>> preloaded = preloader.createPreloadTasks(
        context, gadget, PreloaderService.PreloadPhase.HTML_RENDER);

    assertEquals(1, preloaded.size());
    PreloadedData data = preloaded.iterator().next().call();

    HttpRequest request = oauthFetcher.requests.get(0);
    checkRequest(request);
    checkResults((JSONObject) data.toJson().get(PRELOAD_HREF));
  }

  @Test
  public void multiplePreloads() throws Exception {
    String xml =
        "<Module><ModulePrefs title=''>" +
        " <Preload href='" + PRELOAD_HREF + "'/>" +
        " <Preload href='" + PRELOAD_HREF2 + "'/>" +
        "</ModulePrefs><Content/></Module>";
    GadgetSpec gadget = new GadgetSpec(GADGET_URL, xml);
    Preloader preloader = new HttpPreloader(requestPipeline);

    Collection<Callable<PreloadedData>> preloaded = preloader.createPreloadTasks(
        context, gadget, PreloaderService.PreloadPhase.HTML_RENDER);

    assertEquals(2, preloaded.size());
    Map<String, Object> map = getAll(preloaded);

    checkRequest(plainFetcher.requests.get(0));
    checkResults((JSONObject) map.get(PRELOAD_HREF));

    checkRequest(plainFetcher.requests.get(1));
    checkResults((JSONObject) map.get(PRELOAD_HREF2));
  }

  private Map<String, Object> getAll(
      Collection<Callable<PreloadedData>> preloaded) throws Exception {
    Map<String, Object> map = Maps.newHashMap();
    for (Callable<PreloadedData> preloadCallable : preloaded) {
      map.putAll(preloadCallable.call().toJson());
    }

    return map;
  }

  @Test
  public void onlyPreloadForCorrectView() throws Exception {
    String xml =
        "<Module><ModulePrefs title=''>" +
        " <Preload href='" + PRELOAD_HREF + "' views='foo,bar,baz'/>" +
        " <Preload href='" + PRELOAD_HREF2 + "' views='bar'/>" +
        "</ModulePrefs><Content/></Module>";
    GadgetSpec gadget = new GadgetSpec(GADGET_URL, xml);
    Preloader preloader = new HttpPreloader(requestPipeline);

    view = "foo";

    Collection<Callable<PreloadedData>> preloaded
        = preloader.createPreloadTasks(context, gadget, PreloaderService.PreloadPhase.HTML_RENDER);

    Map<String, Object> map = getAll(preloaded);

    checkRequest(plainFetcher.requests.get(0));
    checkResults((JSONObject) map.get(PRELOAD_HREF));

    assertFalse("Preloaded an item that should not have been.", map.containsKey(PRELOAD_HREF2));
  }

  @Test
  public void proxiedPreloadIsEmpty() throws Exception {
    String xml =
        "<Module><ModulePrefs title=''>" +
        " <Preload href='" + PRELOAD_HREF + "'/>" +
        "</ModulePrefs><Content/></Module>";
    GadgetSpec gadget = new GadgetSpec(GADGET_URL, xml);
    Preloader preloader = new HttpPreloader(requestPipeline);

    Collection<Callable<PreloadedData>> preloaded =
        preloader.createPreloadTasks(context, gadget, PreloaderService.PreloadPhase.PROXY_FETCH);

    assertEquals(0, preloaded.size());
  }

  private static class RecordingHttpFetcher implements HttpFetcher {
    protected final List<HttpRequest> requests = Lists.newArrayList();

    protected RecordingHttpFetcher() {
    }

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