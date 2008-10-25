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

import com.google.common.collect.Lists;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.util.Utf8UrlCoder;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.servlet.ProxyBase;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.View;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class JsTagConcatContentRewriter implements ContentRewriter {
  private final static int MAX_URL_LENGTH = 1500;

  private final ContentRewriterFeature.Factory rewriterFeatureFactory;
  private final String concatUrlBase;

  private static final String DEFAULT_CONCAT_URL_BASE = "/gadgets/concat?";

  public JsTagConcatContentRewriter(ContentRewriterFeature.Factory rewriterFeatureFactory,
      String concatUrlBase) {
    this.rewriterFeatureFactory = rewriterFeatureFactory;
    if (concatUrlBase != null) {
      this.concatUrlBase = concatUrlBase;
    } else {
      this.concatUrlBase = DEFAULT_CONCAT_URL_BASE;
    }
  }

  public RewriterResults rewrite(HttpRequest request, HttpResponse original,
      MutableContent content) {
    // JS Concatenation not supported for HTTP responses at present.
    return null;
  }

  public RewriterResults rewrite(Gadget gadget, MutableContent content) {
    ContentRewriterFeature rewriterFeature = rewriterFeatureFactory.get(gadget.getSpec());
    if (!rewriterFeature.isRewriteEnabled() ||
        !rewriterFeature.getIncludedTags().contains("script")) {
      return null;
    }

    // Get all the script tags
    NodeList scriptTags = content.getDocument().getElementsByTagName("SCRIPT");

    // Copy NodeList as it respects changes to the underlying document which is a
    // behavior we dont want when removing nodes
    List<Node> nodeList = Lists.newArrayListWithExpectedSize(scriptTags.getLength());
    for (int i = 0; i < scriptTags.getLength(); i++) {
      nodeList.add(scriptTags.item(i));
    }

    String concatBase = getJsConcatBase(gadget.getSpec(), rewriterFeature);
    Uri contentBase = gadget.getSpec().getUrl();
    View view = gadget.getCurrentView();
    if (view != null && view.getHref() != null) {
      contentBase = view.getHref();
    }

    boolean mutated = false;
    List<Node> concatenateable = new ArrayList<Node>();
    for (int i = 0; i < nodeList.size(); i++) {
      Node scriptTag = nodeList.get(i);
      Node nextSciptTag = null;
      if (i + 1 < nodeList.size()) {
        nextSciptTag = nodeList.get(i+1);
      }
      Node src = scriptTag.getAttributes().getNamedItem("src");
      if (src != null) {
        mutated = true;
        concatenateable.add(scriptTag);
        if (nextSciptTag == null ||
            !nextSciptTag.equals(getNextSiblingElement(scriptTag))) {
          // Next tag is not concatenateable
          concatenateTags(concatenateable, concatBase, contentBase);
          concatenateable.clear();
        }
      } else {
        concatenateTags(concatenateable, concatBase, contentBase);
        concatenateable.clear();
      }
    }
    concatenateTags(concatenateable, concatBase, contentBase);

    if (mutated) {
      MutableContent.notifyEdit(content.getDocument());
    }

    return RewriterResults.cacheableIndefinitely();
  }

  private void concatenateTags(List<Node> tags, String concatBase, Uri contentBase) {
    List<Uri> scriptSrcList = Lists.newArrayListWithExpectedSize(tags.size());
    for (Node scriptNode : tags) {
      try {
        scriptSrcList.add(
            contentBase.resolve(
                Uri.parse(scriptNode.getAttributes().getNamedItem("src").getNodeValue())));
      } catch (IllegalArgumentException e) {
        // Same behavior as JavascriptTagMerger
        // Perhaps switch to ignoring script src instead?
        throw new RuntimeException(e);
      }
    }

    List<Uri> concatented = getConcatenatedUris(concatBase, scriptSrcList);
    for (int i = 0; i < tags.size(); i++) {
      if (i < concatented.size()) {
        // Set new URLs into existing tags
        tags.get(i).getAttributes().getNamedItem("src").setNodeValue(
            concatented.get(i).toString());
      } else {
        // Remove remainder
        tags.get(i).getParentNode().removeChild(tags.get(i));
      }
    }
  }

  private List<Uri> getConcatenatedUris(String concatBase, List<Uri> uris) {
    List<Uri> concatUris = new LinkedList<Uri>();
    int paramIndex = 1;
    StringBuilder builder = null;
    int maxUriLen = MAX_URL_LENGTH + concatBase.length();
    try {
      int uriIx = 0, lastUriIx = (uris.size() - 1);
      for (Uri uri : uris) {
        if (paramIndex == 1) {
          builder = new StringBuilder(concatBase);
        } else {
          builder.append("&");
        }
        builder.append(paramIndex).append("=")
            .append(URLEncoder.encode(uri.toString(), "UTF-8"));
        if (builder.length() > maxUriLen ||
            uriIx == lastUriIx) {
          // Went over URI length warning limit or on the last uri
          concatUris.add(Uri.parse(builder.toString()));
          builder = null;
          paramIndex = 0;
        }
        ++paramIndex;
        ++uriIx;
      }
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
    return concatUris;
  }

  String getJsConcatBase(GadgetSpec spec, ContentRewriterFeature rewriterFeature) {
    return concatUrlBase +
           ProxyBase.REWRITE_MIME_TYPE_PARAM +
           "=text/javascript&" +
           "gadget=" +
           Utf8UrlCoder.encode(spec.getUrl().toString()) +
           "&fp=" +
           rewriterFeature.getFingerprint() +
           '&';
  }

  private Node getNextSiblingElement(Node n) {
    n = n.getNextSibling();
    while (n != null && n.getNodeType() != Node.ELEMENT_NODE) {
      n = n.getNextSibling();
    }
    return n;
  }

}
