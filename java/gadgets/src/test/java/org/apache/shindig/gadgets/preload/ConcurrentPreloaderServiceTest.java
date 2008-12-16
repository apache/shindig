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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.shindig.common.testing.TestExecutorService;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import static org.junit.Assert.*;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
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
  public void preloadSingleService() throws Exception {
    preloader.tasks.add(new TestPreloadCallable(
        new DataPreload(PRELOAD_STRING_KEY, PRELOAD_STRING_VALUE)));

    PreloaderService service = new ConcurrentPreloaderService(new TestExecutorService(),
        Arrays.<Preloader>asList(preloader));

    Preloads preloads = service.preload(null, null, PreloaderService.PreloadPhase.HTML_RENDER);

    Map<String, Object> preloaded = getAll(preloads);
    assertEquals(ImmutableMap.of(PRELOAD_STRING_KEY, PRELOAD_STRING_VALUE), preloaded);
  }

  /** Load all the data out of a Preloads object */
  private Map<String, Object> getAll(Preloads preloads) throws PreloadException {
    Map<String, Object> map = Maps.newHashMap();
    for (PreloadedData preloadCallable : preloads.getData()) {
      map.putAll(preloadCallable.toJson());
    }

    return map;
  }

  @Test
  public void preloadMultipleServices() throws PreloadException {
    preloader.tasks.add(new TestPreloadCallable(
        new DataPreload(PRELOAD_STRING_KEY, PRELOAD_STRING_VALUE)));

    preloader.tasks.add(new TestPreloadCallable(
        new DataPreload(PRELOAD_NUMERIC_KEY, PRELOAD_NUMERIC_VALUE)));

    preloader2.tasks.add(new TestPreloadCallable(
        new DataPreload(PRELOAD_MAP_KEY, PRELOAD_MAP_VALUE)));

    PreloaderService service = new ConcurrentPreloaderService(new TestExecutorService(),
        Arrays.<Preloader>asList(preloader, preloader2));

    Preloads preloads = service.preload(null, null, PreloaderService.PreloadPhase.HTML_RENDER);

    Map<String, Object> preloaded = getAll(preloads);
    assertEquals(ImmutableMap.of(
        PRELOAD_STRING_KEY, PRELOAD_STRING_VALUE,
        PRELOAD_NUMERIC_KEY, PRELOAD_NUMERIC_VALUE,
        PRELOAD_MAP_KEY, PRELOAD_MAP_VALUE), preloaded);
  }

  @Test
  public void multiplePreloadsFiresJustOneInCurrentThread() throws Exception {
    TestPreloadCallable first =
        new TestPreloadCallable(new DataPreload(PRELOAD_STRING_KEY, PRELOAD_STRING_VALUE));
    TestPreloadCallable second =
        new TestPreloadCallable(new DataPreload(PRELOAD_NUMERIC_KEY, PRELOAD_MAP_VALUE));
    TestPreloadCallable third =
        new TestPreloadCallable(new DataPreload(PRELOAD_MAP_KEY, PRELOAD_NUMERIC_VALUE));

    preloader.tasks.add(first);
    preloader.tasks.add(second);
    preloader.tasks.add(third);

    PreloaderService service = new ConcurrentPreloaderService(Executors.newFixedThreadPool(5),
        Arrays.<Preloader>asList(preloader));

    service.preload(null, null, PreloaderService.PreloadPhase.HTML_RENDER);

    TestPreloadCallable ranInSameThread = null;
    for (TestPreloadCallable preloadCallable: Lists.newArrayList(first, second, third)) {
      if (preloadCallable.executedThread == Thread.currentThread()) {
        if (ranInSameThread != null) {
          fail("More than one request ran in the current thread.");
        }

        ranInSameThread = preloadCallable;
      }
    }

    assertNotNull("No preloads executed in the current thread. ", ranInSameThread);
  }

  @Test
  public void singlePreloadExecutesInCurrentThread() throws Exception {
    TestPreloadCallable callable =
        new TestPreloadCallable(new DataPreload(PRELOAD_STRING_KEY, PRELOAD_STRING_VALUE));
    preloader.tasks.add(callable);

    PreloaderService service = new ConcurrentPreloaderService(Executors.newCachedThreadPool(),
        Arrays.<Preloader>asList(preloader));

    service.preload(null, null, PreloaderService.PreloadPhase.HTML_RENDER);

    assertSame("Single request not run in current thread",
        Thread.currentThread(), callable.executedThread);
  }

  private static class TestPreloader implements Preloader {
    private final Collection<Callable<PreloadedData>> tasks = Lists.newArrayList();

    public Collection<Callable<PreloadedData>> createPreloadTasks(
        GadgetContext context, GadgetSpec spec, PreloaderService.PreloadPhase phase) {
      assertEquals(PreloaderService.PreloadPhase.HTML_RENDER, phase);
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
    private final String key;
    private final Object data;

    public DataPreload(String key, Object data) {
      this.key = key;
      this.data = data;
    }

    public Map<String, Object> toJson() {
      return ImmutableMap.of(key, data);
    }
  }
}
