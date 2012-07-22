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
package org.apache.shindig.gadgets.features;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import org.apache.shindig.common.cache.Cache;
import org.apache.shindig.common.cache.CacheProvider;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.util.TimeSource;
import org.apache.shindig.gadgets.GadgetException;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Maps;

import java.util.List;

/**
 * Helper classes extending FeatureRegistry for use by test classes.
 * Includes several helpers to load (fake) feature.xml and JS files from
 * memory (or tempfile), but does not change FeatureRegistry logic.
 */
public class TestFeatureRegistry extends FeatureRegistry {
  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {
    private static String RESOURCE_BASE_PATH = "/resource/base/path";
    private static int resourceIdx = 0;

    private final ResourceMock resourceMock;
    private final List<String> featureFiles;

    private Builder() {
      this.resourceMock = new ResourceMock();
      this.featureFiles = Lists.newLinkedList();
    }

    public TestFeatureRegistry build(String useFeature) throws GadgetException {
      return new TestFeatureRegistry(
          new TestFeatureResourceLoader(resourceMock),
          new TestCacheProvider(),
          useFeature);
    }

    public TestFeatureRegistry build() throws GadgetException {
      return build(Joiner.on(",").join(featureFiles));
    }

    public Builder addFeatureFile(String featureFile) {
      featureFiles.add(featureFile);
      return this;
    }

    /* Expectation methods and helpers */
    public Uri expectResource(String content) {
      return expectResource(content, ".xml");
    }

    public Uri expectResource(String content, String suffix) {
      Uri res = makeResourceUri(suffix);
      resourceMock.put(res.getPath(), content);
      return res;
    }

    private static Uri makeResourceUri(String suffix) {
      return Uri.parse("res://" + RESOURCE_BASE_PATH + "/file" + (++resourceIdx) + suffix);
    }
  }

  /* Actual class contents here */
  private final TestFeatureResourceLoader resourceLoader;
  private final TestCacheProvider cacheProvider;
  private TestFeatureRegistry(
      TestFeatureResourceLoader resourceLoader,
      TestCacheProvider cacheProvider,
      String featureFiles) throws GadgetException {
    super(resourceLoader, cacheProvider, ImmutableList.<String>of(featureFiles),
        new DefaultFeatureFileSystem());
    this.resourceLoader = resourceLoader;
    this.cacheProvider = cacheProvider;
  }

  public Map<String, String> getLastAttribs() {
    return Collections.unmodifiableMap(resourceLoader.lastAttribs);
  }

  @SuppressWarnings("unchecked")
  public Cache<String, LookupResult> getLookupCache() {
    Cache<?, ?> cacheEntry = cacheProvider.caches.get(FeatureRegistry.CACHE_NAME);
    if (cacheEntry == null) {
      return null;
    }
    return (Cache<String, LookupResult>)cacheEntry;
  }

  private static class TestFeatureResourceLoader extends FeatureResourceLoader {
    private final ResourceMock resourceMock;
    private Map<String, String> lastAttribs;

    private TestFeatureResourceLoader(ResourceMock resourceMock) {
      super(null, new TimeSource(), new DefaultFeatureFileSystem());
      this.resourceMock = resourceMock;
    }

    @Override
    public FeatureResource load(Uri uri, Map<String, String> attribs) throws GadgetException {
      lastAttribs = ImmutableMap.copyOf(attribs);
      return super.load(uri, attribs);
    }

    @Override
    public String getResourceContent(String resource) throws IOException {
      return resourceMock.get(resource);
    }
  }

  private static class ResourceMock {
    private final Map<String, String> resourceMap;

    private ResourceMock() {
      this.resourceMap = Maps.newHashMap();
    }

    private void put(String key, String value) {
      resourceMap.put(clean(key), value);
    }

    private String get(String key) throws IOException {
      key = clean(key);
      if (!resourceMap.containsKey(key)) {
        throw new IOException("Missing resource: " + key);
      }
      return resourceMap.get(key);
    }

    private String clean(String key) {
      // Resource loading doesn't support leading '/'
      return key.startsWith("/") ? key.substring(1) : key;
    }
  }

  // TODO: generalize the below into common classes
  private static class TestCacheProvider implements CacheProvider {
    private final Map<String, Cache<?, ?>> caches = new MapMaker().makeMap();

    @SuppressWarnings("unchecked")
    public <K, V> Cache<K, V> createCache(String name) {
      Cache<K, V> cache = (Cache<K, V>)caches.get(name);
      if (cache == null) {
        cache = new MapCache<K, V>();
        caches.put(name, cache);
      }
      return cache;
    }
  }

  private static class MapCache<K, V> implements Cache<K, V> {
    private final Map<K, V> cache = new MapMaker().makeMap();

    public void addElement(K key, V value) {
      cache.put(key, value);
    }

    public long getCapacity() {
      // Memory-bounded.
      return Integer.MAX_VALUE;
    }

    public V getElement(K key) {
      return cache.get(key);
    }

    public long getSize() {
      return cache.size();
    }

    public V removeElement(K key) {
      return cache.get(key);
    }

  }
}
