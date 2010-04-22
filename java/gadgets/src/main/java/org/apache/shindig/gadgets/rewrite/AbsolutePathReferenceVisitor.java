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

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.rewrite.DomWalker.Visitor;
import org.w3c.dom.Attr;
import org.w3c.dom.Node;

import com.google.common.collect.ImmutableMap;

import java.util.List;

public class AbsolutePathReferenceVisitor implements Visitor {
  public final static Map<String, String> RESOURCE_TAGS =
    ImmutableMap.<String, String>builder()
        .put("a", "href")
        .put("area", "href")
        .put("q", "cite")
        .put("img", "src")
        .put("input", "src")
        .put("body", "background")
        .put("embed", "src")
        .put("link", "href")
        .put("script", "src")
        .put("object", "src").build();

  public VisitStatus visit(Gadget gadget, Node node) throws RewritingException {
    String nodeName = node.getNodeName().toLowerCase();
    if (node.getNodeType() == Node.ELEMENT_NODE &&
        RESOURCE_TAGS.containsKey(nodeName)) {
      Attr attr = (Attr)node.getAttributes().getNamedItem(RESOURCE_TAGS.get(nodeName));
      String nodeUri = attr != null ? attr.getValue() : null;
      if (!StringUtils.isEmpty(nodeUri)) {
        try {
          Uri prevUri = Uri.parse(nodeUri);
          Uri resolved = gadget.getSpec().getUrl().resolve(prevUri);
          if (!resolved.equals(prevUri)) {
            attr.setValue(resolved.toString());
            return VisitStatus.MODIFY;
          }
        } catch (Uri.UriException e) {
          // UriException on illegal input. Ignore.
        }
      }
    }
    return VisitStatus.BYPASS;
  }

  public boolean revisit(Gadget gadget, List<Node> node) throws RewritingException {
    // Modification happens immediately.
    return false;
  }

}
