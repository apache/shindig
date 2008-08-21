/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.gadgets.http;

import org.apache.shindig.common.cache.Cache;
import org.apache.shindig.common.cache.CacheProvider;
import org.apache.shindig.common.cache.LruCache;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * Simple cache of HttpResponse. Uses WeakHashMap for memory management
 */
public class BasicHttpCache extends AbstractHttpCache {

  private final Cache<String, HttpResponse> cache;

  @Override
  protected HttpResponse getResponseImpl(String key) {
    return cache.getElement(key);
  }

  @Override
  protected void addResponseImpl(String key, HttpResponse response) {
    cache.addElement(key, response);
  }

  @Override
  protected HttpResponse removeResponseImpl(String key) {
    return cache.removeElement(key);
  }

  @Inject
  public BasicHttpCache(CacheProvider cacheProvider, @Named("shindig.cache.capacity") int capacity) {
    cache = cacheProvider.createCache(capacity);
  }
}
