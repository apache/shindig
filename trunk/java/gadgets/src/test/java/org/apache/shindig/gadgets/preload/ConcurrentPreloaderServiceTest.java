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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import org.apache.shindig.common.testing.ImmediateExecutorService;
import org.apache.shindig.gadgets.Gadget;
import org.junit.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

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

  @Test
  public void preloadSingleService() throws Exception {
    preloader.tasks.add(new TestPreloadCallable(
        new DataPreload(PRELOAD_STRING_KEY, PRELOAD_STRING_VALUE)));

    PreloaderService service = new ConcurrentPreloaderService(new ImmediateExecutorService(),
        preloader);

    Collection<PreloadedData> preloads = service.preload((Gadget) null);

    Collection<Object> preloaded = getAll(preloads);
    assertEquals(ImmutableMap.of(PRELOAD_STRING_KEY, PRELOAD_STRING_VALUE),
        preloaded.iterator().next());
  }

  /** Load all the data out of a preloads object */
  private Collection<Object> getAll(Collection<PreloadedData> preloads) throws PreloadException {
    List<Object> list = Lists.newArrayList();
    for (PreloadedData preloadCallable : preloads) {
      list.addAll(preloadCallable.toJson());
    }

    return list;
  }

  @Test
  public void preloadMultipleServices() throws PreloadException {
    preloader.tasks.add(new TestPreloadCallable(
        new DataPreload(PRELOAD_STRING_KEY, PRELOAD_STRING_VALUE)));

    preloader.tasks.add(new TestPreloadCallable(
        new DataPreload(PRELOAD_NUMERIC_KEY, PRELOAD_NUMERIC_VALUE)));

    preloader.tasks.add(new TestPreloadCallable(
        new DataPreload(PRELOAD_MAP_KEY, PRELOAD_MAP_VALUE)));

    PreloaderService service = new ConcurrentPreloaderService(new ImmediateExecutorService(),
        preloader);

    Collection<PreloadedData> preloads =
      service.preload((Gadget) null);

    Collection<Object> preloaded = getAll(preloads);
    assertEquals(ImmutableList.<Object>of(
        ImmutableMap.of(PRELOAD_STRING_KEY, PRELOAD_STRING_VALUE),
        ImmutableMap.of(PRELOAD_NUMERIC_KEY, PRELOAD_NUMERIC_VALUE),
        ImmutableMap.of(PRELOAD_MAP_KEY, PRELOAD_MAP_VALUE)), preloaded);
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
        preloader);

    service.preload((Gadget) null);

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
        preloader);

    service.preload((Gadget) null);

    assertSame("Single request not run in current thread",
        Thread.currentThread(), callable.executedThread);
  }

  private static class TestPreloader implements Preloader {
    protected final Collection<Callable<PreloadedData>> tasks = Lists.newArrayList();

    protected TestPreloader() {
    }

    public Collection<Callable<PreloadedData>> createPreloadTasks(
        Gadget gadget) {
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

    public Collection<Object> toJson() {
      return ImmutableList.of((Object) ImmutableMap.of(key, data));
    }
  }
}
