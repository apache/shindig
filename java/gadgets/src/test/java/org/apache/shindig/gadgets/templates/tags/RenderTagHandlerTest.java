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

import org.apache.shindig.expressions.Expressions;
import org.apache.shindig.expressions.RootELResolver;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.parse.DefaultHtmlSerializer;
import org.apache.shindig.gadgets.parse.ParseModule;
import org.apache.shindig.gadgets.parse.SocialDataTags;
import org.apache.shindig.gadgets.parse.nekohtml.NekoSimplifiedHtmlParser;
import org.apache.shindig.gadgets.templates.DefaultTemplateProcessor;
import org.apache.shindig.gadgets.templates.TagRegistry;
import org.apache.shindig.gadgets.templates.TemplateContext;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.json.JSONObject;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.el.ELResolver;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

public class RenderTagHandlerTest {

  private Expressions expressions;

  private TemplateContext context;
  private DefaultTemplateProcessor processor;
  private Map<String, JSONObject> variables;
  private ELResolver resolver;
  private TagRegistry registry;

  private NekoSimplifiedHtmlParser parser;

  private static final String TEST_NS = "http://example.com";

  @Before
  public void setUp() throws Exception {
    expressions = Expressions.forTesting();
    variables = Maps.newHashMap();
    Set<TagHandler> handlers = ImmutableSet.of((TagHandler) new RenderTagHandler());
    registry = new DefaultTagRegistry(handlers);

    processor = new DefaultTemplateProcessor(expressions);
    resolver = new RootELResolver();
    parser = new NekoSimplifiedHtmlParser(new ParseModule.DOMImplementationProvider().get());
    Gadget gadget = new Gadget();
    gadget.setContext(new GadgetContext());
    context = new TemplateContext(gadget, variables);

    addVariable("foo", new JSONObject("{ title: 'bar' }"));
  }

  @Test
  public void renderAllChildren() throws Exception {
    runTest("Bar",
        "[<os:Render/>]",
        "<foo:Bar>Hello</foo:Bar>", "[Hello]");
  }

  @Test
  public void renderSingleChildren() throws Exception {
    runTest("Panel",
        "<os:Render content='header'/> <os:Render content='footer'/>",
        "<foo:Panel><footer>Second</footer><header>First</header></foo:Panel>",
        "First Second");
  }

  private void runTest(String tagName, String tagMarkup, String templateMarkup,
      String expectedResult) throws GadgetException, IOException {
    Element templateDef = parseTemplate(templateMarkup);
    Element tagInstance = parseTemplate(tagMarkup);

    templateDef.getOwnerDocument().adoptNode(tagInstance);
    TagHandler tagHandler =
      new TemplateBasedTagHandler(tagInstance, TEST_NS, tagName);

    TagRegistry reg = new CompositeTagRegistry(ImmutableList.of(
        registry,
        new DefaultTagRegistry(ImmutableSet.of(tagHandler))));

    DocumentFragment result = processor.processTemplate(templateDef, context, resolver, reg);
    String output = serialize(result);
    assertEquals(expectedResult, output);
  }

  private Element parseTemplate(String markup) throws GadgetException {
    String content = "<script type=\"text/os-template\" xmlns:foo=\"" + TEST_NS +
        "\" xmlns:os=\"" + TagHandler.OPENSOCIAL_NAMESPACE + "\">" + markup + "</script>";
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

  private void addVariable(String key, JSONObject value) {
    variables.put(key, value);
  }

}
