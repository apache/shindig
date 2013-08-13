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

import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import org.apache.shindig.gadgets.parse.ParseModule;
import org.apache.shindig.gadgets.templates.FakeTemplateProcessor;
import org.apache.shindig.gadgets.templates.tags.RepeatTagHandler;
import org.apache.shindig.gadgets.templates.tags.TagHandler;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

public class RepeatTagHandlerTest {
  private FakeTemplateProcessor processor;
  private DOMImplementation documentProvider;
  private TagHandler handler;

  @Before
  public void setUp() throws Exception {
    processor = EasyMock.createMock(FakeTemplateProcessor.class);
    documentProvider = new ParseModule.DOMImplementationProvider().get();
    handler = new RepeatTagHandler();
  }

  @Test
  public void repeat() throws Exception {
    Document doc = documentProvider.createDocument(null, null, null);
    // Create a mock tag;  the name doesn't truly matter
    Element tag = doc.createElement("repeat");
    tag.setAttribute(RepeatTagHandler.EXPRESSION_ATTR, "fakeExpression");

    List<String> mockList = ImmutableList.of("a", "b", "c");
    processor.expressionResults = ImmutableMap.of("fakeExpression", mockList);

    processor.processChildNodes(null, tag);
    EasyMock.expectLastCall().times(3);

    replay(processor);
    handler.process(null, tag, processor);
    verify(processor);
  }

  @Test
  public void repeatWithoutExpression() throws Exception {
    Document doc = documentProvider.createDocument(null, null, null);
    // Create a mock tag;  the name doesn't truly matter
    Element tag = doc.createElement("repeat");

    replay(processor);
    handler.process(null, tag, processor);
    verify(processor);
  }

  @Test
  public void repeatWithIf() throws Exception {
    Document doc = documentProvider.createDocument(null, null, null);
    // Create a mock tag;  the name doesn't truly matter
    Element tag = doc.createElement("repeat");
    tag.setAttribute(RepeatTagHandler.EXPRESSION_ATTR, "fakeExpression");
    tag.setAttribute(RepeatTagHandler.IF_ATTR, "fakeIf");

    List<String> mockList = ImmutableList.of("a", "b", "c");
    processor.expressionResults = ImmutableMap.of("fakeExpression", mockList,
        // Return "false", "true", and "false" for each step
        "fakeIf", Lists.newArrayList(false, true, false));

    processor.processChildNodes(null, tag);
    // "if" should evaluate to true only once
    EasyMock.expectLastCall().times(1);

    replay(processor);
    handler.process(null, tag, processor);
    verify(processor);
  }
}
