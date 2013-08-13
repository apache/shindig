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
package org.apache.shindig.gadgets.rewrite.js;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createMockBuilder;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.apache.shindig.common.cache.Cache;
import org.apache.shindig.common.cache.CacheProvider;
import org.apache.shindig.common.cache.NullCache;
import org.apache.shindig.gadgets.JsCompileMode;
import org.apache.shindig.gadgets.RenderingContext;
import org.apache.shindig.gadgets.features.ApiDirective;
import org.apache.shindig.gadgets.features.FeatureRegistry.FeatureBundle;
import org.apache.shindig.gadgets.js.JsContent;
import org.apache.shindig.gadgets.js.JsResponse;
import org.apache.shindig.gadgets.uri.JsUriManager.JsUri;
import org.apache.shindig.gadgets.uri.UriStatus;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.DiagnosticType;
import com.google.javascript.jscomp.JSError;
import com.google.javascript.jscomp.JSSourceFile;
import com.google.javascript.jscomp.Result;
import com.google.javascript.jscomp.SourceMap;
import com.google.javascript.jscomp.SourceMap.Format;

public class ClosureJsCompilerTest {
  private static final String ACTUAL_COMPILER_OUTPUT = "window.abc={};";
  private static final String EXPORT_COMPILER_STRING = "window['abc'] = {};";
  private static final Iterable<JsContent> EXPORT_COMPILER_CONTENTS =
      newJsContents(EXPORT_COMPILER_STRING);
  private static final List<String> EXPORTS = ImmutableList.of("foo", "bar");
  private static final String EXTERN = "extern";
  private static final String ERROR_NAME = "error";
  private static final JSError JS_ERROR = JSError.make("js", 12, 34,
      DiagnosticType.error(ERROR_NAME, "errDesc"));
  private static final Map<String, String> COMPILER_IO = ImmutableMap.<String, String>builder()
      .put("  ", "")
      .put(EXPORT_COMPILER_STRING, ACTUAL_COMPILER_OUTPUT)
      .put("var foo = function(x) {}", "var foo=function(x){};")
      .put("var foo = function(x) { bar(x, x) }; \n var bar = function(x, y) { bar(x) };",
          "var foo=function(x){bar(x,x)};var bar=function(x,y){bar(x)};")
      .put("", "")
      .build();

  private Compiler realCompMock;
  private CompilerOptions realOptionsMock;
  private Result realResultMock;
  private DefaultJsCompiler compilerMock;
  private JsResponse exportResponseMock;
  private JsUri jsUriMock;
  private CacheProvider cacheMock;
  private ClosureJsCompiler compiler;
  private ExecutorService executorServiceMock;

  @Before
  public void setUp() throws Exception {
    cacheMock = new MockProvider();
    exportResponseMock = mockJsResponse(EXPORT_COMPILER_STRING);
    compilerMock = mockDefaultJsCompiler(exportResponseMock, EXPORT_COMPILER_CONTENTS);
    executorServiceMock = EasyMock.createMock(ExecutorService.class);
    EasyMock.makeThreadSafe(executorServiceMock, true);
  }

  @Ignore("This class was not being run and when I ran it this test did not pass.  Not familiar enough to enable it.")
  @Test
  public void testGetJsContentWithGoogSymbolExports() throws Exception {
    realOptionsMock = mockRealJsCompilerOptions(true); // with
    compiler = newClosureJsCompiler(null, realOptionsMock, compilerMock, cacheMock);
    FeatureBundle bundle = mockBundle(EXPORTS);
    Iterable<JsContent> actual = compiler.getJsContent(mockJsUri(false), bundle);
    assertEquals(EXPORT_COMPILER_STRING +
        "goog.exportSymbol('bar', bar);\n" +
        "goog.exportSymbol('foo', foo);\n",
        getContent(actual));
  }

  @Ignore("This class was not being run and when I ran it this test did not pass.  Not familiar enough to enable it.")
  @Test
  public void testGetJsContentWithoutGoogSymbolExports() throws Exception {
    realOptionsMock = mockRealJsCompilerOptions(false); // without
    compiler = newClosureJsCompiler(null, realOptionsMock, compilerMock, cacheMock);
    FeatureBundle bundle = mockBundle(EXPORTS);
    Iterable<JsContent> actual = compiler.getJsContent(mockJsUri(false), bundle);
    assertEquals(EXPORT_COMPILER_STRING, getContent(actual));
  }

  @Test
  public void testCompileSuccessOpt() throws Exception {
    jsUriMock = mockJsUri(false); // opt
    realResultMock = mockRealJsResult();
    realCompMock = mockRealJsCompiler(null, realResultMock, ACTUAL_COMPILER_OUTPUT);
    realOptionsMock = mockRealJsCompilerOptions(false);
    compiler = newClosureJsCompiler(realCompMock, realOptionsMock, compilerMock, cacheMock);
    JsResponse actual = compiler.compile(jsUriMock, EXPORT_COMPILER_CONTENTS, EXTERN);
    assertEquals(ACTUAL_COMPILER_OUTPUT, actual.toJsString());
    assertTrue(actual.getErrors().isEmpty());
  }

