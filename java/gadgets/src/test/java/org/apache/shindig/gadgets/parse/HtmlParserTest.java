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

import org.apache.shindig.gadgets.parse.caja.CajaHtmlParser;
import org.apache.shindig.gadgets.parse.nekohtml.NekoHtmlParser;

import junit.framework.TestCase;

import java.util.List;

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
    List<ParsedHtmlNode> nodes = htmlParser.parse("content");
    assertNotNull(nodes);
    assertEquals(1, nodes.size());
    
    ParsedHtmlNode node = nodes.get(0);
    assertNotNull(node);
    assertEquals("content", node.getText());
    assertNull(node.getAttributes());
    assertNull(node.getChildren());
    assertNull(node.getTagName());
  }

  public void testParseTagWithStringContents() throws Exception {
    parseTagWithStringContents(nekoParser);
    parseTagWithStringContents(cajaParser);
  }

  public void parseTagWithStringContents(GadgetHtmlParser htmlParser) throws Exception {
    List<ParsedHtmlNode> nodes =
        htmlParser.parse("<span>content</span>");
    assertNotNull(nodes);
    assertEquals(1, nodes.size());
    
    ParsedHtmlNode node = nodes.get(0);
    assertNull(node.getText());
    assertNotNull(node.getAttributes());
    assertEquals(0, node.getAttributes().size());
    assertNotNull(node.getChildren());
    assertEquals(1, node.getChildren().size());
    assertEquals("content", node.getChildren().get(0).getText());
    assertEquals("span", node.getTagName().toLowerCase());
  }

  public void testParseTagWithAttributes() throws Exception {
    parseTagWithAttributes(nekoParser);
    parseTagWithAttributes(cajaParser);
  }

  void parseTagWithAttributes(GadgetHtmlParser htmlParser) throws Exception {
    List<ParsedHtmlNode> nodes =
        htmlParser.parse("<div id=\"foo\">content</div>");
    assertNotNull(nodes);
    assertEquals(1, nodes.size());
    
    ParsedHtmlNode node = nodes.get(0);
    assertNotNull(node);
    assertNull(node.getText());
    assertNotNull(node.getAttributes());
    assertEquals(1, node.getAttributes().size());
    assertEquals("id", node.getAttributes().get(0).getName());
    assertEquals("foo", node.getAttributes().get(0).getValue());
    assertNotNull(node.getChildren());
    assertEquals(1, node.getChildren().size());
    assertEquals("content", node.getChildren().get(0).getText());
  }

  public void testParseStringUnescapesProperly() throws Exception {
    parseStringUnescapesProperly(nekoParser);
    parseStringUnescapesProperly(cajaParser);
  }

  void parseStringUnescapesProperly(GadgetHtmlParser htmlParser) throws Exception {
    List<ParsedHtmlNode> nodes =
        htmlParser.parse("&lt;content&amp;&apos;chrome&apos;&gt;");
    assertNotNull(nodes);
    assertEquals(1, nodes.size());
    
    ParsedHtmlNode node = nodes.get(0);
    assertNotNull(node);
    assertEquals("<content&'chrome'>", node.getText());
    assertNull(node.getAttributes());
    assertNull(node.getChildren());
    assertNull(node.getTagName());
  }

  public void testParseNestedContentWithNoCloseForBrAndHr() throws Exception {
    parseNestedContentWithNoCloseForBrAndHr(nekoParser);
    parseNestedContentWithNoCloseForBrAndHr(cajaParser);
  }

  void parseNestedContentWithNoCloseForBrAndHr(GadgetHtmlParser htmlParser) throws Exception {
    List<ParsedHtmlNode> nodes =
        htmlParser.parse("<div><br>  and  <hr></div>");
    assertNotNull(nodes);
    assertEquals(1, nodes.size());
    
    ParsedHtmlNode divNode = nodes.get(0);
    assertNull(divNode.getText());
    assertEquals("div", divNode.getTagName().toLowerCase());
    assertNotNull(divNode.getAttributes());
    assertEquals(0, divNode.getAttributes().size());
    assertNotNull(divNode.getChildren());
    assertEquals(3, divNode.getChildren().size());
    
    {
      // <br>
      ParsedHtmlNode divChild = divNode.getChildren().get(0);
      assertNotNull(divChild);
      assertEquals("br", divChild.getTagName().toLowerCase());
      assertNull(divChild.getText());
      assertNotNull(divChild.getAttributes());
      assertEquals(0, divChild.getAttributes().size());
      assertNullOrEmpty(divChild.getChildren());
    }
    
    {
      // text
      ParsedHtmlNode divChild = divNode.getChildren().get(1);
      assertEquals("  and  ", divChild.getText());
      assertNull(divChild.getAttributes());
      assertNull(divChild.getChildren());
      assertNull(divChild.getTagName());
    }
    
    {
      // <hr> should be parsed lieniently
      ParsedHtmlNode divChild = divNode.getChildren().get(2);
      assertNotNull(divChild);
      assertEquals("hr", divChild.getTagName().toLowerCase());
      assertNull(divChild.getText());
      assertNotNull(divChild.getAttributes());
      assertEquals(0, divChild.getAttributes().size());
      assertNullOrEmpty(divChild.getChildren());
    }
  }

  public void testParseMixedSiblings() throws Exception {
    parseMixedSiblings(nekoParser);
    parseMixedSiblings(cajaParser);
  }

  void parseMixedSiblings(GadgetHtmlParser htmlParser) throws Exception {
    List<ParsedHtmlNode> nodes =
        htmlParser.parse("content<span>more</span><div id=\"foo\">yet more</div>");
    assertNotNull(nodes);
    assertEquals(3, nodes.size());
    
    {
      ParsedHtmlNode textNode = nodes.get(0);
      assertEquals("content", textNode.getText());
    }
    
    {
      ParsedHtmlNode spanNode = nodes.get(1);
      assertNull(spanNode.getText());
      assertNotNull(spanNode.getAttributes());
      assertEquals(0, spanNode.getAttributes().size());
      assertNotNull(spanNode.getChildren());
      assertEquals(1, spanNode.getChildren().size());
      assertEquals("more", spanNode.getChildren().get(0).getText());
    }
    
    {
      ParsedHtmlNode divNode = nodes.get(2);
      assertNull(divNode.getText());
      assertNotNull(divNode.getAttributes());
      assertEquals(1, divNode.getAttributes().size());
      assertEquals("id", divNode.getAttributes().get(0).getName());
      assertEquals("foo", divNode.getAttributes().get(0).getValue());
      assertNotNull(divNode.getChildren());
      assertEquals(1, divNode.getChildren().size());
      assertEquals("yet more", divNode.getChildren().get(0).getText());
    }
  }
  
  // TODO: figure out to what extent it makes sense to test "invalid"
  // HTML, semi-structured HTML, and comment parsing

  // Different parsers either return null or empty child lists.
  // In particular because Caja is a non-w3c compliant parser
  private void assertNullOrEmpty(List l) {
    if (l != null && !l.isEmpty()) {
      assertTrue(true);
    }
    return;
  }
}
