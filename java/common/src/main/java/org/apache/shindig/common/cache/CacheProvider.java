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

import com.google.inject.ImplementedBy;

/**
 * Interface for Shindig caches.
 */
@ImplementedBy(LruCacheProvider.class)
public interface CacheProvider {
  /**
   * Create a named single instance cache in this cache manager, if the cache
   * already exists, return it if the name is null, a new anonymous cache is
   * created
   *
   * @param <K>  The Key type for the cache
   * @param <V>  The pay-load type
   * @param name The non-null name of the cache.
   * @return A Cache configured to the required specification.
   */
  <K, V> Cache<K, V> createCache(String name);
}
