/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.gadgets.parse.nekohtml;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.apache.shindig.gadgets.parse.DomUtil;
import org.apache.shindig.gadgets.parse.GadgetHtmlParser;
import org.apache.shindig.gadgets.parse.HtmlSerializer;
import org.apache.xerces.xni.*;
import org.apache.xerces.xni.parser.XMLDocumentSource;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.apache.xml.serialize.HTMLSerializer;
import org.apache.xml.serialize.OutputFormat;
import org.cyberneko.html.HTMLEventInfo;
import org.cyberneko.html.HTMLScanner;
import org.cyberneko.html.HTMLTagBalancer;
import org.w3c.dom.*;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.Set;
import java.util.Stack;

/**
 * Creates a greatly simplified DOM model that contains elements for only the specified
 * element set and creates unescaped text nodes for all other content.
 * It requires special serialization to prevent escaping of text nodes but behaves like a
 * regular DOM in all other respects. Only element types which are produced are balanced.
 */
public class NekoSimplifiedHtmlParser extends GadgetHtmlParser {

  private static final Set<String> elements =
      ImmutableSet.of("html", "body", "head", "link", "img", "style", "script", "embed");

  private final DOMImplementation documentFactory;

  @Inject
  public NekoSimplifiedHtmlParser(DOMImplementation documentFactory) {
    this.documentFactory = documentFactory;
  }

  @Override
  protected Document parseDomImpl(String source) {

    HTMLScanner htmlScanner = new HTMLScanner();
    HTMLTagBalancer tagBalancer = new HTMLTagBalancer();
    DocumentHandler handler = new DocumentHandler(source);
    tagBalancer.setDocumentHandler(handler);
    htmlScanner.setDocumentHandler(tagBalancer);
    tagBalancer.setFeature("http://cyberneko.org/html/features/augmentations", true);
    htmlScanner.setFeature("http://cyberneko.org/html/features/augmentations", true);

    XMLInputSource inputSource = new XMLInputSource(null, null, null);
    inputSource.setEncoding("UTF-8");
    inputSource.setCharacterStream(new StringReader(source));
    try {
      htmlScanner.setInputSource(inputSource);
      htmlScanner.scanDocument(true);
      Document document = handler.getDocument();
      DocumentFragment fragment = handler.getFragment();
      Node htmlNode = DomUtil.getFirstNamedChildNode(fragment, "HTML");
      if (htmlNode != null) {
        document.appendChild(htmlNode);
      } else {
        Node root = document.appendChild(document.createElement("HTML"));
        root.appendChild(fragment);
      }
      HtmlSerializer.attach(document, new Serializer(), source);
      return document;
    } catch (IOException ioe) {
      return null;
    }
  }


  /**
   * Handler for XNI events from Neko
   */
  private class DocumentHandler implements XMLDocumentHandler {
    private final List<Integer> lines;
    private final Stack<Node> elementStack = new Stack<Node>();
    private final int[] startCharOffsets;
    private final int[] lastCharOffsets;
    private DocumentFragment documentFragment;
    private Document document;
    private final String content;

    public DocumentHandler(String content) {
      this.content = content;
      // Populate lines
      lines = Lists.newArrayListWithExpectedSize(content.length() / 30);
      lines.add(0);
      for (int i = 0; i < content.length(); i++) {
        char c = content.charAt(i);
        if (c == '\n' || c == '\r') {
          if (i + 1 < content.length() && (c == '\r' && content.charAt(i+1) == '\n')) {
            i++;
            lines.add(i);
          } else {
            lines.add(i);
          }
        }
      }
      startCharOffsets = new int[]{-1,-1};
      lastCharOffsets = new int[]{-1,-1};
    }

    public DocumentFragment getFragment() {
      return documentFragment;
    }

    public Document getDocument() {
      return document;
    }

