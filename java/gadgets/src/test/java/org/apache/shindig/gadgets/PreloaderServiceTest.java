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
package org.apache.shindig.gadgets;

import org.apache.shindig.common.testing.TestExecutorService;

import com.google.common.collect.Maps;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * Tests for PreloaderService.
 */
public class PreloaderServiceTest {
  private static final String PRELOAD_STRING_KEY = "key a";
  private static final String PRELOAD_NUMERIC_KEY = "key b";
  private static final String PRELOAD_MAP_KEY = "key c";
  private static final String PRELOAD_BEAN_KEY = "key d";
  private static final String PRELOAD_STRING_VALUE = "Some random string";
  private static final Integer PRELOAD_NUMERIC_VALUE = 5;
  private static final Map<String, String> PRELOAD_MAP_VALUE
      = Maps.immutableMap("foo", "bar", "baz", "blah");
  private static final Object PRELOAD_BEAN_VALUE = new Object() {
    public String getFoo() {
      return "foo";
    }

    public String getBar() {
      return "bar";
    }
  };

  private final TestPreloader preloader = new TestPreloader();
  private final TestPreloader preloader2 = new TestPreloader();

  @Test
  public void preloadSingleService() throws Exception {
    preloader.tasks.put(PRELOAD_STRING_KEY,
        new TestPreloadCallable(new DataPreload(PRELOAD_STRING_VALUE)));

    PreloaderService service
        = new PreloaderService(new TestExecutorService(), Arrays.asList(preloader));

    assertEquals(PRELOAD_STRING_VALUE,
                (String)service.preload(null).get(PRELOAD_STRING_KEY).get().toJson());
  }

  @Test
  public void preloadMultipleServices() throws Exception {
    preloader.tasks.put(PRELOAD_STRING_KEY,
        new TestPreloadCallable(new DataPreload(PRELOAD_STRING_VALUE)));

    preloader.tasks.put(PRELOAD_NUMERIC_KEY,
        new TestPreloadCallable(new DataPreload(PRELOAD_NUMERIC_VALUE)));

    preloader2.tasks.put(PRELOAD_MAP_KEY,
        new TestPreloadCallable(new DataPreload(PRELOAD_MAP_VALUE)));

    preloader2.tasks.put(PRELOAD_BEAN_KEY,
        new TestPreloadCallable(new DataPreload(PRELOAD_BEAN_VALUE)));

    PreloaderService service
        = new PreloaderService(new TestExecutorService(), Arrays.asList(preloader, preloader2));

    Map<String, Future<Preload>> preloads = service.preload(null);

    assertEquals(PRELOAD_STRING_VALUE,
                 (String) preloads.get(PRELOAD_STRING_KEY).get().toJson());
    assertEquals(PRELOAD_NUMERIC_VALUE,
                 (Integer) preloads.get(PRELOAD_NUMERIC_KEY).get().toJson());
    assertEquals(PRELOAD_MAP_VALUE,
                 (Map) preloads.get(PRELOAD_MAP_KEY).get().toJson());
    assertEquals(PRELOAD_BEAN_VALUE, preloads.get(PRELOAD_BEAN_KEY).get().toJson());
  }

  private static class TestPreloader implements Preloader {
    private final Map<String, Callable<Preload>> tasks = Maps.newHashMap();

    public Map<String, Callable<Preload>> createPreloadTasks(Gadget gadget) {
      return tasks;
    }
  }

  private static class TestPreloadCallable implements Callable<Preload> {
    private final Preload preload;

    public TestPreloadCallable(Preload preload) {
      this.preload = preload;
    }

    public Preload call() {
      return preload;
    }
  }

  private static class DataPreload implements Preload {
    private final Object data;

    public DataPreload(Object data) {
      this.data = data;
    }

    public Object toJson() {
      return data;
    }
  }
}
