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
import org.apache.shindig.common.xml.DomUtil;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.parse.nekohtml.NekoSimplifiedHtmlParser;

import com.google.inject.ImplementedBy;
import com.google.inject.Inject;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;

/**
 * Parser for arbitrary HTML content
 */
@ImplementedBy(NekoSimplifiedHtmlParser.class)
public abstract class GadgetHtmlParser {

  public static final String PARSED_DOCUMENTS = "parsedDocuments";

  private Cache<String, Document> documentCache;

  @Inject
  public void setCacheProvider(CacheProvider cacheProvider) {
    documentCache = cacheProvider.createCache(PARSED_DOCUMENTS);
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
      // Ensure head tag exists
      if (DomUtil.getFirstNamedChildNode(document.getDocumentElement(), "head") == null) {
        // Add as first element
        document.getDocumentElement().insertBefore(
            document.createElement("head"),
            document.getDocumentElement().getFirstChild());
      }
      // If body not found the document was entirely empty. Create the
      // element anyway
      if (DomUtil.getFirstNamedChildNode(document.getDocumentElement(), "body") == null) {
        document.getDocumentElement().appendChild(
            document.createElement("body"));
      }
      if (shouldCache) {
        documentCache.addElement(key, document);
      }
    }
    if (shouldCache) {
      Document copy = (Document)document.cloneNode(true);
      HtmlSerializer.copySerializer(document, copy);
      return copy;
    }
    return document;
  }

  private boolean shouldCache() {
    return documentCache != null && documentCache.getCapacity() != 0;
  }

  /**
   * @param source
   * @return a parsed document or document fragment
   * @throws GadgetException
   */
  protected abstract Document parseDomImpl(String source) throws GadgetException;

  /**
   * Normalize head and body tags in the passed fragment before including it
   * in the document
   * @param document
   * @param fragment
   */
  protected void normalizeFragment(Document document, DocumentFragment fragment) {
    Node htmlNode = DomUtil.getFirstNamedChildNode(fragment, "HTML");
    if (htmlNode != null) {
      document.appendChild(htmlNode);
    } else {
      Node bodyNode = DomUtil.getFirstNamedChildNode(fragment, "body");
      Node headNode = DomUtil.getFirstNamedChildNode(fragment, "head");
      if (bodyNode != null || headNode != null) {
        // We have either a head or body so put fragment into HTML tag
        Node root = document.appendChild(document.createElement("html"));
        if (headNode != null && bodyNode == null) {
          fragment.removeChild(headNode);
          root.appendChild(headNode);
          Node body = root.appendChild(document.createElement("body"));
          body.appendChild(fragment);
        } else {
          root.appendChild(fragment);
        }
      } else {
        // No head or body so put fragment into a body
        Node root = document.appendChild(document.createElement("html"));
        Node body = root.appendChild(document.createElement("body"));
        body.appendChild(fragment);
      }
    }
  }
}
