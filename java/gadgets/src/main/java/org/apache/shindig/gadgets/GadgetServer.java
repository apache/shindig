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

import org.apache.shindig.util.Check;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.logging.Logger;
import java.util.logging.Level;

public class GadgetServer {
  private final GadgetServerConfig config;

  private static final Logger logger
      = Logger.getLogger("org.apache.shindig.gadgets");

  /**
   * Creates a GadgetServer without a config.
   *
   * @deprecated Replaced by {@link #GadgetServer(GadgetServerConfigReader)}.
   * @param executor
   */
  @Deprecated
  public GadgetServer(Executor executor) {
    config = new GadgetServerConfig();
    config.setExecutor(executor);
  }

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
    Check.notNull(configuration.getSpecCache(), "SpecCache is required.");
    Check.notNull(configuration.getMessageBundleCache(),
        "MessageBundleCache is required.");
    Check.notNull(configuration.getContentFetcher(),
        "ContentFetcher is required.");

    config = new GadgetServerConfig();
    config.copyFrom(configuration);
  }

  /**
   * @deprecated Replaced by {@link #GadgetServer(GadgetServerConfigReader)}.
   */
  @Deprecated
  public void setSpecCache(GadgetDataCache<GadgetSpec> specCache) {
    config.setSpecCache(specCache);
  }

  /**
   * @deprecated Replaced by {@link #GadgetServer(GadgetServerConfigReader)}.
   */
  @Deprecated
  public void setMessageBundleCache(GadgetDataCache<MessageBundle> cache) {
    config.setMessageBundleCache(cache);
  }

  /**
   * @deprecated Replaced by {@link #GadgetServer(GadgetServerConfigReader)}.
   */
  @Deprecated
  public void setContentFetcher(RemoteContentFetcher fetcher) {
    config.setContentFetcher(fetcher);
  }

  /**
   * @deprecated Replaced by {@link #GadgetServer(GadgetServerConfigReader)}.
   */
  @Deprecated
  public void setGadgetFeatureRegistry(GadgetFeatureRegistry registry) {
    config.setFeatureRegistry(registry);
  }

  /**
   * @deprecated Replaced by {@link #GadgetServer(GadgetServerConfigReader)}.
   */
  @Deprecated
  public void setGadgetBlacklist(GadgetBlacklist gadgetBlacklist) {
    config.setGadgetBlacklist(gadgetBlacklist);
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
   * @param gadgetId
   * @param userPrefs
   * @param locale
   * @param rctx
   * @param options
   * @return The processed gadget.
   * @throws GadgetProcessException
   */
  public Gadget processGadget(GadgetView.ID gadgetId,
                              UserPrefs userPrefs,
                              Locale locale,
                              RenderingContext rctx,
                              ProcessingOptions options)
      throws GadgetProcessException {
    // TODO: Remove dep checks when GadgetServer(Executor) is removed.
    if (config.getSpecCache() == null) {
      throw new GadgetProcessException(GadgetException.Code.MISSING_SPEC_CACHE);
    }
    if (config.getMessageBundleCache() == null ) {
      throw new GadgetProcessException(
          GadgetException.Code.MISSING_MESSAGE_BUNDLE_CACHE);
    }
    if (config.getContentFetcher() == null) {
      throw new GadgetProcessException(
          GadgetException.Code.MISSING_REMOTE_OBJECT_FETCHER);
    }
    if (config.getFeatureRegistry() == null) {
      throw new GadgetProcessException(
          GadgetException.Code.MISSING_FEATURE_REGISTRY);
    }

    // Queue/tree of all jobs to be run for successful processing
    GadgetContext gc = new GadgetContext(config.getContentFetcher(),
                                         config.getMessageBundleCache(),
                                         locale,
                                         rctx,
                                         options);
    WorkflowContext wc = new WorkflowContext(gc);

    // Bootstrap tree of jobs to process
    WorkflowDependency cacheLoadDep =
        new WorkflowDependency(WorkflowDependency.Type.CORE, CACHE_LOAD);

    CacheLoadTask cacheLoadTask = new CacheLoadTask(gadgetId,
                                                    userPrefs,
                                                    config.getSpecCache());
    wc.jobsToRun.addJob(cacheLoadTask, cacheLoadDep);

    WorkflowDependency urlFetchDep =
        new WorkflowDependency(WorkflowDependency.Type.CORE, URL_FETCH);

    SpecLoadTask specLoadTask = new SpecLoadTask(config.getContentFetcher(),
                                                 gadgetId,
                                                 userPrefs,
                                                 config.getSpecCache(),
                                                 config.getGadgetBlacklist());
    wc.jobsToRun.addJob(specLoadTask, urlFetchDep, cacheLoadDep);

    WorkflowDependency enqueueFeatDep =
        new WorkflowDependency(WorkflowDependency.Type.CORE, ENQUEUE_FEATURES);
    wc.jobsToRun.addJob(new EnqueueFeaturesTask(config.getFeatureRegistry()),
                        enqueueFeatDep,
                        urlFetchDep);

    // Instantiate CompletionService
    CompletionService<GadgetException> processor =
        new ExecutorCompletionService<GadgetException>(config.getExecutor());

    // All exceptions caught during processing
    List<GadgetException> gadgetExceptions = new LinkedList<GadgetException>();

    // Loop through queue of Callables, executing each in CompletionService
    // whose precursors have been satisfied
    int jobsSubmitted = 0;
    do {
      // Loop through all jobs, submitting to run if all deps satisfied
      List<WorkflowJob> runThisCycle = new LinkedList<WorkflowJob>();
      for (WorkflowJob candidate : wc.jobsToRun) {
        if (candidate.ready(wc.depsDone)) {
          runThisCycle.add(candidate);
        }
      }

      // Fire off ready jobs and remove from jobsToRun list
      for (WorkflowJob runJob : runThisCycle) {
        processor.submit(runJob.task);
        jobsSubmitted++;
        wc.jobsToRun.remove(runJob);
      }

      // Wait around for at least one job to have completed.
      // Completion of a job results in an additional dep added to wc.depsDone,
      // thus potentially freeing up other jobs to run
      Future<GadgetException> latestResult = null;
      GadgetException gadgetException = null;
      try {
        latestResult = processor.take();
      } catch (InterruptedException e) {
        gadgetException = new GadgetException(
            GadgetException.Code.INTERNAL_SERVER_ERROR, e);
      }

      // Ensure the task ran successfully
      if (latestResult != null) {
        try {
          gadgetException = latestResult.get();
        } catch (ExecutionException e) {
          gadgetException = new GadgetException(
              GadgetException.Code.INTERNAL_SERVER_ERROR, e);
        } catch (InterruptedException e) {
          gadgetException = new GadgetException(
              GadgetException.Code.INTERNAL_SERVER_ERROR, e);
        }
      }

      if (gadgetException != null) {
        logger.log(Level.SEVERE, gadgetException.getCode().toString(), gadgetException);
        
        // Add to list of all exceptions caught, clear jobs, and continue
        // to aggressively catch as many exceptions as possible. Since
        // tasks are running anyway, we may as well get their results in
        // case it would be useful to the user.
        gadgetExceptions.add(gadgetException);
        wc.jobsToRun.clear();
        jobsSubmitted = wc.depsDone.size();
      }
    } while(wc.jobsToRun.size() > 0 || jobsSubmitted > wc.depsDone.size());

    if (gadgetExceptions.size() > 0) {
      throw new GadgetProcessException(gadgetExceptions);
    }

    // terminate when all Callables are finished (or Exception detected?)
    return wc.gadget;
  }

  public List<GadgetSpec.UserPref> getPrefsInfo(Gadget.ID gadgetId) {
    return null;
  }

  private static final String CACHE_LOAD = "cache-load";
  private static final String URL_FETCH = "url-fetch";
  private static final String ENQUEUE_FEATURES = "enqueue-features";

  private static class WorkflowJobList extends ArrayList<WorkflowJob> {
    private final WorkflowContext wc;
    public WorkflowJobList(WorkflowContext wc) {
      this.wc = wc;
    }

    public void addJob(WorkflowTask task,
                       WorkflowDependency done,
                       WorkflowDependency... deps) {
      task.setup(wc, done);
      this.add(new WorkflowJob(task, deps));
    }
  }

  private static class WorkflowDependency {
    private static enum Type {
      CORE, FEATURE_PREPARE, FEATURE_PROCESS
    }

    private final Type type;
    private final String id;

    private WorkflowDependency(Type type, String id) {
      this.type = type;
      this.id = id;
    }

    @Override
    public boolean equals(Object other) {
      if (other instanceof WorkflowDependency) {
        WorkflowDependency wd = (WorkflowDependency)other;
        return type.equals(wd.type) && id.equals(wd.id);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return (type.toString() + id).hashCode();
    }
  }

  private static class WorkflowContext {
    private Gadget gadget;
    private GadgetContext context;
    private WorkflowJobList jobsToRun;
    private final Set<WorkflowDependency> depsDone;

    private WorkflowContext(GadgetContext context) {
      this.context = context;
      this.depsDone = Collections.synchronizedSet(new HashSet<WorkflowDependency>());
      this.jobsToRun = new WorkflowJobList(this);
    }
  }

  private static class WorkflowJob {
    private final WorkflowTask task;
    private final List<WorkflowDependency> deps;

    private WorkflowJob(WorkflowTask task, WorkflowDependency... deps) {
      this.task = task;
      this.deps = new LinkedList<WorkflowDependency>(Arrays.asList(deps));
    }

    private boolean ready(Set<WorkflowDependency> depsDone) {
      return depsDone.containsAll(deps);
    }
  }

  private static class CacheLoadTask extends WorkflowTask {
    private final GadgetView.ID gadgetId;
    private final UserPrefs prefs;
    private final GadgetDataCache<GadgetSpec> specCache;

    private CacheLoadTask(GadgetView.ID gadgetId,
                          UserPrefs prefs,
                          GadgetDataCache<GadgetSpec> specCache) {
      this.gadgetId = gadgetId;
      this.specCache = specCache;
      this.prefs = prefs;
    }

    @Override
    public void run(WorkflowContext wc) throws GadgetException {
      if (wc.context.getOptions().getIgnoreCache()) {
        return;
      }

      GadgetSpec spec = specCache.get(gadgetId.getKey());
      if (spec != null) {
        wc.gadget = new Gadget(gadgetId, spec, prefs);
      }
    }
  }

  private static class SpecLoadTask extends WorkflowTask {
    private final RemoteContentFetcher fetcher;
    private final GadgetView.ID gadgetId;
    private final GadgetDataCache<GadgetSpec> specCache;
    private final UserPrefs prefs;
    private final GadgetBlacklist blacklist;

    private SpecLoadTask(RemoteContentFetcher fetcher,
                         GadgetView.ID gadgetId,
                         UserPrefs prefs,
                         GadgetDataCache<GadgetSpec> specCache,
                         GadgetBlacklist blacklist) {
      this.fetcher = fetcher;
      this.gadgetId = gadgetId;
      this.specCache = specCache;
      this.prefs = prefs;
      this.blacklist = blacklist;
    }

    @Override
    public void run(WorkflowContext wc) throws GadgetException {
      if (blacklist != null && blacklist.isBlacklisted(gadgetId.getURI())) {
        throw new GadgetException(
            GadgetException.Code.BLACKLISTED_GADGET,
            "Gadget blacklisted at: " + gadgetId.getURI());
      }

      if (wc.gadget != null) {
        // Already retrieved: do nothing.
        return;
      }

      byte[] xml = null;
      try {
        xml = fetcher.fetch(
            gadgetId.getURI().toURL(), wc.context.getOptions()).getByteArray();
      } catch (MalformedURLException e) {
        throw new GadgetException(
            GadgetException.Code.FAILED_TO_RETRIEVE_CONTENT,
            "Malformed gadget spec URL: " + gadgetId.getURI().toString());
      }

      GadgetSpecParser specParser = new GadgetSpecParser();
      GadgetSpec spec = specParser.parse(gadgetId, xml);
      wc.gadget = new Gadget(gadgetId, spec, prefs);

      // This isn't a separate job because if it is we'd just need another
      // flag telling us not to store to the cache.
      if (!wc.context.getOptions().getIgnoreCache()) {
        specCache.put(wc.gadget.getId().getKey(), wc.gadget.copy());
      }
    }
  }

  private static class EnqueueFeaturesTask extends WorkflowTask {
    private final GadgetFeatureRegistry registry;

    private EnqueueFeaturesTask(GadgetFeatureRegistry registry) {
      this.registry = registry;
    }

    @Override
    public void run(WorkflowContext wc) throws GadgetException {
      Set<String> needed = new HashSet<String>();
      Set<String> optionalNames = new HashSet<String>();
      Map<String, GadgetSpec.FeatureSpec> requires = wc.gadget.getRequires();
      for (Map.Entry<String, GadgetSpec.FeatureSpec> entry : requires.entrySet()) {
        needed.add(entry.getKey());
        if (entry.getValue().isOptional()) {
          optionalNames.add(entry.getKey());
        }
      }

      // Retrieve needed feature processors from registry
      Set<GadgetFeatureRegistry.Entry> resultsFound =
          new HashSet<GadgetFeatureRegistry.Entry>();
      Set<String> resultsMissing = new HashSet<String>();
      registry.getIncludedFeatures(needed, resultsFound, resultsMissing);

      // Classify features this server is missing
      List<String> missingRequired = new LinkedList<String>();
      List<String> missingOptional = new LinkedList<String>();
      for (String missingResult : resultsMissing) {
        if (optionalNames.contains(missingResult)) {
          missingOptional.add(missingResult);
        } else {
          missingRequired.add(missingResult);
        }
      }

      if (missingRequired.size() > 0) {
        throw new GadgetException(GadgetException.Code.UNSUPPORTED_FEATURE,
            missingRequired.get(0));
      }

      if (missingOptional.size() > 0) {
        // TODO: add custom task, dependent on nothing, adding metadata re:
        // missing optionals to the gadget's output (satisfies HasFeature(...))
      }

      WorkflowDependency specLoadDep =
          new WorkflowDependency(WorkflowDependency.Type.CORE, URL_FETCH);
      for (GadgetFeatureRegistry.Entry entry : resultsFound) {
        List<WorkflowDependency> prepareDeps =
            new LinkedList<WorkflowDependency>();
        List<WorkflowDependency> processDeps =
          new LinkedList<WorkflowDependency>();

        // sanity check: each depends on the spec having been loaded
        prepareDeps.add(specLoadDep);

        for (GadgetFeatureRegistry.Entry featureDep : entry.getDependencies()) {
          // prepare depends on all its own deps...
          WorkflowDependency prepareNeedsDep =
              new WorkflowDependency(WorkflowDependency.Type.FEATURE_PREPARE,
                                     featureDep.getName());
          prepareDeps.add(prepareNeedsDep);

          WorkflowDependency processNeedsDep =
            new WorkflowDependency(WorkflowDependency.Type.FEATURE_PROCESS,
                                   featureDep.getName());
          // Can't process until all dependencies prepare() and process()
          // have completed.
          processDeps.add(prepareNeedsDep);
          processDeps.add(processNeedsDep);
        }

        // Create task for prepare and process, each with the dependency
        // that running each satisfies
        WorkflowDependency prepareDep =
            new WorkflowDependency(WorkflowDependency.Type.FEATURE_PREPARE,
                                   entry.getName());

        // We must guarantee that process is called after prepare. This is
        // implicitly stating that process has all of prepare's dependencies.
        processDeps.add(prepareDep);
        WorkflowDependency processDep =
          new WorkflowDependency(WorkflowDependency.Type.FEATURE_PROCESS,
                                 entry.getName());

        // Then add a new job for each task, with appropriate execution
        // precursors/dependencies, to the jobs queue
        GadgetFeature feature = entry.getFeature().create();
        wc.jobsToRun.addJob(new FeaturePrepareTask(entry.getName(), feature),
            prepareDep,
            prepareDeps.toArray(new WorkflowDependency[prepareDeps.size()]));
        wc.jobsToRun.addJob(new FeatureProcessTask(entry.getName(), feature),
            processDep,
            processDeps.toArray(new WorkflowDependency[processDeps.size()]));
      }
    }
  }

  private static class FeaturePrepareTask extends WorkflowTask {
    private final GadgetFeature feature;
    private final String name;
    private FeaturePrepareTask(String name, GadgetFeature feature) {
      this.name = name;
      this.feature = feature;
    }

    @Override
    public void run(WorkflowContext wc) throws GadgetException {
      Map<String, String> params = Gadget.getFeatureParams(wc.gadget, name);
      feature.prepare(wc.gadget, wc.context, params);
    }
  }

  private static class FeatureProcessTask extends WorkflowTask {
    private final GadgetFeature feature;
    private final String name;
    private FeatureProcessTask(String name, GadgetFeature feature) {
      this.name = name;
      this.feature = feature;
    }

    @Override
    public void run(WorkflowContext wc) throws GadgetException {
      Map<String, String> params = Gadget.getFeatureParams(wc.gadget, name);
      feature.process(wc.gadget,  wc.context,  params);
    }
  }

  private static abstract class WorkflowTask
      implements Callable<GadgetException> {
    // This class is mostly just an alias to Callable<GadgetException>
    // providing a helper method for passing in context
    public abstract void run(WorkflowContext wc) throws GadgetException;

    private WorkflowContext wc;
    private WorkflowDependency done;
    public WorkflowTask setup(WorkflowContext wc, WorkflowDependency done) {
      this.wc = wc;
      this.done = done;
      return this;
    }

    public GadgetException call() {
      GadgetException ret = null;
      try {
        this.run(wc);
      } catch (GadgetException e) {
        ret = e;
      } catch (Exception e) {
        // TODO: capture Throwable cause in wrapped exception
        ret = new GadgetException(
            GadgetException.Code.INTERNAL_SERVER_ERROR, e);
      } finally {
        wc.depsDone.add(done);
      }
      return ret;
    }
  }

  public static class GadgetProcessException extends Exception {
    private final List<GadgetException> components;

    public GadgetProcessException(List<GadgetException> components) {
      this.components = components;
    }

    public GadgetProcessException(GadgetException.Code code) {
      this.components = new ArrayList<GadgetException>();
      this.components.add(new GadgetException(code));
    }

    public List<GadgetException> getComponents() {
      return components;
    }
  }
}
