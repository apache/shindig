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

import com.google.common.base.Nullable;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.util.Utf8UrlCoder;
import org.apache.shindig.common.xml.DomUtil;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.servlet.ProxyBase;
import org.apache.shindig.gadgets.spec.View;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.LinkedHashSet;
import java.util.List;
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
  
  private final static String JS_MIME_TYPE = "text/javascript";

  public final static Set<String> TAGS = ImmutableSet.of("img", "embed", "link", "script", "style");

  private final static ImmutableMap<String, ImmutableSet<String>> LINKING_TAG_ATTRS = ImmutableMap.of(
      "img", ImmutableSet.of("src"),
      "embed", ImmutableSet.of("src")
  );

  private final ContentRewriterFeatureFactory rewriterFeatureFactory;
  private final String proxyBaseNoGadget;
  private final String concatBaseNoGadget;

  @Inject
  public HTMLContentRewriter(ContentRewriterFeatureFactory rewriterFeatureFactory,
      @Named("shindig.content-rewrite.proxy-url")String proxyBaseNoGadget,
      @Named("shindig.content-rewrite.concat-url")String concatBaseNoGadget) {
    this.rewriterFeatureFactory = rewriterFeatureFactory;
    this.concatBaseNoGadget = concatBaseNoGadget;
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
    List<Element> tagList =
        DomUtil.getElementsByTagNameCaseInsensitive(content.getDocument(), TAGS);

    Element head = (Element)DomUtil.getFirstNamedChildNode(
        content.getDocument().getDocumentElement(), "head");

    boolean mutated = false;


    // 1st step. Rewrite links in all embedded style tags. Convert @import statements into
    // links and add them to the tag list.
    // Move all style and link tags into head and concat the link tags
    mutated = rewriteStyleTags(head, tagList, feature, gadgetUri, contentBase);
    // Concat script links
    mutated |= rewriteJsTags(tagList, feature, gadgetUri, contentBase);
    // Rewrite links in images, embeds etc
    mutated |= rewriteContentReferences(tagList, feature, gadgetUri, contentBase);

    if (mutated) {
      MutableContent.notifyEdit(content.getDocument());
    }

    return RewriterResults.cacheableIndefinitely();
  }

  protected boolean rewriteStyleTags(Element head, List<Element> elementList,
      ContentRewriterFeature feature, Uri gadgetUri, Uri contentBase) {
    if (!feature.getIncludedTags().contains("style")) {
      return false;
    }
    boolean mutated = false;

    // Filter to just style tags
    Iterable<Element> styleTags = Lists.newArrayList(Iterables.filter(elementList,
        new Predicate<Element>() {
      public boolean apply(@Nullable Element element) {
        return element.getNodeName().equalsIgnoreCase("style");
      }
    }));

    LinkRewriter linkRewriter = createLinkRewriter(gadgetUri, feature);

    for (Element styleTag : styleTags) {
      mutated |= true;
      if (styleTag.getParentNode() != head) {
        styleTag.getParentNode().removeChild(styleTag);
        head.appendChild(styleTag);
      }
      String styleText = styleTag.getTextContent();
      StringWriter sw = new StringWriter(styleText.length());
      List<String> extractedUrls = CssRewriter.rewrite(new StringReader(styleText),
          contentBase, linkRewriter, sw, true);
      styleText = sw.toString().trim();
      if (styleText.length() == 0 || (styleText.length() < 25 &&
        styleText.replace("<!--", "").replace("//-->", "").
            replace("-->", "").trim().length() == 0)) {
        styleTag.getParentNode().removeChild(styleTag);
        elementList.remove(styleTag);
      } else {
        styleTag.setTextContent(styleText);
      }
      for (String extractedUrl : extractedUrls) {
        // Add extracted urls as link elements to head
        Element newLink = head.getOwnerDocument().createElement("link");
        newLink.setAttribute("rel", "stylesheet");
        newLink.setAttribute("type", "text/css");
        newLink.setAttribute("href", extractedUrl);
        head.appendChild(newLink);
        elementList.add(newLink);
      }
    }

    // Filter to just stylesheet link tags
    List<Element> linkTags = Lists.newArrayList(Iterables.filter(elementList,
        new Predicate<Element>() {
          public boolean apply(@Nullable Element element) {
            return element.getNodeName().equalsIgnoreCase("link") &&
                ("stylesheet".equalsIgnoreCase(element.getAttribute("rel")) ||
                    element.getAttribute("type").toLowerCase().contains("css"));
          }
        }));

    String concatBase = getConcatBase(gadgetUri, feature, "text/css");

    concatenateTags(feature, linkTags, concatBase, contentBase, "href");

    return mutated;
  }

  protected LinkRewriter createLinkRewriter(Uri gadgetUri, ContentRewriterFeature feature) {
    return new ProxyingLinkRewriter(gadgetUri, feature, proxyBaseNoGadget);
  }

  protected String getConcatBase(Uri gadgetUri, ContentRewriterFeature feature, String mimeType) {
    return concatBaseNoGadget +
           ProxyBase.REWRITE_MIME_TYPE_PARAM +
        '=' + mimeType +
           ((gadgetUri == null) ? "" : "&gadget=" + Utf8UrlCoder.encode(gadgetUri.toString())) +
           "&fp=" + feature.getFingerprint() +'&';
  }

  protected boolean rewriteJsTags(List<Element> elementList, ContentRewriterFeature feature,
      Uri gadgetUri, Uri contentBase) {
    if (!feature.getIncludedTags().contains("script")) {
      return false;
    }
    boolean mutated = false;

    // Filter to just script tags
    List<Element> scriptTags = Lists.newArrayList(Iterables.filter(elementList,
        new Predicate<Element>() {
      public boolean apply(@Nullable Element node) {
        if (node.getNodeName().equalsIgnoreCase("script")) {
          String type = node.getAttribute("type");
          return type == null || type.length() == 0 || type.equalsIgnoreCase(JS_MIME_TYPE);
        }
        return false;
      }
    }));

    String concatBase = getConcatBase(gadgetUri, feature, JS_MIME_TYPE);
    List<Element> concatenateable = Lists.newArrayList();
    for (int i = 0; i < scriptTags.size(); i++) {
      Element scriptTag = scriptTags.get(i);
      Element nextSciptTag = null;
      if (i + 1 < scriptTags.size()) {
        nextSciptTag = scriptTags.get(i+1);
      }
      if (scriptTag.hasAttribute("src") &&
          feature.shouldRewriteURL(scriptTag.getAttribute("src"))) {
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

  protected boolean rewriteContentReferences(List<Element> elementList,
      ContentRewriterFeature feature, Uri gadgetUri, Uri contentBase) {
    boolean mutated = false;
    LinkRewriter rewriter = createLinkRewriter(gadgetUri, feature);

    final Set<String> tagNames = Sets.intersection(LINKING_TAG_ATTRS.keySet(), feature.getIncludedTags());

    // Filter to just style tags
    Iterable<Element> tags = Iterables.filter(elementList, new Predicate<Element>() {
      public boolean apply(@Nullable Element node) {
        return tagNames.contains(node.getNodeName().toLowerCase());
      }
    });

    for (Element node : tags) {
      NamedNodeMap attributes = node.getAttributes();
      Set<String> rewriteable = LINKING_TAG_ATTRS.get(node.getNodeName().toLowerCase());
      for (int i = 0; i < attributes.getLength(); i++) {
        Node attr = attributes.item(i);
        if (rewriteable.contains(attr.getNodeName().toLowerCase())) {
          mutated = true;
          attr.setNodeValue(rewriter.rewrite(attr.getNodeValue(), contentBase));
        }
      }
    }
    return mutated;
  }

  private static void concatenateTags(final ContentRewriterFeature feature,
                               List<Element> tags, String concatBase, Uri contentBase,
                               final String attr) {
    // Filter out excluded URLs
    tags = Lists.newArrayList(Iterables.filter(tags, new Predicate<Element>() {
      public boolean apply(@Nullable Element element) {
        return (element.hasAttribute(attr) && feature.shouldRewriteURL(element.getAttribute(attr)));
      }
    }));

    // Eliminate duplicates while maintaining order
    LinkedHashSet<Uri> nodeRefList = Sets.newLinkedHashSet();
    for (Element tag : tags) {
      try {
        nodeRefList.add(contentBase.resolve(Uri.parse(tag.getAttribute(attr))));
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
        tags.get(i).setAttribute(attr, concatented.get(i).toString());
      } else {
        // Remove remainder
        tags.get(i).getParentNode().removeChild(tags.get(i));
      }
    }
  }

  private static List<Uri> getConcatenatedUris(String concatBase, LinkedHashSet<Uri> uris) {
    List<Uri> concatUris = Lists.newLinkedList();
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
          builder.append('&');
        }
        builder.append(paramIndex).append('=')
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


  private Element getNextSiblingElement(Element elem) {
    Node n = elem;
    n = n.getNextSibling();
    while (n != null && n.getNodeType() != Node.ELEMENT_NODE) {
      n = n.getNextSibling();
    }
    return (Element)n;
  }
}
