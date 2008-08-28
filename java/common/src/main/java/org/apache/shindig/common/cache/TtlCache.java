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

import org.apache.shindig.common.util.TimeSource;

/**
 * Cache enforcing a Time-To-Live value atop whatever other base
 * caching characteristics are provided. A minimum and maximum
 * TTL is provided to ensure that TTL values are within reasonable
 * limits. This class contains a Cache but doesn't implement the
 * Cache interface due to the necessity of changing its addElement()
 * signature. If needed, however, it could implement addElement without
 * an explicitly provided object lifetime by using minTtl in that case.
 * @param <K> Key for the cache.
 * @param <V> Value the cache stores by Key.
 */
public class TtlCache<K, V> {
  private final Cache<K, TimeoutPair<V>> baseCache;
  private final long minTtl;
  private final long maxTtl;
  private TimeSource timeSource;
  
  /**
   * Create a new TtlCache with the given capacity and TTL values.
   * The cache provider provides an implementation of the actual storage.
   * @param cacheProvider Creator of the actual cache.
   * @param capacity Size of the underlying cache.
   * @param minTtl Minimum amount of time a given entry must stay in the cache, in millis.
   * @param maxTtl Maximum amount of time a given entry can stay in the cache, in millis.
   */
  public TtlCache(CacheProvider cacheProvider, int capacity, long minTtl, long maxTtl) {
	this.baseCache = cacheProvider.createCache(capacity);
	this.minTtl = minTtl;
	this.maxTtl = maxTtl;
	this.timeSource = new TimeSource();
  }
  
  /**
   * Retrieve an element from the cache by key. If there is no such element
   * for that key in the cache, or if the element has timed out, null is returned.
   * @param key Key whose element to look up.
   * @return Element in the cache, if present and not timed out.
   */
  public V getElement(K key) {
	return getElementMaybeRemove(key, false);
  }
  
  /**
   * Add an element to the cache, with the intended amount of time
   * it should live in the cache provided in milliseconds. If below
   * minTtl, minTtl is used. If above maxTtl, maxTtl is used.
   * @param key Element key.
   * @param val Cached element value.
   * @param lifetime Intended lifetime, in millis, of the element's entry.
   */
  public void addElement(K key, V val, long lifetime) {
	long now = timeSource.currentTimeMillis();
    long expiration = lifetime;
    expiration = Math.max(now + minTtl, Math.min(now + maxTtl, expiration));
	TimeoutPair<V> entry = new TimeoutPair<V>(val, expiration);
	synchronized(baseCache) {
  	  baseCache.addElement(key, entry);
	}
  }
  
  /**
   * Removes element for the given key from the cache. Returns it if
   * it hasn't yet expired.
   * @param key Element key.
   * @return Element value.
   */
  public V removeElement(K key) {
	return getElementMaybeRemove(key, true);
  }
  
  /**
   * Set a new time source. Used for testing, so package-private.
   * @param timeSource New time source to use.
   */
  void setTimeSource(TimeSource timeSource) {
	this.timeSource = timeSource;
  }
  
  private V getElementMaybeRemove(K key, boolean remove) {
	TimeoutPair<V> entry = null;
	if (remove) {
	  entry = baseCache.removeElement(key);
	} else {
	  entry = baseCache.getElement(key);
	}
	if (entry == null) {
	  return null;
	}

	long now = timeSource.currentTimeMillis();
	if (now < entry.expiration) {
      // Not yet timed out. Still valid, so return.
	  return entry.cachedObj;
	}
		
	// No need to clean up the cache - that happens under the covers.
	return null;
  }
  
  /**
   * Actual stored content in the cache. Saves the cached object along
   * with its expiration date (in ms).
   * @param <V> Type of stored object.
   */
  private static final class TimeoutPair<V> {
	private V cachedObj;
	private long expiration;

	private TimeoutPair(V cachedObj, long expiration) {
	  this.cachedObj = cachedObj;
	  this.expiration = expiration;
	}
  }
}
