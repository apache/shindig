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

public class StyleLinksContentRewriterTest extends FeatureBasedRewriterTestBase {
  private LinkRewriter pfxLinkRewriter;
  private ContentRewriterFeature styleFeature;
  private StyleLinksContentRewriter rewriter;
  
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
    styleFeature = makeFeature("style");
    ContentRewriterFeature.Factory factory = mockContentRewriterFeatureFactory(styleFeature);
    rewriter = new StyleLinksContentRewriter(factory, pfxLinkRewriter);
  }
  
  public void testStyleTagRewrites() throws Exception {
    String css =
      "div {list-style-image:url('http://a.b.com/bullet.gif');list-style-position:outside;margin:5px;padding:0}\n" +
      ".someid {background-image:url(http://a.b.com/bigimg.png);float:right;width:165px;height:23px;margin-top:4px;margin-left:5px}";
    String s = "<style>" + css + "</style>";
    ParsedHtmlNode[] styleKids = {
      GadgetHtmlNodeTest.makeParsedTextNode(css)
    };
    ParsedHtmlNode[] p = {
      GadgetHtmlNodeTest.makeParsedTagNode("style", null, styleKids)
    };
    String rewritten =
      "<style>div {list-style-image:url(\"" + LINK_PREFIX + "http://a.b.com/bullet.gif\");list-style-position:outside;margin:5px;padding:0}\n" +
      ".someid {background-image:url(\"" + LINK_PREFIX + "http://a.b.com/bigimg.png\");float:right;width:165px;height:23px;margin-top:4px;margin-left:5px}</style>";
    assertEquals(rewritten, rewriteHelper(rewriter, s, p));
  }
  
  public void testStyleTagRewritesIgnoredOnBadParse() throws Exception {
    String css = 
      "div {list-style-image:url('http://a.b.com/bullet.gif');list-style-position:outside;margin:5px;padding:0}\n" +
      ".someid {background-image:url(http://a.b.com/bigimg.png);float:right;width:165px;height:23px;margin-top:4px;margin-left:5px}";
    String s = "<style>" + css + "</style";
    assertEquals(s, rewriteHelper(rewriter, s, null));
  }
  
  public void testStyleTagRewritesIgnoredOnNoFeatureKey() throws Exception {
    ContentRewriterFeature overrideFeature = makeFeature("foo");  // doesn't include "style"
    ContentRewriterFeature.Factory factory = mockContentRewriterFeatureFactory(overrideFeature);
    StyleLinksContentRewriter overrideRewriter = new StyleLinksContentRewriter(factory, pfxLinkRewriter);
    String css =
      "div {list-style-image:url('http://a.b.com/bullet.gif');list-style-position:outside;margin:5px;padding:0}\n" +
      ".someid {background-image:url(http://a.b.com/bigimg.png);float:right;width:165px;height:23px;margin-top:4px;margin-left:5px}";
    String s = "<style>" + css + "</style>";
    ParsedHtmlNode[] styleKids = {
      GadgetHtmlNodeTest.makeParsedTextNode(css)
    };
    ParsedHtmlNode[] p = {
      GadgetHtmlNodeTest.makeParsedTagNode("style", null, styleKids)
    };
    assertEquals(s, rewriteHelper(overrideRewriter, s, p));
  }
  
  public void testStyleTagRewritesIgnoredOnNullLinkRewriter() throws Exception {
    ContentRewriterFeature.Factory factory = mockContentRewriterFeatureFactory(styleFeature);
    StyleLinksContentRewriter overrideRewriter = new StyleLinksContentRewriter(factory, null);
    String css =
      "div {list-style-image:url('http://a.b.com/bullet.gif');list-style-position:outside;margin:5px;padding:0}\n" +
      ".someid {background-image:url(http://a.b.com/bigimg.png);float:right;width:165px;height:23px;margin-top:4px;margin-left:5px}";
    String s = "<style>" + css + "</style>";
    ParsedHtmlNode[] styleKids = {
      GadgetHtmlNodeTest.makeParsedTextNode(css)
    };
    ParsedHtmlNode[] p = {
      GadgetHtmlNodeTest.makeParsedTagNode("style", null, styleKids)
    };
    assertEquals(s, rewriteHelper(overrideRewriter, s, p));
  }
}
