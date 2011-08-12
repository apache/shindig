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

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.shindig.common.cache.Cache;
import org.apache.shindig.common.cache.CacheProvider;
import org.apache.shindig.common.logging.i18n.MessageKeys;
import org.apache.shindig.common.util.HashUtil;
import org.apache.shindig.gadgets.features.ApiDirective;
import org.apache.shindig.gadgets.features.FeatureRegistry.FeatureBundle;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.js.JsContent;
import org.apache.shindig.gadgets.js.JsResponse;
import org.apache.shindig.gadgets.js.JsResponseBuilder;
import org.apache.shindig.gadgets.uri.JsUriManager.JsUri;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.debugging.sourcemap.SourceMapConsumerFactory;
import com.google.debugging.sourcemap.SourceMapParseException;
import com.google.debugging.sourcemap.SourceMapping;
import com.google.debugging.sourcemap.proto.Mapping.OriginalMapping;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.javascript.jscomp.BasicErrorManager;
import com.google.javascript.jscomp.CheckLevel;
import com.google.javascript.jscomp.CommandLineRunner;
import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.JSError;
import com.google.javascript.jscomp.JSSourceFile;
import com.google.javascript.jscomp.Result;
import com.google.javascript.jscomp.SourceMap;

public class ClosureJsCompiler implements JsCompiler {
  // Based on Closure Library's goog.exportSymbol implementation.
  private static final JsContent EXPORTSYMBOL_CODE =
      JsContent.fromText("var goog=goog||{};goog.exportSymbol=function(name,obj){"
              + "var parts=name.split('.'),cur=window,part;"
              + "for(;parts.length&&(part=parts.shift());){if(!parts.length){"
              + "cur[part]=obj;}else{cur=cur[part]||(cur[part]={})}}};", "[goog.exportSymbol]");

  //class name for logging purpose
  private static final String classname = ClosureJsCompiler.class.getName();
  private static final Logger LOG = Logger.getLogger(classname, MessageKeys.MESSAGES);

  @VisibleForTesting
  static final String CACHE_NAME = "CompiledJs";

  private final DefaultJsCompiler defaultCompiler;
  private final Cache<String, JsResponse> cache;
  private final List<JSSourceFile> defaultExterns;
  private final String compileLevel;
  private final CompilerOptions compilerOptions;

  @Inject
  public ClosureJsCompiler(DefaultJsCompiler defaultCompiler, CacheProvider cacheProvider,
      @Named("shindig.closure.compile.level") String level) {
    this.cache = cacheProvider.createCache(CACHE_NAME);
    this.defaultCompiler = defaultCompiler;
    List<JSSourceFile> externs = null;
    try {
      externs = Collections.unmodifiableList(CommandLineRunner.getDefaultExterns());
    } catch(IOException e) {
      if (LOG.isLoggable(Level.WARNING)) {
        LOG.log(Level.WARNING, "Unable to load default closure externs: " + e.getMessage(), e);
      }
    }
    defaultExterns = externs;

    compileLevel = level.toLowerCase().trim();
    compilerOptions = defaultCompilerOptions();
  }

  public CompilerOptions defaultCompilerOptions() {
    CompilerOptions result = new CompilerOptions();
    if (compileLevel.equals("advanced")) {
      CompilationLevel.ADVANCED_OPTIMIZATIONS.setOptionsForCompilationLevel(result);
    }
    else if (compileLevel.equals("whitespace_only")) {
      CompilationLevel.WHITESPACE_ONLY.setOptionsForCompilationLevel(result);
    }
    else {
      // If 'none', this complier will not run, @see compile
      CompilationLevel.SIMPLE_OPTIMIZATIONS.setOptionsForCompilationLevel(result);
    }
    return result;
  }

  @VisibleForTesting
  protected CompilerOptions getCompilerOptions(JsUri uri) {
    /*
     * This method gets called many times over the course of a single compilation.
     * Keep the instantiated compiler options unless we need to set SourceMap options
     */
    if (!outputCorrelatedJs()) {
      return compilerOptions;
    }

    CompilerOptions options = defaultCompilerOptions();
    setSourceMapCompilerOptions(options);
    return options;
  }

  protected void setSourceMapCompilerOptions(CompilerOptions options) {
    options.sourceMapOutputPath = "create.out";
    options.sourceMapFormat = SourceMap.Format.DEFAULT;
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
      return cachedResult;
    }

    // Only run actual compiler if necessary.
    CompilerOptions options = getCompilerOptions(jsUri);

    if (!compileLevel.equals("none")) {
      /*
       *  isDebug usually will turn off all compilation, however, setting
       *  isExternExportsEnabled and specifying an export path will keep the
       *  closure compiler on and export the externs for debugging.
       */
      if (!jsUri.isDebug() || options.isExternExportsEnabled()) {
        return doCompile(jsUri, content, externs, cacheKey);
      }
    }

