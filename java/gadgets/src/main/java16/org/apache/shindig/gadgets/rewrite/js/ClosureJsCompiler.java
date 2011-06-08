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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.javascript.jscomp.BasicErrorManager;
import com.google.javascript.jscomp.CheckLevel;
import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.JSError;
import com.google.javascript.jscomp.JSSourceFile;
import com.google.javascript.jscomp.Result;
import com.google.javascript.jscomp.SourceMap;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.shindig.common.cache.Cache;
import org.apache.shindig.common.cache.CacheProvider;
import org.apache.shindig.common.util.HashUtil;
import org.apache.shindig.gadgets.features.ApiDirective;
import org.apache.shindig.gadgets.features.FeatureRegistry.FeatureBundle;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.js.JsContent;
import org.apache.shindig.gadgets.js.JsResponse;
import org.apache.shindig.gadgets.js.JsResponseBuilder;
import org.apache.shindig.gadgets.rewrite.js.JsCompiler;
import org.apache.shindig.gadgets.uri.JsUriManager.JsUri;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ClosureJsCompiler implements JsCompiler {
  // Based on Closure Library's goog.exportSymbol implementation.
  private static final JsContent EXPORTSYMBOL_CODE =
      JsContent.fromText("var goog=goog||{};goog.exportSymbol=function(name,obj){"
              + "var parts=name.split('.'),cur=window,part;"
              + "for(;parts.length&&(part=parts.shift());){if(!parts.length){"
              + "cur[part]=obj;}else{cur=cur[part]||(cur[part]={})}}};", "[goog.exportSymbol]");
  
  @VisibleForTesting
  static final String CACHE_NAME = "CompiledJs";

  private final DefaultJsCompiler defaultCompiler;
  private final Cache<String, JsResponse> cache;
  private JsResponse lastResult;

  @Inject
  public ClosureJsCompiler(DefaultJsCompiler defaultCompiler, CacheProvider cacheProvider) {
    this.cache = cacheProvider.createCache(CACHE_NAME);
    this.defaultCompiler = defaultCompiler;
  }

  public static CompilerOptions defaultCompilerOptions() {
    CompilerOptions result = new CompilerOptions();
    CompilationLevel.ADVANCED_OPTIMIZATIONS.setOptionsForCompilationLevel(result);
    return result;
  }
  
  protected CompilerOptions getCompilerOptions(JsUri uri) {
    CompilerOptions options = defaultCompilerOptions();

    if (outputCorrelatedJs()) {
      setSourceMapCompilerOptions(options);
    }
    
    return options;
  }

  protected void setSourceMapCompilerOptions(CompilerOptions options) {
    options.sourceMapOutputPath = "create.out";
    options.sourceMapFormat = SourceMap.Format.V1;
    options.sourceMapDetailLevel = SourceMap.DetailLevel.ALL;
  }

  @VisibleForTesting
  Compiler newCompiler() {
    BasicErrorManager errorManager = new BasicErrorManager() {
      @Override
      protected void printSummary() { /* Do nothing */ }

      @Override
      public void println(CheckLevel arg0, JSError arg1) { /* Do nothing */ }
    };
    return new Compiler(errorManager);
  }

  public JsResponse compile(JsUri jsUri, Iterable<JsContent> content, String externs) {
    JsResponse exportResponse = defaultCompiler.compile(jsUri, content, externs);
    content = exportResponse.getAllJsContent();

    String cacheKey = makeCacheKey(exportResponse.toJsString(), externs, jsUri);
    JsResponse cachedResult = cache.getElement(cacheKey);
    if (cachedResult != null) {
      lastResult = cachedResult;
      return cachedResult;
    }

    JsResponseBuilder builder = new JsResponseBuilder();
    
    // Only run actual compiler if necessary.
    CompilerOptions options = getCompilerOptions(jsUri);
    
    if (!jsUri.isDebug() || options.isExternExportsEnabled()) {
      List<JSSourceFile> allExterns = Lists.newArrayList();
      allExterns.add(JSSourceFile.fromCode("externs", externs));

      List<JsContent> allContent = Lists.newLinkedList(content);
      if (options.isExternExportsEnabled()) {
        allContent.add(EXPORTSYMBOL_CODE);
      }

      Compiler actualCompiler = newCompiler();
      Result result = actualCompiler.compile(
          allExterns,
          convertToJsSource(allContent),
          options);

      if (actualCompiler.hasErrors()) {
        ImmutableList.Builder<String> errors = ImmutableList.builder();
        for (JSError error : actualCompiler.getErrors()) {
          errors.add(error.toString()); 
        }
        return cacheAndReturnErrorResult(
            builder, cacheKey,
            HttpResponse.SC_NOT_FOUND,
            errors.build());
      }

      String compiled = compileToSource(actualCompiler, result, jsUri);
      if (outputCorrelatedJs()) {
        // Emit code correlated w/ original source.
        // This operation is equivalent in final code to bundled-output,
        // but is less efficient and should perhaps only be used in code profiling.
        SourceMappings sm = processSourceMap(result, allContent);
        if (sm != null) {
          builder.appendAllJs(sm.mapCompiled(compiled));
        } else {
          return cacheAndReturnErrorResult(
              builder, cacheKey,
              HttpResponse.SC_INTERNAL_SERVER_ERROR,
              Lists.newArrayList("Parse error for source map"));
        }
      } else {
        builder.appendJs(compiled, "[compiled]");
      }

      builder.clearExterns().appendRawExtern(result.externExport);
    } else {
      // Otherwise, return original content and null exports.
      builder.appendAllJs(content);
    }

    JsResponse result = builder.build();
    cache.addElement(cacheKey, result);
    lastResult = result;
    return result;
  }

  protected String compileToSource(Compiler compiler, Result result, JsUri jsUri) {
    return compiler.toSource();
  }
  
  private JsResponse cacheAndReturnErrorResult(
      JsResponseBuilder builder, String cacheKey,
      int statusCode, List<String> messages) {
    builder.setStatusCode(statusCode);
    builder.addErrors(messages);
    JsResponse result = builder.build(); 
    cache.addElement(cacheKey, result);
    return result;
  }
  
  // Override this method to return "true" for cases where individual chunks of
  // compiled JS should be emitted as JsContent objects, each correlating output JS
  // with the original source file from which they came.
  protected boolean outputCorrelatedJs() {
    return false;
  }
  
  private List<JSSourceFile> convertToJsSource(Iterable<JsContent> content) {
    Map<String, Integer> sourceMap = Maps.newHashMap();
    List<JSSourceFile> sources = Lists.newLinkedList();
    for (JsContent src : content) {
      sources.add(JSSourceFile.fromCode(getUniqueSrc(src.getSource(), sourceMap), src.get()));
    }
    return sources;
  }
  
  // Return a unique string to represent the inbound "source" parameter.
  // Closure Compiler errors out when two JSSourceFiles with the same name are
  // provided, so this method tracks the currently-used source names (in the
  // provided sourceMap) and ensures that a unique name is returned.
  private static String getUniqueSrc(String source, Map<String, Integer> sourceMap) {
    Integer ix = sourceMap.get(source);
    if (ix == null) {
      ix = 0;
    }
    String ret = source + (ix > 0 ? ":" + ix : "");
    sourceMap.put(source, ix + 1);
    return ret;
  }
  
  private static String getRootSrc(String source) {
    int colIx = source.lastIndexOf(":");
    if (colIx == -1) {
      return source;
    }
    return source.substring(0, colIx);
  }

  public Iterable<JsContent> getJsContent(JsUri jsUri, FeatureBundle bundle) {
    jsUri = new JsUri(jsUri) {
      @Override
      public boolean isDebug() {
        // Force debug JS in the raw JS content retrieved.
        return true;
      }
    };
    List<JsContent> builder = Lists.newLinkedList(defaultCompiler.getJsContent(jsUri, bundle));

    CompilerOptions options = getCompilerOptions(jsUri);
    if (options.isExternExportsEnabled()) {
      List<String> exports = Lists.newArrayList(bundle.getApis(ApiDirective.Type.JS, true));
      Collections.sort(exports);
      String prevExport = null;
      for (String export : exports) {
        if (!export.equals(prevExport)) {
          builder.add(JsContent.fromText(
              "goog.exportSymbol('" + StringEscapeUtils.escapeJavaScript(export) +
              "', " + export + ");\n", "[export-symbol]"));
          prevExport = export;
        }
      }
    }
    return builder;
  }

  public JsResponse getLastResult() {
    return this.lastResult;
  }

  protected String makeCacheKey(String code, String externs, JsUri uri) {
    // TODO: include compilation options in the cache key
    return Joiner.on(":").join(
        HashUtil.checksum(code.getBytes()),
        HashUtil.checksum(externs.getBytes()),
        uri.getCompileMode(),
        uri.isDebug(),
        outputCorrelatedJs());
  }
  
  /**
   * Pull the source map out of the given Closure Result, and convert
   * it to a local SourceMappings object used to correlate compiled
   * content with originating source.
   * @param result Closure result object with source map.
   * @param allInputs All inputs supplied to the compiler, in JsContent form.
   * @return SourceMappings object correlating compiled with originating input.
   */
  private SourceMappings processSourceMap(Result result, List<JsContent> allInputs) {
    StringBuilder sb = new StringBuilder();
    try {
      result.sourceMap.appendTo(sb, "done");
      return SourceMappings.parseV1(sb.toString(), allInputs);
    } catch (IOException e) {
      return null;
    } catch (JSONException e) {
      return null;
    }
  }
  
  private static class SourceMappings {
    private final Map<String, JsContent> orig;
    private final int[][] lines;
    private final String[] mappings;
    
    private SourceMappings(int[][] lines, String[] mappings, List<JsContent> content) {
      this.lines = lines;
      this.mappings = mappings;
      this.orig = Maps.newHashMap();
      for (JsContent js : content) {
        orig.put(js.getSource(), js);
      }
    }
    
    private List<JsContent> mapCompiled(String compiled) {
      List<JsContent> compiledOut = Lists.newLinkedList();
      int codeStart = 0;
      int codePos = 0;
      int curMapping = -1;
      for (int line = 0; line < lines.length; ++line) {
        for (int col = 0; col < lines[line].length; ++col) {
          int nextMapping = lines[line][col];
          codePos++;
          if (nextMapping != curMapping && curMapping != -1) {
            appendJsContent(compiledOut, codeStart, codePos, compiled, curMapping);
            codeStart = codePos;
          }
          curMapping = nextMapping;
        }
      }
      appendJsContent(compiledOut, codeStart, codePos + 1, compiled, curMapping);
      return compiledOut;
    }
    
    private void appendJsContent(List<JsContent> out, int startPos, int codePos, 
        String compiled, int mapping) {
      JsContent sourceJs = orig.get(getRootSrc(mappings[mapping]));
      String sourceName = "[closure-compiler-synthesized]";
      FeatureBundle bundle = null;
      if (sourceJs != null) {
        sourceName = sourceJs.getSource() != null ? sourceJs.getSource() : "";
        bundle = sourceJs.getFeatureBundle();
      }
      out.add(JsContent.fromFeature(compiled.substring(startPos, codePos),
          sourceName, bundle, null));
    }
    
    private static final String BEGIN_COMMENT = "/*";
    private static final String END_COMMENT = "*/";
    private static SourceMappings parseV1(String sourcemap, List<JsContent> orig)
        throws IOException, JSONException {
      BufferedReader reader = new BufferedReader(new StringReader(sourcemap));
      JSONObject summary = new JSONObject(stripComment(reader.readLine()));
      
      int lineCount = summary.getInt("count");
      
      // Read lines info.
      int maxMappingIndex = 0;
      int[][] lines = new int[lineCount][];
      for (int i = 0; i < lineCount; ++i) {
        String lineDescriptor = reader.readLine();
        JSONArray lineArr = new JSONArray(lineDescriptor);
        lines[i] = new int[lineArr.length()];
        for (int j = 0; j < lineArr.length(); ++j) {
          int mappingIndex = lineArr.getInt(j);
          lines[i][j] = mappingIndex;
          maxMappingIndex = Math.max(mappingIndex, maxMappingIndex);
        }
      }
      
      // Bypass comment and unused file info for each line.
      reader.readLine(); // comment
      for (int i = 0; i < lineCount; ++i) {
        reader.readLine();
      }
      
      // Read mappings objects.
      reader.readLine(); // comment
      String[] mappings = new String[maxMappingIndex + 1];
      for (int i = 0; i <= maxMappingIndex; ++i) {
        String mappingLine = reader.readLine();
        JSONArray mappingObj = new JSONArray(mappingLine);
        mappings[i] = mappingObj.getString(0);
      }
      
      return new SourceMappings(lines, mappings, orig);
    }
    
    private static String stripComment(String line) {
      int begin = line.indexOf(BEGIN_COMMENT);
      if (begin != -1) {
        int end = line.indexOf(END_COMMENT, begin + 1);
        if (end != -1) {
          return line.substring(0, begin) + line.substring(end + END_COMMENT.length());
        }
      }
      return line;
    }
  }
}
