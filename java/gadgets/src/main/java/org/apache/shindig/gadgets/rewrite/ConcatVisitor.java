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

import com.google.common.base.Strings;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.uri.ConcatUriManager;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * DOM mutator that concatenates resources using the concat servlet
 * @since 2.0.0
 */
public class ConcatVisitor implements DomWalker.Visitor {
  public static class Js extends ConcatVisitor {
    public Js(ContentRewriterFeature.Config config,
              ConcatUriManager uriManager) {
      super(config, uriManager, ConcatUriManager.Type.JS);
    }
  }

  public static class Css extends ConcatVisitor {
    public Css(ContentRewriterFeature.Config config,
               ConcatUriManager uriManager) {
      super(config, uriManager, ConcatUriManager.Type.CSS);
    }
  }

  private final ConcatUriManager uriManager;
  private final ConcatUriManager.Type type;
  private final ContentRewriterFeature.Config config;
  private final boolean split;
  private final boolean singleResourceConcat;

  private ConcatVisitor(ContentRewriterFeature.Config config,
      ConcatUriManager uriManager, ConcatUriManager.Type type) {
    this.uriManager = uriManager;
    this.type = type;
    this.config = config;
    this.split = (type == ConcatUriManager.Type.JS && config.isSplitJsEnabled());
    this.singleResourceConcat = config.isSingleResourceConcatEnabled();
  }

  public VisitStatus visit(Gadget gadget, Node node) throws RewritingException {
    // Reserve JS nodes; always if there's an adjacent rewritable JS node and also when
    // directed to support split-resource concatenation
    if (node.getNodeType() != Node.ELEMENT_NODE ||
        !node.getNodeName().equalsIgnoreCase(type.getTagName())) {
      return VisitStatus.BYPASS;
    }

    Element element = (Element)node;
    if (isRewritableExternData(element)) {
      if (split || singleResourceConcat ||
          isRewritableExternData(getSibling(element, true)) ||
          isRewritableExternData(getSibling(element, false))) {
        return VisitStatus.RESERVE_NODE;
      }
    }

    return VisitStatus.BYPASS;
  }

