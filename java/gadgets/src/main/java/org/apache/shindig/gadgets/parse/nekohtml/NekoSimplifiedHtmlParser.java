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

import org.apache.commons.lang.StringUtils;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.parse.GadgetHtmlParser;
import org.apache.shindig.gadgets.parse.HtmlSerialization;
import org.apache.xerces.xni.Augmentations;
import org.apache.xerces.xni.NamespaceContext;
import org.apache.xerces.xni.QName;
import org.apache.xerces.xni.XMLAttributes;
import org.apache.xerces.xni.XMLDocumentHandler;
import org.apache.xerces.xni.XMLLocator;
import org.apache.xerces.xni.XMLResourceIdentifier;
import org.apache.xerces.xni.XMLString;
import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.parser.XMLDocumentSource;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.cyberneko.html.HTMLConfiguration;
import org.cyberneko.html.HTMLElements;
import org.cyberneko.html.HTMLEntities;
import org.cyberneko.html.HTMLScanner;
import org.cyberneko.html.HTMLTagBalancer;
import org.cyberneko.html.filters.NamespaceBinder;
import org.w3c.dom.DOMException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.IOException;
import java.io.StringReader;
import java.util.Set;
import java.util.Stack;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Neko based DOM parser that concatentates elements which we dont care about into
 * text nodes to keep DOM model simplified. Much of this code is based on
 * org.cyberneko.html.filters.Writer
 *
 * TODO: Create a reusable instance in ThreadLocal
 */
