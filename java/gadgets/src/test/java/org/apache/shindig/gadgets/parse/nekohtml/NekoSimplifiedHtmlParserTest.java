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

import org.apache.commons.io.IOUtils;
import org.apache.shindig.gadgets.parse.HtmlSerializer;
import org.apache.shindig.gadgets.parse.ParseModule;

import junit.framework.TestCase;
import org.w3c.dom.Document;

/**
 * Test behavior of simplified HTML parser
 */
public class NekoSimplifiedHtmlParserTest extends TestCase {

  public void testParser() throws Exception {
    String content = IOUtils.toString(this.getClass().getClassLoader().
        getResourceAsStream("org/apache/shindig/gadgets/parse/nekohtml/test.html"));
    String expected = IOUtils.toString(this.getClass().getClassLoader().
        getResourceAsStream("org/apache/shindig/gadgets/parse/nekohtml/test-expected.html"));
    parseAndCompareBalanced(content, expected);
  }

  private void parseAndCompareBalanced(String content, String expected) throws Exception {
    NekoSimplifiedHtmlParser builder = new NekoSimplifiedHtmlParser(
        new ParseModule.DOMImplementationProvider().get());
    Document document = builder.parseDom(content);
    assertEquals(expected, HtmlSerializer.serialize(document));
  }
}
