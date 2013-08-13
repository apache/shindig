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
import org.apache.shindig.common.util.HashUtil;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpResponse;

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.CssLexer;
import com.google.caja.lexer.CssTokenType;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.Token;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A parser that records the stream of CSS lexial tokens from the Caja lexer and creates a
 * pseudo-DOM from that stream.
 *
 * TODO: Remove once Caja CSS DOM parser issues are resolved.
 */
public class CajaCssLexerParser {

  private static final Pattern urlMatcher =
      Pattern.compile("(url\\s*\\(\\s*['\"]?)([^\\)'\"]*)(['\"]?\\s*\\))",
          Pattern.CASE_INSENSITIVE);

  private static final URI DUMMY_SOURCE = URI.create("http://www.example.org");

  public static final String CACHE_NAME = "parsedCss";

  private Cache<String, List<Object>> parsedCssCache;

  @Inject
  public void setCacheProvider(CacheProvider cacheProvider) {
    parsedCssCache = cacheProvider.createCache(CACHE_NAME);
  }

  public List<Object> parse(String content) throws GadgetException {
    List<Object> parsedCss = null;
    boolean shouldCache = shouldCache();
    String key = null;
    if (shouldCache) {
      // TODO - Consider using the source if its under a certain size
      key = HashUtil.checksum(content.getBytes());
      parsedCss = parsedCssCache.getElement(key);
    }
    if (parsedCss == null) {
      parsedCss = parseImpl(content);
      if (shouldCache) {
        parsedCssCache.addElement(key, parsedCss);
      }
    }

    if (shouldCache) {
      List<Object> cloned = Lists.newArrayListWithCapacity(parsedCss.size());
      for (Object o : parsedCss) {
        if (o instanceof ImportDecl) {
          cloned.add(new ImportDecl(((ImportDecl) o).getUri()));
        } else if (o instanceof UriDecl) {
          cloned.add(new UriDecl(((UriDecl) o).getUri()));
        } else {
          cloned.add(o);
        }
      }
      return cloned;
    }
    return parsedCss;
  }

  List<Object> parseImpl(String content) throws GadgetException {
    List<Object> parsedCss = Lists.newArrayList();
    CharProducer producer = CharProducer.Factory.create(new StringReader(content),
        new InputSource(DUMMY_SOURCE));
    CssLexer lexer = new CssLexer(producer);
    try {
      StringBuilder builder = new StringBuilder();
      boolean inImport = false;
      while (lexer.hasNext()) {
        Token<CssTokenType> token = lexer.next();
        if (token.type == CssTokenType.SYMBOL && token.text.equalsIgnoreCase("@import")) {
          parsedCss.add(builder.toString());
          builder.setLength(0);
          inImport = true;
        } else if (inImport) {
          if (token.type == CssTokenType.URI) {
            parsedCss.add(builder.toString());
            builder.setLength(0);
            Matcher matcher = urlMatcher.matcher(token.text);
            if (matcher.find()) {
              parsedCss.add(new ImportDecl(matcher.group(2).trim()));
            }
          } else if (token.type != CssTokenType.SPACE && token.type != CssTokenType.PUNCTUATION) {
            inImport = false;
            builder.append(token.text);
          } else {
            //builder.append(token.text);
          }
        } else if (token.type == CssTokenType.URI) {
          Matcher matcher = urlMatcher.matcher(token.text);
          if (!matcher.find()) {
            builder.append(token.text);
          } else {
            parsedCss.add(builder.toString());
            builder.setLength(0);
            parsedCss.add(new UriDecl(matcher.group(2).trim()));
          }
        } else {
          builder.append(token.text);
        }
      }
      parsedCss.add(builder.toString());
    } catch (ParseException pe) {
      throw new GadgetException(GadgetException.Code.CSS_PARSE_ERROR, pe,
          HttpResponse.SC_BAD_REQUEST);
    }
    return parsedCss;
  }

  /** Serialize a stylesheet to a String */
  public String serialize(List<Object> styleSheet) {
    StringWriter writer = new StringWriter();
    serialize(styleSheet, writer);
    return writer.toString();
  }

  /** Serialize a stylesheet to a Writer. */
  public void serialize(List<Object> styleSheet, Appendable writer) {
    try {
      for (Object o : styleSheet) {
        writer.append(o.toString());
      }
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }


  private boolean shouldCache() {
    return parsedCssCache != null && parsedCssCache.getCapacity() != 0;
  }

  public static class ImportDecl {

    private String uri;

    public ImportDecl(String uri) {
      this.uri = uri;
    }

    public String getUri() {
      return uri;
    }

    public void setUri(String uri) {
      this.uri = uri;
    }

    @Override
    public String toString() {
      return "@import url('" + uri + "');\n";
    }
  }

  public static class UriDecl {

    private String uri;

    public UriDecl(String uri) {
      this.uri = uri;
    }

    public String getUri() {
      return uri;
    }

    public void setUri(String uri) {
      this.uri = uri;
    }

    @Override
    public String toString() {
      return "url('" + uri + "')";
    }
  }
}
