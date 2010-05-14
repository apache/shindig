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

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.google.common.base.Function;
import com.google.common.collect.ForwardingCollection;
import com.google.common.collect.Lists;

/**
 * Preloads data by evaluating Futures for PreloadedData.
 * This class is not, however, thread-safe - tasks must be
 * added and read from a single thread..
 */
class ConcurrentPreloads extends ForwardingCollection<PreloadedData> {
  private final List<Future<PreloadedData>> tasks;
  private Collection<PreloadedData> loaded;

  ConcurrentPreloads() {
    tasks = Lists.newArrayList();
  }

  ConcurrentPreloads(int size) {
    tasks = Lists.newArrayListWithCapacity(size);
  }

  /**
   * Add an active preloading process.
   *
   * @param futureData A future that will return the preloaded data.
   */
  ConcurrentPreloads add(Future<PreloadedData> futureData) {
    tasks.add(futureData);
    return this;
  }

  @Override
  protected Collection<PreloadedData> delegate() {
    if (loaded == null) {
      loaded = getData();
    }

    return loaded;
  }

  private Collection<PreloadedData> getData() {
    return Lists.transform(tasks, new Function<Future<PreloadedData>, PreloadedData>() {
      public PreloadedData apply(Future<PreloadedData> preloadedDataFuture) {
        return getPreloadedData(preloadedDataFuture);
      }
    });
  }

  /**
   * Gets the preloaded data, handling any exceptions from Future processing.
   */
  protected PreloadedData getPreloadedData(Future<PreloadedData> preloadedDataFuture) {
    try {
     return preloadedDataFuture.get();
    } catch (ExecutionException ee) {
      return new FailedPreload(ee.getCause());
    } catch (InterruptedException ie) {
      // Do NOT Propagate the interrupt
      throw new RuntimeException("Preloading was interrupted by thread termination", ie);
    }
  }

  /** PreloadData implementation that reports failure */
  private static class FailedPreload implements PreloadedData {
    private final Throwable t;

    public FailedPreload(Throwable t) {
      this.t = t;
    }

    public Collection<Object> toJson() throws PreloadException {
      if (t instanceof PreloadException) {
        throw (PreloadException) t;
      }

      throw new PreloadException(t);
    }
  }
}
