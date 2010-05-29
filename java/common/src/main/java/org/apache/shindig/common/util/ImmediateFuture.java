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

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutionException;

/**
 * Implements a future that is immediately available.
 */
public final class ImmediateFuture {
  private ImmediateFuture() {}
  
  /**
   * Returns a future instance.
   * @param value the value, which may be null.
   * @return the future
   */
  public static <T> Future<T> newInstance(final T value) {
    return new Future<T>() {
      public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
      }

      public boolean isCancelled() {
        return false;
      }

      public boolean isDone() {
        return true;
      }

      public T get() {
        return value;
      }

      public T get(long timeout, TimeUnit unit) {
        return value;
      }
    };
  }

  /**
   * Returns a future instance that produces an error.
   * @param error the exception that should be wrapped in an
   *     ExecutionException when {link #get()} is called.
   * @return the future
   */
  public static <T> Future<T> errorInstance(final Throwable error) {
    return new Future<T>() {
      public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
      }

      public boolean isCancelled() {
        return false;
      }

      public boolean isDone() {
        return true;
      }

      public T get() throws ExecutionException {
        throw new ExecutionException(error);
      }

      public T get(long timeout, TimeUnit unit) throws ExecutionException {
        throw new ExecutionException(error);
      }
    };
  }
}
