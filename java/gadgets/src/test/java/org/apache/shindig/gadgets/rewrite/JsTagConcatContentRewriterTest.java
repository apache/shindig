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

import static org.easymock.EasyMock.expect;
import static org.easymock.classextension.EasyMock.replay;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.parse.GadgetHtmlNodeTest;
import org.apache.shindig.gadgets.parse.ParsedHtmlNode;
import org.apache.shindig.gadgets.spec.GadgetSpec;

import org.easymock.classextension.EasyMock;

public class JsTagConcatContentRewriterTest extends FeatureBasedRewriterTestBase {
  private ContentRewriterFeature jsFeature;
  private JsTagConcatContentRewriter rewriter;
  private String concatBase;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    jsFeature = makeFeature("script");
    ContentRewriterFeature.Factory factory = mockContentRewriterFeatureFactory(jsFeature);
    rewriter = new JsTagConcatContentRewriter(factory, null);
    GadgetSpec spec = EasyMock.createNiceMock(GadgetSpec.class);
    expect(spec.getUrl()).andReturn(Uri.fromJavaUri(baseUri)).anyTimes();
    replay(spec);
    concatBase = rewriter.getJsConcatBase(spec, jsFeature);
  }

  public void testJSMergePreserveNoExternal() throws Exception {
    String s = "<script>\n"
        + "doSomething\n"
        + "</script>";
    ParsedHtmlNode[] scriptKids = {
      GadgetHtmlNodeTest.makeParsedTextNode("\ndoSomething\n")
    };
    ParsedHtmlNode[] p = {
      GadgetHtmlNodeTest.makeParsedTagNode("script", null, scriptKids)
    };
    assertEquals(s, rewriteHelper(rewriter, s, p));
  }

  public void testJSMergePreserveNoScript() throws Exception {
    String s
        = "<html><div id=\"test\">ceci ne pas une script</div></html>";
    String[][] attribs = { { "id", "test" } };
    ParsedHtmlNode[] divKids = {
      GadgetHtmlNodeTest.makeParsedTextNode("ceci ne pas une script")
    };
    ParsedHtmlNode[] htmlKids = {
      GadgetHtmlNodeTest.makeParsedTagNode("div", attribs, divKids)
    };
    ParsedHtmlNode[] p = {
      GadgetHtmlNodeTest.makeParsedTagNode("html", null, htmlKids)
    };
    assertEquals(s, rewriteHelper(rewriter, s, p));
  }

  public void testJSMergePreserveWithComment() throws Exception {
    String s = "<script>" +
        "<!--\ndoSomething\n-->" +
        "</script>";
    ParsedHtmlNode[] scriptKids = {
      GadgetHtmlNodeTest.makeParsedTextNode("<!--\ndoSomething\n-->")
    };
    ParsedHtmlNode[] p = {
      GadgetHtmlNodeTest.makeParsedTagNode("script", null, scriptKids)
    };
    assertEquals(s, rewriteHelper(rewriter, s, p));
  }

  public void testJSMergeSingleScriptReWrite() throws Exception {
    String s = "<script src=\"http://a.b.com/1.js\"></script>";
    String[][] attribs = { { "src", "http://a.b.com/1.js" } };
    ParsedHtmlNode[] p = {
      GadgetHtmlNodeTest.makeParsedTagNode("script", attribs, null)
    };
    String rewritten
        = "<script src=\"" + concatBase + "1=http%3A%2F%2Fa.b.com%2F1.js\"></script>";
    assertEquals(rewritten, rewriteHelper(rewriter, s, p));
  }

  public void testJSMergeTwoScriptReWriteWithWhitespace() throws Exception {
    String s = "<script src=\"http://a.b.com/1.js\"></script>\n"
        + "<script src=\"http://a.b.com/2.js\"></script>";
    String[][] attr1 = { { "src", "http://a.b.com/1.js" } };
    String[][] attr2 = { { "src", "http://a.b.com/2.js" } };
    ParsedHtmlNode[] p = {
      GadgetHtmlNodeTest.makeParsedTagNode("script", attr1, null),
      GadgetHtmlNodeTest.makeParsedTextNode("\n"),
      GadgetHtmlNodeTest.makeParsedTagNode("script", attr2, null)
    };
    String rewritten
        = "<script src=\"" + concatBase + "1=http%3A%2F%2Fa.b.com%2F1.js&2=http%3A%2F%2Fa.b.com%2F2.js\"></script>";
    assertEquals(rewritten, rewriteHelper(rewriter, s, p));
  }

  public void testJSMergeLeadAndTrailingScriptReWrite() throws Exception {
    String s = "<script>\n"
        + "doSomething\n"
        + "</script>"
        + "<script src=\"http://a.b.com/1.js\"></script>"
        + "<script src=\"http://a.b.com/2.js\"></script>"
        + "<script>"
        + "doSomething\n"
        + "</script>";
    String[][] attr1 = { { "src", "http://a.b.com/1.js" } };
    String[][] attr2 = { { "src", "http://a.b.com/2.js" } };
    ParsedHtmlNode[] scriptKids = {
      GadgetHtmlNodeTest.makeParsedTextNode("\ndoSomething\n")
    };
    ParsedHtmlNode[] p = {
      GadgetHtmlNodeTest.makeParsedTagNode("script", null, scriptKids),
      GadgetHtmlNodeTest.makeParsedTagNode("script", attr1, null),
      GadgetHtmlNodeTest.makeParsedTagNode("script", attr2, null),
      GadgetHtmlNodeTest.makeParsedTagNode("script", null, scriptKids)
    };
    String rewritten = "<script>\n"
        + "doSomething\n"
        + "</script>"
        + "<script src=\"" + concatBase + "1=http%3A%2F%2Fa.b.com%2F1.js&2=http%3A%2F%2Fa.b.com%2F2.js\"></script>"
        + "<script>\n"
        + "doSomething\n"
        + "</script>";
    assertEquals(rewritten, rewriteHelper(rewriter, s, p));
  }

  public void testJSMergeInterspersed() throws Exception {
    String s = "<script src=\"http://a.b.com/1.js\"></script>"
        + "<script src=\"http://a.b.com/2.js\"></script>"
        + "<script><!-- doSomething --></script>"
        + "<script src=\"http://a.b.com/3.js\"></script>"
        + "<script src=\"http://a.b.com/4.js\"></script>";
    String[][] attr1 = { { "src", "http://a.b.com/1.js" } };
    String[][] attr2 = { { "src", "http://a.b.com/2.js" } };
    String[][] attr3 = { { "src", "http://a.b.com/3.js" } };
    String[][] attr4 = { { "src", "http://a.b.com/4.js" } };
    ParsedHtmlNode[] scriptKids = {
      GadgetHtmlNodeTest.makeParsedTextNode("<!-- doSomething -->")
    };
    ParsedHtmlNode[] p = {
      GadgetHtmlNodeTest.makeParsedTagNode("script", attr1, null),
      GadgetHtmlNodeTest.makeParsedTagNode("script", attr2, null),
      GadgetHtmlNodeTest.makeParsedTagNode("script", null, scriptKids),
      GadgetHtmlNodeTest.makeParsedTagNode("script", attr3, null),
      GadgetHtmlNodeTest.makeParsedTagNode("script", attr4, null)
    };
    String rewritten =
        "<script src=\"" + concatBase + "1=http%3A%2F%2Fa.b.com%2F1.js&2=http%3A%2F%2Fa.b.com%2F2.js\"></script>" +
        "<script><!-- doSomething --></script>" +
        "<script src=\"" + concatBase + "1=http%3A%2F%2Fa.b.com%2F3.js&2=http%3A%2F%2Fa.b.com%2F4.js\"></script>";
    assertEquals(rewritten, rewriteHelper(rewriter, s, p));
  }

  public void testJSMergeDerelativizeHostRelative() throws Exception {
    String s = "<script src=\"/1.js\"></script>";
    String[][] attr1 = { { "src", "/1.js" } };
    ParsedHtmlNode[] p = {
      GadgetHtmlNodeTest.makeParsedTagNode("script", attr1, null)
    };
    String rewritten
        = "<script src=\"" + concatBase + "1=http%3A%2F%2Fgadget.org%2F1.js\"></script>";
    assertEquals(rewritten, rewriteHelper(rewriter, s, p));
  }

  public void testJSMergeDerelativizePathRelative() throws Exception {
    String s = "<script src=\"1.js\"></script>";
    String[][] attr1 = { { "src", "1.js" } };
    ParsedHtmlNode[] p = {
      GadgetHtmlNodeTest.makeParsedTagNode("script", attr1, null)
    };
    String rewritten
        = "<script src=\"" + concatBase + "1=http%3A%2F%2Fgadget.org%2Fdir%2F1.js\"></script>";
    assertEquals(rewritten, rewriteHelper(rewriter, s, p));
  }
}
