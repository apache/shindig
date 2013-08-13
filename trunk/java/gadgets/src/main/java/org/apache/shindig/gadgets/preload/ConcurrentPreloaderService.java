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
package org.apache.shindig.gadgets.preload;

import org.apache.shindig.gadgets.Gadget;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;

import com.google.inject.Inject;

/**
 * Preloads will be fetched concurrently using the injected ExecutorService, and they can be read
 * lazily using the returned map of futures.
 *
 * The last preloaded object always executes in the current thread to avoid creating unnecessary
 * additional threads when we're blocking the current request anyway.
 */
public class ConcurrentPreloaderService implements PreloaderService {
  private final ExecutorService executor;
  private Preloader preloader;

  @Inject
  public ConcurrentPreloaderService(ExecutorService executor, Preloader preloader) {
    this.executor = executor;
    this.preloader = preloader;
  }

  public Collection<PreloadedData> preload(Gadget gadget) {
    Collection<Callable<PreloadedData>> tasks =
        preloader.createPreloadTasks(gadget);

    return preload(tasks);
  }

  public Collection<PreloadedData> preload(Collection<Callable<PreloadedData>> tasks) {
    ConcurrentPreloads preloads = new ConcurrentPreloads(tasks.size());
    int processed = tasks.size();
    for (Callable<PreloadedData> task : tasks) {
      processed -= 1;
      if (processed == 0) {
        // The last preload fires in the current thread.
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
