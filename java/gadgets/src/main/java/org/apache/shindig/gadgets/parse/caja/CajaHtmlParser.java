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
package org.apache.shindig.gadgets.parse.caja;

import java.util.LinkedList;

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.HtmlLexer;
import com.google.caja.lexer.HtmlTokenType;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.TokenQueue;
import com.google.caja.parser.html.DomParser;
import com.google.caja.parser.html.Namespaces;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.SimpleMessageQueue;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.parse.GadgetHtmlParser;
import org.apache.shindig.gadgets.parse.HtmlSerialization;
import org.apache.shindig.gadgets.parse.HtmlSerializer;
import org.apache.shindig.gadgets.parse.SocialDataTags;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;

public class CajaHtmlParser extends GadgetHtmlParser {
  private static final String OSML_DATA_START = '<' + SocialDataTags.OSML_DATA_TAG;
  private static final String OSML_TEMPLATE_START = '<' + SocialDataTags.OSML_TEMPLATE_TAG;

  @Inject
  public CajaHtmlParser(DOMImplementation documentFactory) {
    super(documentFactory);
  }

  public CajaHtmlParser(DOMImplementation documentFactory,
      HtmlSerializer serializer) {
    super(documentFactory, serializer);
  }

  @Override
  protected Document parseDomImpl(String source) throws GadgetException {
    DocumentFragment fragment = parseFragmentImpl(source);

    // TODO: remove parseDomImpl() altogether; only have subclasses
    // support parseFragmentImpl() with base class cleaning up.
    Document document = fragment.getOwnerDocument();
    CajaHtmlSerializer serializer = new CajaHtmlSerializer();
    HtmlSerialization.attach(document, serializer, null);
    Node html = null;
    LinkedList<Node> beforeHtml = Lists.newLinkedList();
    while (fragment.hasChildNodes()) {
      Node child = fragment.removeChild(fragment.getFirstChild());
      if (child.getNodeType() == Node.ELEMENT_NODE &&
          "html".equalsIgnoreCase(child.getNodeName())) {
        if (html == null) {
          html = child;
        } else {
          // Ignore the current (duplicated) html node but add its children
          transferChildren(html, child);
        }
      } else if (html != null) {
        html.appendChild(child);
      } else {
        beforeHtml.add(child);
      }
    }

    if (html == null) {
      html = document.createElement("html");
    }

    prependToNode(html, beforeHtml);

    // Ensure document.getDocumentElement() is html node.
    document.appendChild(html);

    return document;
  }

  @Override
  protected DocumentFragment parseFragmentImpl(String source)
      throws GadgetException {
    try {
      MessageQueue mq = makeMessageQueue();

      DomParser parser = getDomParser(source, mq);
      DocumentFragment fragment = parser.parseFragment();

      if (mq.hasMessageAtLevel(MessageLevel.ERROR)) {
        StringBuilder err = new StringBuilder();
        for (Message m : mq.getMessages()) {
          err.append(m.toString()).append('\n');
        }
        throw new GadgetException(GadgetException.Code.HTML_PARSE_ERROR, err.toString(),
            HttpResponse.SC_BAD_REQUEST);
      }
      return fragment;
    } catch (ParseException e) {
      throw new GadgetException(
          GadgetException.Code.HTML_PARSE_ERROR, e.getCajaMessage().toString(),
          HttpResponse.SC_BAD_REQUEST);
    }
  }

  protected InputSource getInputSource() {
    // Returns a default/dummy InputSource.
    // We might consider adding the gadget URI to the GadgetHtmlParser API,
    // but in the meantime this method is protected to allow overriding this
    // with request-scoped retrieval of this same data.
    return InputSource.UNKNOWN;
  }

  protected MessageQueue makeMessageQueue() {
    return new SimpleMessageQueue();
  }

  protected boolean needsDebugData() {
    return false;
  }

  private DomParser getDomParser(String source, final MessageQueue mq) throws ParseException {
    InputSource is = getInputSource();
    HtmlLexer lexer = new HtmlLexer(CharProducer.Factory.fromString(source, is));
    TokenQueue<HtmlTokenType> tokenQueue = new TokenQueue<HtmlTokenType>(lexer, is);
    final Namespaces ns = Namespaces.HTML_DEFAULT;  // Includes OpenSocial
    final boolean needsDebugData = needsDebugData();

    // OpenSocial Tempates need to be parsed as XML since tags can be self-closing.
    final boolean asXml =
        (source.startsWith(OSML_DATA_START) || source.startsWith(OSML_TEMPLATE_START));
    DomParser parser = new DomParser(tokenQueue, asXml, mq);
    parser.setDomImpl(documentFactory);
    parser.setNeedsDebugData(needsDebugData);
    return parser;
  }
}
