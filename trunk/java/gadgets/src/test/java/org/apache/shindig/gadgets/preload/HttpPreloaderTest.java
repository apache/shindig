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

import org.apache.shindig.common.JsonAssert;
import org.apache.shindig.gadgets.AuthType;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.apache.shindig.gadgets.http.RequestPipeline;
import org.apache.shindig.gadgets.spec.GadgetSpec;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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

  private static void checkResults(Object results, String url) throws Exception {
    Map<String, Object> expected = Maps.newHashMap();
    expected.put("body", PRELOAD_CONTENT);
    expected.put("rc", HttpResponse.SC_OK);
    expected.put("id", url);
    expected.put("headers", Collections.singletonMap("set-cookie", Arrays.asList("yo=momma")));
    expected.putAll(PRELOAD_METADATA);

    JsonAssert.assertObjectEquals(expected, results);
  }

  private static void checkResults(Object results) throws Exception {
    checkResults(results, PRELOAD_HREF);
  }

  @Test
  public void normalPreloads() throws Exception {
    String xml =
        "<Module><ModulePrefs title=''>" +
        " <Preload href='" + PRELOAD_HREF + "'/>" +
        "</ModulePrefs><Content/></Module>";
    GadgetSpec spec = new GadgetSpec(GADGET_URL, xml);
    Preloader preloader = new HttpPreloader(requestPipeline);

    Gadget gadget = new Gadget()
        .setContext(context)
        .setSpec(spec)
        .setCurrentView(spec.getView(GadgetSpec.DEFAULT_VIEW));
    Collection<Callable<PreloadedData>> preloaded =
        preloader.createPreloadTasks(gadget);

    assertEquals(1, preloaded.size());
    PreloadedData data = preloaded.iterator().next().call();

    checkRequest(plainFetcher.requests.get(0));
    assertFalse("request should not ignore cache", plainFetcher.requests.get(0).getIgnoreCache());
    checkResults(data.toJson().iterator().next());
  }

  @Test
  public void ignoreCachePreloads() throws Exception {
    String xml =
        "<Module><ModulePrefs title=''>" +
        " <Preload href='" + PRELOAD_HREF + "' authz='signed' sign_viewer='false'/>" +
        "</ModulePrefs><Content/></Module>";
    GadgetSpec spec = new GadgetSpec(GADGET_URL, xml);
    Preloader preloader = new HttpPreloader(requestPipeline);

    ignoreCache = true;

    Gadget gadget = new Gadget()
        .setContext(context)
        .setSpec(spec)
        .setCurrentView(spec.getView(GadgetSpec.DEFAULT_VIEW));
    Collection<Callable<PreloadedData>> preloaded =
        preloader.createPreloadTasks(gadget);

    assertEquals(1, preloaded.size());
    preloaded.iterator().next().call();

    HttpRequest request = oauthFetcher.requests.get(0);
    assertTrue("request should ignore cache", request.getIgnoreCache());
    checkRequest(request);
  }


  @Test
  public void signedPreloads() throws Exception {
    String xml =
        "<Module><ModulePrefs title=''>" +
        " <Preload href='" + PRELOAD_HREF + "' authz='signed' sign_viewer='false'/>" +
        "</ModulePrefs><Content/></Module>";
    GadgetSpec spec = new GadgetSpec(GADGET_URL, xml);
    Preloader preloader = new HttpPreloader(requestPipeline);

    Gadget gadget = new Gadget()
        .setContext(context)
        .setSpec(spec)
        .setCurrentView(spec.getView(GadgetSpec.DEFAULT_VIEW));
    Collection<Callable<PreloadedData>> preloaded =
        preloader.createPreloadTasks(gadget);

    assertEquals(1, preloaded.size());
    PreloadedData data = preloaded.iterator().next().call();

    HttpRequest request = oauthFetcher.requests.get(0);
    checkRequest(request);
    assertTrue(request.getOAuthArguments().getSignOwner());
    assertFalse(request.getOAuthArguments().getSignViewer());
    checkResults(data.toJson().iterator().next());
  }

  @Test
  public void oauthPreloads() throws Exception {
    String xml =
        "<Module><ModulePrefs title=''>" +
        // This is kind of a bogus test since oauth params aren't set.
        " <Preload href='" + PRELOAD_HREF + "' authz='oauth'/>" +
        "</ModulePrefs><Content/></Module>";
    GadgetSpec spec = new GadgetSpec(GADGET_URL, xml);
    Preloader preloader = new HttpPreloader(requestPipeline);

    Gadget gadget = new Gadget()
        .setContext(context)
        .setSpec(spec)
        .setCurrentView(spec.getView(GadgetSpec.DEFAULT_VIEW));
    Collection<Callable<PreloadedData>> preloaded = preloader.createPreloadTasks(
        gadget);

    assertEquals(1, preloaded.size());
    PreloadedData data = preloaded.iterator().next().call();

    HttpRequest request = oauthFetcher.requests.get(0);
    checkRequest(request);
    checkResults(data.toJson().iterator().next());
  }

  @Test
  public void multiplePreloads() throws Exception {
    String xml =
        "<Module><ModulePrefs title=''>" +
        " <Preload href='" + PRELOAD_HREF + "'/>" +
        " <Preload href='" + PRELOAD_HREF2 + "'/>" +
        "</ModulePrefs><Content/></Module>";
    GadgetSpec spec = new GadgetSpec(GADGET_URL, xml);
    Preloader preloader = new HttpPreloader(requestPipeline);

    Gadget gadget = new Gadget()
        .setContext(context)
        .setSpec(spec)
        .setCurrentView(spec.getView(GadgetSpec.DEFAULT_VIEW));
    Collection<Callable<PreloadedData>> preloaded = preloader.createPreloadTasks(
        gadget);

    assertEquals(2, preloaded.size());
    List<Object> list = getAll(preloaded);
    assertEquals(2, list.size());

    checkRequest(plainFetcher.requests.get(0));
    checkResults(list.get(0));

    checkRequest(plainFetcher.requests.get(1));
    checkResults(list.get(1), PRELOAD_HREF2);
  }

  private List<Object> getAll(Collection<Callable<PreloadedData>> preloaded) throws Exception {
    List<Object> list = Lists.newArrayList();
    for (Callable<PreloadedData> preloadCallable : preloaded) {
      list.addAll(preloadCallable.call().toJson());
    }

    return list;
  }

  @Test
  public void onlyPreloadForCorrectView() throws Exception {
    String xml =
        "<Module><ModulePrefs title=''>" +
        " <Preload href='" + PRELOAD_HREF + "' views='foo,bar,baz'/>" +
        " <Preload href='" + PRELOAD_HREF2 + "' views='bar'/>" +
        "</ModulePrefs><Content/></Module>";
    GadgetSpec spec = new GadgetSpec(GADGET_URL, xml);
    Preloader preloader = new HttpPreloader(requestPipeline);

    view = "foo";

    Gadget gadget = new Gadget()
        .setContext(context)
        .setSpec(spec)
        .setCurrentView(spec.getView(GadgetSpec.DEFAULT_VIEW));
    Collection<Callable<PreloadedData>> preloaded
        = preloader.createPreloadTasks(gadget);

    List<Object> list = getAll(preloaded);
    assertEquals(1, list.size());
    checkRequest(plainFetcher.requests.get(0));
    checkResults(list.get(0));
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
