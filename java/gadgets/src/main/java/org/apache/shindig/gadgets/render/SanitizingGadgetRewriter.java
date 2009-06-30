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

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.parse.caja.CajaCssSanitizer;
import org.apache.shindig.gadgets.rewrite.ContentRewriterFeature;
import org.apache.shindig.gadgets.rewrite.ContentRewriterFeatureFactory;
import org.apache.shindig.gadgets.rewrite.ContentRewriterUris;
import org.apache.shindig.gadgets.rewrite.GadgetRewriter;
import org.apache.shindig.gadgets.rewrite.LinkRewriter;
import org.apache.shindig.gadgets.rewrite.MutableContent;
import org.apache.shindig.gadgets.rewrite.ProxyingLinkRewriter;
import org.apache.shindig.gadgets.servlet.ProxyBase;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.UserDataHandler;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableList;
import com.google.inject.BindingAnnotation;
import com.google.inject.Inject;

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
public class SanitizingGadgetRewriter implements GadgetRewriter {

  /** Key stored as element user-data to bypass sanitization */
  private static final String BYPASS_SANITIZATION_KEY = "shindig.bypassSanitization";

  /**
   * Is the Gadget to be rendered sanitized?
   * @return true if sanitization will be enabled
   */
  public static boolean isSanitizedRenderingRequest(Gadget gadget) {
    return ("1".equals(gadget.getContext().getParameter("sanitize")));
  }
  
  /**
   * Marks that an element and all its attributes are trusted content.
   * This status is preserved across {@link Node#cloneNode} calls.  Be
   * extremely careful when using this, especially with {@code includingChildren}
   * set to {@code true}, as untrusted content that gets inserted (e.g, via
   * os:RenderAll in templating) would become trusted.
   * 
   * @param element the trusted element
   * @param includingChildren if true, children of this element will are also
   *     trusted.  Never set this to true on an element that will ever have
   *     untrusted children inserted (e.g., if it contains or may contain os:Render).
   */
  public static void bypassSanitization(Element element, boolean includingChildren) {
    element.setUserData(BYPASS_SANITIZATION_KEY,
        includingChildren ? Bypass.ALL : Bypass.ONLY_SELF, copyOnClone);
  }
  
  private static enum Bypass { ALL, ONLY_SELF, NONE };
  private static UserDataHandler copyOnClone = new UserDataHandler() {
    public void handle(short operation, String key, Object data, Node src, Node dst) {
      if (operation == NODE_IMPORTED || operation == NODE_CLONED) {
        dst.setUserData(key, data, copyOnClone);
      }
    }
  };
  
  private final Set<String> allowedTags;
  private final Set<String> allowedAttributes;
  private final CajaCssSanitizer cssSanitizer;
  private final ContentRewriterFeatureFactory rewriterFeatureFactory;
  private final ContentRewriterUris rewriterUris;

  @Inject
  public SanitizingGadgetRewriter(@AllowedTags Set<String> allowedTags,
      @AllowedAttributes Set<String> allowedAttributes,
      ContentRewriterFeatureFactory rewriterFeatureFactory,
      ContentRewriterUris rewriterUris,
      CajaCssSanitizer cssSanitizer) {
    this.allowedTags = allowedTags;
    this.allowedAttributes = allowedAttributes;
    this.rewriterUris = rewriterUris;
    this.cssSanitizer = cssSanitizer;
    this.rewriterFeatureFactory = rewriterFeatureFactory;
  }


  public void rewrite(Gadget gadget, MutableContent content) {
    if (gadget.sanitizeOutput()) {
      boolean sanitized = false;
      try {
        new NodeSanitizer(gadget).sanitize(content.getDocument().getDocumentElement());
        content.documentChanged();
        sanitized = true;
      } finally {
        // Defensively clean the content in case of failure
        if (!sanitized) {
          content.setContent("");
        }
      }
    }
  }

  /**
   * Utiliity class to sanitize HTML nodes recursively.
   */
  class NodeSanitizer {
    private final Uri context;
    private final List<DomFilter> filters;

