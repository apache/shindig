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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.apache.shindig.gadgets.parse.caja.CajaCssParser;
import org.apache.shindig.gadgets.uri.PassthruManager;
import org.apache.shindig.gadgets.uri.ProxyUriManager;
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
public class CssResponseRewriterTest extends RewriterTestBase {
  private CssResponseRewriter rewriter;
  private CssResponseRewriter rewriterNoOverrideExpires;
  private Uri dummyUri;
  private ProxyUriManager proxyUriManager;
  private ContentRewriterFeature.Factory factory;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    final ContentRewriterFeature.Config overrideFeatureNoOverrideExpires =
        rewriterFeatureFactory.get(createSpecWithRewrite(".*", ".*exclude.*", null, tags));
    ContentRewriterFeature.Factory factoryNoOverrideExpires =
        new ContentRewriterFeature.Factory(null, null) {
          @Override
          public ContentRewriterFeature.Config get(HttpRequest req) {
            return overrideFeatureNoOverrideExpires;
          }
        };
    proxyUriManager = new PassthruManager("www.test.com", "/dir/proxy");
    rewriterNoOverrideExpires = new CssResponseRewriter(new CajaCssParser(),
        proxyUriManager, factoryNoOverrideExpires);
    final ContentRewriterFeature.Config overrideFeature =
        rewriterFeatureFactory.get(createSpecWithRewrite(".*", ".*exclude.*", "3600", tags));
    factory = new ContentRewriterFeature.Factory(null, null) {
      @Override
      public ContentRewriterFeature.Config get(HttpRequest req) {
        return overrideFeature;
      }
    };
    
    rewriter = new CssResponseRewriter(new CajaCssParser(),
        proxyUriManager, factory);
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

    HttpResponseBuilder response = new HttpResponseBuilder().setHeader("Content-Type", "text/css")
        .setResponseString(content);
    rewriter.rewrite(request, response);

