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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Rewrite a linking attribute of an HTML tag to an arbitrary scheme */
public class LinkingTagRewriter implements HtmlTagTransformer {

  private final URI relativeBase;

  private final LinkRewriter linkRewriter;

  private final Map<String, Set<String>> tagAttributeTargets;

  private final StringBuilder builder;

  private Set<String> currentTagAttrs;

  public static Map<String, Set<String>> getDefaultTargets() {
    Map<String, Set<String>> targets  = new HashMap<String, Set<String>>();
    targets.put("img", new HashSet<String>(Arrays.asList("src")));
    targets.put("embed", new HashSet<String>(Arrays.asList("src")));
    targets.put("link", new HashSet<String>(Arrays.asList("href")));
    return targets;
  }

  public LinkingTagRewriter(LinkRewriter linkRewriter, URI relativeBase) {
    this(getDefaultTargets(), linkRewriter, relativeBase);
  }

  public LinkingTagRewriter(Map<String, Set<String>> tagAttributeTargets,
      LinkRewriter linkRewriter, URI relativeBase) {
    this.tagAttributeTargets = tagAttributeTargets;
    this.linkRewriter = linkRewriter;
    this.relativeBase = relativeBase;
    builder = new StringBuilder(300);
  }

  public Set<String> getSupportedTags() {
    return tagAttributeTargets.keySet();
  }

  public void accept(Token<HtmlTokenType> token,
      Token<HtmlTokenType> lastToken) {
    if (token.type == HtmlTokenType.TAGBEGIN) {
      currentTagAttrs = tagAttributeTargets
          .get(token.text.substring(1).toLowerCase());
    }

    if (currentTagAttrs != null &&
        lastToken != null &&
        lastToken.type == HtmlTokenType.ATTRNAME &&
        currentTagAttrs.contains(lastToken.text.toLowerCase())) {
      String link = stripQuotes(token.text);
      builder.append("=\"");
      builder.append(linkRewriter.rewrite(link, relativeBase));
      builder.append("\"");
      return;
    }
    builder.append(HtmlRewriter.producePreTokenSeparator(token, lastToken));
    builder.append(token.text);
    builder.append(HtmlRewriter.producePostTokenSeparator(token, lastToken));
  }

  public boolean acceptNextTag(Token<HtmlTokenType> tagStart) {
    return false;
  }

  public String close() {
    String result = builder.toString();
    currentTagAttrs = null;
    builder.setLength(0);
    return result;
  }

  private String stripQuotes(String s) {
    return s.replaceAll("\"", "").replaceAll("'", "");
  }
}
