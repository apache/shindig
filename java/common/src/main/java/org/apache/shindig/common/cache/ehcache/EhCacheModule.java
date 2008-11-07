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
package org.apache.shindig.common.cache.ehcache;

import org.apache.shindig.common.cache.CacheProvider;
import org.apache.shindig.common.util.ResourceLoader;

import com.google.inject.AbstractModule;
import com.google.inject.CreationException;
import com.google.inject.Scopes;
import com.google.inject.name.Names;
import com.google.inject.spi.Message;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;

/**
 * Creates a module to supply a EhCache Provider
 */
public class EhCacheModule extends AbstractModule {
  private final Properties properties;
  private final static String DEFAULT_PROPERTIES = "ehcache.properties";

  /** {@inheritDoc} */
  @Override
  protected void configure() {
    Names.bindProperties(this.binder(), properties);
    bind(CacheProvider.class).to(EhCacheCacheProvider.class).in(Scopes.SINGLETON);
  }

  public EhCacheModule(Properties properties) {
    this.properties = properties;
  }

  /**
   * Creates module with standard properties.
   */
  public EhCacheModule() {
    Properties properties = null;
    try {
      InputStream is = ResourceLoader.openResource(DEFAULT_PROPERTIES);
      properties = new Properties();
      properties.load(is);
    } catch (IOException e) {
      throw new CreationException(Arrays.asList(new Message("Unable to load properties: "
          + DEFAULT_PROPERTIES)));
    }
    this.properties = properties;
  }
}
