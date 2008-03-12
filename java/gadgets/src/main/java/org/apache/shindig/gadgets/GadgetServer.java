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

import org.apache.shindig.gadgets.spec.Feature;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.LocaleSpec;
import org.apache.shindig.gadgets.spec.MessageBundle;
import org.apache.shindig.util.Check;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;

public class GadgetServer {
  private final GadgetServerConfigReader config;

  /**
   * Creates a GadgetServer using the provided configuration.
   *
   * @param configuration
   * @throws IllegalArgumentException When missing required fields aren't set.
   */
  public GadgetServer(GadgetServerConfigReader configuration) {
    Check.notNull(configuration.getExecutor(), "Executor is required.");
    Check.notNull(configuration.getFeatureRegistry(),
        "FeatureRegistry is required.");
    Check.notNull(configuration.getGadgetSpecFetcher(),
        "GadgetSpecFetcher is required");
    Check.notNull(configuration.getMessageBundleFetcher(),
        "MessageBundleFetcher is required.");
    Check.notNull(configuration.getContentFetcher(),
        "ContentFetcher is required.");
    Check.notNull(configuration.getSyndicatorConfig(),
        "SyndicatorConfig is required");
    config = new GadgetServerConfigReader();
    config.copyFrom(configuration);
  }

  /**
   * @return A read-only view of the server's configuration.
   */
  public GadgetServerConfigReader getConfig() {
    return config;
  }

  /**
   * Process a single gadget.
   *
   * @param context
   * @return The processed gadget.
   * @throws GadgetException
   */
  public Gadget processGadget(GadgetContext context) throws GadgetException {
    if (config.getGadgetBlacklist() != null) {
      if (config.getGadgetBlacklist().isBlacklisted(context.getUrl())) {
        throw new GadgetException(GadgetException.Code.BLACKLISTED_GADGET);
      }
    }
    GadgetSpec spec = config.getGadgetSpecFetcher().fetch(context.getUrl(),
        context.getIgnoreCache());
    return createGadgetFromSpec(spec, context);
  }

  /**
   *
   * @param localeSpec
   * @param context
   * @return A new message bundle
   * @throws GadgetException
   */
  private MessageBundle getBundle(LocaleSpec localeSpec, GadgetContext context)
      throws GadgetException {
    MessageBundle bundle;
    bundle = config.getMessageBundleFetcher().fetch(
        localeSpec.getMessages(), context.getIgnoreCache());
    return bundle;
  }

  /**
   * Creates a Gadget from the specified gadget spec and context objects.
   * This performs message bundle substitution as well as feature processing.
   *
   * @param spec
   * @param context
   * @return The final Gadget, ready for consumption.
   * @throws GadgetException
   */
  private Gadget createGadgetFromSpec(GadgetSpec spec, GadgetContext context)
      throws GadgetException {
    LocaleSpec localeSpec
        = spec.getModulePrefs().getLocale(context.getLocale());
    MessageBundle bundle;
    String dir;
    if (localeSpec == null) {
      bundle = MessageBundle.EMPTY;
      dir = "ltr";
    } else {
      if (localeSpec.getMessages() != null &&
          localeSpec.getMessages().toString().length() > 0) {
        bundle = getBundle(localeSpec, context);
      } else {
        bundle = MessageBundle.EMPTY;
      }
      dir = localeSpec.getLanguageDirection();
    }

    Substitutions substituter = new Substitutions();
    substituter.addSubstitutions(
        Substitutions.Type.MESSAGE, bundle.getMessages());
    BidiSubstituter.addSubstitutions(substituter, dir);
    substituter.addSubstitution(Substitutions.Type.MODULE, "ID",
        Integer.toString(context.getModuleId()));
    UserPrefSubstituter.addSubstitutions(
        substituter, spec, context.getUserPrefs());
    spec = spec.substitute(substituter);

    Set<GadgetFeatureRegistry.Entry> features = getFeatures(spec);

    List<JsLibrary> jsLibraries = new LinkedList<JsLibrary>();
    Set<String> done = new HashSet<String>(features.size());

    Map<GadgetFeatureRegistry.Entry, GadgetFeature> tasks
        = new HashMap<GadgetFeatureRegistry.Entry, GadgetFeature>();

    do {
      for (GadgetFeatureRegistry.Entry entry : features) {
        if (!done.contains(entry.getName())
            && done.containsAll(entry.getDependencies())) {
          GadgetFeature feature = entry.getFeature().create();
          jsLibraries.addAll(feature.getJsLibraries(context));
          if (!feature.isJsOnly()) {
            tasks.put(entry, feature);
          }
          done.add(entry.getName());
        }
      }
    } while (done.size() != features.size());

    Gadget gadget = new Gadget(context, spec, bundle, jsLibraries);

    runTasks(gadget, tasks);
    return gadget;
  }

