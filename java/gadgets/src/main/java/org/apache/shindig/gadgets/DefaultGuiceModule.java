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

import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.shindig.common.crypto.BasicBlobCrypter;
import org.apache.shindig.common.servlet.Authority;
import org.apache.shindig.common.servlet.BasicAuthority;
import org.apache.shindig.common.servlet.GuiceServletContextListener;
import org.apache.shindig.common.util.GenericDigestUtils;
import org.apache.shindig.gadgets.config.DefaultConfigContributorModule;
import org.apache.shindig.gadgets.http.AbstractHttpCache;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.InvalidationHandler;
import org.apache.shindig.gadgets.js.JsCompilerModule;
import org.apache.shindig.gadgets.js.JsServingPipelineModule;
import org.apache.shindig.gadgets.parse.ParseModule;
import org.apache.shindig.gadgets.preload.PreloadModule;
import org.apache.shindig.gadgets.render.RenderModule;
import org.apache.shindig.gadgets.rewrite.RewriteModule;
import org.apache.shindig.gadgets.servlet.GadgetsHandler;
import org.apache.shindig.gadgets.servlet.HttpRequestHandler;
import org.apache.shindig.gadgets.templates.TemplateModule;
import org.apache.shindig.gadgets.uri.ProxyUriBase;
import org.apache.shindig.gadgets.uri.UriModule;
import org.apache.shindig.gadgets.variables.SubstituterModule;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

/**
 * Creates a module to supply all of the core gadget classes.
 *
 * Instead of subclassing this consider adding features to the
 * multibindings for features and rpc handlers.
 */
public class DefaultGuiceModule extends AbstractModule {

  /** {@inheritDoc} */
  @Override
  protected void configure() {

    bind(ExecutorService.class).to(ShindigExecutorService.class);
    bind(Executor.class).annotatedWith(Names.named("shindig.concat.executor")).to(ShindigExecutorService.class);

    bind(Authority.class).to(BasicAuthority.class);

    bindConstant().annotatedWith(Names.named("shindig.jsload.ttl-secs")).to(60 * 60); // 1 hour
    bindConstant().annotatedWith(Names.named("shindig.jsload.require-onload-with-jsload")).to(true);

    install(new DefaultConfigContributorModule());
    install(new ParseModule());
    install(new PreloadModule());
    install(new RenderModule());
    install(new RewriteModule());
    install(new SubstituterModule());
    install(new TemplateModule());
    install(new UriModule());
    install(new JsCompilerModule());
    install(new JsServingPipelineModule());

    // We perform static injection on HttpResponse for cache TTLs.
    requestStaticInjection(HttpResponse.class);
    requestStaticInjection(AbstractHttpCache.class);
    requestStaticInjection(ProxyUriBase.class);
    requestStaticInjection(GenericDigestUtils.class);
    requestStaticInjection(BasicBlobCrypter.class);
    registerGadgetHandlers();
    registerFeatureHandlers();
  }

  /**
   * Sets up multibinding for rpc handlers
   */
  protected void registerGadgetHandlers() {
    Multibinder<Object> handlerBinder = Multibinder.newSetBinder(binder(), Object.class,
        Names.named("org.apache.shindig.handlers"));
    handlerBinder.addBinding().to(InvalidationHandler.class);
    handlerBinder.addBinding().to(HttpRequestHandler.class);
    handlerBinder.addBinding().to(GadgetsHandler.class);
  }

  /**
   * Sets up the multibinding for extended feature resources
   */
  protected void registerFeatureHandlers() {
    /*Multibinder<String> featureBinder = */
        Multibinder.newSetBinder(binder(), String.class, Names.named("org.apache.shindig.features-extended"));
  }

  /**
   * Merges the features provided in shindig.properties with the extended features from multibinding
   * @param features Comma separated string from shindig.properties key 'shindig.features.default'
   * @param extended Set of paths/resources from plugins
   * @return the merged, list of all features to load.
   */
  @Provides
  @Singleton
  @Named("org.apache.shindig.features")
  protected List<String> defaultFeatures(@Named("shindig.features.default")String features,
                                         @Named("org.apache.shindig.features-extended")Set<String> extended) {
    return ImmutableList.<String>builder()
        .addAll(Splitter.on(',').split(features))
        .addAll(extended)
        .build();
  }

  /**
   * A thread factory that sets the daemon flag to allow for clean servlet shutdown.
   */
  public static final ThreadFactory DAEMON_THREAD_FACTORY =
    new ThreadFactory() {
        private final ThreadFactory factory = Executors.defaultThreadFactory();

        public Thread newThread(Runnable r) {
            Thread t = factory.newThread(r);
            t.setDaemon(true);
            return t;
        }
    };

  /**
   * An Executor service that mimics Executors.newCachedThreadPool(DAEMON_THREAD_FACTORY);
   * Registers a cleanup handler to shutdown the thread.
   */
  @Singleton
  public static class ShindigExecutorService extends ThreadPoolExecutor implements GuiceServletContextListener.CleanupCapable {
    @Inject
    public ShindigExecutorService(GuiceServletContextListener.CleanupHandler cleanupHandler) {
      super(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS,
          new SynchronousQueue<Runnable>(),
          DAEMON_THREAD_FACTORY);
          cleanupHandler.register(this);
    }

    public void cleanup() {
      this.shutdown();
    }
  }

}
