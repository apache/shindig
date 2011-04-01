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
import static org.easymock.EasyMock.same;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.List;

import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.RenderingContext;
import org.apache.shindig.gadgets.features.ApiDirective;
import org.apache.shindig.gadgets.features.FeatureRegistry;
import org.apache.shindig.gadgets.features.FeatureRegistry.FeatureBundle;
import org.apache.shindig.gadgets.features.FeatureRegistry.LookupResult;
import org.apache.shindig.gadgets.rewrite.js.JsCompiler;
import org.apache.shindig.gadgets.uri.JsUriManager.JsUri;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class CompilationProcessorTest {
  private IMocksControl control;
  private JsCompiler compiler;
  private FeatureRegistry registry;
  private CompilationProcessor processor;
  
  @Before
  public void setUp() throws Exception {
    control = createControl();
    compiler = control.createMock(JsCompiler.class);
    registry = control.createMock(FeatureRegistry.class);
    processor = new CompilationProcessor(compiler, registry);
  }
  
  @Test
  public void compilerIsRun() throws Exception {
    JsUri jsUri = control.createMock(JsUri.class);
    LookupResult lookupResult = control.createMock(LookupResult.class);
    FeatureBundle bundle = control.createMock(FeatureBundle.class);
    ImmutableList<String> externs = ImmutableList.of("e1", "e2");
    expect(bundle.getApis(ApiDirective.Type.JS, false)).andReturn(externs);
    expect(lookupResult.getBundles()).andReturn(ImmutableList.<FeatureBundle>of(bundle));
    expect(registry.getFeatureResources(isA(GadgetContext.class), eq(ImmutableList.of("f1", "f2")),
        eq(ImmutableList.<String>of()))).andReturn(lookupResult);
    JsResponseBuilder builder =
        new JsResponseBuilder().setCacheTtlSecs(1234).setStatusCode(200)
          .appendJs(JsContent.fromFeature("content1:", "source1", "f1", null))
          .appendJs(JsContent.fromFeature("content2", "source2", "f2", null));
    JsResponse outputResponse = new JsResponseBuilder().appendJs("content3", "s3").build();
    JsRequest request = control.createMock(JsRequest.class);
    expect(request.getJsUri()).andReturn(jsUri);
    expect(compiler.compile(same(jsUri), eq(builder.build().getAllJsContent()), eq(externs)))
        .andReturn(outputResponse);
    
    control.replay();
    boolean status = processor.process(request, builder);
    control.verify();
    
    assertTrue(status);
    JsResponse compResult = builder.build();
    assertEquals(200, compResult.getStatusCode());
    assertEquals(1234, compResult.getCacheTtlSecs());
    assertEquals("content3", compResult.toJsString());
    Iterator<JsContent> outIterator = compResult.getAllJsContent().iterator();
    JsContent firstOut = outIterator.next();
    assertEquals("content3", firstOut.get());
    assertEquals("s3", firstOut.getSource());
    assertFalse(outIterator.hasNext());
  }

  @Test(expected = JsException.class)
  public void compilerExceptionThrows() throws Exception {
    JsUri jsUri = control.createMock(JsUri.class);
    LookupResult lookupResult = control.createMock(LookupResult.class);
    FeatureBundle bundle = control.createMock(FeatureBundle.class);
    ImmutableList<String> externs = ImmutableList.of();
    expect(bundle.getApis(ApiDirective.Type.JS, false)).andReturn(externs);
    expect(lookupResult.getBundles()).andReturn(ImmutableList.<FeatureBundle>of(bundle));
    expect(registry.getFeatureResources(isA(GadgetContext.class), eq(ImmutableList.of("f1")),
        eq(ImmutableList.<String>of()))).andReturn(lookupResult);
    JsResponseBuilder builder =
        new JsResponseBuilder().setCacheTtlSecs(1234).setStatusCode(200)
          .appendJs(JsContent.fromFeature("content1:", "source1", "f1", null));
    JsRequest request = control.createMock(JsRequest.class);
    expect(request.getJsUri()).andReturn(jsUri);
    expect(compiler.compile(same(jsUri), eq(builder.build().getAllJsContent()), eq(externs)))
        .andThrow(new JsException(400, "foo"));
    control.replay();
    processor.process(request, builder);
  }
}
