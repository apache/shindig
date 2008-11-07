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

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.parse.DomUtil;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LinkingTagContentRewriter implements ContentRewriter {

  private final static Map<String, Set<String>> SUPPORTED_TAG_ATTRS = Maps.newHashMap();

  static {
    SUPPORTED_TAG_ATTRS.put("img", Sets.newHashSet("src"));
    SUPPORTED_TAG_ATTRS.put("embed", Sets.newHashSet("src"));
    SUPPORTED_TAG_ATTRS.put("link", Sets.newHashSet("href"));
  }

  private final ContentRewriterFeatureFactory rewriterFactory;
  private final String proxyUrl;

  @Inject
  public LinkingTagContentRewriter(ContentRewriterFeatureFactory rewriterFactory,
      @Named("shindig.content-rewrite.proxy-url")String proxyUrl) {
    this.rewriterFactory = rewriterFactory;
    this.proxyUrl = proxyUrl;
  }

  public RewriterResults rewrite(HttpRequest request, HttpResponse original, MutableContent content) {
    return rewriteImpl(rewriterFactory.get(request), content,
        request.getUri(), request.getGadget());
  }

  public RewriterResults rewrite(Gadget gadget, MutableContent content) {
    return rewriteImpl(rewriterFactory.get(gadget.getSpec()), content,
        gadget.getSpec().getUrl(), gadget.getSpec().getUrl());
  }

  protected RewriterResults rewriteImpl(ContentRewriterFeature feature, MutableContent content,
                                        Uri baseUri, Uri gadgetUri) {
    if (content.getDocument() == null) return null;
    
    boolean mutated = false;
    LinkRewriter rewriter = createLinkRewriter(gadgetUri.toJavaUri(), feature);

    Set<String> tags = SUPPORTED_TAG_ATTRS.keySet();
    tags.retainAll(feature.getIncludedTags());
    List<Node> nodes = DomUtil.getElementsByTagNameCaseInsensitive(
        content.getDocument(), tags);

    for (Node node : nodes) {
      NamedNodeMap attributes = node.getAttributes();
      Set<String> rewriteable = SUPPORTED_TAG_ATTRS.get(node.getNodeName().toLowerCase());
      for (int i = 0; i < attributes.getLength(); i++) {
        Node attr = attributes.item(i);
        if (rewriteable.contains(attr.getNodeName().toLowerCase())) {
          mutated = true;
          attr.setNodeValue(rewriter.rewrite(attr.getNodeValue(), baseUri.toJavaUri()));
        }
      }
    }
    if (mutated) {
      MutableContent.notifyEdit(content.getDocument());
    }

    return RewriterResults.cacheableIndefinitely();
  }

  protected LinkRewriter createLinkRewriter(URI gadgetUri, ContentRewriterFeature feature) {
    return new ProxyingLinkRewriter(gadgetUri, feature, proxyUrl);
  }
}