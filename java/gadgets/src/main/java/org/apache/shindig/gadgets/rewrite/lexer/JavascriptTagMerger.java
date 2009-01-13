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
package org.apache.shindig.gadgets.rewrite.lexer;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.util.Utf8UrlCoder;
import org.apache.shindig.gadgets.rewrite.ContentRewriterFeature;
import org.apache.shindig.gadgets.servlet.ProxyBase;
import org.apache.shindig.gadgets.spec.GadgetSpec;

import com.google.common.collect.Lists;

import com.google.caja.lexer.HtmlTokenType;
import com.google.caja.lexer.Token;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

/**
 * Transform a contiguous block of script tags that refer to external scripts
 * and rewrite them to a single script tag that uses a concatenating proxy
 * to concatenate these scripts in order and also potentially perform other
 * optimizations on the generated unified script such as minification
 */
public class JavascriptTagMerger implements HtmlTagTransformer {

  private final static int MAX_URL_LENGTH = 1500;

  private final List<Object> scripts = Lists.newArrayList();

  private final String concatBase;

  private final Uri relativeUrlBase;

  private boolean isTagOpen = true;

  /**
   * @param concatBase Base url of the Concat servlet. Expected to be of the
   *                   form www.host.com/concat?
   * @param relativeUrlBase to resolve relative urls
   */
  public JavascriptTagMerger(GadgetSpec spec, ContentRewriterFeature rewriterFeature,
                             String concatBase, Uri relativeUrlBase) {
    // Force the mime-type to mimic browser expectation so rewriters
    // can function properly
    this.concatBase = concatBase
        + ProxyBase.REWRITE_MIME_TYPE_PARAM
        + "=text/javascript&"
        + "gadget="
        + Utf8UrlCoder.encode(spec.getUrl().toString())
        + "&fp="
        + rewriterFeature.getFingerprint()
        + '&';

    this.relativeUrlBase = relativeUrlBase;
  }

  public void accept(Token<HtmlTokenType> token,
      Token<HtmlTokenType> lastToken) {
    try {
      if (isTagOpen) {
        if (lastToken != null &&
            lastToken.type == HtmlTokenType.ATTRNAME &&
            lastToken.text.equalsIgnoreCase("src")) {
          scripts.add(Uri.parse(stripQuotes(token.text)));
        } else if (token.type == HtmlTokenType.UNESCAPED) {
          scripts.add(token);
        }
      }
    } catch (IllegalArgumentException use) {
      throw new RuntimeException(use);
    }
  }

  public boolean acceptNextTag(Token<HtmlTokenType> tagStart) {
    if (tagStart.text.equalsIgnoreCase("<script")) {
      isTagOpen = true;
      return true;
    } else if (tagStart.text.equalsIgnoreCase("</script")) {
      isTagOpen = false;
      return true;
    }
    return false;
  }

  @SuppressWarnings("unchecked")
  public String close() {
    List<Uri> concat = Lists.newArrayList();
    StringBuilder builder = new StringBuilder(100);
    for (Object o : scripts) {
      if (o instanceof Uri) {
        concat.add((Uri) o);
      } else {
        flushConcat(concat, builder);
        builder.append("<script type=\"text/javascript\">")
            .append(((Token<HtmlTokenType>) o).text).append("</script>");
      }
    }
    flushConcat(concat, builder);
    scripts.clear();
    isTagOpen = true;
    return builder.toString();
  }

  private void flushConcat(List<Uri> concat, StringBuilder builder) {
    if (concat.isEmpty()) {
      return;
    }
    builder.append("<script src=\"").append(concatBase);
    int urlStart = builder.length();
    int paramIndex = 1;
    try {
      for (int i = 0; i < concat.size(); i++) {
        Uri srcUrl = concat.get(i);
        if (!srcUrl.isAbsolute()) {
          srcUrl = relativeUrlBase.resolve(srcUrl);
        }
        builder.append(paramIndex).append('=')
            .append(URLEncoder.encode(srcUrl.toString(), "UTF-8"));
        if (i < concat.size() - 1) {
          if (builder.length() - urlStart > MAX_URL_LENGTH) {
            paramIndex = 1;
            builder.append("\" type=\"text/javascript\"></script>\n");
            builder.append("<script src=\"").append(concatBase);
            urlStart = builder.length();
          } else {
            builder.append('&');
            paramIndex++;
          }
        }
      }
      builder.append("\" type=\"text/javascript\"></script>");
      concat.clear();
    } catch (UnsupportedEncodingException uee) {
      throw new RuntimeException(uee);
    }
  }

  private String stripQuotes(String s) {
    return s.replaceAll("\"", "").replaceAll("'","");
  }
}
