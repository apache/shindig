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

import org.apache.shindig.common.cache.Cache;
import org.apache.shindig.common.cache.CacheProvider;
import org.apache.shindig.common.cache.ehcache.EhCacheCacheProvider;

import com.google.inject.Guice;
import com.google.inject.Injector;

import org.junit.Test;
import org.junit.Assert;

/**
 *
 */
public class EhCacheCacheProviderTest {

  @Test
  public void getAnonCache() {
    CacheProvider defaultProvider = new EhCacheCacheProvider(
        "/org/apache/shindig/common/cache/ehcache/ehcacheConfig.xml", "true");
    Cache<String, String> cache = defaultProvider.createCache(100);
    Assert.assertNotNull(cache);
    Assert.assertNull(cache.getElement("test"));
    cache.addElement("test", "value1");
    Assert.assertEquals(cache.getElement("test"), "value1");
    cache.removeElement("test");
    Assert.assertNull(cache.getElement("test"));

  }

  @Test
  public void getNamedCache() {
    CacheProvider defaultProvider = new EhCacheCacheProvider(
        "/org/apache/shindig/common/cache/ehcache/ehcacheConfig.xml", "true");
    Cache<String, String> cache = defaultProvider.createCache(100, "testcache");
    Cache<String, String> cache2 = defaultProvider.createCache(100, "testcache");
    Assert.assertNotNull(cache);
    Assert.assertEquals(cache, cache2);
    Assert.assertNull(cache.getElement("test"));
    cache.addElement("test", "value1");
    Assert.assertEquals(cache.getElement("test"), "value1");
    cache.removeElement("test");
    Assert.assertNull(cache.getElement("test"));
  }

  @Test
  public void getAnonCacheNoConfig() {
    CacheProvider defaultProvider = new EhCacheCacheProvider();
    Cache<String, String> cache = defaultProvider.createCache(100);
    Assert.assertNotNull(cache);
    Assert.assertNull(cache.getElement("test"));
    cache.addElement("test", "value1");
    Assert.assertEquals(cache.getElement("test"), "value1");
    cache.removeElement("test");
    Assert.assertNull(cache.getElement("test"));

  }

  @Test
  public void getNamedCacheNoConfig() {
    CacheProvider defaultProvider = new EhCacheCacheProvider();
    Cache<String, String> cache = defaultProvider.createCache(100, "testcache");
    Cache<String, String> cache2 = defaultProvider.createCache(100, "testcache");
    Assert.assertNotNull(cache);
    Assert.assertEquals(cache, cache2);
    Assert.assertNull(cache.getElement("test"));
    cache.addElement("test", "value1");
    Assert.assertEquals(cache.getElement("test"), "value1");
    cache.removeElement("test");
    Assert.assertNull(cache.getElement("test"));
  }

  @Test
  public void testGuiceModule() {
    Injector i = Guice.createInjector(new EhCacheGuiceCacheModule());
    CacheProvider cacheProvider = i.getInstance(CacheProvider.class);
    Cache<String, String> cache = cacheProvider.createCache(100, "testcache");
    Cache<String, String> cache2 = cacheProvider.createCache(100, "testcache");
    Assert.assertNotNull(cache);
    Assert.assertEquals(cache, cache2);
    Assert.assertNull(cache.getElement("test"));
    cache.addElement("test", "value1");
    Assert.assertEquals(cache.getElement("test"), "value1");
    cache.removeElement("test");
    Assert.assertNull(cache.getElement("test"));
  }
}
