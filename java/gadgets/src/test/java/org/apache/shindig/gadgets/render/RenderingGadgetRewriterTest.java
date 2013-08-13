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

import static org.apache.shindig.gadgets.render.RenderingGadgetRewriter.DEFAULT_CSS;
import static org.apache.shindig.gadgets.render.RenderingGadgetRewriter.FEATURES_KEY;
import static org.apache.shindig.gadgets.render.RenderingGadgetRewriter.INSERT_BASE_ELEMENT_KEY;
import static org.apache.shindig.gadgets.render.RenderingGadgetRewriter.IS_GADGET_BEACON;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.getCurrentArguments;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.same;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.shindig.common.EasyMockTestCase;
import org.apache.shindig.common.JsonAssert;
import org.apache.shindig.common.PropertiesModule;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.xml.XmlUtil;
import org.apache.shindig.config.BasicContainerConfig;
import org.apache.shindig.expressions.Expressions;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.admin.GadgetAdminStore;
import org.apache.shindig.gadgets.config.ConfigContributor;
import org.apache.shindig.gadgets.config.CoreUtilConfigContributor;
import org.apache.shindig.gadgets.config.DefaultConfigProcessor;
import org.apache.shindig.gadgets.config.XhrwrapperConfigContributor;
import org.apache.shindig.gadgets.features.FeatureRegistry;
import org.apache.shindig.gadgets.features.FeatureRegistryProvider;
import org.apache.shindig.gadgets.features.FeatureResource;
import org.apache.shindig.gadgets.js.JsException;
import org.apache.shindig.gadgets.js.JsRequest;
import org.apache.shindig.gadgets.js.JsResponseBuilder;
import org.apache.shindig.gadgets.js.JsServingPipeline;
import org.apache.shindig.gadgets.parse.GadgetHtmlParser;
import org.apache.shindig.gadgets.parse.ParseModule;
import org.apache.shindig.gadgets.preload.PreloadException;
import org.apache.shindig.gadgets.preload.PreloadedData;
import org.apache.shindig.gadgets.rewrite.MutableContent;
import org.apache.shindig.gadgets.rewrite.RewritingException;
import org.apache.shindig.gadgets.spec.Feature;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.View;
import org.apache.shindig.gadgets.uri.JsUriManager;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * Tests for RenderingContentRewriter.
 */
public class RenderingGadgetRewriterTest extends EasyMockTestCase{
  private static final Uri SPEC_URL = Uri.parse("http://example.org/gadget.xml");
  private static final String BODY_CONTENT = "Some body content";
  static final Pattern DOCUMENT_SPLIT_PATTERN = Pattern.compile(
      "(.*)<head>(.*?)<\\/head>(?:.*)<body(.*?)>(.*?)<\\/body>(?:.*)", Pattern.DOTALL |
      Pattern.CASE_INSENSITIVE);
  private static final String CUSTOM_DOCTYPE = "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">";
  private static final String CUSTOM_DOCTYPE_QNAME = "html";
  private static final String CUSTOM_DOCTYPE_PUBID = "-//W3C//DTD HTML 4.01 Transitional//EN";
  private static final String CUSTOM_DOCTYPE_SYSID = "http://www.w3.org/TR/html4/loose.dtd";

  static final int BEFORE_HEAD_GROUP = 1;
  static final int HEAD_GROUP = 2;
  static final int BODY_ATTRIBUTES_GROUP = 3;
  static final int BODY_GROUP = 4;

  private final FakeMessageBundleFactory messageBundleFactory = new FakeMessageBundleFactory();
  private final FakeContainerConfig config = new FakeContainerConfig();
  private final JsUriManager jsUriManager = new FakeJsUriManager();
  private final MapGadgetContext context = new MapGadgetContext();
  private final GadgetAdminStore gadgetAdminStore = mock(GadgetAdminStore.class);

  private FeatureRegistry featureRegistry;
  private JsServingPipeline jsServingPipeline;
  private RenderingGadgetRewriter rewriter;
  private GadgetHtmlParser parser;
  private Expressions expressions;

  @Before
  public void setUp() throws Exception {
    expressions = Expressions.forTesting();
    featureRegistry = createMock(FeatureRegistry.class);
    FeatureRegistryProvider featureRegistryProvider = new FeatureRegistryProvider() {
      public FeatureRegistry get(String repository) {
        return featureRegistry;
      }
    };
    jsServingPipeline = createMock(JsServingPipeline.class);
    Map<String, ConfigContributor> configContributors = ImmutableMap.of(
        "core.util", new CoreUtilConfigContributor(featureRegistry,
                gadgetAdminStore),
        "shindig.xhrwrapper", new XhrwrapperConfigContributor()
    );
    rewriter
        = new RenderingGadgetRewriter(messageBundleFactory, expressions, config, featureRegistryProvider,
            jsServingPipeline, jsUriManager,
            new DefaultConfigProcessor(configContributors, config), gadgetAdminStore);
    Injector injector = Guice.createInjector(new ParseModule(), new PropertiesModule());
    parser = injector.getInstance(GadgetHtmlParser.class);
  }

  private Gadget makeGadgetWithSpec(String gadgetXml) throws GadgetException {
    GadgetSpec spec = new GadgetSpec(SPEC_URL, gadgetXml);
    Gadget gadget = new Gadget()
        .setContext(context)
        .setPreloads(ImmutableList.<PreloadedData>of())
        .setSpec(spec)
        .setCurrentView(spec.getView(GadgetSpec.DEFAULT_VIEW))
        .setGadgetFeatureRegistry(featureRegistry);

    // Convenience: by default expect no features requested, by gadget or extern.
    // expectFeatureCalls(...) resets featureRegistry if called again.
    expectFeatureCalls(gadget,
        ImmutableList.<FeatureResource>of(),
        ImmutableSet.<String>of(),
        ImmutableList.<FeatureResource>of());

    //Convenience: by default expect that the gadget is allowed to render
    reset(gadgetAdminStore);
    expect(gadgetAdminStore.checkFeatureAdminInfo(isA(Gadget.class))).andReturn(true);
    expect(gadgetAdminStore.isAllowedFeature(isA(Feature.class), isA(Gadget.class)))
    .andReturn(true).anyTimes();
    replay(gadgetAdminStore);
    return gadget;
  }

  private Gadget makeDefaultGadget() throws GadgetException {
    String defaultXml = "<Module><ModulePrefs title=''/><Content type='html'/></Module>";
    return makeGadgetWithSpec(defaultXml);
  }

  private Gadget makeDefaultOpenSocial2Gadget(boolean useQuirks) throws GadgetException {
    String defaultXml = "<Module specificationVersion='2' ><ModulePrefs " + (useQuirks ? "doctype='quirksmode'" : "") +" title=''/><Content type='html'/></Module>";
    return makeGadgetWithSpec(defaultXml);
  }

