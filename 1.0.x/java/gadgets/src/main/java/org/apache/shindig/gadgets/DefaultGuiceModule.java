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
package org.apache.shindig.gadgets;

import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.parse.ParseModule;
import org.apache.shindig.gadgets.preload.HttpPreloader;
import org.apache.shindig.gadgets.preload.Preloader;
import org.apache.shindig.gadgets.render.RenderingContentRewriter;
import org.apache.shindig.gadgets.rewrite.ContentRewriter;
import org.apache.shindig.gadgets.rewrite.lexer.DefaultContentRewriter;
import org.apache.shindig.gadgets.servlet.CajaContentRewriter;

import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Creates a module to supply all of the Basic* classes
 */
public class DefaultGuiceModule extends AbstractModule {

  /** {@inheritDoc} */
  @Override
  protected void configure() {

    ExecutorService service = Executors.newCachedThreadPool();
    bind(Executor.class).toInstance(service);
    bind(ExecutorService.class).toInstance(service);

    this.install(new ParseModule());

    bind(new TypeLiteral<List<ContentRewriter>>(){}).toProvider(ContentRewritersProvider.class);
    bind(new TypeLiteral<List<Preloader>>(){}).toProvider(PreloaderProvider.class);

    // We perform static injection on HttpResponse for cache TTLs.
    requestStaticInjection(HttpResponse.class);
  }

  private static class ContentRewritersProvider implements Provider<List<ContentRewriter>> {
    private final List<ContentRewriter> rewriters;

    @Inject
    public ContentRewritersProvider(DefaultContentRewriter optimizingRewriter,
                                    CajaContentRewriter cajaRewriter,
                                    RenderingContentRewriter renderingRewriter) {
      rewriters = Lists.newArrayList();
      rewriters.add(optimizingRewriter);
      rewriters.add(cajaRewriter);
      rewriters.add(renderingRewriter);
    }

    public List<ContentRewriter> get() {
      return rewriters;
    }
  }

  private static class PreloaderProvider implements Provider<List<Preloader>> {
    private final List<Preloader> preloaders;

    @Inject
    public PreloaderProvider(HttpPreloader httpPreloader) {
      preloaders = Lists.<Preloader>newArrayList(httpPreloader);
    }

    public List<Preloader> get() {
      return preloaders;
    }
  }
}
