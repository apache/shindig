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

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.parse.DomUtil;
import org.apache.shindig.gadgets.spec.View;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.net.URI;
import java.util.List;

public class StyleLinksContentRewriter implements ContentRewriter {
  private final ContentRewriterFeatureFactory rewriterFeatureFactory;
  private final String proxyUrl;

  @Inject
  public StyleLinksContentRewriter(ContentRewriterFeatureFactory rewriterFeatureFactory,
      @Named("shindig.content-rewrite.proxy-url")String proxyUrl) {
    this.rewriterFeatureFactory = rewriterFeatureFactory;
    this.proxyUrl = proxyUrl;
  }

  public RewriterResults rewrite(HttpRequest request, HttpResponse original,
      MutableContent content) {
    ContentRewriterFeature rewriterFeature = rewriterFeatureFactory.get(request);
    if (!rewriterFeature.isRewriteEnabled() ||
        !rewriterFeature.getIncludedTags().contains("style")) {
      return null;
    }

    if (RewriterUtils.isHtml(request, original)) {
      rewriteHtml(content.getDocument(), request.getUri().toJavaUri(),
          createLinkRewriter(request.getGadget().toJavaUri(), rewriterFeature));
    } else if (RewriterUtils.isCss(request, original)) {
      content.setContent(rewriteCss(content.getContent(), request.getUri().toJavaUri(),
          createLinkRewriter(request.getGadget().toJavaUri(), rewriterFeature)));
    }
    return RewriterResults.cacheableIndefinitely();
  }

  public RewriterResults rewrite(Gadget gadget, MutableContent content) {
    ContentRewriterFeature rewriterFeature = rewriterFeatureFactory.get(gadget.getSpec());
    if (!rewriterFeature.isRewriteEnabled() ||
        !rewriterFeature.getIncludedTags().contains("style")) {
      return null;
    }

    Uri base = gadget.getSpec().getUrl();
    View view = gadget.getCurrentView();
    if (view != null && view.getHref() != null) {
      base = view.getHref();
    }

    return rewriteHtml(content.getDocument(), base.toJavaUri(),
        createLinkRewriter(gadget.getSpec().getUrl().toJavaUri(), rewriterFeature));
  }

  protected LinkRewriter createLinkRewriter(URI gadgetUri, ContentRewriterFeature feature) {
    return new ProxyingLinkRewriter(gadgetUri, feature, proxyUrl);
  }

  private RewriterResults rewriteHtml(Document doc, URI baseUri, LinkRewriter linkRewriter) {
    if (doc == null) {
      return null;
    }
    boolean mutated = false;

    Node head = DomUtil.getFirstNamedChildNode(doc.getDocumentElement(), "head");

    // Move all style tags into head
    // TODO Convert all @imports into a concatenated link tag
    List<Node> styleTags = DomUtil.getElementsByTagNameCaseInsensitive(doc,
        Sets.newHashSet("style"));
    for (Node styleNode : styleTags) {      
      mutated = true;
      if (styleNode.getParentNode() != head) {
        styleNode.getParentNode().removeChild(styleNode);
        head.appendChild(styleNode);
      }
      styleNode.setTextContent(rewriteCss(styleNode.getTextContent(), baseUri, linkRewriter));
    }

    if (mutated) {
      MutableContent.notifyEdit(doc);
    }
    return RewriterResults.cacheableIndefinitely();
  }

  private String rewriteCss(String styleText, URI baseUri, LinkRewriter linkRewriter) {
    return CssRewriter.rewrite(styleText, baseUri, linkRewriter);
  }
}
