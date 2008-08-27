/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.gadgets;

import com.google.common.collect.Maps;
import com.google.inject.Inject;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Handles preloading operations, such as HTTP fetches, social data retrieval, or anything else that
 * would benefit from preloading on the server instead of incurring a network request for users.
 *
 * Preloads will be fetched concurrently using the injected ExecutorService, and they can be read
 * lazily using the returned map of futures.
 */
public class PreloaderService {
  private final ExecutorService executor;
  private final List<? extends Preloader> preloaders;

  @Inject
  public PreloaderService(ExecutorService executor, List<? extends Preloader> preloaders) {
    this.executor = executor;
    this.preloaders = preloaders;
  }

  /**
   * Begin all preload operations.
   *
   * @param gadget The gadget to perform preloading for.
   * @return A map of all preloaded data. Callers can retrieve preloads by unique key.
   */
  public Map<String, Future<Preload>> preload(Gadget gadget) {
    Map<String, Future<Preload>> preloads = Maps.newHashMap();
    for (Preloader preloader : preloaders) {
      Map<String, Callable<Preload>> tasks = preloader.createPreloadTasks(gadget);
      for (Map.Entry<String, Callable<Preload>> entry : tasks.entrySet()) {
        preloads.put(entry.getKey(), executor.submit(entry.getValue()));
      }
    }
    return preloads;
  }
}
