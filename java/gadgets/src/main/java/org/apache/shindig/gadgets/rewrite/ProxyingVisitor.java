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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import org.apache.shindig.common.Pair;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.uri.Uri.UriException;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.uri.ProxyUriManager;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Simple visitor that, when plugged into a DomWalker, rewrites
 * resource links to proxied versions of the same.
 */
public class ProxyingVisitor implements DomWalker.Visitor {
  public final static Map<String, String> RESOURCE_TAGS =
    ImmutableMap.of(
        "body", "background",
        "img", "src",
        "input", "src",
        "link", "href",
        "script", "src");

  private final ContentRewriterFeature.Config featureConfig;
  private final ProxyUriManager uriManager;

  public ProxyingVisitor(ContentRewriterFeature.Config featureConfig,
                              ProxyUriManager uriManager) {
    this.featureConfig = featureConfig;
    this.uriManager = uriManager;
  }

  public VisitStatus visit(Gadget gadget, Node node) throws RewritingException {
    String nodeName = node.getNodeName().toLowerCase();
    if (node.getNodeType() == Node.ELEMENT_NODE &&
        RESOURCE_TAGS.containsKey(nodeName) &&
        featureConfig.shouldRewriteTag(nodeName)) {
      Attr attr = (Attr)node.getAttributes().getNamedItem(
          RESOURCE_TAGS.get(nodeName));
      if (attr != null) {
        String urlValue = attr.getValue();
        if (featureConfig.shouldRewriteURL(urlValue)) {
          return VisitStatus.RESERVE_NODE;
        }
      }
    }
    return VisitStatus.BYPASS;
  }

  public boolean revisit(Gadget gadget, List<Node> nodes) throws RewritingException {
    List<Pair<Node, Uri>> proxiedUris = getProxiedUris(gadget, nodes);
    
    boolean mutated = false;
    for (Pair<Node, Uri> proxyPair : proxiedUris) {
      if (proxyPair.two == null) {
        continue;
      }
      Element element = (Element)proxyPair.one;
      String nodeName = element.getNodeName().toLowerCase();
      element.setAttribute(RESOURCE_TAGS.get(nodeName), proxyPair.two.toString());
      mutated = true;
    }
    
    return mutated;
  }
  
  private List<Pair<Node, Uri>> getProxiedUris(Gadget gadget, List<Node> nodes) {
    List<ProxyUriManager.ProxyUri> reservedUris =
        Lists.newArrayListWithCapacity(nodes.size());
    
    for (Node node : nodes) {
      Element element = (Element)node;
      String nodeName = node.getNodeName().toLowerCase();
      String uriStr = element.getAttribute(RESOURCE_TAGS.get(nodeName)).trim();
      try {
        reservedUris.add(new ProxyUriManager.ProxyUri(gadget, Uri.parse(uriStr)));
      } catch (UriException e) {
        // Uri parse exception, add null.
        reservedUris.add(null);
      }
    }
    
    List<Uri> resourceUris = uriManager.make(reservedUris, featureConfig.getExpires());
    
    // By contract, resourceUris matches by index with inbound Uris. Create an easy-access
    // List with the results.
    List<Pair<Node, Uri>> proxiedUris = Lists.newArrayListWithCapacity(nodes.size());
    
    Iterator<Uri> uriIt = resourceUris.iterator();
    for (Node node : nodes) {
      proxiedUris.add(Pair.of(node, uriIt.next()));
    }
    
    return proxiedUris;
  }
}
