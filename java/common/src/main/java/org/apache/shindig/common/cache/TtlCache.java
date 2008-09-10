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
 * limits. Objects are not forced out of the cache after their TTL;
 * they're simply treated as invalid, unless "stale" entries are
 * explicitly requested.
 * 
 * Two sets of APIs are supported:
 * 1. The standard Cache interface. addElement() adds to the cache
 * with a particular default lifetime, which may be set separately. Without
 * being overridden, its default value is 0 (no TTL), which applies subject
 * to minTtl and maxTtl configured for the cache. getElement() retrieves
 * an element from the cache subject to configured expiration.
 * 2. Extended TTL and expired-object caching interfaces. An addElement
 * method is provided with a requested expiration for the object, which is
 * in turn subject to minTtl and maxTtl restrictions as configured for the
 * cache. Method getElementWithExpiration is provided which returns the cached object
 * along with whether or not it is expired.
 * @param <K> Type of key for the cache.
 * @param <V> Type of value the cache stores by Key.
 */
public class TtlCache<K, V> implements Cache<K, V> {
  private final Cache<K, TimeoutPair<V>> baseCache;
  private final long minTtl;
  private final long maxTtl;
  private TimeSource timeSource;
  private long defaultLifetime;
  
  private static final long DEFAULT_LIFETIME_MILLIS = 0;
  
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
    this.defaultLifetime = DEFAULT_LIFETIME_MILLIS;
  }
  
  /**
   * Sets the default lifetime of a given element added to the cache (using the standard
   * addElement method), in milliseconds.
   * @param defaultLifetime
   */
  public void setDefaultLifetimeMillis(long defaultLifetime) {
    this.defaultLifetime = defaultLifetime;
  }
  
  /**
   * Retrieve an element from the cache by key. If there is no such element
   * for that key in the cache, or if the element has timed out, null is returned.
   * @param key Key whose element to look up.
   * @return Element in the cache, if present and not timed out.
   */
  public V getElement(K key) {
    CachedObject<V> cached = getElementMaybeRemove(key, false);
    
    if (!cached.isExpired) {
      return cached.obj;
    }
    
    return null;
  }
  
  /**
   * Retrieve an element from the cache along with whether or not it is
   * expired. A "stale" element may be used by calling code if it so
   * chooses, ie. if it's unable to pull a "fresh" version of content.
   * @param key Key whose element to look up.
   * @return Pair of cached element and whether or not it is expired.
   */
  public CachedObject<V> getElementWithExpiration(K key) {
    return getElementMaybeRemove(key, false);
  }
  
  /**
   * Add an element to the cache, with the intended amount of time
   * it should live in the cache provided in milliseconds. If below
   * minTtl, minTtl is used. If above maxTtl, maxTtl is used.
   * @param key Element key.
   * @param val Element value to cache.
   * @param expiration Time, in millis, that the value is to expire.
   */
  public void addElement(K key, V val, long expiration) {
    long now = timeSource.currentTimeMillis();
    expiration = Math.max(now + minTtl, Math.min(now + maxTtl, expiration));
    TimeoutPair<V> entry = new TimeoutPair<V>(val, expiration);
    synchronized(baseCache) {
      baseCache.addElement(key, entry);
    }
  }
  
  /**
   * Add an element to the cache, with lifetime set to the default configured
   * for this cache object.
   * @param key Element key.
   * @param val Element value to cache.
   */
  public void addElement(K key, V val) {
    addElement(key, val, defaultLifetime);
  }
  
  /**
   * Removes element for the given key from the cache. Returns it if
   * it hasn't yet expired.
   * @param key Element key.
   * @return Element value.
   */
  public V removeElement(K key) {
    CachedObject<V> cached = getElementMaybeRemove(key, true);
    
    if (!cached.isExpired) {
      return cached.obj;
    }
    
    return null;
  } 
  
  /**
   * Set a new time source. Used for testing, so package-private.
   * @param timeSource New time source to use.
   */
  void setTimeSource(TimeSource timeSource) {
    this.timeSource = timeSource;
  }
  
  private CachedObject<V> getElementMaybeRemove(K key, boolean remove) {
    TimeoutPair<V> entry = null;
    
    if (remove) {
      entry = baseCache.removeElement(key);
    } else {
      entry = baseCache.getElement(key);
	  }
	  if (entry == null) {
	    return new CachedObject<V>((V)null, true);
	  }

	  long now = timeSource.currentTimeMillis();
	  
	  return new CachedObject<V>(entry.cachedObj, now >= entry.expiration);
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
  
  public static class CachedObject<V> {
    public V obj;
    public boolean isExpired;
    
    private CachedObject(V obj, boolean isExpired) {
      this.obj = obj;
      this.isExpired = isExpired;
    }
  }
}
