/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.gadgets.rewrite;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.rewrite.DomWalker.Visitor.VisitStatus;
import org.apache.shindig.gadgets.uri.ProxyUriManager;
import org.easymock.Capture;
import org.junit.Test;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.util.List;

/**
 * Test of proxying rewriter
 */
public class ProxyingVisitorTest extends DomWalkerTestBase {
  private static final String URL_STRING = "http://www.foo.com/";
  
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
  public void linkVisitReserved() throws Exception {
    checkVisitReserved("link", true);
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
  
  private void checkVisitReserved(String tag, boolean result) throws Exception {
    tag = tag.toLowerCase();
    assertEquals(result, ProxyingVisitor.RESOURCE_TAGS.containsKey(tag));
    assertEquals(result, getVisitReserved(tag, true, true));
    assertEquals(result, getVisitReserved(tag.toUpperCase(), true, true));
    assertFalse(getVisitReserved(tag, false, true));
    assertFalse(getVisitReserved(tag, true, false));
    assertFalse(getVisitReserved(tag, false, false));
  }
  
  private boolean getVisitReserved(String tag, boolean resUrl, boolean resTag) throws Exception {
    // Reserved when lower-case and both URL and Tag reserved.
    String attrName = ProxyingVisitor.RESOURCE_TAGS.get(tag.toLowerCase());
    Node node = elem(tag, attrName != null ? attrName : "src", URL_STRING);
    ContentRewriterFeature.Config config = createMock(ContentRewriterFeature.Config.class);
    expect(config.shouldRewriteURL(URL_STRING)).andReturn(resUrl).anyTimes();
    expect(config.shouldRewriteTag(tag.toLowerCase())).andReturn(resTag).anyTimes();
    replay(config);    
    
    ProxyingVisitor rewriter = new ProxyingVisitor(config, null);
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
    List<Uri> returned = Lists.newArrayList(rewrittenUri, null, rewrittenUri, rewrittenUri);
    ContentRewriterFeature.Config config = createMock(ContentRewriterFeature.Config.class);
    Integer expires = new Integer(3);
    expect(config.getExpires()).andReturn(expires).once();
    expect(config);
    Capture<List<ProxyUriManager.ProxyUri>> cap = new Capture<List<ProxyUriManager.ProxyUri>>();
    Capture<Integer> intCap = new Capture<Integer>();
    expect(uriManager.make(capture(cap), capture(intCap))).andReturn(returned).once();
    replay(config, uriManager);
    Gadget gadget = gadget();

    ProxyingVisitor rewriter = new ProxyingVisitor(config, uriManager);
    assertTrue(rewriter.revisit(gadget, nodes));
    verify(config, uriManager);

    assertEquals(4, cap.getValue().size());
    assertEquals(Uri.parse(scriptSrc), cap.getValue().get(0).getResource());
    assertNull(cap.getValue().get(1));
    assertEquals(Uri.parse(imgSrc), cap.getValue().get(2).getResource());
    assertEquals(Uri.parse(scriptSrc), cap.getValue().get(3).getResource());
    assertSame(expires, intCap.getValue());
    assertEquals(rewrittenUri.toString(), e1.getAttribute("src"));
    assertEquals("^!,,|BLARGH", e2.getAttribute("src"));
    assertEquals(rewrittenUri.toString(), e3.getAttribute("src"));
    assertEquals(rewrittenUri.toString(), e4.getAttribute("src"));
  }
}
