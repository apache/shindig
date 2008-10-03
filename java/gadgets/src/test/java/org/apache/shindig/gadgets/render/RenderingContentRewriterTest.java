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

import static org.apache.shindig.gadgets.render.RenderingContentRewriter.BEFORE_HEAD_GROUP;
import static org.apache.shindig.gadgets.render.RenderingContentRewriter.BODY_ATTRIBUTES_GROUP;
import static org.apache.shindig.gadgets.render.RenderingContentRewriter.BODY_GROUP;
import static org.apache.shindig.gadgets.render.RenderingContentRewriter.DEFAULT_HEAD_CONTENT;
import static org.apache.shindig.gadgets.render.RenderingContentRewriter.DOCUMENT_SPLIT_PATTERN;
import static org.apache.shindig.gadgets.render.RenderingContentRewriter.HEAD_GROUP;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.shindig.common.ContainerConfig;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.xml.XmlUtil;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.GadgetFeature;
import org.apache.shindig.gadgets.GadgetFeatureRegistry;
import org.apache.shindig.gadgets.JsLibrary;
import org.apache.shindig.gadgets.MessageBundleFactory;
import org.apache.shindig.gadgets.UrlGenerator;
import org.apache.shindig.gadgets.preload.NullPreloads;
import org.apache.shindig.gadgets.preload.PreloadException;
import org.apache.shindig.gadgets.preload.PreloadedData;
import org.apache.shindig.gadgets.preload.Preloads;
import org.apache.shindig.gadgets.rewrite.MutableContent;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.LocaleSpec;
import org.apache.shindig.gadgets.spec.MessageBundle;
import org.apache.shindig.gadgets.spec.View;

import com.google.caja.util.Join;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tests for RenderingContentRewriter.
 */
public class RenderingContentRewriterTest {
  private static final Uri SPEC_URL = Uri.parse("http://example.org/gadget.xml");
  private static final String BODY_CONTENT = "Some body content";
  private final IMocksControl control = EasyMock.createNiceControl();
  private final FakeMessageBundleFactory messageBundleFactory = new FakeMessageBundleFactory();
  private final ContainerConfig config = control.createMock(ContainerConfig.class);
  private final UrlGenerator urlGenerator = new FakeUrlGenerator();

  private FakeGadgetFeatureRegistry featureRegistry;
  private RenderingContentRewriter rewriter;

  @Before
  public void setUp() throws Exception {
    featureRegistry = new FakeGadgetFeatureRegistry();
    rewriter
        = new RenderingContentRewriter(messageBundleFactory, config, featureRegistry, urlGenerator);
  }

  private Gadget makeGadgetWithSpec(String gadgetXml) throws GadgetException {
    GadgetSpec spec = new GadgetSpec(SPEC_URL, gadgetXml);
    return new Gadget()
        .setContext(new GadgetContext())
        .setPreloads(new NullPreloads())
        .setSpec(spec);
  }

  private Gadget makeDefaultGadget() throws GadgetException {
    String defaultXml = "<Module><ModulePrefs title=''/><Content type='html'/></Module>";
    return makeGadgetWithSpec(defaultXml);
  }

  private String rewrite(Gadget gadget, String content) {
    MutableContent mc = new MutableContent(null);
    mc.setContent(content);
    assertEquals(0, rewriter.rewrite(gadget, mc).getCacheTtl());
    return mc.getContent();
  }

  @Test
  public void defaultOutput() throws Exception {
    Gadget gadget = makeDefaultGadget();

    control.replay();

    String rewritten = rewrite(gadget, BODY_CONTENT);

    Matcher matcher = DOCUMENT_SPLIT_PATTERN.matcher(rewritten);
    assertTrue("Output is not valid HTML.", matcher.matches());
    assertTrue("Missing opening html tag", matcher.group(BEFORE_HEAD_GROUP).contains("<html>"));
    assertTrue("Default head content is missing.",
        matcher.group(HEAD_GROUP).contains(DEFAULT_HEAD_CONTENT));
    // Not very accurate -- could have just been user prefs.
    assertTrue("Default javascript not included.",
        matcher.group(HEAD_GROUP).contains("<script>"));
    assertTrue("Original document not preserved.",
        matcher.group(BODY_GROUP).contains(BODY_CONTENT));
    assertTrue("gadgets.util.runOnLoadHandlers not invoked.",
        matcher.group(BODY_GROUP).contains("gadgets.util.runOnLoadHandlers();"));
  }

