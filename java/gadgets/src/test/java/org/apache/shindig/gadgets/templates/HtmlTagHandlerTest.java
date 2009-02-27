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

import org.apache.shindig.gadgets.parse.ParseModule;
import org.apache.shindig.gadgets.parse.nekohtml.SocialMarkupHtmlParser;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.el.ELResolver;

public class HtmlTagHandlerTest {

  private TemplateProcessor processor;
  private DOMImplementation documentProvider;
  private HtmlTagHandler handler;
   
  @Before
  public void setUp() throws Exception {
    processor = new TemplateProcessor() {
      public <T extends Object> T evaluate(String expression, Class<T> type, T defaultValue) {
        // The test only "supports" String expressions
        return type.cast(expression);
      }
      
      public DocumentFragment processTemplate(Element template,
          TemplateContext templateContext, ELResolver globals) {
        return null;
      }

      public void processChildNodes(Node result, Node source) {
      }
    };

    documentProvider = new ParseModule.DOMImplementationProvider().get();
    handler = new HtmlTagHandler(new SocialMarkupHtmlParser(documentProvider));
  }
  
  @Test
  public void testHtmlTag() throws Exception {
    Document doc = documentProvider.createDocument(null, null, null);
    // Create a mock tag;  the name doesn't truly matter
    Element tag = doc.createElement("test");
    tag.setAttribute("code", "Hello <b>World</b>!");
    DocumentFragment fragment = doc.createDocumentFragment();
    handler.process(fragment, tag, processor);
    assertEquals(3, fragment.getChildNodes().getLength());
    assertEquals("b", fragment.getChildNodes().item(1).getNodeName());
  }
}