  /**
   * Processes tasks required for this gadget. Attempts to run as many tasks
   * in parallel as possible.
   *
   * @param gadget
   * @param tasks
   * @throws GadgetException
   */
  private void runTasks(Gadget gadget,
      Map<GadgetFeatureRegistry.Entry, GadgetFeature> tasks)
      throws GadgetException {
    CompletionService<GadgetException> processor
        = new ExecutorCompletionService<GadgetException>(config.getExecutor());
    // FeatureTask is OK has a hash key because we want actual instances, not
    // names.
    GadgetContext context = gadget.getContext();
    Set<FeatureTask> pending = new HashSet<FeatureTask>();
    for (Map.Entry<GadgetFeatureRegistry.Entry, GadgetFeature> entry
        : tasks.entrySet()) {
      FeatureTask task = new FeatureTask(entry.getKey().getName(),
          entry.getValue(), gadget, context, entry.getKey().getDependencies());
      pending.add(task);
    }

    Set<FeatureTask> running = new HashSet<FeatureTask>();
    Set<String> done = new HashSet<String>();
    do {
      for (FeatureTask task : pending) {
        if (task.depsDone(done)) {
          pending.remove(task);
          running.add(task);
          processor.submit(task);
        }
      }

      if (running.size() > 0) {
        try {
          Future<GadgetException> future;
          while ((future = processor.take()) != null) {
            GadgetException e = future.get();
            if (future.get() != null) {
              throw future.get();
            }
          }
        } catch (Exception e) {
          throw new GadgetException(
              GadgetException.Code.INTERNAL_SERVER_ERROR, e);
        }
      }

      for (FeatureTask task : running) {
        if (task.isDone()) {
          done.add(task.getName());
          running.remove(task);
        }
      }
    } while (pending.size() > 0 || running.size() > 0);
  }

  /**
   * Constructs a set of dependencies from the given spec.
   *
   * @return The dependencies that are requested in the spec and are also
   *     supported by this server.
   * @throws GadgetException If the spec requires a feature that is not
   *     supported by this server.
   */
  private Set<GadgetFeatureRegistry.Entry> getFeatures(GadgetSpec spec)
      throws GadgetException {
    // Check all required features for the gadget.
    Map<String, Feature> features = spec.getModulePrefs().getFeatures();

    Set<GadgetFeatureRegistry.Entry> dependencies
        = new HashSet<GadgetFeatureRegistry.Entry>(features.size());
    Set<String> unsupported = new HashSet<String>();
    config.getFeatureRegistry().getIncludedFeatures(features.keySet(),
        dependencies, unsupported);

    for (String missing : unsupported) {
      Feature feature = features.get(missing);
      if (feature.getRequired()) {
        throw new GadgetException(GadgetException.Code.UNSUPPORTED_FEATURE,
            missing);
      }
    }

    return dependencies;
  }
}

/**
 * Provides a task for processing non-trival features (anything that is not
 * js only)
 */
class FeatureTask implements Callable<GadgetException> {
  private final Set<String> dependencies;
  public boolean depsDone(Set<String> deps) {
    return deps.containsAll(dependencies);
  }
  private final String name;
  public String getName() {
    return name;
  }
  private final GadgetFeature feature;
  private final Gadget gadget;
  private final GadgetContext context;

  private boolean done = false;
  public boolean isDone() {
    return done;
  }

  public GadgetException call() {
    try {
      feature.process(gadget, context);
      done = true;
      return null;
    } catch (GadgetException e) {
      return e;
    } catch (Exception e) {
      return new GadgetException(GadgetException.Code.INTERNAL_SERVER_ERROR, e);
    }
  }

  public FeatureTask(String name, GadgetFeature feature, Gadget gadget,
      GadgetContext context, Set<String> dependencies) {
    this.name = name;
    this.feature = feature;
    this.gadget = gadget;
    this.context = context;
    this.dependencies = dependencies;
  }
}