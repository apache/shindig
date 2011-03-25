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
package org.apache.shindig.gadgets.rewrite.js;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.JsCompileMode;
import org.apache.shindig.gadgets.RenderingContext;
import org.apache.shindig.gadgets.features.ApiDirective;
import org.apache.shindig.gadgets.features.FeatureRegistry;
import org.apache.shindig.gadgets.features.FeatureRegistry.FeatureBundle;
import org.apache.shindig.gadgets.features.FeatureRegistry.LookupResult;
import org.apache.shindig.gadgets.features.FeatureResource;
import org.apache.shindig.gadgets.js.JsContent;
import org.apache.shindig.gadgets.js.JsResponse;
import org.apache.shindig.gadgets.uri.JsUriManager.JsUri;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class ExportJsCompilerTest {
  private final String EXPORT_JS_DEB = "function exportJs() { };";
  private final String EXPORT_JS_OPT = "function a(){};";
  private final String RESOURCE_NAME = "xyz";
  private final String RESOURCE_CONTENT_DEB = "alert('deb');";
  private final String RESOURCE_CONTENT_OPT = "alert('opt');";
  private final String RESOURCE_URL_DEB = "deb.js";
  private final String RESOURCE_URL_OPT = "opt.js";
  private final String COMPILE_CONTENT = "alert('compile');";
  private final String CONTAINER = "container";

  private final List<String> EMPTY = ImmutableList.of();

  private final List<String> EXPORTS = ImmutableList.of(
      "gadgets",
      "gadgets.rpc.call",
      "cc",
      "cc.prototype.site");

  private final List<String> EXTERNS = ImmutableList.of(
      "foo",
      "foo.bar",
      "for.prototype.bar");

  private ExportJsCompiler compiler;

  @Before
  public void setUp() throws Exception {
    FeatureResource featureResourceMock = mockResource(true, EXPORT_JS_DEB, EXPORT_JS_OPT);
    FeatureRegistry.FeatureBundle featureBundle = createMock(FeatureRegistry.FeatureBundle.class);
    expect(featureBundle.getResources()).andReturn(
        ImmutableList.of(featureResourceMock)).anyTimes();
    expect(featureBundle.getName()).andReturn("feature").anyTimes();
    replay(featureBundle);
    LookupResult lookupMock = mockLookupResult(featureBundle);
    FeatureRegistry featureRegistryMock = mockRegistry(lookupMock);
    compiler = new ExportJsCompiler(featureRegistryMock);
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
    JsUri result = createMock(JsUri.class);
    expect(result.getContext()).andReturn(RenderingContext.GADGET).anyTimes();
    expect(result.getContainer()).andReturn(CONTAINER).anyTimes();
    expect(result.getCompileMode()).andReturn(mode).anyTimes();
    expect(result.isDebug()).andReturn(false).anyTimes();
    replay(result);
    return result;
  }

  private LookupResult mockLookupResult(FeatureRegistry.FeatureBundle featureBundle) {
    LookupResult result = createMock(LookupResult.class);
    expect(result.getBundles()).andReturn(ImmutableList.of(featureBundle)).anyTimes();
    replay(result);
    return result;
  }

  private FeatureResource mockResource(boolean external, String debContent, String optContent) {
    FeatureResource result = createMock(FeatureResource.class);
    expect(result.getDebugContent()).andReturn(debContent).anyTimes();
    expect(result.getContent()).andReturn(optContent).anyTimes();
    expect(result.isExternal()).andReturn(external).anyTimes();
    expect(result.getName()).andReturn("js").anyTimes();
    replay(result);
    return result;
  }

  private FeatureBundle mockBundle(List<FeatureResource> resources, List<String> exports, List<String> externs) {
    List<ApiDirective> apis = Lists.newArrayList();
    for (String e : exports) apis.add(mockApiDirective(true, e));
    for (String e : externs) apis.add(mockApiDirective(false, e));
    FeatureBundle result = createMock(FeatureBundle.class);
    expect(result.getResources()).andReturn(resources).anyTimes();
    expect(result.getName()).andReturn(RESOURCE_NAME).anyTimes();
    expect(result.getApis(ApiDirective.Type.JS, true)).andReturn(exports).anyTimes();
    expect(result.getApis(ApiDirective.Type.JS, false)).andReturn(externs).anyTimes();
    replay(result);
    return result;
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
  public void testGetJsContentEmpty() throws Exception {
    JsUri jsUri = mockJsUri(JsCompileMode.ALL_RUN_TIME);
    FeatureBundle bundle = mockBundle(Lists.<FeatureResource>newArrayList(), EMPTY, EMPTY); // empty
    Iterable<JsContent> actual = compiler.getJsContent(jsUri, bundle);
    assertEquals(
        "\n/* [start] feature=" + RESOURCE_NAME + " */\n" +
        "\n/* [end] feature=" + RESOURCE_NAME + " */\n",
        getContent(actual));
  }

  @Test
  public void testGetJsContentNotEmpty() throws Exception {
    JsUri jsUri = mockJsUri(JsCompileMode.ALL_RUN_TIME);
    FeatureResource extRes = mockResource(true, RESOURCE_URL_DEB, RESOURCE_URL_OPT);
    FeatureResource intRes = mockResource(false, RESOURCE_CONTENT_DEB, RESOURCE_CONTENT_OPT);
    FeatureBundle bundle = mockBundle(Lists.newArrayList(extRes, intRes), EXPORTS, EXTERNS);
    Iterable<JsContent> actual = compiler.getJsContent(jsUri, bundle);
    assertEquals(
        "\n/* [start] feature=" + RESOURCE_NAME + " */\n" +
        "document.write('<script src=\"" + RESOURCE_URL_DEB + "\"></script>');\n" +
        RESOURCE_CONTENT_DEB + ";\n" +
        "exportJs('gadgets',[gadgets]);" +
        "exportJs('cc',[cc]);" +
        "exportJs('gadgets.rpc',[gadgets,gadgets.rpc],{call:'call'});" +
        "exportJs('cc.prototype',[cc,cc.prototype],{site:'site'});" +
        "\n/* [end] feature=" + RESOURCE_NAME + " */\n",
        getContent(actual));
  }

  @Test
  public void testCompileNotEmpty() throws Exception {
    JsUri jsUri = mockJsUri(JsCompileMode.ALL_RUN_TIME);

    JsResponse actual = compiler.compile(jsUri,
        ImmutableList.of(new JsContent(COMPILE_CONTENT, "js")), EXTERNS);
    assertEquals(EXPORT_JS_DEB + COMPILE_CONTENT, actual.toJsString());
  }

  @Test
  public void testCompileEmpty() throws Exception {
    JsUri jsUri = mockJsUri(JsCompileMode.ALL_RUN_TIME);
    JsResponse actual = compiler.compile(jsUri,
        ImmutableList.of(new JsContent("", "js")), EXTERNS);
    assertEquals(EXPORT_JS_DEB, actual.toJsString());
  }

  private String getContent(Iterable<JsContent> jsContent) {
    StringBuilder sb = new StringBuilder();
    for (JsContent js : jsContent) {
      sb.append(js.get());
    }
    return sb.toString();
  }
}
