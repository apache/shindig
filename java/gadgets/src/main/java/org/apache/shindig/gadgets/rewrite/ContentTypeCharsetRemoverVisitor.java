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

import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.uri.UriUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.List;

/**
 * Removes charset information from &lt;meta http-equip="Content-Type"&gt;
 *
 * @since 2.0.0
 */
public class ContentTypeCharsetRemoverVisitor implements DomWalker.Visitor {
  public final static String CONTENT = "content";
  public final static String CONTENT_TYPE = "content-type";
  public final static String HTTP_EQUIV = "http-equiv";
  public final static String META = "meta";

  // @Override
  public VisitStatus visit(Gadget gadget, Node node) throws RewritingException {
    if (node.getNodeType() == Node.ELEMENT_NODE &&
        META.equalsIgnoreCase(node.getNodeName())) {

      Element elem = (Element) node;
      String httpEquip = elem.getAttribute(HTTP_EQUIV);
      String content = elem.getAttribute(CONTENT);
      if (httpEquip != null && content != null &&
          CONTENT_TYPE.equalsIgnoreCase(httpEquip)) {
        elem.setAttribute(CONTENT, UriUtils.getContentTypeWithoutCharset(content));
        return VisitStatus.MODIFY;
      }
    }
    return VisitStatus.BYPASS;
  }

  // @Override
  public boolean revisit(Gadget gadget, List<Node> nodes) {
    // Edits in place.
    return false;
  }
}