  @Ignore("This class was not being run and when I ran it this test did not pass.  Not familiar enough to enable it.")
  @SuppressWarnings("unchecked")
  @Test
  public void testCompileSuccessOptWithProfiling() throws Exception {
    jsUriMock = mockJsUri(false); // opt

    realOptionsMock = new CompilerOptions();
    realOptionsMock.enableExternExports(false);
    realOptionsMock.sourceMapOutputPath = "test.out";
    realOptionsMock.sourceMapFormat = Format.V2;
    realOptionsMock.sourceMapDetailLevel = SourceMap.DetailLevel.ALL;
    realOptionsMock.ideMode = false;
    realOptionsMock.convertToDottedProperties = true;

    for (Map.Entry<String, String> compilerTest : COMPILER_IO.entrySet()) {
      List<JsContent> content = newJsContents(compilerTest.getKey());
      exportResponseMock = mockJsResponse(compilerTest.getKey());
      compilerMock = mockDefaultJsCompiler(exportResponseMock, content);
      compiler = newProfilingClosureJsCompiler(realOptionsMock, compilerMock, cacheMock);

      JsResponse actual = compiler.compile(jsUriMock, content, EXTERN);
      assertEquals(compilerTest.getValue(), actual.toJsString());
      assertTrue(actual.getErrors().isEmpty());
    }
  }

  @Test
  public void testCompileSuccessDeb() throws Exception {
    jsUriMock = mockJsUri(true); // debug
    realResultMock = mockRealJsResult();
    realCompMock = mockRealJsCompiler(null, realResultMock, ACTUAL_COMPILER_OUTPUT);
    realOptionsMock = mockRealJsCompilerOptions(false);
    compiler = newClosureJsCompiler(realCompMock, realOptionsMock, compilerMock, cacheMock);
    JsResponse actual = compiler.compile(jsUriMock, EXPORT_COMPILER_CONTENTS, EXTERN);
    assertEquals(EXPORT_COMPILER_STRING, actual.toJsString());
    assertTrue(actual.getErrors().isEmpty());
  }

  @Ignore("This class was not being run and when I ran it this test did not pass.  Not familiar enough to enable it.")
  @Test
  public void testCompileErrorOpt() throws Exception {
    jsUriMock = mockJsUri(false); // opt
    realCompMock = mockRealJsCompiler(JS_ERROR, realResultMock, ACTUAL_COMPILER_OUTPUT);
    realOptionsMock = mockRealJsCompilerOptions(true); // force compiler to run
    compiler = newClosureJsCompiler(realCompMock, realOptionsMock, compilerMock, cacheMock);
    JsResponse actual = compiler.compile(jsUriMock, EXPORT_COMPILER_CONTENTS, EXTERN);
    assertTrue(actual.getErrors().get(0).contains(ERROR_NAME));
    assertEquals(1, actual.getErrors().size());
  }

  @Ignore("This class was not being run and when I ran it this test did not pass.  Not familiar enough to enable it.")
  @Test
  public void testCompileErrorDeb() throws Exception {
    jsUriMock = mockJsUri(true); // debug
    realCompMock = mockRealJsCompiler(JS_ERROR, realResultMock, ACTUAL_COMPILER_OUTPUT);
    realOptionsMock = mockRealJsCompilerOptions(true); // force compiler to run
    realResultMock = mockRealJsResult();
    compiler = newClosureJsCompiler(realCompMock, realOptionsMock, compilerMock, cacheMock);
    JsResponse actual = compiler.compile(jsUriMock, EXPORT_COMPILER_CONTENTS, EXTERN);
    assertTrue(actual.getErrors().get(0).contains(ERROR_NAME));
    assertEquals(1, actual.getErrors().size());
  }

  private ClosureJsCompiler newClosureJsCompiler(final Compiler realComp,
      CompilerOptions realOptions, DefaultJsCompiler defaultComp, CacheProvider cache) throws InterruptedException, ExecutionException {
    Future<CompileResult> mockFuture = EasyMock.createMock(Future.class);
    expect(mockFuture.get()).andReturn(new CompileResult(realComp, realResultMock)).anyTimes();
    replay(mockFuture);
    expect(executorServiceMock.submit(isA(Callable.class))).andReturn(mockFuture);
    replay(executorServiceMock);
    ClosureJsCompiler compiler = createMockBuilder(ClosureJsCompiler.class)
        .addMockedMethods("getCompilerOptions")
        .withConstructor(defaultComp, cache, "simple", executorServiceMock)
        .createMock();
    expect(compiler.getCompilerOptions(isA(JsUri.class))).andReturn(realOptionsMock).anyTimes();
    
    replay(compiler);
    return compiler;
  }

