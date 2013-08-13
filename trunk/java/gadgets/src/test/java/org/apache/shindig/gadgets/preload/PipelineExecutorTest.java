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

import static org.easymock.EasyMock.and;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.reportMatcher;
import static org.easymock.EasyMock.same;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.apache.shindig.common.JsonAssert;
import org.apache.shindig.common.JsonSerializer;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.xml.XmlUtil;
import org.apache.shindig.expressions.Expressions;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.spec.PipelinedData;
import org.apache.shindig.gadgets.spec.RequestAuthenticationInfo;
import org.apache.shindig.gadgets.spec.SpecParserException;
import org.easymock.Capture;
import org.easymock.IArgumentMatcher;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Element;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public class PipelineExecutorTest {

  private IMocksControl control;
  private PipelinedDataPreloader preloader;
  private PreloaderService preloaderService;
  private GadgetContext context;
  private PipelineExecutor executor;

  private static final Uri GADGET_URI = Uri.parse("http://example.org/gadget.php");

  private static final String CONTENT =
    "<Content xmlns:os=\"http://ns.opensocial.org/2008/markup\">"
      + "  <os:PeopleRequest key=\"me\" userId=\"canonical\"/>"
      + "  <os:HttpRequest key=\"json\" href=\"test.json\"/>"
      + "</Content>";

  // Two requests, one depends on the other
  private static final String TWO_BATCH_CONTENT =
    "<Content xmlns:os=\"http://ns.opensocial.org/2008/markup\">"
    + "  <os:PeopleRequest key=\"me\" userId=\"${json.user}\"/>"
    + "  <os:HttpRequest key=\"json\" href=\"${ViewParams.file}\"/>"
    + "</Content>";

  // One request, but it requires data that isn\"t present
  private static final String BLOCKED_FIRST_BATCH_CONTENT =
    "<Content xmlns:os=\"http://ns.opensocial.org/2008/markup\">"
    + "  <os:PeopleRequest key=\"me\" userId=\"${json.user}\"/>"
    + "</Content>";

  @Before
  public void setUp() throws Exception {
    control = EasyMock.createStrictControl();
    preloader = control.createMock(PipelinedDataPreloader.class);
    preloaderService = new ConcurrentPreloaderService(Executors.newSingleThreadExecutor(), null);
    executor = new PipelineExecutor(preloader, preloaderService, Expressions.forTesting());

    context = new GadgetContext(){};
  }

  private PipelinedData getPipelinedData(String pipelineXml) throws SpecParserException {
    Element element = XmlUtil.parseSilent(pipelineXml);
    return new PipelinedData(element, GADGET_URI);
  }

  @Test
  public void execute() throws Exception {
    PipelinedData pipeline = getPipelinedData(CONTENT);

    Capture<PipelinedData.Batch> batchCapture =
      new Capture<PipelinedData.Batch>();

    JSONObject expectedData = new JSONObject("{result: {foo: 'bar'}}");

    // Dummy return results (the "real" return would have two values)
    Callable<PreloadedData> callable = createPreloadTask("key", expectedData.toString());

    // One batch with 1 each HTTP and Social preload
    expect(preloader.createPreloadTasks(same(context),
            and(eqBatch(1, 1), capture(batchCapture))))
            .andReturn(ImmutableList.of(callable));

    control.replay();

    PipelineExecutor.Results results = executor.execute(context,
        ImmutableList.of(pipeline));

    // Verify the data set is injected, and the os-data was deleted
    assertTrue(batchCapture.getValue().getPreloads().containsKey("me"));
    assertTrue(batchCapture.getValue().getPreloads().containsKey("json"));

    JsonAssert.assertJsonEquals("[{id: 'key', result: {foo: 'bar'}}]",
        JsonSerializer.serialize(results.results));
    JsonAssert.assertJsonEquals("{foo: 'bar'}",
        JsonSerializer.serialize(results.keyedResults.get("key")));
    assertTrue(results.remainingPipelines.isEmpty());

    control.verify();
  }

  @Test
  public void executeWithTwoBatches() throws Exception {
    PipelinedData pipeline = getPipelinedData(TWO_BATCH_CONTENT);

    context = new GadgetContext() {
      @Override
      public String getParameter(String property) {
        // Provide the filename to be requested in the first batch
        if ("view-params".equals(property)) {
          return "{'file': 'test.json'}";
        }
        return null;
      }
    };

    // First batch, the HTTP fetch
    Capture<PipelinedData.Batch> firstBatch =
      new Capture<PipelinedData.Batch>();
    Callable<PreloadedData> firstTask = createPreloadTask("json",
        "{result: {user: 'canonical'}}");

    // Second batch, the user fetch
    Capture<PipelinedData.Batch> secondBatch =
      new Capture<PipelinedData.Batch>();
    Callable<PreloadedData> secondTask = createPreloadTask("me",
        "{result: {'id':'canonical'}}");

    // First, a batch with an HTTP request
    expect(
        preloader.createPreloadTasks(same(context),
            and(eqBatch(0, 1), capture(firstBatch))))
            .andReturn(ImmutableList.of(firstTask));
    // Second, a batch with a social request
    expect(
        preloader.createPreloadTasks(same(context),
            and(eqBatch(1, 0), capture(secondBatch))))
            .andReturn(ImmutableList.of(secondTask));

    control.replay();

    PipelineExecutor.Results results = executor.execute(context,
        ImmutableList.of(pipeline));

    JsonAssert.assertJsonEquals("[{id: 'json', result: {user: 'canonical'}}," +
        "{id: 'me', result: {id: 'canonical'}}]",
        JsonSerializer.serialize(results.results));
    assertEquals(ImmutableSet.of("json", "me"), results.keyedResults.keySet());
    assertTrue(results.remainingPipelines.isEmpty());

    control.verify();

    // Verify the data set is injected, and the os-data was deleted

    // Check the evaluated HTTP request
    RequestAuthenticationInfo request = (RequestAuthenticationInfo)
        firstBatch.getValue().getPreloads().get("json").getData();
    assertEquals("http://example.org/test.json", request.getHref().toString());

    // Check the evaluated person request
    JSONObject personRequest = (JSONObject) secondBatch.getValue().getPreloads().get("me").getData();
    assertEquals("canonical", personRequest.getJSONObject("params").getJSONArray("userId").get(0));
  }

  @Test
  public void executeWithBlockedBatch() throws Exception {
    PipelinedData pipeline = getPipelinedData(BLOCKED_FIRST_BATCH_CONTENT);

    // Expect a batch with no content
    expect(
        preloader.createPreloadTasks(same(context), eqBatch(0, 0)))
            .andReturn(ImmutableList.<Callable<PreloadedData>>of());

    control.replay();

    PipelineExecutor.Results results = executor.execute(context,
        ImmutableList.of(pipeline));
    assertEquals(0, results.results.size());
    assertTrue(results.keyedResults.isEmpty());
    assertEquals(1, results.remainingPipelines.size());
    assertSame(pipeline, results.remainingPipelines.iterator().next());

    control.verify();
  }

  @Test
  public void executeError() throws Exception {
    PipelinedData pipeline = getPipelinedData(CONTENT);

    Capture<PipelinedData.Batch> batchCapture =
      new Capture<PipelinedData.Batch>();

    JSONObject expectedData = new JSONObject("{error: {message: 'NO!', code: 500}}");

    // Dummy return results (the "real" return would have two values)
    Callable<PreloadedData> callable = createPreloadTask("key", expectedData.toString());

    // One batch with 1 each HTTP and Social preload
    expect(preloader.createPreloadTasks(same(context),
            and(eqBatch(1, 1), capture(batchCapture))))
            .andReturn(ImmutableList.of(callable));

    control.replay();

    PipelineExecutor.Results results = executor.execute(context,
        ImmutableList.of(pipeline));

    // Verify the data set is injected, and the os-data was deleted
    assertTrue(batchCapture.getValue().getPreloads().containsKey("me"));
    assertTrue(batchCapture.getValue().getPreloads().containsKey("json"));

    JsonAssert.assertJsonEquals("[{id: 'key', error: {message: 'NO!', code: 500}}]",
        JsonSerializer.serialize(results.results));
    JsonAssert.assertJsonEquals("{message: 'NO!', code: 500}",
        JsonSerializer.serialize(results.keyedResults.get("key")));
    assertTrue(results.remainingPipelines.isEmpty());

    control.verify();
  }

  @Test
  public void executePreloadException() throws Exception {
    PipelinedData pipeline = getPipelinedData(CONTENT);
    final PreloadedData willThrow = control.createMock(PreloadedData.class);

    Callable<PreloadedData> callable = new Callable<PreloadedData>() {
      public PreloadedData call() throws Exception {
        return willThrow;
      }
    };

    // One batch
    expect(preloader.createPreloadTasks(same(context),
        isA(PipelinedData.Batch.class))).andReturn(ImmutableList.of(callable));
    // And PreloadedData that throws an exception
    expect(willThrow.toJson()).andThrow(new PreloadException("Failed"));


    control.replay();

    PipelineExecutor.Results results = executor.execute(context,
        ImmutableList.of(pipeline));

    // The exception is fully handled, and leads to empty results
    assertEquals(0, results.results.size());
    assertTrue(results.keyedResults.isEmpty());
    assertTrue(results.remainingPipelines.isEmpty());

    control.verify();
  }

  /** Match a batch with the specified count of social and HTTP data items */
  private PipelinedData.Batch eqBatch(int socialCount, int httpCount) {
    reportMatcher(new BatchMatcher(socialCount, httpCount));
    return null;
  }

  private static class BatchMatcher implements IArgumentMatcher {
    private final int socialCount;
    private final int httpCount;

    public BatchMatcher(int socialCount, int httpCount) {
      this.socialCount = socialCount;
      this.httpCount = httpCount;
    }

    public void appendTo(StringBuffer buffer) {
      buffer.append("eqBuffer[social=").append(socialCount).append(",http=").append(httpCount).append(']');
    }

    public boolean matches(Object obj) {
      if (!(obj instanceof PipelinedData.Batch)) {
        return false;
      }

      PipelinedData.Batch batch = (PipelinedData.Batch) obj;
      int actualSocialCount = 0;
      int actualHttpCount = 0;
      for (PipelinedData.BatchItem item : batch.getPreloads().values()) {
        if (item.getType() == PipelinedData.BatchType.HTTP) {
          actualHttpCount++;
        } else if (item.getType() == PipelinedData.BatchType.SOCIAL) {
          actualSocialCount++;
        }
      }

      return socialCount == actualSocialCount && httpCount == actualHttpCount;
    }

  }
  /** Create a mock Callable for a single preload task */
  private Callable<PreloadedData> createPreloadTask(final String key, String jsonResult)
      throws JSONException {
    final JSONObject value = new JSONObject(jsonResult);
    value.put("id", key);
    final PreloadedData preloadResult = new PreloadedData() {
      public Collection<Object> toJson() throws PreloadException {
        return ImmutableList.<Object>of(value);
      }
    };

    Callable<PreloadedData> callable = new Callable<PreloadedData>() {
      public PreloadedData call() throws Exception {
        return preloadResult;
      }
    };
    return callable;
  }
}
