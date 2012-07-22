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

public class ExportJsProcessorTest {
  private final String EXPORT_JS_DEB = "function exportJs() { };";
  private final String TEXT_CONTENT_1 = "text1;";
  private final String TEXT_CONTENT_2 = "text2;";
  private final String FEATURE_CONTENT_1 = "feature1;";
  private final String FEATURE_CONTENT_2 = "feature2;";
  private final String FEATURE_CONTENT_3 = "feature3;";

  private final List<String> EXPORTS_1 = ImmutableList.of(
      "gadgets",
      "gadgets.rpc.call",
      "gadgets.rpc.register",
      "shindig",
      "shindig.random");

  private final List<String> EXPORTS_2 = ImmutableList.of(
      "foo",
      "foo.prototype.bar");

  private final List<String> EXPORTS_3 = ImmutableList.<String>of();

  private final String EXPORT_STRING_1 =
      "exportJs('gadgets',[gadgets]);" +
      "exportJs('shindig',[shindig]);" +
      "exportJs('gadgets.rpc',[gadgets,gadgets.rpc],{call:'call',register:'register'});" +
      "exportJs('shindig',[shindig],{random:'random'});";

  private final String EXPORT_STRING_2 =
      "exportJs('foo',[foo]);" +
      "exportJs('foo.prototype',[foo,foo.prototype],{bar:'bar'});";

  private final String EXPORT_STRING_3 = "";

  private JsContent textJsContent1;
  private JsContent textJsContent2;
  private JsContent featureJsContent1;
  private JsContent featureJsContent2;
  private JsContent featureJsContent3;
  private ExportJsProcessor processor;

  @Before
  public void setUp() throws Exception {
    GadgetContext ctx = new GadgetContext();
    Provider<GadgetContext> contextProviderMock = Providers.of(ctx);
    FeatureResource resource = mockResource(EXPORT_JS_DEB);
    FeatureRegistry.FeatureBundle bundle = mockExportJsBundle(resource);
    LookupResult lookupMock = mockLookupResult(bundle);
    final FeatureRegistry featureRegistryMock = mockRegistry(lookupMock);
    FeatureRegistryProvider registryProvider = new FeatureRegistryProvider() {
      public FeatureRegistry get(String repository) {
        return featureRegistryMock;
      }
    };

    textJsContent1 = JsContent.fromText(TEXT_CONTENT_1, "source1");
    textJsContent2 = JsContent.fromText(TEXT_CONTENT_2, "source2");
    featureJsContent1 = JsContent.fromFeature(FEATURE_CONTENT_1, "source3", mockBundle(EXPORTS_1), null);
    featureJsContent2 = JsContent.fromFeature(FEATURE_CONTENT_2, "source4", mockBundle(EXPORTS_2), null);
    featureJsContent3 = JsContent.fromFeature(FEATURE_CONTENT_3, "source5", mockBundle(EXPORTS_3), null);
    processor = new ExportJsProcessor(registryProvider, contextProviderMock);
  }

  @SuppressWarnings("unchecked")
  private FeatureRegistry mockRegistry(LookupResult lookupMock) {
    FeatureRegistry result = createMock(FeatureRegistry.class);
    expect(result.getFeatureResources(
        isA(GadgetContext.class), isA(List.class), EasyMock.isNull(List.class))).
        andReturn(lookupMock).anyTimes();
    replay(result);
    return result;
  }

  private JsUri mockJsUri(JsCompileMode mode) {
    return mockJsUri(mode, false);
  }

