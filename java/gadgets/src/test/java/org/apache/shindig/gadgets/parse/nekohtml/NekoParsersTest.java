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
package org.apache.shindig.gadgets.parse.nekohtml;

import org.apache.shindig.gadgets.parse.GadgetHtmlParser;
import org.apache.shindig.gadgets.parse.HtmlSerialization;
import org.apache.shindig.gadgets.parse.ParseModule;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.cyberneko.html.xercesbridge.XercesBridge;
import org.w3c.dom.Document;

import junit.framework.TestCase;

/**
 * Test behavior of simplified HTML parser
 */
public class NekoParsersTest extends TestCase {

  /** The vm line separator */
  private static final String EOL = System.getProperty( "line.separator" );

  private NekoSimplifiedHtmlParser simple = new NekoSimplifiedHtmlParser(
        new ParseModule.DOMImplementationProvider().get());
  private NekoHtmlParser full = new NekoHtmlParser(
        new ParseModule.DOMImplementationProvider().get());

  public void testDocWithDoctype() throws Exception {
    // Note that doctype is properly retained
    String content = IOUtils.toString(this.getClass().getClassLoader().
        getResourceAsStream("org/apache/shindig/gadgets/parse/nekohtml/test.html"));
    String expected = IOUtils.toString(this.getClass().getClassLoader().
        getResourceAsStream("org/apache/shindig/gadgets/parse/nekohtml/test-expected.html"));
    String expected_full = removeDoctypeForXml4j(expected);
    parseAndCompareBalanced(content, expected_full, full);
    parseAndCompareBalanced(content, expected, simple);
  }

  public void testDocNoDoctype() throws Exception {
    // Note that no doctype is properly created when none specified
    String content = IOUtils.toString(this.getClass().getClassLoader().
        getResourceAsStream("org/apache/shindig/gadgets/parse/nekohtml/test-fulldocnodoctype.html"));
    assertNull(full.parseDom(content).getDoctype());
    assertNull(simple.parseDom(content).getDoctype());
  }

  public void testNotADocument() throws Exception {
    // Note that no doctype is injected for fragments
    String content = IOUtils.toString(this.getClass().getClassLoader().
        getResourceAsStream("org/apache/shindig/gadgets/parse/nekohtml/test-fragment.html"));
    String expected = IOUtils.toString(this.getClass().getClassLoader().
        getResourceAsStream("org/apache/shindig/gadgets/parse/nekohtml/test-fragment-expected.html"));
    parseAndCompareBalanced(content, expected, full);
    parseAndCompareBalanced(content, expected, simple);
  }

  public void testNoBody() throws Exception {
    // Note that no doctype is injected for fragments
    String content = IOUtils.toString(this.getClass().getClassLoader().
        getResourceAsStream("org/apache/shindig/gadgets/parse/nekohtml/test-headnobody.html"));
    String expected = IOUtils.toString(this.getClass().getClassLoader().
        getResourceAsStream(
        "org/apache/shindig/gadgets/parse/nekohtml/test-headnobody-expected.html"));
    parseAndCompareBalanced(content, expected, full);
    parseAndCompareBalanced(content, expected, simple);
  }

  public void testAmpersand() throws Exception {
    // Note that no doctype is injected for fragments
    String content = IOUtils.toString(this.getClass().getClassLoader().
        getResourceAsStream("org/apache/shindig/gadgets/parse/nekohtml/test-with-ampersands.html"));
    String expected = IOUtils.toString(this.getClass().getClassLoader().
        getResourceAsStream(
        "org/apache/shindig/gadgets/parse/nekohtml/test-with-ampersands-expected.html"));
    String expected_full = removeDoctypeForXml4j(expected);
    parseAndCompareBalanced(content, expected_full, full);
    parseAndCompareBalanced(content, expected, simple);
  }

  private void parseAndCompareBalanced(String content, String expected, GadgetHtmlParser parser)
      throws Exception {
    Document document = parser.parseDom(content);
    expected = StringUtils.replace(expected, EOL, "\n");
    assertEquals(expected, HtmlSerialization.serialize(document));
  }

  protected String removeDoctypeForXml4j(String content) {
    String VERSION = XercesBridge.getInstance().getVersion();
    return VERSION.startsWith("XML4J") ? content.replaceFirst("[^\n]*\n", "")
        : content;
  }
}
