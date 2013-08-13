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
package org.apache.shindig.gadgets.templates;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.xml.XmlUtil;
import org.apache.shindig.expressions.Expressions;
import org.apache.shindig.gadgets.templates.tags.TagHandler;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.google.common.collect.ImmutableMap;

/**
 * Test for TemplateLibrary parsing.
 *
 * TODO: Parse failure tests
 */
public class XmlTemplateLibraryTest {

  public static final String LIB_MARKUP =
    "<Templates xmlns:my='#my'>" +
    "  <Namespace prefix='my' url='#my'/>" +
    "  <JavaScript>libscript</JavaScript>" +
    "  <JavaScript>libscript2</JavaScript>" +
    "  <Style>libstyle</Style>" +
    "  <Style>libstyle2</Style>" +
    "  <Template tag='my:Flat'>Flat tag</Template>" +
    "  <TemplateDef tag='my:Def'>" +
    "    <Template>Def tag</Template>" +
    "    <JavaScript>tagscript</JavaScript>" +
    "    <Style>tagstyle</Style>" +
    "  </TemplateDef>" +
    "</Templates>";

  private static TemplateLibrary lib;

  private static Element doc;

  @BeforeClass
  public static void createDefaultLibrary() throws Exception {
    doc = XmlUtil.parse(LIB_MARKUP);
    lib = new XmlTemplateLibrary(Uri.parse("http://example.com/my"), doc, LIB_MARKUP);
  }

  @Test
  public void testTemplateElement() throws Exception {
    TagRegistry registry = lib.getTagRegistry();
    assertNotNull(registry.getHandlerFor(new TagRegistry.NSName("#my", "Flat")));
  }

  @Test
  public void testTemplateDefElement() throws Exception {
    TagRegistry registry = lib.getTagRegistry();
    assertNotNull(registry.getHandlerFor(new TagRegistry.NSName("#my", "Def")));
  }

  @Test
  public void testMissingElements() {
    TagRegistry registry = lib.getTagRegistry();
    assertNull(registry.getHandlerFor(new TagRegistry.NSName("#my", "Foo")));
    assertNull(registry.getHandlerFor(new TagRegistry.NSName("my", "Flat")));
  }

  @Test
  public void testAddedResources() {
    final TemplateContext context = new TemplateContext(null, ImmutableMap.<String, Object>of());
    TemplateProcessor processor = new DefaultTemplateProcessor(Expressions.forTesting()) {
      @Override
      public TemplateContext getTemplateContext() {
        return context;
      }
    };

    TagHandler handlerWithResources = lib.getTagRegistry()
       .getHandlerFor(new TagRegistry.NSName("#my", "Def"));
    TagHandler handlerWithNoResources = lib.getTagRegistry()
        .getHandlerFor(new TagRegistry.NSName("#my", "Flat"));

    Node result = doc.getOwnerDocument().createDocumentFragment();
    Element tag = doc.getOwnerDocument().createElement("test");

    // Script and style elements for the library should get registered
    // with the first tag for the whole library
    handlerWithNoResources.process(result, tag, processor);
    assertEquals("<STYLE>libstyle\nlibstyle2</STYLE>" +
                 "<JAVASCRIPT>libscript\nlibscript2</JAVASCRIPT>",
                 serializeResources(context));

    // Now script and style elements for the tag should get registered
    handlerWithResources.process(result, tag, processor);
    assertEquals("<STYLE>libstyle\nlibstyle2</STYLE>" +
        "<JAVASCRIPT>libscript\nlibscript2</JAVASCRIPT>" +
        "<JAVASCRIPT>tagscript</JAVASCRIPT>" +
        "<STYLE>tagstyle</STYLE>",
        serializeResources(context));

    // Nothing new should get registered with one more call
    handlerWithResources.process(result, tag, processor);
    assertEquals("<STYLE>libstyle\nlibstyle2</STYLE>" +
        "<JAVASCRIPT>libscript\nlibscript2</JAVASCRIPT>" +
        "<JAVASCRIPT>tagscript</JAVASCRIPT>" +
        "<STYLE>tagstyle</STYLE>",
        serializeResources(context));
  }

  private String serializeResources(TemplateContext context) {
    StringBuilder builder = new StringBuilder();
    for (TemplateResource resource : context.getResources()) {
      builder.append(resource);
    }

    return builder.toString();
  }
  @Test
  public void testSerialize() {
    assertEquals(LIB_MARKUP, lib.serialize());
  }

}
