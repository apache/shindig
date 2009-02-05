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
package org.apache.shindig.gadgets.render;

import org.apache.sanselan.ImageFormat;
import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.Sanselan;
import org.apache.sanselan.common.byteSources.ByteSourceInputStream;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.parse.caja.CajaCssSanitizer;
import org.apache.shindig.gadgets.rewrite.ContentRewriter;
import org.apache.shindig.gadgets.rewrite.ContentRewriterFeature;
import org.apache.shindig.gadgets.rewrite.ContentRewriterFeatureFactory;
import org.apache.shindig.gadgets.rewrite.LinkRewriter;
import org.apache.shindig.gadgets.rewrite.MutableContent;
import org.apache.shindig.gadgets.rewrite.ProxyingLinkRewriter;
import org.apache.shindig.gadgets.rewrite.RewriterResults;
import org.apache.shindig.gadgets.servlet.ProxyBase;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.BindingAnnotation;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A content rewriter that will sanitize output for simple 'badge' like display.
 *
 * This is intentionally not as robust as Caja. It is a simple element whitelist. It can not be used
 * for sanitizing either javascript or CSS. CSS is desired in the long run, but it can't be proven
 * safe in the short term.
 *
 * Generally used in conjunction with a gadget that gets its dynamic behavior externally (proxied
 * rendering, OSML, etc.)
 */
public class SanitizedRenderingContentRewriter implements ContentRewriter {
  private static final Logger logger =
      Logger.getLogger(SanitizedRenderingContentRewriter.class.getName());

  private static final Set<String> URI_ATTRIBUTES = ImmutableSet.of("href", "src");

  // Attributes to forcibly rewrite and require an image mime type
  private static final Map<String, ImmutableSet<String>> PROXY_IMAGE_ATTRIBUTES =
      ImmutableMap.of("img", ImmutableSet.of("src"));

  private final Set<String> allowedTags;
  private final Set<String> allowedAttributes;
  private final CajaCssSanitizer cssSanitizer;
  private final ContentRewriterFeatureFactory rewriterFeatureFactory;
  private final String proxyBaseNoGadget;

  @Inject
  public SanitizedRenderingContentRewriter(@AllowedTags Set<String> allowedTags,
      @AllowedAttributes Set<String> allowedAttributes,
      ContentRewriterFeatureFactory rewriterFeatureFactory,
      @Named("shindig.content-rewrite.proxy-url")String proxyBaseNoGadget,
      CajaCssSanitizer cssSanitizer) {
    this.allowedTags = allowedTags;
    this.allowedAttributes = allowedAttributes;
    this.cssSanitizer = cssSanitizer;
    this.rewriterFeatureFactory = rewriterFeatureFactory;
    this.proxyBaseNoGadget = proxyBaseNoGadget;
  }

  public RewriterResults rewrite(HttpRequest request, HttpResponse resp, MutableContent content) {
    // Content fetched through the proxy can stipulate that it must be sanitized.
    if (request.isSanitizationRequested()) {
      ContentRewriterFeature rewriterFeature =
          rewriterFeatureFactory.createRewriteAllFeature(request.getCacheTtl());
      if (request.getRewriteMimeType().equalsIgnoreCase("text/css")) {
        return rewriteProxiedCss(request, resp, content, rewriterFeature);
      } else if (request.getRewriteMimeType().toLowerCase().startsWith("image/")) {
        return rewriteProxiedImage(request, resp, content);
      } else {
        logger.log(Level.WARNING, "Request to sanitize unknown content type "
            + request.getRewriteMimeType()
            + " for " + request.getUri().toString());
        content.setContent("");
        return RewriterResults.notCacheable();
      }
    } else {
      // No Op
      return null;
    }
  }

  public RewriterResults rewrite(Gadget gadget, MutableContent content) {
    if ("1".equals(gadget.getContext().getParameter(ProxyBase.SANITIZE_CONTENT_PARAM))) {
      boolean sanitized = false;
      try {
        new NodeSanitizer(gadget).sanitize(content.getDocument().getDocumentElement());
        content.documentChanged();
        sanitized = true;
      } finally {
        // Defensively cleat the content in case of failure
        if (!sanitized) {
          content.setContent("");
        }
      }
    }
    return RewriterResults.notCacheable();
  }

  /**
   * We don't actually rewrite the image we just ensure that it is in fact a valid
   * and known image type.
   */
  private RewriterResults rewriteProxiedImage(HttpRequest request, HttpResponse resp,
      MutableContent content) {
    boolean imageIsSafe = false;
    try {
      String contentType = resp.getHeader("Content-Type");
      if (contentType == null || contentType.toLowerCase().startsWith("image/")) {
        // Unspecified or unknown image mime type.
        try {
          ImageFormat imageFormat = Sanselan
              .guessFormat(new ByteSourceInputStream(resp.getResponse(),
                  request.getUri().getPath()));
          if (imageFormat == ImageFormat.IMAGE_FORMAT_UNKNOWN) {
            logger.log(Level.INFO, "Unable to sanitize unknown image type "
                + request.getUri().toString());
            return RewriterResults.notCacheable();
          }
          imageIsSafe = true;
          // Return null to indicate that no rewriting occurred
          return null;
        } catch (IOException ioe) {
          throw new RuntimeException(ioe);
        } catch (ImageReadException ire) {
          throw new RuntimeException(ire);
        }
      } else {
        return RewriterResults.notCacheable();
      }
    } finally {
      if (!imageIsSafe) {
        content.setContent("");
      }
    }
  }

