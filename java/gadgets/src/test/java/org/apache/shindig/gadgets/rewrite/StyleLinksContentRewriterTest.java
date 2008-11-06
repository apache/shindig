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

import com.google.common.collect.Sets;

import java.net.URI;

public class StyleLinksContentRewriterTest extends BaseRewriterTestCase {
  private StyleLinksContentRewriter rewriter;
  
  private static final String LINK_PREFIX = "px-";

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    rewriter = new StyleLinksContentRewriter(rewriterFeatureFactory, DEFAULT_PROXY_BASE) {
      protected LinkRewriter createLinkRewriter(URI gadgetUri, ContentRewriterFeature feature) {
        return new LinkRewriter() {
          public String rewrite(String uri, URI context) {
            // Just prefixes with LINK_PREFIX
            return LINK_PREFIX + uri;
          }
        };
      }
    };
  }
  
  public void testStyleTagRewrites() throws Exception {
    String css =
      "div {list-style-image:url('http://a.b.com/bullet.gif');list-style-position:outside;margin:5px;padding:0}\n" +
      ".someid {background-image:url(http://a.b.com/bigimg.png);float:right;width:165px;height:23px;margin-top:4px;margin-left:5px}";
    String s = "<style>" + css + "</style>";

    String rewritten =
      "div {list-style-image:url(\"" + LINK_PREFIX + "http://a.b.com/bullet.gif\");list-style-position:outside;margin:5px;padding:0}\n" +
      ".someid {background-image:url(\"" + LINK_PREFIX + "http://a.b.com/bigimg.png\");float:right;width:165px;height:23px;margin-top:4px;margin-left:5px}";
    // Rewrite, document is mutated in-place
    MutableContent content = rewriteContent(rewriter, s);
    assertEquals(rewritten,
        RewriterUtils.getElementsByTagNameCaseInsensitive(content.getDocument(),
            Sets.newHashSet("style")).get(0).getTextContent());
  }
  
  public void testStyleTagRewritesIgnoredOnNoFeatureKey() throws Exception {
    ContentRewriterFeature overrideFeature =
        rewriterFeatureFactory.get(createSpecWithRewrite(".*", "", "HTTP",
            Sets.newConcurrentHashSet("foo")));  // doesn't include "style"
    ContentRewriterFeatureFactory factory = mockContentRewriterFeatureFactory(overrideFeature);
    StyleLinksContentRewriter overrideRewriter =
        new StyleLinksContentRewriter(factory, LINK_PREFIX);
    String css =
      "div {list-style-image:url('http://a.b.com/bullet.gif');list-style-position:outside;margin:5px;padding:0}\n" +
      ".someid {background-image:url(http://a.b.com/bigimg.png);float:right;width:165px;height:23px;margin-top:4px;margin-left:5px}";
    String s = "<style>" + css + "</style>";
    assertEquals(s, rewriteHelper(overrideRewriter, s));
  }
}
