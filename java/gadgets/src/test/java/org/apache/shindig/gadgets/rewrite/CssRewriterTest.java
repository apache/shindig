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

import java.net.URI;

/**
 * test CSS link rewriting
 */
public class CssRewriterTest extends BaseRewriterTestCase {

  private URI dummyUri;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    dummyUri = new URI("http://www.w3c.org");
  }

  private void validateRewritten(String content, URI base,
      LinkRewriter rewriter, String expected) {
    assertEquals(expected, CssRewriter.rewrite(content, base, rewriter));
  }

  private void validateRewritten(String content, String expected) {
    validateRewritten(content, dummyUri, defaultRewriter, expected);
  }

  public void testUrlDeclarationRewrite() {
    String original =
        "div {list-style-image:url('http://a.b.com/bullet.gif');list-style-position:outside;margin:5px;padding:0}\n" +
         ".someid {background-image:url(http://a.b.com/bigimg.png);float:right;width:165px;height:23px;margin-top:4px;margin-left:5px}";
    String rewritten =
        "div {list-style-image:url(\"http://www.test.com/proxy?url=http%3A%2F%2Fa.b.com%2Fbullet.gif&gadget=http%3A%2F%2Fexample.org%2Fg.xml&fp=-840722081\");list-style-position:outside;margin:5px;padding:0}\n" +
         ".someid {background-image:url(\"http://www.test.com/proxy?url=http%3A%2F%2Fa.b.com%2Fbigimg.png&gadget=http%3A%2F%2Fexample.org%2Fg.xml&fp=-840722081\");float:right;width:165px;height:23px;margin-top:4px;margin-left:5px}";
    validateRewritten(original, rewritten);
  }

}