    private HTMLEventInfo getEventInfo(Augmentations augmentations) {
      HTMLEventInfo htmlEventInfo =
          (HTMLEventInfo) augmentations.getItem("http://cyberneko.org/html/features/augmentations");
      return htmlEventInfo;
    }

    private String getUnstructuredString(int[] start, int[] end) {
      if (start[0] == -1) return "";

      int charStart = start[0];
      int charEnd;
      if (end[0] == -1) {
        charEnd = start[1];
      } else {
        charEnd = end[1];
      }
      String s = content.substring(charStart, charEnd);
      return s;
    }

    private void recordStartEnd(HTMLEventInfo info, int[] offsets) {
      offsets[0] = lines.get(info.getBeginLineNumber() - 1) + info.getBeginColumnNumber() - 1;
      offsets[1] = lines.get(info.getEndLineNumber() - 1) + info.getEndColumnNumber() - 1;
    }

    public void handleEvent(boolean shouldClose, Object content, Augmentations augs) {
      HTMLEventInfo info = getEventInfo(augs);
      if (info.isSynthesized()) {
        // NOTE! Remove this to balance syntesized close tags
        if (!shouldClose) return;
        // Must close with existing content
        String unstructured = getUnstructuredString(startCharOffsets, lastCharOffsets);
        elementStack.peek().appendChild(document.createTextNode(unstructured));
        startCharOffsets[0] = -1;
        lastCharOffsets[0] = -1;
        if (content != null) {
          elementStack.peek().appendChild(document.createTextNode(content.toString()));
        }
      } else {
        if (shouldClose) {
          String unstructured = getUnstructuredString(startCharOffsets, lastCharOffsets);
          elementStack.peek().appendChild(document.createTextNode(unstructured));
          startCharOffsets[0] = -1;
          lastCharOffsets[0] = -1;
        } else if (startCharOffsets[0] == -1) {
          recordStartEnd(info, startCharOffsets);
          lastCharOffsets[0] = -1;
        } else {
          recordStartEnd(info, lastCharOffsets);
        }
      }
    }

    private void trace(String prefix, Augmentations augmentations) {
      HTMLEventInfo info = getEventInfo(augmentations);
      String text = "";
      if (!info.isSynthesized()) {
        int[] startEnd = new int[2];
        recordStartEnd(info, startEnd);
        text = content.substring(startEnd[0], startEnd[1]);
        text = text.replaceAll("\n", "\\n");
        text = text.replaceAll("\r", "\\r");
      }
      System.out.println("Event " + prefix + info.toString() + " -> " + text);
    }

    public void startDocument(XMLLocator xmlLocator, String encoding,
                              NamespaceContext namespaceContext, Augmentations augs) throws XNIException {
      document = documentFactory.createDocument(null, null, null);
      elementStack.clear();
      documentFragment = document.createDocumentFragment();
      elementStack.push(documentFragment);
      //trace("StartDoc", augs);
    }

    public void xmlDecl(String version, String encoding, String standalone, Augmentations augs) throws XNIException {
      //trace("xmlDecl", augs);
      handleEvent(false, null, augs);
    }

    public void doctypeDecl(String rootElement, String publicId, String systemId, Augmentations augs) throws XNIException {
      // Recreate the document with the specific doctype
      document = documentFactory.createDocument(null, null,
          documentFactory.createDocumentType(rootElement, publicId, systemId));
      elementStack.clear();
      documentFragment = document.createDocumentFragment();
      elementStack.push(documentFragment);
      //trace("docTypeDecl", augs);
      handleEvent(false, null, augs);
    }

    public void comment(XMLString xmlString, Augmentations augs) throws XNIException {
      //trace("comment", augs);
      handleEvent(false, xmlString, augs);
      //trackInfo(augs);
    }

    public void processingInstruction(String s, XMLString xmlString, Augmentations augs) throws XNIException {
      //trace("PI", augs);
      handleEvent(false, xmlString, augs);
    }

