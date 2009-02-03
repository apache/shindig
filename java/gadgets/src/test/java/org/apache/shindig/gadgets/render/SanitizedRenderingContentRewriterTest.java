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

import org.apache.shindig.common.PropertiesModule;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.parse.GadgetHtmlParser;
import org.apache.shindig.gadgets.parse.nekohtml.NekoHtmlParser;
import org.apache.shindig.gadgets.rewrite.ContentRewriter;
import org.apache.shindig.gadgets.rewrite.MutableContent;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provider;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SanitizedRenderingContentRewriterTest {
  private static final Set<String> DEFAULT_TAGS = ImmutableSet.of("html", "head", "body");
  private static final Pattern BODY_REGEX = Pattern.compile(".*<body>(.*)</body>.*");

  private final GadgetContext sanitaryGadgetContext = new GadgetContext() {
    @Override
    public String getParameter(String name) {
      return "sanitize".equals(name) ? "1" : null;
    }
  };

  private final GadgetContext unsanitaryGadgetContext = new GadgetContext();

  private GadgetHtmlParser parser;

  @Before
  public void setUp() throws Exception {
    Injector injector = Guice.createInjector(new TestParseModule(), new PropertiesModule());
    parser = injector.getInstance(GadgetHtmlParser.class);
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

  private static Set<String> set(String... items) {
    return Sets.newHashSet(items);
  }

  private ContentRewriter createRewriter(Set<String> tags, Set<String> attributes) {
    Set<String> newTags = new HashSet<String>(tags);
    newTags.addAll(DEFAULT_TAGS);
    return new SanitizedRenderingContentRewriter(newTags, attributes);
  }

  @Test
  public void enforceTagWhiteList() {
    String markup =
        "<p><style type=\"text/css\">styles</style>text <b>bold text</b></p>" +
        "<b>Bold text</b><i>Italic text<b>Bold text</b></i>";

    String sanitized = "<p>text <b>bold text</b></p><b>Bold text</b>";



    Gadget gadget = new Gadget().setContext(sanitaryGadgetContext);

    assertEquals(sanitized, rewrite(gadget, markup, set("p", "b"), set()));
  }

  @Test
  public void enforceAttributeWhiteList() {
    String markup = "<p foo=\"bar\" bar=\"baz\">Paragraph</p>";
    String sanitized = "<p bar=\"baz\">Paragraph</p>";

    Gadget gadget = new Gadget().setContext(sanitaryGadgetContext);

    assertEquals(sanitized, rewrite(gadget, markup, set("p"), set("bar")));
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

    Gadget gadget = new Gadget().setContext(sanitaryGadgetContext);

    assertEquals(sanitized, rewrite(gadget, markup, set("element"), set("href", "src")));
  }

  @Test
  public void allCommentsStripped() {
    String markup = "<b>Hello, world</b><!--<b>evil</b>-->";

    Gadget gadget = new Gadget().setContext(sanitaryGadgetContext);

    assertEquals("<b>Hello, world</b>", rewrite(gadget, markup, set("b"), set()));
  }

  @Test
  public void doesNothingWhenNotSanitized() {
    String markup = "<script src=\"http://evil.org/evil\"></script> <b>hello</b>";

    Gadget gadget = new Gadget().setContext(unsanitaryGadgetContext);

    assertEquals(markup, rewrite(gadget, markup, set("b"), set()));
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
