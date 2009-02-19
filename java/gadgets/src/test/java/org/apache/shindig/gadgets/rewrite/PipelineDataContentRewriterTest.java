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

import static org.easymock.EasyMock.and;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.reportMatcher;
import static org.easymock.EasyMock.same;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.parse.ParseModule;
import org.apache.shindig.gadgets.parse.nekohtml.SocialMarkupHtmlParser;
import org.apache.shindig.gadgets.preload.ConcurrentPreloaderService;
import org.apache.shindig.gadgets.preload.PipelinedDataPreloader;
import org.apache.shindig.gadgets.preload.PreloadException;
import org.apache.shindig.gadgets.preload.PreloadedData;
import org.apache.shindig.gadgets.preload.PreloaderService;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.PipelinedData;
import org.apache.shindig.gadgets.spec.RequestAuthenticationInfo;
import org.apache.shindig.gadgets.spec.SpecParserException;
import org.easymock.Capture;
import org.easymock.IArgumentMatcher;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import com.google.common.collect.ImmutableList;

/**
 * Test of PipelineDataContentRewriter.
 */
public class PipelineDataContentRewriterTest {

  private IMocksControl control;
  private PipelinedDataPreloader preloader;
  private PreloaderService preloaderService;
  private PipelineDataContentRewriter rewriter;
  private GadgetSpec gadgetSpec;
  private Gadget gadget;
  private MutableContent content;
  private static final Uri GADGET_URI = Uri.parse("http://example.org/gadget.php");

  private static final String CONTENT =
    "<script xmlns:os='http://ns.opensocial.org/2008/markup' type='text/os-data'>"
      + "  <os:PeopleRequest key='me' userId='canonical'/>"
      + "  <os:HttpRequest key='json' href='test.json'/>"
      + "</script>";

  // Two requests, one depends on the other
  private static final String TWO_BATCH_CONTENT =
    "<script xmlns:os='http://ns.opensocial.org/2008/markup' type='text/os-data'>"
    + "  <os:PeopleRequest key='me' userId='${json.user}'/>"
    + "  <os:HttpRequest key='json' href='${ViewParams.file}'/>"
    + "</script>";

  // One request, but it requires data that isn't present
  private static final String BLOCKED_FIRST_BATCH_CONTENT =
    "<script xmlns:os='http://ns.opensocial.org/2008/markup' type='text/os-data'>"
    + "  <os:PeopleRequest key='me' userId='${json.user}'/>"
    + "</script>";

  private static final String XML_WITHOUT_FEATURE = "<Module>" + "<ModulePrefs title='Title'>"
      + "</ModulePrefs>" + "<Content>" + "    <![CDATA[" + CONTENT + "]]></Content></Module>";

  private static final String XML_WITHOUT_PIPELINE = "<Module>" + "<ModulePrefs title='Title'>"
      + "<Require feature='opensocial-data'/>" + "</ModulePrefs>" + "<Content/></Module>";

  @Before
  public void setUp() throws Exception {
    control = EasyMock.createStrictControl();
    preloader = control.createMock(PipelinedDataPreloader.class);
//    preloaderService = control.createMock(PreloaderService.class);
    preloaderService = new ConcurrentPreloaderService(Executors.newSingleThreadExecutor(), null);
    rewriter = new PipelineDataContentRewriter(preloader, preloaderService);
  }

  private void setupGadget(String gadgetXml) throws SpecParserException {
    gadgetSpec = new GadgetSpec(GADGET_URI, gadgetXml);
    gadget = new Gadget();
    gadget.setSpec(gadgetSpec);
    gadget.setContext(new GadgetContext() {});
    gadget.setCurrentView(gadgetSpec.getView("default"));

    content = new MutableContent(new SocialMarkupHtmlParser(
        new ParseModule.DOMImplementationProvider().get()), gadget.getCurrentView().getContent());
  }