  private JsUri mockJsUri(JsCompileMode mode, boolean isJsload) {
    JsUri result = createMock(JsUri.class);
    expect(result.getCompileMode()).andStubReturn(mode);
    expect(result.getRepository()).andStubReturn(null);
    expect(result.isJsload()).andReturn(isJsload).anyTimes();
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

  private FeatureBundle mockBundle(List<String> exports) {
    List<ApiDirective> apis = Lists.newArrayList();
    for (String e : exports) apis.add(mockApiDirective(true, e));
    FeatureBundle result = createMock(FeatureBundle.class);
    expect(result.getApis(ApiDirective.Type.JS, true)).andReturn(exports).anyTimes();
    expect(result.isSupportDefer()).andReturn(false).anyTimes();
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

  @Test
  public void processEmpty() throws Exception {
    JsUri jsUri = mockJsUri(JsCompileMode.CONCAT_COMPILE_EXPORT_ALL);
    JsRequest jsRequest = new JsRequest(jsUri, null, false, null);
    JsResponseBuilder jsBuilder = new JsResponseBuilder();
    boolean actualReturnCode = processor.process(jsRequest, jsBuilder);
    assertTrue(actualReturnCode);
    assertEquals("", jsBuilder.build().toJsString());
  }

  @Test
  public void processWithOneText() throws Exception {
    JsUri jsUri = mockJsUri(JsCompileMode.CONCAT_COMPILE_EXPORT_ALL);
    JsRequest jsRequest = new JsRequest(jsUri, null, false, null);
    JsResponseBuilder jsBuilder = new JsResponseBuilder();
    jsBuilder.appendJs(textJsContent1);
    boolean actualReturnCode = processor.process(jsRequest, jsBuilder);
    assertTrue(actualReturnCode);
    assertEquals(
        TEXT_CONTENT_1,
        jsBuilder.build().toJsString());
  }

  @Test
  public void processWithOneNonEmptyFeature() throws Exception {
    JsUri jsUri = mockJsUri(JsCompileMode.CONCAT_COMPILE_EXPORT_ALL);
    JsRequest jsRequest = new JsRequest(jsUri, null, false, null);
    JsResponseBuilder jsBuilder = new JsResponseBuilder();
    jsBuilder.appendJs(featureJsContent1);
    boolean actualReturnCode = processor.process(jsRequest, jsBuilder);
    assertTrue(actualReturnCode);
    assertEquals(
        EXPORT_JS_DEB + FEATURE_CONTENT_1 + EXPORT_STRING_1,
        jsBuilder.build().toJsString());
  }

  @Test
  public void processWithOneEmptyFeature() throws Exception {
    JsUri jsUri = mockJsUri(JsCompileMode.CONCAT_COMPILE_EXPORT_ALL);
    JsRequest jsRequest = new JsRequest(jsUri, null, false, null);
    JsResponseBuilder jsBuilder = new JsResponseBuilder();
    jsBuilder.appendJs(featureJsContent3);
    boolean actualReturnCode = processor.process(jsRequest, jsBuilder);
    assertTrue(actualReturnCode);
    assertEquals(
        FEATURE_CONTENT_3 + EXPORT_STRING_3,
        jsBuilder.build().toJsString());
  }

  @Test
  public void processWithFeaturesAndTexts() throws Exception {
    JsUri jsUri = mockJsUri(JsCompileMode.CONCAT_COMPILE_EXPORT_ALL);
    JsRequest jsRequest = new JsRequest(jsUri, null, false, null);
    JsResponseBuilder jsBuilder = new JsResponseBuilder();
    jsBuilder.appendJs(textJsContent1);
    jsBuilder.appendJs(featureJsContent1);
    jsBuilder.appendJs(featureJsContent2);
    jsBuilder.appendJs(textJsContent2);
    jsBuilder.appendJs(featureJsContent3);
    boolean actualReturnCode = processor.process(jsRequest, jsBuilder);
    assertTrue(actualReturnCode);
    assertEquals(EXPORT_JS_DEB + TEXT_CONTENT_1 +
        FEATURE_CONTENT_1 + EXPORT_STRING_1 +
        FEATURE_CONTENT_2 + EXPORT_STRING_2 +
        TEXT_CONTENT_2 +
        FEATURE_CONTENT_3 + EXPORT_STRING_3,
        jsBuilder.build().toJsString());
  }
}
