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
package org.apache.shindig.gadgets.parse;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Provider;

import org.apache.shindig.common.cache.Cache;
import org.apache.shindig.common.cache.CacheProvider;
import org.apache.shindig.common.logging.i18n.MessageKeys;
import org.apache.shindig.common.util.HashUtil;
import org.apache.shindig.gadgets.GadgetException;
import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Parser for arbitrary HTML content
 */

public abstract class GadgetHtmlParser {

  //class name for logging purpose
  private static final String classname = GadgetHtmlParser.class.getName();
  private static final Logger LOG = Logger.getLogger(classname,MessageKeys.MESSAGES);


  public static final String PARSED_DOCUMENTS = "parsedDocuments";
  public static final String PARSED_FRAGMENTS = "parsedFragments";

  private Cache<String, Document> documentCache;
  private Cache<String, DocumentFragment> fragmentCache;
  private Provider<HtmlSerializer> serializerProvider = new DefaultSerializerProvider();
  protected final DOMImplementation documentFactory;

  protected GadgetHtmlParser(DOMImplementation documentFactory) {
    this.documentFactory = documentFactory;
  }

  protected GadgetHtmlParser(DOMImplementation documentFactory,
      final HtmlSerializer serializer) {
    this.documentFactory = documentFactory;
    this.serializerProvider = new Provider<HtmlSerializer>() {
      public HtmlSerializer get() {
        return serializer;
      }
    };
  }

  @Inject
  public void setCacheProvider(CacheProvider cacheProvider) {
    documentCache = cacheProvider.createCache(PARSED_DOCUMENTS);
    fragmentCache = cacheProvider.createCache(PARSED_FRAGMENTS);
  }

  @Inject
  public void setSerializerProvider(Provider<HtmlSerializer> serProvider) {
    this.serializerProvider = serProvider;
  }

  /**
   * @param content
   * @return true if we detect a preamble of doctype or html
   */
  protected static boolean attemptFullDocParseFirst(String content) {
    String normalized = content.substring(0, Math.min(100, content.length())).toUpperCase();
    return normalized.contains("<!DOCTYPE") || normalized.contains("<HTML");
  }

  public Document parseDom(String source) throws GadgetException {
    Document document = null;
    String key = null;
    // Avoid checksum overhead if we arent caching
    boolean shouldCache = shouldCache();
    if (shouldCache) {
      // TODO - Consider using the source if its under a certain size
      key = HashUtil.checksum(source.getBytes());
      document = documentCache.getElement(key);
    }

    if (document == null) {
      try {
        document = parseDomImpl(source);
      } catch (DOMException e) {
        // DOMException is a RuntimeException
        document = errorDom(e);
        HtmlSerialization.attach(document, serializerProvider.get(), source);
        return document;
      } catch (NullPointerException e) {
        throw new GadgetException(GadgetException.Code.INTERNAL_SERVER_ERROR,
                                  "Caught exception in parseDomImpl", e);
      }

      HtmlSerialization.attach(document, serializerProvider.get(), source);

      Node html = document.getDocumentElement();

      Node head = null;
      Node body = null;
      LinkedList<Node> beforeHead = Lists.newLinkedList();
      LinkedList<Node> beforeBody = Lists.newLinkedList();

      while (html.hasChildNodes()) {
        Node child = html.removeChild(html.getFirstChild());
        if (child.getNodeType() == Node.ELEMENT_NODE &&
            "head".equalsIgnoreCase(child.getNodeName())) {
          if (head == null) {
            head = child;
          } else {
            // Concatenate <head> elements together.
            transferChildren(head, child);
          }
        } else if (child.getNodeType() == Node.ELEMENT_NODE &&
                   "body".equalsIgnoreCase(child.getNodeName())) {
          if (body == null) {
            body = child;
          } else {
            // Concatenate <body> elements together.
            transferChildren(body, child);
          }
        } else if (head == null) {
          beforeHead.add(child);
        } else if (body == null) {
          beforeBody.add(child);
        } else {
          // Both <head> and <body> are present. Append to tail of <body>.
          body.appendChild(child);
        }
      }

      // Ensure head tag exists
      if (head == null) {
        // beforeHead contains all elements that should be prepended to <body>. Switch them.
        LinkedList<Node> temp = beforeBody;
        beforeBody = beforeHead;
        beforeHead = temp;

        // Add as first element
        head = document.createElement("head");
        html.insertBefore(head, html.getFirstChild());
      } else {
        // Re-append head node.
        html.appendChild(head);
      }

      // Ensure body tag exists.
      if (body == null) {
        // Add immediately after head.
        body = document.createElement("body");
        html.insertBefore(body, head.getNextSibling());
      } else {
        // Re-append body node.
        html.appendChild(body);
      }

      // Leftovers: nodes before the first <head> node found and the first <body> node found.
      // Prepend beforeHead to the front of <head>, and beforeBody to beginning of <body>,
      // in the order they were found in the document.
      prependToNode(head, beforeHead);
      prependToNode(body, beforeBody);

      // One exception. <style>/<link rel="stylesheet" nodes from <body> end up at the end of <head>,
      // since doing so is HTML compliant and can never break rendering due to ordering concerns.
      LinkedList<Node> styleNodes = Lists.newLinkedList();
      NodeList bodyKids = body.getChildNodes();
      for (int i = 0; i < bodyKids.getLength(); ++i) {
        Node bodyKid = bodyKids.item(i);
        if (bodyKid.getNodeType() == Node.ELEMENT_NODE &&
            isStyleElement((Element)bodyKid)) {
          styleNodes.add(bodyKid);
        }
      }

      for (Node styleNode : styleNodes) {
        head.appendChild(body.removeChild(styleNode));
      }

      // Finally, reprocess all script nodes for OpenSocial purposes, as these
      // may be interpreted (rightly, from the perspective of HTML) as containing text only.
      reprocessScriptForOpenSocial(html);

      if (shouldCache) {
        documentCache.addElement(key, document);
      }
    }

    if (shouldCache) {
      Document copy = (Document)document.cloneNode(true);
      HtmlSerialization.copySerializer(document, copy);
      return copy;
    }
    return document;
  }

