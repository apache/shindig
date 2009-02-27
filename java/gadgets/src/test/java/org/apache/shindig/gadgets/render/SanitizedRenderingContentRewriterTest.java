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
package org.apache.shindig.gadgets.render;

import org.apache.commons.io.IOUtils;
import org.apache.shindig.common.PropertiesModule;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.apache.shindig.gadgets.parse.GadgetHtmlParser;
import org.apache.shindig.gadgets.parse.caja.CajaCssParser;
import org.apache.shindig.gadgets.parse.caja.CajaCssSanitizer;
import org.apache.shindig.gadgets.parse.nekohtml.NekoHtmlParser;
import org.apache.shindig.gadgets.rewrite.ContentRewriter;
import org.apache.shindig.gadgets.rewrite.ContentRewriterFeatureFactory;
import org.apache.shindig.gadgets.rewrite.MutableContent;
import org.apache.shindig.gadgets.servlet.ProxyBase;
import org.apache.shindig.gadgets.spec.GadgetSpec;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SanitizedRenderingContentRewriterTest {
  private static final Set<String> DEFAULT_TAGS = ImmutableSet.of("html", "head", "body");
  private static final Pattern BODY_REGEX = Pattern.compile(".*<body>(.*)</body>.*");

  private static final Uri CONTENT_URI = Uri.parse("www.example.org/content");

  private final GadgetContext sanitaryGadgetContext = new GadgetContext() {
    @Override
    public String getParameter(String name) {
      return ProxyBase.SANITIZE_CONTENT_PARAM.equals(name) ? "1" : null;
    }
  };

  private final GadgetContext unsanitaryGadgetContext = new GadgetContext();

  private GadgetHtmlParser parser;

  private Gadget gadget;

  @Before
  public void setUp() throws Exception {
    Injector injector = Guice.createInjector(new TestParseModule(), new PropertiesModule());
    parser = injector.getInstance(GadgetHtmlParser.class);
    gadget = new Gadget().setContext(unsanitaryGadgetContext);
    gadget.setSpec(new GadgetSpec(Uri.parse("www.example.org/gadget.xml"),
        "<Module><ModulePrefs title=''/><Content type='x-html-sanitized'/></Module>"));
    gadget.setCurrentView(gadget.getSpec().getViews().values().iterator().next());
  }

  private String rewrite(Gadget gadget, String content, Set<String> tags, Set<String> attributes) {
    ContentRewriter rewriter = createRewriter(tags, attributes);

    MutableContent mc = new MutableContent(parser, content);
    assertEquals(0, rewriter.rewrite(gadget, mc).getCacheTtl());

    Matcher matcher = BODY_REGEX.matcher(mc.getContent());
    if (matcher.matches()) {
      return matcher.group(1);
    }
    return mc.getContent();
  }

  private String rewrite(HttpRequest request, HttpResponse response) {
    request.setSanitizationRequested(true);
    ContentRewriter rewriter = createRewriter(Collections.<String>emptySet(),
        Collections.<String>emptySet());

    MutableContent mc = new MutableContent(parser, response);
    if (rewriter.rewrite(request, response, mc) == null) {
      return null;
    }
    return mc.getContent();
  }

  private static Set<String> set(String... items) {
    return Sets.newHashSet(items);
  }

  private ContentRewriter createRewriter(Set<String> tags, Set<String> attributes) {
    Set<String> newTags = new HashSet<String>(tags);
    newTags.addAll(DEFAULT_TAGS);
    ContentRewriterFeatureFactory rewriterFeatureFactory =
        new ContentRewriterFeatureFactory(null, ".*", "", "HTTP", "embed,img,script,link,style");
    return new SanitizedRenderingContentRewriter(newTags, attributes, rewriterFeatureFactory,
        "http://www.test.com/base", new CajaCssSanitizer(new CajaCssParser()));
  }

  @Test
  public void enforceTagWhiteList() throws Exception {
    String markup =
        "<p><style type=\"text/css\">A { font : bold }</style>text <b>bold text</b></p>" +
        "<b>Bold text</b><i>Italic text<b>Bold text</b></i>";

    String sanitized = "<p>text <b>bold text</b></p><b>Bold text</b>";

    assertEquals(sanitized, rewrite(gadget, markup, set("p", "b"), set()));
  }

  @Test
  public void enforceStyleSanitized() {
    String markup =
        "<p><style type=\"text/css\">A { font : bold; behavior : bad }</style>text <b>bold text</b></p>" +
        "<b>Bold text</b><i>Italic text<b>Bold text</b></i>";

    String sanitized = "<html><head></head><body><p><style>A {\n  font: bold\n}</style>text " +
        "<b>bold text</b></p><b>Bold text</b></body></html>";
    assertEquals(sanitized, rewrite(gadget, markup, set("p", "b", "style"), set()));
  }

  @Test
  public void enforceCssImportLinkRewritten() {
    String markup =
        "<style type=\"text/css\">@import url('www.evil.com/x.js');</style>";
    String sanitized = "<style>@import url('http\\3A//www.test.com/basewww.example.org%2Fwww.evil.com%2Fx.js\\26gadget\\3Dwww.example.org%2Fgadget.xml\\26 fp\\3D 45508\\26sanitize\\3D 1\\26rewriteMime\\3Dtext/css');</style>";
    assertEquals(sanitized, rewrite(gadget, markup, set("style"), set()));
  }

  @Test
  public void enforceCssImportBadLinkStripped() {
    String markup =
        "<style type=\"text/css\">@import url('javascript:doevil()'); A { font : bold }</style>";
    String sanitized = "<html><head></head><body><style>A {\n"
        + "  font: bold\n"
        + "}</style></body></html>";
    assertEquals(sanitized, rewrite(gadget, markup, set("style"), set()));
  }

  @Test
  public void enforceAttributeWhiteList() {
    String markup = "<p foo=\"bar\" bar=\"baz\">Paragraph</p>";
    String sanitized = "<p bar=\"baz\">Paragraph</p>";
    assertEquals(sanitized, rewrite(gadget, markup, set("p"), set("bar")));
  }

  @Test
  public void enforceImageSrcProxied() {
    String markup = "<img src='http://www.evil.com/x.js'>Evil happens</img>";
    String sanitized = "<img src=\"http://www.test.com/basehttp%3A%2F%2Fwww.evil.com%2Fx.js&gadget=www.example.org%2Fgadget.xml&fp=45508&sanitize=1&rewriteMime=image/*\">Evil happens";
    assertEquals(sanitized, rewrite(gadget, markup, set("img"), set("src")));
  }

  @Test
  public void enforceBadImageUrlStripped() {
    String markup = "<img src='java\\ script:evil()'>Evil happens</img>";
    String sanitized = "<img>Evil happens";
    assertEquals(sanitized, rewrite(gadget, markup, set("img"), set("src")));
  }

  @Test
  public void enforceInvalidProxedCssRejected() {
    HttpRequest req = new HttpRequest(CONTENT_URI);
    req.setRewriteMimeType("text/css");
    HttpResponse response = new HttpResponseBuilder().setResponseString("doEvil()").create();
    String sanitized = "";
    assertEquals(sanitized, rewrite(req, response));
  }

  @Test
  public void enforceValidProxedCssAccepted() {
    HttpRequest req = new HttpRequest(CONTENT_URI);
    req.setRewriteMimeType("text/css");
    HttpResponse response = new HttpResponseBuilder().setResponseString(
        "@import url('http://www.evil.com/more.css'); A { font : BOLD }").create();
    String sanitized = "@import url('http\\3A//www.test.com/basehttp%3A%2F%2Fwww.evil.com%2Fmore.css\\26 fp\\3D 45508\\26sanitize\\3D 1\\26rewriteMime\\3Dtext/css');\n"
        + "A {\n"
        + "  font: BOLD\n"
        + "}";
    assertEquals(sanitized, rewrite(req, response));
  }

  @Test
  public void enforceInvalidProxedImageRejected() {
    HttpRequest req = new HttpRequest(CONTENT_URI);
    req.setRewriteMimeType("image/*");
    HttpResponse response = new HttpResponseBuilder().setResponse("NOTIMAGE".getBytes()).create();
    String sanitized = "";
    assertEquals(sanitized, rewrite(req, response));
  }

   @Test
  public void validProxiedImageAccepted() throws Exception {
    HttpRequest req = new HttpRequest(CONTENT_URI);
    req.setRewriteMimeType("image/*");
    HttpResponse response = new HttpResponseBuilder().setResponse(
        IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream(
            "org/apache/shindig/gadgets/rewrite/image/inefficient.png"))).create();
    assertNull(rewrite(req, response));
  }

  @Test
  public void restrictHrefAndSrcAttributes() {
    String markup =
        "<element " +
        "href=\"http://example.org/valid-href\" " +
        "src=\"http://example.org/valid-src\"/> " +
        "<element " +
        "href=\"https://example.org/valid-href\" " +
        "src=\"https://example.org/valid-src\"/> " +
        "<element " +
        "href=\"http-evil://example.org/valid-href\" " +
        "src=\"http-evil://example.org/valid-src\"/> " +
        "<element " +
        "href=\"javascript:evil()\" " +
        "src=\"javascript:evil()\" /> " +
        "<element " +
        "href=\"//example.org/valid-href\" " +
        "src=\"//example.org/valid-src\"/>";

    // TODO: This test is only valid when using a parser that converts empty tags to
    // balanced tags. The default (Neko) parser does this, with special case logic for handling
    // empty tags like br or link.
    String sanitized =
      "<element " +
      "href=\"http://example.org/valid-href\" " +
      "src=\"http://example.org/valid-src\"></element> " +
      "<element " +
      "href=\"https://example.org/valid-href\" " +
      "src=\"https://example.org/valid-src\"></element> " +
      "<element></element> " +
      "<element></element> " +
      "<element " +
      "href=\"//example.org/valid-href\" " +
      "src=\"//example.org/valid-src\"></element>";

    assertEquals(sanitized, rewrite(gadget, markup, set("element"), set("href", "src")));
  }

  @Test
  public void allCommentsStripped() {
    String markup = "<b>Hello, world</b><!--<b>evil</b>-->";
    assertEquals("<b>Hello, world</b>", rewrite(gadget, markup, set("b"), set()));
  }

  @Test
  public void doesNothingWhenNotSanitized() throws Exception {
    String markup = "<script src=\"http://evil.org/evil\"></script> <b>hello</b>";
    Gadget gadget = new Gadget().setContext(unsanitaryGadgetContext);
    gadget.setSpec(new GadgetSpec(Uri.parse("www.example.org/gadget.xml"),
        "<Module><ModulePrefs title=''/><Content type='html'/></Module>"));
    gadget.setCurrentView(gadget.getSpec().getViews().values().iterator().next());
    assertEquals(markup, rewrite(gadget, markup, set("b"), set()));
  }

  @Test
  public void forceSanitizeUnsanitaryGadget() throws Exception {
    String markup =
        "<p><style type=\"text/css\">A { font : bold; behavior : bad }</style>text <b>bold text</b></p>" +
        "<b>Bold text</b><i>Italic text<b>Bold text</b></i>";

    String sanitized = "<html><head></head><body><p><style>A {\n  font: bold\n}</style>text " +
        "<b>bold text</b></p><b>Bold text</b></body></html>";

    Gadget gadget = new Gadget().setContext(sanitaryGadgetContext);
    gadget.setSpec(new GadgetSpec(Uri.parse("www.example.org/gadget.xml"),
        "<Module><ModulePrefs title=''/><Content type='html'/></Module>"));
    gadget.setCurrentView(gadget.getSpec().getViews().values().iterator().next());
    assertEquals(sanitized, rewrite(gadget, markup, set("p", "b", "style"), set()));
  }

  private static class TestParseModule extends AbstractModule {

    @Override
    protected void configure() {
      bind(GadgetHtmlParser.class).to(NekoHtmlParser.class);
      bind(DOMImplementation.class).toProvider(DOMImplementationProvider.class);
    }

    /**
     * Provider of new HTMLDocument implementations. Used to hide XML parser weirdness
     */
    public static class DOMImplementationProvider implements Provider<DOMImplementation> {

      DOMImplementation domImpl;

      public DOMImplementationProvider() {
        try {
          DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
          // Require the traversal API
          domImpl = registry.getDOMImplementation("XML 1.0 Traversal 2.0");
        } catch (Exception e) {
          // Try another
        }
        // This is ugly but effective
        try {
          if (domImpl == null) {
            domImpl = (DOMImplementation)
                Class.forName("org.apache.xerces.internal.dom.DOMImplementationImpl").
                    getMethod("getDOMImplementation").invoke(null);
          }
        } catch (Exception ex) {
          //try another
        }
        try {
          if (domImpl == null) {
          domImpl = (DOMImplementation)
            Class.forName("com.sun.org.apache.xerces.internal.dom.DOMImplementationImpl").
                getMethod("getDOMImplementation").invoke(null);
          }
        } catch (Exception ex) {
          throw new RuntimeException("Could not find HTML DOM implementation", ex);
        }
      }

      public DOMImplementation get() {
        return domImpl;
      }
    }
  }
}
