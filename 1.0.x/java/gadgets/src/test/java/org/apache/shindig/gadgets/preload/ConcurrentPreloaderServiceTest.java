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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import org.apache.shindig.common.testing.TestExecutorService;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.spec.GadgetSpec;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import org.junit.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

/**
 * Tests for FuturePreloaderService.
 */
public class ConcurrentPreloaderServiceTest {
  private static final String PRELOAD_STRING_KEY = "key a";
  private static final String PRELOAD_NUMERIC_KEY = "key b";
  private static final String PRELOAD_MAP_KEY = "key c";
  private static final String PRELOAD_STRING_VALUE = "Some random string";
  private static final Integer PRELOAD_NUMERIC_VALUE = 5;
  private static final Map<String, String> PRELOAD_MAP_VALUE
      = ImmutableMap.of("foo", "bar", "baz", "blah");

  private final TestPreloader preloader = new TestPreloader();
  private final TestPreloader preloader2 = new TestPreloader();

  @Test
  public void preloadSingleService() throws PreloadException {
    preloader.tasks.put(PRELOAD_STRING_KEY,
        new TestPreloadCallable(new DataPreload(PRELOAD_STRING_VALUE)));

    PreloaderService service = new ConcurrentPreloaderService(new TestExecutorService(),
        Arrays.<Preloader>asList(preloader));

    assertEquals(PRELOAD_STRING_VALUE,
                 service.preload(null, null).getData(PRELOAD_STRING_KEY).toJson());
  }

  @Test
  public void preloadMultipleServices() throws PreloadException {
    preloader.tasks.put(PRELOAD_STRING_KEY,
        new TestPreloadCallable(new DataPreload(PRELOAD_STRING_VALUE)));

    preloader.tasks.put(PRELOAD_NUMERIC_KEY,
        new TestPreloadCallable(new DataPreload(PRELOAD_NUMERIC_VALUE)));

    preloader2.tasks.put(PRELOAD_MAP_KEY,
        new TestPreloadCallable(new DataPreload(PRELOAD_MAP_VALUE)));

    PreloaderService service = new ConcurrentPreloaderService(new TestExecutorService(),
        Arrays.<Preloader>asList(preloader, preloader2));

    Preloads preloads = service.preload(null, null);

    assertEquals(PRELOAD_STRING_VALUE, preloads.getData(PRELOAD_STRING_KEY).toJson());
    assertEquals(PRELOAD_NUMERIC_VALUE, preloads.getData(PRELOAD_NUMERIC_KEY).toJson());
    assertEquals(PRELOAD_MAP_VALUE, preloads.getData(PRELOAD_MAP_KEY).toJson());
  }

  @Test
  public void multiplePreloadsFiresJustOneInCurrentThread() throws Exception {
    preloader.tasks.put(PRELOAD_STRING_KEY,
        new TestPreloadCallable(new DataPreload(PRELOAD_STRING_VALUE)));

    preloader.tasks.put(PRELOAD_NUMERIC_KEY,
        new TestPreloadCallable(new DataPreload(PRELOAD_MAP_VALUE)));

    preloader.tasks.put(PRELOAD_MAP_KEY,
        new TestPreloadCallable(new DataPreload(PRELOAD_NUMERIC_VALUE)));

    PreloaderService service = new ConcurrentPreloaderService(Executors.newFixedThreadPool(5),
        Arrays.<Preloader>asList(preloader));

    service.preload(null, null);

    TestPreloadCallable ranInSameThread = null;
    for (Callable<PreloadedData> callable : preloader.tasks.values()) {
      TestPreloadCallable preloadCallable = (TestPreloadCallable)callable;
      if (ranInSameThread != null) {
        fail("More than one request ran in the current thread.");
      }

      if (preloadCallable.executedThread == Thread.currentThread()) {
        ranInSameThread = preloadCallable;
      }
    }

    assertNotNull("No preloads executed in the current thread. ", ranInSameThread);
  }

  @Test
  public void singlePreloadExecutesInCurrentThread() throws Exception {
    preloader.tasks.put(PRELOAD_STRING_KEY,
        new TestPreloadCallable(new DataPreload(PRELOAD_STRING_VALUE)));

    PreloaderService service = new ConcurrentPreloaderService(Executors.newCachedThreadPool(),
        Arrays.<Preloader>asList(preloader));

    service.preload(null, null);

    TestPreloadCallable callable = (TestPreloadCallable)preloader.tasks.get(PRELOAD_STRING_KEY);

    assertSame("Single request not run in current thread",
        Thread.currentThread(), callable.executedThread);
  }

  @Test(expected = PreloadException.class)
  public void exceptionsArePropagated() throws PreloadException {
    preloader.tasks.put(PRELOAD_STRING_KEY, new TestPreloadCallable(null));
    PreloaderService service = new ConcurrentPreloaderService(new TestExecutorService(),
        Arrays.<Preloader>asList(preloader));
    service.preload(null, null).getData(PRELOAD_STRING_KEY);
  }

  private static class TestPreloader implements Preloader {
    private final Map<String, Callable<PreloadedData>> tasks = Maps.newHashMap();

    public Map<String, Callable<PreloadedData>> createPreloadTasks(
        GadgetContext context, GadgetSpec spec) {
      return tasks;
    }
  }

  private static class TestPreloadCallable implements Callable<PreloadedData> {
    private final PreloadedData preload;
    public Thread executedThread;

    public TestPreloadCallable(PreloadedData preload) {
      this.preload = preload;
    }

    public PreloadedData call() throws Exception {
      executedThread = Thread.currentThread();
      if (preload == null) {
        throw new PreloadException("No preload for this test.");
      }
      return preload;
    }
  }

  private static class DataPreload implements PreloadedData {
    private final Object data;

    public DataPreload(Object data) {
      this.data = data;
    }

    public Object toJson() {
      return data;
    }
  }
}
