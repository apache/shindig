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

import com.google.common.base.Preconditions;
import org.apache.shindig.common.cache.Cache;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;


/**
 * Produces a cache configured from ehcache.
 *
 * @param <K> the type of key used to cache elements
 * @param <V> the type of element stored in this cache
 */
public class EhConfiguredCache<K, V> implements Cache<K, V> {

  private net.sf.ehcache.Cache cache;

  /**
   * Create a new EhCache cache with the given name for the given cache manager if one does not
   * already exist.
   *
   * @param cacheName
   *          the name to use for the cache
   * @param cacheManager
   *          the cache manager in which to create the cache
   */
  public EhConfiguredCache(String cacheName, CacheManager cacheManager) {
    synchronized (cacheManager) {
      cache = cacheManager.getCache(Preconditions.checkNotNull(cacheName));
      if (cache == null) {
        cacheManager.addCache(cacheName);
        cache = cacheManager.getCache(cacheName);
        if (cache == null) {
          throw new RuntimeException("Failed to create Cache with name " + cacheName);
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  public void addElement(K key, V value) {
    cache.put(new Element(key, value));
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  public V getElement(K key) {
    Element cacheElement = cache.get(key);
    if (cacheElement != null) {
      return (V) cacheElement.getObjectValue();
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  public V removeElement(K key) {
    Object value = getElement(key);
    cache.remove(key);
    return (V) value;
  }

  /**
   * {@inheritDoc}
   */
  public long getCapacity() {
    // EhCache returns 0 to represent an unbounded cache, where the Cache interface expects -1
    // EhCache also returns 0 when using resource pooling as a count-based capacity does not apply
    long totalCapacity = cache.getCacheConfiguration().getMaxEntriesLocalHeap()
            + cache.getCacheConfiguration().getMaxEntriesLocalDisk();
    return totalCapacity == 0 ? -1 : totalCapacity;
  }

  /**
   * @return The current size of the cache.
   *
   * Note that this does not call getSize on the underlying cache, which is very expensive. This
   * will not include the size of remote caches.
   */
  public long getSize() {
    return cache.getMemoryStoreSize() + cache.getDiskStoreSize();
  }
}
