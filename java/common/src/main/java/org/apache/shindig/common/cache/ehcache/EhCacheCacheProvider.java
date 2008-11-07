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
package org.apache.shindig.common.cache.ehcache;

import org.apache.shindig.common.cache.Cache;
import org.apache.shindig.common.cache.CacheProvider;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.management.ManagementService;

import java.lang.management.ManagementFactory;
import java.net.URL;
import java.util.Map;
import java.util.logging.Logger;

import javax.management.MBeanServer;

public class EhCacheCacheProvider implements CacheProvider {
  private final Logger LOG = Logger.getLogger(EhCacheCacheProvider.class.getName());
  private final CacheManager cacheManager;
  private final Map<String, Cache<?, ?>> caches = Maps.newConcurrentHashMap();

  @Inject
  public EhCacheCacheProvider(@Named("cache.config") String configPath,
                              @Named("cache.jmx.stats") boolean withCacheStats) {
    URL url = getClass().getResource(configPath);
    cacheManager = new CacheManager(url);
    create(withCacheStats);
  }

  public void create(boolean withCacheStats) {
    /*
     * Add in a shutdown hook
     */
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        try {
          shutdown();
        } catch (Throwable t) {
          // I really do want to swallow this, and make the shutdown clean for
          // others
        }
      }
    });

    // register the cache manager with JMX
    MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
    ManagementService.registerMBeans(cacheManager, mBeanServer, true, true, true, withCacheStats);
  }

  /**
   * perform a shutdown
   */
  public void shutdown() {
    cacheManager.shutdown();
  }

  @SuppressWarnings("unchecked")
  public <K, V> Cache<K, V> createCache(String name) {
    if (name == null) {
      LOG.info("Creating anonymous cache");
      return new EhConfiguredCache<K, V>(name, cacheManager);
    } else {
      Cache<K, V> cache = (Cache<K, V>) caches.get(name);
      if (cache == null) {
        LOG.info("Creating cache named " + name);
        cache = new EhConfiguredCache<K, V>(name, cacheManager);
        caches.put(name, cache);
      }
      return cache;
    }
  }

}
