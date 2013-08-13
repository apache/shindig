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

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.isA;

import com.google.common.collect.Lists;

import org.apache.shindig.common.EasyMockTestCase;
import org.apache.shindig.common.PropertiesModule;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.xml.DomUtil;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.features.FeatureRegistry;
import org.apache.shindig.gadgets.features.FeatureResource;
import org.apache.shindig.gadgets.parse.ParseModule;
import org.apache.shindig.gadgets.parse.nekohtml.NekoSimplifiedHtmlParser;
import org.apache.shindig.gadgets.rewrite.XPathWrapper;
import org.apache.shindig.gadgets.templates.TagRegistry;
import org.apache.shindig.gadgets.templates.TemplateContext;
import org.apache.shindig.gadgets.templates.TemplateProcessor;
import org.apache.shindig.protocol.conversion.BeanJsonConverter;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.easymock.EasyMock;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.el.ELResolver;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Basic tests for Flash tag
 */
public class FlashTagHandlerTest extends EasyMockTestCase {

  private MyTemplateProcessor processor;
  private DOMImplementation documentProvider;
  private FlashTagHandler handler;
  private FeatureRegistry featureRegistry;
  private GadgetContext gadgetContext = mock(GadgetContext.class);
  private Gadget gadget = mock(Gadget.class);

  private NekoSimplifiedHtmlParser parser;
  protected Document result;

  @Before
  public void setUp() throws Exception {
    processor = new MyTemplateProcessor();
    processor.context = new TemplateContext(gadget, Collections.<String, JSONObject>emptyMap());
    Injector injector = Guice.createInjector(new ParseModule(), new PropertiesModule());
    documentProvider = injector.getInstance(DOMImplementation.class);
    parser = injector.getInstance(NekoSimplifiedHtmlParser.class);
    featureRegistry = mock(FeatureRegistry.class, true);
    handler = new FlashTagHandler(new BeanJsonConverter(injector), featureRegistry,
        "http://example.org/ns", "9.0.115");
    result = parser.parseDom("");

    EasyMock.expect(gadget.getContext()).andReturn(gadgetContext).anyTimes();
  }

  private void expectFeatureLookup() throws GadgetException {
    List<FeatureResource> swfObjectResources = Lists.newArrayList();
    swfObjectResources.add(new SwfResource());
    final FeatureRegistry.LookupResult lr = EasyMock.createMock(FeatureRegistry.LookupResult.class);
    EasyMock.expect(lr.getResources()).andReturn(swfObjectResources);
    EasyMock.replay(lr);
    EasyMock.expect(featureRegistry
        .getFeatureResources(isA(GadgetContext.class), eq(ImmutableSet.of("swfobject")),
            EasyMock.<List<String>>isNull())).andReturn(lr);
  }

  private static class SwfResource extends FeatureResource.Default {
    public String getContent() {
      return "swfobject()";
    }

    public String getDebugContent() {
      return "swfobject";
    }

    public String getName() {
      return "swfname";
    }
  }

  private void expectSecurityToken() {
    EasyMock.expect(gadgetContext.getParameter(EasyMock.eq("st"))).andReturn("12345");
  }

  @Test
  public void testBasicRender() throws Exception {
    Document document = parser.parseDom(
        "<script type='text/os-template'>"
            + "<osx:Flash swf='http://www.example.org/test.swf'>"
            + "Click Me"
          + "</osx:Flash></script>");
    Element tag = DomUtil.getElementsByTagNameCaseInsensitive(document, ImmutableSet.of("osx:flash"))
        .get(0);

    expectSecurityToken();
    EasyMock.expect(gadget.sanitizeOutput()).andReturn(false);
    expectFeatureLookup();
    replay();
    handler.process(result.getDocumentElement().getFirstChild().getNextSibling(), tag, processor);
    XPathWrapper wrapper = new XPathWrapper(result);
    assertEquals("swfobject()", wrapper.getValue("/html/head/script[1]"));
    assertEquals("os_xFlash_alt_1", wrapper.getValue("/html/body/div/@id"));
    assertEquals("Click Me", wrapper.getValue("/html/body/div"));
    assertNull(wrapper.getNode("/html/body/div/@onclick"));
    assertEquals(wrapper.getValue("/html/body/script[1]"),
        "swfobject.embedSWF(\"http://www.example.org/test.swf\",\"os_xFlash_alt_1\",\"100px\","
            + "\"100px\",\"9.0.115\",null,null,{\"flashvars\":\"st=12345\"},{});");
    verify();
  }

