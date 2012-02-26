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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.apache.shindig.gadgets.rewrite.DomWalker.Visitor.VisitStatus;

import org.w3c.dom.Node;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class StyleAdjacencyVisitorTest extends DomWalkerTestBase {
  @Test
  public void visitStyle() throws Exception {
    Node node = elem("style");
    assertEquals(VisitStatus.RESERVE_TREE, visit(node));
  }

  @Test
  public void visitLinkWithRel() throws Exception {
    Node node = elem("link", "rel", "stylesheet");
    assertEquals(VisitStatus.RESERVE_TREE, visit(node));
  }

  @Test
  public void visitLinkWithType() throws Exception {
    Node node = elem("link", "type", "text/css");
    assertEquals(VisitStatus.RESERVE_TREE, visit(node));
  }

  @Test
  public void visitStyleCaseInsensitive() throws Exception {
    Node node = elem("sTYlE");
    assertEquals(VisitStatus.RESERVE_TREE, visit(node));
  }

  @Test
  public void visitLinkCaseInsensitive() throws Exception {
    Node node = elem("lINK", "REL", "stYlEsheet");
    assertEquals(VisitStatus.RESERVE_TREE, visit(node));
    node = elem("LINk", "tyPe", "text/csS");
    assertEquals(VisitStatus.RESERVE_TREE, visit(node));
  }

  @Test
  public void visitStyleWithAttribs() throws Exception {
    Node node = elem("style", "foo", "bar");
    assertEquals(VisitStatus.RESERVE_TREE, visit(node));
  }

  @Test
  public void bypassUnknownElement() throws Exception {
    Node node = elem("div");
    assertEquals(VisitStatus.BYPASS, visit(node));
  }

  @Test
  public void bypassLinkWithoutAttribs() throws Exception {
    Node node = elem("link");
    assertEquals(VisitStatus.BYPASS, visit(node));
  }

  @Test
  public void bypassLinkWithWrongAttribs() throws Exception {
    Node node = elem("link", "type", "somecss");
    assertEquals(VisitStatus.BYPASS, visit(node));
  }

  @Test
  public void bypassText() throws Exception {
    Node node = doc.createTextNode("text");
    assertEquals(VisitStatus.BYPASS, visit(node));
  }

  @Test
  public void bypassComment() throws Exception {
    Node node = doc.createComment("comment");
    assertEquals(VisitStatus.BYPASS, visit(node));
  }

  @Test
  public void reshuffleSingleNodeInHead() throws Exception {
    Node style = elem("style");
    Node html = htmlDoc(new Node[] { elem("script"), doc.createTextNode("foo"),
        style, doc.createComment("comment") });
    assertTrue(revisit(style));

    // Document structure sanity tests.
    assertEquals(2, html.getChildNodes().getLength());
    Node head = html.getFirstChild();
    assertEquals("head", head.getNodeName());
    Node body = html.getLastChild();
    assertEquals("body", body.getNodeName());

    // Reshuffling validation.
    assertEquals(4, head.getChildNodes().getLength());
    assertSame(style, head.getChildNodes().item(0)); // First.
  }

  @Test
  public void reshuffleSingleNodeFromBody() throws Exception {
    Node style = elem("style");
    Node html = htmlDoc(new Node[] { elem("foo") }, elem("script"), doc.createTextNode("foo"),
        style, doc.createComment("comment"));
    assertTrue(revisit(style));

    // Document structure sanity tests.
    assertEquals(2, html.getChildNodes().getLength());
    Node head = html.getFirstChild();
    assertEquals("head", head.getNodeName());
    Node body = html.getLastChild();
    assertEquals("body", body.getNodeName());

    // Reshuffling validation.
    assertEquals(2, head.getChildNodes().getLength());
    assertSame(style, head.getChildNodes().item(0)); // First.
    assertEquals(3, body.getChildNodes().getLength());
  }

  @Test
  public void reshuffleMultipleStyleNodesWithNoChildernInHead() throws Exception {
    Node style1 = elem("style");
    Node style2 = elem("style");
    Node style3 = elem("style");

    // Some in head, some in body.
    Node html = htmlDoc(new Node[] {}, elem("script"), style1, elem("foo"),
        doc.createTextNode("text1"), style2, doc.createComment("comment"), elem("div"),
        style3);
    assertTrue(revisit(style1, style2, style3));

    // Document structure sanity tests.
    assertEquals(2, html.getChildNodes().getLength());
    Node head = html.getFirstChild();
    assertEquals("head", head.getNodeName());
    Node body = html.getLastChild();
    assertEquals("body", body.getNodeName());

    // Reshuffling validation.
    assertEquals(3, head.getChildNodes().getLength());
    assertSame(style1, head.getChildNodes().item(0));
    assertSame(style2, head.getChildNodes().item(1));
    assertSame(style3, head.getChildNodes().item(2));
    assertEquals(5, body.getChildNodes().getLength());
  }

  @Test
  public void reshuffleMultipleStyleNodes() throws Exception {
    Node style1 = elem("style");
    Node style2 = elem("style");
    Node style3 = elem("style");

    // Some in head, some in body.
    Node html = htmlDoc(new Node[] { elem("script"), style1, elem("foo") },
        doc.createTextNode("text1"), style2, doc.createComment("comment"), elem("div"),
        style3);
    assertTrue(revisit(style1, style2, style3));

    // Document structure sanity tests.
    assertEquals(2, html.getChildNodes().getLength());
    Node head = html.getFirstChild();
    assertEquals("head", head.getNodeName());
    Node body = html.getLastChild();
    assertEquals("body", body.getNodeName());

    // Reshuffling validation.
    assertEquals(5, head.getChildNodes().getLength());
    assertSame(style1, head.getChildNodes().item(0));
    assertSame(style2, head.getChildNodes().item(1));
    assertSame(style3, head.getChildNodes().item(2));
    assertEquals(3, body.getChildNodes().getLength());
  }

  @Test
  public void reshuffleMultipleLinkNodes() throws Exception {
    Node link1 = elem("link", "rel", "stylesheet");
    Node link2 = elem("link", "rel", "stylesheet");
    Node link3 = elem("link", "rel", "stylesheet");

    // Some in head, some in body.
    Node html = htmlDoc(new Node[] { link1, elem("script"), elem("foo") },
        doc.createTextNode("text1"), link2, doc.createComment("comment"), elem("div"),
        link3);
    assertTrue(revisit(link1, link2, link3));

    // Document structure sanity tests.
    assertEquals(2, html.getChildNodes().getLength());
    Node head = html.getFirstChild();
    assertEquals("head", head.getNodeName());
    Node body = html.getLastChild();
    assertEquals("body", body.getNodeName());

    // Reshuffling validation.
    assertEquals(5, head.getChildNodes().getLength());
    assertSame(link1, head.getChildNodes().item(0));
    assertSame(link2, head.getChildNodes().item(1));
    assertSame(link3, head.getChildNodes().item(2));
    assertEquals(3, body.getChildNodes().getLength());
  }

  @Test
  public void reshuffleMultiMatchedNodes() throws Exception {
    Node style1 = elem("style");
    Node style2 = elem("style");
    Node link1 = elem("link", "rel", "stylesheet");
    Node link2 = elem("link", "type", "text/css");

    // Some in head, some in body, one embedded.
    Node div = elem("div");
    div.appendChild(style2);
    Node html = htmlDoc(new Node[] { elem("base"), elem("script"), elem("script"), style1,
        doc.createComment("comment"), link1 },
        elem("div"), div, link2, doc.createTextNode("text"));
    assertTrue(revisit(style1, link1, style2, link2));

    // Document structure sanity tests.
    assertEquals(2, html.getChildNodes().getLength());
    Node head = html.getFirstChild();
    assertEquals("head", head.getNodeName());
    Node body = html.getLastChild();
    assertEquals("body", body.getNodeName());

    // Reshuffling validation.
    assertEquals(8, head.getChildNodes().getLength());
    assertSame(style1, head.getChildNodes().item(0));
    assertSame(link1, head.getChildNodes().item(1));
    assertSame(style2, head.getChildNodes().item(2));
    assertSame(link2, head.getChildNodes().item(3));
    assertEquals(0, div.getChildNodes().getLength());
    assertEquals(3, body.getChildNodes().getLength());
  }

  @Test
  public void avoidReshufflingInHeadlessDocument() throws Exception {
    Node style = elem("style");
    Node html = elem("html");
    Node body = elem("body");
    body.appendChild(style);
    html.appendChild(body);
    doc.appendChild(html);

    assertFalse(revisit(style));

    // Document structure sanity tests.
    assertEquals(1, html.getChildNodes().getLength());
    assertSame(body, html.getFirstChild());
  }

  @Test
  public void singleStyleNodeInHead() throws Exception {
    Node style = elem("style", "type", "text/css");
    Node head = elem("head");
    head.appendChild(style);

    Node html = elem("html");
    html.appendChild(head);
    html.appendChild(elem("body"));
    doc.appendChild(html);

    assertTrue(revisit(style));

    // Document structure sanity tests.
    assertEquals(2, html.getChildNodes().getLength());
    assertSame(head, html.getFirstChild());
  }



  private VisitStatus visit(Node node) throws Exception {
    return new StyleAdjacencyVisitor().visit(gadget(), node);
  }

  private boolean revisit(Node... nodes) throws Exception {
    return new StyleAdjacencyVisitor().revisit(gadget(), ImmutableList.<Node>copyOf(nodes));
  }
}