  /**
   * Sanitize a CSS file.
   */
  private RewriterResults rewriteProxiedCss(HttpRequest request, HttpResponse response,
      MutableContent content, ContentRewriterFeature rewriterFeature) {
    String sanitized = "";
    try {
      String contentType = response.getHeader("Content-Type");
      if (contentType == null || contentType.toLowerCase().startsWith("text/")) {
        SanitizingProxyingLinkRewriter cssLinkRewriter = new SanitizingProxyingLinkRewriter(
            request.getGadget(), rewriterFeature, proxyBaseNoGadget, "text/css");
        sanitized = cssSanitizer.sanitize(content.getContent(), request.getUri(), cssLinkRewriter);
        return RewriterResults.cacheable(response.getCacheTtl());
      } else {
        return RewriterResults.notCacheable();
      }
    } finally {
      // Set sanitized content in finally to ensure it is always cleared in
      // the case of errors
      content.setContent(sanitized);
    }
  }

  /**
   * Utiliity class to sanitize HTML nodes recursively.
   */
  class NodeSanitizer {

    private final LinkRewriter cssRewriter;
    private final LinkRewriter imageRewriter;
    private final Uri context;

    NodeSanitizer(Gadget gadget) {
      this.context = gadget.getSpec().getUrl();
      Integer expires = rewriterFeatureFactory.getDefault().getExpires();
      ContentRewriterFeature rewriterFeature =
          rewriterFeatureFactory.createRewriteAllFeature(expires == null ? -1 : expires);

      cssRewriter = new SanitizingProxyingLinkRewriter(gadget.getSpec().getUrl(),
          rewriterFeature, proxyBaseNoGadget, "text/css");
      imageRewriter = new SanitizingProxyingLinkRewriter(gadget.getSpec().getUrl(),
          rewriterFeature, proxyBaseNoGadget, "image/*");
    }

    private void sanitize(Node node) {
      switch (node.getNodeType()) {
        case Node.CDATA_SECTION_NODE:
        case Node.TEXT_NODE:
        case Node.ENTITY_REFERENCE_NODE:
          break;
        case Node.ELEMENT_NODE:
        case Node.DOCUMENT_NODE:
          Element element = (Element) node;
          if (allowedTags.contains(element.getTagName().toLowerCase())) {
            // TODO - Add special case for stylesheet LINK nodes.
            // Special case handling for style nodes
            if (element.getTagName().equalsIgnoreCase("style")) {
              cssSanitizer.sanitize(element, context, cssRewriter);
            }
            filterAttributes(element);
            for (Node child : toList(node.getChildNodes())) {
              sanitize(child);
            }
          } else {
            node.getParentNode().removeChild(node);
          }
          break;
        case Node.COMMENT_NODE:
        default:
          // Must remove all comments to avoid conditional comment evaluation.
          // There might be other, unknown types as well. Don't trust them.
          node.getParentNode().removeChild(node);
          break;
      }
    }

    private void filterAttributes(Element element) {
      Set<String> rewriteImageAttrs = PROXY_IMAGE_ATTRIBUTES.get(element.getNodeName().toLowerCase());
      for (Attr attribute : toList(element.getAttributes())) {
        String name = attribute.getNodeName().toLowerCase();
        if (allowedAttributes.contains(name)) {
          if (URI_ATTRIBUTES.contains(name)) {
            try {
              Uri uri = Uri.parse(attribute.getNodeValue());
              String scheme = uri.getScheme();
              if (!isAllowedScheme(scheme)) {
                element.removeAttributeNode(attribute);
              } else if (rewriteImageAttrs != null && rewriteImageAttrs.contains(name)) {
                // Force rewrite the src of the image through the proxy. This is necessary
                // because IE will run arbitrary script in files referenced from src
                attribute.setValue(imageRewriter.rewrite(attribute.getNodeValue(), context));
              }
            } catch (IllegalArgumentException e) {
              // Not a valid URI.
              element.removeAttributeNode(attribute);
            }
          }
        } else {
          element.removeAttributeNode(attribute);
        }
      }
    }
  }


  /** Convert a NamedNodeMap to a list for easy and safe operations */
  private static List<Attr> toList(NamedNodeMap nodes) {
    List<Attr> list = new ArrayList<Attr>(nodes.getLength());

    for (int i = 0, j = nodes.getLength(); i < j; ++i) {
      list.add((Attr) nodes.item(i));
    }

    return list;
  }

  /** Convert a NamedNodeMap to a list for easy and safe operations */
  private static List<Node> toList(NodeList nodes) {
    List<Node> list = new ArrayList<Node>(nodes.getLength());

    for (int i = 0, j = nodes.getLength(); i < j; ++i) {
      list.add(nodes.item(i));
    }

    return list;
  }

  private static boolean isAllowedScheme(String scheme) {
    return scheme == null || scheme.equals("http") || scheme.equals("https");
  }

  /**
   * Forcible rewrite the link through the proxy and force sanitization with
   * an expected mime type
   */
  static class SanitizingProxyingLinkRewriter extends ProxyingLinkRewriter {

    private final String expectedMime;

    SanitizingProxyingLinkRewriter(Uri gadgetUri, ContentRewriterFeature rewriterFeature,
        String prefix, String expectedMime) {
      super(gadgetUri, rewriterFeature, prefix);
      this.expectedMime = expectedMime;
    }

    @Override
    public String rewrite(String link, Uri context) {
      try {
        Uri.parse(link);
      } catch (RuntimeException re) {
        // Any failure in parse
        return "about:blank";
      }
      String rewritten = super.rewrite(link, context);
      rewritten += "&" + ProxyBase.SANITIZE_CONTENT_PARAM + "=1";
      rewritten += "&" + ProxyBase.REWRITE_MIME_TYPE_PARAM + "=" + expectedMime;
      return rewritten;
    }
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.PARAMETER)
  @BindingAnnotation
  public @interface AllowedTags { }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.PARAMETER)
  @BindingAnnotation
  public @interface AllowedAttributes { }
}
