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
package org.apache.shindig.gadgets.parse;

import org.apache.shindig.common.cache.Cache;
import org.apache.shindig.common.cache.CacheProvider;
import org.apache.shindig.common.util.HashUtil;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.parse.nekohtml.NekoSimplifiedHtmlParser;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.Lists;
import com.google.inject.ImplementedBy;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.LinkedList;

/**
 * Parser for arbitrary HTML content
 */
@ImplementedBy(NekoSimplifiedHtmlParser.class)
public abstract class GadgetHtmlParser {

  public static final String PARSED_DOCUMENTS = "parsedDocuments";
  public static final String PARSED_FRAGMENTS = "parsedFragments";

  private Cache<String, Document> documentCache;
  private Cache<String, DocumentFragment> fragmentCache;
  private Provider<HtmlSerializer> serializerProvider = new DefaultSerializerProvider();

  /**
   * Allowed tag names for OpenSocial Data and template blocks.
   */
  public static final String OSML_DATA_TAG = "OSData";
  public static final String OSML_TEMPLATE_TAG = "OSTemplate";

  /**
   * Bi-map of OpenSocial tags to their script type attribute values.
   */
  public static final BiMap<String, String> SCRIPT_TYPE_TO_OSML_TAG = ImmutableBiMap.of(
      "text/os-data", OSML_DATA_TAG, "text/os-template", OSML_TEMPLATE_TAG);

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

  public final Document parseDom(String source) throws GadgetException {
    Document document = null;
    String key = null;
    // Avoid checksum overhead if we arent caching
    boolean shouldCache = shouldCache();
    if (shouldCache) {
      // TODO - Consider using the source if its under a certain size
      key = HashUtil.rawChecksum(source.getBytes());
      document = documentCache.getElement(key);
    }
    
    if (document == null) {
      document = parseDomImpl(source);

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
      
      // One exception. <style> nodes from <body> end up at the end of <head>, since doing so
      // is HTML compliant and can never break rendering due to ordering concerns.
      LinkedList<Node> styleNodes = Lists.newLinkedList();
      NodeList bodyKids = body.getChildNodes();
      for (int i = 0; i < bodyKids.getLength(); ++i) {
        Node bodyKid = bodyKids.item(i);
        if (bodyKid.getNodeType() == Node.ELEMENT_NODE &&
            "style".equalsIgnoreCase(bodyKid.getNodeName())) {
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
    while (from.size() > 0) {
      to.insertBefore(from.removeLast(), to.getFirstChild());
    }
  }

  /**
   * Parses a snippet of markup and appends the result as children to the 
   * provided node.
   * 
   * @param source markup to be parsed
   * @param result Node to append results to
   * @throws GadgetException
   */
  public final void parseFragment(String source, Node result) throws GadgetException {
    boolean shouldCache = shouldCache();
    String key = null;    
    if (shouldCache) {
      key = HashUtil.rawChecksum(source.getBytes());
      DocumentFragment cachedFragment = fragmentCache.getElement(key);
      if (cachedFragment != null) {
        copyFragment(cachedFragment, result);
        return;
      }
    }
    DocumentFragment fragment = parseFragmentImpl(source);
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
        if (typeAttr != null && SCRIPT_TYPE_TO_OSML_TAG.get(typeAttr.getValue()) != null) {
          // The underlying parser impl may have already parsed these.
          // Only re-parse with the coalesced text children if that's all there are.
          boolean parseOs = true;
          StringBuilder sb = new StringBuilder();
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
            DocumentFragment osFragment = parseFragmentImpl(sb.toString());
            while (osFragment.hasChildNodes()) {
              Node osKid = osFragment.removeChild(osFragment.getFirstChild());
              osKid = next.getOwnerDocument().adoptNode(osKid);
              if (osKid.getNodeType() == Node.ELEMENT_NODE) {
                next.appendChild(osKid);
              }
            }
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
