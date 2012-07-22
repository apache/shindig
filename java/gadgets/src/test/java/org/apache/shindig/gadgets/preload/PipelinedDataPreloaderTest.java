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
import org.apache.shindig.common.JsonSerializer;
import org.apache.shindig.common.JsonUtil;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.expressions.Expressions;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetELResolver;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.apache.shindig.gadgets.http.RequestPipeline;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.PipelinedData;
import org.apache.shindig.gadgets.spec.PipelinedData.Batch;
import org.easymock.EasyMock;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Test for PipelinedDataPreloader.
 */
public class PipelinedDataPreloaderTest extends PreloaderTestFixture {
  private ContainerConfig containerConfig;
  private final Expressions expressions = Expressions.forTesting();

  private static final String XML = "<Module xmlns:os=\"" + PipelinedData.OPENSOCIAL_NAMESPACE
      + "\">" + "<ModulePrefs title=\"Title\"/>"
      + "<Content href=\"http://example.org/proxied.php\" view=\"profile\">"
      + "  <os:PeopleRequest key=\"p\" userIds=\"you\"/>"
      + "  <os:PersonAppDataRequest key=\"a\" userId=\"she\"/>" + "</Content></Module>";

  private static final String HTTP_REQUEST_URL =  "http://example.org/preload.html";
  private static final String PARAMS = "a=b&c=d";
  private static final String XML_PARAMS = "a=b&amp;c=d";

  private static final String XML_WITH_HTTP_REQUEST = "<Module xmlns:os=\""
      + PipelinedData.OPENSOCIAL_NAMESPACE + "\">"
      + "<ModulePrefs title=\"Title\"/>"
      + "<Content href=\"http://example.org/proxied.php\" view=\"profile\">"
      + "  <os:HttpRequest key=\"p\" href=\"" + HTTP_REQUEST_URL + "\" "
      + "refreshInterval=\"60\" method=\"POST\"/>" + "</Content></Module>";

  private static final String XML_WITH_VARIABLE = "<Module " +
      "xmlns:os=\"" + PipelinedData.OPENSOCIAL_NAMESPACE + "\" " +
        "xmlns:osx=\"" + PipelinedData.EXTENSION_NAMESPACE + "\">"
    + "<ModulePrefs title=\"Title\"/>"
    + "<Content href=\"http://example.org/proxied.php\" view=\"profile\">"
    + "  <osx:Variable key=\"p\" value=\"${1+1}\"/>" + "</Content></Module>";

  private static final String XML_WITH_HTTP_REQUEST_FOR_TEXT = "<Module xmlns:os=\""
    + PipelinedData.OPENSOCIAL_NAMESPACE + "\">"
    + "<ModulePrefs title=\"Title\"/>"
    + "<Content href=\"http://example.org/proxied.php\" view=\"profile\">"
    + "  <os:HttpRequest key=\"p\" format=\"text\" href=\"" + HTTP_REQUEST_URL + "\" "
    + "refreshInterval=\"60\" method=\"POST\"/>" + "</Content></Module>";

  private static final String XML_WITH_HTTP_REQUEST_AND_PARAMS = "<Module xmlns:os=\""
    + PipelinedData.OPENSOCIAL_NAMESPACE + "\">"
    + "<ModulePrefs title=\"Title\"/>"
    + "<Content href=\"http://example.org/proxied.php\" view=\"profile\">"
    + "  <os:HttpRequest key=\"p\" href=\"" + HTTP_REQUEST_URL + "\" "
    + "                  method=\"POST\" params=\"" + XML_PARAMS + "\"/>"
    + "</Content></Module>";

  private static final String XML_WITH_HTTP_REQUEST_AND_GET_PARAMS = "<Module xmlns:os=\""
    + PipelinedData.OPENSOCIAL_NAMESPACE + "\">"
    + "<ModulePrefs title=\"Title\"/>"
    + "<Content href=\"http://example.org/proxied.php\" view=\"profile\">"
    + "  <os:HttpRequest key=\"p\" href=\"" + HTTP_REQUEST_URL + "\" "
    + "                  method=\"GET\" params=\"" + XML_PARAMS + "\"/>"
    + "</Content></Module>";

