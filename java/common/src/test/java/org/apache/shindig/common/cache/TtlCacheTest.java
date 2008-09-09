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

import org.apache.shindig.common.util.FakeTimeSource;

import junit.framework.TestCase;

public class TtlCacheTest extends TestCase {
  private FakeTimeSource timeSource;
  private CacheProvider cacheProvider;
  
  @Override
  public void setUp() throws Exception {
    timeSource = new FakeTimeSource(0);
    cacheProvider = new DefaultCacheProvider();
  }
  
  // Capacity just needs to be big enough to retain elements.
  private static final int CAPACITY = 100;
  private TtlCache<String, String> makeTtlCache(long minTtl, long maxTtl) {
    TtlCache<String, String> ttlCache =
        new TtlCache<String, String>(cacheProvider, CAPACITY, minTtl, maxTtl);
    ttlCache.setTimeSource(timeSource);
    return ttlCache;
  }
  
  public void testGeneralCacheExpiration() {
    TtlCache<String, String> ttlCache = makeTtlCache(120 * 1000, 360 * 1000);
    String key = "key1", val = "val1";
    ttlCache.addElement(key, val, 240 * 1000);
	
    // Time is still 0: should be in the cache.
    assertEquals(val, ttlCache.getElement(key));
    assertEquals(val, ttlCache.getElementWithExpiration(key).obj);
    assertFalse(ttlCache.getElementWithExpiration(key).isExpired);
	
    // Time = 120 seconds: still in cache.
    timeSource.setCurrentTimeMillis(120 * 1000);
    assertEquals(val, ttlCache.getElement(key));
    assertEquals(val, ttlCache.getElementWithExpiration(key).obj);
    assertFalse(ttlCache.getElementWithExpiration(key).isExpired);
	
    // Time = 240 seconds - 1 ms: still in cache.
    timeSource.setCurrentTimeMillis(240 * 1000 - 1);
    assertEquals(val, ttlCache.getElement(key));
    assertEquals(val, ttlCache.getElementWithExpiration(key).obj);
    assertFalse(ttlCache.getElementWithExpiration(key).isExpired);
	
    // Time = 300 seconds: out of cache.
    timeSource.setCurrentTimeMillis(300 * 1000);
    assertNull(ttlCache.getElement(key));
    assertEquals(val, ttlCache.getElementWithExpiration(key).obj);
    assertTrue(ttlCache.getElementWithExpiration(key).isExpired);
  }
  
  public void testRemoveFromCache() {
    TtlCache<String, String> ttlCache = makeTtlCache(120 * 1000, 360 * 1000);
    String key = "key1", val = "val1";
    ttlCache.addElement(key, val, 240 * 1000);
    
    // Time at 0: still in cache
    assertEquals(val, ttlCache.getElement(key));
    
    // Time at 120: still in cache, but removing it.
    timeSource.setCurrentTimeMillis(120 * 1000);
    assertEquals(val, ttlCache.removeElement(key));
    
    // Still at 120: should be gone.
    assertNull(ttlCache.removeElement(key));
  }
  
  public void testCacheMinTtl() {
    TtlCache<String, String> ttlCache = makeTtlCache(120 * 1000, 360 * 1000);
    String key = "key1", val = "val1";
    
    // Add with a value below minTtl
    ttlCache.addElement(key, val, 60 * 1000);
    
    // Time 0: still in cache.
    assertEquals(val, ttlCache.getElement(key));
    
    // Time 65: still in cache - not expired! minTtl takes precedence.
    timeSource.setCurrentTimeMillis(65 * 1000);
    assertEquals(val, ttlCache.getElement(key));
    
    // Time 121: out of cache.
    timeSource.setCurrentTimeMillis(121 * 1000);
    assertNull(ttlCache.getElement(key));
  }
  
  public void testCacheMaxTtl() {
    TtlCache<String, String> ttlCache = makeTtlCache(120 * 1000, 360 * 1000);
    String key = "key1", val = "val1";
    
    // Add with a value above maxTtl
    ttlCache.addElement(key, val, 400 * 1000);
    
    // Time 0: still in cache.
    assertEquals(val, ttlCache.getElement(key));
    
    // Time 360 - 1ms: still in cache.
    timeSource.setCurrentTimeMillis(360 * 1000 - 1);
    assertEquals(val, ttlCache.getElement(key));
    
    // Time 361: out of cache. Expired despite "desired" ttl of 400 secs.
    timeSource.setCurrentTimeMillis(361 * 1000);
    assertNull(ttlCache.getElement(key));
  }
}
