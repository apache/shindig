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
package org.apache.shindig.gadgets.servlet;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.caja.plugin.stages.JobCache;

import java.util.Iterator;
import java.util.Set;

/**
 * A bundle of {@link ModuleCacheKey}s.
 */
final class ModuleCacheKeys implements JobCache.Keys {

  final ImmutableList<ModuleCacheKey> keys;

  ModuleCacheKeys(ModuleCacheKey key) {
    this.keys = ImmutableList.of(key);
  }

  private ModuleCacheKeys(Iterable<? extends ModuleCacheKey> keys) {
    this.keys = ImmutableList.copyOf(keys);
  }

  public ModuleCacheKeys union(JobCache.Keys other) {
    if (!other.iterator().hasNext()) { return this; }
    ModuleCacheKeys that = (ModuleCacheKeys) other;
    Set<ModuleCacheKey> allKeys = Sets.newLinkedHashSet();
    allKeys.addAll(this.keys);
    allKeys.addAll(that.keys);
    if (allKeys.size() == this.keys.size()) { return this; }
    if (allKeys.size() == that.keys.size()) { return that; }
    return new ModuleCacheKeys(allKeys);
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof ModuleCacheKeys && keys.equals(((ModuleCacheKeys) o).keys);
  }

  @Override
  public int hashCode() {
    return keys.hashCode();
  }

  public Iterator<JobCache.Key> iterator() {
    return ImmutableList.<JobCache.Key>copyOf(keys).iterator();
  }

}