  private static final String XML_IN_DEFAULT_CONTAINER = "<Module xmlns:os=\""
    + PipelinedData.OPENSOCIAL_NAMESPACE + "\">" + "<ModulePrefs title=\"Title\"/>"
    + "<Content href=\"http://example.org/proxied.php\">"
    + "  <os:PeopleRequest key=\"p\" userIds=\"you\"/>"
    + "  <os:PersonAppDataRequest key=\"a\" userId=\"she\"/>" + "</Content></Module>";

  @Before
  public void createContainerConfig() {
    containerConfig = EasyMock.createMock(ContainerConfig.class);
    EasyMock.expect(containerConfig.getString(CONTAINER, "gadgets.osDataUri")).andStubReturn(
        "http://%host%/social/rpc");
    EasyMock.replay(containerConfig);
  }

  @Test
  public void testSocialPreload() throws Exception {
    GadgetSpec spec = new GadgetSpec(GADGET_URL, XML);

    String socialResult = "[{id:'p', result:1}, {id:'a', result:2}]";
    RecordingRequestPipeline pipeline = new RecordingRequestPipeline(socialResult);
    PipelinedDataPreloader preloader = new PipelinedDataPreloader(pipeline, containerConfig);

    view = "profile";
    contextParams.put("st", "token");

    Gadget gadget = new Gadget()
        .setContext(context)
        .setSpec(spec)
        .setCurrentView(spec.getView("profile"));

    PipelinedData.Batch batch = getBatch(gadget);
    Collection<Callable<PreloadedData>> tasks = preloader.createPreloadTasks(
        context, batch);
    assertEquals(1, tasks.size());
    // Nothing fetched yet
    assertEquals(0, pipeline.requests.size());

    Collection<Object> result = tasks.iterator().next().call().toJson();
    assertEquals(2, result.size());

    JSONObject resultWithKeyP = new JSONObject("{id: 'p', result: 1}");
    JSONObject resultWithKeyA = new JSONObject("{id: 'a', result: 2}");
    Map<String, String> resultsById = getResultsById(result);
    JsonAssert.assertJsonEquals(resultWithKeyA.toString(), resultsById.get("a"));
    JsonAssert.assertJsonEquals(resultWithKeyP.toString(), resultsById.get("p"));

    // Should have only fetched one request
    assertEquals(1, pipeline.requests.size());
    HttpRequest request = pipeline.requests.get(0);

    assertEquals("http://" + context.getHost() + "/social/rpc?st=token", request.getUri()
        .toString());
    assertEquals("POST", request.getMethod());
    assertTrue(request.getContentType().startsWith("application/json"));
  }

  @Test
  public void testSocialPreloadWithBatchError() throws Exception {
    GadgetSpec spec = new GadgetSpec(GADGET_URL, XML);

    String socialResult = "{code: 401, message: 'unauthorized'}";
    RecordingRequestPipeline pipeline = new RecordingRequestPipeline(socialResult);
    PipelinedDataPreloader preloader = new PipelinedDataPreloader(pipeline, containerConfig);

    view = "profile";
    contextParams.put("st", "token");

    Gadget gadget = new Gadget()
        .setContext(context)
        .setSpec(spec)
        .setCurrentView(spec.getView("profile"));

    PipelinedData.Batch batch = getBatch(gadget);
    Collection<Callable<PreloadedData>> tasks = preloader.createPreloadTasks(
        context, batch);
    assertEquals(1, tasks.size());
    // Nothing fetched yet
    assertEquals(0, pipeline.requests.size());

    Collection<Object> result = tasks.iterator().next().call().toJson();
    assertEquals(2, result.size());

    JSONObject resultWithKeyP = new JSONObject("{id: 'p', error: {code: 401, message: 'unauthorized'}}");
    JSONObject resultWithKeyA = new JSONObject("{id: 'a', error: {code: 401, message: 'unauthorized'}}");
    Map<String, String> resultsById = getResultsById(result);
    JsonAssert.assertJsonEquals(resultWithKeyA.toString(), resultsById.get("a"));
    JsonAssert.assertJsonEquals(resultWithKeyP.toString(), resultsById.get("p"));
  }

