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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.DiagnosticType;
import com.google.javascript.jscomp.JSError;
import com.google.javascript.jscomp.JSSourceFile;
import com.google.javascript.jscomp.Result;

import junit.framework.TestCase;

import org.apache.shindig.common.cache.Cache;
import org.apache.shindig.common.cache.CacheProvider;
import org.apache.shindig.common.cache.NullCache;
import org.apache.shindig.gadgets.JsCompileMode;
import org.apache.shindig.gadgets.RenderingContext;
import org.apache.shindig.gadgets.features.ApiDirective;
import org.apache.shindig.gadgets.features.FeatureRegistry.FeatureBundle;
import org.apache.shindig.gadgets.js.JsContent;
import org.apache.shindig.gadgets.js.JsResponse;
import org.apache.shindig.gadgets.rewrite.js.DefaultJsCompiler;
import org.apache.shindig.gadgets.uri.UriStatus;
import org.apache.shindig.gadgets.uri.JsUriManager.JsUri;

import java.util.List;

public class ClosureJsCompilerTest extends TestCase {

  private Compiler realCompMock;
  private CompilerOptions realOptionsMock;
  private Result realResultMock;
  private DefaultJsCompiler compilerMock;
  private JsResponse exportResponseMock;
  private JsUri jsUriMock;
  private CacheProvider cacheMock;
  private ClosureJsCompiler compiler;

  private final String ACTUAL_COMPILER_OUTPUT = "window.abc={};";
  private final String EXPORT_COMPILER_STRING = "window['abc'] = {};";
  private final Iterable<JsContent> EXPORT_COMPILER_CONTENTS =
      newJsContents(EXPORT_COMPILER_STRING);

  private final String CLOSURE_ACTUAL_COMPILER_OUTPUT = ACTUAL_COMPILER_OUTPUT;
  private final String CLOSURE_EXPORT_COMPILER_OUTPUT = EXPORT_COMPILER_STRING;

  private final List<String> EXPORTS = ImmutableList.of("foo", "bar");

  private final String EXTERN = "extern";
  private final String ERROR_NAME = "error";
  private final JSError JS_ERROR = JSError.make(
      "js", 12, 34, DiagnosticType.error(ERROR_NAME, "errDesc"));

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    cacheMock = new MockProvider();
    exportResponseMock = mockJsResponse();
    compilerMock = mockDefaultJsCompiler(exportResponseMock);
  }

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

  public void testGetJsContentWithoutGoogSymbolExports() throws Exception {
    realOptionsMock = mockRealJsCompilerOptions(false); // without
    compiler = newClosureJsCompiler(null, realOptionsMock, compilerMock, cacheMock);
    FeatureBundle bundle = mockBundle(EXPORTS);
    Iterable<JsContent> actual = compiler.getJsContent(mockJsUri(false), bundle);
    assertEquals(EXPORT_COMPILER_STRING, getContent(actual));
  }

  public void testCompileSuccessOpt() throws Exception {
    jsUriMock = mockJsUri(false); // opt
    realResultMock = mockRealJsResult();
    realCompMock = mockRealJsCompiler(null, realResultMock, ACTUAL_COMPILER_OUTPUT);
    realOptionsMock = mockRealJsCompilerOptions(false);
    compiler = newClosureJsCompiler(realCompMock, realOptionsMock, compilerMock, cacheMock);
    JsResponse actual = compiler.compile(jsUriMock, EXPORT_COMPILER_CONTENTS,
        ImmutableList.of(EXTERN));
    assertEquals(CLOSURE_ACTUAL_COMPILER_OUTPUT, actual.toJsString());
    assertTrue(actual.getErrors().isEmpty());
  }

  public void testCompileSuccessDeb() throws Exception {
    jsUriMock = mockJsUri(true); // debug
    realResultMock = mockRealJsResult();
    realCompMock = mockRealJsCompiler(null, realResultMock, ACTUAL_COMPILER_OUTPUT);
    realOptionsMock = mockRealJsCompilerOptions(false);
    compiler = newClosureJsCompiler(realCompMock, realOptionsMock, compilerMock, cacheMock);
    JsResponse actual = compiler.compile(jsUriMock, EXPORT_COMPILER_CONTENTS,
        ImmutableList.of(EXTERN));
    assertEquals(CLOSURE_EXPORT_COMPILER_OUTPUT, actual.toJsString());
    assertTrue(actual.getErrors().isEmpty());
  }

  public void testCompileErrorOpt() throws Exception {
    jsUriMock = mockJsUri(false); // opt
    realCompMock = mockRealJsCompiler(JS_ERROR, realResultMock, ACTUAL_COMPILER_OUTPUT);
    realOptionsMock = mockRealJsCompilerOptions(true); // force compiler to run
    compiler = newClosureJsCompiler(realCompMock, realOptionsMock, compilerMock, cacheMock);
    JsResponse actual = compiler.compile(jsUriMock, EXPORT_COMPILER_CONTENTS,
        ImmutableList.of(EXTERN));
    assertTrue(actual.getErrors().get(0).contains(ERROR_NAME));
    assertEquals(1, actual.getErrors().size());
  }

  public void testCompileErrorDeb() throws Exception {
    jsUriMock = mockJsUri(true); // debug
    realCompMock = mockRealJsCompiler(JS_ERROR, realResultMock, ACTUAL_COMPILER_OUTPUT);
    realOptionsMock = mockRealJsCompilerOptions(true); // force compiler to run
    compiler = newClosureJsCompiler(realCompMock, realOptionsMock, compilerMock, cacheMock);
    JsResponse actual = compiler.compile(jsUriMock, EXPORT_COMPILER_CONTENTS,
        ImmutableList.of(EXTERN));
    assertTrue(actual.getErrors().get(0).contains(ERROR_NAME));
    assertEquals(1, actual.getErrors().size());
  }

  private ClosureJsCompiler newClosureJsCompiler(final Compiler realComp,
      CompilerOptions realOptions, DefaultJsCompiler defaultComp, CacheProvider cache) {
    return new ClosureJsCompiler(defaultComp, cache) {
      @Override
      Compiler newCompiler() {
        return realComp;
      }
      
      @Override
      protected CompilerOptions getCompilerOptions(JsUri uri) {
        return realOptionsMock;
      }
    };
  }

  private JsResponse mockJsResponse() {
    JsResponse result = createMock(JsResponse.class);
    expect(result.toJsString()).andReturn(EXPORT_COMPILER_STRING).anyTimes();
    expect(result.getAllJsContent()).andReturn(EXPORT_COMPILER_CONTENTS).anyTimes();
    replay(result);
    return result;
  }

  @SuppressWarnings("unchecked")
  private DefaultJsCompiler mockDefaultJsCompiler(JsResponse res) {
    DefaultJsCompiler result = createMock(DefaultJsCompiler.class);
    expect(result.getJsContent(isA(JsUri.class), isA(FeatureBundle.class)))
        .andReturn(EXPORT_COMPILER_CONTENTS).anyTimes();
    expect(result.compile(isA(JsUri.class), isA(Iterable.class), isA(List.class)))
        .andReturn(res).anyTimes();
    replay(result);
    return result;
  }

  private Result mockRealJsResult() {
    return createMock(Result.class);
  }

  private Compiler mockRealJsCompiler(JSError error, Result res, String toSource) {
    Compiler result = createMock(Compiler.class);
    expect(result.compile(isA(JSSourceFile[].class), isA(JSSourceFile[].class),
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
    result.add(JsContent.fromText(jsCode, null));
    return result;
  }
}
