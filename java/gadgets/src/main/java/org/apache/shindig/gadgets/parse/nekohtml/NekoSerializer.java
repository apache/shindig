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
package org.apache.shindig.gadgets.parse.nekohtml;

import org.apache.shindig.gadgets.parse.HtmlSerializer;

import org.cyberneko.html.HTMLElements;
import org.cyberneko.html.HTMLEntities;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.io.StringWriter;

/**
 * This parser does not try to escape entities in text content as it expects the parser
 * to have retained the original entity references rather than its resolved form in text nodes
 */
public class NekoSerializer extends HtmlSerializer
{
  public NekoSerializer() {
  }

  @Override
  public String serializeImpl(Document doc) {
    try {
      StringWriter sw = createWriter(doc);
      if (doc.getDoctype() != null) {
        outputDocType(doc.getDoctype(), sw);
      }
      serialize(doc, sw);
      return sw.toString();
    } catch (IOException ioe) {
      return null;
    }
  }

  public static void serialize(Node n, Appendable output) throws IOException {
    switch (n.getNodeType()) {
      case Node.CDATA_SECTION_NODE: {
        break;
      }
      case Node.COMMENT_NODE: {
        output.append("<!--").append(n.getNodeValue()).append("-->");
        break;
      }
      case Node.DOCUMENT_NODE: {
        serialize(((Document)n).getDocumentElement(), output);
        break;
      }
      case Node.ELEMENT_NODE: {
        Element elem = (Element)n;
        HTMLElements.Element htmlElement =
            HTMLElements.getElement(elem.getNodeName());
        NodeList children = elem.getChildNodes();
        printStartElement(elem, output);
        for (int i = 0; i < children.getLength(); i++) {
          serialize(children.item(i), output);
        }
        if (!htmlElement.isEmpty()) {
          output.append("</").append(elem.getNodeName()).append('>');
        }
        break;
      }
      case Node.ENTITY_REFERENCE_NODE: {
        output.append("&").append(n.getNodeName()).append(";");
        break;
      }
      case Node.TEXT_NODE: {
        output.append(n.getTextContent());
        break;
      }
    }
  }

  public static void outputDocType(DocumentType docType, Appendable output) throws IOException {
    output.append("<!DOCTYPE ");
    // Use this so name matches case for XHTML
    output.append(docType.getOwnerDocument().getDocumentElement().getNodeName());
    if (docType.getPublicId() != null && docType.getPublicId().length() > 0) {
      output.append(" ");
      output.append("PUBLIC ").append('"').append(docType.getPublicId()).append('"');
    }
    if (docType.getSystemId() != null && docType.getSystemId().length() > 0) {
      output.append(" ");
      output.append('"').append(docType.getSystemId()).append('"');
    }
    output.append(">\n");
  }

  public static void printStartElement(Element elem, Appendable output) throws IOException {
    output.append("<").append(elem.getTagName());
    NamedNodeMap attributes = elem.getAttributes();
    for (int i = 0; i < attributes.getLength(); i++) {
      Attr attr = (Attr)attributes.item(i);
      output.append(' ').append(attr.getNodeName());
      if (attr.getNodeValue() != null &&
          attr.getNodeValue().length() > 0) {
        output.append("=\"");
        printAttributeValue(attr.getNodeValue(), output);
        output.append('"');
      }
    }
    output.append(">");
  }

  public static void printAttributeValue(String text, Appendable output) throws IOException {
    int length = text.length();
    for (int j = 0; j < length; j++) {
      char c = text.charAt(j);
      if (c == '"') {
        output.append("&quot;");
      } else {
        output.append(c);
      }
    }
  }

  public static void printEscapedText(CharSequence text, Appendable output) throws IOException {
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      String entity = HTMLEntities.get(c);
      if (entity != null) {
        output.append('&').append(entity).append(";");
      } else {
        output.append(c);
      }
    }
  }
}
