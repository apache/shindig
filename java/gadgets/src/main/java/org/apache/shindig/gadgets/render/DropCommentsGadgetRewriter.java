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
package org.apache.shindig.gadgets.render;

import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.rewrite.DomWalker;
import org.apache.shindig.gadgets.rewrite.MutableContent;
import org.apache.shindig.gadgets.rewrite.RewritingException;
import org.w3c.dom.Node;

import java.util.List;

/**
 * Removes all HTML comments from the document source when sanitize=1.
 */
public class DropCommentsGadgetRewriter extends DomWalker.Rewriter {

  public DropCommentsGadgetRewriter() {
    super(new CommentFilter());
  }

  @Override
  public void rewrite(Gadget gadget, MutableContent content) throws RewritingException {
    if (gadget.sanitizeOutput()) {
      MutableContent rewritten =
          new MutableContent(content.getContentParser(), content.getContent());
      super.rewrite(gadget, rewritten);
      content.setContent(rewritten.getContent());
    }
  }

  private static class CommentFilter implements DomWalker.Visitor {

    public VisitStatus visit(Gadget gadget, Node node) {
      if (node.getNodeType() == Node.COMMENT_NODE) {
        return VisitStatus.RESERVE_TREE;
      }
      return VisitStatus.BYPASS;
    }

    public boolean revisit(Gadget gadget, List<Node> nodes) {
      for (Node node : nodes) {
        node.getParentNode().removeChild(node);
      }
      return true;
    }
  }
}
