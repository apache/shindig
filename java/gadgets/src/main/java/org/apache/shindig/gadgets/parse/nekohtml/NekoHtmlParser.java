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

import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.parse.GadgetHtmlParser;
import org.apache.shindig.gadgets.parse.HtmlSerializer;
import org.cyberneko.html.parsers.DOMFragmentParser;
import org.cyberneko.html.parsers.DOMParser;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Parser that uses the NekoHtml parser and produces an un-abridged DOM
 *
 * TODO: Create a reusable instance in ThreadLocal
 */
@Singleton
public class NekoHtmlParser extends GadgetHtmlParser {

  private final DOMImplementation documentProvider;

  @Inject
  public NekoHtmlParser(DOMImplementation documentProvider) {
    this.documentProvider = documentProvider;
  }

  @Override
  public Document parseDomImpl(String source) throws GadgetException {
    try {
      Document document = parseDomInternal(source);
      HtmlSerializer.attach(document, new NekoSerializer(), source);
      return document;
    } catch (Exception e) {
      throw new GadgetException(GadgetException.Code.HTML_PARSE_ERROR, e);
    }
  }

  private Document parseDomInternal(String source) throws SAXException, IOException, GadgetException {
    if (attemptFullDocParseFirst(source)) {
      InputSource input = new InputSource(new StringReader(source));
      DOMParser parser = new DOMParser();
      // Force parser not to use HTMLDocumentImpl as document implementation otherwise
      // it forces all element names to uppercase.
      parser.setProperty("http://apache.org/xml/properties/dom/document-class-name",
          "org.apache.xerces.dom.DocumentImpl");
      // Dont convert element names to upper/lowercase
      parser.setProperty("http://cyberneko.org/html/properties/names/elems", "default");
      // Preserve case of attributes
      parser.setProperty("http://cyberneko.org/html/properties/names/attrs", "no-change");
      // Record entity references
      parser.setFeature("http://apache.org/xml/features/scanner/notify-char-refs", true);
      parser.setFeature("http://cyberneko.org/html/features/scanner/notify-builtin-refs", true);
      // No need to defer as full DOM is walked later
      parser.setFeature("http://apache.org/xml/features/dom/defer-node-expansion", false);
      parser.parse(input);
      return parser.getDocument();
    } else {
      DocumentFragment fragment = parseFragmentImpl(source);
      normalizeFragment(fragment.getOwnerDocument(), fragment);
      return fragment.getOwnerDocument();
    }
  }
  
  @Override
  protected DocumentFragment parseFragmentImpl(String source) throws GadgetException {
    try {
      Document htmlDoc = documentProvider.createDocument(null, null, null);
      // Workaround for error check failure adding text node to entity ref as a child
      htmlDoc.setStrictErrorChecking(false);
      DocumentFragment fragment = htmlDoc.createDocumentFragment();
      InputSource input = new InputSource(new StringReader(source));
      DOMFragmentParser parser = new DOMFragmentParser();
      parser.setProperty("http://cyberneko.org/html/properties/names/elems", "default");
      parser.setFeature("http://cyberneko.org/html/features/document-fragment", true);
      parser.setProperty("http://cyberneko.org/html/properties/names/attrs", "no-change");
      parser.setFeature("http://apache.org/xml/features/scanner/notify-char-refs", true);
      parser.setFeature("http://cyberneko.org/html/features/scanner/notify-builtin-refs", true);
      parser.parse(input, fragment);
      return fragment;
    } catch (Exception e) {
      throw new GadgetException(GadgetException.Code.HTML_PARSE_ERROR, e);
    }
  }
  
}
