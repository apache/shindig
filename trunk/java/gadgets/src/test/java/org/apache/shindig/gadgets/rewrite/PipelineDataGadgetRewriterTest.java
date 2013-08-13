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
package org.apache.shindig.gadgets.rewrite;

import org.apache.shindig.common.JsonAssert;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.expressions.Expressions;
import org.apache.shindig.expressions.RootELResolver;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.parse.ParseModule;
import org.apache.shindig.gadgets.parse.nekohtml.NekoSimplifiedHtmlParser;
import org.apache.shindig.gadgets.preload.ConcurrentPreloaderService;
import org.apache.shindig.gadgets.preload.PipelineExecutor;
import org.apache.shindig.gadgets.preload.PipelinedDataPreloader;
import org.apache.shindig.gadgets.preload.PreloadException;
import org.apache.shindig.gadgets.preload.PreloadedData;
import org.apache.shindig.gadgets.preload.PreloaderService;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.PipelinedData;
import org.apache.shindig.gadgets.spec.SpecParserException;

import com.google.common.collect.ImmutableList;
import org.easymock.Capture;
import static org.easymock.EasyMock.and;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.reportMatcher;
import static org.easymock.EasyMock.same;
import org.easymock.IArgumentMatcher;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.json.JSONException;
import org.json.JSONObject;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

/**
 * Test of PipelineDataContentRewriter.
 */
public class PipelineDataGadgetRewriterTest {

  private IMocksControl control;
  private PipelinedDataPreloader preloader;
  private PreloaderService preloaderService;
  private PipelineDataGadgetRewriter rewriter;
  private GadgetSpec gadgetSpec;
  private Gadget gadget;
  private MutableContent content;
  private static final Uri GADGET_URI = Uri.parse("http://example.org/gadget.php");

  private static final String CONTENT =
    "<script xmlns:os=\"http://ns.opensocial.org/2008/markup\" type=\"text/os-data\">"
      + "  <os:PeopleRequest key=\"me\" userId=\"canonical\"/>"
      + "  <os:HttpRequest key=\"json\" href=\"test.json\"/>"
      + "</script>";

  // One request, but it requires data that isn\"t present
  private static final String BLOCKED_FIRST_BATCH_CONTENT =
    "<script xmlns:os=\"http://ns.opensocial.org/2008/markup\" type=\"text/os-data\">"
    + "  <os:PeopleRequest key=\"me\" userId=\"${json.user}\"/>"
    + "</script>";

  private static final String XML_WITHOUT_FEATURE = "<Module>" + "<ModulePrefs title=\"Title\">"
      + "</ModulePrefs>" + "<Content>" + "    <![CDATA[" + CONTENT + "]]></Content></Module>";

  private static final String XML_WITHOUT_PIPELINE = "<Module>" + "<ModulePrefs title=\"Title\">"
      + "<Require feature=\"opensocial-data\"/>" + "</ModulePrefs>" + "<Content/></Module>";

  @Before
  public void setUp() throws Exception {
    control = EasyMock.createStrictControl();
    preloader = control.createMock(PipelinedDataPreloader.class);
    preloaderService = new ConcurrentPreloaderService(Executors.newSingleThreadExecutor(), null);
    rewriter = new PipelineDataGadgetRewriter(new PipelineExecutor(preloader, preloaderService,
        Expressions.forTesting()));
  }

  private void setupGadget(String gadgetXml) throws SpecParserException {
    gadgetSpec = new GadgetSpec(GADGET_URI, gadgetXml);
    gadget = new Gadget();
    gadget.setSpec(gadgetSpec);
    gadget.setContext(new GadgetContext() {});
    gadget.setCurrentView(gadgetSpec.getView("default"));

    content = new MutableContent(new NekoSimplifiedHtmlParser(
        new ParseModule.DOMImplementationProvider().get()), gadget.getCurrentView().getContent());
  }

