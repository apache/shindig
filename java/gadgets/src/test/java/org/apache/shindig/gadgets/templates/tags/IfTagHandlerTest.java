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

import static org.easymock.EasyMock.isNull;
import static org.easymock.EasyMock.same;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import org.apache.shindig.gadgets.parse.ParseModule;
import org.apache.shindig.gadgets.templates.FakeTemplateProcessor;
import org.apache.shindig.gadgets.templates.tags.IfTagHandler;
import org.apache.shindig.gadgets.templates.tags.TagHandler;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.google.common.collect.ImmutableMap;

public class IfTagHandlerTest {
  private FakeTemplateProcessor processor;
  private DOMImplementation documentProvider;
  private TagHandler handler;

  @Before
  public void setUp() throws Exception {
    processor = EasyMock.createMock(FakeTemplateProcessor.class);
    documentProvider = new ParseModule.DOMImplementationProvider().get();
    handler = new IfTagHandler();
  }

  @Test
  public void conditionIsFalse() throws Exception {
    Document doc = documentProvider.createDocument(null, null, null);
    // Create a mock tag;  the name doesn't truly matter
    Element tag = doc.createElement("if");

    tag.setAttribute(IfTagHandler.CONDITION_ATTR, "fakeExpression");
    processor.expressionResults = ImmutableMap.of("fakeExpression", false);

    replay(processor);
    handler.process(null, tag, processor);
    verify(processor);
  }

  @Test
  public void conditionIsTrue() throws Exception {
    Document doc = documentProvider.createDocument(null, null, null);
    // Create a mock tag;  the name doesn't truly matter
    Element tag = doc.createElement("if");
    tag.setAttribute(IfTagHandler.CONDITION_ATTR, "fakeExpression");

    processor.expressionResults = ImmutableMap.of("fakeExpression", true);
    processor.processChildNodes((Node) isNull(), same(tag));

    replay(processor);
    handler.process(null, tag, processor);
    verify(processor);
  }

  @Test
  public void conditionIsMissing() throws Exception {
    Document doc = documentProvider.createDocument(null, null, null);
    // Create a mock tag;  the name doesn't truly matter
    Element tag = doc.createElement("if");

    replay(processor);
    handler.process(null, tag, processor);
    verify(processor);
  }

}
