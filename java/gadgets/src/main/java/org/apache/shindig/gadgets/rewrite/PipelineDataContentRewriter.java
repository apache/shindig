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
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.preload.PipelineExecutor;
import org.apache.shindig.gadgets.spec.PipelinedData;
import org.apache.shindig.gadgets.spec.SpecParserException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.collect.Maps;
import com.google.inject.Inject;

/**
 * ContentRewriter that resolves opensocial-data elements on the server.
 * 
 * This rewriter cannot be used currently without the SocialMarkupHtmlParser.
 */
public class PipelineDataContentRewriter implements ContentRewriter {

  private static final Logger logger = Logger.getLogger(
      PipelineDataContentRewriter.class.getName());
  
  private final PipelineExecutor executor;

  @Inject
  public PipelineDataContentRewriter(PipelineExecutor executor) {
    this.executor = executor;
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
    
    Map<PipelinedData, Node> pipelineNodes = Maps.newHashMap();
    for (Node n = nodeIterator.nextNode(); n != null ; n = nodeIterator.nextNode()) {
      try {
        PipelinedData pipelineData = new PipelinedData((Element) n, gadget.getSpec().getUrl());
        pipelineNodes.put(pipelineData, n);
      } catch (SpecParserException e) {
        // Leave the element to the client
        logger.log(Level.INFO, "Failed to parse preload in " + gadget.getSpec().getUrl(), e);
      }
    }
    
    if (pipelineNodes.isEmpty()) {
      return null;
    }
    
    PipelineExecutor.Results results =
        executor.execute(gadget.getContext(), pipelineNodes.keySet());
    
    // Remove all pipeline entries that were fully evaluated
    for (Map.Entry<PipelinedData, Node> nodeEntry : pipelineNodes.entrySet()) {
      if (!results.remainingPipelines.contains(nodeEntry.getKey())) {
        Node node = nodeEntry.getValue();
        node.getParentNode().removeChild(node);
        MutableContent.notifyEdit(doc);
      }
    }

    // Insert script elements for all the successful results
    if (!results.keyedResults.isEmpty()) {
      Element head = (Element) DomUtil.getFirstNamedChildNode(doc.getDocumentElement(), "head");
      Element pipelineScript = doc.createElement("script");
      pipelineScript.setAttribute("type", "text/javascript");
  
      StringBuilder script = new StringBuilder();
      for (Map.Entry<String, ? extends Object> entry : results.keyedResults.entrySet()) {
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
    }
    
    // And if no pipelines remain unexecuted, remove the opensocial-data feature
    if (results.remainingPipelines.isEmpty()) {
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