  @Test
  public void testSocialPreloadWithHttpError() throws Exception {
    GadgetSpec spec = new GadgetSpec(GADGET_URL, XML);

    HttpResponse httpError = new HttpResponseBuilder()
        .setHttpStatusCode(HttpResponse.SC_INTERNAL_SERVER_ERROR)
        .create();
    RecordingRequestPipeline pipeline = new RecordingRequestPipeline(httpError);
    PipelinedDataPreloader preloader = new PipelinedDataPreloader(pipeline, containerConfig);

    view = "profile";
    contextParams.put("st", "token");

    Gadget gadget = new Gadget()
        .setContext(context)
        .setSpec(spec)
        .setCurrentView(spec.getView("profile"));

    PipelinedData.Batch batch = getBatch(gadget);
    Collection<Callable<PreloadedData>> tasks = preloader.createPreloadTasks(
        context, batch);

    Collection<Object> result = tasks.iterator().next().call().toJson();
    assertEquals(2, result.size());

    JSONObject resultWithKeyP = new JSONObject("{id: 'p', error: {code: 500}}");
    JSONObject resultWithKeyA = new JSONObject("{id: 'a', error: {code: 500}}");
    Map<String, String> resultsById = getResultsById(result);
    JsonAssert.assertJsonEquals(resultWithKeyA.toString(), resultsById.get("a"));
    JsonAssert.assertJsonEquals(resultWithKeyP.toString(), resultsById.get("p"));
  }

  @Test
  /**
   * Verify that social preloads where the request doesn't contain a token
   * serve up 403s for the preloaded data, instead of failing the whole request.
   */
  public void testSocialPreloadWithoutToken() throws Exception {
    GadgetSpec spec = new GadgetSpec(GADGET_URL, XML);

    RecordingRequestPipeline pipeline = new RecordingRequestPipeline("");
    PipelinedDataPreloader preloader = new PipelinedDataPreloader(pipeline, containerConfig);
    view = "profile";
    // But don't set the security token

    Gadget gadget = new Gadget()
        .setContext(context)
        .setSpec(spec)
        .setCurrentView(spec.getView("profile"));
    PipelinedData.Batch batch = getBatch(gadget);
    Collection<Callable<PreloadedData>> tasks = preloader.createPreloadTasks(
        context, batch);
    PreloadedData data = tasks.iterator().next().call();
    JSONObject resultWithKeyA = new JSONObject(
        "{error:{code:403,data:{content:\"Security token missing\"}},id:\"a\"}");
    JSONObject resultWithKeyP = new JSONObject(
        "{error:{code:403,data:{content:\"Security token missing\"}},id:\"p\"}");
    Collection<Object> result = data.toJson();
    assertEquals(2, result.size());
    Map<String, String> resultsById = getResultsById(result);
    JsonAssert.assertJsonEquals(resultWithKeyA.toString(), resultsById.get("a"));
    JsonAssert.assertJsonEquals(resultWithKeyP.toString(), resultsById.get("p"));
  }

  private Map<String, String> getResultsById(Collection<Object> result) {
    Map<String, String> resultsById = Maps.newHashMap();
    for (Object o : result) {
      resultsById.put((String) JsonUtil.getProperty(o, "id"),
          JsonSerializer.serialize(o));
    }

    return resultsById;
  }

  private Batch getBatch(Gadget gadget) {
    return gadget.getCurrentView().getPipelinedData().getBatch(expressions,
        new GadgetELResolver(gadget.getContext()));
  }

  @Test
  public void testHttpPreloadOfJsonObject() throws Exception {
    HttpResponse response = new HttpResponseBuilder()
        .setResponseString("{foo: 'bar'}")
        .create();
    String expectedResult = "{result: {status: 200, content: {foo: 'bar'}}, id: 'p'}";

    verifyHttpPreload(response, expectedResult);
  }

  @Test
  public void testHttpPreloadOfJsonArrayWithHeaders() throws Exception {
    HttpResponse response = new HttpResponseBuilder()
        .setResponseString("[1, 2]")
        .addHeader("content-type", "application/json")
        .addHeader("set-cookie", "cookiecookie")
        .addHeader("not-ok", "shouldn'tbehere")
        .create();

    String expectedResult = "{result: {status: 200, headers:" +
        "{'content-type': ['application/json; charset=UTF-8'], 'set-cookie': ['cookiecookie']}," +
        "content: [1, 2]}, id: 'p'}";

    verifyHttpPreload(response, expectedResult);
  }

