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
package org.apache.shindig.gadgets;

import org.apache.shindig.gadgets.http.HttpFetcher;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.MapMaker;
import com.google.common.base.Preconditions;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Maintains a registry of all {@code GadgetFeature} types supported by
 * a given Gadget Server installation.
 *
 * To register a feature:
 * GadgetFeatureRegistry registry = // get your global registry
 * registry.register("my-feature", null, new MyFeatureFactory());
 */
@Singleton
public class GadgetFeatureRegistry {
  private final Map<String, GadgetFeature> features;
  private final Map<String, GadgetFeature> core;

  // Caches the transitive dependencies to enable faster lookups.
  final Map<Set<String>, Collection<GadgetFeature>> cache = new MapMaker().makeMap();

  private boolean locked = false;

  private final static Logger logger
      = Logger.getLogger("org.apache.shindig.gadgets");

  /**
   * Creates a new feature registry and loads the specified features.
   *
   * @param httpFetcher HttpFetcher for retrieving content
   * @param featureFiles Injected from shindig.features.default
   * @throws GadgetException
   */
  @Inject
  public GadgetFeatureRegistry(@Named("shindig.features.default") String featureFiles,
      HttpFetcher httpFetcher) throws GadgetException {

    features = Maps.newHashMap();
    core = Maps.newHashMap();

    if (featureFiles != null) {
      JsFeatureLoader loader = new JsFeatureLoader(httpFetcher);
      loader.loadFeatures(featureFiles, this);
    }
  }

  /**
   * Lock the registry, no longer allow the registration of new entries.
   * A locked registry can never be unlocked.
   */
  private void lock() {
    locked = true;
  }

  /**
   * Returns the locking state of the registry.
   * @return true if the registry is locked.
   */
  public boolean isLocked() {
    return locked;
  }

  /**
   * Register a {@code GadgetFeature}.
   *
   * @param feature Class implementing the feature.
   */
  public void register(GadgetFeature feature) {
    Preconditions.checkState(!isLocked(), "registry is locked and can no longer be changed!");

    if (logger.isLoggable(Level.FINE)) logger.fine("Registering feature: " + feature.getName());

    if (isCore(feature)) {
      core.put(feature.getName(), feature);
      for (GadgetFeature feat : features.values()) {
        feat.addDependency(feature.getName());
      }
    } else {
      feature.addDependencies(core.keySet());
    }
    features.put(feature.getName(), feature);
  }

  /**
   * @param feature the GadgetFeature to test
   * @return True if the entry is "core" (a dependency of all other features)
   */
  private boolean isCore(GadgetFeature feature) {
    return feature.getName().startsWith("core");
  }

  /**
   * @return All registered features.
   */
  public Collection<GadgetFeature> getAllFeatures() {
    return Collections.unmodifiableCollection(features.values());
  }

  /**
   * @return All {@code GadgetFeature} objects necessary for {@code needed} in
   *     graph-dependent order.
   */
  public Collection<GadgetFeature> getFeatures(Collection<String> needed) {
    return getFeatures(needed, null);
  }

  /**
   * @param needed All features requested by the gadget.
   * @param unsupported Populated with any unsupported features.
   * @return All {@code GadgetFeature} objects necessary for {@code needed} in
   *     graph-dependent order.
   */
  public Collection<GadgetFeature> getFeatures(Collection<String> needed,
                                               Collection<String> unsupported) {
    lock();

    Set<String> neededSet;
    neededSet = needed.isEmpty() ? core.keySet() : ImmutableSet.copyOf(needed);

    // We use the cache only for situations where all needed are available.
    // if any are missing, the result won't be cached.
    Collection<GadgetFeature> libCache = cache.get(neededSet);
    if (libCache != null) {
      return libCache;
    }
    List<GadgetFeature> ret = Lists.newLinkedList();
    populateDependencies(neededSet, ret);
    // Fill in anything that was optional but missing. These won't be cached.
    if (unsupported != null) {
      for (String feature : neededSet) {
        if (!features.containsKey(feature)) {
          unsupported.add(feature);
        }
      }
    }
    if (unsupported == null || unsupported.isEmpty()) {
      cache.put(neededSet, Collections.unmodifiableList(ret));
      logger.info("Added to cache. Size is now: " + cache.size());
    }
    return ret;
  }

  /**
   * Recursively populates {@code libraries} with libraries from dependent
   * features. This ensures that features will always be loaded in the order
   * that they are declared.
   */
  private void populateDependencies(Collection<String> needed,
      List<GadgetFeature> deps) {
    for (String feature : needed) {
      GadgetFeature feat = features.get(feature);
      if (feat != null && !deps.contains(feat)) {
        populateDependencies(feat.getDependencies(), deps);
        deps.add(feat);
      }
    }
  }
}
