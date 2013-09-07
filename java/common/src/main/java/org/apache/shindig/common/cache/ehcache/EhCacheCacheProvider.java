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
import com.google.common.base.Strings;

import org.apache.shindig.common.cache.Cache;
import org.apache.shindig.common.cache.CacheProvider;
import org.apache.shindig.common.servlet.GuiceServletContextListener;
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
 * Cache interface based on ehcache.
 *
 * @see <a href="http://www.ehcache.org">http://www.ehcache.org</a>
 */
public class EhCacheCacheProvider implements CacheProvider,
        GuiceServletContextListener.CleanupCapable {
  private static final Logger LOG = Logger.getLogger(EhCacheCacheProvider.class.getName());
  private final CacheManager cacheManager;
  private final ConcurrentMap<String, Cache<?, ?>> caches = new MapMaker().makeMap();

  /**
   * @param configPath
   *          the path to the EhCache configuration file
   * @param filterPath
   *          the path to the EhCache SizeOf engine filter file
   * @param jmxEnabled
   *          true if JMX should be enabled for EhCache, false otherwise
   * @param withCacheStats
   *          true if cache statistics should be enabled globally, false otherwise
   * @param cleanupHandler
   *          cleanup handler with which to register to ensure proper cache shutdown via
   *          {@link #cleanup()}
   * @throws IOException
   *           if there was an issue parsing the given configuration
   */
  @Inject
  public EhCacheCacheProvider(@Named("shindig.cache.ehcache.config") String configPath,
                              @Named("shindig.cache.ehcache.sizeof.filter") String filterPath,
                              @Named("shindig.cache.ehcache.jmx.enabled") boolean jmxEnabled,
                              @Named("shindig.cache.ehcache.jmx.stats") boolean withCacheStats,
                              GuiceServletContextListener.CleanupHandler cleanupHandler)
      throws IOException {
    // TODO: Setting this system property is currently the only way to hook in our own filter
    // https://jira.terracotta.org/jira/browse/EHC-938
    // https://jira.terracotta.org/jira/browse/EHC-924
    // Remove res:// and file:// prefixes.  EhCache can't understand them.
    String normalizedFilterPath = filterPath.replaceFirst(ResourceLoader.RESOURCE_PREFIX, "");
    normalizedFilterPath = normalizedFilterPath.replaceFirst(ResourceLoader.FILE_PREFIX, "");
    System.setProperty("net.sf.ehcache.sizeof.filter", normalizedFilterPath);

    // If ehcache.disk.store.dir isn't already set, set it to java.io.tmpdir.
    // See http://ehcache.org/documentation/user-guide/storage-options#diskstore-configuration-element
    String diskStoreProperty = System.getProperty("ehcache.disk.store.dir");
    if (Strings.isNullOrEmpty(diskStoreProperty)) {
      System.setProperty("ehcache.disk.store.dir", System.getProperty("java.io.tmpdir"));
    }

    cacheManager = CacheManager.newInstance(getConfiguration(configPath));
    create(jmxEnabled, withCacheStats);
    cleanupHandler.register(this);
  }

  /**
   * Read the cache configuration from the specified resource.
   *
   * This function is intended to be overrideable to allow for programmatic cache configuration.
   *
   * @param configPath
   *          the path to the configuration file
   * @return Configuration the configuration object parsed from the configuration file
   * @throws IOException
   *           if there was an error parsing the given configuration
   */
  protected Configuration getConfiguration(String configPath) throws IOException {
    InputStream configStream = ResourceLoader.open(configPath);
    return ConfigurationFactory.parseConfiguration(configStream);
  }

  private void create(boolean jmxEnabled, boolean withCacheStats) {
    if (jmxEnabled) {
      // register the cache manager with JMX
      MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
      ManagementService.registerMBeans(cacheManager, mBeanServer, true, true, true, withCacheStats);
    }
  }

  /**
   * Perform a shutdown of the underlying cache manager.
   */
  public void cleanup() {
    cacheManager.shutdown();
  }

  /**
   * {@inheritDoc}
   */
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
