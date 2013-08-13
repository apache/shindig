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
package org.apache.shindig.gadgets.js;

import static org.easymock.EasyMock.createControl;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.isNull;
import static org.junit.Assert.*;

import java.util.List;

import org.apache.shindig.gadgets.RenderingContext;
import org.apache.shindig.gadgets.features.DefaultFeatureRegistryProvider;
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
import org.apache.shindig.gadgets.features.ApiDirective;

/**
 * Tests for {@link GetJsContentProcessor}.
 */
public class GetJsContentProcessorTest {
  private static final String JS_CODE1 = "js1";
  private static final String JS_CODE2 = "js2";

  private static final List<String> EMPTY_STRING_LIST = ImmutableList.<String>of();
  private static final List<FeatureBundle> EMPTY_BUNDLE_LIST = ImmutableList.<FeatureBundle>of();

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
    expect(jsUri.getRepository()).andStubReturn(null);
    request = control.createMock(JsRequest.class);
    response = new JsResponseBuilder();
    processor = new GetJsContentProcessor(new DefaultFeatureRegistryProvider(registry), compiler);
    processor.setVersionedMaxAge(GetJsContentProcessor.DEFAULT_VERSIONED_MAXAGE);
    processor.setUnversionedMaxAge(GetJsContentProcessor.DEFAULT_UNVERSIONED_MAXAGE);
    processor.setInvalidMaxAge(GetJsContentProcessor.DEFAULT_INVALID_MAXAGE);
  }

  @Test
  public void testPopulatesResponseForUnversionedRequest() throws Exception {
    setupForVersionAndProxy(true, UriStatus.VALID_UNVERSIONED);
    control.replay();
    processor.process(request, response);
    checkResponse(true, GetJsContentProcessor.DEFAULT_UNVERSIONED_MAXAGE, JS_CODE1 + JS_CODE2, "");
    control.verify();
  }

  @Test
  public void testPopulatesResponseForVersionedRequest() throws Exception {
    setupForVersionAndProxy(true, UriStatus.VALID_VERSIONED);
    control.replay();
    processor.process(request, response);
    checkResponse(true, GetJsContentProcessor.DEFAULT_VERSIONED_MAXAGE, JS_CODE1 + JS_CODE2, "");
    control.verify();
  }

  @Test
  public void testPopulatesResponseForInvalidVersion() throws Exception {
    setupForVersionAndProxy(true, UriStatus.INVALID_VERSION);
    control.replay();
    processor.process(request, response);
    checkResponse(true, GetJsContentProcessor.DEFAULT_INVALID_MAXAGE, JS_CODE1 + JS_CODE2, "");
    control.verify();
  }

  @Test
  public void testPopulatesResponseForNoProxyCacheable() throws Exception {
    setupForVersionAndProxy(false, UriStatus.VALID_UNVERSIONED);
    control.replay();
    processor.process(request, response);
    checkResponse(false, GetJsContentProcessor.DEFAULT_UNVERSIONED_MAXAGE, JS_CODE1 + JS_CODE2, "");
    control.verify();
  }

  @Test
  public void testPopulateWithLoadedFeatures() throws Exception {
    List<String> reqLibs = ImmutableList.of("feature1", "feature2");
    List<String> loadLibs = ImmutableList.of("feature2");

    FeatureResource resource1 = mockResource(true);
    FeatureBundle bundle1 = mockBundle("feature1", null, null, Lists.newArrayList(resource1));
    FeatureBundle bundle2 = mockBundle("feature2", "export2", "extern2", null);

    setupJsUriAndRegistry(UriStatus.VALID_UNVERSIONED,
        reqLibs, ImmutableList.of(bundle1, bundle2),
        loadLibs, ImmutableList.of(bundle2));

    expect(compiler.getJsContent(jsUri, bundle1))
      .andReturn(ImmutableList.<JsContent>of(
          JsContent.fromFeature(JS_CODE1, "source1", null, null)));

    control.replay();
    processor.process(request, response);
    checkResponse(true, GetJsContentProcessor.DEFAULT_UNVERSIONED_MAXAGE, JS_CODE1, "export2", "extern2");
    control.verify();
  }

  private void setupForVersionAndProxy(boolean proxyCacheable, UriStatus uriStatus) {
    List<String> reqLibs = ImmutableList.of("feature");
    List<String> loadLibs = EMPTY_STRING_LIST;

    FeatureResource resource1 = mockResource(proxyCacheable);
    FeatureResource resource2 = mockResource(proxyCacheable);
    FeatureBundle bundle = mockBundle("feature", null, null,
        Lists.newArrayList(resource1, resource2));

    setupJsUriAndRegistry(uriStatus,
        reqLibs, Lists.newArrayList(bundle),
        loadLibs, EMPTY_BUNDLE_LIST);

    expect(compiler.getJsContent(jsUri, bundle))
        .andReturn(ImmutableList.<JsContent>of(
            JsContent.fromFeature(JS_CODE1, "source1", bundle, resource1),
            JsContent.fromFeature(JS_CODE2, "source2", bundle, resource2)));
  }

  @SuppressWarnings("unchecked")
  private void setupJsUriAndRegistry(UriStatus uriStatus,
      List<String> reqLibs, List<FeatureBundle> reqLookupBundles,
      List<String> loadLibs, List<FeatureBundle> loadLookupBundles) {

    expect(jsUri.getStatus()).andReturn(uriStatus);
    expect(jsUri.getContainer()).andReturn("container");
    expect(jsUri.getContext()).andReturn(RenderingContext.CONFIGURED_GADGET);
    expect(jsUri.isDebug()).andReturn(false);
    expect(jsUri.getLibs()).andReturn(reqLibs);
    expect(jsUri.getLoadedLibs()).andReturn(loadLibs);

    expect(request.getJsUri()).andReturn(jsUri);

    LookupResult reqLookup = mockLookupResult(reqLookupBundles);
    LookupResult loadLookup = mockLookupResult(loadLookupBundles);

    expect(registry.getFeatureResources(isA(JsGadgetContext.class), eq(reqLibs),
        isNull(List.class))).andReturn(reqLookup);
    expect(registry.getFeatureResources(isA(JsGadgetContext.class), eq(loadLibs),
        isNull(List.class))).andReturn(loadLookup);
  }

  private LookupResult mockLookupResult(List<FeatureBundle> bundles) {
    LookupResult result = control.createMock(LookupResult.class);
    expect(result.getBundles()).andReturn(bundles);
    return result;
  }

  private FeatureResource mockResource(boolean proxyCacheable) {
    FeatureResource result = control.createMock(FeatureResource.class);
    expect(result.isProxyCacheable()).andReturn(proxyCacheable).anyTimes();
    return result;
  }

  private FeatureBundle mockBundle(String name, String export, String extern, List<FeatureResource> resources) {
    FeatureBundle result = control.createMock(FeatureBundle.class);
    expect(result.getName()).andReturn(name).anyTimes();
    if (export != null) {
      expect(result.getApis(ApiDirective.Type.JS, true)).andReturn(ImmutableList.of(export));
    }
    if (extern != null) {
      expect(result.getApis(ApiDirective.Type.JS, false)).andReturn(ImmutableList.of(extern));
    }
    if (resources != null) {
      expect(result.getResources()).andReturn(resources);
    }
    return result;
  }

  private void checkResponse(boolean proxyCacheable, int expectedTtl,
      String jsString, String... externs) {
    assertEquals(proxyCacheable, response.isProxyCacheable());
    assertEquals(expectedTtl, response.getCacheTtlSecs());
    assertEquals(jsString, response.build().toJsString());
    for (String extern : externs) {
      assertTrue(response.build().getExterns().contains(extern));
    }
  }
}
