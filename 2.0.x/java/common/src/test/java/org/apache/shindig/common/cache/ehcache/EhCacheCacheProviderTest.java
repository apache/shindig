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

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 */
public class EhCacheCacheProviderTest {

  static CacheProvider defaultProvider;
  @BeforeClass
  public static void setup() throws Exception {
    defaultProvider = new EhCacheCacheProvider(
        "res://org/apache/shindig/common/cache/ehcache/ehcacheConfig.xml", true, true);
  }

  @Test
  public void getNamedCache() throws Exception {
    Cache<String, String> cache = defaultProvider.createCache("testcache");
    Cache<String, String> cache2 = defaultProvider.createCache("testcache");
    Assert.assertNotNull(cache);
    Assert.assertEquals(cache, cache2);
    Assert.assertNull(cache.getElement("test"));
    cache.addElement("test", "value1");
    Assert.assertEquals("value1", cache.getElement("test"));
    cache.removeElement("test");
    Assert.assertNull(cache.getElement("test"));
    Assert.assertEquals(cache.getCapacity(), cache2.getCapacity());
    Assert.assertEquals(cache.getSize(), cache2.getSize());
  }
}
