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

import org.apache.shindig.common.util.ResourceLoader;
import org.apache.shindig.gadgets.http.BasicHttpCache;
import org.apache.shindig.gadgets.http.BasicHttpFetcher;
import org.apache.shindig.gadgets.http.HttpCache;
import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.gadgets.http.ContentFetcherFactory;
import org.apache.shindig.gadgets.http.RemoteContentFetcherFactory;
import org.apache.shindig.gadgets.oauth.OAuthFetcherFactory;

import com.google.inject.AbstractModule;
import com.google.inject.CreationException;
import com.google.inject.Scopes;
import com.google.inject.name.Names;
import com.google.inject.spi.Message;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
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

    bind(HttpFetcher.class).to(BasicHttpFetcher.class);
    bind(HttpCache.class).to(BasicHttpCache.class);

    bind(RemoteContentFetcherFactory.class);
    bind(SigningFetcherFactory.class);
    // Needed becuase OAuth fetcher factory fetches its config
    bind(HttpFetcher.class)
        .annotatedWith(OAuthFetcherFactory.OAuthConfigFetcher.class)
        .to(BasicHttpFetcher.class);
    bind(OAuthFetcherFactory.class);
    bind(ContentFetcherFactory.class);

    bind(HttpFetcher.class)
        .annotatedWith(GadgetSpecFetcher.class)
        .toProvider(ContentFetcherFactory.class);
    bind(HttpFetcher.class)
        .annotatedWith(MessageBundleFetcher.class)
        .toProvider(ContentFetcherFactory.class);

    bind(GadgetBlacklist.class).to(BasicGadgetBlacklist.class);
    bind(Executor.class).toInstance(Executors.newCachedThreadPool());
    bind(LockedDomainService.class).to(HashLockedDomainService.class);

    bind(ContainerConfig.class).in(Scopes.SINGLETON);
    bind(GadgetFeatureRegistry.class).in(Scopes.SINGLETON);
    bind(GadgetServer.class).in(Scopes.SINGLETON);
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
}