  @Test
  public void testSanitizedRender() throws Exception {
    Document document = parser.parseDom(
        "<script type='text/os-template'>"
            + "<osx:Flash swf='http://www.example.org/test.swf'>"
            + "Click Me"
          + "</osx:Flash></script>");
    Element tag = DomUtil.getElementsByTagNameCaseInsensitive(document, ImmutableSet.of("osx:flash"))
        .get(0);

    expectSecurityToken();
    EasyMock.expect(gadget.sanitizeOutput()).andReturn(true);
    expectFeatureLookup();
    replay();
    handler.process(result.getDocumentElement().getFirstChild().getNextSibling(), tag, processor);
    XPathWrapper wrapper = new XPathWrapper(result);
    assertEquals("swfobject()", wrapper.getValue("/html/head/script[1]"));
    assertEquals("os_xFlash_alt_1", wrapper.getValue("/html/body/div/@id"));
    assertEquals("Click Me", wrapper.getValue("/html/body/div"));
    assertNull(wrapper.getNode("/html/body/div/@onclick"));
    assertEquals(wrapper.getValue("/html/body/script[1]"),
        "swfobject.embedSWF(\"http://www.example.org/test.swf\",\"os_xFlash_alt_1\",\"100px\","
            + "\"100px\",\"9.0.115\",null,null,{\"swliveconnect\":false,"
            + "\"flashvars\":\"st=12345\",\"allowscriptaccess\":\"never\",\"allownetworking\":\"internal\"},{});");
    verify();
  }

  @Test
  public void testSanitizedRenderClickToPlay() throws Exception {
    Document document = parser.parseDom(
        "<script type='text/os-template'>"
            + "<osx:flash swf='http://www.example.org/test.swf' play='onclick'>"
            + "Click Me"
          + "</osx:flash></script>");
    Element tag = DomUtil.getElementsByTagNameCaseInsensitive(document, ImmutableSet.of("osx:flash"))
        .get(0);

    expectSecurityToken();
    EasyMock.expect(gadget.sanitizeOutput()).andReturn(true);
    expectFeatureLookup();
    replay();
    handler.process(result.getDocumentElement().getFirstChild().getNextSibling(), tag, processor);
    XPathWrapper wrapper = new XPathWrapper(result);
    assertEquals("swfobject()", wrapper.getValue("/html/head/script[1]"));
    assertEquals("os_xFlash_alt_1", wrapper.getValue("/html/body/div/@id"));
    assertEquals("Click Me", wrapper.getValue("/html/body/div"));
    assertEquals("os_xFlash_alt_1()", wrapper.getValue("/html/body/div/@onclick"));
    assertEquals(wrapper.getValue("/html/body/script[1]"),
        "function os_xFlash_alt_1(){ swfobject.embedSWF(\"http://www.example.org/test.swf\","
            + "\"os_xFlash_alt_1\",\"100px\",\"100px\",\"9.0.115\",null,null,"
            + "{\"swliveconnect\":false,\"flashvars\":\"st=12345\",\"allowscriptaccess\":\"never\",\"allownetworking\":\"internal\"},{}); }");
    verify();
  }

