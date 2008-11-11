/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.gadgets.rewrite;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.util.Utf8UrlCoder;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.parse.DomUtil;
import org.apache.shindig.gadgets.servlet.ProxyBase;
import org.apache.shindig.gadgets.spec.View;

import com.google.common.base.Nullable;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Perform rewriting of HTML content including
 * - Concatenating & proxying of referred script content
 * - Concatenating & proxying of stylesheet links
 * - Moving all style into head and converting @imports into links
 * - Proxying referred content of images and embeds
 */
public class HTMLContentRewriter  implements ContentRewriter {
  private final static int MAX_URL_LENGTH = 1500;
  private static final String DEFAULT_CONCAT_URL_BASE = "/gadgets/concat?";

  public final static Set<String> TAGS =
      Sets.newHashSet("img", "embed", "link", "script", "style");
                                                                                       
  private final static Map<String, Set<String>> LINKING_TAG_ATTRS = Maps.newHashMap();

  static {
    LINKING_TAG_ATTRS.put("img", Sets.newHashSet("src"));
    LINKING_TAG_ATTRS.put("embed", Sets.newHashSet("src"));
  }

  private final ContentRewriterFeatureFactory rewriterFeatureFactory;
  private final String proxyBaseNoGadget;
  private final String concatBaseNoGadget;

  @Inject
  public HTMLContentRewriter(ContentRewriterFeatureFactory rewriterFeatureFactory,
      @Named("shindig.content-rewrite.proxy-url")String proxyBaseNoGadget,
      @Named("shindig.content-rewrite.concat-url")String concatBaseNoGadget) {
    this.rewriterFeatureFactory = rewriterFeatureFactory;
    if (concatBaseNoGadget != null) {
      this.concatBaseNoGadget = concatBaseNoGadget;
    } else {
      this.concatBaseNoGadget = DEFAULT_CONCAT_URL_BASE;
    }
    this.proxyBaseNoGadget = proxyBaseNoGadget;
  }

  public RewriterResults rewrite(HttpRequest request, HttpResponse original,
      MutableContent content) {
    if (RewriterUtils.isHtml(request, original)) {
      ContentRewriterFeature feature = rewriterFeatureFactory.get(request);
      return rewriteImpl(feature, request.getGadget(), request.getUri(), content);
    }
    return null;
  }

  public RewriterResults rewrite(Gadget gadget, MutableContent content) {
    ContentRewriterFeature feature = rewriterFeatureFactory.get(gadget.getSpec());
    Uri contentBase = gadget.getSpec().getUrl();
    View view = gadget.getCurrentView();
    if (view != null && view.getHref() != null) {
      contentBase = view.getHref();
    }
    return rewriteImpl(feature, gadget.getSpec().getUrl(), contentBase, content);
  }

  protected RewriterResults rewriteImpl(ContentRewriterFeature feature, Uri gadgetUri,
                                        Uri contentBase, MutableContent content) {
    if (!feature.isRewriteEnabled() || content.getDocument() == null) {
      return null;
    }

    // Get ALL interesting tags
    List<Node> nodeList =
        DomUtil.getElementsByTagNameCaseInsensitive(content.getDocument(), TAGS);

    Node head = DomUtil.getFirstNamedChildNode(content.getDocument().getDocumentElement(), "head");

    boolean mutated = false;


    // 1st step. Rewrite links in all embedded style tags. Convert @import statements into
    // links and add them to the node list.
    // Move all style and link tags into head and concat the link tags
    mutated = rewriteStyleTags(head, nodeList, feature, gadgetUri, contentBase);
    // Concat script links
    mutated |= rewriteJsTags(nodeList, feature, gadgetUri, contentBase);
    // Rewrite links in images, embeds etc
    mutated |= rewriteContentReferences(nodeList, feature, gadgetUri, contentBase);

    if (mutated) {
      MutableContent.notifyEdit(content.getDocument());
    }

    return RewriterResults.cacheableIndefinitely();
  }

