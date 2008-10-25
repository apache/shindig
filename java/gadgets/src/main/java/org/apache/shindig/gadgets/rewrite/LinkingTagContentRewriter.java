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

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;

import java.net.URI;
import java.util.*;

public class LinkingTagContentRewriter extends HtmlContentRewriter {
  private final LinkRewriter linkRewriter;
  private final Map<String, Set<String>> tagAttributeTargets;

  public LinkingTagContentRewriter(LinkRewriter linkRewriter,
                                   Map<String, Set<String>> attributeTargets) {
    this.linkRewriter = linkRewriter;
    if (attributeTargets != null) {
      this.tagAttributeTargets = attributeTargets;
    } else {
      this.tagAttributeTargets = getDefaultTargets();
    }
  }

  @Override
  protected RewriterResults rewrite(Document root, final URI baseUri) {
    if (linkRewriter == null) {
      // Sanity test.
      return null;
    }
    boolean mutated = false;

    if (root instanceof DocumentTraversal) {
      NodeIterator nodeIterator = ((DocumentTraversal) root)
          .createNodeIterator(root, NodeFilter.SHOW_ELEMENT,
              new NodeFilter() {
                public short acceptNode(Node n) {
                  Set<String> stringSet = tagAttributeTargets.get(n.getNodeName());
                  if (stringSet != null) {
                    NamedNodeMap attributes = n.getAttributes();
                    // TODO - Check is NodeMap lookup is case insensitive, if so use that
                    for (String attribute : stringSet) {
                      for (int j = 0; j < attributes.getLength(); j++) {
                        Node attributeNode = attributes.item(j);
                        if (attributeNode.getNodeName().equalsIgnoreCase(attribute)) {
                          attributeNode.setNodeValue(linkRewriter.rewrite(
                              attributeNode.getNodeValue(), baseUri));
                        }
                      }
                    }
                    return NodeFilter.FILTER_ACCEPT;
                  } else {
                    return NodeFilter.FILTER_REJECT;
                  }
                }
              }, false);
      
      while (nodeIterator.nextNode() != null) {
        mutated= true;
      }
    }

    if (mutated) {
      MutableContent.notifyEdit(root);
    }

    return RewriterResults.cacheableIndefinitely();
  }

  private static Map<String, Set<String>> getDefaultTargets() {
    Map<String, Set<String>> targets = new HashMap<String, Set<String>>();
    targets.put("IMG", new HashSet<String>(Arrays.asList("src")));
    targets.put("EMBED", new HashSet<String>(Arrays.asList("src")));
    targets.put("LINK", new HashSet<String>(Arrays.asList("href")));
    return targets;
  }
}