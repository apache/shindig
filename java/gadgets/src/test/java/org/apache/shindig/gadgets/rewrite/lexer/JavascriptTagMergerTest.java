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

import org.apache.shindig.gadgets.rewrite.BaseRewriterTestCase;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Test for Javascript tag merge functionality
 */
public class JavascriptTagMergerTest extends BaseRewriterTestCase {

  private URI dummyUri;

  private Map<String, HtmlTagTransformer> defaultTransformerMap;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    dummyUri = new URI("http://www.w3c.org");
    URI relativeBase = new URI("http://a.b.com/");
    defaultTransformerMap = new HashMap<String, HtmlTagTransformer>();
    defaultTransformerMap
        .put("script", new JavascriptTagMerger(getSpecWithoutRewrite(), contentRewriterFeature,
            "http://www.test.com/concat?", relativeBase));
  }

  private void validateRewritten(String content, URI base,
      Map<String, HtmlTagTransformer> transformerMap,
      String expected) {
    assertEquals(expected, HtmlRewriter.rewrite(content, base, transformerMap));
  }

  private void validateRewritten(String content, String expected) {
    validateRewritten(content, dummyUri, defaultTransformerMap, expected);
  }

  public void testPreserveNoExternal() {
    String original = "<script type=\"text/javascript\">\n"
        + "doSomething\n"
        + "</script>";
    validateRewritten(original, original);
  }

  public void testPreserveNoScript() {
    String original
        = "<html><div id=\"test\">ceci ne pas une script</div></html>";
    validateRewritten(original, original);
  }

  public void testPreserveWithComment() {
    String original = "<script type=\"text/javascript\"><!--\n"
        + "doSomething\n"
        + "--></script>";
    validateRewritten(original, original);
  }

  public void testSingleScriptReWrite() {
    String original = "<script src=\"http://a.b.com/1.js\"></script>";
    String rewritten
        = "<script src=\"http://www.test.com/concat?rewriteMime=text/javascript&gadget=http%3A%2F%2Fexample.org%2Fg.xml&fp=-840722081&1=http%3A%2F%2Fa.b.com%2F1.js\" type=\"text/javascript\"></script>";
    validateRewritten(original, rewritten);
  }

  public void testTwoScriptReWrite() {
    String original = "<script src=\"http://a.b.com/1.js\"></script>\n"
        + "<script src=\"http://a.b.com/2.js\"></script>";
    String rewritten
        = "<script src=\"http://www.test.com/concat?rewriteMime=text/javascript&gadget=http%3A%2F%2Fexample.org%2Fg.xml&fp=-840722081&1=http%3A%2F%2Fa.b.com%2F1.js&2=http%3A%2F%2Fa.b.com%2F2.js\" type=\"text/javascript\"></script>";
    validateRewritten(original, rewritten);
  }

  public void testLeadAndTrailingScriptReWrite() {
    String original = "<script>\n"
        + "doSomething\n"
        + "</script>\n"
        + "<script src=\"http://a.b.com/1.js\"></script>\n"
        + "<script src=\"http://a.b.com/2.js\"></script>\n"
        + "<script>\n"
        + "doSomething\n"
        + "</script>";
    String rewritten = "<script type=\"text/javascript\">\n"
        + "doSomething\n"
        + "</script>"
        + "<script src=\"http://www.test.com/concat?rewriteMime=text/javascript&gadget=http%3A%2F%2Fexample.org%2Fg.xml&fp=-840722081&1=http%3A%2F%2Fa.b.com%2F1.js&2=http%3A%2F%2Fa.b.com%2F2.js\" type=\"text/javascript\"></script>"
        + "<script type=\"text/javascript\">\n"
        + "doSomething\n"
        + "</script>";
    validateRewritten(original, rewritten);
  }

  public void testInterspersed() {
    String original = "<script src=\"http://a.b.com/1.js\"></script>\n"
        + "<script src=\"http://a.b.com/2.js\"></script>\n"
        + "<script type=\"text/javascript\"><!-- doSomething --></script>\n"
        + "<script src=\"http://a.b.com/3.js\"></script>\n"
        + "<script src=\"http://a.b.com/4.js\"></script>";
    String rewritten =
        "<script src=\"http://www.test.com/concat?rewriteMime=text/javascript&gadget=http%3A%2F%2Fexample.org%2Fg.xml&fp=-840722081&1=http%3A%2F%2Fa.b.com%2F1.js&2=http%3A%2F%2Fa.b.com%2F2.js\" type=\"text/javascript\"></script>"
            + "<script type=\"text/javascript\"><!-- doSomething --></script>"
            + "<script src=\"http://www.test.com/concat?rewriteMime=text/javascript&gadget=http%3A%2F%2Fexample.org%2Fg.xml&fp=-840722081&1=http%3A%2F%2Fa.b.com%2F3.js&2=http%3A%2F%2Fa.b.com%2F4.js\" type=\"text/javascript\"></script>";
    validateRewritten(original, rewritten);
  }

  public void testDerelativizeHostRelative() {
    String original = "<script src=\"/1.js\"></script>";
    String rewritten
        = "<script src=\"http://www.test.com/concat?rewriteMime=text/javascript&gadget=http%3A%2F%2Fexample.org%2Fg.xml&fp=-840722081&1=http%3A%2F%2Fa.b.com%2F1.js\" type=\"text/javascript\"></script>";
    validateRewritten(original, rewritten);
  }

  public void testDerelativizePathRelative() {
    String original = "<script src=\"1.js\"></script>";
    String rewritten
        = "<script src=\"http://www.test.com/concat?rewriteMime=text/javascript&gadget=http%3A%2F%2Fexample.org%2Fg.xml&fp=-840722081&1=http%3A%2F%2Fa.b.com%2F1.js\" type=\"text/javascript\"></script>";
    validateRewritten(original, rewritten);
  }

}
