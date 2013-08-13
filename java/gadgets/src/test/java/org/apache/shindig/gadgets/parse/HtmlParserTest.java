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

import org.apache.shindig.gadgets.parse.nekohtml.NekoSimplifiedHtmlParser;
import org.apache.shindig.gadgets.rewrite.XPathWrapper;

import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;

/**
 * Note these tests are of marginal use. Consider removing. More useful tests would exercise
 * the capability of the parser to handle strange HTML.
 */
public class HtmlParserTest extends Assert {

  private final GadgetHtmlParser nekoParser = new NekoSimplifiedHtmlParser(
      new ParseModule.DOMImplementationProvider().get());

  @Test
  public void testParseSimpleString() throws Exception {
    parseSimpleString(nekoParser);
  }

  private void parseSimpleString(GadgetHtmlParser htmlParser) throws Exception {
    Document doc = htmlParser.parseDom("content");
    XPathWrapper wrapper = new XPathWrapper(doc);
    assertEquals("content", wrapper.getValue("/html/body"));
  }

  @Test
  public void testParseTagWithStringContents() throws Exception {
    parseTagWithStringContents(nekoParser);
  }

  void parseTagWithStringContents(GadgetHtmlParser htmlParser) throws Exception {
    Document doc = htmlParser.parseDom("<span>content</span>");
    XPathWrapper wrapper = new XPathWrapper(doc);
    assertEquals("content", wrapper.getValue("/html/body/span"));
  }

  @Test
  public void testParseTagWithAttributes() throws Exception {
    parseTagWithAttributes(nekoParser);
  }

  void parseTagWithAttributes(GadgetHtmlParser htmlParser) throws Exception {
    Document doc = htmlParser.parseDom("<div id=\"foo\">content</div>");
    XPathWrapper wrapper = new XPathWrapper(doc);
    assertEquals("content", wrapper.getValue("/html/body/div"));
    assertEquals("foo", wrapper.getValue("/html/body/div/@id"));
  }

  @Test
  public void testParseNestedContentWithNoCloseForBrAndHr() throws Exception {
    parseNestedContentWithNoCloseForBrAndHr(nekoParser);
  }

  void parseNestedContentWithNoCloseForBrAndHr(GadgetHtmlParser htmlParser) throws Exception {
    Document doc = htmlParser.parseDom("<div>x and y<br> and <hr>z</div>");
    XPathWrapper wrapper = new XPathWrapper(doc);
    assertEquals("x and y and z", wrapper.getValue("/html/body/div"));
    assertEquals(1, wrapper.getNodeList("/html/body/div/br").getLength());
    assertEquals(1, wrapper.getNodeList("/html/body/div/hr").getLength());
  }

  // TODO: figure out to what extent it makes sense to test "invalid"
  // HTML, semi-structured HTML, and comment parsing
}
