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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Provider;
import com.google.inject.util.Providers;

import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.JsCompileMode;
import org.apache.shindig.gadgets.features.ApiDirective;
import org.apache.shindig.gadgets.features.FeatureRegistry;
import org.apache.shindig.gadgets.features.FeatureRegistry.FeatureBundle;
import org.apache.shindig.gadgets.features.FeatureRegistry.LookupResult;
import org.apache.shindig.gadgets.features.FeatureRegistryProvider;
import org.apache.shindig.gadgets.features.FeatureResource;
import org.apache.shindig.gadgets.uri.JsUriManager.JsUri;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class DeferJsProcessorTest {
  private final String DEFER_JS_DEB = "function deferJs() {};";

  private final List<String> EXPORTS_1 = ImmutableList.of(
      "gadgets",
      "gadgets.rpc.call",
      "gadgets.rpc.register",
      "shindig",
      "shindig.random");

  private final List<String> EXPORTS_2 = ImmutableList.of(
      "foo",
      "foo.prototype.bar");

  private final String EXPORT_STRING_1_DEFER =
    "deferJs('gadgets');" +
    "deferJs('shindig');" +
    "deferJs('gadgets.rpc',['call','register']);" +
    "deferJs('shindig',['random']);";

  private final List<String> LIBS_WITH_DEFER = Lists.newArrayList("lib1");
  private final List<String> LIBS_WITHOUT_DEFER = Lists.newArrayList("lib2");
  private final List<String> LOADED = Lists.newArrayList();

  private DeferJsProcessor processor;
  private FeatureRegistry featureRegistry;

  @Before
  public void setUp() throws Exception {
    GadgetContext ctx = new GadgetContext();
    Provider<GadgetContext> contextProviderMock = Providers.of(ctx);
    FeatureResource resource = mockResource(DEFER_JS_DEB);
    FeatureRegistry.FeatureBundle bundle = mockExportJsBundle(resource);
    LookupResult lookupMock = mockLookupResult(bundle);
    final FeatureRegistry featureRegistryMock = mockRegistry(lookupMock);
    featureRegistry = featureRegistryMock;
    FeatureRegistryProvider registryProvider = new FeatureRegistryProvider() {
      public FeatureRegistry get(String repository) {
        return featureRegistryMock;
      }
    };
    processor = new DeferJsProcessor(registryProvider, contextProviderMock);
  }

  @Test
  public void processWithOneNonEmptyFeatureDeferred() throws Exception {
    JsUri jsUri = mockJsUri(JsCompileMode.CONCAT_COMPILE_EXPORT_ALL, true, LIBS_WITH_DEFER);
    JsRequest jsRequest = new JsRequest(jsUri, null, false, featureRegistry);
    JsResponseBuilder jsBuilder = new JsResponseBuilder();
    boolean actualReturnCode = processor.process(jsRequest, jsBuilder);
    assertTrue(actualReturnCode);
    assertEquals(
        DEFER_JS_DEB + EXPORT_STRING_1_DEFER,
        jsBuilder.build().toJsString());
  }

  @Test
  public void processWithOneNonEmptyFeatureDeferredNotSupported() throws Exception {
    JsUri jsUri = mockJsUri(JsCompileMode.CONCAT_COMPILE_EXPORT_ALL, true, LIBS_WITHOUT_DEFER);
    JsRequest jsRequest = new JsRequest(jsUri, null, false, featureRegistry);
    JsResponseBuilder jsBuilder = new JsResponseBuilder();
    boolean actualReturnCode = processor.process(jsRequest, jsBuilder);
    assertTrue(actualReturnCode);
    assertEquals(
        "",
        jsBuilder.build().toJsString());
  }

  @SuppressWarnings("unchecked")
  private FeatureRegistry mockRegistry(LookupResult lookupMock) {
    FeatureRegistry result = createMock(FeatureRegistry.class);
    expect(result.getFeatureResources(
        isA(GadgetContext.class), isA(List.class), EasyMock.isNull(List.class))).
        andReturn(lookupMock).anyTimes();
    expect(result.getFeatureResources(
        isA(GadgetContext.class), eq(LIBS_WITH_DEFER), EasyMock.isNull(List.class), eq(false))).
        andReturn(mockLookupResult(mockBundle(EXPORTS_1, true))).anyTimes();
    expect(result.getFeatureResources(
        isA(GadgetContext.class), eq(LIBS_WITHOUT_DEFER), EasyMock.isNull(List.class), eq(false))).
        andReturn(mockLookupResult(mockBundle(EXPORTS_2, false))).anyTimes();
    expect(result.getFeatures(LIBS_WITHOUT_DEFER)).andReturn(LIBS_WITHOUT_DEFER).anyTimes();
    expect(result.getFeatures(LIBS_WITH_DEFER)).andReturn(LIBS_WITH_DEFER).anyTimes();
    expect(result.getFeatures(LOADED)).andReturn(LOADED).anyTimes();
    replay(result);
    return result;
  }

  private JsUri mockJsUri(JsCompileMode mode, boolean isJsload, List<String> libs) {
    JsUri result = createMock(JsUri.class);
    expect(result.getCompileMode()).andStubReturn(mode);
    expect(result.getRepository()).andStubReturn(null);
    expect(result.isJsload()).andReturn(isJsload).anyTimes();
    expect(result.getLibs()).andReturn(libs).anyTimes();
    expect(result.getLoadedLibs()).andReturn(LOADED).anyTimes();
    replay(result);
    return result;
  }

  private LookupResult mockLookupResult(FeatureRegistry.FeatureBundle featureBundle) {
    LookupResult result = createMock(LookupResult.class);
    expect(result.getBundles()).andReturn(ImmutableList.of(featureBundle)).anyTimes();
    replay(result);
    return result;
  }

  private FeatureResource mockResource(String debContent) {
    FeatureResource result = createMock(FeatureResource.class);
    expect(result.getDebugContent()).andReturn(debContent).anyTimes();
    expect(result.getName()).andReturn("js").anyTimes();
    replay(result);
    return result;
  }

  private FeatureBundle mockBundle(List<String> exports, boolean isDefer) {
    List<ApiDirective> apis = Lists.newArrayList();
    for (String e : exports) apis.add(mockApiDirective(true, e));
    FeatureBundle result = createMock(FeatureBundle.class);
    expect(result.getApis(ApiDirective.Type.JS, true)).andReturn(exports).anyTimes();
    expect(result.isSupportDefer()).andReturn(isDefer).anyTimes();
    replay(result);
    return result;
  }

  private FeatureBundle mockExportJsBundle(FeatureResource featureResourceMock) {
    FeatureRegistry.FeatureBundle featureBundle = createMock(FeatureBundle.class);
    expect(featureBundle.getResources()).andReturn(
        ImmutableList.of(featureResourceMock)).anyTimes();
    replay(featureBundle);
    return featureBundle;
  }

  private ApiDirective mockApiDirective(boolean isExports, String value) {
    ApiDirective result = createMock(ApiDirective.class);
    expect(result.getType()).andReturn(ApiDirective.Type.JS).anyTimes();
    expect(result.getValue()).andReturn(value).anyTimes();
    expect(result.isExports()).andReturn(isExports).anyTimes();
    replay(result);
    return result;
  }
}
