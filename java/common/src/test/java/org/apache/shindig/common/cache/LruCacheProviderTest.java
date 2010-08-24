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
package org.apache.shindig.common.cache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.name.Names;

import org.junit.Test;

public class LruCacheProviderTest {

  private LruCache<Object, Object> getCache(CacheProvider provider, String name) {
    Cache<Object, Object> base = provider.createCache(name);
    return (LruCache<Object, Object>)base;
  }

  @Test
  public void defaultCapacityForNamedCache() throws Exception {
    LruCacheProvider provider = new LruCacheProvider(10);
    assertEquals(10, getCache(provider, "foo").capacity);
  }

  LruCacheProvider createProvider(final String name, final String capacity, int defaultCapacity) {
    Module module = new AbstractModule() {
      @Override
      public void configure() {
        binder().bindConstant()
            .annotatedWith(Names.named("shindig.cache.lru." + name + ".capacity"))
            .to(capacity);
      }
    };

    Injector injector = Guice.createInjector(module);

    return new LruCacheProvider(injector, defaultCapacity);
  }

  @Test
  public void configuredMultipleCalls() throws Exception {
    LruCacheProvider provider = createProvider("foo", "100", 10);
    assertSame(getCache(provider, "foo"), getCache(provider, "foo"));
  }

  @Test
  public void configuredCapacity() throws Exception {
    LruCacheProvider provider = createProvider("foo", "100", 10);
    assertEquals(100, getCache(provider, "foo").capacity);
  }

  @Test
  public void missingConfiguredCapacity() throws Exception {
    LruCacheProvider provider = createProvider("foo", "100", 10);
    assertEquals(10, getCache(provider, "bar").capacity);
  }

  @Test
  public void malformedConfiguredCapacity() throws Exception {
    LruCacheProvider provider = createProvider("foo", "adfdf", 10);
    assertEquals(10, getCache(provider, "foo").capacity);
  }

}
