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

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.xml.XmlUtil;
import org.apache.shindig.config.BasicContainerConfig;
import org.apache.shindig.expressions.Expressions;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.parse.ParseModule;
import org.apache.shindig.gadgets.parse.nekohtml.NekoSimplifiedHtmlParser;
import org.apache.shindig.gadgets.render.FakeMessageBundleFactory;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.SpecParserException;
import org.apache.shindig.gadgets.templates.ContainerTagLibraryFactory;
import org.apache.shindig.gadgets.templates.DefaultTemplateProcessor;
import org.apache.shindig.gadgets.templates.TemplateLibrary;
import org.apache.shindig.gadgets.templates.TemplateLibraryFactory;
import org.apache.shindig.gadgets.templates.TemplateProcessor;
import org.apache.shindig.gadgets.templates.XmlTemplateLibrary;
import org.apache.shindig.gadgets.templates.tags.AbstractTagHandler;
import org.apache.shindig.gadgets.templates.tags.DefaultTagRegistry;
import org.apache.shindig.gadgets.templates.tags.TagHandler;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.inject.Provider;
import org.json.JSONException;
import org.json.JSONObject;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.Map;
import java.util.Set;

/**
 * Tests for TemplateRewriter
 */
public class TemplateRewriterTest {

  private GadgetSpec gadgetSpec;
  private Gadget gadget;
  private MutableContent content;
  private TemplateRewriter rewriter;
  private final Map<String, Object> data = Maps.newHashMap();

  private static final Uri GADGET_URI = Uri.parse("http://example.org/gadget.php");

  private static final String CONTENT_PLAIN =
    "<script type='text/os-template'>Hello, ${user.name}</script>";

  private static final String CONTENT_WITH_MESSAGE =
    "<script type='text/os-template'>Hello, ${Msg.name}</script>";

  private static final String CONTENT_REQUIRE =
    "<script type='text/os-template' require='user'>Hello, ${user.name}</script>";

  private static final String CONTENT_REQUIRE_MISSING =
    "<script type='text/os-template' require='foo'>Hello, ${user.name}</script>";

  private static final String CONTENT_WITH_TAG =
    "<script type='text/os-template' xmlns:foo='#foo' tag='foo:Bar'>Hello, ${user.name}</script>";

  private static final String CONTENT_WITH_AUTO_UPDATE =
    "<script type='text/os-template' autoUpdate='true'>Hello, ${user.name}</script>";

  private static final String TEMPLATE_LIBRARY =
    "<Templates xmlns:my='#my'>" +
    "  <Namespace prefix='my' url='#my'/>" +
    "  <JavaScript>script</JavaScript>" +
    "  <Style>style</Style>" +
    "  <Template tag='my:Tag1'>external1</Template>" +
    "  <Template tag='my:Tag2'>external2</Template>" +
    "  <Template tag='my:Tag3'>external3</Template>" +
    "  <Template tag='my:Tag4'>external4</Template>" +
    "</Templates>";

  private static final String TEMPLATE_LIBRARY_URI = "http://example.org/library.xml";
  private static final String CONTENT_WITH_TAG_FROM_LIBRARY =
    "<script type='text/os-template' xmlns:my='#my'><my:Tag4/></script>";

  private static final String CONTENT_TESTING_PRECEDENCE_RULES =
    "<script type='text/os-template' xmlns:my='#my' tag='my:Tag1'>inline1</script>" +
    "<script type='text/os-template' xmlns:my='#my' tag='my:Tag2'>inline2</script>" +
    "<script type='text/os-template' xmlns:my='#my' tag='my:Tag3'>inline3</script>" +
    "<script type='text/os-template' xmlns:my='#my'><my:Tag1/><my:Tag2/><my:Tag3/><my:Tag4/></script>";

  @Before
  public void setUp() {
    Set<TagHandler> handlers = ImmutableSet.of(testTagHandler("Tag1", "default1"));
    rewriter = new TemplateRewriter(
        new Provider<TemplateProcessor>() {
          public TemplateProcessor get() {
            return new DefaultTemplateProcessor(Expressions.forTesting());
          }
        },
        new FakeMessageBundleFactory(),
        Expressions.forTesting(),
        new DefaultTagRegistry(handlers),
        new FakeTemplateLibraryFactory(),
        new ContainerTagLibraryFactory(new FakeContainerConfig()));
  }

 private static TagHandler testTagHandler(String name, final String content) {
   return new AbstractTagHandler("#my", name) {
    public void process(Node result, Element tag, TemplateProcessor processor) {
      result.appendChild(result.getOwnerDocument().createTextNode(content));
    }
   };
 }

  @Test
  public void simpleTemplate() throws Exception {
    // Render a simple template
    testExpectingTransform(getGadgetXml(CONTENT_PLAIN), "simple");
    testFeatureRemoved();
  }

