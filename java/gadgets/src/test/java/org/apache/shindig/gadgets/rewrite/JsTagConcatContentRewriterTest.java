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
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.parse.GadgetHtmlParser;
import org.apache.shindig.gadgets.parse.ParseModule;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import static org.easymock.EasyMock.expect;
import org.easymock.classextension.EasyMock;
import static org.easymock.classextension.EasyMock.replay;
import org.w3c.dom.Document;

public class JsTagConcatContentRewriterTest extends FeatureBasedRewriterTestBase {
  private JsTagConcatContentRewriter rewriter;
  private String concatBase;
  private GadgetHtmlParser htmlParser;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    ContentRewriterFeature jsFeature = makeFeature("script");
    Injector injector = Guice.createInjector(new ParseModule());
    htmlParser = injector.getInstance(GadgetHtmlParser.class);
    ContentRewriterFeature.Factory factory = mockContentRewriterFeatureFactory(jsFeature);
    rewriter = new JsTagConcatContentRewriter(factory, null);
    GadgetSpec spec = EasyMock.createNiceMock(GadgetSpec.class);
    expect(spec.getUrl()).andReturn(Uri.fromJavaUri(baseUri)).anyTimes();
    replay(spec);
    concatBase = rewriter.getJsConcatBase(spec, jsFeature);
  }

  public void testJSMergePreserveNoExternal() throws Exception {
    String s = "<SCRIPT>\n"
        + "doSomething\n"
        + "</SCRIPT>";

    Document document = htmlParser.parseDom(s);
    String rewritten = rewriteHelper(rewriter, s, document);
    assertEquals(rewritten, s);
  }

  public void testJSMergePreserveNoScript() throws Exception {
    String s
        = "<DIV id=\"test\">ceci ne pas une script</DIV>";
    Document document = htmlParser.parseDom(s);
    String rewritten = rewriteHelper(rewriter, s, document);
    assertEquals(rewritten, s);
  }

  public void testJSMergePreserveWithComment() throws Exception {
    String s = "<SCRIPT>" +
        "<!--\ndoSomething\n-->" +
        "</SCRIPT>";
    Document document = htmlParser.parseDom(s);
    String rewritten = rewriteHelper(rewriter, s, document);
    assertEquals(rewritten, s);
  }

  public void testJSMergeSingleScriptReWrite() throws Exception {
    String s = "<SCRIPT src=\"http://a.b.com/1.js\"></SCRIPT>";
    String expected = "<SCRIPT src=\"" + concatBase + "1=http%3A%2F%2Fa.b.com%2F1.js\"></SCRIPT>";
    Document document = htmlParser.parseDom(s);
    String rewritten = rewriteHelper(rewriter, s, document);
    assertEquals(rewritten, expected);
  }

  public void testJSMergeTwoScriptReWriteWithWhitespace() throws Exception {
    String s = "<SCRIPT src=\"http://a.b.com/1.js\"></SCRIPT>"
        + "<SCRIPT src=\"http://a.b.com/2.js\"></SCRIPT>";
    String expected
        = "<SCRIPT src=\"" + concatBase + "1=http%3A%2F%2Fa.b.com%2F1.js&2=http%3A%2F%2Fa.b.com%2F2.js\"></SCRIPT>";
    Document document = htmlParser.parseDom(s);
    String rewritten = rewriteHelper(rewriter, s, document);
    assertEquals(rewritten, expected);
  }

  public void testJSMergeLeadAndTrailingScriptReWrite() throws Exception {
    String s = "<SCRIPT>\n"
        + "doSomething\n"
        + "</SCRIPT>"
        + "<SCRIPT src=\"http://a.b.com/1.js\"></SCRIPT>"
        + "<SCRIPT src=\"http://a.b.com/2.js\"></SCRIPT>"
        + "<SCRIPT>\n"
        + "doSomething\n"
        + "</SCRIPT>";
    String expected = "<SCRIPT>\n"
        + "doSomething\n"
        + "</SCRIPT>"
        + "<SCRIPT src=\"" + concatBase + "1=http%3A%2F%2Fa.b.com%2F1.js&2=http%3A%2F%2Fa.b.com%2F2.js\"></SCRIPT>"
        + "<SCRIPT>\n"
        + "doSomething\n"
        + "</SCRIPT>";
    Document document = htmlParser.parseDom(s);
    String rewritten = rewriteHelper(rewriter, s, document);
    assertEquals(rewritten, expected);
  }

  public void testJSMergeInterspersed() throws Exception {
    String s = "<SCRIPT src=\"http://a.b.com/1.js\"></SCRIPT>"
        + "<SCRIPT src=\"http://a.b.com/2.js\"></SCRIPT>"
        + "<SCRIPT><!-- doSomething --></SCRIPT>"
        + "<SCRIPT src=\"http://a.b.com/3.js\"></SCRIPT>"
        + "<SCRIPT src=\"http://a.b.com/4.js\"></SCRIPT>";
    String expected =
        "<SCRIPT src=\"" + concatBase + "1=http%3A%2F%2Fa.b.com%2F1.js&2=http%3A%2F%2Fa.b.com%2F2.js\"></SCRIPT>" +
        "<SCRIPT><!-- doSomething --></SCRIPT>" +
        "<SCRIPT src=\"" + concatBase + "1=http%3A%2F%2Fa.b.com%2F3.js&2=http%3A%2F%2Fa.b.com%2F4.js\"></SCRIPT>";
    Document document = htmlParser.parseDom(s);
    String rewritten = rewriteHelper(rewriter, s, document);
    assertEquals(expected, rewritten);
  }

  public void testJSMergeDerelativizeHostRelative() throws Exception {
    String s = "<SCRIPT src=\"/1.js\"></SCRIPT>";
    String expected
        = "<SCRIPT src=\"" + concatBase + "1=http%3A%2F%2Fgadget.org%2F1.js\"></SCRIPT>";
    Document document = htmlParser.parseDom(s);
    String rewritten = rewriteHelper(rewriter, s, document);
    assertEquals(rewritten, expected);
  }

  public void testJSMergeDerelativizePathRelative() throws Exception {
    String s = "<SCRIPT src=\"1.js\"></SCRIPT>";
    String expected
        = "<SCRIPT src=\"" + concatBase + "1=http%3A%2F%2Fgadget.org%2Fdir%2F1.js\"></SCRIPT>";
    Document document = htmlParser.parseDom(s);
    String rewritten = rewriteHelper(rewriter, s, document);
    assertEquals(rewritten, expected);
  }
}