  @Test
  public void completeDocument() throws Exception {
    String docType = "<![DOCTYPE html]>";
    String head = "<script src='foo.js'></script><style type='text/css'>body{color:red;}</style>";
    String bodyAttr = " onload='foo();'";
    String body = "hello, world.";
    String doc = new StringBuilder()
        .append(docType)
        .append("<html><head>")
        .append(head)
        .append("</head><body").append(bodyAttr).append(">")
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

    featureRegistry.addInline("foo", "does-not-matter");
    control.replay();

    String rewritten = rewrite(gadget, doc);

    Matcher matcher = DOCUMENT_SPLIT_PATTERN.matcher(rewritten);
    assertTrue("Output is not valid HTML.", matcher.matches());
    assertTrue("DOCTYPE not preserved", matcher.group(BEFORE_HEAD_GROUP).contains(docType));
    assertTrue("Missing opening html tag", matcher.group(BEFORE_HEAD_GROUP).contains("<html>"));
    assertTrue("Custom head content is missing.", matcher.group(HEAD_GROUP).contains(head));
    assertTrue("Forced javascript not included.",
        matcher.group(HEAD_GROUP).contains("<script src=\"/js/foo\">"));
    assertTrue("Custom body attributes missing.",
        matcher.group(BODY_ATTRIBUTES_GROUP).contains(bodyAttr));
    assertTrue("Original document not preserved.",
        matcher.group(BODY_GROUP).contains(body));
    assertTrue("gadgets.util.runOnLoadHandlers not invoked.",
        matcher.group(BODY_GROUP).contains("gadgets.util.runOnLoadHandlers();"));

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

    control.replay();

    String rewritten = rewrite(gadget, "");

    assertTrue("Bi-directional locale settings not preserved.",
        rewritten.contains("<body dir='rtl'>"));
  }

  private Set<String> getInjectedScript(String content) {
    Pattern featurePattern
        = Pattern.compile("(?:.*)<script src=\"\\/js\\/(.*?)\"><\\/script>(?:.*)", Pattern.DOTALL);
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

    final Collection<String> libs = Arrays.asList("foo", "bar", "baz");
    GadgetContext context = new GadgetContext() {
      @Override
      public String getParameter(String name) {
        if (name.equals("libs")) {
          return Join.join(":", libs);
        }
        return null;
      }
    };

    Gadget gadget = makeGadgetWithSpec(gadgetXml).setContext(context);

    featureRegistry.addInline("foo", "does-not-matter");
    featureRegistry.addInline("bar", "does-not-matter");
    featureRegistry.addInline("baz", "does-not-matter");
    control.replay();

    String rewritten = rewrite(gadget, "");

    Set<String> actual = getInjectedScript(rewritten);
    Set<String> expected = Sets.immutableSortedSet("foo", "bar", "baz");
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

    featureRegistry.addInline("foo", "foo_content();");
    control.replay();

    String rewritten = rewrite(gadget, "");

    assertTrue("Requested scripts not inlined.", rewritten.contains("foo_content();"));
  }

  @Test
  public void mixedExternalAndInline() throws Exception {
    String gadgetXml =
      "<Module><ModulePrefs title=''>" +
      "  <Require feature='foo'/>" +
      "</ModulePrefs>" +
      "<Content type='html'/>" +
      "</Module>";

    final Collection<String> libs = Arrays.asList("bar", "baz");
    GadgetContext context = new GadgetContext() {
      @Override
      public String getParameter(String name) {
        if (name.equals("libs")) {
          return Join.join(":", libs);
        }
        return null;
      }
    };

    Gadget gadget = makeGadgetWithSpec(gadgetXml).setContext(context);

    featureRegistry.addInline("foo", "foo_content();");
    featureRegistry.addInline("bar", "does-not-matter");
    featureRegistry.addInline("baz", "does-not-matter");
    control.replay();

    String rewritten = rewrite(gadget, "");

    Set<String> actual = getInjectedScript(rewritten);
    Set<String> expected = Sets.immutableSortedSet("bar", "baz");
    assertEquals(expected, actual);
    assertTrue("Requested scripts not inlined.", rewritten.contains("foo_content();"));
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

    featureRegistry.addInline("foo", "foo_content();");
    featureRegistry.addExternal("bar", "http://example.org/external.js");
    featureRegistry.addInline("baz", "does-not-matter");
    control.replay();

    String rewritten = rewrite(gadget, "");

    Set<String> actual = getInjectedScript(rewritten);
    Set<String> expected = Sets.immutableSortedSet("baz");
    assertEquals(expected, actual);
    assertTrue("Requested scripts not inlined.", rewritten.contains("foo_content();"));
    assertTrue("Forced external file not forced.",
        rewritten.contains("<script src=\"http://example.org/external.js\">"));
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

    featureRegistry.addInline("foo", "");

    JSONObject conf = new JSONObject();
    conf.put("foo", "blah");
    expect(config.getJsonObject("default", "gadgets.features")).andReturn(conf);
    control.replay();

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

    featureRegistry.addInline("foo", "");
    featureRegistry.addInline("bar", "");
    JSONObject conf = new JSONObject();
    conf.put("foo", "blah")
        .put("bar", "baz");
    expect(config.getJsonObject("default", "gadgets.features")).andReturn(conf);
    control.replay();

    String rewritten = rewrite(gadget, "");

    JSONObject json = getConfigJson(rewritten);
    assertEquals("blah", json.get("foo"));
    assertEquals("baz", json.get("bar"));
  }

