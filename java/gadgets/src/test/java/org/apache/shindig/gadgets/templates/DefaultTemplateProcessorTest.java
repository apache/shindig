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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import org.apache.shindig.expressions.Expressions;
import org.apache.shindig.expressions.RootELResolver;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.parse.ParseModule;
import org.apache.shindig.gadgets.parse.nekohtml.NekoSerializer;
import org.apache.shindig.gadgets.parse.nekohtml.SocialMarkupHtmlParser;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import javax.el.ELResolver;

/**
 * Unit tests for DefaultTemplateProcessor.
 * TODO: Refactor to remove boilerplate.
 * TODO: Add tests for special vars.
 * TODO: Add test for @var in @repeat loops. 
 */
public class DefaultTemplateProcessorTest {

  private Expressions expressions;

  private TemplateContext context;
  private DefaultTemplateProcessor processor;
  private Map<String, JSONObject> variables;
  private ELResolver resolver;
  private TagRegistry registry;

  private SocialMarkupHtmlParser parser;
  
  private static final String TEST_NS = "http://example.com";
  
  @Before
  public void setUp() throws Exception {
    expressions = new Expressions();
    variables = Maps.newHashMap();
    Set<TagHandler> handlers = ImmutableSet.of((TagHandler) new TestTagHandler());
    registry = new TagRegistry(handlers);

    processor = new DefaultTemplateProcessor(expressions);
    resolver = new RootELResolver();
    parser = new SocialMarkupHtmlParser(new ParseModule.DOMImplementationProvider().get());    
    context = new TemplateContext(new Gadget(), variables);
    
    addVariable("foo", new JSONObject("{ title: 'bar' }"));
    addVariable("user", new JSONObject("{ id: '101', name: { first: 'John', last: 'Doe' }}"));
    addVariable("toys", new JSONObject("{ list: [{name: 'Ball'}, {name: 'Car'}]}"));
    addVariable("xss", new JSONObject("{ script: '<script>alert();</script>'," +
    		"quote:'\"><script>alert();</script>'}"));
  }

  @Test
  public void testTextNode() throws Exception {
    String output = executeTemplate("${foo.title}");
    assertEquals("bar", output);
  }
  
  @Test
  public void testTopVariable() throws Exception {
    String output = executeTemplate("${Top.foo.title}");
    assertEquals("bar", output);
  }
  
  @Test
  public void testCurVariable() throws Exception {
    // Cur starts as Top
    String output = executeTemplate("${Cur.foo.title}");
    assertEquals("bar", output);
  }
  
  @Test
  public void testMyVariable() throws Exception {
    // My starts as null
    String output = executeTemplate("${My.foo.title}");
    assertEquals("", output);
  }
  
  @Test
  public void testPlainText() throws Exception {
    // Verify that plain text is not interfered with, or incorrectly escaped
    String output = executeTemplate("<span>foo&amp;&bar</span>");
    assertEquals("<span>foo&amp;&bar</span>", output);
  }

  @Test
  public void testTextNodeEscaping() throws Exception {
    String output = executeTemplate("${xss.script}");
    assertFalse("Escaping not performed: \"" + output + "\"", output.contains("<script>alert("));
  }
  
  @Test
  public void testAppending() throws Exception {
    String output = executeTemplate("${user.id}${user.name.first}");
    assertEquals("101John", output);
    
    output = executeTemplate("foo${user.id}bar${user.name.first}baz");
    assertEquals("foo101barJohnbaz", output);

    output = executeTemplate("foo${user.nope}bar${user.nor}baz");
    assertEquals("foobarbaz", output);
  }
  
  @Test
  public void testEscapedExpressions() throws Exception {
    String output = executeTemplate("\\${escaped}");
    assertEquals("${escaped}", output);

    output = executeTemplate("foo\\${escaped}bar");
    assertEquals("foo${escaped}bar", output);
  }

  @Test
  public void testElement() throws Exception {
    String output = executeTemplate("<span title=\"${user.id}\">${user.name.first} baz</span>");
    assertEquals("<span title=\"101\">John baz</span>", output);
  }

  @Test
  public void testAttributeEscaping() throws Exception {
    String output = executeTemplate("<span title=\"${xss.quote}\">${user.name.first} baz</span>");
    assertFalse(output.contains("\"><script>alert("));
  }

  @Test
  public void testRepeat() throws Exception {
    String output = executeTemplate("<span repeat=\"${toys}\">${name}</span>");
    assertEquals("<span>Ball</span><span>Car</span>", output);
  }
  
  @Test
  public void testCurAttribute() throws Exception {
    String output = executeTemplate("<span cur=\"${user.name}\">${first}</span>");
    assertEquals("<span>John</span>", output);
  }
  
  @Test
  public void testConditional() throws Exception {
    String output = executeTemplate(
        "<span repeat=\"${toys}\">" +
          "<span if=\"${name == 'Car'}\">Car</span>" +
          "<span if=\"${name != 'Car'}\">Not Car</span>" +
        "</span>");
    assertEquals("<span><span>Not Car</span></span><span><span>Car</span></span>", output);
  }
  
  @Test
  public void testCustomTag() throws Exception {
    String output = executeTemplate("<test:Foo text='${foo.title}' data='${user}'/>", 
        "xmlns:test='" + TEST_NS + "'");
    assertEquals("<b>BAR</b>", output);
  }

  private String executeTemplate(String markup) throws Exception {
    return executeTemplate(markup, "");
  }
  
  private String executeTemplate(String markup, String extra) throws Exception {
    Element template = prepareTemplate(markup, extra);
    DocumentFragment result = processor.processTemplate(template, context, resolver, registry);
    return serialize(result);
  }
  
  private Element prepareTemplate(String markup, String extra) throws GadgetException {    
    String content = "<script type=\"text/os-template\"" + extra + ">" + markup + "</script>";
    Document document = parser.parseDom(content);
    return (Element) document.getElementsByTagName("script").item(0);
  }
  
  private String serialize(Node node) throws IOException {
    StringBuilder sb = new StringBuilder();
    NodeList children = node.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node child = children.item(i);
      NekoSerializer.serialize(child, sb);
    }
    return sb.toString();
  }
  
  private void addVariable(String key, JSONObject value) {
    variables.put(key, value);
  }
  
  /**
   * A dummy custom tag.
   * Expects a @text attribute equal to "bar", and a @data attribute that
   * evaluates to a JSONObject with an id property equal to "101".
   * If these conditions are met, returns <code>&lt;b&gt;BAR&lt;/b&gt;</code> 
   */
  private static class TestTagHandler extends AbstractTagHandler {
    
    public TestTagHandler() {
      super(TEST_NS, "Foo");
    }
    
    public void process(Node result, Element tag, TemplateProcessor processor) {     
      Object data = getValueFromTag(tag, "data", processor, Object.class);
      assertTrue(data instanceof JSONObject);
      assertEquals("101", ((JSONObject) data).optString("id"));
            
      String text = getValueFromTag(tag, "text", processor, String.class);
      text = text.toUpperCase();
      Document doc = result.getOwnerDocument();
      Element b = doc.createElement("b");
      b.appendChild(doc.createTextNode(text));
      result.appendChild(b);
    }
  }
}
