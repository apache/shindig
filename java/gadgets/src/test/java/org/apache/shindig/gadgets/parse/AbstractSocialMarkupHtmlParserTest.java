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
package org.apache.shindig.gadgets.parse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;
import org.apache.shindig.gadgets.spec.PipelinedData;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Iterator;
import java.util.List;

/**
 * Test for the social markup parser.
 */
public abstract class AbstractSocialMarkupHtmlParserTest extends AbstractParsingTestBase {
  private GadgetHtmlParser parser;
  private Document document;

  protected abstract GadgetHtmlParser makeParser();

  @Before
  public void setUp() throws Exception {
    parser = makeParser();

    String content = loadFile("org/apache/shindig/gadgets/parse/test-socialmarkup.html");
    document = parser.parseDom(content);
  }

  @Test
  public void testSocialData() {
    // Verify elements are preserved in social data
    List<Element> scripts = SocialDataTags.getTags(document, SocialDataTags.OSML_DATA_TAG);
    assertEquals(1, scripts.size());

    NodeList viewerRequests = scripts.get(0).getElementsByTagNameNS(
        PipelinedData.OPENSOCIAL_NAMESPACE, "ViewerRequest");
    assertEquals(1, viewerRequests.getLength());
    Element viewerRequest = (Element) viewerRequests.item(0);
    assertEquals("viewer", viewerRequest.getAttribute("key"));
    assertEmpty(viewerRequest);
  }

  @Test
  public void testSocialTemplate() {
    // Verify elements and text content are preserved in social templates
    List<Element> scripts = SocialDataTags.getTags(document, SocialDataTags.OSML_TEMPLATE_TAG);
    assertEquals(1, scripts.size());

    assertEquals("template-id", scripts.get(0).getAttribute("id"));
    assertEquals("template-name", scripts.get(0).getAttribute("name"));
    assertEquals("template-tag", scripts.get(0).getAttribute("tag"));

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
    String content = HtmlSerialization.serialize(document);
    assertTrue("Empty elements not preserved as XML inside template",
        content.contains("<img/>"));
  }

  @Test
  public void testJavascript() {
    // Verify text content is unmodified in javascript blocks
    List<Element> scripts = SocialDataTags.getTags(document, "script");

    // Remove any OpenSocial-specific nodes.
    Iterator<Element> scriptIt = scripts.iterator();
    while (scriptIt.hasNext()) {
      if (SocialDataTags.isOpenSocialScript(scriptIt.next())) {
        scriptIt.remove();
      }
    }

    assertEquals(1, scripts.size());

    NodeList boldElements = scripts.get(0).getElementsByTagName("b");
    assertEquals(0, boldElements.getLength());

    String scriptContent = scripts.get(0).getTextContent().trim();
    assertEquals("<b>Some ${viewer} content</b>", scriptContent);
  }

  @Test
  public void testPlainContent() {
    // Verify text content is preserved in non-script content
    NodeList spanElements = document.getElementsByTagName("span");
    assertEquals(1, spanElements.getLength());
    assertEquals("Some content", spanElements.item(0).getTextContent());
  }

  @Test
  public void testCommentOrdering() {
    NodeList divElements = document.getElementsByTagName("div");
    assertEquals(1, divElements.getLength());
    NodeList children = divElements.item(0).getChildNodes();
    assertEquals(3, children.getLength());

    // Should be comment/text/comment, not comment/comment/text
    assertEquals(Node.COMMENT_NODE, children.item(0).getNodeType());
    assertEquals(Node.TEXT_NODE, children.item(1).getNodeType());
    assertEquals(Node.COMMENT_NODE, children.item(2).getNodeType());
  }

  @Test
  public void testInvalid() throws Exception {
    String content =
        "<html><div id=\"div_super\" class=\"div_super\" valign:\"middle\"></div></html>";
    Document doc = parser.parseDom(content);

    // Returns a bare Document with error text in it.
    Node body = doc.getElementsByTagName("body").item(0);

    assertTrue(body.getTextContent().contains("INVALID_CHARACTER_ERR"));
    assertTrue(body.getTextContent().contains(
        "Around ...<div id=\"div_super\" class=\"div_super\"..."));
    // Verify Serialization:
    assertTrue(HtmlSerialization.serialize(doc).contains("INVALID_CHARACTER_ERR"));
  }

  private void assertEmpty(Node n) {
    if (n.getChildNodes().getLength() != 0) {
      assertTrue(Strings.isNullOrEmpty(n.getTextContent()) ||
          StringUtils.isWhitespace(n.getTextContent()));
    }
  }
}
