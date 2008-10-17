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
import org.apache.shindig.gadgets.parse.ParsedHtmlAttribute;
import org.apache.shindig.gadgets.parse.ParsedHtmlNode;

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.HtmlLexer;
import com.google.caja.lexer.HtmlTokenType;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.TokenQueue;
import com.google.caja.parser.html.DomParser;
import com.google.caja.parser.html.DomTree;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.SimpleMessageQueue;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.Inject;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Document;
import org.w3c.dom.html.HTMLDocument;

import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * Caja-based implementation of a {@code GadgetHtmlParser}.
 */
@Singleton
public class CajaHtmlParser extends GadgetHtmlParser {

  Provider<HTMLDocument> documentProvider;

  @Inject
  public CajaHtmlParser(Provider<HTMLDocument> documentProvider) {
    this.documentProvider = documentProvider;
  }

  @Override
  public List<ParsedHtmlNode> parse(String source) throws GadgetException {
    DomTree domTree = getFragment(source);
    List<ParsedHtmlNode> nodes =
        new ArrayList<ParsedHtmlNode>(domTree.children().size());
    for (DomTree child : domTree.children()) {
      nodes.add(new CajaParsedHtmlNode(child));
    }
    return nodes;
  }

  @Override
  public Document parseDom(String source) throws GadgetException {
    // Wrap the whole thing in a top-level node to get full contents.
    return makeDocument(getFragment(source));
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

  private HTMLDocument makeDocument(DomTree.Fragment fragment) {
    HTMLDocument htmlDocument = documentProvider.get();

    // Check if doc contains an HTML node. If so just add it and recurse
    for (DomTree node : fragment.children()) {
      if (node instanceof DomTree.Tag &&
          ((DomTree.Tag)node).getTagName().equalsIgnoreCase("HTML")) {
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

  private static void recurseDocument(HTMLDocument doc, Node parent, DomTree elem) {
    if (elem instanceof DomTree.Tag) {
      DomTree.Tag tag = (DomTree.Tag) elem;
      Element element = doc.createElement(tag.getTagName());
      parent.appendChild(element);
      for (DomTree child : elem.children()) {
        recurseDocument(doc, element, child);
      }
    } else if (elem instanceof DomTree.Attrib) {
      DomTree.Attrib attrib = (DomTree.Attrib) elem;
      Attr domAttrib = doc.createAttribute(attrib.getAttribName());
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

  /**
   * {@code ParsedHtmlNode} implementation built using Caja parsing primitives.
   */
  private static class CajaParsedHtmlNode implements ParsedHtmlNode {
    private final List<ParsedHtmlAttribute> attributes;
    private final List<ParsedHtmlNode> children;
    private final String name;
    private final String text;
    
    private CajaParsedHtmlNode(DomTree elem) {
      if (elem instanceof DomTree.Tag) {
        DomTree.Tag tag = (DomTree.Tag)elem;
        attributes = new ArrayList<ParsedHtmlAttribute>(1);
        children = new ArrayList<ParsedHtmlNode>();
        name = tag.getTagName();
        text = null;
        for (DomTree child : elem.children()) {
          if (child instanceof DomTree.Attrib) {
            attributes.add(new CajaParsedHtmlAttribute((DomTree.Attrib)child));
          } else {
            children.add(new CajaParsedHtmlNode(child));
          }
        }
      } else if (elem instanceof DomTree.Text ||
                 elem instanceof DomTree.CData) {
        // DomTree.CData can theoretically occur since it's supported
        // in HTML5, but the implementation doesn't supply this yet.
        attributes = null;
        children = null;
        name = null;
        text = ((DomTree.Text)elem).getValue();
      } else {
        // This should never happen. The only remaining types are
        // DomTree.Fragment, which is simply a top-level container
        // that results from the DomTree.parseFragment() method,
        // and DomTree.Value, which is always a child of DomTree.Attrib.
        attributes = null;
        children = null;
        name = null;
        text = null;
      }
    }
    
    public List<ParsedHtmlAttribute> getAttributes() {
      return attributes;
    }

    public List<ParsedHtmlNode> getChildren() {
      return children;
    }

    public String getTagName() {
      return name;
    }

    public String getText() {
      return text;
    }
  }
  
  /**
   * {@code ParsedHtmlAttribute} built from a Caja DomTree primitive.
   */
  private static class CajaParsedHtmlAttribute implements ParsedHtmlAttribute {
    private final String name;
    private final String value;
    
    private CajaParsedHtmlAttribute(DomTree.Attrib attrib) {
      name = attrib.getAttribName();
      value = attrib.getAttribValue();
    }
    
    public String getName() {
      return name;
    }

    public String getValue() {
      return value;
    }
  }
}
