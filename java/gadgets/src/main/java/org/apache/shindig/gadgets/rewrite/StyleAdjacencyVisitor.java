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

import com.google.common.base.Objects;
import org.apache.shindig.common.xml.DomUtil;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.rewrite.DomWalker.Visitor;

import org.w3c.dom.Attr;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.List;

/**
 * Visitor that pulls all stylesheet nodes in a document to head, in
 * the order they were found in the document. This maintains CSS semantics
 * in all but the most pathological (JS manipulating CSS through stylesheets
 * in an order-dependent way) cases while reducing browser reflows and making
 * CSS concatenated-proxying more likely.
 *
 * @since 2.0.0
 */
public class StyleAdjacencyVisitor implements Visitor {

  public VisitStatus visit(Gadget gadget, Node node) throws RewritingException {
    if (node.getNodeType() == Node.ELEMENT_NODE &&
        ("style".equalsIgnoreCase(node.getNodeName()) ||
         ("link".equalsIgnoreCase(node.getNodeName()) &&
          ("stylesheet".equalsIgnoreCase(getAttrib(node, "rel")) ||
           ("text/css".equalsIgnoreCase(getAttrib(node, "type"))))))) {
      // Reserve <style...>, <link rel="stylesheet"...>, or <link type="text/css"...>
      return VisitStatus.RESERVE_TREE;
    }

    return VisitStatus.BYPASS;
  }

  public boolean revisit(Gadget gadget, List<Node> nodes)
      throws RewritingException {
    Node head = DomUtil.getFirstNamedChildNode(
        nodes.get(0).getOwnerDocument().getDocumentElement(), "head");

    if (head == null) {
      // Should never occur; do for paranoia's sake.
      return false;
    }

    // Detach nodes
    for (Node n : nodes) {
      n.getParentNode().removeChild(n);
    }

    // Add nodes back to DOM
    if (head.getFirstChild() == null) {
      // add each node to head
      for (Node n : nodes) {
        head.appendChild(n);
      }
    } else {
      // existing nodes in head, inject all nodes before the first one
      Node firstChild = head.getFirstChild();
      for (Node n : nodes) {
        head.insertBefore(n, firstChild);
      }
    }

    return true;
  }

  private String getAttrib(Node node, String key) {
    String value = null;
    NamedNodeMap attribs = node.getAttributes();
    for (int i = 0; i < attribs.getLength(); ++i) {
      Attr attr = (Attr)attribs.item(i);
      if (key.equalsIgnoreCase(attr.getName())) {
        value = attr.getValue();
        break;
      }
    }
    return Objects.firstNonNull(value, "");
  }
}
