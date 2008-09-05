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

import java.util.LinkedList;
import java.util.Queue;

import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.parse.GadgetHtmlNode;
import org.apache.shindig.gadgets.rewrite.ContentRewriterFeature;

public class StyleLinksContentRewriter implements ContentRewriter {
  // TODO: consider providing helper base class for node-visitor content rewriters
  private final ContentRewriterFeature.Factory rewriterFeatureFactory;
  private final LinkRewriter linkRewriter;
  
  public StyleLinksContentRewriter(ContentRewriterFeature.Factory rewriterFeatureFactory,
      LinkRewriter linkRewriter) {
    this.rewriterFeatureFactory = rewriterFeatureFactory;
    this.linkRewriter = linkRewriter;
  }

  public HttpResponse rewrite(HttpRequest request, HttpResponse original) {
    // TODO Auto-generated method stub
    return null;
  }

  public void rewrite(Gadget gadget) {
    ContentRewriterFeature rewriterFeature = rewriterFeatureFactory.get(gadget.getSpec());
    if (linkRewriter == null || 
        !rewriterFeature.isRewriteEnabled() ||
        !rewriterFeature.getIncludedTags().contains("style")) {
      return;
    }
    
    Queue<GadgetHtmlNode> nodesToProcess =
      new LinkedList<GadgetHtmlNode>();
    GadgetHtmlNode root = gadget.getParseTree();
    if (root == null) {
      return;
    }
    
    nodesToProcess.addAll(root.getChildren());
  
    while (!nodesToProcess.isEmpty()) {
      GadgetHtmlNode curNode = nodesToProcess.remove();
      if (!curNode.isText()) {
        // Depth-first iteration over children. Order doesn't matter anyway.
        nodesToProcess.addAll(curNode.getChildren());
        
        if (curNode.getTagName().equalsIgnoreCase("style")) {
          String styleText = getNodeChildText(curNode);
          curNode.clearChildren();
          curNode.appendChild(new GadgetHtmlNode(
              CssRewriter.rewrite(styleText, gadget.getSpec().getUrl(), linkRewriter)));
        }
      }
    }
  }
  
  private static String getNodeChildText(GadgetHtmlNode node) {
    // TODO: move this to GadgetHtmlNode as a helper
    StringBuilder builder = new StringBuilder();
    for (GadgetHtmlNode child : node.getChildren()) {
      if (child.isText()) {
        builder.append(child.getText());
      }
    }
    return builder.toString();
  }
  
}
