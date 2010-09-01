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

import com.google.common.base.Preconditions;
import org.apache.shindig.common.cache.Cache;
import org.apache.shindig.common.cache.CacheProvider;
import org.apache.shindig.common.util.ResourceLoader;

import com.google.common.collect.MapMaker;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.ConfigurationFactory;
import net.sf.ehcache.management.ManagementService;

import javax.management.MBeanServer;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Cache interface based on ehcache
 * @see http://www.ehcache.org
 */
public class EhCacheCacheProvider implements CacheProvider {
  private static final Logger LOG = Logger.getLogger(EhCacheCacheProvider.class.getName());
  private final CacheManager cacheManager;
  private final ConcurrentMap<String, Cache<?, ?>> caches = new MapMaker().makeMap();

  @Inject
  public EhCacheCacheProvider(@Named("shindig.cache.ehcache.config") String configPath,
                              @Named("shindig.cache.ehcache.jmx.enabled") boolean jmxEnabled,
                              @Named("shindig.cache.ehcache.jmx.stats") boolean withCacheStats)
      throws IOException {
    cacheManager = new CacheManager(getConfiguration(configPath));
    create(jmxEnabled, withCacheStats);
  }

  /**
   * Read the cache configuration from the specified resource.
   * This function is intended to be overrideable to allow for programmatic
   * cache configuration.
   * @param configPath
   * @return Configuration
   * @throws IOException
   */
  protected Configuration getConfiguration(String configPath) throws IOException {
    InputStream configStream = ResourceLoader.open(configPath);
    return ConfigurationFactory.parseConfiguration(configStream);
  }

  public void create(boolean jmxEnabled, boolean withCacheStats) {
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
    if (jmxEnabled) {
      MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
      ManagementService.registerMBeans(cacheManager, mBeanServer, true, true, true, withCacheStats);
    }
  }

  /**
   * perform a shutdown
   */
  public void shutdown() {
    cacheManager.shutdown();
  }

  @SuppressWarnings("unchecked")
  public <K, V> Cache<K, V> createCache(String name) {
    if (!caches.containsKey(Preconditions.checkNotNull(name))) {
      if (LOG.isLoggable(Level.FINE)) {
        LOG.fine("Creating cache named " + name);
      }
      caches.putIfAbsent(name, new EhConfiguredCache<K, V>(name, cacheManager));
    }
    return (Cache<K, V>) caches.get(Preconditions.checkNotNull(name));
  }
}
