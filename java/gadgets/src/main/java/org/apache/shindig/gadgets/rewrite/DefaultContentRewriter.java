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

import org.apache.shindig.gadgets.http.HttpResponse;

import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Default implementation of content rewriting.
 */
public class DefaultContentRewriter implements ContentRewriter {

  public DefaultContentRewriter() {
  }

  public HttpResponse rewrite(URI source, HttpResponse original) {
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream(
          (original.getContentLength() * 110) / 100);
      OutputStreamWriter output = new OutputStreamWriter(baos,
          original.getEncoding());
      if (rewrite(source,
          new InputStreamReader(original.getResponse(), original.getEncoding()),
          original.getHeader("Content-Type"),
          output)) {
        return new HttpResponse(original.getHttpStatusCode(),
            baos.toByteArray(),
            original.getAllHeaders());
      }
      return null;
    } catch (UnsupportedEncodingException uee) {
      throw new RuntimeException(uee);
    }
  }

  public String rewrite(URI source, String original, String mimeType) {
    StringWriter sw = new StringWriter();
    if (rewrite(source, new StringReader(original), mimeType, sw)) {
      return sw.toString();
    } else {
      return null;
    }
  }

  public boolean rewrite(URI source, Reader r, String mimeType, Writer w) {
    if (isHTML(mimeType)) {
      Map<String, HtmlTagTransformer> transformerMap
          = new HashMap<String, HtmlTagTransformer>();

      if (getProxyUrl() != null) {
        LinkRewriter linkRewriter = createLinkRewriter();
        LinkingTagRewriter rewriter = new LinkingTagRewriter(
            linkRewriter,
            source);
        for (String tag : rewriter.getSupportedTags()) {
          transformerMap.put(tag, rewriter);
        }
        transformerMap.put("style", new StyleTagRewriter(source, linkRewriter));
      }
      if (getConcatUrl() != null) {
        transformerMap
            .put("script", new JavascriptTagMerger(getConcatUrl(), source));
      }
      HtmlRewriter.rewrite(r, source, transformerMap, w);
      return true;
    } else if (isCSS(mimeType)) {
      if (getProxyUrl() != null) {
        CssRewriter.rewrite(r, source, createLinkRewriter(), w);
        return true;
      } else {
        return false;
      }
    }
    return false;
  }

  private boolean isHTML(String mime) {
    return (mime.toLowerCase().indexOf("html") != -1);
  }

  private boolean isCSS(String mime) {
    return (mime.toLowerCase().indexOf("css") != -1);
  }

  protected String getProxyUrl() {
    return "/gadgets/proxy?url=";
  }

  protected String getConcatUrl() {
    return "/gadgets/concat?";
  }

  protected LinkRewriter createLinkRewriter() {
    return new ProxyingLinkRewriter(getProxyUrl());
  }
}
