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

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.apache.shindig.gadgets.rewrite.ResponseRewriterList.RewriteFlow;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Provider;
import com.google.inject.util.Providers;

/**
 * Tests for ContextAwareRegistryTest.
 */
public class ContextAwareRegistryTest extends RewriterTestBase {
  private ContextAwareRegistry contextAwareRegistry;
  public static final String TEST_CONTAINER = "test";
  public static final String DUMMY_CONTAINER = "dummy";

  private class TestRewriter implements ResponseRewriter {
    public final String val;
    public TestRewriter(String val) {
      this.val = val;
    }

    public void rewrite(HttpRequest request, HttpResponseBuilder response, Gadget gadget)
            throws RewritingException {
      response.addHeader("helloo", val);
      response.addHeader("gadget", val);
    }
  }

  void addBindingForRewritePath(String container, RewriteFlow rewriteFlow,
                                Provider<List<ResponseRewriter>> list,
                                Map<RewritePath, Provider<List<ResponseRewriter>>> map) {
    RewritePath rewritePath = new RewritePath(container, rewriteFlow);
    map.put(rewritePath, list);
  }

  @Test
  public void testGetResponseRewriters() throws Exception {
    final Map<RewritePath, Provider<List<ResponseRewriter>>> rewritePathToList = Maps.newHashMap();
    Provider<Map<RewritePath, Provider<List<ResponseRewriter>>>> mapProvider = Providers.of(
        rewritePathToList);

    List<ResponseRewriter> list = ImmutableList.<ResponseRewriter>of(
        new TestRewriter("helo"), new TestRewriter("buffalo"));
    List<ResponseRewriter> emptyList = ImmutableList.of();
    List<ResponseRewriter> list2 = ImmutableList.<ResponseRewriter>of(new TestRewriter(null));

    addBindingForRewritePath(ContainerConfig.DEFAULT_CONTAINER, RewriteFlow.ACCELERATE,
                             Providers.of(list), rewritePathToList);
    addBindingForRewritePath(ContainerConfig.DEFAULT_CONTAINER, RewriteFlow.DEFAULT,
                             Providers.of(emptyList), rewritePathToList);

    list = ImmutableList.<ResponseRewriter>of(new TestRewriter("cat"),
                                               new TestRewriter("dog"));
    addBindingForRewritePath(TEST_CONTAINER, RewriteFlow.ACCELERATE,
                             Providers.of(list), rewritePathToList);
    addBindingForRewritePath(TEST_CONTAINER, RewriteFlow.DEFAULT,
                             Providers.of(list2), rewritePathToList);

    // Test container present and flow present.
    contextAwareRegistry = new ContextAwareRegistry(null, RewriteFlow.ACCELERATE, mapProvider);
    list = contextAwareRegistry.getResponseRewriters(TEST_CONTAINER);
    assertEquals(2, list.size());
    assertEquals("cat", ((TestRewriter) list.get(0)).val);
    assertEquals("dog", ((TestRewriter) list.get(1)).val);

    // Test container present but flow absent.
    contextAwareRegistry = new ContextAwareRegistry(null, RewriteFlow.DUMMY_FLOW, mapProvider);
    list = contextAwareRegistry.getResponseRewriters(TEST_CONTAINER);
    assertEquals(0, list.size());

    // Test container absent, fallback to default container.
    contextAwareRegistry = new ContextAwareRegistry(null, RewriteFlow.ACCELERATE, mapProvider);
    list = contextAwareRegistry.getResponseRewriters(DUMMY_CONTAINER);
    assertEquals(2, list.size());
    assertEquals("helo", ((TestRewriter) list.get(0)).val);
    assertEquals("buffalo", ((TestRewriter) list.get(1)).val);

    // Test container absent, fallback to default container which is also absent.
    rewritePathToList.remove(new RewritePath(ContainerConfig.DEFAULT_CONTAINER,
                                             RewriteFlow.ACCELERATE));
    rewritePathToList.remove(new RewritePath(ContainerConfig.DEFAULT_CONTAINER,
                                             RewriteFlow.DEFAULT));
    contextAwareRegistry = new ContextAwareRegistry(null, RewriteFlow.ACCELERATE, mapProvider);
    list = contextAwareRegistry.getResponseRewriters(DUMMY_CONTAINER);
    assertEquals(0, list.size());
  }

  @Test
  public void testRewriteResponse() throws Exception {
  final Map<RewritePath, Provider<List<ResponseRewriter>>> rewritePathToList = Maps.newHashMap();

    List<ResponseRewriter> list = ImmutableList.<ResponseRewriter>of(
        new TestRewriter("helo"), new TestRewriter("buffalo"));
    List<ResponseRewriter> emptyList = ImmutableList.of();

    addBindingForRewritePath(TEST_CONTAINER, RewriteFlow.ACCELERATE,
                             Providers.of(list), rewritePathToList);
    addBindingForRewritePath(TEST_CONTAINER, RewriteFlow.DEFAULT,
                             Providers.of(emptyList), rewritePathToList);

    // Test container present and flow present.
    contextAwareRegistry = new ContextAwareRegistry(
        null, RewriteFlow.ACCELERATE, Providers.of(rewritePathToList));

    HttpRequest req = new HttpRequest(Uri.parse("http://www.example.org/"));
    req.setContainer(TEST_CONTAINER);
    HttpResponseBuilder builder = new HttpResponseBuilder();
    HttpResponse resp = contextAwareRegistry.rewriteHttpResponse(
        req, builder.create(), null);

    List<String> headers = Lists.newArrayList(resp.getHeaders("helloo"));
    assertEquals(2, headers.size());
    assertEquals("helo", headers.get(0));
    assertEquals("buffalo", headers.get(1));
  }

  @Test
  public void testRewriteResponseGadget() throws Exception {
    Gadget gadget = mockGadget();
    control.replay();
    final Map<RewritePath, Provider<List<ResponseRewriter>>> rewritePathToList = Maps.newHashMap();

    List<ResponseRewriter> list = ImmutableList.<ResponseRewriter>of(
        new TestRewriter("helo"), new TestRewriter("buffalo"));
    List<ResponseRewriter> emptyList = ImmutableList.of();

    addBindingForRewritePath(TEST_CONTAINER, RewriteFlow.ACCELERATE,
                             Providers.of(list), rewritePathToList);
    addBindingForRewritePath(TEST_CONTAINER, RewriteFlow.DEFAULT,
                             Providers.of(emptyList), rewritePathToList);

    // Test container present and flow present.
    contextAwareRegistry = new ContextAwareRegistry(
        null, RewriteFlow.ACCELERATE, Providers.of(rewritePathToList));

    HttpRequest req = new HttpRequest(Uri.parse("http://www.example.org/"));
    req.setContainer(TEST_CONTAINER);
    HttpResponseBuilder builder = new HttpResponseBuilder();
    HttpResponse resp = contextAwareRegistry.rewriteHttpResponse(
        req, builder.create(), gadget);

    List<String> headers = Lists.newArrayList(resp.getHeaders("helloo"));
    assertEquals(2, headers.size());
    assertEquals("helo", headers.get(0));
    assertEquals("buffalo", headers.get(1));

    headers = Lists.newArrayList(resp.getHeaders("gadget"));
    assertEquals(2, headers.size());
    assertEquals("helo", headers.get(0));
    assertEquals("buffalo", headers.get(1));
  }
}