  @Test
  public void noTemplateFeature() throws Exception {
    // Without opensocial-templates feature, shouldn't render
    testExpectingNoTransform(getGadgetXml(CONTENT_PLAIN, false), "no feature");
  }

  @Test
  public void requiredDataPresent() throws Exception {
    // Required data is present - render
    testExpectingTransform(getGadgetXml(CONTENT_REQUIRE), "required data");
    testFeatureRemoved();
  }

  @Test
  public void requiredDataMissing() throws Exception {
    // Required data is missing - don't render
    testExpectingNoTransform(getGadgetXml(CONTENT_REQUIRE_MISSING), "missing data");
    testFeatureNotRemoved();
  }

  @Test
  public void tagAttributePresent() throws Exception {
    // Don't render templates with a @tag
    testExpectingNoTransform(getGadgetXml(CONTENT_WITH_TAG), "with @tag");
    testFeatureRemoved();
  }

  @Test
  public void templateUsingMessage() throws Exception {
    // Render a simple template
    testExpectingTransform(getGadgetXml(CONTENT_WITH_MESSAGE), "simple");
    testFeatureRemoved();
  }

  @Test
  public void autoUpdateTemplate() throws Exception {
    setupGadget(getGadgetXml(CONTENT_WITH_AUTO_UPDATE));
    rewriter.rewrite(gadget, content);
    // The template should get transformed, but not removed
    assertTrue("Template wasn't transformed",
        content.getContent().indexOf("Hello, John") > 0);
    assertTrue("Template tag was removed",
        content.getContent().contains("text/os-template"));
    assertTrue("ID span was not created",
        content.getContent().contains("<span id=\"_T_template_auto0\">"));
    testFeatureNotRemoved();
  }

  @Test
  public void templateWithLibrary() throws Exception {
    setupGadget(getGadgetXmlWithLibrary(CONTENT_WITH_TAG_FROM_LIBRARY));
    rewriter.rewrite(gadget, content);
    assertTrue("Script not inserted", content.getContent().indexOf(
        "<script type=\"text/javascript\">script</script>") > 0);
    assertTrue("Style not inserted", content.getContent().indexOf(
        "<style type=\"text/css\">style</style>") > 0);
    assertTrue("Tag not executed", content.getContent().indexOf(
        "external4") > 0);

    testFeatureRemoved();
  }

  @Test
  public void osmlWithLibrary() throws Exception {
    setupGadget(getGadgetXmlWithLibrary(CONTENT_WITH_TAG_FROM_LIBRARY, "osml"));
    rewriter.rewrite(gadget, content);
    assertTrue("Custom tags were evaluated", content.getContent().equals(
        "<html><head></head><body><my:Tag4></my:Tag4></body></html>"));

    testFeatureRemoved();
  }

  @Test
  public void tagPrecedenceRules() throws Exception {
    // Tag definitions include:
    // Default handlers: tag1 default1
    // OSML: tag1 osml1 tag2 osml2
    // inline tags: tag1 inline1 tag2 inline2 tag3 inline3
    // External tags: tag1 external1 tag2 external2 tag3 external3 tag4 external4

    data.put("${Cur['gadgets.features'].osml.library}",
        "org/apache/shindig/gadgets/rewrite/OSML_test.xml");

    setupGadget(getGadgetXmlWithLibrary(CONTENT_TESTING_PRECEDENCE_RULES));
    rewriter.rewrite(gadget, content);
    assertTrue("Precedence rules violated",
        content.getContent().indexOf("default1osml2inline3external4") > 0);

    testFeatureRemoved();
  }

  @Test
  public void tagPrecedenceRulesWithOSMLFeature() throws Exception {
    // A strict subset of os templating is enabled when the osml feature is required
    // Tag definitions include:
    // Default handlers: tag1 default1
    // OSML: tag1 osml1 tag2 osml2

    data.put("${Cur['gadgets.features'].osml.library}",
        "org/apache/shindig/gadgets/rewrite/OSML_test.xml");

    setupGadget(getGadgetXmlWithLibrary(CONTENT_TESTING_PRECEDENCE_RULES, "osml"));
    rewriter.rewrite(gadget, content);
    assertTrue("Precedence rules violated", content.getContent().indexOf(
        "default1osml2<my:Tag3></my:Tag3><my:Tag4></my:Tag4>") > 0);

    testFeatureRemoved();
  }

  @Test
  public void tagPrecedenceRulesWithoutOSML() throws Exception {
    // Tag definitions include:
    // Default handlers: tag1 default1
    // OSML: tag1 osml1 tag2 osml2
    // inline tags: tag1 inline1 tag2 inline2 tag3 inline3
    // External tags: tag1 external1 tag2 external2 tag3 external3 tag4 external4

    // Explicitly don't support OSML
    data.put("${Cur['gadgets.features'].osml.library}", "");

    setupGadget(getGadgetXmlWithLibrary(CONTENT_TESTING_PRECEDENCE_RULES));
    rewriter.rewrite(gadget, content);
    assertTrue("Precedence rules violated",
        content.getContent().indexOf("default1inline2inline3external4") > 0);

    testFeatureRemoved();
  }

