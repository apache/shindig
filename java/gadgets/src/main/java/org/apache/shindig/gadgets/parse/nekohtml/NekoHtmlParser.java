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

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.apache.shindig.common.xml.XmlUtil;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.parse.GadgetHtmlParser;
import org.cyberneko.html.parsers.DOMFragmentParser;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.w3c.dom.html.HTMLDocument;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;

/**
 * Parser that uses the NekoHtml parser.
 *
 * TODO:
 * Currently this code uses the ParsedXXX wrapper types so we can share abstraction
 * with Caja. This is probably unnecessary overhead and we would prefer that Caja
 * implements up to org.w3c.dom (or perhaps the Caja wrapper types should?)
 */
public class NekoHtmlParser extends GadgetHtmlParser {

  Provider<HTMLDocument> documentProvider;

  @Inject
  public NekoHtmlParser(Provider<HTMLDocument> documentProvider) {
    this.documentProvider = documentProvider;
  }

  @Override
  public Document parseDom(String source) throws GadgetException {
    try {
      return parseFragment(source);
    } catch (Exception e) {
      throw new GadgetException(GadgetException.Code.HTML_PARSE_ERROR, e);
    }
  }

  private Document parseFragment(String source) throws SAXException, IOException {
    InputSource input = new InputSource(new StringReader(source));
    DOMFragmentParser parser = new DOMFragmentParser();

    HTMLDocument htmlDoc = documentProvider.get();
    DocumentFragment fragment = htmlDoc.createDocumentFragment();
    parser.parse(input, fragment);
    Node htmlNode = XmlUtil.getFirstNamedChildNode(fragment, "HTML");
    if (htmlNode != null) {
      htmlDoc.appendChild(htmlNode);
    } else {
      Node root = htmlDoc.appendChild(htmlDoc.createElement("HTML"));
      root.appendChild(fragment);
    }
    return htmlDoc;
  }
}
