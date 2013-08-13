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
package org.apache.shindig.gadgets.templates.tags;

import org.apache.shindig.gadgets.parse.ParseModule;
import org.apache.shindig.gadgets.parse.nekohtml.NekoSimplifiedHtmlParser;
import org.apache.shindig.gadgets.templates.FakeTemplateProcessor;

import com.google.common.collect.ImmutableMap;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;

/**
 * Test of the <os:Html> tag.
 */
public class HtmlTagHandlerTest {
  private FakeTemplateProcessor processor;
  private DOMImplementation documentProvider;
  private HtmlTagHandler handler;

  @Before
  public void setUp() throws Exception {
    processor = new FakeTemplateProcessor();
    documentProvider = new ParseModule.DOMImplementationProvider().get();
    handler = new HtmlTagHandler(new NekoSimplifiedHtmlParser(documentProvider));
  }

  @Test
  public void testHtmlTag() throws Exception {
    Document doc = documentProvider.createDocument(null, null, null);
    // Create a mock tag;  the name doesn't truly matter
    Element tag = doc.createElement("test");
    tag.setAttribute("code", "${code}");
    processor.expressionResults = ImmutableMap.of(
        "${code}", "Hello <b>World</b>!");
    DocumentFragment fragment = doc.createDocumentFragment();
    handler.process(fragment, tag, processor);
    assertEquals(3, fragment.getChildNodes().getLength());
    assertEquals("b", fragment.getChildNodes().item(1).getNodeName());
  }
}