  @Test
  public void testConfigCreation() throws Exception {
    Document doc = documentProvider.createDocument(null, null, null);
    // Create a mock tag;  the name doesn't truly matter
    Element tag = doc.createElement("test");
    tag.setAttribute("id", "myflash");
    tag.setAttribute("class", "stylish");
    tag.setAttribute("swf", "http://www.example.org/x.swf");
    tag.setAttribute("width", "100px");
    tag.setAttribute("height", "200px");
    tag.setAttribute("name", "myflashname");
    tag.setAttribute("play", "onclick");
    tag.setAttribute("menu", "true");
    tag.setAttribute("scale", "exactfit");
    tag.setAttribute("wmode", "transparent");
    tag.setAttribute("devicefont", "true");
    tag.setAttribute("swliveconnect", "true");
    tag.setAttribute("allowscriptaccess", "samedomain");
    //tag.setAttribute("loop", "true");
    tag.setAttribute("quality", "autohigh");
    tag.setAttribute("salign", "tl");
    tag.setAttribute("bgcolor", "#77ff77");
    tag.setAttribute("allowfullscreen", "true");
    tag.setAttribute("allownetworking", "none");
    tag.setAttribute("flashvars", "a=b&c=d");
    FlashTagHandler.SwfObjectConfig config = handler.getSwfConfig(tag, processor);
    assertEquals("myflash", config.id);
    assertEquals("stylish", config.clazz);
    assertEquals(config.swf, Uri.parse("http://www.example.org/x.swf"));
    assertEquals("100px", config.width);
    assertEquals("200px", config.height);
    assertEquals("myflashname", config.name);
    assertEquals(FlashTagHandler.SwfObjectConfig.Play.onclick, config.play);
    assertEquals(Boolean.TRUE, config.menu);
    assertEquals(FlashTagHandler.SwfObjectConfig.Scale.exactfit, config.scale);
    assertEquals(FlashTagHandler.SwfObjectConfig.WMode.transparent, config.wmode);
    assertEquals(Boolean.TRUE, config.devicefont);
    assertEquals(Boolean.TRUE, config.swliveconnect);
    assertEquals(FlashTagHandler.SwfObjectConfig.ScriptAccess.samedomain, config.allowscriptaccess);
    assertNull(config.loop);
    assertEquals(FlashTagHandler.SwfObjectConfig.Quality.autohigh, config.quality);
    assertEquals(FlashTagHandler.SwfObjectConfig.SAlign.tl, config.salign);
    assertEquals("#77ff77", config.bgcolor);
    assertEquals(Boolean.TRUE, config.allowfullscreen);
    assertEquals(FlashTagHandler.SwfObjectConfig.NetworkAccess.none, config.allownetworking);
    assertEquals("a=b&c=d", config.flashvars);
  }

  @Test
  public void testConfigBindingFailure() throws Exception {
    Document document = parser.parseDom(
        "<script type='text/os-template'>"
            + "<osx:flash swf='http://www.example.org/test.swf' play='junk'>"
            + "Click Me"
          + "</osx:flash></script>");
    Element tag = DomUtil.getElementsByTagNameCaseInsensitive(document, ImmutableSet.of("osx:flash"))
        .get(0);
    handler.process(result.getDocumentElement().getFirstChild().getNextSibling(), tag, processor);
    XPathWrapper wrapper = new XPathWrapper(result);
    assertTrue(wrapper.getValue("/html/body/span").startsWith("Failed to process os:Flash tag"));
  }

  private static class MyTemplateProcessor implements TemplateProcessor {
    public TemplateContext context;

    public DocumentFragment processTemplate(Element template, TemplateContext templateContext,
                                            ELResolver globals, TagRegistry registry) {
      throw new UnsupportedOperationException();
    }

    public TemplateContext getTemplateContext() {
      return context;
    }

    public void processRepeat(Node result, Element element, Iterable<?> dataList,
                              Runnable onEachLoop) {
      // for (Object data : dataList) produces an unused variable warning
      Iterator<?> iterator = dataList.iterator();
      while (iterator.hasNext()) {
        iterator.next();
        onEachLoop.run();
      }
    }

    public <T> T evaluate(String expression, Class<T> type, T defaultValue) {
      return type.cast(expression);
    }

    public void processChildNodes(Node result, Node source) {
      NodeList childNodes = source.getChildNodes();
      for (int i = 0; i < childNodes.getLength(); i++) {
        Node child = childNodes.item(0).cloneNode(true);
        result.getOwnerDocument().adoptNode(child);
        result.appendChild(child);
      }
    }
  }
}