  private String rewrite(Gadget gadget, String content) throws Exception {
    MutableContent mc = new MutableContent(parser, content);
    rewriter.rewrite(gadget, mc);
    return mc.getContent();
  }

  @Test
  public void defaultOutput() throws Exception {
    Gadget gadget = makeDefaultGadget();

    String rewritten = rewrite(gadget, BODY_CONTENT);

    Matcher matcher = DOCUMENT_SPLIT_PATTERN.matcher(rewritten);
    assertTrue("Output is not valid HTML.", matcher.matches());
    assertTrue("Missing opening html tag", matcher.group(BEFORE_HEAD_GROUP).
        toLowerCase().contains("<html"));
    assertTrue("Default CSS missing.", matcher.group(HEAD_GROUP).contains(DEFAULT_CSS));
    // Not very accurate -- could have just been user prefs.
    assertTrue("Default javascript not included.",
        matcher.group(HEAD_GROUP).contains("<script>"));
    assertTrue("Original document not preserved.",
        matcher.group(BODY_GROUP).contains(BODY_CONTENT));
    assertTrue("gadgets.util.runOnLoadHandlers not invoked.",
        matcher.group(BODY_GROUP).contains("gadgets.util.runOnLoadHandlers();"));
  }

  @Test
  public void overrideDefaultDoctype() throws Exception{
    Gadget gadget = makeDefaultOpenSocial2Gadget(false);
    String body = "hello, world.";
    String doc = new StringBuilder()
        .append("<html><head>")
        .append("</head><body>")
        .append(body)
        .append("</body></html>")
        .toString();

    rewriter.setDefaultDoctypeQName(CUSTOM_DOCTYPE_QNAME);
    rewriter.setDefaultDoctypePubId(CUSTOM_DOCTYPE_PUBID);
    rewriter.setDefaultDoctypeSysId(CUSTOM_DOCTYPE_SYSID);
    String rewritten = rewrite(gadget, doc);

    Matcher matcher = DOCUMENT_SPLIT_PATTERN.matcher(rewritten);
    assertTrue("Output is not valid HTML.", matcher.matches());
    assertTrue("DOCTYPE not preserved", matcher.group(BEFORE_HEAD_GROUP).contains(CUSTOM_DOCTYPE));

  }

  @Test
  public void quirksmodeInOS2() throws Exception{
    Gadget gadget = makeDefaultOpenSocial2Gadget(true);
    String body = "hello, world.";
    String doc = new StringBuilder()
        .append("<html><head>")
        .append("</head><body>")
        .append(body)
        .append("</body></html>")
        .toString();

    String rewritten = rewrite(gadget, doc);

    Matcher matcher = DOCUMENT_SPLIT_PATTERN.matcher(rewritten);
    assertTrue("Output is not valid HTML.", matcher.matches());
    assertTrue("Should not include doctype, this will default to quirksmode (old Shindig behavior)", !matcher.group(BEFORE_HEAD_GROUP).contains("<!DOCTYPE"));

    gadget = makeDefaultOpenSocial2Gadget(true);
    String docType = "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">";
    doc = new StringBuilder()
        .append(docType)
        .append("<html><head>")
        .append("</head><body>")
        .append(body)
        .append("</body></html>")
        .toString();
    rewritten = rewrite(gadget, doc);

    matcher = DOCUMENT_SPLIT_PATTERN.matcher(rewritten);
    assertTrue("Output is not valid HTML.", matcher.matches());
    assertTrue("Should include doctype, when in quirksmode we should use pre OS2.0 Shindig behavior.", matcher.group(BEFORE_HEAD_GROUP).contains(docType));


  }

  @Test
  public void completeDocument() throws Exception {
    String docType = "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">";
    String head = "<script src=\"foo.js\"></script><style type=\"text/css\">body{color:red;}</style>";
    String bodyAttr = " onload=\"foo();\"";
    String body = "hello, world.";
    String doc = new StringBuilder()
        .append(docType)
        .append("<html><head>")
        .append(head)
        .append("</head><body").append(bodyAttr).append('>')
        .append(body)
        .append("</body></html>")
        .toString();

    GadgetContext context = new GadgetContext() {
      @Override
      public String getParameter(String name) {
        if (name.equals("libs")) {
          return "foo";
        }
        return null;
      }
    };

    Gadget gadget = makeDefaultGadget()
        .setContext(context);

    expectFeatureCalls(gadget,
        ImmutableList.<FeatureResource>of(),
        ImmutableSet.of("foo"),
        ImmutableList.of(inline("blah", "n/a")));

    String rewritten = rewrite(gadget, doc);

    Matcher matcher = DOCUMENT_SPLIT_PATTERN.matcher(rewritten);
    assertTrue("Output is not valid HTML.", matcher.matches());
    assertTrue("DOCTYPE not preserved", matcher.group(BEFORE_HEAD_GROUP).contains(docType));
    assertTrue("Missing opening html tag", matcher.group(BEFORE_HEAD_GROUP).contains("<html"));
    // TODO: reinstate test when non-tag-reordering parser is used.
    // assertTrue("Custom head content is missing.", matcher.group(HEAD_GROUP).contains(head));
    assertTrue("IsGadget beacon not included.",
        matcher.group(HEAD_GROUP).contains("<script>" + IS_GADGET_BEACON + "</script>"));
    assertTrue("Forced javascript not included.",
        matcher.group(HEAD_GROUP).contains("<script src=\"/js/foo?jsload=0\">"));
    assertFalse("Default styling was injected when a doctype was specified.",
        matcher.group(HEAD_GROUP).contains(DEFAULT_CSS));
    assertTrue("Custom body attributes missing.",
        matcher.group(BODY_ATTRIBUTES_GROUP).contains(bodyAttr));
    assertTrue("Original document not preserved.",
        matcher.group(BODY_GROUP).contains(body));
    assertTrue("gadgets.util.runOnLoadHandlers not invoked.",
        matcher.group(BODY_GROUP).contains("gadgets.util.runOnLoadHandlers();"));

    // Skipping other tests; code path should be the same for the rest.
  }

