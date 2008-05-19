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
package org.apache.shindig.gadgets.rewrite;

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.HtmlLexer;
import com.google.caja.lexer.HtmlTokenType;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.Token;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.util.Map;

/**
 * Rewrites an HTML content block
 */
public class HtmlRewriter {

  private HtmlRewriter() {
  }

  public static String rewrite(String content, URI source,
      Map<String, HtmlTagTransformer> transformers) {
    StringWriter sw = new StringWriter((content.length() * 110) / 100);
    rewrite(new StringReader(content), source, transformers, sw);
    return sw.toString();
  }

  public static void rewrite(Reader content, URI source,
      Map<String, HtmlTagTransformer> transformers,
      Writer writer) {
    CharProducer producer = CharProducer.Factory.create(content,
        new InputSource(source));
    HtmlLexer lexer = new HtmlLexer(producer);
    try {
      Token<HtmlTokenType> lastToken = null;
      Token<HtmlTokenType> currentTag = null;
      HtmlTagTransformer currentTransformer = null;
      boolean tagChanged;
      while (lexer.hasNext()) {
        tagChanged = false;
        Token<HtmlTokenType> token = lexer.next();
        if (token.type == HtmlTokenType.IGNORABLE) {
          continue;
        }
        if (token.type == HtmlTokenType.TAGBEGIN) {
          currentTag = token;
          tagChanged = true;
        }
        if (tagChanged) {
          if (currentTransformer == null) {
            currentTransformer = transformers
                .get(currentTag.text.substring(1).toLowerCase());
          } else {
            if (!currentTransformer.acceptNextTag(currentTag)) {
              writer.write(currentTransformer.close());
              currentTransformer = transformers
                  .get(currentTag.text.substring(1).toLowerCase());
            }
          }
        }
        if (currentTransformer == null) {
          writer.write(producePreTokenSeparator(token, lastToken));
          writer.write(token.text);
          writer.write(producePostTokenSeparator(token, lastToken));
        } else {
          currentTransformer.accept(token, lastToken);
        }
        if (token.type == HtmlTokenType.TAGEND) {
          currentTag = null;
        }
        lastToken = token;
      }
      if (currentTransformer != null) {
        writer.write(currentTransformer.close());
      }
    } catch (ParseException pe) {
      pe.printStackTrace();
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }
  }


  public static String producePreTokenSeparator(Token<HtmlTokenType> token,
      Token<HtmlTokenType> lastToken) {
    if (token.type == HtmlTokenType.ATTRNAME) {
      return " ";
    }
    if (token.type == HtmlTokenType.ATTRVALUE &&
        lastToken != null &&
        lastToken.type == HtmlTokenType.ATTRNAME) {
      return "=";
    }
    return "";
  }


  public static String producePostTokenSeparator(Token<HtmlTokenType> token,
      Token<HtmlTokenType> lastToken) {
    return "";
  }

}
