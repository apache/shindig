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

import org.apache.shindig.gadgets.EasyMockTestCase;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Test link rewriting 
 */
public class LinkingTagRewriterTest  extends EasyMockTestCase {

  private URI dummyUri;
  private URI relativeBase;

  private Map<String, HtmlTagTransformer> defaultTransformerMap;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    dummyUri = new URI("http://www.w3c.org");
    relativeBase = new URI("http://a.b.com/");
    LinkingTagRewriter rewriter = new LinkingTagRewriter(
        new ProxyingLinkRewriter("http://www.test.com/proxy?url="),
          relativeBase);

    defaultTransformerMap = new HashMap<String, HtmlTagTransformer>();
    for (String tag : rewriter.getSupportedTags()) {
      defaultTransformerMap
        .put(tag, rewriter);
    }
  }

  private void validateRewritten(String content, URI base,
      Map<String, HtmlTagTransformer> transformerMap,
      String expected) {
    assertEquals(expected, HtmlRewriter.rewrite(content, base, transformerMap));
  }

  private void validateRewritten(String content, String expected) {
    validateRewritten(content, dummyUri, defaultTransformerMap, expected);
  }

  public void testStandardRewrite() {
    String original = "<img src=\"http://a.b.com/img.gif\"></img>\n"
        + "<IMG src=\"http://a.b.com/img.gif\"/>\n"
        + "<eMbeD src=\"http://a.b.com/some.mov\" width=\"100\" height=\"30px\"/>";
    String expected = "<img src=\"http://www.test.com/proxy?url=http%3A%2F%2Fa.b.com%2Fimg.gif\"></img>\n"
        + "<IMG src=\"http://www.test.com/proxy?url=http%3A%2F%2Fa.b.com%2Fimg.gif\"/>\n"
        + "<eMbeD src=\"http://www.test.com/proxy?url=http%3A%2F%2Fa.b.com%2Fsome.mov\" width=\"100\" height=\"30px\"/>";
    validateRewritten(original, expected);
  }


}
