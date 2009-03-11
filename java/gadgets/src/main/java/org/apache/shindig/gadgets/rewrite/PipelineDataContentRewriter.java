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
package org.apache.shindig.gadgets.rewrite;

import org.apache.shindig.common.JsonSerializer;
import org.apache.shindig.common.xml.DomUtil;
import org.apache.shindig.expressions.RootELResolver;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetELResolver;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.preload.PipelinedDataPreloader;
import org.apache.shindig.gadgets.preload.PreloadException;
import org.apache.shindig.gadgets.preload.PreloadedData;
import org.apache.shindig.gadgets.preload.PreloaderService;
import org.apache.shindig.gadgets.spec.PipelinedData;
import org.apache.shindig.gadgets.spec.SpecParserException;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;

import javax.el.CompositeELResolver;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ContentRewriter that resolves opensocial-data elements on the server.
 * 
 * This rewriter cannot be used currently without the SocialMarkupHtmlParser.
 */
public class PipelineDataContentRewriter implements ContentRewriter {

  private static final Logger logger = Logger.getLogger(
      PipelineDataContentRewriter.class.getName());
  // TODO: support configuration
  private static final int MAX_BATCH_COUNT = 3;
  
  private final PipelinedDataPreloader preloader;
  private final PreloaderService preloaderService;

  @Inject
  public PipelineDataContentRewriter(PipelinedDataPreloader preloader,
      PreloaderService preloaderService) {
    this.preloader = preloader;
    this.preloaderService = preloaderService;
  }
  
  public RewriterResults rewrite(HttpRequest request, HttpResponse original, MutableContent content) {
    return null;
  }

  public RewriterResults rewrite(Gadget gadget, MutableContent content) {
    // Only bother for gadgets using the opensocial-data feature
    if (!gadget.getSpec().getModulePrefs().getFeatures().containsKey("opensocial-data")) {
      return null;
    }
    
    Document doc = content.getDocument();
    NodeIterator nodeIterator = ((DocumentTraversal) doc)
        .createNodeIterator(doc, NodeFilter.SHOW_ELEMENT,
            new NodeFilter() {
              public short acceptNode(Node n) {
                if ("script".equalsIgnoreCase(n.getNodeName()) &&
                    "text/os-data".equals(((Element) n).getAttribute("type"))) {
                  return NodeFilter.FILTER_ACCEPT;
                }
                return NodeFilter.FILTER_REJECT;
              }
            }, false);
    
    
    Map<String, JSONObject> results = Maps.newHashMap();
    
    // Use the default objects in the GadgetContext, and any objects that
    // have been resolved
    List<PipelineState> pipelines = Lists.newArrayList();
    CompositeELResolver rootObjects = new CompositeELResolver();
    rootObjects.add(new GadgetELResolver(gadget.getContext()));
    rootObjects.add(new RootELResolver(results));
    
    for (Node n = nodeIterator.nextNode(); n != null ; n = nodeIterator.nextNode()) {
      try {
        PipelinedData pipelineData = new PipelinedData((Element) n, gadget.getSpec().getUrl());
        PipelinedData.Batch batch = pipelineData.getBatch(rootObjects);
        if (batch == null) {
          // An empty pipeline element - just remove it
          n.getParentNode().removeChild(n);
        } else {
          // Not empty, ready it 
          PipelineState state = new PipelineState();
          state.batch = batch;
          state.node = n;
          pipelines.add(state);
        }
      } catch (SpecParserException e) {
        // Leave the element to the client
        logger.log(Level.INFO, "Failed to parse preload in " + gadget.getSpec().getUrl(), e);
      }
    }
    
    // No pipline elements found, return early
    if (pipelines.isEmpty()) {
      return null;
    }

    // Run batches until we run out
    int batchCount = 0;
    while (true) {
      // Gather all tasks from the first batch
      List<Callable<PreloadedData>> tasks = Lists.newArrayList();
      for (PipelineState pipeline : pipelines) {
        if (pipeline.batch != null) {
          tasks.addAll(preloader.createPreloadTasks(gadget.getContext(), pipeline.batch));
        }
      }
     
      // No further progress - quit
      if (tasks.isEmpty()) {
        break;
      }
      
    // And run the pipeline
      Collection<PreloadedData> preloads = preloaderService.preload(tasks);
      for (PreloadedData preloaded : preloads) {
        try {
          for (Object entry : preloaded.toJson()) {
            JSONObject obj = (JSONObject) entry;
            if (obj.has("data")) {
              results.put(obj.getString("id"), obj.getJSONObject("data"));
            }
            // TODO: handle errors?
          }
        } catch (PreloadException pe) {
          // This will be thrown in the event of some unexpected exception. We can move on.
          logger.log(Level.WARNING, "Unexpected error when preloading", pe);
        } catch (JSONException je) {
          throw new RuntimeException(je);
        }
      }
      
      // Advance to the next batch
      for (PipelineState pipeline : pipelines) {
        if (pipeline.batch != null) {
          pipeline.batch = pipeline.batch.getNextBatch(rootObjects);
          // Once there are no more batches, delete the associated script node.
          if (pipeline.batch == null) {
            pipeline.node.getParentNode().removeChild(pipeline.node);
          }
        }
      }
      
      // TODO: necessary?
      if (batchCount++ >= MAX_BATCH_COUNT) {
        break;
      }
    }
    
    Element head = (Element) DomUtil.getFirstNamedChildNode(doc.getDocumentElement(), "head");
    Element pipelineScript = doc.createElement("script");
    pipelineScript.setAttribute("type", "text/javascript");

    StringBuilder script = new StringBuilder();
    for (Map.Entry<String, JSONObject> entry : results.entrySet()) {
      String key = entry.getKey();

      // TODO: escape key
      content.addPipelinedData(key, entry.getValue());
      script.append("opensocial.data.DataContext.putDataSet(\"")
          .append(key)
          .append("\",")
          .append(JsonSerializer.serialize(entry.getValue()))
          .append(");");
    }

    pipelineScript.appendChild(doc.createTextNode(script.toString()));
    head.appendChild(pipelineScript);
    MutableContent.notifyEdit(doc);
    
    boolean allBatchesCompleted = true;
    for (PipelineState pipeline : pipelines) {
      if (pipeline.batch != null) {
        allBatchesCompleted = false;
        break;
      }
    }
    
    if (allBatchesCompleted) {
      gadget.addFeature("opensocial-data-context");
      gadget.removeFeature("opensocial-data");
    }
    
    return RewriterResults.notCacheable();
  }
  
  static class PipelineState {
    public Node node;
    public PipelinedData.Batch batch;
    
  }
}