  @Test
  public void completeDocumentOpenSocial2() throws Exception {
    String head = "<script src=\"foo.js\"></script><style type=\"text/css\">body{color:red;}</style>";
    String bodyAttr = " onload=\"foo();\"";
    String body = "hello, world.";
    String doc = new StringBuilder()
        .append("<html><head>")
        .append(head)
        .append("</head><body").append(bodyAttr).append('>')
        .append(body)
        .append("</body></html>")
        .toString();

    GadgetContext context = new GadgetContext() {
      @Override
      public String getParameter(String name) {
        if (name.equals("libs")) {
          return "foo";
        }
        return null;
      }
    };

    Gadget gadget = makeDefaultOpenSocial2Gadget(false)
        .setContext(context);
    expectFeatureCalls(gadget,
        ImmutableList.<FeatureResource>of(),
        ImmutableSet.of("foo"),
        ImmutableList.of(inline("blah", "n/a")));

    String rewritten = rewrite(gadget, doc);

    Matcher matcher = DOCUMENT_SPLIT_PATTERN.matcher(rewritten);
    assertTrue("Output is not valid HTML.", matcher.matches());
    assertTrue("Doctype should have been rewritten to HTML5", matcher.group(BEFORE_HEAD_GROUP).contains("<!DOCTYPE html>"));

    // Skipping other tests; code path should be the same for the rest.
  }

  @Test
  public void bidiSettings() throws Exception {
    String gadgetXml =
      "<Module><ModulePrefs title=''>" +
      "  <Locale language_direction='rtl'/>" +
      "</ModulePrefs>" +
      "<Content type='html'/>" +
      "</Module>";

    Gadget gadget = makeGadgetWithSpec(gadgetXml);

    String rewritten = rewrite(gadget, "");

    assertTrue("Bi-directional locale settings not preserved.",
        rewritten.contains("<body dir=\"rtl\">"));
  }

  private Set<String> getInjectedScript(String content) {
    Pattern featurePattern
        = Pattern.compile("(?:.*)<script src=\"\\/js\\/(.*?)\\?jsload=0\"><\\/script>(?:.*)", Pattern.DOTALL);
    Matcher matcher = featurePattern.matcher(content);

    assertTrue("Forced scripts not injected.", matcher.matches());

    return Sets.newHashSet(matcher.group(1).split(":"));
  }

  @Test
  public void forcedFeaturesInjectedExternal() throws Exception {
    String gadgetXml =
      "<Module><ModulePrefs title=''>" +
      "  <Require feature='foo'/>" +
      "</ModulePrefs>" +
      "<Content type='html'/>" +
      "</Module>";

    final Set<String> libs = ImmutableSortedSet.of("foo", "bar", "baz");
    GadgetContext context = new GadgetContext() {
      @Override
      public String getParameter(String name) {
        if (name.equals("libs")) {
          return Joiner.on(':').join(libs);
        }
        return null;
      }
    };

    Gadget gadget = makeGadgetWithSpec(gadgetXml).setContext(context);

    FeatureResource fooResource = inline("foo-content", "foo-debug");
    expectFeatureCalls(gadget,
        ImmutableList.of(fooResource),
        libs,
        ImmutableList.of(fooResource, inline("bar-c", "bar-d"), inline("baz-c", "baz-d")));

    String rewritten = rewrite(gadget, "");

    Set<String> actual = getInjectedScript(rewritten);
    Set<String> expected = ImmutableSortedSet.of("foo", "bar", "baz");
    assertEquals(expected, actual);
  }

  @Test
  public void inlinedFeaturesWhenNothingForced() throws Exception {
    String gadgetXml =
      "<Module><ModulePrefs title=''>" +
      "  <Require feature='foo'/>" +
      "</ModulePrefs>" +
      "<Content type='html'/>" +
      "</Module>";

    Gadget gadget = makeGadgetWithSpec(gadgetXml);

    expectFeatureCalls(gadget,
        ImmutableList.of(inline("foo_content();", "foo_content_debug();")),
        ImmutableSet.<String>of(),
        ImmutableList.<FeatureResource>of());

    String rewritten = rewrite(gadget, "");

    assertTrue("Requested scripts not inlined.", rewritten.contains("foo_content();"));
  }

  @Test
  public void featuresNotInjectedWhenRemoved() throws Exception {
    String gadgetXml =
      "<Module><ModulePrefs title=''>" +
      "  <Require feature='foo'/>" +
      "</ModulePrefs>" +
      "<Content type='html'/>" +
      "</Module>";

    Gadget gadget = makeGadgetWithSpec(gadgetXml);
    gadget.removeFeature("foo");

    expectFeatureCalls(gadget,
        ImmutableList.<FeatureResource>of(),
        ImmutableSet.<String>of(),
        ImmutableList.<FeatureResource>of());

    String rewritten = rewrite(gadget, "");

    assertFalse("Removed script still inlined.", rewritten.contains("foo_content();"));
  }

  @Test
  public void featuresInjectedWhenAdded() throws Exception {
    String gadgetXml =
      "<Module><ModulePrefs title=''>" +
      "</ModulePrefs>" +
      "<Content type='html'/>" +
      "</Module>";

    Gadget gadget = makeGadgetWithSpec(gadgetXml);
    gadget.addFeature("foo");
    // add non existing feature,
    gadget.addFeature("do-not-exists");

    expectFeatureCalls(gadget,
        ImmutableList.of(inline("foo_content();", "foo_content_dbg();")),
        ImmutableSet.<String>of(),
        ImmutableList.<FeatureResource>of());

    String rewritten = rewrite(gadget, "");

    assertTrue("Added script not inlined.", rewritten.contains("foo_content();"));
  }

  @Test
  public void mixedExternalAndInline() throws Exception {
    String gadgetXml =
      "<Module><ModulePrefs title=''>" +
      "  <Require feature='foo'/>" +
      "</ModulePrefs>" +
      "<Content type='html'/>" +
      "</Module>";

    final Set<String> libs = ImmutableSet.of("bar", "baz");
    GadgetContext context = new GadgetContext() {
      @Override
      public String getParameter(String name) {
        if (name.equals("libs")) {
          return Joiner.on(':').join(libs);
        }
        return null;
      }
    };

    Gadget gadget = makeGadgetWithSpec(gadgetXml).setContext(context);

    expectFeatureCalls(gadget,
        ImmutableList.of(inline("foo_content();", "foo_content_debug();")),
        libs,
        ImmutableList.of(inline("bar-c", "bar-d"), inline("baz-c", "baz-d")));

    String rewritten = rewrite(gadget, "");

    Set<String> actual = getInjectedScript(rewritten);
    Set<String> expected = ImmutableSortedSet.of("bar", "baz");
    assertEquals(expected, actual);
    assertTrue("Requested scripts not inlined.", rewritten.contains("foo_content();"));
  }

