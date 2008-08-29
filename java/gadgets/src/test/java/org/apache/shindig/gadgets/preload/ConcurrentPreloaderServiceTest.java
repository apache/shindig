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

import org.apache.shindig.common.testing.TestExecutorService;
import org.apache.shindig.gadgets.Gadget;

import com.google.common.collect.Maps;

import org.junit.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Callable;

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
      = Maps.immutableMap("foo", "bar", "baz", "blah");

  private final TestPreloader preloader = new TestPreloader();
  private final TestPreloader preloader2 = new TestPreloader();

  @Test
  public void preloadSingleService() throws PreloadException {
    preloader.tasks.put(PRELOAD_STRING_KEY,
        new TestPreloadCallable(new DataPreload(PRELOAD_STRING_VALUE)));

    PreloaderService service
        = new ConcurrentPreloaderService(new TestExecutorService(), Arrays.asList(preloader));

    assertEquals(PRELOAD_STRING_VALUE, service.preload(null).getData(PRELOAD_STRING_KEY).toJson());
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
        Arrays.asList(preloader, preloader2));

    Preloads preloads = service.preload(null);

    assertEquals(PRELOAD_STRING_VALUE, preloads.getData(PRELOAD_STRING_KEY).toJson());
    assertEquals(PRELOAD_NUMERIC_VALUE, preloads.getData(PRELOAD_NUMERIC_KEY).toJson());
    assertEquals(PRELOAD_MAP_VALUE, preloads.getData(PRELOAD_MAP_KEY).toJson());
  }

  @Test(expected = PreloadException.class)
  public void exceptionsArePropagated() throws PreloadException {
    preloader.tasks.put(PRELOAD_STRING_KEY, new TestPreloadCallable(null));
    PreloaderService service
        = new ConcurrentPreloaderService(new TestExecutorService(), Arrays.asList(preloader));
    service.preload(null).getData(PRELOAD_STRING_KEY);
  }

  private static class TestPreloader implements Preloader {
    private final Map<String, Callable<PreloadedData>> tasks = Maps.newHashMap();

    public Map<String, Callable<PreloadedData>> createPreloadTasks(Gadget gadget) {
      return tasks;
    }
  }

  private static class TestPreloadCallable implements Callable<PreloadedData> {
    private final PreloadedData preload;

    public TestPreloadCallable(PreloadedData preload) {
      this.preload = preload;
    }

    public PreloadedData call() throws Exception {
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
