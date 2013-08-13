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

import org.apache.commons.lang3.StringUtils;
import org.apache.shindig.common.xml.DomUtil;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.parse.GadgetHtmlParser;
import org.apache.shindig.gadgets.parse.SocialDataTags;
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

import com.google.inject.Inject;
import com.google.inject.Singleton;
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
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.io.StringReader;
import java.util.Stack;

/**
 * Supports parsing of social markup blocks inside gadget content.
 * &lt;script&gt; elements with types of either "text/os-template"
 * or "text/os-data" are parsed inline into contained DOM hierarchies
 * for subsequent processing by the pipeline and template rewriters.
 */
@Singleton
public class NekoSimplifiedHtmlParser extends GadgetHtmlParser {

  private static final HTMLElements.Element OSML_TEMPLATE_ELEMENT;
  private static final HTMLElements.Element OSML_DATA_ELEMENT;

  static {
    HTMLElements.Element unknown = HTMLElements.getElement(HTMLElements.UNKNOWN);
    OSML_TEMPLATE_ELEMENT = new HTMLElements.Element(unknown.code,
        SocialDataTags.OSML_TEMPLATE_TAG, unknown.flags, HTMLElements.BODY, unknown.closes);
    // Passing parent in constructor is ignored.
    // Only allow template tags in BODY
    OSML_TEMPLATE_ELEMENT.parent =
        new HTMLElements.Element[]{HTMLElements.getElement(HTMLElements.BODY)};

    // data tags are allowed in BODY only, since Neko disallows HEAD elements from
    // having child elements of their own.
    OSML_DATA_ELEMENT = new HTMLElements.Element(unknown.code,
        SocialDataTags.OSML_TEMPLATE_TAG, unknown.flags, HTMLElements.BODY, unknown.closes);
    OSML_DATA_ELEMENT.parent = new HTMLElements.Element[]{
        HTMLElements.getElement(HTMLElements.BODY)};
  }


  @Inject
  public NekoSimplifiedHtmlParser(DOMImplementation documentFactory) {
    super(documentFactory);
  }

  @Override
  protected Document parseDomImpl(String source) throws GadgetException {
    DocumentHandler handler;

    HTMLConfiguration config = newConfiguration();
    try {
      handler = parseHtmlImpl(source, config, new NormalizingTagBalancer());
    } catch (IOException ioe) {
      return null;
    }

    Document document = handler.getDocument();
    document.appendChild(DomUtil.getFirstNamedChildNode(handler.getFragment(), "html"));
    fixNekoWeirdness(document);
    return document;
  }

  @Override
  protected DocumentFragment parseFragmentImpl(String source) throws GadgetException {
    DocumentHandler handler;

    HTMLConfiguration config = newConfiguration();
    // http://cyberneko.org/html/features/balance-tags/document-fragment
    // deprecated http://cyberneko.org/html/features/document-fragment
    config.setFeature("http://cyberneko.org/html/features/balance-tags/document-fragment", true);
    config.setProperty("http://cyberneko.org/html/properties/balance-tags/fragment-context-stack",
        new QName[]{new QName(null, "HTML", "HTML", null), new QName(null, "BODY", "BODY", null)});

    try {
      handler = parseHtmlImpl(source, config, new NekoPatchTagBalancer());
    } catch (IOException ioe) {
      return null;
    }

    return handler.getFragment();
  }

  /**
   * Parse HTML source.
   *
   * @return a document handler containing the parsed source
   */
  private DocumentHandler parseHtmlImpl(String source, HTMLConfiguration config,
      NormalizingTagBalancer tagBalancer)
      throws IOException {

    HTMLScanner htmlScanner = new HTMLScanner();
    tagBalancer.setScanner(htmlScanner);

    DocumentHandler handler = newDocumentHandler(source);

    NamespaceBinder namespaceBinder = new NamespaceBinder();
    namespaceBinder.setDocumentHandler(handler);
    namespaceBinder.setDocumentSource(tagBalancer);
    namespaceBinder.reset(config);
    tagBalancer.setDocumentHandler(namespaceBinder);

    // Order of filter is Scanner -> OSMLFilter -> Tag Balancer
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

  private void fixNekoWeirdness(Document document) {
    // Neko as of versions > 1.9.13 stuffs all leading <script> nodes into <head>.
    // This breaks all sorts of assumptions in gadgets, notably the existence of document.body.
    // We can't tell Neko to avoid putting <script> into <head> however, since gadgets
    // like <Content><script>...</script><style>...</style> will break due to both
    // <script> and <style> ending up in <body> -- at which point Neko unceremoniously
    // drops the <style> (and <link>) elements.
    // Therefore we just search for <script> elements in <head> and stuff them all into
    // the top of <body>.
    // This method assumes a normalized document as input.
    Node html = DomUtil.getFirstNamedChildNode(document, "html");
    if (html.getNextSibling() != null &&
        html.getNextSibling().getNodeName().equalsIgnoreCase("html")) {
      // if a doctype is specified, then the desired root <html> node is wrapped by an <HTML> node
      // Pull out the <html> root.
      html = html.getNextSibling();
    }
    Node head = DomUtil.getFirstNamedChildNode(html, "head");
    if (head == null) {
      head = document.createElement("head");
      html.insertBefore(head, html.getFirstChild());
    }
    NodeList headNodes = head.getChildNodes();
    Stack<Node> headScripts = new Stack<Node>();
    for (int i = 0; i < headNodes.getLength(); ++i) {
      Node headChild = headNodes.item(i);
      if (headChild.getNodeName().equalsIgnoreCase("script")) {
        headScripts.add(headChild);
      }
    }

    // Remove from head, add to top of <body> in <head> order.
    Node body = DomUtil.getFirstNamedChildNode(html, "body");
    if (body == null) {
      body = document.createElement("body");
      html.insertBefore(body, head.getNextSibling());
    }
    Node bodyFirst = body.getFirstChild();
    while (!headScripts.isEmpty()) {
      Node headScript = headScripts.pop();
      head.removeChild(headScript);
      body.insertBefore(headScript, bodyFirst);
      bodyFirst = headScript;
    }
  }

  protected HTMLConfiguration newConfiguration() {
    HTMLConfiguration config = new HTMLConfiguration();
    // Maintain original case for elements and attributes
    config.setProperty("http://cyberneko.org/html/properties/names/elems", "match");
    config.setProperty("http://cyberneko.org/html/properties/names/attrs", "no-change");
    // Get notified of entity and character references
    config.setFeature("http://apache.org/xml/features/scanner/notify-char-refs", true);
    config.setFeature("http://cyberneko.org/html/features/scanner/notify-builtin-refs", true);
    config.setFeature("http://xml.org/sax/features/namespaces", true);
    return config;
  }

  protected DocumentHandler newDocumentHandler(String source) {
    return new DocumentHandler(source);
  }

  /** Handler for XNI events from Neko */
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
      flushTextBuffer();

      // Add comments as comment nodes - needed to support sanitization
      // of SocialMarkup-parsed content
      Node comment = getDocument().createComment(new String(text.ch, text.offset, text.length));
      appendChild(comment);
    }