  @Test
  public void featuresInjectedBeforeExistingScript() throws Exception {
    Gadget gadget = makeDefaultGadget();

    String rewritten = rewrite(gadget,
        "<html><head><script src=\"foo.js\"></script></head><body>hello</body></html>");

    Matcher matcher = DOCUMENT_SPLIT_PATTERN.matcher(rewritten);
    assertTrue("Output is not valid HTML.", matcher.matches());

    String headContent = matcher.group(HEAD_GROUP);

    // Locate user script.
    int userPosition = headContent.indexOf("<script src=\"foo.js\"></script>");

    // Anything else here, we added.
    int ourPosition = headContent.indexOf("<script>");

    // TODO: restore when moved to a non-tag-shifting HTML parser (userPosition == -1 in body)
    // assertTrue("Injected script must come before user script.", ourPosition < userPosition);
  }

  @Test
  public void featuresDeclaredBeforeUsed() throws Exception {
    String gadgetXml =
      "<Module><ModulePrefs title=''>" +
      "  <Require feature='foo'/>" +
      "</ModulePrefs>" +
      "<Content type='html'/>" +
      "</Module>";

    Gadget gadget = makeGadgetWithSpec(gadgetXml);

    expectFeatureCalls(gadget,
        ImmutableList.of(inline("foo_content();", "foo_content_debug();")),
        ImmutableSet.<String>of(),
        ImmutableList.<FeatureResource>of());

    String rewritten = rewrite(gadget, BODY_CONTENT);

    Matcher matcher = DOCUMENT_SPLIT_PATTERN.matcher(rewritten);
    assertTrue("Output is not valid HTML.", matcher.matches());

    String headContent = matcher.group(HEAD_GROUP);

    // Locate user script.
    int declaredPosition = headContent.indexOf("foo_content();");
    assertTrue(declaredPosition >= 0);

    // Anything else here, we added.
    int usedPosition = headContent.indexOf("gadgets.Prefs.setMessages_");
    assertTrue(usedPosition >= 0);

    assertTrue("Inline JS needs to exist before it is used.", declaredPosition < usedPosition);
  }

  @Test
  public void urlFeaturesForcedExternal() throws Exception {
    String gadgetXml =
      "<Module><ModulePrefs title=''>" +
      "  <Require feature='foo'/>" +
      "  <Require feature='bar'/>" +
      "</ModulePrefs>" +
      "<Content type='html'/>" +
      "</Module>";

    GadgetContext context = new GadgetContext() {
      @Override
      public String getParameter(String name) {
        if (name.equals("libs")) {
          return "baz";
        }
        return null;
      }
    };

    Gadget gadget = makeGadgetWithSpec(gadgetXml).setContext(context);

    expectFeatureCalls(gadget,
        ImmutableList.of(inline("foo_content();", "foo_content_debug();"),
                         extern("http://example.org/external.js", "dbg")),
        ImmutableSet.of("baz"),
        ImmutableList.of(inline("does-not-matter", "dbg")));

    String rewritten = rewrite(gadget, "");

    Set<String> actual = getInjectedScript(rewritten);
    Set<String> expected = ImmutableSortedSet.of("baz");
    assertEquals(expected, actual);
    assertTrue("Requested scripts not inlined.", rewritten.contains("foo_content();"));
    assertTrue("Forced external file not forced.",
        rewritten.contains("<script src=\"http://example.org/external.js\">"));
  }

  @Test(expected = RewritingException.class)
  public void exceptionWhenFeatureNotAllowed() throws Exception {
    Gadget gadget = makeDefaultGadget();
    reset(gadgetAdminStore);
    expect(gadgetAdminStore.checkFeatureAdminInfo(isA(Gadget.class))).andReturn(false);
    replay(gadgetAdminStore);
    rewrite(gadget, BODY_CONTENT);
  }

  @Test
  public void  gadgetAdminDefaultContent() throws Exception {
    String gadgetXml =
      "<Module><ModulePrefs title=''>" +
      "  <Require feature='foo'/>" +
      "</ModulePrefs>" +
      "<Content type='html'/>" +
      "</Module>";

    final Set<String> libs = ImmutableSortedSet.of("foo", "bar", "baz");
    GadgetContext context = new GadgetContext() {
      @Override
      public String getParameter(String name) {
        if (name.equals("libs")) {
          return Joiner.on(':').join(libs);
        }
        return null;
      }
    };

    Gadget gadget = makeGadgetWithSpec(gadgetXml).setContext(context);
    FeatureResource fooResource = inline("foo-content", "foo-debug");
    expectFeatureCalls(gadget,
        ImmutableList.of(fooResource),
        libs,
        ImmutableList.of(fooResource, inline("bar-c", "bar-d"), inline("baz-c", "baz-d")));
    String rewritten = rewrite(gadget, BODY_CONTENT);

    Matcher matcher = DOCUMENT_SPLIT_PATTERN.matcher(rewritten);
    assertTrue("Output is not valid HTML.", matcher.matches());
    assertTrue("Missing opening html tag", matcher.group(BEFORE_HEAD_GROUP).
        toLowerCase().contains("<html"));
    assertTrue("Default CSS missing.", matcher.group(HEAD_GROUP).contains(DEFAULT_CSS));
    // Not very accurate -- could have just been user prefs.
    assertTrue("Default javascript not included.",
        matcher.group(HEAD_GROUP).contains("<script>"));
    assertTrue("Original document not preserved.",
        matcher.group(BODY_GROUP).contains(BODY_CONTENT));
    assertTrue("gadgets.util.runOnLoadHandlers not invoked.",
        matcher.group(BODY_GROUP).contains("gadgets.util.runOnLoadHandlers();"));
  }

  @Test
  public void optionalDeniedFeature() throws Exception {
    String gadgetXml =
      "<Module><ModulePrefs title=''>" +
      "  <Require feature='core.util'/>" +
      "  <Require feature='foo'/>" +
      "  <Optional feature='hello'/>" +
      "</ModulePrefs>" +
      "<Content type='html'/>" +
      "</Module>";

    final Set<String> libs = ImmutableSortedSet.of("core.util", "foo", "bar", "baz");
    GadgetContext context = new GadgetContext() {
      @Override
      public String getParameter(String name) {
        if (name.equals("libs")) {
          return Joiner.on(':').join(libs);
        }
        return null;
      }
    };

    Gadget gadget = makeGadgetWithSpec(gadgetXml).setContext(context);
    reset(gadgetAdminStore);
    Feature denied = mock(Feature.class);
    expect(denied.getName()).andReturn("hello");
    expect(gadgetAdminStore.checkFeatureAdminInfo(isA(Gadget.class))).andReturn(true);
    expect(gadgetAdminStore.isAllowedFeature(eq(denied), isA(Gadget.class))).andReturn(false);
    replay();

    FeatureResource fooResource = inline("foo-content", "foo-debug");
    expectFeatureCalls(gadget,
        ImmutableList.of(fooResource),
        libs,
        ImmutableList.of(fooResource, inline("bar-c", "bar-d"), inline("baz-c", "baz-d")));

    String rewritten = rewrite(gadget, "");
    JSONObject json = getConfigJson(rewritten);

    Set<String> actual = getInjectedScript(rewritten);
    Set<String> expected = ImmutableSortedSet.of("core.util", "foo", "bar", "baz");
    assertFalse(actual.contains("hello"));
    assertEquals(expected, actual);
    assertFalse(json.getJSONObject("core.util").has("hello"));
  }

