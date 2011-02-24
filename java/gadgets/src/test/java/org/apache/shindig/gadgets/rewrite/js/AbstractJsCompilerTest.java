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
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableList;

import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.features.FeatureRegistry;
import org.apache.shindig.gadgets.features.FeatureRegistry.LookupResult;
import org.apache.shindig.gadgets.features.FeatureResource;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class AbstractJsCompilerTest {
  private final String EXPORT_JS_FUNCTION = "function exportJs() { };";

  private AbstractJsCompiler compiler;
  private GadgetContext context;

  @Before
  public void setUp() throws Exception {
    context = new GadgetContext();
    FeatureResource featureResourceMock = mockFeatureResource();
    FeatureRegistry.FeatureBundle featureBundle = new FeatureRegistry.FeatureBundle(
        null, ImmutableList.of(featureResourceMock));
    LookupResult lookupMock = mockLookupResult(featureBundle);
    FeatureRegistry featureRegistryMock = mockFeatureRegistry(lookupMock);

    compiler = new AbstractJsCompiler(featureRegistryMock) {
      public Result compile(String jsData, List<String> externs) {
        return null;
      }
    };
  }

  @SuppressWarnings("unchecked")
  private FeatureRegistry mockFeatureRegistry(LookupResult lookupMock) {
    FeatureRegistry result = createMock(FeatureRegistry.class);
    expect(result.getFeatureResources(
        context, ImmutableList.of(AbstractJsCompiler.FEATURE_NAME), null)).
        andReturn(lookupMock).anyTimes();
    replay(result);
    return result;
  }

  private LookupResult mockLookupResult(FeatureRegistry.FeatureBundle featureBundle) {
    LookupResult result = createMock(LookupResult.class);
    expect(result.getBundles()).andReturn(ImmutableList.of(featureBundle)).anyTimes();
    replay(result);
    return result;
  }

  private FeatureResource mockFeatureResource() {
    FeatureResource result = createMock(FeatureResource.class);
    expect(result.getDebugContent()).andReturn(EXPORT_JS_FUNCTION).anyTimes();
    replay(result);
    return result;
  }

  @Test
  public void testGenerateExportStatementsEmpty() throws Exception {
    String actual = compiler.generateExportStatements(context, ImmutableList.<String>of());
    assertEquals("", actual);
  }

  @Test
  public void testGenerateExportStatementsNotEmpty() throws Exception {
    ImmutableList<String> list = ImmutableList.of(
        "gadgets",
        "gadgets.rpc.call",
        "cc",
        "cc.prototype.site");
    String actual = compiler.generateExportStatements(context, list);
    assertEquals(
        EXPORT_JS_FUNCTION +
        "exportJs('gadgets',[gadgets]);" +
        "exportJs('cc',[cc]);" +
        "exportJs('gadgets.rpc',[gadgets,gadgets.rpc],{call:'call'});" +
        "exportJs('cc.prototype',[cc,cc.prototype],{site:'site'});",
        actual);
    list = ImmutableList.of("gadgets.util.makeClosure");
    actual = compiler.generateExportStatements(context, list);
    assertEquals(
        // JS functions only get inlined once.
        "exportJs('gadgets.util',[gadgets,gadgets.util],{makeClosure:'makeClosure'});",
        actual);
  }
}
