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

import junit.framework.TestCase;
import org.apache.shindig.gadgets.parse.ParseModule;
import org.apache.xml.serialize.HTMLSerializer;
import org.apache.xml.serialize.OutputFormat;
import org.w3c.dom.Document;

import java.io.IOException;
import java.io.StringWriter;

/**
 * Test behavior of simplified HTML parser
 */
public class NekoSimplifiedHtmlParserTest extends TestCase {

  public void testUnbalanced() throws Exception {
    parseAndCompareBalanced("<html><body><center>content</body></html>",
        "<html><body><center>content</body></html>");
  }

  public void testUnbalanced2() throws Exception {
    parseAndCompareBalanced("<html><body><img>content<img>content</body></html>",
        "<HTML><body><IMG>content<IMG>content</body></HTML>");
  }

  public void testUnbalanced3() throws Exception {
    parseAndCompareBalanced("<html><body><select><option>content<option></body></html>",
        "<html><body><select><option>content<option></body></html>");
  }

  public void testUnbalanced4() throws Exception {
    parseAndCompareBalanced("<html><body>Something awful</html>",
        "<HTML><body>Something awful</body></HTML>");
  }

  public void testUnbalanced5() throws Exception {
    parseAndCompareBalanced("<html><body><br />content<br></html>",
        "<HTML><body><br />content<br></body></HTML>");
  }

  private void parseAndCompareBalanced(String content, String expected) throws Exception {
    NekoSimplifiedHtmlParser builder = new NekoSimplifiedHtmlParser(
        new ParseModule.DOMImplementationProvider().get());
    Document document = builder.parseDom(content);
    StringWriter sw = new StringWriter();
    OutputFormat outputFormat = new OutputFormat();
    outputFormat.setPreserveSpace(true);
    outputFormat.setOmitDocumentType(true);
    HTMLSerializer serializer = new HTMLSerializer(sw, outputFormat) {
      protected void characters(String s) throws IOException {
        this.content();
        this._printer.printText(s);
      }
    };
    serializer.serialize(document);

    assertEquals(sw.toString().toLowerCase(), expected.toLowerCase());
  }
}