  protected boolean rewriteStyleTags(Node head, List<Node> nodeList,
      ContentRewriterFeature feature, Uri gadgetUri, Uri contentBase) {
    if (!feature.getIncludedTags().contains("style")) {
      return false;
    }
    boolean mutated = false;

    // Filter to just style tags
    Iterable<Node> styleTags = Lists.newArrayList(Iterables.filter(nodeList, new Predicate<Node>() {
      public boolean apply(@Nullable Node node) {
        return node.getNodeName().equalsIgnoreCase("style");
      }
    }));

    LinkRewriter linkRewriter = createLinkRewriter(gadgetUri.toJavaUri(), feature);

    for (Node styleNode : styleTags) {
      mutated |= true;
      if (styleNode.getParentNode() != head) {
        styleNode.getParentNode().removeChild(styleNode);
        head.appendChild(styleNode);
      }
      String styleText = styleNode.getTextContent();
      StringWriter sw = new StringWriter(styleText.length());
      List<String> extractedUrls = CssRewriter.rewrite(new StringReader(styleText),
          contentBase.toJavaUri(), linkRewriter, sw, true);
      styleText = sw.toString().trim();
      if (styleText.length() == 0 || (styleText.length() < 25 &&
        styleText.replace("<!--", "").replace("//-->", "").
            replace("-->", "").trim().length() == 0)) {
        styleNode.getParentNode().removeChild(styleNode);
        nodeList.remove(styleNode);
      } else {
        styleNode.setTextContent(styleText);
      }
      for (String extractedUrl : extractedUrls) {
        // Add extracted urls as link elements to head
        Element newLink = head.getOwnerDocument().createElement("link");
        newLink.setAttribute("rel", "stylesheet");
        newLink.setAttribute("type", "text/css");
        newLink.setAttribute("href", extractedUrl);
        head.appendChild(newLink);
        nodeList.add(newLink);
      }
    }

    // Filter to just stylesheet link tags
    List<Node> linkTags = Lists.newArrayList(Iterables.filter(nodeList, new Predicate<Node>() {
      public boolean apply(@Nullable Node node) {
         return node.getNodeName().equalsIgnoreCase("link") &&
          (node.getAttributes().getNamedItem("rel").
              getNodeValue().equalsIgnoreCase("stylesheet") ||
           node.getAttributes().getNamedItem("type").
               getNodeValue().toLowerCase().contains("css"));
      }
    }));

    String concatBase = getConcatBase(gadgetUri.toJavaUri(), feature,
      "text/css");

    concatenateTags(feature, linkTags, concatBase, contentBase, "href");

    return mutated;
  }

  protected LinkRewriter createLinkRewriter(URI gadgetUri, ContentRewriterFeature feature) {
    return new ProxyingLinkRewriter(gadgetUri, feature, proxyBaseNoGadget);
  }

  protected String getConcatBase(URI gadgetUri, ContentRewriterFeature feature, String mimeType) {
    return concatBaseNoGadget +
           ProxyBase.REWRITE_MIME_TYPE_PARAM +
           "=" + mimeType +
           ((gadgetUri == null) ? "" : "&gadget=" + Utf8UrlCoder.encode(gadgetUri.toString())) +
           "&fp=" + feature.getFingerprint() +'&';
  }

  protected boolean rewriteJsTags(List<Node> nodeList, ContentRewriterFeature feature,
      Uri gadgetUri, Uri contentBase) {
    if (!feature.getIncludedTags().contains("script")) {
      return false;
    }
    boolean mutated = false;

    // Filter to just script tags
    List<Node> scriptNodes = Lists.newArrayList(Iterables.filter(nodeList, new Predicate<Node>() {
      public boolean apply(@Nullable Node node) {
        return node.getNodeName().equalsIgnoreCase("script");
      }
    }));

    String concatBase = getConcatBase(gadgetUri.toJavaUri(), feature, "text/javascript");
    List<Node> concatenateable = new ArrayList<Node>();
    for (int i = 0; i < scriptNodes.size(); i++) {
      Node scriptTag = scriptNodes.get(i);
      Node nextSciptTag = null;
      if (i + 1 < scriptNodes.size()) {
        nextSciptTag = scriptNodes.get(i+1);
      }
      Node src = scriptTag.getAttributes().getNamedItem("src");
      if (src != null && feature.shouldRewriteURL(src.getNodeValue())) {
        mutated = true;
        concatenateable.add(scriptTag);
        if (nextSciptTag == null ||
            !nextSciptTag.equals(getNextSiblingElement(scriptTag))) {
          // Next tag is not concatenateable
          concatenateTags(feature, concatenateable, concatBase, contentBase, "src");
          concatenateable.clear();
        }
      } else {
        concatenateTags(feature, concatenateable, concatBase, contentBase, "src");
        concatenateable.clear();
      }
    }
    concatenateTags(feature, concatenateable, concatBase, contentBase, "src");
    return mutated;
  }

