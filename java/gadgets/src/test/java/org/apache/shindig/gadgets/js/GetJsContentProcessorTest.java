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

package org.apache.shindig.gadgets.js;

import static org.easymock.EasyMock.createControl;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.*;

import java.util.List;

import org.apache.shindig.gadgets.RenderingContext;
import org.apache.shindig.gadgets.features.FeatureRegistry;
import org.apache.shindig.gadgets.features.FeatureResource;
import org.apache.shindig.gadgets.features.FeatureRegistry.FeatureBundle;
import org.apache.shindig.gadgets.features.FeatureRegistry.LookupResult;
import org.apache.shindig.gadgets.rewrite.js.JsCompiler;
import org.apache.shindig.gadgets.uri.JsUriManager.JsUri;
import org.apache.shindig.gadgets.uri.UriStatus;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;


/**
 * Tests for {@link GetJsContentProcessor}.
 */
public class GetJsContentProcessorTest {
  private static final String JS_CODE1 = "some JS data";
  private static final String JS_CODE2 = "some JS data";

  private IMocksControl control;
  private FeatureRegistry registry;
  private JsCompiler compiler;
  private JsUri jsUri;
  private JsRequest request;
  private JsResponseBuilder response;
  private GetJsContentProcessor processor;

  @Before
  public void setUp() {
    control = createControl();
    registry = control.createMock(FeatureRegistry.class);
    compiler = control.createMock(JsCompiler.class);
    jsUri = control.createMock(JsUri.class);
    request = control.createMock(JsRequest.class);
    response = new JsResponseBuilder();
    processor = new GetJsContentProcessor(registry, compiler);
  }

  @Test
  public void testPopulatesResponseForUnversionedRequest() throws Exception {
    setExpectations(true, UriStatus.VALID_UNVERSIONED);
    control.replay();
    processor.process(request, response);
    checkResponse(true, 3600);
    control.verify();
  }

  @Test
  public void testPopulatesResponseForVersionedRequest() throws Exception {
    setExpectations(true, UriStatus.VALID_VERSIONED);
    control.replay();
    processor.process(request, response);
    checkResponse(true, -1);
    control.verify();
  }

  @Test
  public void testPopulatesResponseForInvalidVersion() throws Exception {
    setExpectations(true, UriStatus.INVALID_VERSION);
    control.replay();
    processor.process(request, response);
    checkResponse(true, 0);
    control.verify();
  }

  @Test
  public void testPopulatesResponseForNoProxyCacheable() throws Exception {
    setExpectations(false, UriStatus.VALID_UNVERSIONED);
    control.replay();
    processor.process(request, response);
    checkResponse(false, 3600);
    control.verify();
  }

  private void setExpectations(boolean proxyCacheable, UriStatus uriStatus) throws JsException {
    expect(jsUri.getStatus()).andReturn(uriStatus);
    List<String> libs = ImmutableList.of("feature1", "feature2");
    expect(jsUri.getLibs()).andReturn(libs);
    expect(jsUri.getContainer()).andReturn("container");
    expect(jsUri.getContext()).andReturn(RenderingContext.CONFIGURED_GADGET);
    expect(jsUri.isDebug()).andReturn(false);
    expect(jsUri.getLoadedLibs()).andReturn(ImmutableList.<String>of());
    expect(request.getJsUri()).andReturn(jsUri);

    List<FeatureBundle> bundles = mockBundles(proxyCacheable);
    LookupResult lr = control.createMock(LookupResult.class);
    expect(lr.getBundles()).andReturn(bundles);

    expect(registry.getFeatureResources(isA(JsGadgetContext.class), eq(libs),
        eq(ImmutableList.<String>of()))).andReturn(lr);
    expect(compiler.getJsContent(jsUri, bundles.get(0)))
        .andReturn(ImmutableList.<JsContent>of(
            JsContent.fromFeature(JS_CODE1, "source1", bundles.get(0), null)));
    expect(compiler.getJsContent(jsUri, bundles.get(1)))
        .andReturn(ImmutableList.<JsContent>of(
            JsContent.fromFeature(JS_CODE2, "source2", bundles.get(1), null)));
  }

  private List<FeatureBundle> mockBundles(boolean proxyCacheable) {
    FeatureBundle bundle1 = control.createMock(FeatureBundle.class);
    expect(bundle1.getName()).andReturn("feature1");
    FeatureResource resource1 = control.createMock(FeatureResource.class);
    expect(resource1.isProxyCacheable()).andReturn(proxyCacheable);
    List<FeatureResource> resources1 = Lists.newArrayList(resource1);
    expect(bundle1.getResources()).andReturn(resources1);

    FeatureBundle bundle2 = control.createMock(FeatureBundle.class);
    expect(bundle2.getName()).andReturn("feature2");
    FeatureResource resource2 = control.createMock(FeatureResource.class);
    if (proxyCacheable) {
      // Only consulted if the first bundle/resource is proxyCacheable.
      expect(resource2.isProxyCacheable()).andReturn(proxyCacheable);
    }
    List<FeatureResource> resources2 = Lists.newArrayList(resource2);
    expect(bundle2.getResources()).andReturn(resources2);

    return Lists.newArrayList(bundle1, bundle2);
  }

  private void checkResponse(boolean proxyCacheable, int expectedTtl) {
    assertEquals(proxyCacheable, response.isProxyCacheable());
    assertEquals(expectedTtl, response.getCacheTtlSecs());
    assertEquals(JS_CODE1 + JS_CODE2, response.build().toJsString());
  }
}
