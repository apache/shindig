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

import static org.junit.Assert.assertEquals;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.parse.caja.CajaCssParser;
import org.apache.shindig.gadgets.parse.caja.CajaCssSanitizer;
import org.apache.shindig.gadgets.rewrite.BaseRewriterTestCase;
import org.apache.shindig.gadgets.rewrite.ContentRewriterFeatureFactory;
import org.apache.shindig.gadgets.rewrite.GadgetRewriter;
import org.apache.shindig.gadgets.rewrite.MutableContent;
import org.apache.shindig.gadgets.servlet.ProxyBase;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

public class SanitizingGadgetRewriterTest extends BaseRewriterTestCase {
  private static final Set<String> DEFAULT_TAGS = ImmutableSet.of("html", "head", "body");
  private static final Pattern BODY_REGEX = Pattern.compile(".*<body>(.*)</body>.*");

  private final GadgetContext sanitaryGadgetContext = new GadgetContext() {
    @Override
    public String getParameter(String name) {
      return ProxyBase.SANITIZE_CONTENT_PARAM.equals(name) ? "1" : null;
    }
    
    @Override
    public String getContainer() {
      return MOCK_CONTAINER;
    }
  };

  private final GadgetContext unsanitaryGadgetContext = new GadgetContext();
  private Gadget gadget;

  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();
    
    gadget = new Gadget().setContext(unsanitaryGadgetContext);
    gadget.setSpec(new GadgetSpec(Uri.parse("www.example.org/gadget.xml"),
        "<Module><ModulePrefs title=''/><Content type='x-html-sanitized'/></Module>"));
    gadget.setCurrentView(gadget.getSpec().getViews().values().iterator().next());
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
    ContentRewriterFeatureFactory rewriterFeatureFactory =
        new ContentRewriterFeatureFactory(null, ".*", "", "HTTP", "embed,img,script,link,style");
    return new SanitizingGadgetRewriter(newTags, attributes, rewriterFeatureFactory,
        rewriterUris, new CajaCssSanitizer(new CajaCssParser()));
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
  public void enforceCssImportLinkRewritten() throws Exception {
    String markup =
        "<style type=\"text/css\">@import url('www.evil.com/x.js');</style>";
    // The caja css sanitizer does *not* remove the initial colon in urls
    // since this does not work in IE
    String sanitized = 
        "<style>" 
      + "@import url('http://www.test.com/dir/proxy?url=www.example.org%2F"
      +	"www.evil.com%2Fx.js\\26gadget=www.example.org%2Fgadget.xml\\26 "
      +	"fp=45508\\26sanitize=1\\26rewriteMime=text/css');" 
      + "</style>";
    String rewritten = rewrite(gadget, markup, set("style"), set());
    assertEquals(sanitized, rewritten);
  }

  @Test
  public void enforceCssImportBadLinkStripped() throws Exception {
    String markup =
        "<style type=\"text/css\">@import url('javascript:doevil()'); A { font : bold }</style>";
    String sanitized = "<html><head></head><body><style>A {\n"
        + "  font: bold\n"
        + "}</style></body></html>";
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
    String sanitized = "<img src=\"http://www.test.com/dir/proxy?url=http%3A%2F%2Fwww.evil.com%2Fx.js&gadget=www.example.org%2Fgadget.xml&fp=45508&sanitize=1&rewriteMime=image/*\">Evil happens";
    assertEquals(sanitized, rewrite(gadget, markup, set("img"), set("src")));
  }

  @Test
  public void enforceBadImageUrlStripped() throws Exception {
    String markup = "<img src='java\\ script:evil()'>Evil happens</img>";
    String sanitized = "<img>Evil happens";
    assertEquals(sanitized, rewrite(gadget, markup, set("img"), set("src")));
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
  public void restrictHrefAndSrcAttributes() throws Exception {
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
    gadget.setSpec(new GadgetSpec(Uri.parse("www.example.org/gadget.xml"),
        "<Module><ModulePrefs title=''/><Content type='html'/></Module>"));
    gadget.setCurrentView(gadget.getSpec().getViews().values().iterator().next());
    assertEquals(sanitized, rewrite(gadget, markup, set("p", "b", "style"), set()));
  }
}
