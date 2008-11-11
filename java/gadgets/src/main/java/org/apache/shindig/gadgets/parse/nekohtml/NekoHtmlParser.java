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
import org.apache.shindig.gadgets.parse.DomUtil;
import org.apache.shindig.gadgets.parse.GadgetHtmlParser;
import org.apache.shindig.gadgets.parse.HtmlSerializer;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.xml.serialize.HTMLSerializer;
import org.apache.xml.serialize.OutputFormat;
import org.cyberneko.html.parsers.DOMFragmentParser;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

/**
 * Parser that uses the NekoHtml parser.
 *
 * TODO:
 * Currently this code uses the ParsedXXX wrapper types so we can share abstraction
 * with Caja. This is probably unnecessary overhead and we would prefer that Caja
 * implements up to org.w3c.dom (or perhaps the Caja wrapper types should?)
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
      Document document = parseFragment(source);
      HtmlSerializer.attach(document, new Serializer(), source);
      return document;
    } catch (Exception e) {
      throw new GadgetException(GadgetException.Code.HTML_PARSE_ERROR, e);
    }
  }

  private Document parseFragment(String source) throws SAXException, IOException {
    InputSource input = new InputSource(new StringReader(source));
    DOMFragmentParser parser = new DOMFragmentParser();

    Document htmlDoc = documentProvider.createDocument(null, null, null);
    DocumentFragment fragment = htmlDoc.createDocumentFragment();
    parser.parse(input, fragment);
    Node htmlNode = DomUtil.getFirstNamedChildNode(fragment, "HTML");
    if (htmlNode != null) {
      htmlDoc.appendChild(htmlNode);
    } else {
      Node root = htmlDoc.appendChild(htmlDoc.createElement("HTML"));
      root.appendChild(fragment);
    }
    return htmlDoc;
  }

  static class Serializer extends HtmlSerializer {

    static final OutputFormat outputFormat = new OutputFormat();
    static {
      outputFormat.setPreserveSpace(true);
      outputFormat.setPreserveEmptyAttributes(false);
    }

    public String serializeImpl(Document doc) {
      StringWriter sw = createWriter(doc);
      HTMLSerializer serializer = new HTMLSerializer(sw, outputFormat);
      try {
        serializer.serialize(doc);
        return sw.toString();
      } catch (IOException ioe) {
        return null;
      }
    }
  }
}
