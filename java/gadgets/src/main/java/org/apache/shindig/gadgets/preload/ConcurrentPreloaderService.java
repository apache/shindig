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

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.spec.GadgetSpec;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;

/**
 * Preloads will be fetched concurrently using the injected ExecutorService, and they can be read
 * lazily using the returned map of futures.
 *
 * The last preloaded object always executes in the current thread to avoid creating unnecessary
 * additional threads when we're blocking the current request anyway.
 */
public class ConcurrentPreloaderService implements PreloaderService {
  private final ExecutorService executor;
  private final List<? extends Preloader> preloaders;

  @Inject
  public ConcurrentPreloaderService(ExecutorService executor, List<Preloader> preloaders) {
    this.executor = executor;
    this.preloaders = preloaders;
  }

  public Preloads preload(GadgetContext context, GadgetSpec gadget,
      PreloadPhase phase) {
    if (preloaders.isEmpty()) {
      return new NullPreloads();
    }

    List<Callable<PreloadedData>> tasks = Lists.newArrayList();
    for (Preloader preloader : preloaders) {
      Collection<Callable<PreloadedData>> taskCollection =
          preloader.createPreloadTasks(context, gadget, phase);
      tasks.addAll(taskCollection);
    }

    ConcurrentPreloads preloads = new ConcurrentPreloads();
    int processed = tasks.size();
    for (Callable<PreloadedData> task : tasks) {
      processed -= 1;
      if (processed == 0) {
        // The last preload fires in the current thread.
        // TODO: for the HTML_RENDER phase, if there's also a proxied fetch, this
        // is counter-productive
        FutureTask<PreloadedData> futureTask = new FutureTask<PreloadedData>(task);
        futureTask.run();
        preloads.add(futureTask);
      } else {
        preloads.add(executor.submit(task));
      }
    }
    return preloads;
  }
}
