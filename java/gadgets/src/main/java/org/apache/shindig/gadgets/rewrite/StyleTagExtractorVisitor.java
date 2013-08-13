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

import java.util.List;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.xml.DomUtil;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.rewrite.DomWalker.Visitor;
import org.apache.shindig.gadgets.spec.View;
import org.apache.shindig.gadgets.uri.ProxyUriManager;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Visits nodes in the dom extracting style tags.
 * @since 2.0.0
 */
public class StyleTagExtractorVisitor implements Visitor {
  private final ContentRewriterFeature.Config config;
  private final CssResponseRewriter cssRewriter;
  private final ProxyUriManager proxyUriManager;

  public StyleTagExtractorVisitor(ContentRewriterFeature.Config config,
      CssResponseRewriter cssRewriter, ProxyUriManager proxyUriManager) {
    this.config = config;
    this.cssRewriter = cssRewriter;
    this.proxyUriManager = proxyUriManager;
  }

  public VisitStatus visit(Gadget gadget, Node node) throws RewritingException {
    if (!config.isRewriteEnabled() || !config.getIncludedTags().contains("style")) {
      return VisitStatus.BYPASS;
    }

    // Only process <style> elements.
    if (node.getNodeType() != Node.ELEMENT_NODE ||
        !node.getNodeName().equalsIgnoreCase("style")) {
      return VisitStatus.BYPASS;
    }

    return VisitStatus.RESERVE_NODE;
  }

  public boolean revisit(Gadget gadget, List<Node> nodes)
      throws RewritingException {
    boolean mutated = false;
    if (nodes.isEmpty()) {
      return mutated;
    }

    Uri contentBase = gadget.getSpec().getUrl();
    View view = gadget.getCurrentView();
    if (view != null && view.getHref() != null) {
      contentBase = view.getHref();
    }

    Element head = (Element)DomUtil.getFirstNamedChildNode(
        nodes.get(0).getOwnerDocument().getDocumentElement(), "head");
    for (Node node : nodes) {
      // Guaranteed safe cast due to reservation logic.
      Element elem = (Element)node;
      List<String> extractedUrls = cssRewriter.rewrite(
          elem, contentBase, CssResponseRewriter.uriMaker(proxyUriManager, config), true, gadget.getContext());
      for (String extractedUrl : extractedUrls) {
        // Add extracted urls as link elements to head
        Element newLink = head.getOwnerDocument().createElement("link");
        newLink.setAttribute("rel", "stylesheet");
        newLink.setAttribute("type", "text/css");
        newLink.setAttribute("href", extractedUrl);
        head.appendChild(newLink);
        mutated = true;
      }
    }

    return mutated;
  }
}
