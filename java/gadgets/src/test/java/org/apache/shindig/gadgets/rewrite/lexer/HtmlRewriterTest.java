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
package org.apache.shindig.gadgets.rewrite.lexer;

import com.google.common.collect.Maps;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.rewrite.BaseRewriterTestCase;

import java.util.Map;

/**
 * Test the HTML rewriter foundation for basic operation
 */
public class HtmlRewriterTest extends BaseRewriterTestCase {

  private Uri dummyUri;

  private Map<String, HtmlTagTransformer> defaultTransformerMap;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    dummyUri = Uri.parse("http://www.w3c.org");
    defaultTransformerMap = Maps.newHashMap();
  }

  private void validateRewritten(String content, Uri base,
      Map<String, HtmlTagTransformer> transformerMap,
      String expected) {
    assertEquals(expected, HtmlRewriter.rewrite(content, base, transformerMap));
  }

  private void validateRewritten(String content, String expected) {
    validateRewritten(content, dummyUri, defaultTransformerMap, expected);
  }

  public void testPreserveJunk() {
    String s = "<div id=notvalid name='horrorShow\" />\n"
        + "</br>\n"
        + "</div>";
    validateRewritten(s, s);
  }

  public void testPreserveScript() {
    String s = "<script src=\"http://a.b.com/1.js\">\n</script>"
        + "\n<script src=\"http://a.b.com/2.js\">\n</script>";
    validateRewritten(s, s);
  }

  public void testPreserveCss() {
    String s = "<html><style>body { background-color:#7f7f7f }</style>";
    validateRewritten(s, s);
  }

  public void testBigChunk() {
    // The source of the goolge homepage at a point in time
    String s =
        "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\" \"http://www.w3.org/TR/REC-html40/loose.dtd\">\n"
            + "<html><head><title>DRAFT - HTML4 Test Suite:7_5_5-BF-01</title>\n"
            + "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\">\n"
            + "<meta http-equiv=\"Content-Style-Type\" content=\"text/css\">\n"
            + "<link rel=\"stylesheet\" type=\"text/css\" media=\"screen\" href=\"static.css\"></head>\n"
            + "<body>\n"
            + "<div class=\"navigation\">\n"
            + "<h2>DRAFT - HTML4 Test Suite: Test 7_5_5-BF-01 Headings: The H1, H2,  H3, H4, H5, H6 elements</h2>\n"
            + "<hr>[<a href=\"sec7_5_4-BF-02.html\">Previous</a>] [<a href=\"section8.html\">Next</a>] [<a href=\"section7.html\">Section</a>]"
            + " [<a href=\"index.html\">Contents</a>] [<a href=\"http://www.w3.org/TR/html401/struct/global.html#h-7.5.5\">Specification</a>]<BR>\n"
            + '\n'
            + "</div>\n"
            + "<object height=\"100%\" width=\"100%\" border=\"0\" type=\"text/html\" data=\"7_5_5-BF-01.html\">\n"
            + "<a class=\"navigation\" href=\"7_5_5-BF-01.html\" target=\"testwindow\">Test</a></object>\n"
            + "</body></html>";
    validateRewritten(s, s);
  }

  public void testPreserveCData() {
    String s = "<script><![CDATA[dontcare]]></script>";
    validateRewritten(s, s);
  }

  public void testPreserveComment() {
    String s = "<script>  <!-- something here --></script>";
    validateRewritten(s, s);
  }

  // Eventaully we want the opposite.
  public void testPreserveUselessWhitespace() {
    String s = "          <script>         \n</script>";
    validateRewritten(s, s);
  }

}
