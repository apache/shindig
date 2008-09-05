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
package org.apache.shindig.gadgets.rewrite;

import java.net.URI;

import org.apache.shindig.gadgets.parse.GadgetHtmlNodeTest;
import org.apache.shindig.gadgets.parse.ParsedHtmlNode;

public class LinkingTagContentRewriterTest extends FeatureBasedRewriterTestBase {
  private LinkRewriter pfxLinkRewriter;
  private LinkingTagContentRewriter rewriter;
  
  private static final String LINK_PREFIX = "px-";

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    pfxLinkRewriter = new LinkRewriter() {
      public String rewrite(String uri, URI context) {
        // Just prefixes with LINK_PREFIX
        return LINK_PREFIX + uri;
      }
    };
    rewriter = new LinkingTagContentRewriter(pfxLinkRewriter, null);
  }
  
  public void testLinkingTagStandardRewrite() throws Exception {
    String s = "<img src=\"http://a.b.com/img.gif\"></img>\n"
        + "<IMG src=\"http://a.b.com/img2.gif\"/>\n"
        + "<eMbeD src=\"http://a.b.com/some.mov\"/>\n"
        + "<link href=\"http://a.b.com/link.html\"></link>";
    String[][] img1attrib = { { "src", "http://a.b.com/img.gif" } };
    String[][] img2attrib = { { "src", "http://a.b.com/img2.gif" } };
    String[][] emb1attrib = { { "src", "http://a.b.com/some.mov" } };
    String[][] href1attr = { { "href", "http://a.b.com/link.html" } };
    ParsedHtmlNode[] p = {
        GadgetHtmlNodeTest.makeParsedTagNode("img", img1attrib, null),
        GadgetHtmlNodeTest.makeParsedTextNode("\n"),
        GadgetHtmlNodeTest.makeParsedTagNode("IMG", img2attrib, null),
        GadgetHtmlNodeTest.makeParsedTextNode("\n"),
        GadgetHtmlNodeTest.makeParsedTagNode("eMbeD", emb1attrib, null),
        GadgetHtmlNodeTest.makeParsedTextNode("\n"),
        GadgetHtmlNodeTest.makeParsedTagNode("link", href1attr, null)
    };
    String rewritten = "<img src=\"" + LINK_PREFIX + "http://a.b.com/img.gif\"/>\n"
        + "<IMG src=\"" + LINK_PREFIX + "http://a.b.com/img2.gif\"/>\n"
        + "<eMbeD src=\"" + LINK_PREFIX + "http://a.b.com/some.mov\"/>\n"
        + "<link href=\"" + LINK_PREFIX + "http://a.b.com/link.html\"/>";
    assertEquals(rewritten, rewriteHelper(rewriter, s, p));
  }
  
  public void testLinkingTagIgnoredWithNoRewriter() throws Exception {
    String s = "<img src=\"http://a.b.com/img.gif\"></img>";
    String[][] img1attrib = { { "src", "http://a.b.com/img.gif" } };
    ParsedHtmlNode[] p = {
        GadgetHtmlNodeTest.makeParsedTagNode("img", img1attrib, null),
    };
    assertEquals(s, rewriteHelper(new LinkingTagContentRewriter(null, null), s, p));
  }
  
  public void testLinkingTagIgnoredWithBadParse() throws Exception {
    String s = "<img src=\"http://a.b.com/img.gif></img>";
    assertEquals(s, rewriteHelper(rewriter, s, null));  // null = couldn't parse
  }
}
