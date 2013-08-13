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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.rewrite.DomWalker.Visitor.VisitStatus;
import org.apache.shindig.gadgets.uri.ProxyUriManager;
import org.easymock.Capture;
import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

/**
 * Test of proxying rewriter
 */
public class ProxyingVisitorTest extends DomWalkerTestBase {
  private static final String URL_STRING = "http://www.foo.com/";
  private static final Map<String, String> ALL_RESOURCES = ProxyingVisitor.Tags
      .ALL_RESOURCES.getResourceTags();

  @Test
  public void imgVisitReserved() throws Exception {
    checkVisitReserved("img", true);
  }

  @Test
  public void inputVisitReserved() throws Exception {
    checkVisitReserved("input", true);
  }

  @Test
  public void bodyVisitReserved() throws Exception {
    checkVisitReserved("body", true);
  }

  @Test
  public void embedVisitReserved() throws Exception {
    checkVisitReserved("embed", false);
  }

  @Test
  public void csslinkVisitReserved() throws Exception {
    checkVisitReserved("link", true, "rel", "stylesheet", "type", "text/css");
  }

  @Test
  public void linkWithNoRelVisitReserved() throws Exception {
    checkVisitReserved("link", false, "type", "text/css");
  }

  @Test
  public void linkWithNoTypeVisitReserved() throws Exception {
    checkVisitReserved("link", false, "rel", "stylesheet");
  }

  @Test
  public void altlinkVisitReserved() throws Exception {
    checkVisitReserved("link", false, "rel", "alternate", "hreflang", "el");
  }

  @Test
  public void scriptVisitReserved() throws Exception {
    checkVisitReserved("script", true);
  }

  @Test
  public void objectVisitReserved() throws Exception {
    checkVisitReserved("object", false);
  }

  @Test
  public void otherVisitNotReserved() throws Exception {
    checkVisitReserved("other", false);
  }

  @Test
  public void imgWithEmptySrc() throws Exception {
    Node node = elem("img", "src", "");
    ContentRewriterFeature.Config config = createMock(ContentRewriterFeature.Config.class);
    expect(config.shouldRewriteURL("")).andReturn(true).anyTimes();
    expect(config.shouldRewriteTag("img")).andReturn(true).anyTimes();
    replay(config);

    ProxyingVisitor rewriter = new ProxyingVisitor(config, null,
        ProxyingVisitor.Tags.SCRIPT,
        ProxyingVisitor.Tags.STYLESHEET,
        ProxyingVisitor.Tags.EMBEDDED_IMAGES);
    VisitStatus status = rewriter.visit(null, node);
    verify(config);

    assertEquals("Empty attribute should not be rewritten", VisitStatus.BYPASS, status);
  }

  private void checkVisitReserved(String tag, boolean result, String ... attrs) throws Exception {
    tag = tag.toLowerCase();
    assertEquals(result, getVisitReserved(tag, true, true, attrs));
    assertEquals(result, getVisitReserved(tag.toUpperCase(), true, true, attrs));
    assertFalse(getVisitReserved(tag, false, true, attrs));
    assertFalse(getVisitReserved(tag, true, false, attrs));
    assertFalse(getVisitReserved(tag, false, false, attrs));
  }

  private boolean getVisitReserved(String tag, boolean resUrl, boolean resTag, String ... attrs) throws Exception {
    // Reserved when lower-case and both URL and Tag reserved.
    String attrName = ALL_RESOURCES.get(tag.toLowerCase());
    attrName = attrName != null ? attrName : "src";

    ArrayList <String> attrsList = Lists.newArrayList(attrs);
    attrsList.add(0, attrName);
    attrsList.add(1, URL_STRING);
    attrs = attrsList.toArray(attrs);
    Node node = elem(tag, attrs);
    ContentRewriterFeature.Config config = createMock(ContentRewriterFeature.Config.class);
    expect(config.shouldRewriteURL(URL_STRING)).andReturn(resUrl).anyTimes();
    expect(config.shouldRewriteTag(tag.toLowerCase())).andReturn(resTag).anyTimes();
    replay(config);

    ProxyingVisitor rewriter = new ProxyingVisitor(config, null,
        ProxyingVisitor.Tags.SCRIPT,
        ProxyingVisitor.Tags.STYLESHEET,
        ProxyingVisitor.Tags.EMBEDDED_IMAGES);
    VisitStatus status = rewriter.visit(null, node);
    verify(config);

    return status != VisitStatus.BYPASS;
  }

  @Test
  public void revisitModifyValidSkipInvalid() throws Exception {
    // Batch test: ensures in-order modification.
    // Includes one mod and one skip.
    // No need to test invalid nodes since visit() and DomWalker tests preclude this.
    String scriptSrc = "http://script.com/foo.js";
    String imgSrc = "http://script.com/foo.jpg";
    Element e1 = elem("script", "src", scriptSrc);
    Element e2 = elem("script", "src", "^!,,|BLARGH");
    Element e3 = elem("IMG", "src", imgSrc);
    Element e4 = elem("script", "src", " " + scriptSrc + " ");
    List<Node> nodes = ImmutableList.<Node>of(e1, e2, e3, e4);
    ProxyUriManager uriManager = createMock(ProxyUriManager.class);
    Uri rewrittenUri = Uri.parse("http://bar.com/");
    List<Uri> returned = Lists.newArrayList(rewrittenUri, rewrittenUri, rewrittenUri);
    ContentRewriterFeature.Config config = createMock(ContentRewriterFeature.Config.class);
    Integer expires = 3;
    expect(config.getExpires()).andReturn(expires).once();
    expect(config);
    Capture<List<ProxyUriManager.ProxyUri>> cap = new Capture<List<ProxyUriManager.ProxyUri>>();
    Capture<Integer> intCap = new Capture<Integer>();
    expect(uriManager.make(capture(cap), capture(intCap))).andReturn(returned).once();
    replay(config, uriManager);
    Gadget gadget = gadget();

    ProxyingVisitor rewriter = new ProxyingVisitor(config, uriManager,
        ProxyingVisitor.Tags.SCRIPT,
        ProxyingVisitor.Tags.STYLESHEET,
        ProxyingVisitor.Tags.EMBEDDED_IMAGES);
    assertTrue(rewriter.revisit(gadget, nodes));
    verify(config, uriManager);

    assertEquals(3, cap.getValue().size());
    assertEquals(Uri.parse(scriptSrc), cap.getValue().get(0).getResource());
    assertEquals(Uri.parse(imgSrc), cap.getValue().get(1).getResource());
    assertEquals(Uri.parse(scriptSrc), cap.getValue().get(2).getResource());
    assertSame(expires, intCap.getValue());
    assertEquals(rewrittenUri.toString(), e1.getAttribute("src"));
    assertEquals("^!,,|BLARGH", e2.getAttribute("src"));
    assertEquals(rewrittenUri.toString(), e3.getAttribute("src"));
    assertEquals(rewrittenUri.toString(), e4.getAttribute("src"));

    // Test that the html tag context has been correctly filled.
    assertEquals("script", cap.getValue().get(0).getHtmlTagContext());
    assertEquals("img", cap.getValue().get(1).getHtmlTagContext());
    assertEquals("script", cap.getValue().get(2).getHtmlTagContext());
  }
}