  @Test
  public void gadgetsUtilConfigInjected() throws Exception {
    String gadgetXml =
      "<Module><ModulePrefs title=''>" +
      "  <Require feature='foo'>" +
      "    <Param name='bar'>baz</Param>" +
      "  </Require>" +
      "</ModulePrefs>" +
      "<Content type='html'/>" +
      "</Module>";

    Gadget gadget = makeGadgetWithSpec(gadgetXml);

    featureRegistry.addInline("foo", "");
    JSONObject conf = new JSONObject();
    conf.put("foo", "blah");
    expect(config.getJsonObject("default", "gadgets.features")).andReturn(conf);
    control.replay();

    String rewritten = rewrite(gadget, "");

    JSONObject json = getConfigJson(rewritten);
    assertEquals("blah", json.get("foo"));

    JSONObject util = json.getJSONObject("core.util");
    JSONObject foo = util.getJSONObject("foo");
    assertEquals("baz", foo.get("bar"));
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
      "<Content type='html'/>" +
      "</Module>";

    Gadget gadget = makeGadgetWithSpec(gadgetXml);

    control.replay();

    String rewritten = rewrite(gadget, "");

    Pattern prefsPattern
        = Pattern.compile("(?:.*)gadgets\\.Prefs\\.setMessages_\\((.*)\\);(?:.*)", Pattern.DOTALL);
    Matcher matcher = prefsPattern.matcher(rewritten);
    assertTrue("gadgets.Prefs.setMessages_ not invoked.", matcher.matches());
    JSONObject json = new JSONObject(matcher.group(1));
    assertEquals("foo", json.get("one"));
    assertEquals("bar", json.get("two"));
  }

  @Test(expected = RuntimeException.class)
  public void unsupportedFeatureThrows() throws Exception {
    String gadgetXml =
      "<Module><ModulePrefs title=''>" +
      "  <Require feature='foo'/>" +
      "</ModulePrefs>" +
      "<Content type='html'/>" +
      "</Module>";

    Gadget gadget = makeGadgetWithSpec(gadgetXml);

    control.replay();

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

    control.replay();

    rewrite(gadget, "");
    // rewrite will throw if the optional unsupported feature doesn't work.
  }

  private JSONObject getPreloadedJson(String content) throws JSONException {
    Pattern preloadPattern
        = Pattern.compile("(?:.*)gadgets\\.io\\.preloaded_=\\{(.*?)\\};(?:.*)", Pattern.DOTALL);
    Matcher matcher = preloadPattern.matcher(content);
    assertTrue("gadgets.io.preloaded not set.", matcher.matches());
    return new JSONObject('{' + matcher.group(1) + '}');
  }

