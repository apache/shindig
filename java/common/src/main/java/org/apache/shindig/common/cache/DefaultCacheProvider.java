/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.common.cache;

import com.google.inject.Singleton;

import java.util.HashMap;
import java.util.Map;

/**
 * A default Cache Manager generates LruCache Cache implementations, where no cache
 * manager is defined this implementation is used.
 */
@Singleton
public class DefaultCacheProvider implements CacheProvider {

  private Map<String, Cache<?, ?>> cacheInstances = new HashMap<String, Cache<?, ?>>();

  /*
   * (non-Javadoc)
   *
   * @see org.apache.shindig.common.cache.CacheProvider#createCache(int)
   */
  public <K, V> Cache<K, V> createCache(int capacity, String name) {
    if (name == null) {
      return new LruCache<K, V>(capacity);
    } else {
      // creation of caches is likely to be rare, so synchronized is Ok
      synchronized (cacheInstances) {
        Cache<K, V> c = (Cache<K, V>) cacheInstances.get(name);
        if (c == null) {
          c = new LruCache<K, V>(capacity);
          cacheInstances.put(name, c);
        }
        return c;    
      }
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.apache.shindig.common.cache.CacheProvider#createCache(int)
   */
  public <K, V> Cache<K, V> createCache(int capacity) {
    return createCache(capacity, null);
  }

}
