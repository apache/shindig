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

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.shindig.common.cache.Cache;
import org.apache.shindig.common.cache.CacheProvider;
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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.javascript.jscomp.BasicErrorManager;
import com.google.javascript.jscomp.CheckLevel;
import com.google.javascript.jscomp.CommandLineRunner;
import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.ErrorManager;
import com.google.javascript.jscomp.JSError;
import com.google.javascript.jscomp.Result;
import com.google.javascript.jscomp.SourceFile;

@Singleton
public class ClosureJsCompiler implements JsCompiler {
  // Default stack size for the compiler threads. The value was copied from closure compiler class.
  private static final long DEFAULT_COMPILER_STACK_SIZE = 1048576L;

  // Based on Closure Library's goog.exportSymbol implementation.
  private static final JsContent EXPORTSYMBOL_CODE =
      JsContent.fromText("var goog=goog||{};goog.exportSymbol=function(name,obj){"
              + "var parts=name.split('.'),cur=window,part;"
              + "for(;parts.length&&(part=parts.shift());){if(!parts.length){"
              + "cur[part]=obj;}else{cur=cur[part]||(cur[part]={})}}};", "[goog.exportSymbol]");

  //class name for logging purpose
  private static final String classname = ClosureJsCompiler.class.getName();
  private static final Logger LOG = Logger.getLogger(classname);

  @VisibleForTesting
  static final String CACHE_NAME = "CompiledJs";

  private final DefaultJsCompiler defaultCompiler;
  private final Cache<String, CompileResult> cache;
  private final List<SourceFile> defaultExterns;
  private final String compileLevel;
  private final Map<String, Future<CompileResult>> compiling;

  private int threadPoolSize = 5;
  private long compilerStackSize = DEFAULT_COMPILER_STACK_SIZE;
  private ExecutorService compilerPool;

  @Inject
  public ClosureJsCompiler(DefaultJsCompiler defaultCompiler, CacheProvider cacheProvider,
          @Named("shindig.closure.compile.level") String level) {
    this(defaultCompiler, cacheProvider, level, null);
  }
  
  public ClosureJsCompiler(DefaultJsCompiler defaultCompiler, CacheProvider cacheProvider,
          String level, ExecutorService executorService) {
    this.cache = cacheProvider.createCache(CACHE_NAME);
    this.defaultCompiler = defaultCompiler;
    List<SourceFile> externs = null;
    try {
      externs = Collections.unmodifiableList(CommandLineRunner.getDefaultExterns());
    } catch(IOException e) {
      if (LOG.isLoggable(Level.WARNING)) {
        LOG.log(Level.WARNING, "Unable to load default closure externs: " + e.getMessage(), e);
      }
    }
    defaultExterns = externs;

    compileLevel = level.toLowerCase().trim();
    if(executorService != null) {
      compilerPool = executorService;
    }else {
      compilerPool = createThreadPool();
    }
    Map<String, Future<CompileResult>> map = Maps.newHashMap();
    compiling = new ConcurrentHashMap<String, Future<CompileResult>>(map);
  }

  @Inject(optional = true)
  public void setThreadPoolSize(
      @Named("shindig.closure.compile.threadPoolSize") Integer threadPoolSize) {

    if (threadPoolSize != null && threadPoolSize != this.threadPoolSize) {
      ExecutorService compilerPool = this.compilerPool;

      this.threadPoolSize = threadPoolSize;
      this.compilerPool = createThreadPool();

      compilerPool.shutdown();
    }
  }

  @Inject(optional = true)
  public void setCompilerStackSize(
      @Named("shindig.closure.compile.compilerStackSize") Long compilerStackSize) {
    if (compilerStackSize > 0L) {
      this.compilerStackSize = compilerStackSize;
    }
  }

  /**
   * Override this to provide your own {@link ExecutorService}
   *
   * @return An {@link ExecutorService} to use for the compiler pool.
   */
  protected ExecutorService createThreadPool() {
    ThreadFactory threadFactory = new ClosureJSThreadFactory();
    return Executors.newFixedThreadPool(threadPoolSize, threadFactory);
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
    CompilerOptions options = defaultCompilerOptions();
    return options;
  }

