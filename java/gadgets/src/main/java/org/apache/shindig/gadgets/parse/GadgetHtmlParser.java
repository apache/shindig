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

import com.google.inject.ImplementedBy;
import com.google.inject.Inject;

import org.w3c.dom.Document;

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
    String normalized = content.substring(Math.min(100, content.length())).toUpperCase();
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
}