    public void startElement(QName qName, XMLAttributes xmlAttributes, Augmentations augs) throws XNIException {
      //trace("StartElem(" + qName.rawname + ")", augs);
      if (elements.contains(qName.rawname.toLowerCase())) {
        handleEvent(true, null, augs);
        Element element = document.createElement(qName.rawname);
        for (int i = 0; i < xmlAttributes.getLength(); i++) {
          element.setAttribute(xmlAttributes.getLocalName(i) , xmlAttributes.getValue(i));
        }
        elementStack.peek().appendChild(element);
        elementStack.push(element);
      } else {
        handleEvent(false, null, augs);
      }
    }

    public void emptyElement(QName qName, XMLAttributes xmlAttributes, Augmentations augs) throws XNIException {
      //trace("EmptyElemm(" + qName.rawname + ")", augs);
      if (elements.contains(qName.rawname.toLowerCase())) {
        handleEvent(true, null, augs);
        Element element = document.createElement(qName.rawname);
        for (int i = 0; i < xmlAttributes.getLength(); i++) {
          element.setAttribute(xmlAttributes.getLocalName(i) , xmlAttributes.getValue(i));
        }
        elementStack.peek().appendChild(element);
      } else {
        handleEvent(false, null, augs);
      }

    }

    public void startGeneralEntity(String s, XMLResourceIdentifier xmlResourceIdentifier, String s1, Augmentations augs) throws XNIException {
      //trace("StartEntity(" + s + ")", augs);
      handleEvent(false, null, augs);
    }

    public void textDecl(String s, String s1, Augmentations augs) throws XNIException {
      //trace("Textdecl(" + s + ")", augs);
      handleEvent(false, null, augs);
    }

    public void endGeneralEntity(String s, Augmentations augs) throws XNIException {
      //trace("EndEntity(" + s + ")", augs);
      handleEvent(false, null, augs);
    }

    public void characters(XMLString xmlString, Augmentations augs) throws XNIException {
      handleEvent(false, xmlString, augs);
    }

    public void ignorableWhitespace(XMLString xmlString, Augmentations augs) throws XNIException {
      //trace("Whitespace", augs);
      handleEvent(false, xmlString, augs);
      //trackInfo(augs);
    }

    public void endElement(QName qName, Augmentations augs) throws XNIException {
      //trace("EndElem(" + qName.rawname + ")", augs);
      if (elements.contains(qName.rawname.toLowerCase())) {
        handleEvent(true, null, augs);
        // FIXME - Balancer
        elementStack.pop();
      } else {
        handleEvent(false, "</" + qName.rawname + ">", augs);
      }
    }

    public void startCDATA(Augmentations augs) throws XNIException {
      //trace("startCData", augs);
      handleEvent(false, null, augs);
    }

    public void endCDATA(Augmentations augs) throws XNIException {
      //trace("endCData", augs);
      handleEvent(false, null, augs);
    }

    public void endDocument(Augmentations augs) throws XNIException {
      //trace("endDoc", augs);
      handleEvent(false, null, augs);
    }

    public void setDocumentSource(XMLDocumentSource xmlDocumentSource) {
    }

    public XMLDocumentSource getDocumentSource() {
      return null;
    }
  }

  static class Serializer extends HtmlSerializer {
    
    static final OutputFormat outputFormat = new OutputFormat();
    static {
      outputFormat.setPreserveSpace(true);
      outputFormat.setPreserveEmptyAttributes(false);
    }

    public String serializeImpl(Document doc) {
      StringWriter sw = createWriter(doc);
      HTMLSerializer serializer = new HTMLSerializer(sw, outputFormat) {
        // Overridden to prevent escaping of literal text
        @Override
        protected void characters(String s) throws IOException {
          this.content();
          this._printer.printText(s);
        }
      };
      try {
        serializer.serialize(doc);
        return sw.toString();
      } catch (IOException ioe) {
        return null;
      }
    }
  }
}