  public JsResponse compile(JsUri jsUri, Iterable<JsContent> content, String externs) {
    JsResponseBuilder builder = new JsResponseBuilder();

    CompilerOptions options = getCompilerOptions(jsUri);
    StringBuilder compiled = new StringBuilder();
    StringBuilder exports = new StringBuilder();
    boolean useExterns = compileLevel.equals("advanced");
    if (!useExterns) {
      /*
       * Kicking the can down the road.  Advanced optimizations doesn't currently work with the closure compiler in shindig.
       * When it's fixed, we need to make sure all externs are included (not just externs for what was requested) otherwise
       * the cache key will fluctuate with the url hit, and we will get massive cache churn and possible DDOS scenarios
       * when we recompile all requested modules on the fly because the cache key was different.
       */
      externs = "";
    }

    // Add externs export to the list if set in options.
    if (options.isExternExportsEnabled()) {
      List<JsContent> allContent = Lists.newLinkedList(content);
      allContent.add(EXPORTSYMBOL_CODE);
      content = allContent;
    }

    try {
      List<Future<CompileResult>> futures = Lists.newLinkedList();

      // Process each content for work
      for (JsContent code : content) {
        JsResponse defaultCompiled = defaultCompiler.compile(jsUri, Lists.newArrayList(code), externs);

        Future<CompileResult> future = null;
        boolean compile = !code.isNoCompile() && !compileLevel.equals("none");
        /*
         *  isDebug usually will turn off all compilation, however, setting
         *  isExternExportsEnabled and specifying an export path will keep the
         *  closure compiler on and export the externs for debugging.
         */
        compile = compile && (!jsUri.isDebug() || options.isExternExportsEnabled());
        if (compile) { // We should compile this code segment.
          String cacheKey = makeCacheKey(defaultCompiled.toJsString(), externs, jsUri, options);

          synchronized (compiling) {
            CompileResult cached = cache.getElement(cacheKey);
            if (cached == null) {
              future = compiling.get(cacheKey);
              if (future == null) {
                // Don't pound on the compiler. Let the first thread queue the work,
                // the rest of them will just wait on the futures later.
                future = getCompileFuture(cacheKey, code, jsUri, externs);
                compiling.put(cacheKey, future);
              }
            } else {
              future = Futures.immediateFuture(cached);
            }
          }
        }

        if (future == null) {
          future = Futures.immediateFuture(new CompileResult(code.get()));
        }
        futures.add(future);
      }

      // Wait on all work to be done.
      for (Future<CompileResult> future : futures) {
        CompileResult result = future.get();
        compiled.append(result.getContent());
        if (useExterns) {
          String export = result.getExternExport();
          if (export != null) {
            exports.append(export);
          }
        }
      }

    } catch (Exception e) {
      if (LOG.isLoggable(Level.WARNING)) {
        LOG.log(Level.WARNING, e.getMessage(), e);
      }
      Throwable cause = e.getCause();
      if (cause instanceof CompilerException) {
        return returnErrorResult(builder, HttpResponse.SC_NOT_FOUND, ((CompilerException)cause).getErrors());
      } else {
        return returnErrorResult(builder, HttpResponse.SC_NOT_FOUND, Lists.newArrayList(e.getMessage()));
      }
    }

    builder.appendJs(compiled.toString(), "[compiled]");
    builder.clearExterns().appendRawExtern(exports.toString());
    return builder.build();
  }

  protected Future<CompileResult> getCompileFuture(final String cacheKey, final JsContent content,
      final JsUri jsUri, final String externs) {

    return compilerPool.submit(new Callable<CompileResult>() {
      @Override
      public CompileResult call() throws Exception {
        // Create the options anew. Passing in the parent options, even cloning it, is not thread safe.
        CompileResult result = doCompileContent(content, getCompilerOptions(jsUri), buildExterns(externs));
        synchronized (compiling) {
          // Other threads should pick this up in the cache now.
          cache.addElement(cacheKey, result);
          compiling.remove(cacheKey);
        }

        return result;
      }
    });
  }

  protected CompileResult doCompileContent(JsContent content, CompilerOptions options,
      List<SourceFile> externs) throws CompilerException {

    Compiler compiler = new Compiler(getErrorManager()); // We shouldn't reuse compilers

    // disable JS Closure Compiler internal thread
    compiler.disableThreads();

    SourceFile source = SourceFile.fromCode(content.getSource(), content.get());
    Result result = compiler.compile(externs, Lists.newArrayList(source), options);

    if (result.errors.length > 0) {
      throw new CompilerException(result.errors);
    }

    return new CompileResult(compiler, result);
  }

  protected List<SourceFile> buildExterns(String externs) {
    List<SourceFile> allExterns = Lists.newArrayList();
    allExterns.add(SourceFile.fromCode("externs", externs));
    if (defaultExterns != null) {
      allExterns.addAll(defaultExterns);
    }
    return allExterns;
  }

  private JsResponse returnErrorResult(
      JsResponseBuilder builder, int statusCode, List<String> messages) {
    builder.setStatusCode(statusCode);
    builder.addErrors(messages);
    JsResponse result = builder.build();
    return result;
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
              "goog.exportSymbol('" + StringEscapeUtils.escapeEcmaScript(export) +
              "', " + export + ");\n", "[export-symbol]"));
          prevExport = export;
        }
      }
    }
    return builder;
  }

  protected String makeCacheKey(String code, String externs, JsUri uri, CompilerOptions options) {
    // TODO: include compilation options in the cache key
    return Joiner.on(":").join(
        HashUtil.checksum(code.getBytes()),
        HashUtil.checksum(externs.getBytes()),
        uri.getCompileMode(),
        uri.isDebug(),
        options.isExternExportsEnabled());
  }

  private static ErrorManager getErrorManager() {
    return new BasicErrorManager() {
      @Override
      protected void printSummary() { /* Do nothing */ }
      @Override
      public void println(CheckLevel checkLevel, JSError jsError) { /* Do nothing */ }
    };
  }

  private class CompilerException extends Exception {
    private static final long serialVersionUID = 1L;
    private final JSError[] errors;
    public CompilerException(JSError[] errors) {
      this.errors = errors;
    }

    public List<String> getErrors() {
      ImmutableList.Builder<String> builder = ImmutableList.builder();
      for (JSError error : errors) {
        builder.add(error.toString());
      };

      return builder.build();
    }
  }

  private class ClosureJSThreadFactory implements ThreadFactory {
    public Thread newThread(Runnable runnable) {
      return new Thread(null, runnable, "shindigjscompiler", compilerStackSize);
    }
  }
}
