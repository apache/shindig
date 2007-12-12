/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.shindig.gadgets;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Maintains a registry of all {@code GadgetFeature} types supported by
 * a given Gadget Server installation.
 *
 * To register a feature, simply create a static initialization block in its
 * class definition:
 * <pre>
 *   static {
 *     GadgetFeatureRegistry.register("my.feature.name",
 *                                    { "my.dep1", "my.dep2" },
 *                                    MyFeature.class);
 *   }
 * </pre>
 */
public class GadgetFeatureRegistry {
  private static final Map<String, Entry> features =
      new HashMap<String, Entry>();
  private static final List<Entry> core =
      new LinkedList<Entry>();

  // Initialization of core components, providing a minimal base context
  // in which all Gadgets operate. This set should be kept as minimal as
  // possible. Anything added as registerCore will automatically become a
  // dependency of every other feature.
  static {
    // Substitution jobs are not order-dependent, because the actual order that
    // they are evaluated in is determined in Substitutions.java. The order
    // defined here is not important.
    registerCore("core.msgbundlesubst", null, MessageBundleSubstituter.class);
    registerCore("core.bidisubst", null, BidiSubstituter.class);
    registerCore("core.modulesubst", null, ModuleSubstituter.class);
    registerCore("core.userprefsubst", null, UserPrefSubstituter.class);

    // Core JS loading.
    registerCore("core.js", null, CoreJsFeature.class);

    // These are all satisfied by core.
    register("setprefs", null, NoOpFeature.class);
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
   * described
   * @param deps List of strings indicating features on which {@code feature}
   * depends to operate correctly, which need to process the {@code Gadget}
   * before it does
   * @param feature Class implementing the feature
   */
  public static void register(String name,
                              String[] deps,
                              Class<? extends GadgetFeature> feature) {
    // Core entries must come first.
    Entry entry = new Entry(name, deps, feature);
    for (Entry coreEntry : core) {
      entry.deps.add(coreEntry.getName());
    }
    features.put(name, entry);
    validateFeatureGraph();
  }

  /**
   * Registers a {@code GadgetFeature} which is <i>always</i> run when
   * processing a {@code Gadget}, and on which all other features
   * implicitly depend. Use of this mechanism should be as sparing as possible
   * to optimize performance.
   *
   * @param deps
   * @param cap
   */
  private static void registerCore(String name,
                                   String[] deps,
                                   Class<? extends GadgetFeature> cap) {
    core.add(new Entry(name, deps, cap));
    validateFeatureGraph();
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
   * Attempts to retrieve all the {@code GadgetFeature} classes specified
   * in the {@code needed} list. Those that are found are returned in
   * {@code resultsFound}, while the names of those that are missing are
   * populated in {@code resultsMissing}.
   * @param needed List of names identifying features to retrieve
   * @param resultsFound List of feature entries found
   * @param resultsMissing List of feature identifiers that could not be found
   * @return True if all features were retrieved
   */
  public static boolean getIncludedFeatures(List<String> needed,
                                            List<Entry> resultsFound,
                                            List<String> resultsMissing) {
    resultsFound.clear();
    resultsMissing.clear();
    resultsFound.addAll(core);
    for (String featureName : needed) {
      Entry entry = features.get(featureName);
      if (entry == null) {
        resultsMissing.add(featureName);
      }
      resultsFound.add(entry);
    }
    return resultsMissing.size() == 0;
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
    private final Set<String> deps;
    private final Class<? extends GadgetFeature> feature;

    private Entry(String name,
                  String[] deps,
                  Class<? extends GadgetFeature> feature) {
      this.name = name;
      this.deps = new HashSet<String>();
      if (deps != null) {
        this.deps.addAll(Arrays.asList(deps));
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
    public List<String> getDependencies() {
      return new LinkedList<String>(deps);
    }

    /**
     * @return Class implementing the feature
     */
    public Class<? extends GadgetFeature> getFeature() {
      return feature;
    }
  }
}