  protected void transferChildren(Node to, Node from) {
    while (from.hasChildNodes()) {
      to.appendChild(from.removeChild(from.getFirstChild()));
    }
  }

  protected void prependToNode(Node to, LinkedList<Node> from) {
    while (!from.isEmpty()) {
      to.insertBefore(from.removeLast(), to.getFirstChild());
    }
  }

  private boolean isStyleElement(Element elem) {
    return "style".equalsIgnoreCase(elem.getNodeName()) ||
           ("link".equalsIgnoreCase(elem.getNodeName()) &&
            ("stylesheet".equalsIgnoreCase(elem.getAttribute("rel")) ||
             elem.getAttribute("type").toLowerCase().contains("css")));
  }

  /**
   * Parses a snippet of markup and appends the result as children to the
   * provided node.
   *
   * @param source markup to be parsed
   * @param result Node to append results to
   * @throws GadgetException
   */
  public void parseFragment(String source, Node result) throws GadgetException {
    boolean shouldCache = shouldCache();
    String key = null;
    if (shouldCache) {
      key = HashUtil.checksum(source.getBytes());
      DocumentFragment cachedFragment = fragmentCache.getElement(key);
      if (cachedFragment != null) {
        copyFragment(cachedFragment, result);
        return;
      }
    }

    DocumentFragment fragment;
    try {
      fragment = parseFragmentImpl(source);
    } catch (DOMException e) {
      // DOMException is a RuntimeException
      appendParseException(result, e);
      return;
    }

    reprocessScriptForOpenSocial(fragment);
    if (shouldCache) {
      fragmentCache.addElement(key, fragment);
    }
    copyFragment(fragment, result);
  }

  private void copyFragment(DocumentFragment source, Node dest) {
    Document destDoc = dest.getOwnerDocument();
    NodeList nodes = source.getChildNodes();
    for (int i = 0; i < nodes.getLength(); i++) {
      Node clone = destDoc.importNode(nodes.item(i), true);
      dest.appendChild(clone);
    }
  }

  protected Document errorDom(DOMException e) {
    // Create a bare-bones DOM whose body is just error text.
    // We do this to echo information to the developer that originally
    // supplied the data, since doing so is more useful than simply
    // returning a black-box HTML error code stemming from an NPE or other condition downstream.
    // The method is protected to allow overriding of this behavior.
    Document doc = documentFactory.createDocument(null, null, null);
    Node html = doc.createElement("html");
    html.appendChild(doc.createElement("head"));
    Node body = doc.createElement("body");
    appendParseException(body, e);
    html.appendChild(body);
    doc.appendChild(html);
    return doc;
  }

