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

import com.google.inject.util.Providers;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.parse.GadgetHtmlParser;
import org.apache.shindig.gadgets.parse.caja.CajaCssParser;
import org.apache.shindig.gadgets.parse.caja.CajaCssSanitizer;
import org.apache.shindig.gadgets.parse.caja.CajaHtmlParser;
import org.apache.shindig.gadgets.rewrite.RewriterTestBase;
import org.apache.shindig.gadgets.rewrite.ContentRewriterFeature;
import org.apache.shindig.gadgets.rewrite.GadgetRewriter;
import org.apache.shindig.gadgets.rewrite.MutableContent;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.uri.PassthruManager;
import org.apache.shindig.gadgets.uri.UriCommon.Param;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SanitizingGadgetRewriterTest extends RewriterTestBase {
  private static final Set<String> DEFAULT_TAGS = ImmutableSet.of("html", "head", "body");
  private static final Pattern BODY_REGEX = Pattern.compile(".*<body>(.+)</body>.*");

  private final GadgetContext sanitaryGadgetContext = new GadgetContext() {
    @Override
    public String getParameter(String name) {
      return Param.SANITIZE.getKey().equals(name) ? "1" : null;
    }

    @Override
    public String getContainer() {
      return MOCK_CONTAINER;
    }
  };

  private final GadgetContext unsanitaryGadgetContext = new GadgetContext();
  private final GadgetContext unsanitaryGadgetContextNoCacheAndDebug = new GadgetContext(){
    @Override
    public boolean getIgnoreCache() {
      return true;
    }
    @Override
    public boolean getDebug() {
      return true;
    }
  };
  private Gadget gadget;
  private Gadget gadgetNoCacheAndDebug;

  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();

    gadget = new Gadget().setContext(unsanitaryGadgetContext);
    gadget.setSpec(new GadgetSpec(Uri.parse("http://www.example.org/gadget.xml"),
        "<Module><ModulePrefs title=''/><Content type='x-html-sanitized'/></Module>"));
    gadget.setCurrentView(gadget.getSpec().getViews().values().iterator().next());

    gadgetNoCacheAndDebug = new Gadget().setContext(unsanitaryGadgetContextNoCacheAndDebug);
    gadgetNoCacheAndDebug.setSpec(new GadgetSpec(Uri.parse("http://www.example.org/gadget.xml"),
        "<Module><ModulePrefs title=''/><Content type='x-html-sanitized'/></Module>"));
    gadgetNoCacheAndDebug.setCurrentView(gadgetNoCacheAndDebug.getSpec().getViews().values().iterator().next());
  }

  @Override
  protected Class<? extends GadgetHtmlParser> getParserClass() {
    return CajaHtmlParser.class;
  }

  private String rewrite(Gadget gadget, String content, Set<String> tags, Set<String> attributes)
      throws Exception {
    GadgetRewriter rewriter = createRewriter(tags, attributes);

    MutableContent mc = new MutableContent(parser, content);
    rewriter.rewrite(gadget, mc);

    Matcher matcher = BODY_REGEX.matcher(mc.getContent());
    if (matcher.matches()) {
      return matcher.group(1);
    }
    return mc.getContent();
  }

  private static Set<String> set(String... items) {
    return Sets.newHashSet(items);
  }

  private GadgetRewriter createRewriter(Set<String> tags, Set<String> attributes) {
    Set<String> newTags = new HashSet<String>(tags);
    newTags.addAll(DEFAULT_TAGS);
    ContentRewriterFeature.Factory rewriterFeatureFactory =
        new ContentRewriterFeature.Factory(null,
          Providers.of(new ContentRewriterFeature.DefaultConfig(
            ".*", "", "HTTP", "embed,img,script,link,style", false, false, false)));
    return new SanitizingGadgetRewriter(Providers.of(newTags), Providers.of(attributes), rewriterFeatureFactory,
        new CajaCssSanitizer(new CajaCssParser()), new PassthruManager("host.com", "/proxy"));
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
  public void enforceStyleSanitized() throws Exception {
    String markup =
        "<p><style type=\"text/css\">A { font : bold; behavior : bad }</style>text <b>bold text</b></p>" +
        "<b>Bold text</b><i>Italic text<b>Bold text</b></i>";

    String sanitized = "<html><head></head><body><p><style>A {\n  font: bold\n}</style>text " +
        "<b>bold text</b></p><b>Bold text</b></body></html>";
    assertEquals(sanitized, rewrite(gadget, markup, set("p", "b", "style"), set()));
  }

  @Test
  public void enforceStyleLinkRewritten() throws Exception {
    String markup =
        "<link rel=\"stylesheet\" "
            + "href=\"http://www.test.com/dir/proxy?"
            + "url=http%3A%2F%2Fwww.evil.com%2Fx.css&gadget=www.example.org%2Fgadget.xml&"
            + "fp=45508&rewriteMime=text/css\"/>";
    String sanitized =
        "<html><head><link href=\"http://host.com/proxy?url=http%3A%2F%2Fwww.test.com%2Fdir%2F" +
        "proxy%3Furl%3Dhttp%253A%252F%252Fwww.evil.com%252Fx.css%26gadget%3Dwww.example.org%252F" +
        "gadget.xml%26fp%3D45508%26rewriteMime%3Dtext%2Fcss&amp;sanitize=1&amp;rewriteMime=text%2Fcss\" " +
        "rel=\"stylesheet\"></head><body></body></html>";
    String rewritten = rewrite(gadget, markup, set("link"), set("rel", "href"));
    assertEquals(sanitized, rewritten);
  }

  @Test
  public void enforceStyleLinkRewrittenNoCacheAndDebug() throws Exception {
    String markup =
        "<link rel=\"stylesheet\" "
            + "href=\"http://www.test.com/dir/proxy?"
            + "url=http%3A%2F%2Fwww.evil.com%2Fx.css&gadget=www.example.org%2Fgadget.xml&"
            + "fp=45508&rewriteMime=text/css\"/>";
    String sanitized =
        "<html><head><link href=\"http://host.com/proxy?url=http%3A%2F%2Fwww.test.com%2F"
            + "dir%2Fproxy%3Furl%3Dhttp%253A%252F%252Fwww.evil.com%252Fx.css%26gadget%3D"
            + "www.example.org%252Fgadget.xml%26fp%3D45508%26rewriteMime%3Dtext%2Fcss&amp;"
            + "sanitize=1&amp;rewriteMime=text%2Fcss\" rel=\"stylesheet\">"
            + "</head><body></body></html>";
    String rewritten = rewrite(gadgetNoCacheAndDebug, markup, set("link"), set("rel", "href"));
    assertEquals(sanitized, rewritten);
  }

  @Test
  public void enforceNonStyleLinkStripped() throws Exception {
    String markup =
        "<link rel=\"script\" "
            + "href=\"www.exmaple.org/evil.js\"/>";
    String rewritten = rewrite(gadget, markup, set("link"), set("rel", "href", "type"));
    assertEquals("<html><head></head><body></body></html>", rewritten);
  }

  @Test
  public void enforceNonStyleLinkStrippedNoCacheAndDebug() throws Exception {
    String markup =
        "<link rel=\"script\" "
            + "href=\"www.exmaple.org/evil.js\"/>";
    String rewritten = rewrite(gadgetNoCacheAndDebug, markup, set("link"), set("rel", "href", "type"));
    assertEquals("<html><head></head><body></body></html>", rewritten);
  }

  @Test
  public void enforceCssImportLinkRewritten() throws Exception {
    String markup =
        "<style type=\"text/css\">@import url('www.evil.com/x.js');</style>";
    // The caja css sanitizer does *not* remove the initial colon in urls
    // since this does not work in IE
    String sanitized =
        "<html><head><style>"
      + "@import url('http://host.com/proxy?url=http%3A%2F%2Fwww.example.org%2Fwww.evil.com%2Fx.js&"
      + "sanitize=1&rewriteMime=text%2Fcss');"
      + "</style></head><body></body></html>";
    String rewritten = rewrite(gadget, markup, set("style"), set());
    assertEquals(sanitized, rewritten);
  }

  @Test
  public void enforceCssImportLinkRewrittenNoCacheAndDebug() throws Exception {
    String markup =
        "<style type=\"text/css\">@import url('www.evil.com/x.js');</style>";
    // The caja css sanitizer does *not* remove the initial colon in urls
    // since this does not work in IE
    String sanitized =
        "<html><head><style>"
      + "@import url('http://host.com/proxy?url=http%3A%2F%2Fwww.example.org%2Fwww.evil.com%2Fx.js&sanitize=1"
      + "&rewriteMime=text%2Fcss');</style></head><body></body></html>";
    String rewritten = rewrite(gadgetNoCacheAndDebug, markup, set("style"), set());
    assertEquals(sanitized, rewritten);
  }

  @Test
  public void enforceCssImportBadLinkStripped() throws Exception {
    String markup =
        "<style type=\"text/css\">@import url('javascript:doevil()'); A { font : bold }</style>";
    String sanitized = "<html><head><style>A {\n"
        + "  font: bold\n"
        + "}</style></head><body></body></html>";
    assertEquals(sanitized, rewrite(gadget, markup, set("style"), set()));
  }

  @Test
  public void enforceAttributeWhiteList() throws Exception {
    String markup = "<p foo=\"bar\" bar=\"baz\">Paragraph</p>";
    String sanitized = "<p bar=\"baz\">Paragraph</p>";
    assertEquals(sanitized, rewrite(gadget, markup, set("p"), set("bar")));
  }

  @Test
  public void enforceImageSrcProxied() throws Exception {
    String markup = "<img src='http://www.evil.com/x.js'>Evil happens</img>";
    String sanitized = "<img src=\"http://host.com/proxy?url=http%3A%2F%2F" +
        "www.evil.com%2Fx.js&amp;sanitize=1&amp;rewriteMime=image%2F*\">Evil happens";
    assertEquals(sanitized, rewrite(gadget, markup, set("img"), set("src")));
  }

  @Test
  public void enforceImageSrcProxiedNoCacheAndDebug() throws Exception {
    String markup = "<img src='http://www.evil.com/x.js'>Evil happens</img>";
    String sanitized = "<img src=\"http://host.com/proxy?url=http%3A%2F%2Fwww.evil.com" +
        "%2Fx.js&amp;sanitize=1&amp;rewriteMime=image%2F*\">Evil happens";
    assertEquals(sanitized, rewrite(gadgetNoCacheAndDebug, markup, set("img"), set("src")));
  }

  @Test
  public void enforceBadImageUrlStripped() throws Exception {
    String markup = "<img src='java\\ script:evil()'>Evil happens</img>";
    String sanitized = "<img>Evil happens";
    assertEquals(sanitized, rewrite(gadget, markup, set("img"), set("src")));
  }

  @Test
  public void enforceTargetTopRestricted() throws Exception {
    String markup = "<a href=\"http://www.example.com\" target=\"_top\">x</a>";
    String sanitized = "<a href=\"http://www.example.com\">x</a>";
    assertEquals(sanitized, rewrite(gadget, markup, set("a"), set("href", "target")));
  }

  @Test
  public void enforceTargetSelfAllowed() throws Exception {
    String markup = "<a href=\"http://www.example.com\" target=\"_self\">x</a>";
    assertEquals(markup, rewrite(gadget, markup, set("a"), set("href", "target")));
  }

  @Test
  public void enforceTargetBlankAllowed() throws Exception {
    String markup = "<a href=\"http://www.example.com\" target=\"_BlAnK\">x</a>";
    assertEquals(markup, rewrite(gadget, markup, set("a"), set("href", "target")));
  }

  @Test
  public void sanitizationBypassAllowed() throws Exception {
    String markup = "<p foo=\"bar\"><b>Parag</b><!--raph--></p>";
    // Create a rewriter that would strip everything
    GadgetRewriter rewriter = createRewriter(set(), set());

    MutableContent mc = new MutableContent(parser, markup);
    Document document = mc.getDocument();
    // Force the content to get re-serialized
    MutableContent.notifyEdit(document);
    String fullMarkup = mc.getContent();

    Element paragraphTag = (Element) document.getElementsByTagName("p").item(0);
    // Mark the paragraph tag element as trusted
    SanitizingGadgetRewriter.bypassSanitization(paragraphTag, true);
    rewriter.rewrite(gadget, mc);

    // The document should be unchanged
    assertEquals(fullMarkup, mc.getContent());
  }

  @Test
  public void sanitizationBypassOnlySelf() throws Exception {
    String markup = "<p foo=\"bar\"><b>Parag</b><!--raph--></p>";
    // Create a rewriter that would strip everything
    GadgetRewriter rewriter = createRewriter(set(), set());

    MutableContent mc = new MutableContent(parser, markup);
    Document document = mc.getDocument();

    Element paragraphTag = (Element) document.getElementsByTagName("p").item(0);
    // Mark the paragraph tag element as trusted
    SanitizingGadgetRewriter.bypassSanitization(paragraphTag, false);
    rewriter.rewrite(gadget, mc);

    // The document should be unchanged
    String content = mc.getContent();
    Matcher matcher = BODY_REGEX.matcher(content);
    matcher.matches();
    assertEquals("<p foo=\"bar\"></p>", matcher.group(1));
  }

  @Test
  public void sanitizationBypassPreservedAcrossClone() throws Exception {
    String markup = "<p foo=\"bar\"><b>Parag</b><!--raph--></p>";
    // Create a rewriter that would strip everything
    GadgetRewriter rewriter = createRewriter(set(), set());

    MutableContent mc = new MutableContent(parser, markup);
    Document document = mc.getDocument();

    Element paragraphTag = (Element) document.getElementsByTagName("p").item(0);
    // Mark the paragraph tag element as trusted
    SanitizingGadgetRewriter.bypassSanitization(paragraphTag, false);

    // Now, clone the paragraph tag and replace the paragraph tag
    Element cloned = (Element) paragraphTag.cloneNode(true);
    paragraphTag.getParentNode().replaceChild(cloned, paragraphTag);

    rewriter.rewrite(gadget, mc);

    // The document should be unchanged
    String content = mc.getContent();
    Matcher matcher = BODY_REGEX.matcher(content);
    matcher.matches();
    assertEquals("<p foo=\"bar\"></p>", matcher.group(1));
  }

  @Test
  public void allCommentsStripped() throws Exception {
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
    gadget.setSpec(new GadgetSpec(Uri.parse("http://www.example.org/gadget.xml"),
        "<Module><ModulePrefs title=''/><Content type='html'/></Module>"));
    gadget.setCurrentView(gadget.getSpec().getViews().values().iterator().next());
    assertEquals(sanitized, rewrite(gadget, markup, set("p", "b", "style"), set()));
  }
}