    public void processingInstruction(String s, XMLString xmlString, Augmentations augs)
        throws XNIException {
      // No-op
    }

    public void startElement(QName qName, XMLAttributes xmlAttributes, Augmentations augs)
        throws XNIException {
      Element element = startElementImpl(qName, xmlAttributes);
      // Not an empty element, so push on the stack
      elementStack.push(element);
    }

    public void emptyElement(QName qName, XMLAttributes xmlAttributes, Augmentations augs)
        throws XNIException {
      startElementImpl(qName, xmlAttributes);
    }

    /** Flush any existing text content to the document.  Call this before appending any nodes. */
    protected void flushTextBuffer() {
      if (builder.length() > 0) {
        appendChild(document.createTextNode(builder.toString()));
        builder.setLength(0);
      }
    }

    /** Create an Element in the DOM */
    private Element startElementImpl(QName qName, XMLAttributes xmlAttributes) {
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
                  sb.append(':');
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
      flushTextBuffer();
      elementStack.pop();
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

    private void appendChild(Node node) {
      elementStack.peek().appendChild(node);
    }
  }

  /**
   * Used when parsing document fragments to correct a bug in Neko 1.9.13. We use the
   * http://cyberneko.org/html/properties/balance-tags/fragment-context-stack
   * property of Neko to force the fragment to be parsed as if it were already container in a body
   * tag. This doesnt quite work together as without this fix it will still introduce head tags
   * if the first parsed tags are allowed in a head tag.
   * See https://sourceforge.net/tracker/?func=detail&atid=952178&aid=2870180&group_id=195122
   */
  private static class NekoPatchTagBalancer extends NormalizingTagBalancer {

    /**
     * Override the document start to record whether HTML, HEAD or BODY have been seen
     */
    @Override
    public void startDocument(XMLLocator locator, String encoding,
        NamespaceContext nscontext, Augmentations augs)
        throws XNIException {

      super.startDocument(locator, encoding, nscontext, augs);
      for (int i = fElementStack.top - 1; i >= 0; i--) {
        fSeenAnything = true;
        if (fElementStack.data[i].element.code == HTMLElements.HTML) {
          fSeenRootElement = true;
        }
        if (fElementStack.data[i].element.code == HTMLElements.HEAD) {
          fSeenHeadElement = true;
        }
        if (fElementStack.data[i].element.code == HTMLElements.BODY) {
          fSeenBodyElement = true;
        }
      }
    }
  }

  /**
   * Subclass of Neko's tag balancer that
   * - Normalizes the case of forced html, head and body tags when they don't exist in the original
   * content.
   * -
   */
  private static class NormalizingTagBalancer extends HTMLTagBalancer {

    private StringBuilder scriptContent;

    private HTMLScanner scanner;

    public NormalizingTagBalancer() {
    }

    public void setScanner(HTMLScanner scanner) {
      this.scanner = scanner;
    }

    @Override
    public void startElement(QName elem, XMLAttributes attrs, Augmentations augs)
        throws XNIException {
      // Normalize the case of forced-elements to lowercase for backward compatability
      if (!fSeenRootElement && elem.rawname.equalsIgnoreCase("html")) {
        elem.localpart = "html";
        elem.rawname = "html";
      } else if (!fSeenHeadElement && elem.rawname.equalsIgnoreCase("head")) {
        elem.localpart = "head";
        elem.rawname = "head";
      } else if (!fSeenBodyElement && elem.rawname.equalsIgnoreCase("body")) {
        elem.localpart = "body";
        elem.rawname = "body";
      }

      super.startElement(elem, attrs, augs);
    }


  }
}
