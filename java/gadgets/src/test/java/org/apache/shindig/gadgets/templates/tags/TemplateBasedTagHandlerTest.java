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

import java.io.IOException;

import javax.el.ELResolver;

import org.apache.shindig.common.PropertiesModule;
import org.apache.shindig.expressions.RootELResolver;
import org.apache.shindig.gadgets.DefaultGuiceModule;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.admin.GadgetAdminModule;
import org.apache.shindig.gadgets.oauth.OAuthModule;
import org.apache.shindig.gadgets.oauth2.OAuth2Module;
import org.apache.shindig.gadgets.oauth2.handler.OAuth2HandlerModule;
import org.apache.shindig.gadgets.oauth2.persistence.sample.OAuth2PersistenceModule;
import org.apache.shindig.gadgets.oauth2.OAuth2MessageModule;
import org.apache.shindig.gadgets.parse.DefaultHtmlSerializer;
import org.apache.shindig.gadgets.parse.GadgetHtmlParser;
import org.apache.shindig.gadgets.parse.SocialDataTags;
import org.apache.shindig.gadgets.templates.TagRegistry;
import org.apache.shindig.gadgets.templates.TemplateContext;
import org.apache.shindig.gadgets.templates.TemplateProcessor;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * Tests the behavior of template-based tag handlers.
 */
public class TemplateBasedTagHandlerTest {

  private TemplateContext context;
  private TemplateProcessor processor;
  private final ELResolver resolver = new RootELResolver();
  private GadgetHtmlParser parser;

  private static final String TEST_NS = "http://example.com";

  @Before
  public void setUp() throws Exception {
    Injector injector = Guice.createInjector(new GadgetAdminModule(), new DefaultGuiceModule(), new OAuthModule(), new OAuth2Module(), new PropertiesModule(), new OAuth2PersistenceModule(), new OAuth2MessageModule(), new OAuth2HandlerModule());
    parser = injector.getInstance(GadgetHtmlParser.class);
    processor = injector.getInstance(TemplateProcessor.class);
    context = new TemplateContext(new Gadget(), null);
  }

  @Test
  public void attributeInMy() throws Exception {
    // Verify attribute EL retrieval
    runTest("Bar",
        "${My.attr}",
        "<foo:Bar attr='Hello'/>", "Hello");
  }

  @Test
  public void elementContentInMy() throws Exception {
    // Verify element content EL retrieval
    runTest("Bar",
        "${My.element}",
        "<foo:Bar><foo:element>Hello</foo:element></foo:Bar>", "Hello");
  }

  @Test
  public void attrTakesPrecedenceInMy() throws Exception {
    // Verify an attribute takes precedence over an element
    runTest("Bar",
        "${My.attr}",
        "<foo:Bar attr='Hello'><foo:attr>Goodbye</foo:attr></foo:Bar>", "Hello");
  }

  @Test
  public void elementAttributeInMy() throws Exception {
    // Verify an attribute of an element is visible
    runTest("Bar",
        "${My.element.text}",
        "<foo:Bar><foo:element text='Hello'/></foo:Bar>", "Hello");
  }

  @Test
  public void descendantElementInMy() throws Exception {
    // Verify the descendant of an element is visible
    runTest("Bar",
        "${My.element.child}",
        "<foo:Bar><foo:element><foo:child>Hello</foo:child></foo:element></foo:Bar>", "Hello");
  }

  @Test
  public void descendantElementNotFoundIfNotFullReference() throws Exception {
    // Verify the descendant element isn't visible unless directly referenced
    runTest("Bar",
        "${My.child}",
        "<foo:Bar><foo:element><foo:child>Hello</foo:child></foo:element></foo:Bar>", "");
  }

  @Test
  @Ignore("The CajaHtmlParser doesn't allow unclosed xml tags.")
  public void missingElementPropertyIsNull() throws Exception {
    // Verify the descendant element isn't visible unless directly referenced
    runTest("Bar",
        "${My.element.foo == null}",
        "<foo:Bar><foo:element>Hello/foo:element></foo:Bar>", "true");
  }

  @Test
  @Ignore("This currently returns [Hello,Goodbye].  Check the spec, and consider changing the spec.")
  public void multipleElementContentInMy() throws Exception {
    // Verify element content EL retrieval is concatenation for multiple elements
    runTest("Bar",
        "${My.element}",
        "<foo:Bar><foo:element>Hello</foo:element><foo:element>Goodbye</foo:element></foo:Bar>", "HelloGoodbye");
  }

  @Test
  public void elementListRepeat() throws Exception {
    // Verify a list of elements can be repeated over
    runTest("Bar",
        "<os:Repeat expression='${My.element}'>${text}</os:Repeat>",
        "<foo:Bar><foo:element text='Hello'/><foo:element text='Goodbye'/></foo:Bar>", "HelloGoodbye");
  }

  @Test
  public void singleElementRepeat() throws Exception {
    // Verify a single element can be "repeated" over
    runTest("Bar",
        "<os:Repeat expression='${My.element}'>${text}</os:Repeat>",
        "<foo:Bar><foo:element text='Hello'/></foo:Bar>", "Hello");
  }

  private void runTest(String tagName, String tagMarkup, String templateMarkup,
      String expectedResult) throws GadgetException, IOException {
    Element templateDef = parseTemplate(templateMarkup);
    Element tagInstance = parseTemplate(tagMarkup);

    templateDef.getOwnerDocument().adoptNode(tagInstance);
    TagHandler tagHandler = new TemplateBasedTagHandler(tagInstance, TEST_NS, tagName);
    TagRegistry reg = new DefaultTagRegistry(
        ImmutableSet.of(tagHandler, new RepeatTagHandler()));

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
}