  @Test
  public void preloadsInjected() throws Exception {
    final Map<String, Object> preloadData = Maps.newHashMap();

    // We want a variety of data.
    preloadData.put("string", "string");
    preloadData.put("integer", 99);
    preloadData.put("double", 4343434.345345d);

    // Other types are supported (anything valid for org.json.JSONObject), but equality comparisons
    // are more complicated because JSON doesn't implement interfaces like Collection or Map, or
    // implementing equals.

    Preloads preloads = new Preloads() {

      public PreloadedData getData(final String key) {
        return new PreloadedData() {
          public Object toJson() {
            return preloadData.get(key);
          }
        };
      }

      public Set<String> getKeys() {
        return preloadData.keySet();
      }
    };

    Gadget gadget = makeDefaultGadget().setPreloads(preloads);
    control.replay();

    String rewritten = rewrite(gadget, "");

    JSONObject json = getPreloadedJson(rewritten);
    for (Map.Entry<String, Object> entry : preloadData.entrySet()) {
      assertEquals(entry.getValue(), json.get(entry.getKey()));
    }
  }

  @Test
  public void failedPreloadHandledGracefully() throws Exception {
    Preloads preloads = new Preloads() {
      public PreloadedData getData(final String key) throws PreloadException {
        throw new PreloadException("broken");
      }
      public Set<String> getKeys() {
        return Sets.immutableSortedSet("foo");
      }
    };

    Gadget gadget = makeDefaultGadget().setPreloads(preloads);
    control.replay();

    String rewritten = rewrite(gadget, "");

    JSONObject json = getPreloadedJson(rewritten);

    assertEquals(0, json.length());
  }

  private String getBaseElement(String content) {
    Matcher matcher = DOCUMENT_SPLIT_PATTERN.matcher(content);
    assertTrue("Output is not valid HTML.", matcher.matches());
    Pattern baseElementPattern
        = Pattern.compile("(?:.*)<base href='(.*?)'\\/>(?:.*)", Pattern.DOTALL);
    Matcher baseElementMatcher = baseElementPattern.matcher(matcher.group(HEAD_GROUP));
    assertTrue("base element missing from head of document.", baseElementMatcher.matches());
    return baseElementMatcher.group(1);
  }

  @Test
  public void baseElementInsertedWhenContentIsInline() throws Exception {
    Gadget gadget = makeDefaultGadget();

    control.replay();

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

    control.replay();

    String rewritten = rewrite(gadget, BODY_CONTENT);
    String base = getBaseElement(rewritten);

    assertEquals(viewUrl, base);
  }

  /**
   * Simple message bundle factory -- only honors inline bundles.
   */
  private static class FakeMessageBundleFactory implements MessageBundleFactory {
    public MessageBundle getBundle(GadgetSpec spec, Locale locale, boolean ignoreCache) {
      LocaleSpec localeSpec = spec.getModulePrefs().getLocale(locale);
      if (localeSpec == null) {
        return MessageBundle.EMPTY;
      }
      return spec.getModulePrefs().getLocale(locale).getMessageBundle();
    }
  }

  private static class FakeUrlGenerator implements UrlGenerator {
    public String getBundledJsParam(Collection<String> features, GadgetContext context) {
      throw new UnsupportedOperationException();
    }

    public String getIframeUrl(Gadget gadget) {
      throw new UnsupportedOperationException();
    }

    public String getBundledJsUrl(Collection<String> features, GadgetContext context) {
      return "/js/" + Join.join(":", features);
    }
  }

  private static class FakeGadgetFeatureRegistry extends GadgetFeatureRegistry {
    private final Map<String, GadgetFeature> features = Maps.newHashMap();

    public void addInline(String name, String content) throws GadgetException {
      List<JsLibrary> libs = Lists.newArrayList();
      libs.add(JsLibrary.create(JsLibrary.Type.INLINE, content, name, null));
      features.put(name, new GadgetFeature(name, libs, null));
    }

    public void addExternal(String name, String content) throws GadgetException {
      List<JsLibrary> libs = Lists.newArrayList();
      libs.add(JsLibrary.create(JsLibrary.Type.URL, content, name, null));
      features.put(name, new GadgetFeature(name, libs, null));
    }

    public FakeGadgetFeatureRegistry() throws GadgetException {
      super(null, null);
    }

    @Override
    public Collection<GadgetFeature> getFeatures(Collection<String> needed) {
      return getFeatures(needed, new HashSet<String>());
    }

    @Override
    public Collection<GadgetFeature> getFeatures(Collection<String> needed,
        Collection<String> unsupported) {
      List<GadgetFeature> out = Lists.newArrayList();
      for (String name : needed) {
        if (features.containsKey(name)) {
          out.add(features.get(name));
        } else {
          unsupported.add(name);
        }
      }
      return out;
    }
  }
}