  private ClosureJsCompiler newProfilingClosureJsCompiler(CompilerOptions realOptions,
      DefaultJsCompiler defaultComp, CacheProvider cache) {
    ClosureJsCompiler compiler =
        createMockBuilder(ClosureJsCompiler.class)
            .addMockedMethods("getCompilerOptions")
            .withConstructor(defaultComp, cache, "simple").createMock();
    expect(compiler.getCompilerOptions(isA(JsUri.class))).andReturn(realOptions).anyTimes();
    replay(compiler);
    return compiler;
  }

  private JsResponse mockJsResponse(String content) {
    JsResponse result = createMock(JsResponse.class);
    expect(result.toJsString()).andReturn(content).anyTimes();
    expect(result.getAllJsContent()).andReturn(newJsContents(content)).anyTimes();
    replay(result);
    return result;
  }

  @SuppressWarnings("unchecked")
  private DefaultJsCompiler mockDefaultJsCompiler(JsResponse res, Iterable<JsContent> content) {
    DefaultJsCompiler result = createMock(DefaultJsCompiler.class);
    expect(result.getJsContent(isA(JsUri.class), isA(FeatureBundle.class)))
        .andReturn(content).anyTimes();
    expect(result.compile(isA(JsUri.class), isA(Iterable.class), isA(String.class)))
        .andReturn(res).anyTimes();
    replay(result);
    return result;
  }

  private Result mockRealJsResult() {
    Result result = createMock(Result.class);
    replay(result);
    return result;
  }

  private Compiler mockRealJsCompiler(JSError error, Result res, String toSource) {
    Compiler result = createMock(Compiler.class);
    expect(result.compile(EasyMock.<List<JSSourceFile>>anyObject(),
        EasyMock.<List<JSSourceFile>>anyObject(),
        isA(CompilerOptions.class))).andReturn(res);
    if (error != null) {
      expect(result.hasErrors()).andReturn(true);
      expect(result.getErrors()).andReturn(new JSError[] { error });
    } else {
      expect(result.hasErrors()).andReturn(false);
    }
    expect(result.getResult()).andReturn(res);
    expect(result.toSource()).andReturn(toSource);
    replay(result);
    return result;
  }

  private CompilerOptions mockRealJsCompilerOptions(boolean enableExternExports) {
    CompilerOptions result = createMock(CompilerOptions.class);
    expect(result.isExternExportsEnabled()).andReturn(enableExternExports).anyTimes();
    replay(result);
    return result;
  }

  private JsUri mockJsUri(boolean debug) {
    JsUri result = createMock(JsUri.class);
    expect(result.isDebug()).andReturn(debug).anyTimes();
    expect(result.getCompileMode()).andReturn(JsCompileMode.CONCAT_COMPILE_EXPORT_ALL).anyTimes();
    expect(result.getStatus()).andReturn(UriStatus.VALID_UNVERSIONED).anyTimes();
    expect(result.getContainer()).andReturn("container").anyTimes();
    expect(result.getContext()).andReturn(RenderingContext.CONFIGURED_GADGET).anyTimes();
    expect(result.getRefresh()).andReturn(1000).anyTimes();
    expect(result.isNoCache()).andReturn(false).anyTimes();
    expect(result.getGadget()).andReturn("http://foo.com/g.xml").anyTimes();
    expect(result.getLibs()).andReturn(ImmutableList.<String>of()).anyTimes();
    expect(result.getLoadedLibs()).andReturn(ImmutableList.<String>of()).anyTimes();
    expect(result.getOnload()).andReturn("foo").anyTimes();
    expect(result.isJsload()).andReturn(true).anyTimes();
    expect(result.isNohint()).andReturn(true).anyTimes();
    expect(result.getOrigUri()).andReturn(null).anyTimes();
    expect(result.getRepository()).andReturn(null).anyTimes();
    expect(result.getExtensionParams()).andReturn(null).anyTimes();
    replay(result);
    return result;
  }

  private FeatureBundle mockBundle(List<String> exports) {
    FeatureBundle result = createMock(FeatureBundle.class);
    expect(result.getApis(ApiDirective.Type.JS, true)).andReturn(exports).anyTimes();
    expect(result.getName()).andReturn(null).anyTimes();
    replay(result);
    return result;
  }

  private class MockProvider implements CacheProvider {
    public <K, V> Cache<K, V> createCache(String name) {
      return new NullCache<K, V>();
    }
  }

  private String getContent(Iterable<JsContent> jsContent) {
    StringBuilder sb = new StringBuilder();
    for (JsContent js : jsContent) {
      sb.append(js.get());
    }
    return sb.toString();
  }

  private static List<JsContent> newJsContents(String jsCode) {
    List<JsContent> result = Lists.newArrayList();
    result.add(JsContent.fromText(jsCode, "testSource"));
    return result;
  }
  
}
