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

import com.google.common.base.Strings;
import com.google.inject.Inject;

import org.apache.shindig.common.xml.DomUtil;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.parse.GadgetHtmlParser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Attr;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.List;

/**
 * Rewrites the gadget to include template and xml information
 * @since 2.0.0
 */
public class OsTemplateXmlLoaderRewriter extends DomWalker.Rewriter {
  public static final String OS_TEMPLATE_MIME = "os/template";
  public static final String OS_TEMPLATES_FEATURE_NAME = "opensocial-templates";
  private static final String PRELOAD_TPL = "gadgets.jsondom.preload_('%s',%s);";

  private final Converter converter;

  @Inject
  public OsTemplateXmlLoaderRewriter(Converter converter) {
    super(new GadgetHtmlVisitor(converter));
    this.converter = converter;
  }

  // Override the HTTP rewrite method to provide custom type checking.
  // The gadget rewrite method remains standard, using the Visitor pattern.
  public boolean rewrite(HttpRequest request, HttpResponse original,
      MutableContent content) throws RewritingException {
    String mimeType = RewriterUtils.getMimeType(request, original);
    if (OS_TEMPLATE_MIME.equalsIgnoreCase(mimeType)) {
      content.setContent(converter.domToJson(content.getContent()));
      return true;
    }
    return false;
  }

  public static class GadgetHtmlVisitor implements DomWalker.Visitor {
    private final Converter converter;

    public GadgetHtmlVisitor(Converter converter) {
      this.converter = converter;
    }

    public VisitStatus visit(Gadget gadget, Node node) throws RewritingException {
      if (node.getNodeType() == Node.ELEMENT_NODE &&
          "div".equalsIgnoreCase(((Element)node).getTagName()) &&
          OS_TEMPLATE_MIME.equalsIgnoreCase(((Element)node).getAttribute("type")) &&
          (!Strings.isNullOrEmpty(((Element) node).getAttribute("id")) ||
           !Strings.isNullOrEmpty(((Element)node).getAttribute("name")))) {
        return VisitStatus.RESERVE_NODE;
      }
      return VisitStatus.BYPASS;
    }

    public boolean revisit(Gadget gadget, List<Node> nodes) throws RewritingException {
      if (!gadget.getAllFeatures().contains(OS_TEMPLATES_FEATURE_NAME)) {
        return false;
      }

      Document doc = nodes.get(0).getOwnerDocument();
      Element docElem = doc.getDocumentElement();
      if (docElem == null) {
        throw new RewritingException("Unexpected error, missing document element",
            HttpResponse.SC_INTERNAL_SERVER_ERROR);
      }

      Node head = DomUtil.getFirstNamedChildNode(doc.getDocumentElement(), "head");
      if (head == null) {
        throw new RewritingException("Unexpected error, could not find <head> node",
            HttpResponse.SC_INTERNAL_SERVER_ERROR);
      }

      StringBuilder preloadScript = new StringBuilder();

      for (Node node : nodes) {
        Element elem = (Element)node;
        String value = elem.getTextContent();
        String id = elem.getAttribute("name");
        if (Strings.isNullOrEmpty(id)) {
          id = elem.getAttribute("id");
        }

        preloadScript.append(String.format(PRELOAD_TPL, id, converter.domToJson(value)));
      }

      Node script = doc.createElement("script");
      script.setTextContent(preloadScript.toString());
      head.appendChild(script);

      return true;
    }
  }

  public static class Converter {
    public static final String NAME_KEY = "n";
    public static final String VALUE_KEY = "v";
    public static final String CHILDREN_KEY = "c";
    public static final String ATTRIBS_KEY = "a";
    public static final String ERROR_KEY = "e";

    private final GadgetHtmlParser parser;
    private final DOMImplementation domImpl;

    @Inject
    public Converter(GadgetHtmlParser parser, DOMImplementation domImpl) {
      this.parser = parser;
      this.domImpl = domImpl;
    }

    public String domToJson(String xml) {
      try {
        Document doc = domImpl.createDocument(null, null, null);
        Element container = doc.createElement("template");
        parser.parseFragment(xml, container);
        return jsonFromElement(container).toString();
      } catch (GadgetException e) {
        return jsonError("Gadget Exception: " + e).toString();
      } catch (JSONException e) {
        return jsonError("JSON Exception: " + e).toString();
      }
    }

    public JSONObject jsonFromElement(Element elem) throws JSONException {
      JSONObject json = new JSONObject();
      json.put(NAME_KEY, elem.getTagName());

      JSONArray attribs = new JSONArray();
      NamedNodeMap attribMap = elem.getAttributes();
      for (int i = 0; i < attribMap.getLength(); ++i) {
        JSONObject attrib = new JSONObject();
        Attr domAttrib = (Attr)attribMap.item(i);
        attrib.put(NAME_KEY, domAttrib.getNodeName());
        attrib.put(VALUE_KEY, domAttrib.getNodeValue());
        attribs.put(attrib);
      }
      json.put(ATTRIBS_KEY, attribs);

      JSONArray children = new JSONArray();
      for (Node child = elem.getFirstChild(); child != null; child = child.getNextSibling()) {
        switch (child.getNodeType()) {
        case Node.TEXT_NODE:
          children.put(child.getNodeValue());
          break;
        case Node.DOCUMENT_NODE:
        case Node.ELEMENT_NODE:
          children.put(jsonFromElement((Element)child));
          break;
        default:
          // No other node types are supported.
          break;
        }
      }
      json.put(CHILDREN_KEY, children);

      return json;
    }

    private JSONObject jsonError(String err) {
      JSONObject json = new JSONObject();
      try {
        json.put(ERROR_KEY, err);
      } catch (JSONException e) {
        // Doesn't happen.
      }
      return json;
    }
  }
}
