/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.gadgets.rewrite;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.cache.LruCacheProvider;
import org.apache.shindig.common.cache.CacheProvider;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.apache.shindig.gadgets.http.RequestPipeline;
import org.apache.shindig.gadgets.http.HttpCache;
import org.apache.shindig.gadgets.http.DefaultHttpCache;
import org.apache.shindig.gadgets.parse.ParseModule.DOMImplementationProvider;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Tests for CacheEnforcementVisitor.
 */
public class CacheEnforcementVisitorTest extends DomWalkerTestBase {
  private ExecutorService executor;
  private HttpCache cache;
  protected Document doc;
  private static final Map<String, String> ALL_RESOURCES =
      CacheEnforcementVisitor.Tags.ALL_RESOURCES.getResourceTags();
  private static final String IMG_URL = "http://www.example.org/1.gif";

  @Before
  public void setUp() {
    executor = Executors.newSingleThreadExecutor();
    DOMImplementationProvider domImpl = new DOMImplementationProvider();
    doc = domImpl.get().createDocument(null, null, null);
    CacheProvider cacheProvider = new LruCacheProvider(10);
    cache = new DefaultHttpCache(cacheProvider);
 }

  @Test
  public void testStaleImgWithNegativeTtlReservedAndFetchTriggered() throws Exception {
    cache.addResponse(new HttpRequest(Uri.parse(IMG_URL)),
                      new HttpResponseBuilder().setResponseString("test")
                          .setCacheTtl(-1).create());
    checkVisitBypassedAndFetchTriggered("img", IMG_URL, false, true);
  }

  @Test
  public void testStaleImgWithZeroMaxAgeReservedAndFetchTriggered() throws Exception {
    cache.addResponse(new HttpRequest(Uri.parse(IMG_URL)),
                      new HttpResponseBuilder().setResponseString("test")
                          .addHeader("Cache-Control", "max-age=0").create());
    checkVisitBypassedAndFetchTriggered("img", IMG_URL, false, true);
  }

  @Test
  public void testImgWithErrorResponseReservedAndFetchTriggered() throws Exception {
    cache.addResponse(new HttpRequest(Uri.parse(IMG_URL)),
                      new HttpResponseBuilder().setResponseString("test")
                          .setHttpStatusCode(404).create());
    checkVisitBypassedAndFetchTriggered("img", IMG_URL, false, true);
  }

  @Test
  public void testImgBypassedAndFetchNotTriggered() throws Exception {
    cache.addResponse(new HttpRequest(Uri.parse(IMG_URL)),
                      new HttpResponseBuilder().setResponseString("test").create());
    checkVisitBypassedAndFetchTriggered("img", IMG_URL, true, false);
  }

  @Test
  public void testEmbedImgBypassedAndFetchNotTriggered() throws Exception {
    checkVisitBypassedAndFetchTriggered("embed", IMG_URL, true, false);
    cache.addResponse(new HttpRequest(Uri.parse(IMG_URL)),
                      new HttpResponseBuilder().setResponseString("test").create());
    checkVisitBypassedAndFetchTriggered("embed", IMG_URL, true, false);
  }

  @Test
  public void testImgNotInCacheReservedAndFetchTriggered() throws Exception {
    checkVisitBypassedAndFetchTriggered("img", IMG_URL, false, true);
  }

  /**
   * Checks whether a node with the specified tag and url is bypassed by the
   * CacheAwareResourceMutateVisitor, and also whether a fetch is triggered for
   * the resource.
   *
   * @param tag The name of the tag for the node.
   * @param url The url of the node.
   * @param expectBypass Boolean to check if the node will be bypassed by the
   *     visitor.
   * @param expectFetch Boolean to check if a fetch will be triggered for the
   * resource.
   * @throws Exception
   */
  private void checkVisitBypassedAndFetchTriggered(String tag, String url, boolean expectBypass,
                                                   boolean expectFetch) throws Exception {
    // Try to get the attribute name for the specified tag, or otherwise use src.
    String attrName = ALL_RESOURCES.get(tag.toLowerCase());
    attrName = attrName != null ? attrName : "src";

    // Create a node with the specified tag name and attribute.
    Element node = doc.createElement(tag);
    Attr attr = doc.createAttribute(attrName);
    attr.setValue(url);
    node.setAttributeNode(attr);

    // Mock the RequestPipeline.
    RequestPipeline requestPipeline = createMock(RequestPipeline.class);
    if (expectFetch) {
      expect(requestPipeline.execute(new HttpRequest(Uri.parse(url))))
          .andReturn(new HttpResponseBuilder().setResponseString("test").create());
    }
    replay(requestPipeline);

    ContentRewriterFeature.Config config = createMock(ContentRewriterFeature.Config.class);
    expect(config.shouldRewriteURL(IMG_URL)).andReturn(true).anyTimes();
    expect(config.shouldRewriteTag("img")).andReturn(true).anyTimes();
    replay(config);
    CacheEnforcementVisitor visitor = new CacheEnforcementVisitor(
        config, executor, cache, requestPipeline,
        ProxyingVisitor.Tags.SCRIPT, ProxyingVisitor.Tags.STYLESHEET,
        ProxyingVisitor.Tags.EMBEDDED_IMAGES);

    DomWalker.Visitor.VisitStatus status = visitor.visit(null, node);

    executor.shutdown();
    executor.awaitTermination(5, TimeUnit.SECONDS);
    verify(requestPipeline);
    verify(config);

    assertEquals(expectBypass, status == DomWalker.Visitor.VisitStatus.BYPASS);
  }
}
