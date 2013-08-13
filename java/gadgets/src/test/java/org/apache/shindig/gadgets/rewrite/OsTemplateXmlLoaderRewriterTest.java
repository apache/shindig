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
package org.apache.shindig.gadgets.rewrite;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import com.google.inject.Guice;
import com.google.inject.Injector;

import org.apache.shindig.common.PropertiesModule;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.xml.DomUtil;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.apache.shindig.gadgets.parse.GadgetHtmlParser;
import org.apache.shindig.gadgets.parse.ParseModule;
import org.apache.shindig.gadgets.rewrite.DomWalker.Visitor.VisitStatus;
import org.apache.shindig.gadgets.rewrite.OsTemplateXmlLoaderRewriter.Converter;

import org.json.JSONObject;

import org.junit.Before;
import org.junit.Test;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class OsTemplateXmlLoaderRewriterTest {
  private GadgetHtmlParser parser;
  private DOMImplementation domImpl;
  private Document doc;
  private Converter converter;

  @Before
  public void setUp() {
    Injector injector = Guice.createInjector(new ParseModule(), new PropertiesModule());
    parser = injector.getInstance(GadgetHtmlParser.class);
    domImpl = injector.getInstance(DOMImplementation.class);
    doc = domImpl.createDocument(null, null, null);
    converter = new Converter(parser, domImpl);
  }

  @Test
  public void convertSingleElement() throws Exception {
    String xml = "<os:elem id=\"id\" foo=\"bar\">String value</os:elem>";
    assertEquals(
        new JSONObject("{n:\"template\",a:[],c:[{n:\"os:elem\",a:[{n:\"foo\",v:\"bar\"}," +
            "{n:\"id\",v:\"id\"}],c:[\"String value\"]}]}").toString(),
        converter.domToJson(xml));
  }

  @Test
  public void convertMixedTreeWithIgnorables() throws Exception {
    String xml = "<b>Some ${viewer} content</b>  <img/><!-- comment --><os:Html/>";
    assertEquals(
        new JSONObject("{n:\"template\",a:[],c:[{n:\"b\",a:[],c:" +
            "[\"Some ${viewer} content\"]},\"  \",{n:\"img\",a:[],c:[]}," +
            "{n:\"os:Html\",a:[],c:[]}]}").toString(),
        converter.domToJson(xml));
  }

  @Test
  public void visitNonElement() throws Exception {
    assertEquals(VisitStatus.BYPASS, visit(doc.createTextNode("text")));
    assertEquals(VisitStatus.BYPASS, visit(doc.createAttribute("foo")));
    assertEquals(VisitStatus.BYPASS, visit(doc.createComment("comment")));
  }

  @Test
  public void visitDivSansType() throws Exception {
    assertEquals(VisitStatus.BYPASS, visit(doc.createElement("div")));
  }

  @Test
  public void visitDivMismatchingType() throws Exception {
    Element div = doc.createElement("div");
    div.setAttribute("id", "id");
    div.setAttribute("type", "os/template-but-not");
    assertEquals(VisitStatus.BYPASS, visit(div));
  }

  @Test
  public void visitDivMatchingTypeNoId() throws Exception {
    Element div = doc.createElement("div");
    div.setAttribute("type", OsTemplateXmlLoaderRewriter.OS_TEMPLATE_MIME);
    assertEquals(VisitStatus.BYPASS, visit(div));
  }

  @Test
  public void visitDivMatchingTypeBlankIdAndName() throws Exception {
    Element div = doc.createElement("div");
    div.setAttribute("id", "");
    div.setAttribute("name", "");
    div.setAttribute("type", OsTemplateXmlLoaderRewriter.OS_TEMPLATE_MIME);
    assertEquals(VisitStatus.BYPASS, visit(div));
  }

  @Test
  public void visitDivMatchingTypeWithId() throws Exception {
    Element div = createRewritableDiv();
    assertEquals(VisitStatus.RESERVE_NODE, visit(div));
  }

  @Test
  public void visitDivMatchingCaseMixedWithId() throws Exception {
    Element div = doc.createElement("dIv");
    div.setAttribute("id", "id");
    div.setAttribute("type", OsTemplateXmlLoaderRewriter.OS_TEMPLATE_MIME.toUpperCase());
    assertEquals(VisitStatus.RESERVE_NODE, visit(div));
  }

  @Test
  public void visitDivMatchingTypeWithName() throws Exception {
    Element div = doc.createElement("div");
    div.setAttribute("name", "id");
    div.setAttribute("type", OsTemplateXmlLoaderRewriter.OS_TEMPLATE_MIME);
    assertEquals(VisitStatus.RESERVE_NODE, visit(div));
  }

  @Test
  public void visitDivMatchingCaseMixedWithName() throws Exception {
    Element div = doc.createElement("dIv");
    div.setAttribute("name", "id");
    div.setAttribute("type", OsTemplateXmlLoaderRewriter.OS_TEMPLATE_MIME.toUpperCase());
    assertEquals(VisitStatus.RESERVE_NODE, visit(div));
  }

  private VisitStatus visit(Node node) throws Exception {
    return new OsTemplateXmlLoaderRewriter.GadgetHtmlVisitor(null).visit(null, node);
  }

  @Test
  public void revisitWithoutOsTemplates() throws Exception {
    assertFalse(revisit(mockGadget("foo", "bar"), null));
  }

  @Test(expected = RewritingException.class)
  public void revisitWithoutValidDocument() throws Exception {
    revisit(mockGadget(OsTemplateXmlLoaderRewriter.OS_TEMPLATES_FEATURE_NAME, "foo"),
        null, createRewritableDiv());
  }

  @Test(expected = RewritingException.class)
  public void revisitWithoutHeadNode() throws Exception {
    Node html = doc.createElement("html");
    html.appendChild(doc.createElement("body"));
    doc.appendChild(html);
    revisit(mockGadget(OsTemplateXmlLoaderRewriter.OS_TEMPLATES_FEATURE_NAME, "foo"),
        null, createRewritableDiv());
  }

  @Test
  public void revisitWithIdDivSingle() throws Exception {
    Element tpl = createRewritableDiv("tpl_id");
    checkRevisitSingle(tpl, "tpl_id");
  }

  @Test
  public void revisitWithNameDivSingle() throws Exception {
    Element tpl = createRewritableDiv();
    tpl.removeAttribute("id");
    tpl.setAttribute("name", "otherid");
    checkRevisitSingle(tpl, "otherid");
  }

  @Test
  public void revisitWithBothLabeledDivSingle() throws Exception {
    Element tpl = createRewritableDiv();
    tpl.setAttribute("name", "otherid");
    checkRevisitSingle(tpl, "otherid");
  }

  private void checkRevisitSingle(Element tpl, String id) throws Exception {
    Gadget gadget = mockGadget(OsTemplateXmlLoaderRewriter.OS_TEMPLATES_FEATURE_NAME, "another");
    String xmlVal = "xml";
    Converter converter = mockConverter(xmlVal, "{thejson}", 1);
    tpl.setTextContent(xmlVal);
    completeDocAsHtml(tpl);
    assertTrue(revisit(gadget, converter, tpl));
    verify(gadget);
    verify(converter);
    Node head = DomUtil.getFirstNamedChildNode(doc.getDocumentElement(), "head");
    assertNotNull(head);
    assertEquals(2, head.getChildNodes().getLength());
    Node addedScript = head.getChildNodes().item(1);
    assertEquals(Node.ELEMENT_NODE, addedScript.getNodeType());
    assertEquals("script", addedScript.getNodeName());
    assertEquals("gadgets.jsondom.preload_('" + id + "',{thejson});", addedScript.getTextContent());
  }

  @Test
  public void revisitMultiples() throws Exception {
    Element tplId = createRewritableDiv("tpl_id");
    Element tplName = createRewritableDiv();
    tplName.removeAttribute("id");
    tplName.setAttribute("name", "otherid");
    Gadget gadget = mockGadget(OsTemplateXmlLoaderRewriter.OS_TEMPLATES_FEATURE_NAME, "another");
    String xmlVal = "thexml";
    Converter converter = mockConverter(xmlVal, "{thejson}", 2);
    tplId.setTextContent(xmlVal);
    tplName.setTextContent(xmlVal);
    completeDocAsHtml(tplId, tplName);
    assertTrue(revisit(gadget, converter, tplId, tplName));
    verify(gadget);
    verify(converter);
    Node head = DomUtil.getFirstNamedChildNode(doc.getDocumentElement(), "head");
    assertNotNull(head);
    assertEquals(2, head.getChildNodes().getLength());
    Node addedScript = head.getChildNodes().item(1);
    assertEquals(Node.ELEMENT_NODE, addedScript.getNodeType());
    assertEquals("script", addedScript.getNodeName());
    assertEquals(
        "gadgets.jsondom.preload_('tpl_id',{thejson});gadgets.jsondom.preload_('otherid',{thejson});",
        addedScript.getTextContent());
  }

  private boolean revisit(Gadget gadget, Converter converter, Node... nodes) throws Exception {
    return new OsTemplateXmlLoaderRewriter.GadgetHtmlVisitor(converter)
        .revisit(gadget, Arrays.asList(nodes));
  }

  private Gadget mockGadget(String... features) {
    Gadget gadget = createMock(Gadget.class);
    expect(gadget.getAllFeatures()).andReturn(Arrays.asList(features)).once();
    replay(gadget);
    return gadget;
  }

  private Converter mockConverter(String xml, String result, int times) {
    Converter converter = createMock(Converter.class);
    expect(converter.domToJson(xml)).andReturn(result).times(times);
    replay(converter);
    return converter;
  }

  private Element createRewritableDiv() {
    return createRewritableDiv("id");
  }

  private Element createRewritableDiv(String id) {
    Element div = doc.createElement("div");
    div.setAttribute("type", OsTemplateXmlLoaderRewriter.OS_TEMPLATE_MIME);
    div.setAttribute("id", id);
    return div;
  }

  private void completeDocAsHtml(Node... nodes) {
    Node html = doc.createElement("html");
    Node head = doc.createElement("head");
    Node headScript = doc.createElement("script");
    head.appendChild(headScript);
    Node body = doc.createElement("body");
    for (Node node : nodes) {
      body.appendChild(node);
    }
    html.appendChild(head);
    html.appendChild(body);
    while (doc.hasChildNodes()) {
      doc.removeChild(doc.getFirstChild());
    }
    doc.appendChild(html);
  }

  @Test
  public void rewriteHttpNoMime() throws Exception {
    checkRewriteHttp(null, null, false);
  }

  @Test
  public void rewriteHttpMismatchedMime() throws Exception {
    checkRewriteHttp("os/template-not!", null, false);
  }

  @Test
  public void rewriteHttpMimeMatchOverride() throws Exception {
    checkRewriteHttp(OsTemplateXmlLoaderRewriter.OS_TEMPLATE_MIME, "os/template-not!", true);
  }

  @Test
  public void rewriteHttpMimeMatchOriginal() throws Exception {
    checkRewriteHttp(null, OsTemplateXmlLoaderRewriter.OS_TEMPLATE_MIME, true);
  }

  @Test
  public void rewriteHttpMimeMatchOverrideMismatchOriginal() throws Exception {
    checkRewriteHttp("foo", OsTemplateXmlLoaderRewriter.OS_TEMPLATE_MIME, false);
  }

  private void checkRewriteHttp(String reqMime, String origMime, boolean expectRewrite)
      throws Exception {
    HttpRequest req = new HttpRequest(Uri.parse("http://dummy.com")).setRewriteMimeType(reqMime);
    HttpResponse resp = new HttpResponseBuilder().setHeader("Content-Type", origMime).create();
    String inXml = "thexml";
    String outJson = "{thejson}";
    Converter converter = mockConverter(inXml, outJson, 1);
    MutableContent mc = createMock(MutableContent.class);
    if (expectRewrite) {
      expect(mc.getContent()).andReturn(inXml).once();
      mc.setContent(outJson);
      expectLastCall().once();
    }
    replay(mc);
    boolean result = new OsTemplateXmlLoaderRewriter(converter).rewrite(req, resp, mc);
    assertEquals(expectRewrite, result);
    verify(mc);
    if (expectRewrite) {
      verify(converter);
    }
  }
}
