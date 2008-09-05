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

import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.MutableContent;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.parse.GadgetHtmlNode;

import java.net.URI;

/**
 * Simple helper base class for ContentRewriters that manipulate an
 * HTML parse tree, whether in rewriting a {@code Gadget} or an
 * {@code HttpResponse}. Passes in the URI from which the content
 * was derived in doing so.
 */
public abstract class HtmlContentRewriter implements ContentRewriter {
  
  protected abstract void rewrite(GadgetHtmlNode root, URI baseUri);

  public static String getMimeType(HttpRequest request, HttpResponse original) {
    String mimeType = request.getRewriteMimeType();
    if (mimeType == null) {
      mimeType = original.getHeader("Content-Type");
    }
    return mimeType != null ? mimeType.toLowerCase() : null;
  }
  
  public void rewrite(HttpRequest request, HttpResponse original,
      MutableContent content) {
    String mimeType = getMimeType(request, original);
    if (mimeType.toLowerCase().contains("html")) {
      rewriteHtml(content.getParseTree(), request.getUri().toJavaUri());
    }
  }

  public void rewrite(Gadget gadget) {
    rewriteHtml(gadget.getParseTree(), gadget.getSpec().getUrl());
  }
  
  private void rewriteHtml(GadgetHtmlNode root, URI baseUri) {
    if (root != null) {
      rewrite(root, baseUri);
    }
  }

}
