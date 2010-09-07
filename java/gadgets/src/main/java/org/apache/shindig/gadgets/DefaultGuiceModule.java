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

import com.google.common.collect.ImmutableList;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

import org.apache.commons.lang.StringUtils;

import org.apache.shindig.gadgets.config.ConfigContributor;
import org.apache.shindig.gadgets.config.CoreUtilConfigContributor;
import org.apache.shindig.gadgets.config.OsapiServicesConfigContributor;
import org.apache.shindig.gadgets.config.ShindigAuthConfigContributor;
import org.apache.shindig.gadgets.config.XhrwrapperConfigContributor;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.InvalidationHandler;
import org.apache.shindig.gadgets.parse.ParseModule;
import org.apache.shindig.gadgets.preload.PreloadModule;
import org.apache.shindig.gadgets.render.RenderModule;
import org.apache.shindig.gadgets.rewrite.RewriteModule;
import org.apache.shindig.gadgets.servlet.GadgetsHandler;
import org.apache.shindig.gadgets.servlet.HttpRequestHandler;
import org.apache.shindig.gadgets.templates.TemplateModule;
import org.apache.shindig.gadgets.uri.UriModule;

import org.apache.shindig.gadgets.variables.SubstituterModule;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

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

    final ExecutorService service = Executors.newCachedThreadPool(DAEMON_THREAD_FACTORY);
    bind(ExecutorService.class).toInstance(service);
    bind(ExecutorService.class).annotatedWith(Names.named("shindig.concat.executor")).toInstance(service);

    Runtime.getRuntime().addShutdownHook(new Thread() {
        public void run() {
            service.shutdownNow();
        }
    });

    install(new ParseModule());
    install(new PreloadModule());
    install(new RenderModule());
    install(new RewriteModule());
    install(new SubstituterModule());
    install(new TemplateModule());
    install(new UriModule());

    // bind(Long.class).annotatedWith(Names.named("org.apache.shindig.serviceExpirationDurationMinutes")).toInstance(60l);

    // We perform static injection on HttpResponse for cache TTLs.
    requestStaticInjection(HttpResponse.class);

    registerGadgetHandlers();
    registerConfigContributors();
    registerFeatureHandlers();
  }

  /**
   * Sets up multibinding for rpc handlers
   */
  protected void registerGadgetHandlers() {
    Multibinder<Object> handlerBinder = Multibinder.newSetBinder(binder(), Object.class, Names.named("org.apache.shindig.handlers"));
    handlerBinder.addBinding().to(InvalidationHandler.class);
    handlerBinder.addBinding().to(HttpRequestHandler.class);
    handlerBinder.addBinding().to(GadgetsHandler.class);
  }

  protected void registerConfigContributors() {
    MapBinder<String, ConfigContributor> configBinder = MapBinder.newMapBinder(binder(), String.class, ConfigContributor.class);
    configBinder.addBinding("core.util").to(CoreUtilConfigContributor.class);
    configBinder.addBinding("osapi").to(OsapiServicesConfigContributor.class);
    configBinder.addBinding("shindig.auth").to(ShindigAuthConfigContributor.class);
    configBinder.addBinding("shindig.xhrwrapper").to(XhrwrapperConfigContributor.class);

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
    return ImmutableList.<String>builder().addAll(extended).add(StringUtils.split(features, ',')).build();
  }

  public static final ThreadFactory DAEMON_THREAD_FACTORY =
    new ThreadFactory() {
        private final ThreadFactory factory = Executors.defaultThreadFactory();

        public Thread newThread(Runnable r) {
            Thread t = factory.newThread(r);
            t.setDaemon(true);
            return t;
        }
    };
}
