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
package org.apache.shindig.gadgets;

import org.apache.shindig.gadgets.GadgetDataCache;

import java.util.HashMap;
import java.util.Map;

/**
 * Basic implementation of an in-memory data cache, backed by a
 * {@code HashMap}.
 * 
 * @param <T> Type of data to store in the cache.
 */
public class BasicGadgetDataCache<T> implements GadgetDataCache<T> {
  private Map<String, T> cache = new HashMap<String, T>();

  /** {@inheritDoc} */
  public T get(String key) {
    return cache.get(key);
  }

  /** {@inheritDoc} */
  public void put(String key, T value) {
    cache.put(key, value);
  }
}
