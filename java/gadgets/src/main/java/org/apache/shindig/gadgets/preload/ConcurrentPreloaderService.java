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
package org.apache.shindig.gadgets.preload;

import com.google.inject.Inject;

import org.apache.shindig.gadgets.Gadget;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

/**
 * Preloads will be fetched concurrently using the injected ExecutorService, and they can be read
 * lazily using the returned map of futures.
 */
public class ConcurrentPreloaderService implements PreloaderService {
  private final ExecutorService executor;
  private final List<? extends Preloader> preloaders;

  @Inject
  public ConcurrentPreloaderService(ExecutorService executor,
      List<? extends Preloader> preloaders) {
    this.executor = executor;
    this.preloaders = preloaders;
  }

  public Preloads preload(Gadget gadget) {
    ConcurrentPreloads preloads = new ConcurrentPreloads();
    for (Preloader preloader : preloaders) {
      Map<String, Callable<PreloadedData>> tasks = preloader.createPreloadTasks(gadget);
      for (Map.Entry<String, Callable<PreloadedData>> entry : tasks.entrySet()) {
        preloads.add(entry.getKey(), executor.submit(entry.getValue()));
      }
    }
    return preloads;
  }
}
