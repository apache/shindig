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

import com.google.inject.AbstractModule;
import com.google.inject.CreationException;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.google.inject.spi.Message;
import org.apache.shindig.gadgets.oauth.OAuthFetcherFactory;
import org.apache.shindig.social.GadgetDataHandler;
import org.apache.shindig.social.opensocial.ActivitiesService;
import org.apache.shindig.social.opensocial.DataService;
import org.apache.shindig.social.opensocial.OpenSocialDataHandler;
import org.apache.shindig.social.opensocial.PeopleService;
import org.apache.shindig.social.samplecontainer.BasicActivitiesService;
import org.apache.shindig.social.samplecontainer.BasicDataService;
import org.apache.shindig.social.samplecontainer.BasicPeopleService;
import org.apache.shindig.social.samplecontainer.StateFileDataHandler;
import org.apache.shindig.util.ResourceLoader;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Creates a module to supply all of the Basic* classes
 */
public class DefaultGuiceModule extends AbstractModule {
  private final Properties properties;
  private final static String DEFAULT_PROPERTIES = "gadgets.properties";

  /** {@inheritDoc} */
  @Override
  protected void configure() {
    Names.bindProperties(this.binder(), properties);

    bind(RemoteContentFetcher.class).to(BasicRemoteContentFetcher.class);

    bind(RemoteContentFetcher.class)
        .annotatedWith(GadgetSpecFetcher.class)
        .to(CachedContentFetcher.class);
    bind(RemoteContentFetcher.class)
        .annotatedWith(MessageBundleFetcher.class)
        .to(CachedContentFetcher.class);

    bind(GadgetBlacklist.class).to(BasicGadgetBlacklist.class);
    bind(GadgetTokenDecoder.class).to(BasicGadgetTokenDecoder.class);
    bind(SigningFetcherFactory.class);
    bind(OAuthFetcherFactory.class);
    bind(Executor.class).toInstance(Executors.newCachedThreadPool());

    bind(ContainerConfig.class).in(Scopes.SINGLETON);
    bind(GadgetFeatureRegistry.class).in(Scopes.SINGLETON);
    bind(GadgetServer.class).in(Scopes.SINGLETON);

    // Social guice
    bind(PeopleService.class).to(BasicPeopleService.class);
    bind(DataService.class).to(BasicDataService.class);
    bind(ActivitiesService.class).to(BasicActivitiesService.class);

    bind(new TypeLiteral<List<GadgetDataHandler>>() {})
        .toProvider(GadgetDataHandlersProvider.class);

  }

  public DefaultGuiceModule(Properties properties) {
    this.properties = properties;
  }

  /**
   * Creates module with standard properties.
   */
  public DefaultGuiceModule() {
    Properties properties = null;
    try {
      InputStream is = ResourceLoader.openResource(DEFAULT_PROPERTIES);
      properties = new Properties();
      properties.load(is);
    } catch (IOException e) {
      throw new CreationException(Arrays.asList(
          new Message("Unable to load properties: " + DEFAULT_PROPERTIES)));
    }
    this.properties = properties;
  }

  public static class GadgetDataHandlersProvider
      implements Provider<List<GadgetDataHandler>> {
    List<GadgetDataHandler> handlers;

    @Inject
    public GadgetDataHandlersProvider(OpenSocialDataHandler
        openSocialDataHandler, StateFileDataHandler stateFileHandler) {
      handlers = new ArrayList<GadgetDataHandler>();
      handlers.add(openSocialDataHandler);
      handlers.add(stateFileHandler);
    }

    public List<GadgetDataHandler> get() {
      return handlers;
    }
  }
}
