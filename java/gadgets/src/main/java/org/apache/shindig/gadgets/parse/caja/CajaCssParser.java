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

import org.apache.shindig.common.cache.Cache;
import org.apache.shindig.common.cache.CacheProvider;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.util.HashUtil;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpResponse;

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.CssLexer;
import com.google.caja.lexer.CssTokenType;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.Token;
import com.google.caja.lexer.TokenQueue;
import com.google.caja.lexer.TokenStream;
import com.google.caja.parser.css.CssParser;
import com.google.caja.parser.css.CssTree;
import com.google.caja.render.Concatenator;
import com.google.caja.render.CssPrettyPrinter;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.RenderContext;
import com.google.caja.reporting.SimpleMessageQueue;
import com.google.caja.util.Criterion;
import com.google.inject.Inject;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;

/**
 * A CSS DOM parser using Caja.
 */
public class CajaCssParser {

  /**
   * Fake URI source if one is not provided by the calling context.
   */
  private static final Uri FAKE_SOURCE = Uri.parse("http://a.dummy.url");

  private static final String PARSED_CSS = "parsedCss";

  private Cache<String, CssTree.StyleSheet> parsedCssCache;

  @Inject
  public void setCacheProvider(CacheProvider cacheProvider) {
    parsedCssCache = cacheProvider.createCache(PARSED_CSS);
  }

  /**
   * Parse CSS content into Caja's CSS DOM model
   *
   * @return A parsed stylesheet
   */
  public CssTree.StyleSheet parseDom(String content) throws GadgetException {
    // Use a fake source if the real source is unknown
    return parseDom(content, FAKE_SOURCE);
  }

  public CssTree.StyleSheet parseDom(String content, Uri source)
      throws GadgetException {
    CssTree.StyleSheet parsedCss = null;
    boolean shouldCache = shouldCache();
    String key = null;
    if (shouldCache) {
      // TODO - Consider using the source if its under a certain size
      key = HashUtil.checksum(content.getBytes());
      parsedCss = parsedCssCache.getElement(key);
    }
    if (parsedCss == null) {
      try {
        parsedCss = parseImpl(content, source);
        if (shouldCache) {
          parsedCssCache.addElement(key, parsedCss);
        }
      } catch (ParseException pe) {
        // Bad input; not server's fault.
        throw new GadgetException(GadgetException.Code.CSS_PARSE_ERROR, pe,
            HttpResponse.SC_BAD_REQUEST);
      }
    }
    if (shouldCache) {
      return (CssTree.StyleSheet)parsedCss.clone();
    }
    return parsedCss;
  }

  private CssTree.StyleSheet parseImpl(String css, Uri source)
      throws ParseException {
    InputSource inputSource = new InputSource(source.toJavaUri());
    CharProducer producer = CharProducer.Factory.create(new StringReader(css),
        inputSource);
    TokenStream<CssTokenType> lexer = new CssLexer(producer);
    TokenQueue<CssTokenType> queue = new TokenQueue<CssTokenType>(lexer, inputSource,
        new Criterion<Token<CssTokenType>>() {
          public boolean accept(Token<CssTokenType> t) {
            return CssTokenType.SPACE != t.type
                && CssTokenType.COMMENT != t.type;
          }
        });
    if (queue.isEmpty()) {
      // Return empty stylesheet
      return new CssTree.StyleSheet(null, Collections.<CssTree.CssStatement>emptyList());
    }
    MessageQueue mq = new SimpleMessageQueue();
    CssParser parser = new CssParser(queue, mq, MessageLevel.WARNING);
    return parser.parseStyleSheet();
  }

  /** Serialize a stylesheet to a String */
  public String serialize(CssTree.StyleSheet styleSheet) {
    StringWriter writer = new StringWriter();
    serialize(styleSheet, writer);
    return writer.toString();
  }

  /** Serialize a stylesheet to a Writer. */
  public void serialize(CssTree.StyleSheet styleSheet, Writer writer) {
    CssPrettyPrinter cssPrinter = new CssPrettyPrinter(new Concatenator(writer, null));
    styleSheet.render(new RenderContext(cssPrinter));
    cssPrinter.noMoreTokens();
  }

  private boolean shouldCache() {
    return parsedCssCache != null && parsedCssCache.getCapacity() != 0;
  }
}
