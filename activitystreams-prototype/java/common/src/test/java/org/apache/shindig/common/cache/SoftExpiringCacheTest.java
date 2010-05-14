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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SoftExpiringCacheTest extends Assert {
  private FakeTimeSource timeSource;
  private Cache<String, String> cache;

  @Before
  public void setUp() throws Exception {
    timeSource = new FakeTimeSource(0);
    cache = new LruCache<String, String>(5);
  }

  private SoftExpiringCache<String, String> makeSoftExpiringCache() {
    SoftExpiringCache<String, String> expiringCache = new SoftExpiringCache<String, String>(cache);
    expiringCache.setTimeSource(timeSource);
    return expiringCache;
  }

  @Test
  public void testGeneralCacheExpiration() {
    SoftExpiringCache<String, String> expiringCache = makeSoftExpiringCache();
    String key = "key1", val = "val1";
    expiringCache.addElement(key, val, 240 * 1000);

    // Time is still 0: should be in the cache.
    assertEquals(val, expiringCache.getElement(key).obj);
    assertFalse(expiringCache.getElement(key).isExpired);

    // Time = 300 seconds: out of cache.
    timeSource.setCurrentTimeMillis(300 * 1000);
    assertEquals(val, expiringCache.getElement(key).obj);
    assertTrue(expiringCache.getElement(key).isExpired);
  }

  @Test
  public void testMissingValue() {
    SoftExpiringCache<String, String> expiringCache = makeSoftExpiringCache();
    assertNull(expiringCache.getElement("not set"));
  }
}