@Singleton
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
    DocumentHandler handler;

    try {
      handler = parseHtmlImpl(source);
    } catch (IOException ioe) {
      return null;
    }

    Document document = handler.getDocument();
    DocumentFragment fragment = handler.getFragment();
    normalizeFragment(document, fragment);
    return document;
  }

  @Override
  protected DocumentFragment parseFragmentImpl(String source) throws GadgetException {
    DocumentHandler handler;

    try {
      handler = parseHtmlImpl(source);
    } catch (IOException ioe) {
      return null;
    }

    return handler.getFragment();
  }

  /**
   * Parse HTML source.
   * @return a document handler containing the parsed source
   */
  private DocumentHandler parseHtmlImpl(String source) throws IOException {
    HTMLConfiguration config = newConfiguration();

    HTMLScanner htmlScanner = new HTMLScanner();
    HTMLTagBalancer tagBalancer = new HTMLTagBalancer() {
      @Override
      protected HTMLElements.Element getElement(String name) {
        // Neko's implementation of this method strips off namespace prefixes
        // before calling HTMLElements.getElement().
        // This breaks elements like "os:Html", is slower, and has no obvious benefit.
        return HTMLElements.getElement(name);
      }
    };

    DocumentHandler handler = newDocumentHandler(source, htmlScanner);

    if (config.getFeature("http://xml.org/sax/features/namespaces")) {
      NamespaceBinder namespaceBinder = new NamespaceBinder();
      namespaceBinder.setDocumentHandler(handler);
      namespaceBinder.setDocumentSource(tagBalancer);
      namespaceBinder.reset(config);
      tagBalancer.setDocumentHandler(namespaceBinder);
    } else {
      tagBalancer.setDocumentHandler(handler);
    }

    tagBalancer.setDocumentSource(htmlScanner);
    htmlScanner.setDocumentHandler(tagBalancer);

    tagBalancer.reset(config);
    htmlScanner.reset(config);

    XMLInputSource inputSource = new XMLInputSource(null, null, null);
    inputSource.setEncoding("UTF-8");
    inputSource.setCharacterStream(new StringReader(source));
    htmlScanner.setInputSource(inputSource);
    htmlScanner.scanDocument(true);
    return handler;
  }

  protected HTMLConfiguration newConfiguration() {
    HTMLConfiguration config = new HTMLConfiguration();
    // Maintain original case for elements and attributes
    config.setProperty("http://cyberneko.org/html/properties/names/elems", "match");
    config.setProperty("http://cyberneko.org/html/properties/names/attrs", "no-change");
    // Parse as fragment.
    config.setFeature("http://cyberneko.org/html/features/balance-tags/document-fragment", true);
    // Get notified of entity and character references
    config.setFeature("http://apache.org/xml/features/scanner/notify-char-refs", true);
    config.setFeature("http://cyberneko.org/html/features/scanner/notify-builtin-refs", true);
    return config;
  }

  protected DocumentHandler newDocumentHandler(String source, HTMLScanner scanner) {
    return new DocumentHandler(source);
  }

  /**
   * Is the given element important enough to preserve in the DOM?
   */
  protected boolean isElementImportant(QName qName) {
    return elements.contains(qName.rawname.toLowerCase());
  }

  /**
   * Handler for XNI events from Neko
   */
  protected class DocumentHandler implements XMLDocumentHandler {
    private final Stack<Node> elementStack = new Stack<Node>();
    private final StringBuilder builder;
    private boolean inEntity = false;


    private DocumentFragment documentFragment;
    private Document document;

    public DocumentHandler(String content) {
      builder = new StringBuilder(content.length() / 10);
    }

    public DocumentFragment getFragment() {
      return documentFragment;
    }

    public Document getDocument() {
      return document;
    }

    public void startDocument(XMLLocator xmlLocator, String encoding,
                              NamespaceContext namespaceContext, Augmentations augs)
        throws XNIException {
      document = documentFactory.createDocument(null, null, null);
      elementStack.clear();
      documentFragment = document.createDocumentFragment();
      elementStack.push(documentFragment);
    }

    public void xmlDecl(String version, String encoding, String standalone, Augmentations augs)
        throws XNIException {
      // Dont really do anything with this
      builder.append("<?xml");
      if (version != null) {
        builder.append(" version=\"").append(version).append('\"');
      }
      if (encoding != null) {
        builder.append(" encoding=\"").append(encoding).append('\"');
      }
      if (standalone != null) {
        builder.append(" standalone=\"").append(standalone).append('\"');
      }
      builder.append('>');
    }

    public void doctypeDecl(String rootElement, String publicId, String systemId,
        Augmentations augs) throws XNIException {
      document = documentFactory.createDocument(null, null,
          documentFactory.createDocumentType(rootElement, publicId, systemId));
      elementStack.clear();
      documentFragment = document.createDocumentFragment();
      elementStack.push(documentFragment);
    }

    public void comment(XMLString text, Augmentations augs) throws XNIException {
      builder.append("<!--").append(text.ch, text.offset, text.length).append("-->");
    }

    public void processingInstruction(String s, XMLString xmlString, Augmentations augs)
        throws XNIException {
      // No-op
    }

    public void startElement(QName qName, XMLAttributes xmlAttributes, Augmentations augs)
        throws XNIException {
      if (isElementImportant(qName)) {
        Element element = startImportantElement(qName, xmlAttributes);
        // Not an empty element, so push on the stack
        elementStack.push(element);
      } else {
        startUnimportantElement(qName, xmlAttributes);
      }
    }

    public void emptyElement(QName qName, XMLAttributes xmlAttributes, Augmentations augs)
        throws XNIException {
      if (isElementImportant(qName)) {
        startImportantElement(qName, xmlAttributes);
      } else {
        startUnimportantElement(qName, xmlAttributes);
      }
    }

    /**
     * Flush any existing text content to the document.  Call this before appending any
     * nodes.
     */
    protected void flushTextBuffer() {
      if (builder.length() > 0) {
        appendChild(document.createTextNode(builder.toString()));
        builder.setLength(0);
      }
    }

    /** Write an unimportant element into content as raw text */
    private void startUnimportantElement(QName qName, XMLAttributes xmlAttributes) {
      builder.append('<').append(qName.rawname);
      for (int i = 0; i < xmlAttributes.getLength(); i++) {
        String attributeName = xmlAttributes.getLocalName(i);
        builder.append(' ').append(attributeName).append("=\"");
        appendAttributeValue(xmlAttributes.getValue(i),
            HtmlSerialization.isUrlAttribute(qName, attributeName));
        builder.append('\"');
      }
      builder.append('>');
    }

    /** Create an Element in the DOM for an important element */
    private Element startImportantElement(QName qName, XMLAttributes xmlAttributes) {
      flushTextBuffer();

      Element element;
      // Preserve XML namespace if present
      if (qName.uri != null) {
        element = document.createElementNS(qName.uri, qName.rawname);
      } else {
        element = document.createElement(qName.rawname);
      }

      for (int i = 0; i < xmlAttributes.getLength(); i++) {
        if (xmlAttributes.getURI(i) != null) {
          element.setAttributeNS(xmlAttributes.getURI(i), xmlAttributes.getQName(i),
              xmlAttributes.getValue(i));
        } else {
          try {
            element.setAttribute(xmlAttributes.getLocalName(i), xmlAttributes
                .getValue(i));
          } catch (DOMException e) {
            switch (e.code) {
            case DOMException.INVALID_CHARACTER_ERR:
              StringBuilder sb = new StringBuilder(e.getMessage());
              sb.append("Around ...<");
              if (qName.prefix != null) {
                sb.append(qName.prefix);
                sb.append(":");
              }
              sb.append(qName.localpart);
              for (int j = 0; j < xmlAttributes.getLength(); j++) {
                if (StringUtils.isNotBlank(xmlAttributes.getLocalName(j))
                    && StringUtils.isNotBlank(xmlAttributes.getValue(j))) {
                  sb.append(' ');
                  sb.append(xmlAttributes.getLocalName(j));
                  sb.append("=\"");
                  sb.append(xmlAttributes.getValue(j)).append('\"');
                }
              }
              sb.append("...");
              throw new DOMException(DOMException.INVALID_CHARACTER_ERR, sb.toString());
            default:
              throw e;
            }
          }
        }
      }
      appendChild(element);
      return element;
    }

    private void appendAttributeValue(String text, boolean isUrl) {
      for (int i = 0; i < text.length(); i++) {
        char c = text.charAt(i);
        if (c == '"') {
          builder.append("&quot;");
        } else if (c == '&' && !isUrl) {
          builder.append("&amp;");
        } else {
          builder.append(c);
        }
      }
    }

    public void startGeneralEntity(String name, XMLResourceIdentifier id, String encoding,
        Augmentations augs) throws XNIException {
      if (name.startsWith("#")) {
        try {
          boolean hex = name.startsWith("#x");
          int offset = hex ? 2 : 1;
          int base = hex ? 16 : 10;
          int value = Integer.parseInt(name.substring(offset), base);
          String entity = HTMLEntities.get(value);
          if (entity != null) {
            name = entity;
          }
        }
        catch (NumberFormatException e) {
          // ignore
        }
      }
      printEntity(name);
      inEntity = true;
    }

    private void printEntity(String name) {
      builder.append('&');
      builder.append(name);
      builder.append(';');
    }

    public void textDecl(String s, String s1, Augmentations augs) throws XNIException {
      builder.append(s);
    }

    public void endGeneralEntity(String s, Augmentations augs) throws XNIException {
      inEntity = false;
    }

    public void characters(XMLString text, Augmentations augs) throws XNIException {
      if (inEntity) {
        return;
      }
      builder.append(text.ch, text.offset, text.length);
    }

    public void ignorableWhitespace(XMLString text, Augmentations augs) throws XNIException {
      builder.append(text.ch, text.offset, text.length);
    }

    public void endElement(QName qName, Augmentations augs) throws XNIException {
      if (isElementImportant(qName)) {
        flushTextBuffer();
        elementStack.pop();
      } else {
        builder.append("</").append(qName.rawname).append('>');
      }
    }

    public void startCDATA(Augmentations augs) throws XNIException {
      //No-op
    }

    public void endCDATA(Augmentations augs) throws XNIException {
      //No-op
    }

    public void endDocument(Augmentations augs) throws XNIException {
      flushTextBuffer();
      elementStack.pop();
    }

    public void setDocumentSource(XMLDocumentSource xmlDocumentSource) {
    }

    public XMLDocumentSource getDocumentSource() {
      return null;
    }
    
    protected final void appendChild(Node node) {
      elementStack.peek().appendChild(node);
    }
  }
}
