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
package org.apache.shindig.gadgets.parse.caja;

import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.parse.GadgetHtmlParser;
import org.apache.shindig.gadgets.parse.HtmlSerializer;
import org.apache.xml.serialize.HTMLSerializer;
import org.apache.xml.serialize.OutputFormat;

import com.google.caja.lexer.*;
import com.google.caja.parser.html.DomParser;
import com.google.caja.parser.html.DomTree;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.SimpleMessageQueue;
import com.google.caja.util.Name;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.w3c.dom.*;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Caja-based implementation of a {@code GadgetHtmlParser}.
 */
@Singleton
public class CajaHtmlParser extends GadgetHtmlParser {

  private final DOMImplementation documentProvider;

  @Inject
  public CajaHtmlParser(DOMImplementation documentProvider) {
    this.documentProvider = documentProvider;
  }

  @Override
  public Document parseDomImpl(String source) throws GadgetException {
    // Wrap the whole thing in a top-level node to get full contents.
    Document document = makeDocument(getFragment(source));
    HtmlSerializer.attach(document, new Serializer(), source);
    return document;
  }

  DomTree.Fragment getFragment(String content) throws GadgetException {
    DomParser parser = getParser(content);
    try {
      return parser.parseFragment();
    } catch (ParseException pe) {
      try {
        // Revert to nastiness
        DomTree.Fragment fragment = getParser("<HTML>" + content + "</HTML>").parseFragment();
        return new DomTree.Fragment(fragment.children().get(0).children());
      } catch (ParseException pe2) {
        throw new GadgetException(GadgetException.Code.HTML_PARSE_ERROR, pe2);
      }
    }
  }

  DomParser getParser(String content) {
    InputSource source = null;
    try {
      source = new InputSource(new URI("http://dummy.com/"));
    } catch (URISyntaxException e) {
      // Never happens. Dummy URI needed to satisfy API.
      // We may want to pass in the gadget URI for auditing
      // purposes at some point.                                      
    }
    CharProducer producer = CharProducer.Factory.create(
        new StringReader(content), source);
    HtmlLexer lexer = new HtmlLexer(producer);
    MessageQueue mQueue = new SimpleMessageQueue();
    return new DomParser(new TokenQueue<HtmlTokenType>(lexer, source), false, mQueue);
  }

  private Document makeDocument(DomTree.Fragment fragment) {
    Document htmlDocument = documentProvider.createDocument(null, null, null);

    // Check if doc contains an HTML node. If so just add it and recurse
    for (DomTree node : fragment.children()) {
      if (node instanceof DomTree.Tag &&
          ((DomTree.Tag)node).getTagName().equals(Name.html("HTML"))) {
        recurseDocument(htmlDocument, htmlDocument, node);
        return htmlDocument;
      }
    }
    Node root = htmlDocument.appendChild(htmlDocument.createElement("HTML"));
    for (DomTree child : fragment.children()) {
      recurseDocument(htmlDocument, root, child);
    }
    return htmlDocument;
  }

  private static void recurseDocument(Document doc, Node parent, DomTree elem) {
    if (elem instanceof DomTree.Tag) {
      DomTree.Tag tag = (DomTree.Tag) elem;
      Element element = doc.createElement(tag.getTagName().getCanonicalForm());
      parent.appendChild(element);
      for (DomTree child : elem.children()) {
        recurseDocument(doc, element, child);
      }
    } else if (elem instanceof DomTree.Attrib) {
      DomTree.Attrib attrib = (DomTree.Attrib) elem;
      Attr domAttrib = doc.createAttribute(attrib.getAttribName().getCanonicalForm());
      parent.getAttributes().setNamedItem(domAttrib);
      domAttrib.setValue(attrib.getAttribValue());
    } else if (elem instanceof DomTree.Text) {
      parent.appendChild(doc.createTextNode(elem.getValue()));
    } else if (elem instanceof DomTree.CData) {
      //
      parent.appendChild(doc.createCDATASection(elem.getValue()));
    } else {
      // TODO Implement for comment, fragment etc...
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
