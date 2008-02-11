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

import org.apache.shindig.util.Check;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Maintains a registry of all {@code GadgetFeature} types supported by
 * a given Gadget Server installation.
 *
 * To register a feature:
 * GadgetFeatureRegistry registry = // get your global registry
 * registry.register("my-feature", null, new MyFeatureFactory());
 */
public class GadgetFeatureRegistry {
  private final Map<String, Entry> features = new HashMap<String, Entry>();
  private final Map<String, Entry> core =  new HashMap<String, Entry>();

  // Constants used for internal feature names.
  private final static String FEAT_MSG_BUNDLE = "core.msgbundlesubst";
  private final static String FEAT_BIDI = "core.bidisubst";
  private final static String FEAT_MODULE = "core.modulesubst";
  private final static String FEAT_USER_PREF_SUBST = "core.prefsubst";

  private boolean coreDone = false;

  private final static Logger logger
      = Logger.getLogger("org.apache.shindig.gadgets");

  /**
   * Creates the gadget feature registry and loads an initial set of features.
   * Any 'core' features loaded at this point will automatically become
   * dependencies for every other feature.
   * @param featurePath
   */
  public GadgetFeatureRegistry(String featurePath) throws GadgetException {
    registerFeatures(featurePath);
  }

  /**
   * Recursively loads a set of features from a path in the filesystem or
   * from the classpath.
   *
   * @param featurePath Path to the directory that contains feature xml files.
   */
  public void registerFeatures(String featurePath) throws GadgetException {
    if (featurePath == null) {
      return;
    }

    List<String> coreDeps = new LinkedList<String>();
    JsFeatureLoader loader = new JsFeatureLoader();
    List<Entry> jsFeatures = loader.loadFeatures(featurePath, this);

    if (!coreDone) {
      for (Entry entry : jsFeatures) {
        if (entry.name.startsWith("core")) {
          coreDeps.add(entry.getName());
          core.put(entry.getName(), entry);
        }
      }

      // Everything depends on core JS being set up first because in gadget
      // rendering mode, we pre-populate some of the data.
      core.put(FEAT_MSG_BUNDLE,
          register(FEAT_MSG_BUNDLE, coreDeps, new MessageBundleSubstituter()));
      core.put(FEAT_BIDI, register(FEAT_BIDI, coreDeps, new BidiSubstituter()));
      core.put(FEAT_MODULE,
          register(FEAT_MODULE, coreDeps, new ModuleSubstituter()));
      core.put(FEAT_USER_PREF_SUBST,
          register(FEAT_USER_PREF_SUBST, coreDeps, new UserPrefSubstituter()));

      // Make sure non-core features depend on core.
      for (Entry entry : jsFeatures) {
        if (!entry.name.startsWith("core")) {
          entry.deps.addAll(core.values());
        }
      }

      coreDone = true;
    }
  }

  /**
   * Register a {@code GadgetFeature} identified by {@code name} which
   * depends on other {@code GadgetFeature}s listed in {@code deps}
   * completing before this one does.
   *
   * Names are freeform, but it is strongly suggested that they are
   * namespaced, optionally (yet often usefully) in Java package-notation ie.
   * 'com.google.gadgets.skins'.
   *
   * @param name Name of the feature to register, ideally using the conventions
   *     described
   * @param deps List of strings indicating features on which {@code feature}
   *     depends to operate correctly, which need to process the {@code Gadget}
   *     before it does
   * @param feature Class implementing the feature
   */
  public Entry register(String name, List<String> deps,
                        GadgetFeatureFactory feature) {
    // Core entries must come first.
    Entry entry = features.get(name);
    if (entry == null) {
      logger.info("Registering feature: " + name);
      entry = new Entry(name, deps, feature, this);
      if (coreDone) {
        entry.deps.addAll(core.values());
      }
      features.put(name, entry);
      validateFeatureGraph();
    }
    return entry;
  }

  /**
   * Traverses the graph traversed by the registered features, validating
   * that it comprises a directed acyclic graph in which all features'
   * dependencies are provided.
   *
   * If the graph is not acyclic, it cannot be used to create a workflow. If
   * any dependencies are missing, {@code Gadget} rendering may be incomplete.
   */
  private static void validateFeatureGraph() {
    // TODO: ensure that features form a DAG and that all deps are provided
  }

  /**
   * @return All registered features.
   */
  public Map<String, Entry> getAllFeatures() {
    return Collections.unmodifiableMap(features);
  }

  /**
   * Attempts to retrieve all the {@code GadgetFeature} classes specified
   * in the {@code needed} list. Those that are found are returned in
   * {@code resultsFound}, while the names of those that are missing are
   * populated in {@code resultsMissing}.
   * @param needed Set of names identifying features to retrieve
   * @param resultsFound Set of feature entries found
   * @param resultsMissing Set of feature identifiers that could not be found
   * @return True if all features were retrieved
   */
  public boolean getIncludedFeatures(Set<String> needed,
                                     Set<Entry> resultsFound,
                                     Set<String> resultsMissing) {
    resultsFound.clear();
    resultsMissing.clear();
    if (needed.size() == 0) {
      // Shortcut for gadgets that don't have any explicit dependencies.
      resultsFound.addAll(core.values());
      return true;
    }
    for (String featureName : needed) {
      Entry entry = features.get(featureName);
      if (entry == null) {
        resultsMissing.add(featureName);
      } else {
        addEntryToSet(resultsFound, entry);
      }
    }
    return resultsMissing.size() == 0;
  }

  /**
   * Recursively add all dependencies.
   * @param results
   * @param entry
   */
  private void addEntryToSet(Set<Entry> results, Entry entry) {
    for (Entry dep : entry.deps) {
      addEntryToSet(results, dep);
    }
    results.add(entry);
  }

  /**
   * Fetches an entry by name.
   * @param name
   * @return The entry, or null if it does not exist.
   */
  Entry getEntry(String name) {
    return features.get(name);
  }

  public static class NoOpFeature implements GadgetFeature {
    public void prepare(GadgetView gadget, GadgetContext context,
        Map<String, String> params) {
    }
    public void process(Gadget gadget, GadgetContext context,
        Map<String, String> params) {
    }
  }

  /**
   * Ties together a {@code GadgetFeature} with its name and dependencies.
   */
  public static class Entry {
    private final String name;
    private final Set<Entry> deps;
    private final Set<Entry> readDeps;
    private final GadgetFeatureFactory feature;

    private Entry(String name,
                  List<String> deps,
                  GadgetFeatureFactory feature,
                  GadgetFeatureRegistry registry)
        throws IllegalStateException {
      this.name = name;
      this.deps = new HashSet<Entry>();
      this.readDeps = Collections.unmodifiableSet(this.deps);
      if (deps != null) {
        for (String dep : deps) {
          Entry entry = registry.getEntry(dep);
          Check.notNull(entry, "Dependency " + dep + " is not registered.");
          this.deps.add(entry);
        }
      }
      this.feature = feature;
    }

    /**
     * @return Name identifier
     */
    public String getName() {
      return name;
    }

    /**
     * @return List of identifiers on which feature depends
     */
    public Set<Entry> getDependencies() {
      return readDeps;
    }

    @Override
    public boolean equals(Object rhs) {
      if (rhs == this) {
        return true;
      }
      if (rhs instanceof Entry) {
        Entry entry = (Entry)rhs;
        return name.equals(entry.name);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return name.hashCode();
    }

    /**
     * @return Class implementing the feature
     */
    public GadgetFeatureFactory getFeature() {
      return feature;
    }
  }
}
