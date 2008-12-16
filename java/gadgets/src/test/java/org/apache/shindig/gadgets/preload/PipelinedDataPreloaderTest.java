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

import com.google.common.collect.Lists;
import org.apache.shindig.common.ContainerConfig;
import org.apache.shindig.gadgets.http.ContentFetcherFactory;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.PipelinedData;
import org.easymock.EasyMock;
import org.json.JSONObject;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Test for PipelinedDataPreloader.
 */
public class PipelinedDataPreloaderTest extends PreloaderTestFixture {
  private ContainerConfig containerConfig;

  private static final String XML = "<Module xmlns:os=\"" + PipelinedData.OPENSOCIAL_NAMESPACE + "\">"
      + "<ModulePrefs title=\"Title\"/>"
      + "<Content href=\"http://example.org/proxied.php\" view=\"profile\">"
      + "  <os:PeopleRequest key=\"p\" userIds=\"you\"/>"
      + "  <os:PersonAppDataRequest key=\"a\" userId=\"she\"/>"
      + "</Content></Module>";

  @Before
  public void createContainerConfig() {
    containerConfig = EasyMock.createMock(ContainerConfig.class);
    EasyMock.expect(containerConfig.get(CONTAINER, "gadgets.osDataUri"))
        .andStubReturn("http://%host%/social/rpc");
    EasyMock.replay(containerConfig);
  }

  @Test
  public void testSocialPreload() throws Exception {
    GadgetSpec spec = new GadgetSpec(GADGET_URL, XML);

    String socialResult = "[{id:'p', data:1}, {id:'a', data:2}]";
    RecordingHttpFetcher fetcher = new RecordingHttpFetcher(socialResult);
    PipelinedDataPreloader preloader = new PipelinedDataPreloader(fetcher, containerConfig);
    view = "profile";
    contextParams.put("st", "token");

    Collection<Callable<PreloadedData>> tasks =
        preloader.createPreloadTasks(context, spec, PreloaderService.PreloadPhase.PROXY_FETCH);
    assertEquals(1, tasks.size());
    // Nothing fetched yet
    assertEquals(0, fetcher.requests.size());

    Map<String, Object> result = tasks.iterator().next().call().toJson();
    assertEquals(2, result.size());

    JSONObject resultWithKeyP = new JSONObject("{id: 'p', data: 1}");
    JSONObject resultWithKeyA = new JSONObject("{id: 'a', data: 2}");
    assertEquals(resultWithKeyP.toString(), result.get("p").toString());
    assertEquals(resultWithKeyA.toString(), result.get("a").toString());

    // Should have only fetched one request
    assertEquals(1, fetcher.requests.size());
    HttpRequest request = fetcher.requests.get(0);

    assertEquals("http://" + context.getHost() + "/social/rpc?st=token",
        request.getUri().toString());
    assertEquals("POST", request.getMethod());
    assertTrue(request.getContentType().startsWith("application/json"));
  }

  @Test
  public void testSocialPreloadForOtherView() throws Exception {
    GadgetSpec spec = new GadgetSpec(GADGET_URL, XML);

    String socialResult = "[{id:'p', data:1}, {id:'a', data:2}]";
    RecordingHttpFetcher fetcher = new RecordingHttpFetcher(socialResult);
    PipelinedDataPreloader preloader = new PipelinedDataPreloader(fetcher, containerConfig);
    view = "canvas";
    contextParams.put("st", "token");

    Collection<Callable<PreloadedData>> tasks =
        preloader.createPreloadTasks(context, spec, PreloaderService.PreloadPhase.PROXY_FETCH);
    assertTrue(tasks.isEmpty());
  }

  @Test
  public void testSocialPreloadForHtmlRender() throws Exception {
    GadgetSpec spec = new GadgetSpec(GADGET_URL, XML);

    String socialResult = "[{id:'p', data:1}, {id:'a', data:2}]";
    RecordingHttpFetcher fetcher = new RecordingHttpFetcher(socialResult);
    PipelinedDataPreloader preloader = new PipelinedDataPreloader(fetcher, containerConfig);
    view = "profile";
    contextParams.put("st", "token");

    Collection<Callable<PreloadedData>> tasks =
        preloader.createPreloadTasks(context, spec, PreloaderService.PreloadPhase.HTML_RENDER);
    assertTrue(tasks.isEmpty());
  }

  // TODO: test HttpPreloads

  private static class RecordingHttpFetcher extends ContentFetcherFactory {
    public final List<HttpRequest> requests = Lists.newArrayList();
    private final String content;

    public RecordingHttpFetcher(String content) {
      super(null, null);
      this.content = content;
    }

    @Override
    public HttpResponse fetch(HttpRequest request) {
      requests.add(request);
      return new HttpResponseBuilder()
          .setResponseString(content)
          .create();
    }
  }
}
