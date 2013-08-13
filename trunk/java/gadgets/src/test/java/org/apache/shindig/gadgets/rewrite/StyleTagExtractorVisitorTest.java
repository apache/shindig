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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.rewrite.CssResponseRewriter.UriMaker;
import org.apache.shindig.gadgets.rewrite.DomWalker.Visitor.VisitStatus;
import org.apache.shindig.gadgets.uri.PassthruManager;
import org.apache.shindig.gadgets.uri.ProxyUriManager;

import org.junit.Before;
import org.junit.Test;

import org.w3c.dom.Comment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import java.util.List;

public class StyleTagExtractorVisitorTest extends DomWalkerTestBase {
  private ProxyUriManager proxyUriManager;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    proxyUriManager = new PassthruManager();
  }

  @Test
  public void visitBypassesComment() throws Exception {
    Comment comment = doc.createComment("comment");
    assertEquals(VisitStatus.BYPASS, getVisitStatus(comment));
  }

  @Test
  public void visitBypassesText() throws Exception {
    Text text = doc.createTextNode("text");
    assertEquals(VisitStatus.BYPASS, getVisitStatus(text));
  }

  @Test
  public void visitBypassesNonStyle() throws Exception {
    Node node = elem("div");
    assertEquals(VisitStatus.BYPASS, getVisitStatus(node));
  }

  @Test
  public void visitBypassesStyleWhenRewriterOff() throws Exception {
    assertEquals(VisitStatus.BYPASS, getVisitStatus(config(false, true, true), elem("style")));
  }

  @Test
  public void visitBypassesStyleWhenStyleTagNotIncluded() throws Exception {
    assertEquals(VisitStatus.BYPASS, getVisitStatus(config(true, false, true), elem("style")));
  }

  @Test
  public void visitReservesStyleNode() throws Exception {
    assertEquals(VisitStatus.RESERVE_NODE, getVisitStatus(elem("style")));
  }

  @Test
  public void visitReservesCasedStyleNode() throws Exception {
    assertEquals(VisitStatus.RESERVE_NODE, getVisitStatus(elem("sTyLE")));
  }

  @Test
  public void revisitNothingExtracted() throws Exception {
    Gadget gadget = gadget();
    CssResponseRewriter cssRewriter = createMock(CssResponseRewriter.class);
    replay(cssRewriter);

    // Tag name isn't inspected since visit() filters this.
    List<Node> nodes = ImmutableList.of();
    Node head = addNodesToHtml(nodes);

    assertFalse(getRevisitStatus(gadget, true, cssRewriter, nodes));
    verify(cssRewriter);
    assertEquals(0, head.getChildNodes().getLength());
  }

  @Test
  public void revisitExtractSpecRelative() throws Exception {
    Uri base = GADGET_URI;
    Gadget gadget = gadget();
    CssResponseRewriter cssRewriter = createMock(CssResponseRewriter.class);
    Element elem1 = elem("elem1");
    Element elem2 = elem("elem2");
    String urlStr1 = "http://foo.com/1.css";
    List<String> extractedUrls1 = ImmutableList.of(urlStr1);
    String urlStr2 = "http://bar.com/1.css";
    List<String> extractedUrls2 = ImmutableList.of(urlStr2);
    expect(cssRewriter.rewrite(eq(elem1), eq(base), isA(UriMaker.class), eq(true), eq(gadget.getContext())))
        .andReturn(extractedUrls1).once();
    expect(cssRewriter.rewrite(eq(elem2), eq(base), isA(UriMaker.class), eq(true), eq(gadget.getContext())))
        .andReturn(extractedUrls2).once();
    replay(cssRewriter);

    // Tag name isn't inspected since visit() filters this.
    List<Node> nodes = ImmutableList.<Node>of(elem1, elem2);
    Node head = addNodesToHtml(nodes);

    assertTrue(getRevisitStatus(gadget, true, cssRewriter, nodes));
    verify(cssRewriter);
    assertEquals(2, head.getChildNodes().getLength());
    Element child1 = (Element)head.getChildNodes().item(0);
    assertEquals("link", child1.getTagName());
    assertEquals("stylesheet", child1.getAttribute("rel"));
    assertEquals("text/css", child1.getAttribute("type"));
    // PassthruManager doesn't modify the inbound URI.
    assertEquals(urlStr1, child1.getAttribute("href"));
    Element child2 = (Element)head.getChildNodes().item(1);
    assertEquals("link", child2.getTagName());
    assertEquals("stylesheet", child2.getAttribute("rel"));
    assertEquals("text/css", child2.getAttribute("type"));
    // PassthruManager doesn't modify the inbound URI.
    assertEquals(urlStr2, child2.getAttribute("href"));
  }

  @Test
  public void revisitExtractViewHrefRelative() throws Exception {
    Uri base = Uri.parse("http://view.com/viewbase.xml");
    Gadget gadget = gadget(true, true, base);
    CssResponseRewriter cssRewriter = createMock(CssResponseRewriter.class);
    Element elem1 = elem("elem1");
    Element elem2 = elem("elem2");
    String urlStr1 = "http://foo.com/1.css";
    List<String> extractedUrls1 = ImmutableList.of(urlStr1);
    String urlStr2 = "http://bar.com/1.css";
    List<String> extractedUrls2 = ImmutableList.of(urlStr2);
    expect(cssRewriter.rewrite(eq(elem1), eq(base), isA(UriMaker.class), eq(true), eq(gadget.getContext())))
        .andReturn(extractedUrls1).once();
    expect(cssRewriter.rewrite(eq(elem2), eq(base), isA(UriMaker.class), eq(true), eq(gadget.getContext())))
        .andReturn(extractedUrls2).once();
    replay(cssRewriter);

    // Tag name isn't inspected since visit() filters this.
    List<Node> nodes = ImmutableList.<Node>of(elem1, elem2);
    Node head = addNodesToHtml(nodes);

    assertTrue(getRevisitStatus(gadget, true, cssRewriter, nodes));
    verify(cssRewriter);
    assertEquals(2, head.getChildNodes().getLength());
    Element child1 = (Element)head.getChildNodes().item(0);
    assertEquals("link", child1.getTagName());
    assertEquals("stylesheet", child1.getAttribute("rel"));
    assertEquals("text/css", child1.getAttribute("type"));
    // PassthruManager doesn't modify the inbound URI.
    assertEquals(urlStr1, child1.getAttribute("href"));
    Element child2 = (Element)head.getChildNodes().item(1);
    assertEquals("link", child2.getTagName());
    assertEquals("stylesheet", child2.getAttribute("rel"));
    assertEquals("text/css", child2.getAttribute("type"));
    // PassthruManager doesn't modify the inbound URI.
    assertEquals(urlStr2, child2.getAttribute("href"));
  }

  @Test
  public void revisitExtractSpecRelativeDisabled() throws Exception {
    Uri base = GADGET_URI;
    Gadget gadget = gadget();
    CssResponseRewriter cssRewriter = createMock(CssResponseRewriter.class);
    Element elem1 = elem("elem1");
    Element elem2 = elem("elem2");
    List<String> extractedUrls1 = ImmutableList.of();
    List<String> extractedUrls2 = ImmutableList.of();
    expect(cssRewriter.rewrite(eq(elem1), eq(base), isA(UriMaker.class), eq(true), eq(gadget.getContext())))
        .andReturn(extractedUrls1).once();
    expect(cssRewriter.rewrite(eq(elem2), eq(base), isA(UriMaker.class), eq(true), eq(gadget.getContext())))
        .andReturn(extractedUrls2).once();
    replay(cssRewriter);

    // Tag name isn't inspected since visit() filters this.
    List<Node> nodes = ImmutableList.<Node>of(elem1, elem2);
    Node head = addNodesToHtml(nodes);

    assertFalse(getRevisitStatus(gadget, false, cssRewriter, nodes));
    verify(cssRewriter);
    assertEquals(0, head.getChildNodes().getLength());
  }

  private VisitStatus getVisitStatus(Node node) throws Exception {
    return getVisitStatus(config(true, true, true), node);
  }

  private VisitStatus getVisitStatus(ContentRewriterFeature.Config config, Node node)
      throws Exception {
    // Pass null for all unused (viz. visitor()) APIs to underscore their lack of use.
    return new StyleTagExtractorVisitor(config, null, null).visit(null, node);
  }

  private boolean getRevisitStatus(
      Gadget gadget, boolean shouldRewriteUrl, CssResponseRewriter cssRewriter, List<Node> nodes)
      throws Exception {
    return new StyleTagExtractorVisitor(
        config(true, true, shouldRewriteUrl), cssRewriter, proxyUriManager)
        .revisit(gadget, nodes);
  }

  private ContentRewriterFeature.Config config(
      boolean enabled, boolean styleInc, boolean rewriteUrl) {
    ContentRewriterFeature.Config config = createMock(ContentRewriterFeature.Config.class);
    expect(config.isRewriteEnabled()).andReturn(enabled).anyTimes();
    expect(config.getIncludedTags())
        .andReturn(ImmutableSet.of(styleInc ? "style" : "foo")).anyTimes();
    expect(config.shouldRewriteURL(isA(String.class))).andReturn(rewriteUrl).anyTimes();
    replay(config);
    return config;
  }

  private Node addNodesToHtml(List<Node> nodes) throws Exception {
    Node html = elem("html");
    Node head = elem("head");
    Node body = elem("body");
    html.appendChild(head);
    html.appendChild(body);
    for (Node node : nodes) {
      body.appendChild(node);
    }
    html.getOwnerDocument().appendChild(html);
    return head;
  }
}
