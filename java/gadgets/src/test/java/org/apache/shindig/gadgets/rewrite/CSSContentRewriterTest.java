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

import org.apache.commons.io.IOUtils;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.apache.shindig.gadgets.parse.caja.CajaCssParser;

import com.google.common.collect.Lists;

import org.easymock.EasyMock;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

/**
 *
 */
public class CSSContentRewriterTest extends BaseRewriterTestCase {
  private CSSContentRewriter rewriter;
  private Uri dummyUri;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    ContentRewriterFeature overrideFeature =
        rewriterFeatureFactory.get(createSpecWithRewrite(".*", ".*exclude.*", "HTTP",
            HTMLContentRewriter.TAGS));
    ContentRewriterFeatureFactory factory = mockContentRewriterFeatureFactory(overrideFeature);
    rewriter = new CSSContentRewriter(factory, DEFAULT_PROXY_BASE, new CajaCssParser());
    dummyUri = Uri.parse("http://www.w3c.org");
  }

  public void testCssBasic() throws Exception {
    String content = IOUtils.toString(this.getClass().getClassLoader().
        getResourceAsStream("org/apache/shindig/gadgets/rewrite/rewritebasic.css"));
    String expected = IOUtils.toString(this.getClass().getClassLoader().
        getResourceAsStream("org/apache/shindig/gadgets/rewrite/rewritebasic-expected.css"));
    HttpRequest request = new HttpRequest(Uri.parse("http://www.example.org/path/rewritebasic.css"));
    request.setMethod("GET");
    request.setGadget(SPEC_URL);

    HttpResponse response = new HttpResponseBuilder().setHeader("Content-Type", "text/css")
      .setResponseString(content).create();

    MutableContent mc = new MutableContent(null, content);
    rewriter.rewrite(request, response, mc);

    assertEquals(expected, mc.getContent());
  }

    public void testNoRewriteUnknownMimeType() {
    // Strict mock as we expect no calls
    MutableContent mc = mock(MutableContent.class, true);
    HttpRequest req = mock(HttpRequest.class);
    EasyMock.expect(req.getRewriteMimeType()).andReturn("unknown");
    replay();
    assertNull(rewriter.rewrite(req, fakeResponse, mc));
    verify();
  }

  private void validateRewritten(String content, Uri base,
      LinkRewriter linkRewriter, String expected) {
    MutableContent mc = new MutableContent(null, content);
    HttpRequest request = new HttpRequest(base);
    rewriter.rewrite(request,
        new HttpResponseBuilder().setHeader("Content-Type", "text/css").create(), mc);
    assertEquals(expected, mc.getContent());
  }

  private void validateRewritten(String content, String expected) {
    validateRewritten(content, dummyUri, defaultLinkRewriter, expected);
  }

  public void testUrlDeclarationRewrite() {
    String original =
        "div {list-style-image:url('http://a.b.com/bullet.gif');list-style-position:outside;margin:5px;padding:0}\n" +
         ".someid {background-image:url(http://a.b.com/bigimg.png);float:right;width:165px;height:23px;margin-top:4px;margin-left:5px}";
    String rewritten =
        "div {\n" +
            "  list-style-image: url('http\\3A//www.test.com/dir/proxy?url\\3Dhttp%3A%2F%2Fa.b.com%2Fbullet.gif\\26 fp\\3D 1150739864');\n" +
            "  list-style-position: outside;\n" +
            "  margin: 5px;\n" +
            "  padding: 0\n" +
            "}\n" +
            ".someid {\n" +
            "  background-image: url('http\\3A//www.test.com/dir/proxy?url\\3Dhttp%3A%2F%2Fa.b.com%2Fbigimg.png\\26 fp\\3D 1150739864');\n" +
            "  float: right;\n" +
            "  width: 165px;\n" +
            "  height: 23px;\n" +
            "  margin-top: 4px;\n" +
            "  margin-left: 5px\n" +
            "}";
    validateRewritten(original, rewritten);
  }

  public void testExtractImports() {
    String original = " @import url(www.example.org/some.css);\n" +
        "@import url('www.example.org/someother.css');\n" +
        "@import url(\"www.example.org/another.css\");\n" +
        " div { color: blue; }\n" +
        " p { color: black; }\n" +
        " span { color: red; }";
    String expected = "div {\n"
        + "  color: blue;\n"
        + "}\n"
        + "p {\n"
        + "  color: black;\n"
        + "}\n"
        + "span {\n"
        + "  color: red;\n"
        + "}";
    StringWriter sw = new StringWriter();
    List<String> stringList = rewriter
        .rewrite(new StringReader(original), dummyUri, defaultLinkRewriter, sw, true);
    assertEquals(expected, sw.toString());
    assertEquals(stringList, Lists.newArrayList("www.example.org/some.css",
        "www.example.org/someother.css", "www.example.org/another.css"));
  }

  /**
   * These tests will fail when Caja successfully parses funky CSS.
   * They can be converted into a test of success once that happens
   */
  public void testCajaParseFailureColonInRValue() {
    String original = " A {\n"
        + " -moz-opacity: 0.80;\n"
        + " filter: alpha(opacity=40);\n"
        + " filter: progid:DXImageTransform.Microsoft.Alpha(opacity=80);\n"
        + "}";
    StringWriter sw = new StringWriter();
    rewriter.rewrite(new StringReader(original), dummyUri, defaultLinkRewriter, sw, true);
    assertEquals(original, sw.toString());
  }

  public void testCajaParseFailureNoLValue() {
    String original = "body, input, td {\n"
        + "  Arial, sans-serif;\n"
        + "}";
    StringWriter sw = new StringWriter();
    rewriter.rewrite(new StringReader(original), dummyUri, defaultLinkRewriter, sw, true);
    assertEquals(original, sw.toString());
  }

  public void testCajaParseFailureCommentInContent() {
    String original = "body { font : bold; } \n//A comment\n A { font : bold; }"; 
    StringWriter sw = new StringWriter();
    rewriter.rewrite(new StringReader(original), dummyUri, defaultLinkRewriter, sw, true);
    assertEquals(original, sw.toString());
  }

  public void testCajaParseFailureDotInIdent() {
    String original = "li{list-style:none;.padding-bottom:4px;}";
    StringWriter sw = new StringWriter();
    rewriter.rewrite(new StringReader(original), dummyUri, defaultLinkRewriter, sw, true);
    assertEquals(original, sw.toString());
  }

  public void testCajaParseFailureDotInFunction() {
    String original = ".iepngfix {behavior: expression(IEPNGFIX.fix(this)); }";
    StringWriter sw = new StringWriter();
    rewriter.rewrite(new StringReader(original), dummyUri, defaultLinkRewriter, sw, true);
    assertEquals(original, sw.toString());
  }
}