  private void appendParseException(Node node, DOMException e) {
    node.appendChild(node.getOwnerDocument().createTextNode(
        GadgetException.Code.HTML_PARSE_ERROR.toString() + ": " + e.toString()));
  }

  protected boolean shouldCache() {
    return documentCache != null && documentCache.getCapacity() != 0;
  }

  private void reprocessScriptForOpenSocial(Node root) throws GadgetException {
    LinkedList<Node> nodeQueue = Lists.newLinkedList();
    nodeQueue.add(root);
    while (!nodeQueue.isEmpty()) {
      Node next = nodeQueue.removeFirst();
      if (next.getNodeType() == Node.ELEMENT_NODE &&
          "script".equalsIgnoreCase(next.getNodeName())) {
        Attr typeAttr = (Attr)next.getAttributes().getNamedItem("type");
        if (typeAttr != null &&
            SocialDataTags.SCRIPT_TYPE_TO_OSML_TAG.get(typeAttr.getValue()) != null) {
          String osType = SocialDataTags.SCRIPT_TYPE_TO_OSML_TAG.get(typeAttr.getValue());

          // The underlying parser impl may have already parsed these.
          // Only re-parse with the coalesced text children wrapped within
          // the corresponding OSData/OSTemplate tag.
          boolean parseOs = true;
          StringBuilder sb = new StringBuilder();

          try {
            // Convert the <script type="os/*" xmlns=""> node into an equivilant OSML tag
            // while preserving all attributes (excluding 'type') in the original script node,
            // including any xmlns attribute. This allows children to be reparsed within the
            // correct xml namespace.
            next.getAttributes().removeNamedItem("type");

            HtmlSerialization.printStartElement(osType,
                next.getAttributes(), sb, /*withXmlClose*/ false);

          } catch (IOException e) {
            if (LOG.isLoggable(Level.INFO)) {
              LOG.logp(Level.INFO, classname, "reprocessScriptForOpenSocial", MessageKeys.UNABLE_TO_CONVERT_SCRIPT);
            }
          }

          NodeList scriptKids = next.getChildNodes();
          for (int i = 0; parseOs && i < scriptKids.getLength(); ++i) {
            Node scriptKid = scriptKids.item(i);
            if (scriptKid.getNodeType() != Node.TEXT_NODE) {
              parseOs = false;
            }
            sb.append(scriptKid.getTextContent());
          }

          if (parseOs) {
            // Clean out the script node.
            while (next.hasChildNodes()) {
              next.removeChild(next.getFirstChild());
            }

            sb.append("</").append(osType).append('>');
            DocumentFragment osFragment = parseFragmentImpl(sb.toString());
            while (osFragment.hasChildNodes()) {
              Node osKid = osFragment.removeChild(osFragment.getFirstChild());
              osKid = next.getOwnerDocument().adoptNode(osKid);
              if (osKid.getNodeType() == Node.ELEMENT_NODE) {
                next.getParentNode().appendChild(osKid);
              }
            }

            next.getParentNode().removeChild(next);
          }
        }
      }

      // Enqueue children for inspection.
      NodeList children = next.getChildNodes();
      for (int i = 0; i < children.getLength(); ++i) {
        nodeQueue.add(children.item(i));
      }
    }
  }

  /**
   * TODO: remove the need for parseDomImpl as a parsing method. Gadget HTML is
   * tag soup handled in custom fashion, or is a legitimate fragment. In either case,
   * we can simply use the fragment parsing implementation and patch up in higher-level calls.
   * @param source a piece of HTML
   * @return a Document parsed from the HTML
   * @throws GadgetException
   */
  protected abstract Document parseDomImpl(String source)
      throws GadgetException;

  /**
   * @param source a snippet of HTML markup
   * @return a DocumentFragment containing the parsed elements
   * @throws GadgetException
   */
  protected abstract DocumentFragment parseFragmentImpl(String source)
      throws GadgetException;

  private static class DefaultSerializerProvider implements Provider<HtmlSerializer> {
    public HtmlSerializer get() {
      return new DefaultHtmlSerializer();
    }
  }
}