  @Test
  public void testHttpPreloadOfJsonWithErrorCode() throws Exception {
    HttpResponse response = new HttpResponseBuilder()
        .setResponseString("not found")
        .addHeader("content-type", "text/html")
        .setHttpStatusCode(HttpResponse.SC_NOT_FOUND)
        .create();

    String expectedResult = "{error: {code: 404, data:" +
        "{headers: {'content-type': ['text/html; charset=UTF-8']}," +
            "content: 'not found'}}, id: 'p'}";

    verifyHttpPreload(response, expectedResult);
  }

  @Test
  public void testHttpPreloadWithBadJson() throws Exception {
    HttpResponse response = new HttpResponseBuilder()
        .setResponseString("notjson")
        .addHeader("content-type", "text/html")
        .create();

    JSONObject result = new JSONObject(executeHttpPreload(response, XML_WITH_HTTP_REQUEST));
    assertFalse(result.has("result"));

    JSONObject error = result.getJSONObject("error");
    assertEquals(HttpResponse.SC_NOT_ACCEPTABLE, error.getInt("code"));
  }

  @Test
  public void testHttpPreloadOfText() throws Exception {
    HttpResponse response = new HttpResponseBuilder()
        .setResponseString("{foo: 'bar'}")
        .addHeader("content-type", "application/json")
        .create();
    // Even though the response was actually JSON, @format=text, so the content
    // will be a block of text
    String expectedResult = "{result: {status: 200, headers:" +
            "{'content-type': ['application/json; charset=UTF-8']}," +
            "content: '{foo: \\'bar\\'}'}, id: 'p'}";

    String resultString = executeHttpPreload(response, XML_WITH_HTTP_REQUEST_FOR_TEXT);
    JsonAssert.assertJsonEquals(expectedResult, resultString);
  }

  private void verifyHttpPreload(HttpResponse response, String expectedJson) throws Exception {
    String resultString = executeHttpPreload(response, XML_WITH_HTTP_REQUEST);
    JsonAssert.assertJsonEquals(expectedJson, resultString);
  }

  /**
   * Run an HTTP Preload test, returning the String result.
   */
  private String executeHttpPreload(HttpResponse response, String xml) throws Exception {
    GadgetSpec spec = new GadgetSpec(GADGET_URL, xml);

    RecordingRequestPipeline pipeline = new RecordingRequestPipeline(response);
    PipelinedDataPreloader preloader = new PipelinedDataPreloader(pipeline, containerConfig);
    view = "profile";

    Gadget gadget = new Gadget()
        .setContext(context)
        .setSpec(spec)
        .setCurrentView(spec.getView("profile"));

    PipelinedData.Batch batch = getBatch(gadget);
    Collection<Callable<PreloadedData>> tasks = preloader.createPreloadTasks(
        context, batch);
    assertEquals(1, tasks.size());
    // Nothing fetched yet
    assertEquals(0, pipeline.requests.size());

    Collection<Object> result = tasks.iterator().next().call().toJson();
    assertEquals(1, result.size());

    // Should have only fetched one request
    assertEquals(1, pipeline.requests.size());
    HttpRequest request = pipeline.requests.get(0);

    assertEquals(HTTP_REQUEST_URL, request.getUri().toString());
    assertEquals("POST", request.getMethod());
    assertEquals(60, request.getCacheTtl());

    return result.iterator().next().toString();
  }

  @Test
  public void testHttpPreloadWithPostParams() throws Exception {
    GadgetSpec spec = new GadgetSpec(GADGET_URL, XML_WITH_HTTP_REQUEST_AND_PARAMS);

    String httpResult = "{foo: 'bar'}";
    RecordingRequestPipeline pipeline = new RecordingRequestPipeline(httpResult);
    PipelinedDataPreloader preloader = new PipelinedDataPreloader(pipeline, containerConfig);
    view = "profile";

    Gadget gadget = new Gadget()
        .setContext(context)
        .setSpec(spec)
        .setCurrentView(spec.getView("profile"));
    PipelinedData.Batch batch = getBatch(gadget);
    Collection<Callable<PreloadedData>> tasks = preloader.createPreloadTasks(
        context, batch);
    tasks.iterator().next().call();

    // Should have only fetched one request
    assertEquals(1, pipeline.requests.size());
    HttpRequest request = pipeline.requests.get(0);

    assertEquals(HTTP_REQUEST_URL, request.getUri().toString());
    assertEquals("POST", request.getMethod());
    assertEquals(PARAMS, request.getPostBodyAsString());
  }

