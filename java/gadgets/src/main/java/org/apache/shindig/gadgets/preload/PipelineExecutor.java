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

import org.apache.shindig.expressions.Expressions;
import org.apache.shindig.expressions.RootELResolver;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetELResolver;
import org.apache.shindig.gadgets.spec.PipelinedData;
import org.apache.shindig.gadgets.spec.PipelinedData.Batch;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
  private static final Logger logger = Logger.getLogger(PipelineExecutor.class.getName());

  private PipelinedDataPreloader preloader;
  private PreloaderService preloaderService;
  private Expressions expressions;

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
    public final JSONArray results;
    
    /**
     * Results in the form of a Map from id to JSONObject.
     */
    public final Map<String, JSONObject> keyedResults;
    
    public Results(Collection<PipelinedData> remainingPipelines,
        JSONArray results,
        Map<String, JSONObject> keyedResults) {
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
    JSONArray results = new JSONArray();
    Map<String, JSONObject> elResults = Maps.newHashMap();
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
            JSONObject obj = (JSONObject) entry;
            results.put(obj);
            if (obj.has("data")) {
              elResults.put(obj.getString("id"), obj.getJSONObject("data"));
            } else if (obj.has("error")) {
              elResults.put(obj.getString("id"), obj.getJSONObject("error"));
            }
          }
        } catch (PreloadException pe) {
          // This will be thrown in the event of some unexpected exception. We can move on.
          logger.log(Level.WARNING, "Unexpected error when preloading", pe);
        } catch (JSONException je) {
          throw new RuntimeException(je);
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