  private JSONObject getConfigJson(String content) throws JSONException {
    Pattern prefsPattern
        = Pattern.compile("(?:.*)gadgets\\.config\\.init\\((.*)\\);(?:.*)", Pattern.DOTALL);
    Matcher matcher = prefsPattern.matcher(content);
    assertTrue("gadgets.config.init not invoked.", matcher.matches());
    return new JSONObject(matcher.group(1));
  }

  @Test
  public void featureConfigurationInjected() throws Exception {
    String gadgetXml =
      "<Module><ModulePrefs title=''>" +
      "  <Require feature='foo'/>" +
      "</ModulePrefs>" +
      "<Content type='html'/>" +
      "</Module>";

    Gadget gadget = makeGadgetWithSpec(gadgetXml);

    expectFeatureCalls(gadget,
        ImmutableList.of(inline("foo", "dbg")),
        ImmutableSet.<String>of(),
        ImmutableList.<FeatureResource>of());

    config.data.put(FEATURES_KEY, ImmutableMap.of("foo", "blah"));

    String rewritten = rewrite(gadget, "");

    JSONObject json = getConfigJson(rewritten);
    assertEquals("blah", json.get("foo"));
  }

  @Test
  public void featureConfigurationForced() throws Exception {
    String gadgetXml =
      "<Module><ModulePrefs title=''>" +
      "  <Require feature='foo'/>" +
      "</ModulePrefs>" +
      "<Content type='html'/>" +
      "</Module>";

    GadgetContext context = new GadgetContext() {
      @Override
      public String getParameter(String name) {
        if (name.equals("libs")) {
          return "bar";
        }
        return null;
      }
    };

    Gadget gadget = makeGadgetWithSpec(gadgetXml).setContext(context);

    expectFeatureCalls(gadget,
        ImmutableList.of(inline("foo", "foo-dbg")),
        ImmutableSet.of("bar"),
        ImmutableList.of(inline("bar", "bar-dbg")));

    config.data.put(FEATURES_KEY, ImmutableMap.of(
        "foo", "blah",
        "bar", "baz"
    ));

    String rewritten = rewrite(gadget, "");

    JSONObject json = getConfigJson(rewritten);
    assertEquals("blah", json.get("foo"));
    assertEquals("baz", json.get("bar"));
  }

  @Test
  public void gadgetsUtilConfigInjected() throws Exception {
    String gadgetXml =
      "<Module><ModulePrefs title=''>" +
      "  <Require feature='core.util'/>" +
      "  <Require feature='foo'>" +
      "    <Param name='bar'>baz</Param>" +
      "  </Require>" +
      "  <Require feature='foo2'>" +
      "    <Param name='bar'>baz</Param>" +
      "    <Param name='bar'>bop</Param>" +
      "  </Require>" +
      "  <Require feature='unsupported'/>" +
      "</ModulePrefs>" +
      "<Content type='html'/>" +
      "</Module>";

    Gadget gadget = makeGadgetWithSpec(gadgetXml);

    expectFeatureCalls(gadget,
        ImmutableList.of(inline("foo", "foo-dbg"), inline("foo2", "foo2-dbg")),
        ImmutableSet.<String>of(),
        ImmutableList.<FeatureResource>of());

    config.data.put(FEATURES_KEY, ImmutableMap.of("foo", "blah"));

    String rewritten = rewrite(gadget, "");

    JSONObject json = getConfigJson(rewritten);
    assertEquals("blah", json.get("foo"));

    JSONObject util = json.getJSONObject("core.util");
    JSONObject foo = util.getJSONObject("foo");
    assertEquals("baz", foo.get("bar"));
    JSONObject foo2 = util.getJSONObject("foo2");
    JsonAssert.assertObjectEquals(ImmutableList.of("baz", "bop"),
        foo2.get("bar"));

    assertTrue(!util.has("unsupported"));
  }

  // TODO: Test for auth token stuff.

  @Test
  public void userPrefsInitializationInjected() throws Exception {
    String gadgetXml =
      "<Module><ModulePrefs title=''>" +
      "  <Locale>" +
      "    <msg name='one'>foo</msg>" +
      "    <msg name='two'>bar</msg>" +
      "  </Locale>" +
      "</ModulePrefs>" +
      "<UserPref name='pref_one' default_value='default_one'/>" +
      "<UserPref name='pref_two'/>" +
      "<Content type='html'/>" +
      "</Module>";

    Gadget gadget = makeGadgetWithSpec(gadgetXml);

    String rewritten = rewrite(gadget, "");

    Pattern prefsPattern
        = Pattern.compile("(?:.*)gadgets\\.Prefs\\.setMessages_\\((.*)\\);(?:.*)", Pattern.DOTALL);
    Matcher matcher = prefsPattern.matcher(rewritten);
    assertTrue("gadgets.Prefs.setMessages_ not invoked.", matcher.matches());
    JSONObject json = new JSONObject(matcher.group(1));
    assertEquals("foo", json.get("one"));
    assertEquals("bar", json.get("two"));

    Pattern defaultsPattern = Pattern.compile(
        "(?:.*)gadgets\\.Prefs\\.setDefaultPrefs_\\((.*)\\);(?:.*)", Pattern.DOTALL);
    Matcher defaultsMatcher = defaultsPattern.matcher(rewritten);
    assertTrue("gadgets.Prefs.setDefaultPrefs_ not invoked.", defaultsMatcher.matches());
    JSONObject defaultsJson = new JSONObject(defaultsMatcher.group(1));
    assertEquals(2, defaultsJson.length());
    assertEquals("default_one", defaultsJson.get("pref_one"));
    assertEquals("", defaultsJson.get("pref_two"));
  }

  @Test
  public void xhrWrapperConfigurationInjected() throws Exception {
    checkXhrWrapperConfigurationInjection(
        "No shindig.xhrwrapper configuration present in rewritten HTML.", null, null, null);

    checkXhrWrapperConfigurationInjection(
        "No shindig.xhrwrapper.authorization=signed configuration present in rewritten HTML.",
        "signed", null, null);

    checkXhrWrapperConfigurationInjection(
        "No shindig.xhrwrapper.oauthService configuration present in rewritten HTML.",
        "oauth", "serviceName", null);

    checkXhrWrapperConfigurationInjection(
        "No shindig.xhrwrapper.oauthTokenName configuration present in rewritten HTML.",
        "oauth", "serviceName", "tokenName");
  }

