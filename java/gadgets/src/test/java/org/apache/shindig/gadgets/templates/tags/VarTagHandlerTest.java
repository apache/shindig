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

import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import org.apache.shindig.expressions.Expressions;
import org.apache.shindig.expressions.RootELResolver;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.parse.DefaultHtmlSerializer;
import org.apache.shindig.gadgets.parse.ParseModule;
import org.apache.shindig.gadgets.parse.SocialDataTags;
import org.apache.shindig.gadgets.parse.nekohtml.NekoSimplifiedHtmlParser;
import org.apache.shindig.gadgets.templates.DefaultTemplateProcessor;
import org.apache.shindig.gadgets.templates.TagRegistry;
import org.apache.shindig.gadgets.templates.TemplateContext;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import javax.el.ELResolver;

public class VarTagHandlerTest {

  private Expressions expressions;

  private TemplateContext context;
  private DefaultTemplateProcessor processor;
  private Map<String, Object> variables;
  private ELResolver resolver;
  private TagRegistry registry;

  private NekoSimplifiedHtmlParser parser;
  protected Document result;

  @Before
  public void setUp() throws Exception {

    expressions = Expressions.forTesting();
    variables = Maps.newHashMap();
    Set<TagHandler> handlers = ImmutableSet.<TagHandler> of(new VarTagHandler());
    registry = new DefaultTagRegistry(handlers);

    processor = new DefaultTemplateProcessor(expressions);
    resolver = new RootELResolver();
    parser = new NekoSimplifiedHtmlParser(new ParseModule.DOMImplementationProvider().get());
    context = new TemplateContext(new Gadget(), variables);

  }

  @Test
  public void testTag() throws Exception {
    String output = executeTemplate("<os:Var key='myvar' value='3'></os:Var>The value of my var is ${myvar}",
            "xmlns:os=\"http://ns.opensocial.org/2008/markup\"");
    assertEquals("The value of my var is 3", output);
  }

  private String executeTemplate(String markup, String extra) throws Exception {
    Element template = prepareTemplate(markup, extra);
    DocumentFragment result = processor.processTemplate(template, context, resolver, registry);
    return serialize(result);
  }

  private Element prepareTemplate(String markup, String extra) throws GadgetException {
    String content = "<script type=\"text/os-template\"" + extra + '>' + markup + "</script>";
    Document document = parser.parseDom(content);
    return SocialDataTags.getTags(document, SocialDataTags.OSML_TEMPLATE_TAG).get(0);
  }

  private String serialize(Node node) throws IOException {
    StringBuilder sb = new StringBuilder();
    NodeList children = node.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node child = children.item(i);
      new DefaultHtmlSerializer().serialize(child, sb);
    }
    return sb.toString();
  }

}
