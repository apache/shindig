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
import static org.junit.Assert.assertFalse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.apache.shindig.gadgets.parse.caja.CajaCssLexerParser;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import com.google.common.collect.Lists;

/**
 *
 */
public class CssRequestRewriterTest extends BaseRewriterTestCase {
  private CssRequestRewriter rewriter;
  private Uri dummyUri;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    ContentRewriterFeature overrideFeature =
        rewriterFeatureFactory.get(createSpecWithRewrite(".*", ".*exclude.*", "HTTP",
            HTMLContentRewriter.TAGS));
    ContentRewriterFeatureFactory factory = mockContentRewriterFeatureFactory(overrideFeature);
    rewriter = new CssRequestRewriter(factory, rewriterUris, new CajaCssLexerParser());
    dummyUri = Uri.parse("http://www.w3c.org");
  }

  @Test
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

    assertEquals(StringUtils.deleteWhitespace(expected),
        StringUtils.deleteWhitespace(mc.getContent()));
  }

  @Test
  public void testCssWithContainerProxy() throws Exception {
    String content = IOUtils.toString(this.getClass().getClassLoader().
        getResourceAsStream("org/apache/shindig/gadgets/rewrite/rewritebasic.css"));
    String expected = IOUtils.toString(this.getClass().getClassLoader().
        getResourceAsStream("org/apache/shindig/gadgets/rewrite/rewritebasic-expected.css"));
    expected = replaceDefaultWithMockServer(expected);
    
    HttpRequest request = new HttpRequest(Uri.parse("http://www.example.org/path/rewritebasic.css"));
    request.setMethod("GET");
    request.setGadget(SPEC_URL);
    request.setContainer(MOCK_CONTAINER);

    HttpResponse response = new HttpResponseBuilder().setHeader("Content-Type", "text/css")
      .setResponseString(content).create();

    MutableContent mc = new MutableContent(null, content);
    rewriter.rewrite(request, response, mc);

    assertEquals(StringUtils.deleteWhitespace(expected),
        StringUtils.deleteWhitespace(mc.getContent()));
  }

  @Test
  public void testNoRewriteUnknownMimeType() {
    MutableContent mc = control.createMock(MutableContent.class); 
    HttpRequest req = control.createMock(HttpRequest.class);
    EasyMock.expect(req.getRewriteMimeType()).andReturn("unknown");
    control.replay();
    assertFalse(rewriter.rewrite(req, fakeResponse, mc));
    control.verify();
  }

  private void validateRewritten(String content, Uri base,
      LinkRewriter linkRewriter, String expected) {
    MutableContent mc = new MutableContent(null, content);
    HttpRequest request = new HttpRequest(base);
    rewriter.rewrite(request,
        new HttpResponseBuilder().setHeader("Content-Type", "text/css").create(), mc);
    assertEquals(StringUtils.deleteWhitespace(expected),
        StringUtils.deleteWhitespace(mc.getContent()));
  }

  private void validateRewritten(String content, String expected) {
    validateRewritten(content, dummyUri, defaultLinkRewriter, expected);
  }

  @Test
  public void testUrlDeclarationRewrite() {
    String original =
        "div {list-style-image:url('http://a.b.com/bullet.gif');list-style-position:outside;margin:5px;padding:0}\n" +
         ".someid {background-image:url(http://a.b.com/bigimg.png);float:right;width:165px;height:23px;margin-top:4px;margin-left:5px}";
    String rewritten =
        "div {list-style-image:url('http://www.test.com/dir/proxy?url=http%3A%2F%2Fa.b.com%2Fbullet.gif&fp=1150739864');\n"
            + "list-style-position:outside;margin:5px;padding:0}\n"
            + ".someid {background-image:url('http://www.test.com/dir/proxy?url=http%3A%2F%2Fa.b.com%2Fbigimg.png&fp=1150739864');\n"
            + "float:right;width:165px;height:23px;margin-top:4px;margin-left:5px}";
    validateRewritten(original, rewritten);
  }

  @Test
  public void testExtractImports() {
    String original = " @import url(www.example.org/some.css);\n" +
        "@import url('www.example.org/someother.css');\n" +
        "@import url(\"www.example.org/another.css\");\n" +
        " div { color: blue; }\n" +
        " p { color: black; }\n" +
        " span { color: red; }";
    String expected = " div { color: blue; }\n" +
        " p { color: black; }\n" +
        " span { color: red; }";
    StringWriter sw = new StringWriter();
    List<String> stringList = rewriter
        .rewrite(new StringReader(original), dummyUri, defaultLinkRewriter, sw, true);
    assertEquals(expected, sw.toString());
    assertEquals(stringList, Lists.newArrayList("www.example.org/some.css",
        "www.example.org/someother.css", "www.example.org/another.css"));
  }
}
