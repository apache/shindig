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
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Element;

/**
 * Test for TemplateLibrary parsing.
 * 
 * TODO: Parse failure tests
 */
public class TemplateLibraryTest {

  public static final String LIB_MARKUP = 
    "<Templates xmlns:my='#my'>" +
    "  <Namespace prefix='my' url='#my'/>" +
    "  <JavaScript>script</JavaScript>" +
    "  <JavaScript>script2</JavaScript>" +
    "  <Style>style</Style>" +
    "  <Style>style2</Style>" +
    "  <Template tag='my:Flat'>Flat tag</Template>" +
    "  <TemplateDef tag='my:Def'>" +
    "    <Template>Def tag</Template>" +
    "    <JavaScript>script3</JavaScript>" +
    "    <Style>style3</Style>" +
    "  </TemplateDef>" +
    "</Templates>";
  
  private static TemplateLibrary lib;
  
  @BeforeClass
  public static void createDefaultLibrary() throws Exception {
    Element doc = XmlUtil.parse(LIB_MARKUP);
    lib = new TemplateLibrary(Uri.parse("http://example.com/my"), doc);
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
  public void testScript() {
    assertEquals("script\nscript2\nscript3", lib.getJavaScript());
  }
  
  @Test
  public void testStyle() {
    assertEquals("style\nstyle2\nstyle3", lib.getStyle());
  }
}