  /**
   * For css:
   * Link tags are first split into buckets separated by tags with mediaType == "all"
   * / title attribute different from their previous link tag / nodes that are
   * not 'link' tags.
   * This ensures that the buckets can be processed separately without losing title /
   * "all" mediaType information.
   *
   * Link tags with same mediaType are concatenated within each bucket.
   * This exercise ensures that css information is loaded in the same relative order
   * as that of the original html page, and that the css information within
   * mediaType=="all" is retained and applies to all media types.
   *
   * Look at the areLinkNodesBucketable method for details on mediaType=="all" and
   * title attribute
   *
   * Example: Assume we have the following node list. (all have same parent,
   * nodes between Node6 and Node12 are non link nodes, and hence did not come
   * to revisit() call)
   *    <link href="1.css" rel="stylesheet" type="text/css" media="screen">       -- Node1
   *    <link href="2.css" rel="stylesheet" type="text/css" media="print">        -- Node2
   *    <link href="3.css" rel="stylesheet" type="text/css" media="screen">       -- Node3
   *    <link href="4.css" rel="stylesheet" type="text/css" media="all">          -- Node4
   *    <link href="5.css" rel="stylesheet" type="text/css" media="all">          -- Node5
   *    <link href="6.css" rel="stylesheet" type="text/css" media="screen">       -- Node6
   *    <link href="12.css" rel="stylesheet" type="text/css" media="screen">      -- Node12
   *    <link href="13.css" rel="stylesheet" type="text/css" media="screen">      -- Node13
   *
   *    First we split to buckets bassed on the adjacency and other conditions.
   *    buckets - [ [ Node1, Node2, Node3 ], [ Node4, Node 5 ], [ Node6 ], [ Node12, Node13 ]
   *    Within each bucket we group them based on media type.
   *    batches - [ Node1, Node2, Node3 ] --> [ [Node1, Node3], [Node2] ]
   *            - [ Node4, Node 5 ] --> [ [ Node4, Node 5 ] ]
   *            - [ Node6 ] --> [ [ Node6 ] ]
   *            - [ Node12, Node13 ] --> [ [ Node12, Node13 ] ]
   *
   * Refer Tests for more examples.
   */
  public boolean revisit(Gadget gadget, List<Node> nodes) throws RewritingException {
    // Collate Elements into Buckets.
    List<List<Element>> concatBuckets = Lists.newLinkedList();
    List<Element> curBucket = Lists.newLinkedList();
    Iterator<Node> nodeIter = nodes.iterator();
    Element cur = (Element)nodeIter.next();
    curBucket.add(cur);
    while (nodeIter.hasNext()) {
      Element next = (Element)nodeIter.next();
      if ((!split && cur != getSibling(next, true)) ||
          (type == ConcatUriManager.Type.CSS && !areLinkNodesBucketable(cur, next))) {
        // Break off current bucket and add to list of all.
        concatBuckets.add(curBucket);
        curBucket = Lists.newLinkedList();
      }
      curBucket.add(next);
      cur = next;
    }

    // Add leftovers.
    concatBuckets.add(curBucket);

    // Split the existing buckets based on media types into concat batches.
    List<List<Element>> concatBatches = Lists.newLinkedList();
    Iterator<List<Element>> batchesIter = concatBuckets.iterator();
    while (batchesIter.hasNext()) {
      splitBatchOnMedia(batchesIter.next(), concatBatches);
    }

    // Prepare batches of Uris to send to generate concat Uris
    List<List<Uri>> uriBatches = Lists.newLinkedList();
    batchesIter = concatBatches.iterator();
    while (batchesIter.hasNext()) {
      List<Element> batch = batchesIter.next();
      List<Uri> uris = Lists.newLinkedList();
      if (batch.isEmpty() || !getUris(type, batch, uris)) {
        batchesIter.remove();
        continue;
      }
      uriBatches.add(uris);
    }

    if (uriBatches.isEmpty()) {
      return false;
    }

    // Generate the ConcatUris, then correlate with original elements.
    List<ConcatUriManager.ConcatData> concatUris =
        uriManager.make(
          ConcatUriManager.ConcatUri.fromList(gadget, uriBatches, type), !split);

    Iterator<List<Element>> elemBatchIt = concatBatches.iterator();
    Iterator<List<Uri>> uriBatchIt = uriBatches.iterator();
    for (ConcatUriManager.ConcatData concatUri : concatUris) {
      List<Element> sourceBatch = elemBatchIt.next();
      List<Uri> sourceUris = uriBatchIt.next();

      // Regardless what happens, inject as many copies of the first node
      // as needed, with new (concat) URI, immediately ahead of the first elem.
      Element firstElem = sourceBatch.get(0);
      for (Uri uri : concatUri.getUris()) {
        Element elemConcat = (Element)firstElem.cloneNode(true);
        elemConcat.setAttribute(type.getSrcAttrib(), uri.toString());
        firstElem.getParentNode().insertBefore(elemConcat, firstElem);
      }

      // Now for all Elements, either A) remove them or B) replace each
      // with a <script> node with snippet of code configuring/evaluating
      // the resultant inserted code. This is useful for split-JS in particular,
      // and might also be used in spriting later.
      Iterator<Uri> uriIt = sourceUris.iterator();
      for (Element elem : sourceBatch) {
        Uri elemOrigUri = uriIt.next();
        String snippet = concatUri.getSnippet(elemOrigUri);
        if (!Strings.isNullOrEmpty(snippet)) {
          Node scriptNode = elem.getOwnerDocument().createElement("script");
          scriptNode.setTextContent(snippet);
          elem.getParentNode().insertBefore(scriptNode, elem);
        }
        elem.getParentNode().removeChild(elem);
      }
    }

    return true;
  }

