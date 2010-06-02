/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.common.util;

import org.apache.shindig.protocol.RestfulCollection;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Utility methods for processing {@link Future} wrapped objects
 */
public final class FutureUtil {
  private FutureUtil() {}
  /**
   * Process a {@link Future} wrapped {@link RestfulCollection}
   * to return the first (if any) object, as a {@link Future}
   * @param collection the collection to retrieve the first object from
   * @return the {@link Future} wrapped object
   */
  public static <T> Future<T> getFirstFromCollection(final Future<RestfulCollection<T>> collection) {
    return new Future<T>() {
      public boolean cancel(boolean mayInterruptIfRunning) {
        return collection.cancel(mayInterruptIfRunning);
      }

      public T get() throws InterruptedException, ExecutionException {
        return getFirstFromCollection(collection.get());
      }

      public T get(long timeout, TimeUnit unit)
          throws InterruptedException, ExecutionException, TimeoutException {
        return getFirstFromCollection(collection.get(timeout, unit));
      }

      public boolean isCancelled() {
        return collection.isCancelled();
      }

      public boolean isDone() {
        return collection.isDone();
      }

      private T getFirstFromCollection(RestfulCollection<T> collection) {
        if (collection.getTotalResults() > 0) {
          return collection.getEntry().get(0);
        }

        return null;
      }
    };
  }
}
