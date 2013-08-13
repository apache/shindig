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

import com.google.common.collect.MapMaker;

import java.util.concurrent.ConcurrentMap;

/**
 * A cache that uses a soft expiration policy. Entries will be kept around for potentially as long
 * as the underlying cache permits, but we keep a timestamp around to retain a notion of the actual
 * age. This provides users of this class with the option of keeping an "expired" entry beyond the
 * normal lifetime.
 *
 * As soon as an entry expires from the underlying cache, it disappears from here as well.
 *
 * Note that this isn't actually a cache itself, but rather a wrapper for one. It differs in the
 * getElement method substantially, since the returned objects are not the same as the V parameter.
 */
public class SoftExpiringCache<K, V> {
  private final Cache<K, V> cache;

  // We keep a weak reference to the value stored in the cache so that when the value in the actual
  // cache is removed, we should lose it here as well.
  private final ConcurrentMap<V, Long> expirationTimes;
  private TimeSource timeSource;

  /**
   * Create a new TtlCache with the given capacity and TTL values.
   * The cache provider provides an implementation of the actual storage.
   *
   * @param cache The underlying cache that will store actual data.
   */
  public SoftExpiringCache(Cache<K, V>  cache) {
    this.cache = cache;
    expirationTimes = new MapMaker().weakKeys().makeMap();
    timeSource = new TimeSource();
  }

  /**
   * Retrieve an element from the cache by key. If there is no such element
   * for that key in the cache, or if the element has timed out, null is returned.
   * @param key Key whose element to look up.
   * @return Element in the cache, if present and not timed out.
   */
  public CachedObject<V> getElement(K key) {
    V value = cache.getElement(key);
    if (value == null) {
      return null;
    }

    Long expiration = expirationTimes.get(value);

    if (expiration == null) {
      return null;
    }

    return new CachedObject<V>(value, expiration < timeSource.currentTimeMillis());
  }

  /**
   * Add an element to the cache, with the intended max age for its cache entry provided in
   * milliseconds.
   *
   * @param key The key to store the entry for.
   * @param value The value to store.
   * @param maxAge The maximum age for this entry before it is deemed expired.
   */
  public void addElement(K key, V value, long maxAge) {
    long now = timeSource.currentTimeMillis();
    cache.addElement(key, value);
    expirationTimes.put(value, now + maxAge);
  }

  /**
   * Set a new time source. For use in testing.
   *
   * @param timeSource New time source to use.
   */
  public void setTimeSource(TimeSource timeSource) {
    this.timeSource = timeSource;
  }

  public static class CachedObject<V> {
    public final V obj;
    public final boolean isExpired;

    protected CachedObject(V obj, boolean isExpired) {
      this.obj = obj;
      this.isExpired = isExpired;
    }
  }
}
