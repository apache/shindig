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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isNull;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.apache.shindig.gadgets.parse.ParseModule;
import org.apache.shindig.gadgets.templates.TemplateProcessor;
import org.apache.shindig.gadgets.templates.tags.AbstractTagHandler;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class AbstractTagHandlerTest {
  private DOMImplementation documentProvider;
  private Document document;
  private AbstractTagHandler handler;
  private TemplateProcessor templateProcessor;


  @Before
  public void setUp() throws Exception {
    documentProvider = new ParseModule.DOMImplementationProvider().get();
    document = documentProvider.createDocument(null, null, null);
    handler = new AbstractTagHandler(null, null) {
      public void process(Node result, Element tag, TemplateProcessor processor) {
      }
    };

    templateProcessor = createMock(TemplateProcessor.class);
  }

  @Test
  public void getValueFromTag() {
    Element element = document.createElement("test");
    element.setAttribute("key", "expression");

    expect(templateProcessor.evaluate(eq("expression"), eq(String.class), (String) isNull()))
        .andReturn("evaluated");
    replay(templateProcessor);

    assertEquals("evaluated",
        handler.getValueFromTag(element, "key", templateProcessor, String.class));
    verify(templateProcessor);
  }

  @Test
  public void getValueFromTagNoAttribute() {
    Element element = document.createElement("test");

    replay(templateProcessor);
    assertNull(handler.getValueFromTag(element, "notthere", templateProcessor, String.class));
    verify(templateProcessor);
  }
}
