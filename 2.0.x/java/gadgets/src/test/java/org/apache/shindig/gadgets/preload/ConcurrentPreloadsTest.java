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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * Tests for ConcurrentPreloads.
 */
public class ConcurrentPreloadsTest {

  @Test
  public void getData() throws Exception {
    ConcurrentPreloads preloads = new ConcurrentPreloads();
    preloads.add(TestFuture.returnsNormal("foo"));
    preloads.add(TestFuture.returnsNormal("bar"));

    assertEquals(2, preloads.size());
    Iterator<PreloadedData> iterator = preloads.iterator();
    assertEquals(TestFuture.expectedResult("foo"), iterator.next().toJson());
    assertEquals(TestFuture.expectedResult("bar"), iterator.next().toJson());
    assertFalse(iterator.hasNext());
  }

  @Test
  public void getDataWithRuntimeException() throws Exception{
    ConcurrentPreloads preloads = new ConcurrentPreloads();
    preloads.add(TestFuture.throwsExecution());
    preloads.add(TestFuture.returnsNormal("foo"));

    assertEquals(2, preloads.size());
    Iterator<PreloadedData> iterator = preloads.iterator();

    // First item should throw an exception, a PreloadException around
    // a RuntimeException
    PreloadedData withError = iterator.next();
    try {
      withError.toJson();
      fail();
    } catch (PreloadException pe) {
      assertSame(pe.getCause().getClass(), RuntimeException.class);
    }

    // And iteration should continue
    assertEquals(TestFuture.expectedResult("foo"), iterator.next().toJson());
  }

  @Test
  public void getDataWithPreloadException() throws Exception{
    ConcurrentPreloads preloads = new ConcurrentPreloads();
    preloads.add(TestFuture.throwsExecutionWrapped());
    preloads.add(TestFuture.returnsNormal("foo"));

    assertEquals(2, preloads.size());
    Iterator<PreloadedData> iterator = preloads.iterator();

    // First item should throw an exception, a straight PreloadException
    PreloadedData withError = iterator.next();
    try {
      withError.toJson();
      fail();
    } catch (PreloadException pe) {
      assertNull(pe.getCause());
    }

    // And iteration should continue
    assertEquals(TestFuture.expectedResult("foo"), iterator.next().toJson());
  }

  @Test(expected = RuntimeException.class)
  public void getDataThrowsInterruped() throws Exception{
    ConcurrentPreloads preloads = new ConcurrentPreloads();
    preloads.add(TestFuture.throwsInterrupted());
    preloads.add(TestFuture.returnsNormal("foo"));

    assertEquals(2, preloads.size());
    Iterator<PreloadedData> iterator = preloads.iterator();
    // InterruptedException should immediately terminate
    iterator.next();
  }

  private static class TestFuture implements Future<PreloadedData> {
    private boolean throwsInterrupted;
    private boolean throwsExecution;
    private boolean throwsExecutionWrapped;
    protected final String key;

    private TestFuture(String key) {
      this.key = key;
    }

    public static TestFuture returnsNormal(String key) {
      return new TestFuture(key);
    }

    public static TestFuture throwsInterrupted() {
      TestFuture future = new TestFuture(null);
      future.throwsInterrupted = true;
      return future;
    }

    public static TestFuture throwsExecution() {
      TestFuture future = new TestFuture(null);
      future.throwsExecution = true;
      return future;
    }

    public static TestFuture throwsExecutionWrapped() {
      TestFuture future = new TestFuture(null);
      future.throwsExecutionWrapped = true;
      return future;
    }

    public static Collection<Object> expectedResult(String key) {
      return ImmutableList.of((Object) ImmutableMap.of(key, "Preloaded"));
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
        public Collection<Object> toJson() {
          return expectedResult(key);
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
      return true;
    }
  }
}
