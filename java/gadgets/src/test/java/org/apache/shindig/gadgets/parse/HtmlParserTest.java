/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.gadgets.parse;

import junit.framework.TestCase;
import org.apache.shindig.gadgets.parse.caja.CajaHtmlParser;
import org.apache.shindig.gadgets.parse.nekohtml.NekoHtmlParser;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Note these tests are of marginal use. Consider removing. More useful tests would exercise
 * the capability of the parser to handle strange HTML.
 */
public class HtmlParserTest extends TestCase {

  private final GadgetHtmlParser cajaParser = new CajaHtmlParser(
      new ParseModule.HTMLDocumentProvider());

  private final GadgetHtmlParser nekoParser = new NekoHtmlParser(
      new ParseModule.HTMLDocumentProvider());

  public void testParseSimpleString() throws Exception {
    parseSimpleString(cajaParser);
    parseSimpleString(nekoParser);
  }

  private void parseSimpleString(GadgetHtmlParser htmlParser) throws Exception {
    Document doc = htmlParser.parseDom("content");

    Node node = doc.getDocumentElement().getFirstChild();
    assertNotNull(node);
    assertEquals("content", node.getTextContent());
    assertNull(node.getAttributes());
    assertNullOrEmpty(node.getChildNodes());
    assertEquals(Node.TEXT_NODE, node.getNodeType());
  }

  public void testParseTagWithStringContents() throws Exception {
    parseTagWithStringContents(nekoParser);
    parseTagWithStringContents(cajaParser);
  }

  public void parseTagWithStringContents(GadgetHtmlParser htmlParser) throws Exception {
    Document doc = htmlParser.parseDom("<span>content</span>");

    Node node = doc.getDocumentElement().getFirstChild();
    assertEquals("content", node.getTextContent());
    assertEquals("span", node.getNodeName().toLowerCase());
  }

  public void testParseTagWithAttributes() throws Exception {
    parseTagWithAttributes(nekoParser);
    parseTagWithAttributes(cajaParser);
  }

  void parseTagWithAttributes(GadgetHtmlParser htmlParser) throws Exception {
    Document doc = htmlParser.parseDom("<div id=\"foo\">content</div>");

    Node node = doc.getDocumentElement().getFirstChild();
    assertNotNull(node);
    assertNotNull(node.getAttributes());
    assertEquals(1, node.getAttributes().getLength());
    assertEquals("id", node.getAttributes().item(0).getNodeName());
    assertEquals("foo", node.getAttributes().item(0).getNodeValue());
    assertNotNull(node.getChildNodes());
    assertEquals(1, node.getChildNodes().getLength());
    assertEquals("content", node.getChildNodes().item(0).getTextContent());
  }

  public void testParseStringUnescapesProperly() throws Exception {
    parseStringUnescapesProperly(nekoParser);
    parseStringUnescapesProperly(cajaParser);
  }

  void parseStringUnescapesProperly(GadgetHtmlParser htmlParser) throws Exception {
    Document doc = htmlParser.parseDom("&lt;content&amp;&apos;chrome&apos;&gt;");

    Node node = doc.getDocumentElement().getFirstChild();
    assertNotNull(node);
    assertEquals("<content&'chrome'>", node.getTextContent());
    assertNull(node.getAttributes());
    assertNullOrEmpty(node.getChildNodes());
  }

  public void testParseNestedContentWithNoCloseForBrAndHr() throws Exception {
    parseNestedContentWithNoCloseForBrAndHr(nekoParser);
    parseNestedContentWithNoCloseForBrAndHr(cajaParser);
  }

  void parseNestedContentWithNoCloseForBrAndHr(GadgetHtmlParser htmlParser) throws Exception {
    Document doc = htmlParser.parseDom("<div><br>  and  <hr></div>");

    Node divNode = doc.getDocumentElement().getFirstChild();
    assertEquals("div", divNode.getNodeName().toLowerCase());
    assertNotNull(divNode.getAttributes());
    assertEquals(0, divNode.getAttributes().getLength());
    assertNotNull(divNode.getChildNodes());
    assertEquals(3, divNode.getChildNodes().getLength());
    
    {
      // <br>
      Node divChild = divNode.getChildNodes().item(0);
      assertNotNull(divChild);
      assertEquals("br", divChild.getNodeName().toLowerCase());
      assertNotNull(divChild.getAttributes());
      assertEquals(0, divChild.getAttributes().getLength());
      assertEquals(0, divChild.getChildNodes().getLength());
    }
    
    {
      // text
      Node divChild = divNode.getChildNodes().item(1);
      assertEquals("  and  ", divChild.getTextContent());
      assertNull(divChild.getAttributes());
      assertNullOrEmpty(divChild.getChildNodes());
    }
    
    {
      // <hr> should be parsed lieniently
      Node divChild = divNode.getChildNodes().item(2);
      assertNotNull(divChild);
      assertEquals("hr", divChild.getNodeName().toLowerCase());
      assertNotNull(divChild.getAttributes());
      assertEquals(0, divChild.getAttributes().getLength());
      assertEquals(0, divChild.getChildNodes().getLength());
    }
  }

  // TODO: figure out to what extent it makes sense to test "invalid"
  // HTML, semi-structured HTML, and comment parsing

  // Different parsers either return null or empty child lists.
  // In particular because Caja is a non-w3c compliant parser
  private void assertNullOrEmpty(NodeList l) {
    assertTrue(l == null || l.getLength() == 0);
  }
}
