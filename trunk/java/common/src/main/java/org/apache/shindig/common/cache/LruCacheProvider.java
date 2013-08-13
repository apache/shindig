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
package org.apache.shindig.common.cache;

import com.google.common.base.Preconditions;
import com.google.common.collect.MapMaker;
import com.google.inject.ConfigurationException;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.shindig.common.logging.i18n.MessageKeys;

/**
 * A cache provider that always produces LRU caches.
 *
 * LRU cache sizes can be configured by specifying property names in the form
 *
 * shindig.cache.lru.<cache name>.capacity=foo
 *
 * The default value is expected under shindig.cache.lru.default.capacity
 *
 * An in memory LRU cache only scales so far. For a production-worthy cache, use
 * {@code EhCacheCacheProvider}.
 */
public class LruCacheProvider implements CacheProvider {
  private static final String classname = LruCacheProvider.class.getName();
  private static final Logger LOG = Logger.getLogger(classname, MessageKeys.MESSAGES);
  private final int defaultCapacity;
  private final Injector injector;
  private final Map<String, Cache<?, ?>> caches = new MapMaker().makeMap();

  @Inject
  public LruCacheProvider(Injector injector,
      @Named("shindig.cache.lru.default.capacity") int defaultCapacity) {
    this.injector = injector;
    this.defaultCapacity = defaultCapacity;
  }

  public LruCacheProvider(int capacity) {
    this(null, capacity);
  }

  private int getCapacity(String name) {
    if (injector != null && name != null) {
      String key = "shindig.cache.lru." + name + ".capacity";
      Key<String> guiceKey = Key.get(String.class, Names.named(key));
      try {
        if (injector.getBinding(guiceKey) == null) {
          if (LOG.isLoggable(Level.WARNING)) {
            LOG.logp(Level.WARNING, classname, "getCapacity(String name)", MessageKeys.LRU_CAPACITY,new Object[] {"No",name});
          }
        } else {
          String value = injector.getInstance(guiceKey);
          try {
            return Integer.parseInt(value);
          } catch (NumberFormatException e) {
            if (LOG.isLoggable(Level.WARNING)) {
              LOG.logp(Level.WARNING, classname, "getCapacity(String name)", MessageKeys.LRU_CAPACITY,new Object[] {"Invalid",name});
            }
          }
        }
      } catch ( ConfigurationException e ) {
        return defaultCapacity;
      }
    }
    return defaultCapacity;
  }

  @SuppressWarnings("unchecked")
  public <K, V> Cache<K, V> createCache(String name) {
    int capacity = getCapacity(Preconditions.checkNotNull(name));
    Cache<K, V> cache = (Cache<K, V>) caches.get(name);
    if (cache == null) {
      if (LOG.isLoggable(Level.FINE)) {
        LOG.fine("Creating cache named " + name);
      }
      cache = new LruCache<K, V>(capacity);
      caches.put(name, cache);
    }
    return cache;
  }
}
