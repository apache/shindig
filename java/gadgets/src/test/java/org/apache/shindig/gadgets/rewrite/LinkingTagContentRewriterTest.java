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

public class LinkingTagContentRewriterTest extends BaseRewriterTestCase {
  private LinkingTagContentRewriter rewriter;
  
  private static final String LINK_PREFIX = "px-";

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    rewriter = new LinkingTagContentRewriter(rewriterFeatureFactory, DEFAULT_PROXY_BASE) {
      @Override
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
  
  public void testLinkingTagStandardRewrite() throws Exception {
    String s = "<img src=\"http://a.b.com/img.gif\"></img>"
        + "<img src=\"http://a.b.com/img2.gif\"/>"
        + "<embed src=\"http://a.b.com/some.mov\"></embed>"
        + "<link href=\"http://a.b.com/link.html\"></link>";
    String expected = "<head></head><BODY><img src=\"" + LINK_PREFIX + "http://a.b.com/img.gif\">"
        + "<img src=\"" + LINK_PREFIX + "http://a.b.com/img2.gif\">"
        + "<embed src=\"" + LINK_PREFIX + "http://a.b.com/some.mov\"></embed>"
        + "<link href=\"" + LINK_PREFIX + "http://a.b.com/link.html\"></BODY>";
    String rewritten = rewriteHelper(rewriter, s);
    assertEquals(rewritten, expected);
  }

  
  public void testLinkingTagWithBadParse() throws Exception {
    String s = "<img src=\"http://a.b.com/img.gif></img>";
    String rewritten = rewriteHelper(rewriter, s);
    assertEquals(s, rewritten);  // null = couldn't parse
  }
}
