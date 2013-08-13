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

import org.apache.shindig.gadgets.Gadget;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Node;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DomWalkerTest extends DomWalkerTestBase {
  private Node root;
  private Node child1;
  private Node child2;
  private Node subchild1;
  private Node text1;
  private Node text2;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();

    // Create a base document with structure:
    // <root>
    //   <child1>text1</child1>
    //   <child2>
    //     <subchild1>text2</subchild1>
    //   </child2>
    // </root>
    // ...which should allow all relevant test cases to be exercised.
    root = doc.createElement("root");
    child1 = doc.createElement("child1");
    text1 = doc.createTextNode("text1");
    child1.appendChild(text1);
    root.appendChild(child1);
    child2 = doc.createElement("child2");
    subchild1 = doc.createElement("subchild1");
    text2 = doc.createTextNode("text2");
    subchild1.appendChild(text2);
    child2.appendChild(subchild1);
    root.appendChild(child2);
    doc.appendChild(root);
  }

  @Test
  public void allBypassDoesNothing() throws Exception {
    Gadget gadget = gadget();

    // Visitor always bypasses nodes, never gets called with revisit(),
    // but visits every node in the document.
    DomWalker.Visitor visitor = createMock(DomWalker.Visitor.class);
    expect(visitor.visit(gadget, root))
        .andReturn(DomWalker.Visitor.VisitStatus.BYPASS).once();
    expect(visitor.visit(gadget, child1))
        .andReturn(DomWalker.Visitor.VisitStatus.BYPASS).once();
    expect(visitor.visit(gadget, child2))
        .andReturn(DomWalker.Visitor.VisitStatus.BYPASS).once();
    expect(visitor.visit(gadget, subchild1))
        .andReturn(DomWalker.Visitor.VisitStatus.BYPASS).once();
    expect(visitor.visit(gadget, text1))
        .andReturn(DomWalker.Visitor.VisitStatus.BYPASS).once();
    expect(visitor.visit(gadget, text2))
        .andReturn(DomWalker.Visitor.VisitStatus.BYPASS).once();
    replay(visitor);

    MutableContent mc = getContent(0);

    DomWalker.Rewriter rewriter = getRewriter(visitor);
    rewriter.rewrite(gadget, mc);

    // Verifying mutations on MutableContent completes the test.
    verify(mc);
  }

  @Test
  public void allMutateMutatesEveryTime() throws Exception {
    Gadget gadget = gadget();

    // Visitor mutates every node it sees immediately and inline.
    DomWalker.Visitor visitor = createMock(DomWalker.Visitor.class);
    expect(visitor.visit(gadget, root))
        .andReturn(DomWalker.Visitor.VisitStatus.MODIFY).once();
    expect(visitor.visit(gadget, child1))
        .andReturn(DomWalker.Visitor.VisitStatus.MODIFY).once();
    expect(visitor.visit(gadget, child2))
        .andReturn(DomWalker.Visitor.VisitStatus.MODIFY).once();
    expect(visitor.visit(gadget, subchild1))
        .andReturn(DomWalker.Visitor.VisitStatus.MODIFY).once();
    expect(visitor.visit(gadget, text1))
        .andReturn(DomWalker.Visitor.VisitStatus.MODIFY).once();
    expect(visitor.visit(gadget, text2))
        .andReturn(DomWalker.Visitor.VisitStatus.MODIFY).once();
    replay(visitor);

    MutableContent mc = getContent(6);

    DomWalker.Rewriter rewriter = getRewriter(visitor);
    rewriter.rewrite(gadget, mc);

    // Verifying mutations on MutableContent completes the test.
    verify(mc);
  }

  @Test
  public void allReserveNodeReservesAll() throws Exception {
    Gadget gadget = gadget();

    // Visitor mutates every node it sees immediately and inline.
    DomWalker.Visitor visitor = createMock(DomWalker.Visitor.class);
    expect(visitor.visit(gadget, root))
        .andReturn(DomWalker.Visitor.VisitStatus.RESERVE_NODE).once();
    expect(visitor.visit(gadget, child1))
        .andReturn(DomWalker.Visitor.VisitStatus.RESERVE_NODE).once();
    expect(visitor.visit(gadget, child2))
        .andReturn(DomWalker.Visitor.VisitStatus.RESERVE_NODE).once();
    expect(visitor.visit(gadget, subchild1))
        .andReturn(DomWalker.Visitor.VisitStatus.RESERVE_NODE).once();
    expect(visitor.visit(gadget, text1))
        .andReturn(DomWalker.Visitor.VisitStatus.RESERVE_NODE).once();
    expect(visitor.visit(gadget, text2))
        .andReturn(DomWalker.Visitor.VisitStatus.RESERVE_NODE).once();

    // All nodes are revisited in DFS order.
    List<Node> allReserved =
        Lists.newArrayList(root, child1, text1, child2, subchild1, text2);
    expect(visitor.revisit(gadget, allReserved))
        .andReturn(true).once();
    replay(visitor);

    MutableContent mc = getContent(1);  // Mutated each revisit.

    DomWalker.Rewriter rewriter = getRewriter(visitor);
    rewriter.rewrite(gadget, mc);

    // Verifying mutations on MutableContent completes the test.
    verify(mc);
  }

  @Test
  public void reserveRootPrecludesAllElse() throws Exception {
    Gadget gadget = gadget();

    // Visitor1 reserves root, visitor2 never gets anything.
    DomWalker.Visitor visitor1 = createMock(DomWalker.Visitor.class);
    expect(visitor1.visit(gadget, root))
        .andReturn(DomWalker.Visitor.VisitStatus.RESERVE_TREE).once();
    List<Node> allReserved = Lists.newArrayList(root);
    expect(visitor1.revisit(gadget, allReserved))
        .andReturn(true).once();
    DomWalker.Visitor visitor2 = createMock(DomWalker.Visitor.class);
    replay(visitor1, visitor2);

    MutableContent mc = getContent(1);  // Mutated once by revisit.

    DomWalker.Rewriter rewriter = getRewriter(visitor1, visitor2);
    rewriter.rewrite(gadget, mc);

    // Verifying mutations on MutableContent completes the test.
    verify(mc);
  }

  @Test
  public void allMixedModes() throws Exception {
    Gadget gadget = gadget();

    // Visitor1 reserves single text node 1
    DomWalker.Visitor visitor1 = createMock(DomWalker.Visitor.class);
    expect(visitor1.visit(gadget, root))
        .andReturn(DomWalker.Visitor.VisitStatus.BYPASS).once();
    expect(visitor1.visit(gadget, child1))
        .andReturn(DomWalker.Visitor.VisitStatus.RESERVE_NODE).once();
    expect(visitor1.visit(gadget, text1))
        .andReturn(DomWalker.Visitor.VisitStatus.BYPASS).once();
    expect(visitor1.visit(gadget, child2))
        .andReturn(DomWalker.Visitor.VisitStatus.BYPASS).once();
    // No visitation of text2 for visitor1 since visitor2 reserves the tree.
    expect(visitor1.visit(gadget, subchild1))
        .andReturn(DomWalker.Visitor.VisitStatus.BYPASS).once();
    // No modification the second time around.
    List<Node> reserved1 = Lists.newArrayList(child1);
    expect(visitor1.revisit(gadget, reserved1))
        .andReturn(false).once();

    // Visitor2 reserves tree of subchild 1
    DomWalker.Visitor visitor2 = createMock(DomWalker.Visitor.class);
    expect(visitor2.visit(gadget, root))
        .andReturn(DomWalker.Visitor.VisitStatus.BYPASS).once();
    // No visitation of v1-reserved child 1
    expect(visitor2.visit(gadget, text1))
        .andReturn(DomWalker.Visitor.VisitStatus.BYPASS).once();
    expect(visitor2.visit(gadget, child2))
        .andReturn(DomWalker.Visitor.VisitStatus.BYPASS).once();
    expect(visitor2.visit(gadget, subchild1))
        .andReturn(DomWalker.Visitor.VisitStatus.RESERVE_TREE).once();
    List<Node> reserved2 = Lists.newArrayList(subchild1);
    expect(visitor2.revisit(gadget, reserved2))
        .andReturn(true).once();

    // Visitor3 modifies child 2
    DomWalker.Visitor visitor3 = createMock(DomWalker.Visitor.class);
    expect(visitor3.visit(gadget, root))
        .andReturn(DomWalker.Visitor.VisitStatus.BYPASS).once();
    // No visitation of v1-reserved child 1
    expect(visitor3.visit(gadget, text1))
        .andReturn(DomWalker.Visitor.VisitStatus.BYPASS).once();
    expect(visitor3.visit(gadget, child2))
        .andReturn(DomWalker.Visitor.VisitStatus.MODIFY).once();
    // No visitation of tree of subchild 1

    replay(visitor1, visitor2, visitor3);

    MutableContent mc = getContent(2);  // Once v2.revisit(), once v3.visit()

    DomWalker.Rewriter rewriter = getRewriter(visitor1, visitor2, visitor3);
    rewriter.rewrite(gadget, mc);

    // As before, MutableContent verification is the test.
    verify(mc);
  }

  @Test
  public void rewriteThrowsRewritingExceptionIfGetDocumentIsNull() throws Exception {
    DomWalker.Visitor visitor1 = createMock(DomWalker.Visitor.class);
    DomWalker.Rewriter rewriter = getRewriter(visitor1);

    MutableContent mc = createMock(MutableContent.class);
    expect(mc.getDocument()).andReturn(null);
    expect(mc.getContent()).andReturn("hello!");
    replay(mc);

    Gadget gadget = gadget();
    boolean exceptionCaught = false;
    try {
      rewriter.rewrite(gadget, mc);
    } catch (RewritingException e) {
      assertEquals(e.getHttpStatusCode(),
                   HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      exceptionCaught = true;
    }

    assertTrue(exceptionCaught);
  }

  private DomWalker.Rewriter getRewriter(DomWalker.Visitor... visitors) {
    return new DomWalker.Rewriter(Lists.newArrayList(visitors));
  }

  private MutableContent getContent(int docChangedTimes) {
    MutableContent mc = createMock(MutableContent.class);
    expect(mc.getDocument()).andReturn(doc).once();
    if (docChangedTimes > 0) {
      mc.documentChanged();
      expectLastCall().times(docChangedTimes);
    }
    replay(mc);
    return mc;
  }
}
