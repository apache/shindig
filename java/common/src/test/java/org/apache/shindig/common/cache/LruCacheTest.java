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

import org.junit.Test;

public class LruCacheTest {

  private static final int TEST_CAPACITY = 2;
  private LruCache<String, String> cache = LruCache.create(TEST_CAPACITY);

  @Test
  public void normalCapacityOk() {
    for (int i = 0; i < TEST_CAPACITY; ++i) {
      cache.addElement(Integer.toString(i), Integer.toString(i));
    }
    assertEquals(TEST_CAPACITY, cache.size());
    assertEquals("0", cache.getElement("0"));
  }

  @Test
  public void exceededCapacityRemoved() {
    for (int i = 0; i < TEST_CAPACITY + 1; ++i) {
      cache.addElement(Integer.toString(i), Integer.toString(i));
    }
    assertEquals(TEST_CAPACITY, cache.size());
    assertEquals(null, cache.getElement("0"));
  }
}
