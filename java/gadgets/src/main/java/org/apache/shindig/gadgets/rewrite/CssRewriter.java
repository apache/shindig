/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.gadgets.rewrite;

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.CssLexer;
import com.google.caja.lexer.CssTokenType;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.Token;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Support rewriting links in CSS
 */
public class CssRewriter {

  private static final Pattern urlMatcher =
      Pattern.compile("(url\\s*\\(\\s*['\"]?)([^\\)'\"]*)(['\"]?\\s*\\))",
        Pattern.CASE_INSENSITIVE);

  public static String rewrite(String content, URI source,
      LinkRewriter linkRewriter) {
    StringWriter sw = new StringWriter((content.length() * 110) / 100);
    rewrite(new StringReader(content), source, linkRewriter, sw);
    return sw.toString();
  }

  public static void rewrite(Reader content, URI source,
      LinkRewriter rewriter,
      Writer writer) {
    CharProducer producer = CharProducer.Factory.create(content,
        new InputSource(source));
    CssLexer lexer = new CssLexer(producer);
    try {
      while (lexer.hasNext()) {
        Token<CssTokenType> token = lexer.next();
        if (token.type == CssTokenType.URI) {
          writer.write(rewriteLink(token, source, rewriter));
          continue;
        }
        writer.write(token.text);
      }
      writer.flush();
    } catch (ParseException pe) {
      pe.printStackTrace();
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }
  }

  private static String rewriteLink(Token<CssTokenType> token,
      URI base, LinkRewriter rewriter) {
    Matcher matcher = urlMatcher.matcher(token.text);
    if (!matcher.find()) return token.text;
    return "url(\"" + rewriter.rewrite(matcher.group(2), base) + "\")";
  }
}

