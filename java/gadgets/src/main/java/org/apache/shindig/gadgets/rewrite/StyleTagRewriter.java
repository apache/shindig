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

import com.google.caja.lexer.HtmlTokenType;
import com.google.caja.lexer.Token;

import java.net.URI;

/**
 * Rewrite the CSS content of a style tag
 */
public class StyleTagRewriter implements HtmlTagTransformer {

  private URI source;
  private LinkRewriter linkRewriter;

  private StringBuffer sb;

  public StyleTagRewriter(URI source, LinkRewriter linkRewriter) {
    this.source = source;
    this.linkRewriter = linkRewriter;
    sb = new StringBuffer(500);
  }

  public void accept(Token<HtmlTokenType> token, Token<HtmlTokenType> lastToken) {
    if (token.type == HtmlTokenType.UNESCAPED) {
      sb.append(CssRewriter.rewrite(token.text, source, linkRewriter));
    } else {
      sb.append(HtmlRewriter.producePreTokenSeparator(token, lastToken));
      sb.append(token.text);
      sb.append(HtmlRewriter.producePostTokenSeparator(token, lastToken));
    }
  }

  public boolean acceptNextTag(Token<HtmlTokenType> tagStart) {
    return false;
  }

  public String close() {
    String result = sb.toString();
    sb.setLength(0);
    return result;
  }
}
