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

import com.google.caja.lexer.*;
import com.google.caja.parser.html.DomParser;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.SimpleMessageQueue;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.parse.GadgetHtmlParser;
import org.apache.shindig.gadgets.parse.HtmlSerialization;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;

/**
 * Simple html parser based on caja.
 */
public class VanillaCajaHtmlParser extends GadgetHtmlParser {
  private final boolean needsDebugData;

  @Inject
  public VanillaCajaHtmlParser(DOMImplementation documentFactory,
                               @Named("vanillaCajaParser.needsDebugData")
                               boolean needsDebugData) {
    super(documentFactory);
    this.needsDebugData = needsDebugData;
  }

  @Override
  public Document parseDom(String source) throws GadgetException {
    // TODO: Add support for caching the DOM after evaluation.
    return parseDomImpl(source);
  }

  private DomParser getDomParser(String source, final MessageQueue mq) throws ParseException {
    InputSource is = InputSource.UNKNOWN;
    HtmlLexer lexer = new HtmlLexer(CharProducer.Factory.fromString(source, is));
    TokenQueue<HtmlTokenType> tokenQueue = new TokenQueue<HtmlTokenType>(
        lexer, is);
    DomParser parser = new DomParser(tokenQueue, /** asXml */ false, mq);

    parser.setDomImpl(documentFactory);
    parser.setNeedsDebugData(needsDebugData);
    return parser;
  }

  @Override
  protected Document parseDomImpl(String source) throws GadgetException {
    MessageQueue mq = new SimpleMessageQueue();
    try {
      DomParser parser = getDomParser(source, mq);
      Document doc = parser.parseDocument().getOwnerDocument();

      VanillaCajaHtmlSerializer serializer = new VanillaCajaHtmlSerializer();
      HtmlSerialization.attach(doc, serializer, null);
      return doc;
    } catch (ParseException e) {
      throw new GadgetException(GadgetException.Code.HTML_PARSE_ERROR,
          e.getCajaMessage().toString(), HttpResponse.SC_INTERNAL_SERVER_ERROR);
    } catch (NullPointerException e) {
      throw new GadgetException(GadgetException.Code.INTERNAL_SERVER_ERROR, e);
    }
  }

  @Override
  protected DocumentFragment parseFragmentImpl(String source)
      throws GadgetException {
    throw new UnsupportedOperationException("Use parseDom instead.");
  }
}
