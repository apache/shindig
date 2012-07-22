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

import static org.junit.Assert.assertEquals;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.config.BasicContainerConfig;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.apache.shindig.gadgets.parse.caja.CajaCssParser;
import org.apache.shindig.gadgets.uri.DefaultProxyUriManager;
import org.apache.shindig.gadgets.uri.ProxyUriManager;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

/**
 * Tests for CssResponseRewriter.
 */
public class CssResponseRewriterTest extends RewriterTestBase {
  private static final ImmutableMap<String, Object> DEFAULT_CONTAINER_CONFIG = ImmutableMap
      .<String, Object>builder()
      .put(ContainerConfig.CONTAINER_KEY, ImmutableList.of("default"))
      .put(DefaultProxyUriManager.PROXY_HOST_PARAM, "www.test.com")
      .put(DefaultProxyUriManager.PROXY_PATH_PARAM, "/dir/proxy")
      .build();
  private static final ImmutableMap<String, Object> MOCK_CONTAINER_CONFIG = ImmutableMap
      .<String, Object>builder()
      .put(ContainerConfig.CONTAINER_KEY, ImmutableList.of(MOCK_CONTAINER))
      .put(DefaultProxyUriManager.PROXY_HOST_PARAM, "www.mock.com")
      .build();

  private CssResponseRewriter rewriter;
  private CssResponseRewriter rewriterNoOverrideExpires;
  private Uri dummyUri;
  private GadgetContext gadgetContext;
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
    ContainerConfig config = new BasicContainerConfig();
    config
        .newTransaction()
        .addContainer(DEFAULT_CONTAINER_CONFIG)
        .addContainer(MOCK_CONTAINER_CONFIG)
        .commit();
    proxyUriManager = new DefaultProxyUriManager(config, null);
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
    gadgetContext = new GadgetContext() {
      @Override
      public Uri getUrl() {
        return dummyUri;
      }
    };
  }

  private void testCssBasic(Gadget gadget) throws Exception {
    String content = IOUtils.toString(this.getClass().getClassLoader().
        getResourceAsStream("org/apache/shindig/gadgets/rewrite/rewritebasic.css"));
    String expected = IOUtils.toString(this.getClass().getClassLoader().
        getResourceAsStream("org/apache/shindig/gadgets/rewrite/rewritebasic-expected.css"));
    HttpRequest request = new HttpRequest(Uri.parse("http://www.example.org/path/rewritebasic.css"));
    request.setMethod("GET");
    request.setGadget(SPEC_URL);

    HttpResponseBuilder response = new HttpResponseBuilder().setHeader("Content-Type", "text/css")
        .setResponseString(content);

    rewriter.rewrite(request, response, gadget);

    assertEquals(StringUtils.deleteWhitespace(expected),
        StringUtils.deleteWhitespace(response.getContent()));
  }

  @Test
  public void testCssBasicGadget() throws Exception {
    Gadget gadget = mockGadget();
    control.replay();
    testCssBasic(gadget);
  }

  @Test
  public void testCssBasicNoGadget() throws Exception {
    testCssBasic(null);
  }

  private void testCssBasicNoOverrideExpires(Gadget gadget) throws Exception {
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

    rewriterNoOverrideExpires.rewrite(request, response, gadget);

    assertEquals(StringUtils.deleteWhitespace(expected),
        StringUtils.deleteWhitespace(response.getContent()));
  }

  @Test
  public void testCssBasicNoOverrideExpiresGadget() throws Exception {
    Gadget gadget = mockGadget();
    control.replay();
    testCssBasicNoOverrideExpires(gadget);
  }

  @Test
  public void testCssBasicNoOverrideExpiresNoGadget() throws Exception {
    testCssBasicNoOverrideExpires(null);
  }

  private void testCssBasicNoCache(Gadget gadget) throws Exception {
    String content = IOUtils.toString(this.getClass().getClassLoader().
        getResourceAsStream("org/apache/shindig/gadgets/rewrite/rewritebasic.css"));
    String expected = IOUtils.toString(this.getClass().getClassLoader().
        getResourceAsStream("org/apache/shindig/gadgets/rewrite/rewritebasic-expected.css"));
    expected = expected.replace("nocache=0", "nocache=1");
    HttpRequest request = new HttpRequest(Uri.parse("http://www.example.org/path/rewritebasic.css"));
    request.setMethod("GET");
    request.setGadget(SPEC_URL);
    request.setIgnoreCache(true);

    HttpResponseBuilder response = new HttpResponseBuilder().setHeader("Content-Type", "text/css")
      .setResponseString(content);

    rewriter.rewrite(request, response, gadget);

    assertEquals(StringUtils.deleteWhitespace(expected),
        StringUtils.deleteWhitespace(response.getContent()));
  }

  @Test
  public void testCssBasicNoCacheGadget() throws Exception {
    Gadget gadget = mockGadget();
    control.replay();
    testCssBasicNoCache(gadget);
  }

  @Test
  public void testCssBasicNoCacheNoGadget() throws Exception {
    testCssBasicNoCache(null);
  }

  private void testCssWithContainerProxy(Gadget gadget) throws Exception {
    String content = IOUtils.toString(this.getClass().getClassLoader().
        getResourceAsStream("org/apache/shindig/gadgets/rewrite/rewritebasic.css"));
    String expected = IOUtils.toString(this.getClass().getClassLoader().
        getResourceAsStream("org/apache/shindig/gadgets/rewrite/rewritebasic-expected.css"));
    expected = replaceDefaultWithMockServer(expected);
    expected = expected.replace("container=default", "container=" + MOCK_CONTAINER);
    rewriter = new CssResponseRewriter(new CajaCssParser(),
        proxyUriManager, factory);

    HttpRequest request = new HttpRequest(Uri.parse("http://www.example.org/path/rewritebasic.css"));
    request.setMethod("GET");
    request.setGadget(SPEC_URL);
    request.setContainer(MOCK_CONTAINER);

    HttpResponseBuilder response = new HttpResponseBuilder().setHeader("Content-Type", "text/css")
      .setResponseString(content);

    rewriter.rewrite(request, response, gadget);

    assertEquals(StringUtils.deleteWhitespace(expected),
        StringUtils.deleteWhitespace(response.getContent()));
  }

  @Test
  public void testCssWithContainerProxyGadget() throws Exception {
    Gadget gadget = mockGadget();
    control.replay();
    testCssWithContainerProxy(gadget);
  }

  @Test
  public void testCssWithContainerProxyNoGadget() throws Exception {
    testCssWithContainerProxy(null);
  }

  private void testNoRewriteUnknownMimeType(Gadget gadget) throws Exception {
    HttpRequest req = control.createMock(HttpRequest.class);
    EasyMock.expect(req.getRewriteMimeType()).andReturn("unknown");
    control.replay();
    int changesBefore = fakeResponse.getNumChanges();

    rewriter.rewrite(req, fakeResponse, gadget);
    assertEquals(changesBefore, fakeResponse.getNumChanges());
    control.verify();
  }

  @Test
  public void testNoRewriteUnknownMimeTypeGadget() throws Exception {
    Gadget gadget = mockGadget();
    testNoRewriteUnknownMimeType(gadget);
  }

  @Test
  public void testNoRewriteUnknownMimeTypeNoGadget() throws Exception {
    testNoRewriteUnknownMimeType(null);
  }

  private void validateRewritten(String content, Uri base, String expected, Gadget gadget) throws Exception {
    HttpResponseBuilder response = new HttpResponseBuilder().setHeader("Content-Type", "text/css");
    response.setContent(content);
    HttpRequest request = new HttpRequest(base);
    if(gadget == null) {
      rewriter.rewrite(request, response, gadget);
    } else {
      rewriter.rewrite(request, response, gadget);
    }
    assertEquals(StringUtils.deleteWhitespace(expected),
        StringUtils.deleteWhitespace(response.getContent()));
  }

  private void validateRewritten(String content, String expected, Gadget gadget) throws Exception {
    validateRewritten(content, dummyUri, expected, gadget);
  }

  public void testUrlDeclarationRewrite(Gadget gadget) throws Exception {
    String original =
        "div {list-style-image:url('http://a.b.com/bullet.gif');list-style-position:outside;margin:5px;padding:0}\n" +
         ".someid {background-image:url(http://a.b.com/bigimg.png);float:right;width:165px;height:23px;margin-top:4px;margin-left:5px}";
    String rewritten =
        "div {list-style-image:url('//www.test.com/dir/proxy?container=default"
            + "&gadget=http%3A%2F%2Fwww.w3c.org&debug=0&nocache=0"
            + "&url=http%3A%2F%2Fa.b.com%2Fbullet.gif');\n"
            + "list-style-position:outside;margin:5px;padding:0}\n"
            + ".someid {background-image:url('//www.test.com/dir/proxy?container=default"
            + "&gadget=http%3A%2F%2Fwww.w3c.org&debug=0&nocache=0"
            + "&url=http%3A%2F%2Fa.b.com%2Fbigimg.png');\n"
            + "float:right;width:165px;height:23px;margin-top:4px;margin-left:5px}";
    validateRewritten(original, rewritten, gadget);
  }

  @Test
  public void testUrlDeclarationRewriteGadget() throws Exception {
    Gadget gadget = mockGadget();
    control.replay();
    testUrlDeclarationRewrite(gadget);
  }

  @Test
  public void testUrlDeclarationRewriteNoGadget() throws Exception {
    testUrlDeclarationRewrite(null);
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
            CssResponseRewriter.uriMaker(proxyUriManager, defaultRewriterFeature), sw,
            true, gadgetContext);
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
            CssResponseRewriter.uriMaker(proxyUriManager, defaultRewriterFeature), sw,
            true, gadgetContext);
    assertEquals(StringUtils.deleteWhitespace(expected),
        StringUtils.deleteWhitespace(sw.toString()));
    assertEquals(Lists.newArrayList("www.example.org/some.css"), stringList);
  }
}
