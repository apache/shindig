/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.gadgets.rewrite;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.google.common.collect.ImmutableSet;

/**
 * Test of proxying rewriter
 */
public class ProxyingLinkRewriterTest extends BaseRewriterTestCase {

  private String rewrite(String uri) {
    return defaultLinkRewriter.rewrite(uri, SPEC_URL);
  }

  @Test
  public void testAbsoluteRewrite() {
    String val = "http://a.b.com";
    assertEquals("http://www.test.com/dir/proxy?url=http%3A%2F%2Fa.b.com&gadget=http%3A%2F%2Fwww.example.org%2Fdir%2Fg.xml&fp=-182800334&refresh=86400",
        rewrite(val));
  }

  @Test
  public void testAbsoluteRewriteNoCache() {
    String val = "http://a.b.com";
    assertEquals("http://www.test.com/dir/proxy?url=http%3A%2F%2Fa.b.com&gadget=http%3A%2F%2Fwww.example.org%2Fdir%2Fg.xml&fp=-182800334&nocache=1&refresh=86400",
        defaultLinkRewriterNoCache.rewrite(val, SPEC_URL));
  }

  @Test
  public void testAbsoluteRewriteNoCacheAndDebug() {
    String val = "http://a.b.com";
    assertEquals("http://www.test.com/dir/proxy?url=http%3A%2F%2Fa.b.com&gadget=http%3A%2F%2Fwww.example.org%2Fdir%2Fg.xml&fp=-182800334&debug=1&nocache=1&refresh=86400",
        defaultLinkRewriterNoCacheAndDebug.rewrite(val, SPEC_URL));
  }

  @Test
  public void testHostRelativeRewrite() {
    String val = "/somepath/test.gif";
    assertEquals("http://www.test.com/dir/proxy?url=http%3A%2F%2Fwww.example.org%2Fsomepath%2Ftest.gif&gadget=http%3A%2F%2Fwww.example.org%2Fdir%2Fg.xml&fp=-182800334&refresh=86400",
        rewrite(val));
  }

  @Test
  public void testPathRelativeRewrite() {
    String val = "test.gif";
    assertEquals("http://www.test.com/dir/proxy?url=http%3A%2F%2Fwww.example.org%2Fdir%2Ftest.gif&gadget=http%3A%2F%2Fwww.example.org%2Fdir%2Fg.xml&fp=-182800334&refresh=86400",
        rewrite(val));
  }

  @Test
  public void testLeadingAndTrailingSpace() {
    String val = " test.gif ";
    assertEquals("http://www.test.com/dir/proxy?url=http%3A%2F%2Fwww.example.org%2Fdir%2Ftest.gif&gadget=http%3A%2F%2Fwww.example.org%2Fdir%2Fg.xml&fp=-182800334&refresh=86400",
        rewrite(val));
  }

  @Test
  public void testWithRefresh() throws Exception {
    ContentRewriterFeature contentRewriterFeature = new ContentRewriterFeature(
        createSpecWithoutRewrite(), ".*", "", "3600",
        ImmutableSet.of("embed", "img", "script", "link", "style"), false);
    ProxyingLinkRewriter rewriter = new DefaultProxyingLinkRewriterFactory(
        defaultContainerRewriterUris).create(SPEC_URL, contentRewriterFeature,
        "default", false, false);
    String val = " test.gif ";
    assertEquals("http://www.test.com/dir/proxy?url=http%3A%2F%2Fwww.example.org%2Fdir%2Ftest.gif&gadget=http%3A%2F%2Fwww.example.org%2Fdir%2Fg.xml&fp=-182800334&refresh=3600",
        rewriter.rewrite(val, SPEC_URL));
  }

  @Test
  public void testWithBadRefresh() throws Exception {
    ContentRewriterFeature contentRewriterFeature = new ContentRewriterFeature(
        createSpecWithoutRewrite(), ".*", "", "HTTP",
        ImmutableSet.of("embed", "img", "script", "link", "style"), false);
    ProxyingLinkRewriter rewriter = new DefaultProxyingLinkRewriterFactory(
        defaultContainerRewriterUris).create(SPEC_URL, contentRewriterFeature,
        "default", false, false);
    String val = " test.gif ";
    assertEquals("http://www.test.com/dir/proxy?url=http%3A%2F%2Fwww.example.org%2Fdir%2Ftest.gif&gadget=http%3A%2F%2Fwww.example.org%2Fdir%2Fg.xml&fp=-182800334",
        rewriter.rewrite(val, SPEC_URL));
  }

  @Test
  public void testInvalidCharRewrite() {
    String val = "/images/opensocial/movie_trivia/76/${quiz.picture_url}";
    assertEquals(val,
        rewrite(val));
  }

  @Test
  public void testEmpty() {
    String val = " ";
    assertEquals("", rewrite(val));
  }
}
