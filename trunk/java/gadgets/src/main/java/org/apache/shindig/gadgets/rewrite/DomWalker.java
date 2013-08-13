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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.uri.UriCommon.Param;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Framework-in-a-framework facilitating the common Visitor case
 * in which a DOM tree is walked in order to manipulate it.
 *
 * See subclass doc for additional detail.
 *
 * @since 2.0.0
 */
public final class DomWalker {
  private DomWalker() {}

  /**
   * Implemented by classes that do actual manipulation of the DOM
   * while {@code DomWalker.ContentVisitor} walks it. {@code Visitor}
   * instances are called for each {@code Node} in the DOM in the order
   * they are registered with the {@code DomVisitor.ContentVisitor}
   */
  public interface Visitor {
    /**
     * Returned by the {@code visit(Gadget, Node)} method, signaling:
     *
     * BYPASS = Visitor doesn't care about the node.
     * MODIFY = Visitor has modified the node.
     * RESERVE_NODE = Visitor reserves exactly the node passed. No other
     *   Visitor will visit the node.
     * RESERVE_TREE = Visitor reserves the node passed and all its descendants
     *   No other Visitor will visit them.
     *
     * Visitors are expected to be well-behaved in that they do not
     * modify unreserved nodes: that is, in revisit(...) they do not access
     * adjacent, parent, etc. nodes and modify them. visit(...) may return
     * MODIFY to indicate a modification of the given node.
     *
     * Other append and delete operations are acceptable
     * but only in revisit(). Reservations are supported in order to support
     * "batched" lookups relating to a similar set of data retrieved from a
     * backend.
     */
    public enum VisitStatus {
      BYPASS,
      MODIFY,
      RESERVE_NODE,
      RESERVE_TREE
    }

    /**
     * Visit a particular Node in the DOM.
     *
     * @param gadget Context for the request.
     * @param node Node being visited.
     * @return Status, see {@code VisitStatus}
     */
    VisitStatus visit(Gadget gadget, Node node) throws RewritingException;

    /**
     * Revisit a node in the DOM that was marked by the
     * {@code visit(Gadget, Node)} as reserved during DOM traversal.
     *
     * @param gadget Context for the request.
     * @param nodes Nodes being revisited, previously marked as reserved.
     * @return True if any node modified, false otherwise.
     */
    boolean revisit(Gadget gadget, List<Node> nodes) throws RewritingException;
  }

  /**
   * Rewriter that traverses the DOM, passing each node to its
   * list of {@code Visitor} instances in order. Each visitor
   * may bypass, modify, or reserve the node. Reserved nodes
   * will be revisited after the entire DOM tree is walked.
   * The DOM tree is walked in depth-first order.
   */
  public static class Rewriter implements GadgetRewriter, ResponseRewriter {
    private final List<Visitor> visitors;

    public Rewriter(List<Visitor> visitors) {
      this.visitors = visitors;
    }

    public Rewriter(Visitor... visitors) {
      this.visitors = Arrays.asList(visitors);
    }

    public Rewriter() {
      this.visitors = null;
    }

    // Override this to supply a list of Visitors generated using request context
    // rather than supplied at construction time.
    protected List<Visitor> makeVisitors(Gadget context, Uri gadgetUri) {
      return visitors;
    }

    /**
     * Performs the DomWalker rewrite operation described in class javadoc.
     */
    public void rewrite(Gadget gadget, MutableContent content)
        throws RewritingException {
      rewrite(makeVisitors(gadget, gadget.getSpec().getUrl()), gadget, content);
    }

    public void rewrite(HttpRequest request, HttpResponseBuilder builder, Gadget gadget)
            throws RewritingException {
      if (RewriterUtils.isHtml(request, builder)) {
        if(gadget == null) {
          gadget = makeGadget(request);
        }
        rewrite(makeVisitors(gadget, request.getGadget()), gadget, builder);
      }
    }

    private boolean rewrite(List<Visitor> visitors, Gadget gadget, MutableContent content)
        throws RewritingException {
      Map<Visitor, List<Node>> reservations = Maps.newHashMap();

      LinkedList<Node> toVisit = Lists.newLinkedList();
      Document doc = content.getDocument();
      if (doc == null) {
        throw new RewritingException("content.getDocument is null. Content: "
                                     + content.getContent(),
                                     HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      }
      toVisit.add(doc.getDocumentElement());
      boolean mutated = false;
      while (!toVisit.isEmpty()) {
        Node visiting = toVisit.removeFirst();

        // Iterate through all visitors evaluating their visitation status.
        boolean treeReserved = false;
        boolean nodeReserved = false;
        for (Visitor visitor : visitors) {
          switch(visitor.visit(gadget, visiting)) {
          case MODIFY:
            content.documentChanged();
            mutated = true;
            break;
          case RESERVE_NODE:
            nodeReserved = true;
            break;
          case RESERVE_TREE:
            treeReserved = true;
            break;
          default:
            // Aka BYPASS - do nothing.
            break;
          }

          if (nodeReserved || treeReserved) {
            // Reservation was made.
            if (!reservations.containsKey(visitor)) {
              reservations.put(visitor, Lists.<Node>newLinkedList());
            }
            reservations.get(visitor).add(visiting);
            break;
          }
        }

        if (!treeReserved && visiting.hasChildNodes()) {
          // Tree wasn't reserved - walk children.
          // In order to preserve DFS order, walk children in reverse.
          for (Node child = visiting.getLastChild(); child != null;
               child = child.getPreviousSibling()) {
            toVisit.addFirst(child);
          }
        }
      }

      // Run through all reservations, revisiting as needed.
      for (Visitor visitor : visitors) {
        List<Node> nodesReserved = reservations.get(visitor);
        if (nodesReserved != null && visitor.revisit(gadget, nodesReserved)) {
          content.documentChanged();
          mutated = true;
        }
      }

      return mutated;
    }
  }

  // TODO: Remove these lame hacks by changing Gadget to a proper general Context object.
  public static Gadget makeGadget(GadgetContext context) {
    try {
      final GadgetSpec spec = new GadgetSpec(context.getUrl(),
          "<Module><ModulePrefs author=\"a\" title=\"t\"></ModulePrefs>" +
          "<Content></Content></Module>");
      return new Gadget().setSpec(spec).setContext(context);
    } catch (Exception e) {
      throw new RuntimeException("Unexpected boilerplate parse failure");
    }
  }

  public static Gadget makeGadget(final HttpRequest request) {
    return makeGadget(new GadgetContext() {
      @Override
      public Uri getUrl() {
        return request.getUri();
      }

      @Override
      public String getParameter(String key) {
        return request.getParam(key);
      }

      @Override
      public boolean getIgnoreCache() {
        return request.getIgnoreCache();
      }

      @Override
      public String getContainer() {
        return request.getContainer();
      }

      @Override
      public boolean getDebug() {
        return "1".equalsIgnoreCase(getParameter(Param.DEBUG.getKey()));
      }
    });
  }
}