  protected boolean rewriteContentReferences(List<Node> nodeList, ContentRewriterFeature feature,
      Uri gadgetUri, Uri contentBase) {
    boolean mutated = false;
    LinkRewriter rewriter = createLinkRewriter(gadgetUri.toJavaUri(), feature);

    final Set<String> tagNames = LINKING_TAG_ATTRS.keySet();
    tagNames.retainAll(feature.getIncludedTags());

    // Filter to just style tags
    Iterable<Node> tags = Iterables.filter(nodeList, new Predicate<Node>() {
      public boolean apply(@Nullable Node node) {
        return tagNames.contains(node.getNodeName().toLowerCase());
      }
    });

    for (Node node : tags) {
      NamedNodeMap attributes = node.getAttributes();
      Set<String> rewriteable = LINKING_TAG_ATTRS.get(node.getNodeName().toLowerCase());
      for (int i = 0; i < attributes.getLength(); i++) {
        Node attr = attributes.item(i);
        if (rewriteable.contains(attr.getNodeName().toLowerCase())) {
          mutated = true;
          attr.setNodeValue(rewriter.rewrite(attr.getNodeValue(), contentBase.toJavaUri()));
        }
      }
    }
    return mutated;
  }

  private static void concatenateTags(final ContentRewriterFeature feature,
                               List<Node> tags, String concatBase, Uri contentBase,
                               final String attr) {
    // Filter out excluded URLs
    tags = Lists.newArrayList(Iterables.filter(tags, new Predicate<Node>() {
      public boolean apply(@Nullable Node node) {
        Node item = node.getAttributes().getNamedItem(attr);
        return (item != null && feature.shouldRewriteURL(item.getNodeValue()));
      }
    }));

    // Eliminate duplicates while maintaining order
    LinkedHashSet<Uri> nodeRefList = Sets.newLinkedHashSet();
    for (Node tag : tags) {
      try {
        nodeRefList.add(
            contentBase.resolve(
                Uri.parse(tag.getAttributes().getNamedItem(attr).getNodeValue())));
      } catch (IllegalArgumentException e) {
        // Same behavior as JavascriptTagMerger
        // Perhaps switch to ignoring script src instead?
        throw new RuntimeException(e);
      }
    }

    List<Uri> concatented = getConcatenatedUris(concatBase, nodeRefList);
    for (int i = 0; i < tags.size(); i++) {
      if (i < concatented.size()) {
        // Set new URLs into existing tags
        tags.get(i).getAttributes().getNamedItem(attr).setNodeValue(
            concatented.get(i).toString());
      } else {
        // Remove remainder
        tags.get(i).getParentNode().removeChild(tags.get(i));
      }
    }
  }

  private static List<Uri> getConcatenatedUris(String concatBase, LinkedHashSet<Uri> uris) {
    List<Uri> concatUris = new LinkedList<Uri>();
    int paramIndex = 1;
    StringBuilder builder = null;
    int maxUriLen = MAX_URL_LENGTH + concatBase.length();
    try {
      int uriIx = 0, lastUriIx = (uris.size() - 1);
      //
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


  private Node getNextSiblingElement(Node n) {
    n = n.getNextSibling();
    while (n != null && n.getNodeType() != Node.ELEMENT_NODE) {
      n = n.getNextSibling();
    }
    return n;
  }

}