  @Test
  public void testClientOverride() throws Exception {
    // Should normally remove feature
    testExpectingTransform(getGadgetXml(CONTENT_PLAIN, true, "true"), "keep client");
    testFeatureNotRemoved();

    // Should normally keep feature
    testExpectingNoTransform(getGadgetXml(CONTENT_WITH_TAG, true, "false"), "remove client");
    testFeatureRemoved();
  }

  private void testFeatureRemoved() {
    assertFalse("Feature wasn't removed",
        gadget.getDirectFeatureDeps().contains("opensocial-templates"));
  }

  private void testFeatureNotRemoved() {
    assertTrue("Feature was removed",
        gadget.getDirectFeatureDeps().contains("opensocial-templates"));
  }

  private void testExpectingTransform(String code, String condition) throws Exception {
    setupGadget(code);
    rewriter.rewrite(gadget, content);
    assertTrue("Template wasn't transformed (" + condition + ')',
        content.getContent().indexOf("Hello, John") > 0);
    assertTrue("Template tag wasn't removed (" + condition + ')',
        !content.getContent().contains("text/os-template"));
  }

  private void testExpectingNoTransform(String code, String condition) throws Exception {
    setupGadget(code);
    rewriter.rewrite(gadget, content);
    assertTrue("Template was transformed (" + condition + ')',
        content.getContent().indexOf("${user.name}") > 0);
    assertTrue("Template tag was removed (" + condition + ')',
        content.getContent().indexOf("text/os-template") > 0);
  }

  private void setupGadget(String gadgetXml) throws SpecParserException, JSONException {
    gadgetSpec = new GadgetSpec(GADGET_URI, gadgetXml);
    gadget = new Gadget();
    gadget.setSpec(gadgetSpec);
    gadget.setContext(new GadgetContext() {

      @Override
      public Uri getUrl() {
        return GADGET_URI;
      }
    });
    gadget.setCurrentView(gadgetSpec.getView("default"));

    content = new MutableContent(new NekoSimplifiedHtmlParser(
        new ParseModule.DOMImplementationProvider().get()), gadget.getCurrentView().getContent());
    putPipelinedData("user", new JSONObject("{ name: 'John'}"));
  }

  private void putPipelinedData(String key, JSONObject data) {
    content.addPipelinedData(key, data);
  }

  private static String getGadgetXml(String content) {
    return getGadgetXml(content, true);
  }

  private static String getGadgetXml(String content, boolean requireFeature) {
    return getGadgetXml(content, requireFeature, null);
  }

  private static String getGadgetXml(String content, boolean requireFeature,
      String clientParam) {
    String feature = requireFeature ?
        "<Require feature='opensocial-templates'" +
        (clientParam != null ?
            ("><Param name='client'>" + clientParam + "</Param></Require>")
            : "/>")
        : "";
    return "<Module>" + "<ModulePrefs title='Title'>"
        + feature
        + "  <Locale>"
        + "    <msg name='name'>John</msg>"
        + "  </Locale>"
        + "</ModulePrefs>"
        + "<Content>"
        + "    <![CDATA[" + content + "]]>"
        + "</Content></Module>";
  }

  private static String getGadgetXmlWithLibrary(String content) {
    return getGadgetXmlWithLibrary(content, "opensocial-templates");
  }

  private static String getGadgetXmlWithLibrary(String content, String feature) {
    return "<Module>" + "<ModulePrefs title='Title'>"
        + "  <Require feature='" + feature + "'>"
        + "    <Param name='" + TemplateRewriter.REQUIRE_LIBRARY_PARAM + "'>"
        + TEMPLATE_LIBRARY_URI
        + "    </Param>"
        + "  </Require>"
        + "</ModulePrefs>"
        + "<Content>"
        + "    <![CDATA[" + content + "]]>"
        + "</Content></Module>";
  }

  private static class FakeTemplateLibraryFactory extends TemplateLibraryFactory {
    public FakeTemplateLibraryFactory() {
      super(null, null);
    }

    @Override
    public TemplateLibrary loadTemplateLibrary(GadgetContext context, Uri uri)
        throws GadgetException {
      assertEquals(TEMPLATE_LIBRARY_URI, uri.toString());
      return new XmlTemplateLibrary(uri, XmlUtil.parseSilent(TEMPLATE_LIBRARY),
          TEMPLATE_LIBRARY);
    }
  }

  private class FakeContainerConfig extends BasicContainerConfig {
    @Override
    public Object getProperty(String container, String name) {
      return data.get(name);
    }

  }
}