  @Test
  public void testHttpPreloadWithGetParams() throws Exception {
    GadgetSpec spec = new GadgetSpec(GADGET_URL, XML_WITH_HTTP_REQUEST_AND_GET_PARAMS);

    String httpResult = "{foo: 'bar'}";
    RecordingRequestPipeline pipeline = new RecordingRequestPipeline(httpResult);
    PipelinedDataPreloader preloader = new PipelinedDataPreloader(pipeline, containerConfig);
    view = "profile";

    Gadget gadget = new Gadget()
        .setContext(context)
        .setSpec(spec)
        .setCurrentView(spec.getView("profile"));
    PipelinedData.Batch batch = getBatch(gadget);
    Collection<Callable<PreloadedData>> tasks = preloader.createPreloadTasks(
        context, batch);
    tasks.iterator().next().call();

    // Should have only fetched one request
    assertEquals(1, pipeline.requests.size());
    HttpRequest request = pipeline.requests.get(0);

    assertEquals(HTTP_REQUEST_URL + '?' + PARAMS, request.getUri().toString());
    assertEquals("GET", request.getMethod());
  }

  /**
   * Verify that social preloads pay attention to view resolution by
   * using gadget.getCurrentView().
   */
  @Test
  public void testSocialPreloadViewResolution() throws Exception {
    GadgetSpec spec = new GadgetSpec(GADGET_URL, XML_IN_DEFAULT_CONTAINER);

    String socialResult = "[{id:'p', result:1}, {id:'a', result:2}]";
    RecordingRequestPipeline pipeline = new RecordingRequestPipeline(socialResult);
    PipelinedDataPreloader preloader = new PipelinedDataPreloader(pipeline, containerConfig);

    view = "profile";
    contextParams.put("st", "token");

    Gadget gadget = new Gadget()
        .setContext(context)
        .setSpec(spec)
        // Assume view resolution has behaved correctly
        .setCurrentView(spec.getView(GadgetSpec.DEFAULT_VIEW));

    PipelinedData.Batch batch = getBatch(gadget);
    Collection<Callable<PreloadedData>> tasks = preloader.createPreloadTasks(
        context, batch);
    assertEquals(1, tasks.size());
  }

  @Test
  public void testVariablePreload() throws Exception {
    GadgetSpec spec = new GadgetSpec(GADGET_URL, XML_WITH_VARIABLE);

    RecordingRequestPipeline pipeline = new RecordingRequestPipeline("");
    PipelinedDataPreloader preloader = new PipelinedDataPreloader(pipeline, containerConfig);

    view = "profile";
    contextParams.put("st", "token");

    Gadget gadget = new Gadget()
        .setContext(context)
        .setSpec(spec)
        .setCurrentView(spec.getView("profile"));

    PipelinedData.Batch batch = getBatch(gadget);
    Collection<Callable<PreloadedData>> tasks = preloader.createPreloadTasks(
        context, batch);
    assertEquals(1, tasks.size());
    // Nothing fetched yet
    assertEquals(0, pipeline.requests.size());

    Collection<Object> result = tasks.iterator().next().call().toJson();
    assertEquals(1, result.size());

    JsonAssert.assertObjectEquals("{id: 'p', result: 2}", result.iterator().next());
  }

  private static class RecordingRequestPipeline implements RequestPipeline {
    public final List<HttpRequest> requests = Lists.newArrayList();
    private final HttpResponse response;

    public RecordingRequestPipeline(String content) {
      this(new HttpResponseBuilder().setResponseString(content).create());
    }

    public RecordingRequestPipeline(HttpResponse response) {
      this.response = response;
    }

    public HttpResponse execute(HttpRequest request) {
      requests.add(request);
      return response;
    }
  }
}
