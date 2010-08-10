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
package org.apache.shindig.common.xml;

import com.google.common.collect.Lists;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;

import java.util.List;
import java.util.Set;

/**
 * Utility functions for navigating DOM
 */
public final class DomUtil {

  private DomUtil() {}

  /**
   * @return first child node matching the specified name
   */
  public static Node getFirstNamedChildNode(Node root, String nodeName) {
    Node current = root.getFirstChild();
    while (current != null) {
      if (current.getNodeName().equalsIgnoreCase(nodeName)) {
        return current;
      }
      current = current.getNextSibling();
    }
    return null;
  }

  /**
   * @return last child node matching the specified name.
   */
  public static Node getLastNamedChildNode(Node root, String nodeName) {
    Node current = root.getLastChild();
    while (current != null) {
      if (current.getNodeName().equalsIgnoreCase(nodeName)) {
        return current;
      }
      current = current.getPreviousSibling();
    }
    return null;
  }

  public static List<Element> getElementsByTagNameCaseInsensitive(Document doc,
      final Set<String> lowerCaseNames) {
    final List<Element> result = Lists.newArrayList();
    NodeIterator nodeIterator = ((DocumentTraversal) doc)
        .createNodeIterator(doc, NodeFilter.SHOW_ELEMENT,
            new NodeFilter() {
              public short acceptNode(Node n) {
                if (lowerCaseNames.contains(n.getNodeName().toLowerCase())) {
                  return NodeFilter.FILTER_ACCEPT;
                }
                return NodeFilter.FILTER_REJECT;
              }
            }, false);
    for (Node n = nodeIterator.nextNode(); n != null ; n = nodeIterator.nextNode()) {
      result.add((Element)n);
    }
    return result;
  }
}
