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

import com.google.caja.plugin.Job;
import com.google.caja.plugin.stages.JobCache;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.util.ContentType;
import com.google.common.collect.ImmutableList;

import java.util.List;

import org.apache.shindig.common.cache.Cache;

/**
 * A per-module cache of intermediate cajoling results.
 */
final class ModuleCache extends JobCache {
  private final Cache<ModuleCacheKey, ImmutableList<Job>> backingCache;

  ModuleCache(Cache<ModuleCacheKey, ImmutableList<Job>> backingCache) {
    this.backingCache = backingCache;
  }

  public ModuleCacheKey forJob(ContentType type, ParseTreeNode node) {
    return new ModuleCacheKey(type, node);
  }

  public List<? extends Job> fetch(Key k) {
    if (!(k instanceof ModuleCacheKey)) { return null; }
    ImmutableList<Job> cachedJobs = backingCache.getElement((ModuleCacheKey) k);
    if (cachedJobs == null) { return null; }
    if (cachedJobs.isEmpty()) { return cachedJobs; }
    return cloneJobs(cachedJobs);
  }

  public void store(Key k, List<? extends Job> derivatives) {
    if (!(k instanceof ModuleCacheKey)) {
      throw new IllegalArgumentException(k.getClass().getName());
    }
    ModuleCacheKey key = (ModuleCacheKey) k;
    backingCache.addElement(key, cloneJobs(derivatives));
  }

  private static ImmutableList<Job> cloneJobs(Iterable<? extends Job> jobs) {
    ImmutableList.Builder<Job> clones = ImmutableList.builder();
    for (Job job : jobs) {
      clones.add(job.clone());
    }
    return clones.build();
  }

}
