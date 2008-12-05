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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.google.common.collect.Sets;

import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Tests for ConcurrentPreloads.
 */
public class ConcurrentPreloadsTest {

  @Test
  public void getKeys() {
    ConcurrentPreloads preloads = new ConcurrentPreloads();
    preloads.add("foo", TestFuture.returnsNormal());
    preloads.add("throwsInterrupted", TestFuture.throwsInterrupted());

    assertEquals(Sets.newHashSet("foo", "throwsInterrupted"), preloads.getKeys());
  }

  @Test
  public void getPreloadedDataNormal() throws Exception {
    ConcurrentPreloads preloads = new ConcurrentPreloads();
    preloads.add("foo", TestFuture.returnsNormal());

    assertNotNull(preloads.getData("foo"));
  }

  @Test
  public void getPreloadedDataNull() throws Exception {
    ConcurrentPreloads preloads = new ConcurrentPreloads();
    preloads.add("foo", null);
    assertNull(preloads.getData("foo"));
  }

  @Test(expected = PreloadException.class)
  public void getPreloadedDataThrowsInterrupted() throws Exception {
    ConcurrentPreloads preloads = new ConcurrentPreloads();
    preloads.add("foo", TestFuture.throwsInterrupted());
    preloads.getData("foo");
  }

  @Test(expected = PreloadException.class)
  public void getPreloadedThrowsExecution() throws Exception {
    ConcurrentPreloads preloads = new ConcurrentPreloads();
    preloads.add("foo", TestFuture.throwsExecution());
    preloads.getData("foo");
  }

  @Test(expected = PreloadException.class)
  public void getPreloadedThrowsExecutionWrapped() throws Exception {
    ConcurrentPreloads preloads = new ConcurrentPreloads();
    preloads.add("foo", TestFuture.throwsExecutionWrapped());
    preloads.getData("foo");
  }

  private static class TestFuture implements Future<PreloadedData> {
    private boolean throwsInterrupted;
    private boolean throwsExecution;
    private boolean throwsExecutionWrapped;

    public static TestFuture returnsNormal() {
      return new TestFuture();
    }

    public static TestFuture returnsNull() {
      return new TestFuture();
    }

    public static TestFuture throwsInterrupted() {
      TestFuture future = new TestFuture();
      future.throwsInterrupted = true;
      return future;
    }

    public static TestFuture throwsExecution() {
      TestFuture future = new TestFuture();
      future.throwsExecution = true;
      return future;
    }

    public static TestFuture throwsExecutionWrapped() {
      TestFuture future = new TestFuture();
      future.throwsExecutionWrapped = true;
      return future;
    }

    public PreloadedData get() throws InterruptedException, ExecutionException {
      if (throwsInterrupted) {
        throw new InterruptedException("Interrupted!");
      }

      if (throwsExecution) {
        throw new ExecutionException(new RuntimeException("Fail"));
      }

      if (throwsExecutionWrapped) {
        throw new ExecutionException(new PreloadException("Preload failed."));
      }

      return new PreloadedData() {
        public Object toJson() {
          return "Preloaded";
        }
      };
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
      return false;
    }

    public PreloadedData get(long timeout, TimeUnit unit) throws InterruptedException,
        ExecutionException  {
      return get();
    }

    public boolean isCancelled() {
      return false;
    }

    public boolean isDone() {
      return false;
    }
  }
}