  /**
   * Split the given batch of elements (assumed to be sibling nodes that can be concatenated)
   * into batches with same media types.
   *
   * @param elements
   * @param output
   */
  private void splitBatchOnMedia(List<Element> elements, List<List<Element>> output) {
    // Multimap to hold the ordered list of elements encountered for a given media type.
    Multimap<String, Element> mediaBatchMap = LinkedHashMultimap.create();
    for (Element element : elements) {
      String mediaType = element.getAttribute("media");
      mediaBatchMap.put(Strings.isNullOrEmpty(mediaType) ? "screen" : mediaType, element);
    }
    Set<String> mediaTypes = mediaBatchMap.keySet();
    for (String mediaType : mediaTypes) {
      Collection<Element> elems = mediaBatchMap.get(mediaType);
      output.add(new LinkedList<Element>(elems));
    }
  }

  private boolean isRewritableExternData(Element elem) {
    String uriStr = elem != null ? elem.getAttribute(type.getSrcAttrib()) : null;
    if (Strings.isNullOrEmpty(uriStr) ||
        !config.shouldRewriteURL(uriStr)) {
      return false;
    }
    if (type == ConcatUriManager.Type.CSS) {
      // rel="stylesheet" and type="text/css" also required.
      return ("stylesheet".equalsIgnoreCase(elem.getAttribute("rel")) &&
              "text/css".equalsIgnoreCase(elem.getAttribute("type")));
    }
    return true;
  }

  private Element getSibling(Element root, boolean isPrev) {
    Node cur = root;
    while ((cur = getNext(cur, isPrev)) != null) {
      // Text nodes are safe to skip, as they won't effect styles or scripts.
      // It is also safe to skip comment nodes except for conditional comments.
      if (cur.getNodeType() == Node.TEXT_NODE ||
          (cur.getNodeType() == Node.COMMENT_NODE && !isConditionalComment(cur))) {
        continue;
      }
      break;
    }
    if (cur != null && cur.getNodeType() == Node.ELEMENT_NODE) {
      return (Element)cur;
    }
    return null;
  }

  private Node getNext(Node node, boolean isPrev) {
    return isPrev ? node.getPreviousSibling() : node.getNextSibling();
  }

  private boolean getUris(ConcatUriManager.Type type, List<Element> elems, List<Uri> uris) {
    for (Element elem : elems) {
      String uriStr = elem.getAttribute(type.getSrcAttrib());
      try {
        uris.add(Uri.parse(uriStr));
      } catch (Uri.UriException e) {
        // Invalid formatted Uri, batch failed.
        return false;
      }
    }
    return true;
  }

  /**
   * Checks if the css link tags can be put into the same bucket.
   */
  private boolean areLinkNodesBucketable(Element current, Element next) {
    boolean areLinkNodesCompatible = false;
    // All link tags with media='all' should be placed in their own buckets.
    // Except for adjacent css links with media='all', which can belong to the
    // same bucket.
    String currMediaType = current.getAttribute("media");
    String nextMediaType = next.getAttribute("media");
    if (("all".equalsIgnoreCase(currMediaType) && "all".equalsIgnoreCase(nextMediaType)) ||
        (!"all".equalsIgnoreCase(currMediaType) && !"all".equalsIgnoreCase(nextMediaType))) {
      areLinkNodesCompatible = true;
    }

    // we can't keep the link tags with different 'title' attribute in same
    // bucket.
    // An example that proves the above comment.
    // <link rel="stylesheet" type="text/css" href="a.css" />
    // <link rel="stylesheet" type="text/css" href="b.css" title="small font"/>
    // <link rel="stylesheet" type="text/css" href="c.css" />
    // <link rel="alternate stylesheet" type="text/css" href="d.css" title="large font"/>
    // Since browser allows to switch between the perferred styles 'small font' and 'large font',
    // we should not batch across the links with title attribute, as it will lead to reordering of
    // styles.
    return areLinkNodesCompatible && current.getAttribute("title").equals(next.getAttribute("title"));
  }

  /**
   * Checks if a given comment node is coditional comment.
   */
  private boolean isConditionalComment(Node node) {
    return node.getNodeValue().trim().startsWith("[if");
  }
}