  private void checkXhrWrapperConfigurationInjection(String message, String auth, String oauthService, String oauthToken)
      throws Exception {
    String oAuthBlock = "";
    String authzAttr = "";
    if (auth != null) {
      authzAttr = " authz='" + auth + '\'';
      if ("oauth".equals(auth)) {
        if (oauthService != null) {
          oAuthBlock =
              "<OAuth><Service name='" + oauthService + "'>" +
              "<Access url='http://foo' method='GET' />" +
              "<Request url='http://bar' method='GET' />" +
              "<Authorization url='http://baz' />" +
              "</Service></OAuth>";
          authzAttr += " oauth_service_name='" + oauthService + '\'';
        }
        if (oauthToken != null) {
          authzAttr += " oauth_token_name='" + oauthToken + '\'';
        }
      }
    }

    String gadgetXml =
      "<Module><ModulePrefs title=''>" +
      "  <Require feature='shindig.xhrwrapper' />" +
      oAuthBlock +
      "</ModulePrefs>" +
      "<Content type='html' href='http://foo.com/bar/baz.html'" + authzAttr + " />" +
      "</Module>";

    String expected = '{' +
        (oauthService == null ? "" : "\"oauthService\":\"serviceName\",") +
        "\"contentUrl\":\"http://foo.com/bar/baz.html\"" +
        (auth == null ? "" : ",\"authorization\":\"" + auth + '\"') +
        (oauthToken == null ? "" : ",\"oauthTokenName\":\"tokenName\"") +
        '}';

    Gadget gadget = makeGadgetWithSpec(gadgetXml);
    gadget.setCurrentView(gadget.getSpec().getView("default"));
    String rewritten = rewrite(gadget, BODY_CONTENT);

    assertXhrConfigContains(message, expected, rewritten);
  }

  private void assertXhrConfigContains(String message, String expected, String content) throws Exception {
    // TODO: make this test a little more robust. This check ensures that ordering is not taken
    // into account during config comparison.
    String prefix = "gadgets.config.init(";
    int configIdx = content.indexOf(prefix);
    assertTrue("gadgets.config.init not found in rewritten content", configIdx != -1);
    int endIdx = content.indexOf(')', configIdx + prefix.length());
    assertTrue("unexpected error, gadgets.config.init not closed", endIdx != -1);
    String configJson = content.substring(configIdx + prefix.length(), endIdx);
    JSONObject config = new JSONObject(configJson);
    JSONObject xhrConfig = config.getJSONObject("shindig.xhrwrapper");
    JSONObject expectedJson = new JSONObject(expected);
    JsonAssert.assertJsonObjectEquals(xhrConfig, expectedJson);
  }

  @Test
  public void xhrWrapperConfigurationNotInjectedIfUnnecessary() throws Exception {
    String gadgetXml =
      "<Module><ModulePrefs title='' />" +
      "<Content type='html' href='http://foo.com/bar/baz.html' />" +
      "</Module>";

    Gadget gadget = makeGadgetWithSpec(gadgetXml);
    gadget.setCurrentView(gadget.getSpec().getView("default"));

    String rewritten = rewrite(gadget, BODY_CONTENT);

    boolean containsConfig = rewritten.contains("\"shindig.xhrwrapper\"");
    assertFalse("shindig.xhrwrapper configuration present in rewritten HTML.", containsConfig);
  }

  @Test(expected = RewritingException.class)
  public void unsupportedFeatureThrows() throws Exception {
    String gadgetXml =
      "<Module><ModulePrefs title=''>" +
      "  <Require feature='foo'/>" +
      "</ModulePrefs>" +
      "<Content type='html'/>" +
      "</Module>";

    Gadget gadget = makeGadgetWithSpec(gadgetXml);

    reset(featureRegistry);
    FeatureRegistry.LookupResult lr = createMock(FeatureRegistry.LookupResult.class);
    expect(lr.getResources()).andReturn(ImmutableList.<FeatureResource>of());
    replay(lr);
    expect(featureRegistry.getFeatureResources(same(gadget.getContext()),
        eq(ImmutableSet.<String>of()), eq(Lists.<String>newLinkedList())))
        .andReturn(lr);
    final FeatureRegistry.LookupResult lr2 = createMock(FeatureRegistry.LookupResult.class);
    expect(lr2.getResources()).andReturn(ImmutableList.<FeatureResource>of());
    replay(lr2);
    assertTrue(gadget.getDirectFeatureDeps().contains("core"));
    assertTrue(gadget.getDirectFeatureDeps().contains("foo"));
    assertEquals(gadget.getDirectFeatureDeps().size(),2);
    expect(featureRegistry.getFeatureResources(same(gadget.getContext()),
        eq(Lists.newLinkedList(gadget.getDirectFeatureDeps())), eq(Lists.<String>newLinkedList())))
        .andAnswer(new IAnswer<FeatureRegistry.LookupResult>() {
          @SuppressWarnings("unchecked")
          public FeatureRegistry.LookupResult answer() throws Throwable {
            List<String> unsupported = (List<String>)getCurrentArguments()[2];
            unsupported.add("foo");
            return lr2;
          }
        });
    replay(featureRegistry);

    rewrite(gadget, "");
  }

  @Test(expected = RewritingException.class)
  public void unsupportedViewFeatureThrows() throws Exception {
    String gadgetXml =
      "<Module><ModulePrefs title=''>" +
      "  <Require feature='foo' views='default'/>" +
      "</ModulePrefs>" +
      "<Content view='default' type='html'/>" +
      "</Module>";

    Gadget gadget = makeGadgetWithSpec(gadgetXml);

    reset(featureRegistry);
    FeatureRegistry.LookupResult lr = createMock(FeatureRegistry.LookupResult.class);
    expect(lr.getResources()).andReturn(ImmutableList.<FeatureResource>of());
    replay(lr);
    expect(featureRegistry.getFeatureResources(same(gadget.getContext()),
        eq(ImmutableSet.<String>of()), eq(Lists.<String>newLinkedList())))
        .andReturn(lr);
    final FeatureRegistry.LookupResult lr2 = createMock(FeatureRegistry.LookupResult.class);
    expect(lr2.getResources()).andReturn(ImmutableList.<FeatureResource>of());
    replay(lr2);
    assertTrue(gadget.getDirectFeatureDeps().contains("core"));
    assertTrue(gadget.getDirectFeatureDeps().contains("foo"));
    assertEquals(gadget.getDirectFeatureDeps().size(),2);
    Lists.newLinkedList();
    expect(featureRegistry.getFeatureResources(same(gadget.getContext()),
        eq(Lists.newLinkedList(gadget.getDirectFeatureDeps())), eq(Lists.<String>newLinkedList())))
        .andAnswer(new IAnswer<FeatureRegistry.LookupResult>() {
          @SuppressWarnings("unchecked")
          public FeatureRegistry.LookupResult answer() throws Throwable {
            List<String> unsupported = (List<String>)getCurrentArguments()[2];
            unsupported.add("foo");
            return lr2;
          }
        });
    replay(featureRegistry);

    rewrite(gadget, "");
  }

