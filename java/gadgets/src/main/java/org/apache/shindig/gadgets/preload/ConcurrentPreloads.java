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

import com.google.common.collect.Maps;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Preloads data by processing all Runnables concurrently.
 */
class ConcurrentPreloads implements Preloads {
  private final Map<String, Future<PreloadedData>> preloads;

  ConcurrentPreloads() {
    preloads = Maps.newHashMap();
  }

  /**
   * Add an active preloading process.
   *
   * @param key The key that this preload will be stored under.
   * @param futureData A future that will return the preloaded data.
   */
  ConcurrentPreloads add(String key, Future<PreloadedData> futureData) {
    preloads.put(key, futureData);
    return this;
  }

  public Set<String> getKeys() {
    return preloads.keySet();
  }

  public PreloadedData getData(String key) throws PreloadException {
    Future<PreloadedData> future = preloads.get(key);

    if (future == null) {
      return null;
    }

    try {
      // TODO: Determine if timeouts should be supported.
      return future.get();
    } catch (InterruptedException e) {
      // Thread was interrupted. We might want to throw a RTE here, but this is probably only going
      // to happen if we're shutting down the server anyway.
      throw new PreloadException("Preloading was interrupted by thread termination.", e);
    } catch (ExecutionException e) {
      // Callable threw an exception. Throw the original.
      Throwable cause = e.getCause();
      if (cause instanceof PreloadException) {
        throw (PreloadException) cause;
      }
      throw new PreloadException(cause);
    }
  }
}
