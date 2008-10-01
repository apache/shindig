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

import com.google.common.collect.Sets;

/**
 * Test of proxying rewriter
 */
public class ProxyingLinkRewriterTest extends BaseRewriterTestCase {

  private String rewrite(String uri) {
    return defaultRewriter.rewrite(uri, SPEC_URL.toJavaUri());
  }

  public void testAbsoluteRewrite() {
    String val = "http://a.b.com";
    assertEquals("http://www.test.com/proxy?url=http%3A%2F%2Fa.b.com&gadget=http%3A%2F%2Fexample.org%2Fg.xml&fp=-840722081",
        rewrite(val));
  }

  public void testHostRelativeRewrite() {
    String val = "/somepath/test.gif";
    assertEquals("http://www.test.com/proxy?url=http%3A%2F%2Fexample.org%2Fsomepath%2Ftest.gif&gadget=http%3A%2F%2Fexample.org%2Fg.xml&fp=-840722081",
        rewrite(val));
  }

  public void testPathRelativeRewrite() {
    String val = "test.gif";
    assertEquals("http://www.test.com/proxy?url=http%3A%2F%2Fexample.org%2Ftest.gif&gadget=http%3A%2F%2Fexample.org%2Fg.xml&fp=-840722081",
        rewrite(val));
  }

  public void testLeadingAndTrailingSpace() {
    String val = " test.gif ";
    assertEquals("http://www.test.com/proxy?url=http%3A%2F%2Fexample.org%2Ftest.gif&gadget=http%3A%2F%2Fexample.org%2Fg.xml&fp=-840722081",
        rewrite(val));
  }

  public void testWithRefresh() throws Exception {
    ContentRewriterFeature contentRewriterFeature = new ContentRewriterFeature(
        getSpecWithoutRewrite(), ".*", "", "86400",
        Sets.newHashSet("embed", "img", "script", "link"));
    ProxyingLinkRewriter rewriter = new ProxyingLinkRewriter(
      SPEC_URL.toJavaUri(),
      contentRewriterFeature,
      "http://www.test.com/proxy?url=");
    String val = " test.gif ";
    assertEquals("http://www.test.com/proxy?url=http%3A%2F%2Fexample.org%2Ftest.gif&gadget=http%3A%2F%2Fexample.org%2Fg.xml&fp=-840722081&refresh=86400",
        rewriter.rewrite(val, SPEC_URL.toJavaUri()));
  }

  public void testInvalidCharRewrite() {
    String val = "/images/opensocial/movie_trivia/76/${quiz.picture_url}";
    assertEquals(val,
        rewrite(val));
  }

   public void testEmpty() {
    String val = " ";
    assertEquals("", rewrite(val));
  }
}