  @Test
  public void unsupportedExternFeatureDoesNotThrow() throws Exception {
    String gadgetXml =
      "<Module><ModulePrefs title=''>" +
      "</ModulePrefs>" +
      "<Content type='html'/>" +
      "</Module>";

    GadgetContext context = new GadgetContext() {
      @Override
      public String getParameter(String name) {
        if (name.equals("libs")) {
          return "bar";
        }
        return null;
      }
    };

    Gadget gadget = makeGadgetWithSpec(gadgetXml).setContext(context);

    reset(featureRegistry);
    final FeatureRegistry.LookupResult lr = createMock(FeatureRegistry.LookupResult.class);
    expect(lr.getResources()).andReturn(ImmutableList.<FeatureResource>of()).anyTimes();
    replay(lr);
    expect(featureRegistry.getFeatureResources(same(gadget.getContext()),
        eq(ImmutableSet.<String>of("bar")), eq(Lists.<String>newArrayList())))
        .andAnswer(new IAnswer<FeatureRegistry.LookupResult>() {
          @SuppressWarnings("unchecked")
          public FeatureRegistry.LookupResult answer() throws Throwable {
            List<String> unsupported = (List<String>)getCurrentArguments()[2];
            unsupported.add("bar");
            return lr;
          }
        });
    expect(featureRegistry.getFeatureResources(same(gadget.getContext()),
        eq(ImmutableList.<String>of("core")), eq(Lists.<String>newArrayList())))
        .andReturn(lr);
    expect(featureRegistry.getFeatures(eq(ImmutableSet.of("core", "bar"))))
        .andReturn(ImmutableList.of("core"));
    expect(featureRegistry.getFeatures(eq(ImmutableList.of("core"))))
        .andReturn(ImmutableList.of("core"));
    replay(featureRegistry);

    rewrite(gadget, "");
  }

  @Test
  public void unsupportedOptionalFeatureDoesNotThrow() throws Exception {
    String gadgetXml =
      "<Module><ModulePrefs title=''>" +
      "  <Optional feature='foo'/>" +
      "</ModulePrefs>" +
      "<Content type='html'/>" +
      "</Module>";

    Gadget gadget = makeGadgetWithSpec(gadgetXml);

    rewrite(gadget, "");
    // rewrite will throw if the optional unsupported feature doesn't work.
  }

  @Test
  public void multipleUnsupportedOptionalFeaturesDoNotThrow() throws Exception {
    String gadgetXml =
      "<Module><ModulePrefs title=''>" +
      "  <Optional feature='foo'/>" +
      "  <Optional feature='bar'/>" +
      "</ModulePrefs>" +
      "<Content type='html'/>" +
      "</Module>";

    Gadget gadget = makeGadgetWithSpec(gadgetXml);

    rewrite(gadget, "");
    // rewrite will throw if the optional unsupported feature doesn't work.
  }

  @Test
  public void unsupportedViewFeaturesDoNotThrow() throws Exception {
    String gadgetXml =
      "<Module><ModulePrefs title=''>" +
      "  <Optional feature='foo'/>" +
      "  <Optional feature='bar'/>" +
      "  <Require feature='bar2' views='view1'/>" +
      "  <Optional feature='bar3' views='view1'/>" +
      "</ModulePrefs>" +
      "<Content type='html'/>" +
      "</Module>";

    Gadget gadget = makeGadgetWithSpec(gadgetXml);

    rewrite(gadget, "");
    // rewrite will throw if the optional unsupported feature doesn't work.
  }

  private JSONArray getPreloadedJson(String content) throws JSONException {
    Pattern preloadPattern
        = Pattern.compile("(?:.*)gadgets\\.io\\.preloaded_=\\[(.*?)\\];(?:.*)", Pattern.DOTALL);
    Matcher matcher = preloadPattern.matcher(content);
    assertTrue("gadgets.io.preloaded not set.", matcher.matches());
    return new JSONArray('[' + matcher.group(1) + ']');
  }

  @Test
  public void preloadsInjected() throws Exception {
    final Collection<Object> someData = ImmutableList.of("string", (Object) 99, 4343434.345345d);

    // Other types are supported (anything valid for org.json.JSONObject), but equality comparisons
    // are more complicated because JSON doesn't implement interfaces like Collection or Map, or
    // implementing equals.
    PreloadedData preloadedData = new PreloadedData() {
      public Collection<Object> toJson() {
        return someData;
      }
    };
    Gadget gadget = makeDefaultGadget().setPreloads(ImmutableList.of(preloadedData));

    String rewritten = rewrite(gadget, "");

    JSONArray json = getPreloadedJson(rewritten);
    int i = 0;
    for (Object entry : someData) {
      assertEquals(entry, json.get(i++));
    }
  }

  @Test
  public void failedPreloadHandledGracefully() throws Exception {
    PreloadedData preloadedData = new PreloadedData() {
      public Collection<Object> toJson() throws PreloadException {
        throw new PreloadException("test");
      }
    };

    Gadget gadget = makeDefaultGadget().setPreloads(ImmutableList.of(preloadedData));
    String rewritten = rewrite(gadget, "");

    JSONArray json = getPreloadedJson(rewritten);

    assertEquals(0, json.length());
  }

  private String getBaseElement(String content) {
    Matcher matcher = DOCUMENT_SPLIT_PATTERN.matcher(content);
    assertTrue("Output is not valid HTML.", matcher.matches());
    Pattern baseElementPattern
        = Pattern.compile("^<base href=\"(.*?)\">(?:.*)", Pattern.DOTALL);
    Matcher baseElementMatcher = baseElementPattern.matcher(matcher.group(HEAD_GROUP));
    assertTrue("Base element does not exist at the beginning of the head element.",
        baseElementMatcher.matches());
    return baseElementMatcher.group(1);
  }

  @Test
  public void baseElementInsertedWhenContentIsInline() throws Exception {
    Gadget gadget = makeDefaultGadget();

    config.data.put(INSERT_BASE_ELEMENT_KEY, true);

    String rewritten = rewrite(gadget, BODY_CONTENT);
    String base = getBaseElement(rewritten);

    assertEquals(SPEC_URL.toString(), base);
  }