    return doDebug(content, cacheKey);
  }

  protected JsResponse doDebug(Iterable<JsContent> content, String cacheKey) {
    JsResponseBuilder builder = new JsResponseBuilder();
    builder.appendAllJs(content);
    JsResponse result = builder.build();
    cache.addElement(cacheKey, result);
    return result;
  }

  protected JsResponse doCompile(JsUri jsUri, Iterable<JsContent> content, String externs,
      String cacheKey) {
    JsResponseBuilder builder = new JsResponseBuilder();

    CompilerOptions options = getCompilerOptions(jsUri);

    List<JSSourceFile> allExterns = Lists.newArrayList();
    allExterns.add(JSSourceFile.fromCode("externs", externs));
    if (defaultExterns != null) {
      allExterns.addAll(defaultExterns);
    }

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
      // but is less efficient and should perhaps only be used in code
      // profiling.
      SourceMapParser parser = processSourceMap(result, allContent);
      if (parser != null) {
        builder.appendAllJs(parser.mapCompiled(compiled));
      } else {
        return cacheAndReturnErrorResult(builder, cacheKey, HttpResponse.SC_INTERNAL_SERVER_ERROR,
            Lists.newArrayList("Parse error for source map"));
      }
    } else {
      builder.appendJs(compiled, "[compiled]");
    }

    builder.clearExterns().appendRawExtern(result.externExport);

    JsResponse response = builder.build();
    cache.addElement(cacheKey, response);
    return response;
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
   * Pull the source map out of the given closure {@link Result} and construct a
   * {@link SourceMapParser}. This instance can be used to correlate compiled
   * content with originating source.
   *
   * @param result Closure result object with source map
   * @param allInputs All inputs supplied to the compiler, in JsContent form
   * @return Utility to parse the sourcemap
   */
  private SourceMapParser processSourceMap(Result result, List<JsContent> allInputs) {
    StringBuilder sb = new StringBuilder();
    try {
      if (result.sourceMap != null) {
        result.sourceMap.appendTo(sb, "done");
        return SourceMapParser.parse(sb.toString(), allInputs);
      }
    } catch (SourceMapParseException e) { // null response
    } catch (IOException e) { // null response
    }
    return null;
  }

  /**
   * Parser for the string representation of a {@link SourceMap}.
   */
  private static class SourceMapParser {
    /**
     * Default source name for constructed {@link JsContent} entries.
     */
    private static final String DEFAULT_JSSOURCE = "[closure-compiler-synthesized]";

    /**
     * Utility to parse a {@link SourceMap} string.
     */
    private final SourceMapping consumer;

    /**
     * Map of mapping identifier to code components.
     */
    private final Map<String, JsContent> orig;

    private SourceMapParser(SourceMapping consumer, List<JsContent> content) {
      this.consumer = consumer;
      this.orig = Maps.newHashMap();
      for (JsContent js : content) {
        orig.put(js.getSource(), js);
      }
    }

    /**
     * Deconstruct the original javascript content for compiled content.
     *
     * This routine iterates through the mapping at every row-column combination
     * of the mapping in order to generate the original content. It is expected
     * to be a considerably expensive operation.
     *
     * @param compiled the compiled javascript
     * @return {@link JsContent} entries for code fragments belonging to a single source
     */
    public Iterable<JsContent> mapCompiled(String compiled) {
      int row = 1, column = 1; // current row-col being parsed
      StringBuilder codeFragment = new StringBuilder(); // code fragment for a single mapping

      OriginalMapping previousMapping = null, // the row-col mapping at the previous valid position
          currentMapping = null; // the row-col mapping at the current valid position

      ImmutableList.Builder<JsContent> contentEntries = ImmutableList.builder();
      Iterable<String> compiledLines = Splitter.on("\n").split(compiled);
      for (String compiledLine : compiledLines) {
        for (column = 0; column < compiledLine.length(); column++) {
          currentMapping = consumer.getMappingForLine(row, column + 1);
          if (!Objects.equal(getSource(currentMapping), getSource(previousMapping))) {
            contentEntries.add(getJsContent(codeFragment.toString(), getSource(previousMapping)));
            codeFragment = new StringBuilder();
          }
          previousMapping = currentMapping;
          codeFragment.append(compiledLine.charAt(column));
        }
        row++;
        codeFragment.append('\n');
      }

      // add the last fragment
      codeFragment.deleteCharAt(codeFragment.length() - 1);
      if (codeFragment.length() > 0) {
        contentEntries.add(getJsContent(codeFragment.toString(), getSource(previousMapping)));
      }
      return contentEntries.build();
    }

    /**
     * Utility to get the source of an {@link OriginalMapping}.
     *
     * @param mapping the mapping
     * @return source of the mapping or a blank source if none is present
     */
    private final String getSource(OriginalMapping mapping) {
      return (mapping != null) ? mapping.getOriginalFile() : "";
    }

    /**
     * Construct {@link JsContent} instances for a given compiled code
     * component.
     *
     * @param codeFragment the fragment of compiled code for this component
     * @param mappingIdentifier positional mapping identifier
     * @return {@link JsContent} for this component
     */
    private JsContent getJsContent(String codeFragment, String mappingIdentifier) {
      JsContent sourceJs = orig.get(getRootSrc(mappingIdentifier));
      String sourceName = DEFAULT_JSSOURCE;
      FeatureBundle bundle = null;
      if (sourceJs != null) {
        sourceName = sourceJs.getSource() != null ? sourceJs.getSource() : "";
        bundle = sourceJs.getFeatureBundle();
      }
      return JsContent.fromFeature(codeFragment, sourceName, bundle, null);
    }

    /**
     * Parse the provided string and return an instance of this parser.
     *
     * @param string the {@link SourceMap} in a string representation
     * @param originalContent the origoinal content
     * @return parsing utility
     * @throws SourceMapParseException
     */
    public static SourceMapParser parse(String string, List<JsContent> originalContent)
        throws SourceMapParseException {
      return new SourceMapParser(SourceMapConsumerFactory.parse(string), originalContent);
    }
  }
}
