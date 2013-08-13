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
package org.apache.shindig.gadgets.preload;

import org.apache.shindig.common.JsonUtil;
import org.apache.shindig.common.logging.i18n.MessageKeys;
import org.apache.shindig.expressions.Expressions;
import org.apache.shindig.expressions.RootELResolver;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetELResolver;
import org.apache.shindig.gadgets.spec.PipelinedData;
import org.apache.shindig.gadgets.spec.PipelinedData.Batch;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.el.CompositeELResolver;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

/**
 * Runs data pipelining, chaining dependencies among batches as needed.
 */
public class PipelineExecutor {
  // TODO: support configuration
  private static final int MAX_BATCH_COUNT = 3;
  //class name for logging purpose
  private static final String classname = PipelineExecutor.class.getName();
  private static final Logger LOG = Logger.getLogger(classname,MessageKeys.MESSAGES);


  private final PipelinedDataPreloader preloader;
  private final PreloaderService preloaderService;
  private final Expressions expressions;

  @Inject
  public PipelineExecutor(PipelinedDataPreloader preloader,
      PreloaderService preloaderService,
      Expressions expressions) {
    this.preloader = preloader;
    this.preloaderService = preloaderService;
    this.expressions = expressions;
  }

  /**
   * Results from a full pipeline execution.
   */
  public static class Results {
    /**
     * A collection of the pipelines that could not be fully
     * evaluated.
     */
    public final Collection<PipelinedData> remainingPipelines;

    /**
     * Results in the form of a full JSON-RPC batch response.
     */
    public final Collection<? extends Object> results;

    /**
     * Results in the form of a Map from id to a JSON-serializable object.
     */
    public final Map<String, ? extends Object> keyedResults;

    public Results(Collection<PipelinedData> remainingPipelines,
        Collection<? extends Object> results,
        Map<String, ? extends Object> keyedResults) {
      this.remainingPipelines = remainingPipelines;
      this.results = results;
      this.keyedResults = keyedResults;
    }
  }

  /**
   * Executes a pipeline, or set of pipelines.
   * @param context the gadget context for the state in which the pipelines execute
   * @param pipelines a collection of pipelines
   * @return results from the pipeline, or null if there are no results
   */
  public Results execute(GadgetContext context, Collection<PipelinedData> pipelines) {
    List<Object> results = Lists.newArrayList();
    Map<String, Object> elResults = Maps.newHashMap();
    CompositeELResolver rootObjects = new CompositeELResolver();
    rootObjects.add(new GadgetELResolver(context));
    rootObjects.add(new RootELResolver(elResults));

    List<PipelineState> pipelineStates = Lists.newArrayList();
    for (PipelinedData pipeline : pipelines) {
      PipelinedData.Batch batch = pipeline.getBatch(expressions, rootObjects);
      pipelineStates.add(new PipelineState(pipeline, batch));
    }

    int batchCount = 0;
    while (true) {
      List<Callable<PreloadedData>> tasks = Lists.newArrayList();
      for (PipelineState pipeline : pipelineStates) {
        if (pipeline.batch != null) {
          tasks.addAll(preloader.createPreloadTasks(context, pipeline.batch));
        }
      }

      if (tasks.isEmpty()) {
        break;
      }

      Collection<PreloadedData> preloads = preloaderService.preload(tasks);
      for (PreloadedData preloaded : preloads) {
        try {
          for (Object entry : preloaded.toJson()) {
            results.add(entry);

            String id = (String) JsonUtil.getProperty(entry, "id");

            Object data = JsonUtil.getProperty(entry, "result");
            if (data == null) {
              // For backward compatiblity, check maybe return old 'data' field:
              data = JsonUtil.getProperty(entry, "data");
            }
            if (data != null) {
              elResults.put(id, data);
            } else {
              Object error = JsonUtil.getProperty(entry, "error");
              if (error != null) {
                elResults.put(id, error);
              }
            }
          }
        } catch (PreloadException pe) {
          // This will be thrown in the event of some unexpected exception. We can move on.
          if (LOG.isLoggable(Level.WARNING)) {
            LOG.logp(Level.WARNING, classname, "execute", MessageKeys.ERROR_PRELOADING);
            LOG.log(Level.WARNING, "", pe);
          }
        }
      }

      // Advance to the next batch
      for (PipelineState pipeline : pipelineStates) {
        if (pipeline.batch != null) {
          pipeline.batch = pipeline.batch.getNextBatch(rootObjects);
        }
      }

      batchCount++;
      if (batchCount == MAX_BATCH_COUNT) {
        break;
      }
    }

    List<PipelinedData> remainingPipelines = Lists.newArrayList();
    for (PipelineState pipeline : pipelineStates) {
      if (pipeline.batch != null) {
        remainingPipelines.add(pipeline.pipeline);
      }
    }

    return new Results(remainingPipelines, results, elResults);
  }

  /** State of one of the pipelines */
  static class PipelineState {
    public PipelineState(PipelinedData pipeline, Batch batch) {
      this.pipeline = pipeline;
      this.batch = batch;
    }

    public final PipelinedData pipeline;
    public PipelinedData.Batch batch;
  }
}