  @Test
  public void baseElementInsertedWhenContentIsProxied() throws Exception {
    Gadget gadget = makeDefaultGadget();

    String viewUrl = "http://example.org/view.html";
    String xml = "<Content href='" + viewUrl + "'/>";
    View fakeView = new View("foo", Arrays.asList(XmlUtil.parse(xml)), SPEC_URL);
    gadget.setCurrentView(fakeView);

    config.data.put(INSERT_BASE_ELEMENT_KEY, true);

    String rewritten = rewrite(gadget, BODY_CONTENT);
    String base = getBaseElement(rewritten);

    assertEquals(viewUrl, base);
  }

  @Test
  public void baseElementNotInsertedWhenConfigDoesNotAllowIt() throws Exception {
    Gadget gadget = makeDefaultGadget();

    config.data.put(INSERT_BASE_ELEMENT_KEY, false);

    String rewritten = rewrite(gadget, BODY_CONTENT);
    assertFalse("Base element injected incorrectly.", rewritten.contains("<base"));
  }

  @Test
  public void doesNotRewriteWhenSanitizeEquals1() throws Exception {
    Gadget gadget = makeDefaultGadget();

    context.params.put("sanitize", "1");

    assertEquals(BODY_CONTENT, rewrite(gadget, BODY_CONTENT));
  }

  @Test
  public void doesRewriteWhenSanitizeEquals0() throws Exception {
    Gadget gadget = makeDefaultGadget();

    context.params.put("sanitize", "0");

    assertFalse("Didn't rewrite when sanitize was '0'.",
        BODY_CONTENT.equals(rewrite(gadget, BODY_CONTENT)));
  }

  private List<String> getAllRequiredFeatures(Gadget gadget) {
    List<String> names = Lists.newArrayList();
    List<Feature> features = gadget.getSpec().getModulePrefs().getAllFeatures();
    for(Feature feature : features) {
      if(feature.getRequired()) {
        names.add(feature.getName());
      }
    }
    return names;
  }

  private void expectFeatureCalls(Gadget gadget,
                                  List<FeatureResource> gadgetResources,
                                  Set<String> externLibs,
                                  List<FeatureResource> externResources) {
    reset(featureRegistry);
    GadgetContext gadgetContext = gadget.getContext();
    List<String> gadgetFeatures = Lists.newLinkedList(gadget.getDirectFeatureDeps());
    List<String> allFeatures = Lists.newLinkedList(gadgetFeatures);
    List<String> allFeaturesAndLibs = Lists.newLinkedList(gadgetFeatures);
    allFeaturesAndLibs.addAll(externLibs);
    List<String> allRequiredFeatures = Lists.newLinkedList(getAllRequiredFeatures(gadget));
    List<String> allRequiredFeatuesAndLibs = Lists.newLinkedList(allRequiredFeatures);
    allRequiredFeatuesAndLibs.addAll(externLibs);
    List<String> emptyList = Lists.newLinkedList();
    final FeatureRegistry.LookupResult externLr = createMock(FeatureRegistry.LookupResult.class);
    expect(externLr.getResources()).andReturn(externResources);
    replay(externLr);
    final FeatureRegistry.LookupResult gadgetLr = createMock(FeatureRegistry.LookupResult.class);
    expect(gadgetLr.getResources()).andReturn(gadgetResources);
    replay(gadgetLr);
    expect(featureRegistry.getFeatureResources(same(gadgetContext), eq(externLibs), eq(emptyList)))
        .andReturn(externLr);
    expect(featureRegistry.getFeatureResources(same(gadgetContext), eq(gadgetFeatures),
        eq(emptyList))).andReturn(gadgetLr);
    expect(featureRegistry.getFeatures(eq(allFeatures)))
        .andReturn(allFeatures).anyTimes();
    expect(featureRegistry.getFeatures(eq(Sets.newHashSet(allFeaturesAndLibs))))
        .andReturn(allFeaturesAndLibs);
    expect(featureRegistry.getFeatures(eq(ImmutableSet.of("*"))))
    .andReturn(ImmutableList.<String>of()).anyTimes();
    expect(featureRegistry.getFeatures(eq(ImmutableSet.of("hello"))))
    .andReturn(ImmutableList.<String>of("hello")).anyTimes();
    if(!allRequiredFeatures.equals(allFeatures)) {
      expect(featureRegistry.getFeatures(eq(allRequiredFeatures)))
      .andReturn(allRequiredFeatuesAndLibs).anyTimes();
      expect(featureRegistry.getFeatures(eq(Sets.newHashSet(allRequiredFeatuesAndLibs))))
      .andReturn(allRequiredFeatuesAndLibs).anyTimes();
    }
    // Add CoreUtilConfigContributor behavior
    expect(featureRegistry.getAllFeatureNames()).
        andReturn(ImmutableSet.of("foo", "foo2", "core.util")).anyTimes();
    replay(featureRegistry);

    JsResponseBuilder builder = new JsResponseBuilder();
    for (FeatureResource r :  gadgetResources) {
      if (r.isExternal()) {
        builder.appendJs("<script src=\"" + r.getContent() + "\">", r.getName());
      } else {
        builder.appendJs(r.getContent(), r.getName());
      }
    }
    reset(jsServingPipeline);
    try {
      expect(jsServingPipeline.execute(EasyMock.<JsRequest>anyObject())).andReturn(builder.build());
    } catch (JsException e) {
      throw new RuntimeException("Should not fail here");
    }
    replay(jsServingPipeline);
  }

  private FeatureResource inline(String content, String debugContent) {
    return new FeatureResource.Simple(content, debugContent, "js");
  }

  private FeatureResource extern(String content, String debugContent) {
    return new FeatureResource.Simple(content, debugContent, "js") {
      @Override
      public boolean isExternal() {
        return true;
      }
    };
  }

  public static class MapGadgetContext extends GadgetContext {
    protected final Map<String, String> params = Maps.newHashMap();

    @Override
    public String getParameter(String name) {
      return params.get(name);
    }
  }

  private static class FakeContainerConfig extends BasicContainerConfig {
    protected final Map<String, Object> data = Maps.newHashMap();

    @Override
    public Object getProperty(String container, String name) {
      return data.get(name);
    }
  }

  private static class FakeJsUriManager implements JsUriManager {
    public Uri makeExternJsUri(JsUri ctx) {
      return Uri.parse("/js/" + Joiner.on(':').join(ctx.getLibs()));
    }

    public JsUri processExternJsUri(Uri uri) {
      throw new UnsupportedOperationException();
    }
  }
}
