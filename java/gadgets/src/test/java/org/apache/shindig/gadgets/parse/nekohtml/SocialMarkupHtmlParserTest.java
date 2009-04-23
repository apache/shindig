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
package org.apache.shindig.gadgets.parse.nekohtml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.apache.commons.io.IOUtils;
import org.apache.shindig.gadgets.parse.ParseModule;
import org.apache.shindig.gadgets.parse.HtmlSerializer;
import org.apache.shindig.gadgets.spec.PipelinedData;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;

import java.util.List;

import com.google.common.collect.Lists;

/**
 * Test for the social markup parser.
 */
public class SocialMarkupHtmlParserTest {
  private SocialMarkupHtmlParser parser;
  private Document document;

  @Before
  public void setUp() throws Exception {
    parser = new SocialMarkupHtmlParser(new ParseModule.DOMImplementationProvider().get());
    
    String content = IOUtils.toString(this.getClass().getClassLoader().
        getResourceAsStream("org/apache/shindig/gadgets/parse/nekohtml/test-socialmarkup.html"));
    document = parser.parseDom(content);
  }

  @Test
  public void testSocialData() {
    // Verify elements are preserved in social data
    List<Element> scripts = getScripts("text/os-data");
    assertEquals(1, scripts.size());
    
    NodeList viewerRequests = scripts.get(0).getElementsByTagNameNS(
        PipelinedData.OPENSOCIAL_NAMESPACE, "ViewerRequest");
    assertEquals(1, viewerRequests.getLength());
    Element viewerRequest = (Element) viewerRequests.item(0);
    assertEquals("viewer", viewerRequest.getAttribute("key"));
    assertNull(viewerRequest.getFirstChild());
  }

  @Test
  public void testSocialTemplate() {
    // Verify elements and text content are preserved in social templates
    List<Element> scripts = getScripts("text/os-template");
    assertEquals(1, scripts.size());
    
    NodeList boldElements = scripts.get(0).getElementsByTagName("b");
    assertEquals(1, boldElements.getLength());
    Element boldElement = (Element) boldElements.item(0);
    assertEquals("Some ${viewer} content", boldElement.getTextContent());
    
    NodeList osHtmlElements = scripts.get(0).getElementsByTagNameNS(
        "http://ns.opensocial.org/2008/markup", "Html");
    assertEquals(1, osHtmlElements.getLength());
  }

  @Test
  public void testSocialTemplateSerialization() {
    String content = HtmlSerializer.serialize(document);
    assertTrue("Empty elements not preserved as XML inside template",
        content.contains("<img/>"));
  }

  @Test
  public void testJavascript() {
    // Verify text content is unmodified in javascript blocks
    List<Element> scripts = getScripts("text/javascript");
    assertEquals(1, scripts.size());
    
    NodeList boldElements = scripts.get(0).getElementsByTagName("b");
    assertEquals(0, boldElements.getLength());

    String scriptContent = scripts.get(0).getTextContent().trim();
    assertEquals("<b>Some ${viewer} content</b>", scriptContent);
  }

  @Test
  @org.junit.Ignore
  public void testPlainContent() {
    // Verify text content is preserved in non-script content
    NodeList spanElements = document.getElementsByTagName("span");
    assertEquals(1, spanElements.getLength());
    assertEquals("Some content", spanElements.item(0).getTextContent());
  }

  private List<Element> getScripts(final String type) {
    NodeIterator nodeIterator = ((DocumentTraversal) document)
    .createNodeIterator(document, NodeFilter.SHOW_ELEMENT,
        new NodeFilter() {
          public short acceptNode(Node n) {
            if ("script".equalsIgnoreCase(n.getNodeName()) &&
                type.equals(((Element) n).getAttribute("type"))) {
              return NodeFilter.FILTER_ACCEPT;
            }
            return NodeFilter.FILTER_REJECT;
          }
        }, false);
        
    List<Element> scripts = Lists.newArrayList();
    for (Node n = nodeIterator.nextNode(); n != null; n = nodeIterator.nextNode()) {
      scripts.add((Element) n);
    }
    
    return scripts;
  }
}