  @Test
  public void rewrite() throws Exception {
    setupGadget(getGadgetXml(CONTENT));

    Capture<PipelinedData.Batch> batchCapture =
      new Capture<PipelinedData.Batch>();

    // Dummy return results (the "real" return would have two values)
    Callable<PreloadedData> callable = createPreloadTask(
        "key", "{result: {foo: 'bar'}}");

    // One batch with 1 each HTTP and Social preload
    expect(preloader.createPreloadTasks(same(gadget.getContext()),
            and(eqBatch(1, 1), capture(batchCapture))))
            .andReturn(ImmutableList.of(callable));

    control.replay();

    rewriter.rewrite(gadget, content);

    // Verify the data set is injected, and the os-data was deleted
    assertTrue("Script not inserted", content.getContent().contains("DataContext.putDataSet(\"key\",{\"foo\":\"bar\"})"));
    assertFalse("os-data wasn't deleted",
        content.getContent().contains("type=\"text/os-data\""));

    assertTrue(batchCapture.getValue().getPreloads().containsKey("me"));
    assertTrue(batchCapture.getValue().getPreloads().containsKey("json"));

    assertFalse(gadget.getDirectFeatureDeps().contains("opensocial-data"));
    assertTrue(gadget.getDirectFeatureDeps().contains("opensocial-data-context"));

    control.verify();
  }
  @Test
  public void rewriteWithBlockedBatch() throws Exception {
    setupGadget(getGadgetXml(BLOCKED_FIRST_BATCH_CONTENT));

    // Expect a batch with no content
    expect(
        preloader.createPreloadTasks(same(gadget.getContext()), eqBatch(0, 0)))
            .andReturn(ImmutableList.<Callable<PreloadedData>>of());

    control.replay();

    rewriter.rewrite(gadget, content);

    control.verify();

    // Check there is no DataContext inserted
    assertFalse("DataContext write shouldn't be present", content.getContent().indexOf(
        "DataContext.putDataSet(") > 0);
    // And the os-data elements should be present
    assertTrue("os-data was deleted",
        content.getContent().indexOf("type=\"text/os-data\"") > 0);
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
      buffer.append("eqBuffer[social=" + socialCount + ",http=" + httpCount + ']');
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

  @Test
  public void rewriteWithoutPipeline() throws Exception {
    setupGadget(XML_WITHOUT_PIPELINE);
    control.replay();

    // If there are no pipeline elements, the rewrite is a no-op
    rewriter.rewrite(gadget, content);

    control.verify();
  }

  @Test
  public void rewriteWithoutFeature() throws Exception {
    // If the opensocial-data feature is present, the rewrite is a no-op
    setupGadget(XML_WITHOUT_FEATURE);

    control.replay();

    rewriter.rewrite(gadget, content);

    control.verify();
  }

  @Test
  /** Test that os:DataRequest is parsed correctly */
  public void parseOfDataRequest() throws Exception {
    final String contentWithDataRequest =
      "<script xmlns:os=\"http://ns.opensocial.org/2008/markup\" type=\"text/os-data\">"
        + "  <os:DataRequest key=\"me\" method=\"people.get\" userId=\"canonical\"/>"
        + "</script>";

    setupGadget(getGadgetXml(contentWithDataRequest));
    Map<PipelinedData, ? extends Object> pipelines =
        rewriter.parsePipelinedData(gadget, content.getDocument());
    assertEquals(1, pipelines.size());
    PipelinedData pipeline = pipelines.keySet().iterator().next();
    PipelinedData.Batch batch = pipeline.getBatch(Expressions.forTesting(), new RootELResolver());
    Map<String, PipelinedData.BatchItem> preloads = batch.getPreloads();
    assertTrue(preloads.containsKey("me"));
    assertEquals(PipelinedData.BatchType.SOCIAL, preloads.get("me").getType());

    JsonAssert.assertObjectEquals(
        "{params: {userId: 'canonical'}, method: 'people.get', id: 'me'}",
        preloads.get("me").getData());
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

  private static String getGadgetXml(String content) {
    return "<Module>" + "<ModulePrefs title='Title'>"
        + "<Require feature='opensocial-data'/>" + "</ModulePrefs>"
        + "<Content>"
        + "    <![CDATA[" + content + "]]>"
        + "</Content></Module>";
  }
}