    NodeSanitizer(Gadget gadget) {
      this.context = gadget.getSpec().getUrl();
      Integer expires = rewriterFeatureFactory.getDefault().getExpires();
      ContentRewriterFeature rewriterFeature =
          rewriterFeatureFactory.createRewriteAllFeature(expires == null ? -1 : expires);

      String proxyBaseNoGadget = rewriterUris.getProxyBase(gadget.getContext().getContainer());
      LinkRewriter cssImportRewriter = new SanitizingProxyingLinkRewriter(gadget.getSpec().getUrl(),
          rewriterFeature, proxyBaseNoGadget, "text/css");
      LinkRewriter imageRewriter = new SanitizingProxyingLinkRewriter(gadget.getSpec().getUrl(),
          rewriterFeature, proxyBaseNoGadget, "image/*");

      // Create the set of filters to process in order.
      filters = ImmutableList.of(
        new BasicElementFilter(allowedTags, allowedAttributes),
        new LinkSchemeCheckFilter(),
        new StyleFilter(cssSanitizer, cssImportRewriter, imageRewriter),
        new LinkFilter(cssImportRewriter),
        new ImageFilter(imageRewriter),
        new TargetFilter()
      );
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
          Bypass bypass = canBypassSanitization(element);
          if (bypass == Bypass.ALL) {
            return;
          } else if (bypass == Bypass.ONLY_SELF) {
            for (Node child : toList(node.getChildNodes())) {
              sanitize(child);
            }
          } else {
            boolean removed = false;
            for (DomFilter filter : filters) {
              DomFilter.Result tagResult = filter.filterTag(element, context);
              if (tagResult == DomFilter.Result.REMOVE) {
                element.getParentNode().removeChild(element);
                removed = true;
                break;
              } else if (tagResult == DomFilter.Result.CONTINUE) {
                for (Attr attr : toList(node.getAttributes())) {
                  DomFilter.Result attrResult = filter.filterAttribute(attr, context);
                  if (attrResult == DomFilter.Result.PASS) {
                    break; // No need to process more attributes
                  } else if (attrResult == DomFilter.Result.REMOVE) {
                    element.removeAttributeNode(attr);
                  }
                }
              }
            }
            if (!removed) {
              for (Node child : toList(node.getChildNodes())) {
                sanitize(child);
              }
            }
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

  private static Bypass canBypassSanitization(Element element) {
    Bypass bypass = (Bypass) element.getUserData(BYPASS_SANITIZATION_KEY);
    if (bypass == null) {
      bypass = Bypass.NONE;
    }
    return bypass;
  }

  /**
   * Filter DOM elements and attributes to check their validity and
   * restrict their allowed content
   */
  static interface DomFilter {
    enum Result {
      PASS,  // Check passed, do not process further checks on this filter
      CONTINUE, // Check passed, process further checks on this filter
      REMOVE // Check failed, remove item
    };

    /**
     * Filter the element and possibly transform it.
     */
    Result filterTag(Element elem, Uri context);

    /**
     * Filter the attribute and possibly transform it
\     */
    Result filterAttribute(Attr attr, Uri context);
  }

  /**
   * Restrict the set of allowed tags and attributes
   */
  static class BasicElementFilter implements DomFilter {
    final Set<String> allowedTags;
    final Set<String> allowedAttrs;

    BasicElementFilter(Set<String> allowedTags, Set<String> allowedAttrs) {
      this.allowedTags = allowedTags;
      this.allowedAttrs = allowedAttrs;
    }

    public Result filterTag(Element elem, Uri context) {
      return allowedTags.contains(elem.getNodeName().toLowerCase()) ?
          Result.CONTINUE : Result.REMOVE;
    }

    public Result filterAttribute(Attr attr, Uri context) {
      return allowedAttrs.contains(attr.getName().toLowerCase()) ?
          Result.CONTINUE : Result.REMOVE;
    }
  }

  /**
   * Enfore that all uri's in the document have either http or https as
   * their scheme
   */
  static class LinkSchemeCheckFilter implements DomFilter {
    protected Set<String> uriAttributes;

    LinkSchemeCheckFilter() {
      uriAttributes = ImmutableSet.of("href", "src");
    }

    public Result filterTag(Element elem, Uri context) {
      return Result.CONTINUE;
    }

    public Result filterAttribute(Attr attr, Uri context) {
      if (uriAttributes.contains(attr.getName().toLowerCase())) {
        try {
          Uri uri = Uri.parse(attr.getValue());
          String scheme = uri.getScheme();
          if (scheme != null && !scheme.equals("http") && !scheme.equals("https")) {
            return Result.REMOVE;
          }
        } catch (IllegalArgumentException iae) {
          return Result.REMOVE;
        }
      }
      return Result.CONTINUE;
    }
  }

  /**
   * Enfore that all images in the document are rewritten through the proxy.
   * Prevents issues in IE where the image content contains script
   */
  static class ImageFilter implements DomFilter {
    protected final LinkRewriter imageRewriter;

    ImageFilter(LinkRewriter imageRewriter) {
      this.imageRewriter = imageRewriter;
    }

    public Result filterTag(Element elem, Uri context) {
      if ("img".equalsIgnoreCase(elem.getNodeName())) {
        return Result.CONTINUE;
      }
      return Result.PASS;
    }

    public Result filterAttribute(Attr attr, Uri context) {
      if ("src".equalsIgnoreCase(attr.getName())) {
        attr.setValue(imageRewriter.rewrite(attr.getValue(), context));
      }
      return Result.PASS;
    }
  }

  /**
   * Pass the contents of style tags through the CSS sanitizer
   */
  static class StyleFilter implements DomFilter {
    final CajaCssSanitizer sanitizer;
    final LinkRewriter importRewriter;
    final LinkRewriter imageRewriter;

    StyleFilter(CajaCssSanitizer sanitizer, LinkRewriter importRewriter,
        LinkRewriter imageRewriter) {
      this.sanitizer = sanitizer;
      this.importRewriter = importRewriter;
      this.imageRewriter = imageRewriter;
    }

    public Result filterTag(Element elem, Uri context) {
      if ("style".equalsIgnoreCase(elem.getNodeName())) {
        sanitizer.sanitize(elem, context, importRewriter, imageRewriter);
      }
      return Result.PASS;
    }

    public Result filterAttribute(Attr attr, Uri context) {
      return Result.PASS;
    }
  }

  /**
   * Restrict link tags to stylesheet content only and force the link to
   * be rewritten through the proxy and sanitized
   */
  static class LinkFilter implements DomFilter {
    final LinkRewriter rewriter;

    LinkFilter(LinkRewriter rewriter) {
      this.rewriter = rewriter;
    }

    public Result filterTag(Element elem, Uri context) {
      if (!elem.getNodeName().equalsIgnoreCase("link")) {
        return Result.CONTINUE;
      }
      boolean hasType = false;
      for (Attr attr : toList(elem.getAttributes())) {
        if ("rel".equalsIgnoreCase(attr.getName())) {
          hasType |= "stylesheet".equalsIgnoreCase(attr.getValue());
        } else if ("type".equalsIgnoreCase(attr.getName())) {
          hasType |= "text/css".equalsIgnoreCase(attr.getValue());
        } else if ("href".equalsIgnoreCase(attr.getName())) {
          attr.setValue(rewriter.rewrite(attr.getValue(), context));
        }
      }
      return hasType ? Result.PASS : Result.REMOVE;
    }

    public Result filterAttribute(Attr attr, Uri context) {
      return Result.PASS;
    }
  }

  /**
   * Restrict the value of the target attribute on anchors etc. to
   * _blank or _self or remove the node
   */
  static class TargetFilter implements DomFilter {

    TargetFilter() {
    }

    public Result filterTag(Element elem, Uri context) {
      return Result.CONTINUE;
    }

    public Result filterAttribute(Attr attr, Uri context) {
      if ("target".equalsIgnoreCase(attr.getName())) {
        String value = attr.getValue().toLowerCase();
        if (!("_blank".equals(value) || "_self".equals(value))) {
          return Result.REMOVE;
        }
      }
      return Result.CONTINUE;
    }
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
      rewritten += '&' + ProxyBase.SANITIZE_CONTENT_PARAM + "=1";
      rewritten += '&' + ProxyBase.REWRITE_MIME_TYPE_PARAM + '=' + expectedMime;
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