  @Test
  public void rewrite() throws Exception {
    setupGadget(getGadgetXml(CONTENT));

    Capture<PipelinedData.Batch> batchCapture =
      new Capture<PipelinedData.Batch>();
    
    // Dummy return results (the "real" return would have two values)
    Callable<PreloadedData> callable = createPreloadTask(
        "key", "{data: {foo: 'bar'}}");

    // One batch with 1 each HTTP and Social preload
    expect(preloader.createPreloadTasks(same(gadget.getContext()),
            and(eqBatch(1, 1), capture(batchCapture))))
            .andReturn(ImmutableList.of(callable));

    control.replay();

    rewriter.rewrite(gadget, content);

    // Verify the data set is injected, and the os-data was deleted
    assertTrue("Script not inserted", content.getContent().indexOf(
        "DataContext.putDataSet(\"key\",{\"foo\":\"bar\"})") >= 0);
    assertFalse("os-data wasn't deleted",
        content.getContent().indexOf("type=\"text/os-data\"") >= 0);

    assertTrue(batchCapture.getValue().getSocialPreloads().containsKey("me"));
    assertTrue(batchCapture.getValue().getHttpPreloads().containsKey("json"));

    control.verify();
  }

  @Test
  public void rewriteWithTwoBatches() throws Exception {
    setupGadget(getGadgetXml(TWO_BATCH_CONTENT));

    gadget.setContext(new GadgetContext() {
      @Override
      public String getParameter(String property) {
        // Provide the filename to be requested in the first batch
        if ("view-params".equals(property)) {
          return "{'file': 'test.json'}";
        }
        return null;
      }
    });

    // First batch, the HTTP fetch
    Capture<PipelinedData.Batch> firstBatch =
      new Capture<PipelinedData.Batch>();
    Callable<PreloadedData> firstTask = createPreloadTask("json",
        "{data: {user: 'canonical'}}");
    
    // Second batch, the user fetch
    Capture<PipelinedData.Batch> secondBatch =
      new Capture<PipelinedData.Batch>();
    Callable<PreloadedData> secondTask = createPreloadTask("me",
        "{data: {'id':'canonical'}}");
    
    // First, a batch with an HTTP request
    expect(
        preloader.createPreloadTasks(same(gadget.getContext()),
            and(eqBatch(0, 1), capture(firstBatch))))
            .andReturn(ImmutableList.of(firstTask));
    // Second, a batch with a social request
    expect(
        preloader.createPreloadTasks(same(gadget.getContext()),
            and(eqBatch(1, 0), capture(secondBatch))))
            .andReturn(ImmutableList.of(secondTask));

    control.replay();

    rewriter.rewrite(gadget, content);
    
    control.verify();

    // Verify the data set is injected, and the os-data was deleted
    assertTrue("First batch not inserted", content.getContent().indexOf(
        "DataContext.putDataSet(\"json\",{\"user\":\"canonical\"})") >= 0);
    assertTrue("Second batch not inserted", content.getContent().indexOf(
        "DataContext.putDataSet(\"me\",{\"id\":\"canonical\"})") >= 0);
    assertFalse("os-data wasn't deleted",
        content.getContent().indexOf("type=\"text/os-data\"") >= 0);

    // Check the evaluated HTTP request
    RequestAuthenticationInfo request = firstBatch.getValue().getHttpPreloads().get("json");
    assertEquals("http://example.org/test.json", request.getHref().toString());
    
    // Check the evaluated person request
    JSONObject personRequest = (JSONObject) secondBatch.getValue().getSocialPreloads().get("me");
    assertEquals("canonical", personRequest.getJSONObject("params").getJSONArray("userId").get(0));
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
      buffer.append("eqBuffer[social=" + socialCount + ",http=" + httpCount + "]");
    }

    public boolean matches(Object obj) {
      if (!(obj instanceof PipelinedData.Batch)) {
        return false;
      }
      
      PipelinedData.Batch batch = (PipelinedData.Batch) obj;
      return (socialCount == batch.getSocialPreloads().size() 
          && httpCount == batch.getHttpPreloads().size());
    }
    
  }
  
  @Test
  public void rewriteWithoutPipeline() throws Exception {
    setupGadget(XML_WITHOUT_PIPELINE);
    control.replay();

    // If there are no pipeline elements, the rewrite is a no-op
    assertNull(rewriter.rewrite(gadget, content));

    control.verify();
  }

  @Test
  public void rewriteWithoutFeature() throws Exception {
    // If the opensocial-data feature is present, the rewrite is a no-op
    setupGadget(XML_WITHOUT_FEATURE);

    control.replay();

    assertNull(rewriter.rewrite(gadget, content));

    control.verify();
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