    assertEquals(StringUtils.deleteWhitespace(expected),
        StringUtils.deleteWhitespace(response.getContent()));
  }

  @Test
  public void testCssBasicNoOverrideExpires() throws Exception {
    String content = IOUtils.toString(this.getClass().getClassLoader().
        getResourceAsStream("org/apache/shindig/gadgets/rewrite/rewritebasic.css"));
    String expected = IOUtils.toString(this.getClass().getClassLoader().
        getResourceAsStream("org/apache/shindig/gadgets/rewrite/rewritebasic-expected.css"));
    expected = expected.replace("refresh=3600", "refresh=86400");
    HttpRequest request = new HttpRequest(Uri.parse("http://www.example.org/path/rewritebasic.css"));
    request.setMethod("GET");
    request.setGadget(SPEC_URL);

    HttpResponseBuilder response = new HttpResponseBuilder().setHeader("Content-Type", "text/css")
      .setResponseString(content);
    rewriterNoOverrideExpires.rewrite(request, response);

    assertEquals(StringUtils.deleteWhitespace(expected),
        StringUtils.deleteWhitespace(response.getContent()));
  }

  @Test
  public void testCssBasicNoCache() throws Exception {
    String content = IOUtils.toString(this.getClass().getClassLoader().
        getResourceAsStream("org/apache/shindig/gadgets/rewrite/rewritebasic.css"));
    String expected = IOUtils.toString(this.getClass().getClassLoader().
        getResourceAsStream("org/apache/shindig/gadgets/rewrite/rewritebasic-expected.css"));
    expected = expected.replace("fp=1150739864", "fp=1150739864&nocache=1");
    HttpRequest request = new HttpRequest(Uri.parse("http://www.example.org/path/rewritebasic.css"));
    request.setMethod("GET");
    request.setGadget(SPEC_URL);
    request.setIgnoreCache(true);

    HttpResponseBuilder response = new HttpResponseBuilder().setHeader("Content-Type", "text/css")
      .setResponseString(content);
    rewriter.rewrite(request, response);

    assertEquals(StringUtils.deleteWhitespace(expected),
        StringUtils.deleteWhitespace(response.getContent()));
  }

  @Test
  public void testCssWithContainerProxy() throws Exception {
    String content = IOUtils.toString(this.getClass().getClassLoader().
        getResourceAsStream("org/apache/shindig/gadgets/rewrite/rewritebasic.css"));
    String expected = IOUtils.toString(this.getClass().getClassLoader().
        getResourceAsStream("org/apache/shindig/gadgets/rewrite/rewritebasic-expected.css"));
    expected = replaceDefaultWithMockServer(expected);
    proxyUriManager = new PassthruManager("www.mock.com", "/dir/proxy");
    rewriter = new CssResponseRewriter(new CajaCssParser(),
        proxyUriManager, factory);
    
    HttpRequest request = new HttpRequest(Uri.parse("http://www.example.org/path/rewritebasic.css"));
    request.setMethod("GET");
    request.setGadget(SPEC_URL);
    request.setContainer(MOCK_CONTAINER);

    HttpResponseBuilder response = new HttpResponseBuilder().setHeader("Content-Type", "text/css")
      .setResponseString(content);

    rewriter.rewrite(request, response);

    assertEquals(StringUtils.deleteWhitespace(expected),
        StringUtils.deleteWhitespace(response.getContent()));
  }

  @Test
  public void testNoRewriteUnknownMimeType() throws Exception {
    HttpRequest req = control.createMock(HttpRequest.class);
    EasyMock.expect(req.getRewriteMimeType()).andReturn("unknown");
    control.replay();
    int changesBefore = fakeResponse.getNumChanges();
    rewriter.rewrite(req, fakeResponse);
    assertEquals(changesBefore, fakeResponse.getNumChanges());
    control.verify();
  }

  private void validateRewritten(String content, Uri base, String expected) throws Exception {
    HttpResponseBuilder response = new HttpResponseBuilder().setHeader("Content-Type", "text/css");
    response.setContent(content);
    HttpRequest request = new HttpRequest(base);
    rewriter.rewrite(request, response);
    assertEquals(StringUtils.deleteWhitespace(expected),
        StringUtils.deleteWhitespace(response.getContent()));
  }

  private void validateRewritten(String content, String expected) throws Exception {
    validateRewritten(content, dummyUri, expected);
  }

  @Test
  public void testUrlDeclarationRewrite() throws Exception {
    String original =
        "div {list-style-image:url('http://a.b.com/bullet.gif');list-style-position:outside;margin:5px;padding:0}\n" +
         ".someid {background-image:url(http://a.b.com/bigimg.png);float:right;width:165px;height:23px;margin-top:4px;margin-left:5px}";
    String rewritten =
        "div {list-style-image:url('http://www.test.com/dir/proxy?url=http%3A%2F%2Fa.b.com%2Fbullet.gif');\n"
            + "list-style-position:outside;margin:5px;padding:0}\n"
            + ".someid {background-image:url('http://www.test.com/dir/proxy?url=http%3A%2F%2Fa.b.com%2Fbigimg.png');\n"
            + "float:right;width:165px;height:23px;margin-top:4px;margin-left:5px}";
    validateRewritten(original, rewritten);
  }

  @Test
  public void testExtractImports() throws Exception {
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
        .rewrite(new StringReader(original), dummyUri,
          CssResponseRewriter.uriMaker(proxyUriManager, defaultRewriterFeature), sw, true);
    assertEquals(StringUtils.deleteWhitespace(expected),
        StringUtils.deleteWhitespace(sw.toString()));
    assertEquals(Lists.newArrayList("www.example.org/some.css",
        "www.example.org/someother.css", "www.example.org/another.css"), stringList);
  }

  @Test
  public void testMalformedImport() throws Exception {
    String original = " @import \"www.example.org/some.css\";\n" +
        " span { color: red; }";
    String expected = " span { color: red; }";
    StringWriter sw = new StringWriter();
    List<String> stringList = rewriter
        .rewrite(new StringReader(original), dummyUri,
          CssResponseRewriter.uriMaker(proxyUriManager, defaultRewriterFeature), sw, true);
    assertEquals(StringUtils.deleteWhitespace(expected),
        StringUtils.deleteWhitespace(sw.toString()));
    assertEquals(Lists.newArrayList("www.example.org/some.css"), stringList);
  }
}
