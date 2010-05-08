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

/**
 * A basic cache interface. If necessary, we can always move to the commons
 * cache for the future.
 */
public interface Cache<K, V> {

  /**
   * Retrieves an entry for the cache.
   *
   * @return The entry stored under the given key, or null if it doesn't exist.
   */
  V getElement(K key);

  /**
   * Stores an entry into the cache.
   */
  void addElement(K key, V value);

  /**
   * Removes an entry from the cache.
   *
   * @param key The entry to return.
   * @return The entry stored under the given key, or null if it doesn't exist.
   */
  V removeElement(K key);

  /**
   * Returns the capacity of the cache.
   *
   * @return a positive integer indicating the upper bound on the number of allowed elements
   * in the cace, -1 signifies that the capacity is unbounded
   */
  long getCapacity();

  /**
   * @return The current size of the cache, or -1 if the cache does not support returning sizes.
   */
  long getSize();
}
