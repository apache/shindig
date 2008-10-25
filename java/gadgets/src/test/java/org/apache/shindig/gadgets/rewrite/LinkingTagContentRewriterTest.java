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

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.shindig.gadgets.parse.GadgetHtmlParser;
import org.apache.shindig.gadgets.parse.ParseModule;
import org.w3c.dom.Document;

import java.net.URI;

public class LinkingTagContentRewriterTest extends FeatureBasedRewriterTestBase {
  private LinkingTagContentRewriter rewriter;
  private GadgetHtmlParser htmlParser;
  
  private static final String LINK_PREFIX = "px-";

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    Injector injector = Guice.createInjector(new ParseModule());
    htmlParser = injector.getInstance(GadgetHtmlParser.class);
    LinkRewriter pfxLinkRewriter = new LinkRewriter() {
      public String rewrite(String uri, URI context) {
        // Just prefixes with LINK_PREFIX
        return LINK_PREFIX + uri;
      }
    };
    rewriter = new LinkingTagContentRewriter(pfxLinkRewriter, null);
  }
  
  public void testLinkingTagStandardRewrite() throws Exception {
    String s = "<img src=\"http://a.b.com/img.gif\"></img>"
        + "<IMG src=\"http://a.b.com/img2.gif\"/>"
        + "<eMbeD src=\"http://a.b.com/some.mov\"/>"
        + "<link href=\"http://a.b.com/link.html\"></link>";
    String expected = "<IMG src=\"" + LINK_PREFIX + "http://a.b.com/img.gif\">"
        + "<IMG src=\"" + LINK_PREFIX + "http://a.b.com/img2.gif\">"
        + "<EMBED src=\"" + LINK_PREFIX + "http://a.b.com/some.mov\"></EMBED>"
        + "<LINK href=\"" + LINK_PREFIX + "http://a.b.com/link.html\">";
    Document document = htmlParser.parseDom(s);
    String rewritten = rewriteHelper(rewriter, s, document);
    assertEquals(rewritten, expected);
  }
  
  public void testLinkingTagIgnoredWithNoRewriter() throws Exception {
    String s = "<img src=\"http://a.b.com/img.gif\"></img>";
    Document document = htmlParser.parseDom(s);
    String rewritten = rewriteHelper(new LinkingTagContentRewriter(null, null), s, document);
    assertEquals(s, rewritten);
  }
  
  public void testLinkingTagIgnoredWithBadParse() throws Exception {
    String s = "<img src=\"http://a.b.com/img.gif></img>";
    String rewritten = rewriteHelper(rewriter, s, null);
    assertEquals(s, rewritten);  // null = couldn't parse
  }
}
