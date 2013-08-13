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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.BindingAnnotation;
import com.google.inject.Inject;
import com.google.inject.Provider;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.parse.caja.CajaCssSanitizer;
import org.apache.shindig.gadgets.rewrite.ContentRewriterFeature;
import org.apache.shindig.gadgets.rewrite.DomWalker;
import org.apache.shindig.gadgets.rewrite.MutableContent;
import org.apache.shindig.gadgets.rewrite.RewritingException;
import org.apache.shindig.gadgets.uri.ProxyUriManager;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.UserDataHandler;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
public class SanitizingGadgetRewriter extends DomWalker.Rewriter {

  /** Key stored as element user-data to bypass sanitization */
  private static final String BYPASS_SANITIZATION_KEY = "shindig.bypassSanitization";

  /**
   * Is the Gadget to be rendered sanitized?
   * @return true if sanitization will be enabled
   */
  public static boolean isSanitizedRenderingRequest(Gadget gadget) {
    return "1".equals(gadget.getContext().getParameter("sanitize"));
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

  // Public so it can be used by the old rewriter
  public static enum Bypass { ALL, ONLY_SELF, NONE }

    private static UserDataHandler copyOnClone = new UserDataHandler() {
    public void handle(short operation, String key, Object data, Node src, Node dst) {
      if (operation == NODE_IMPORTED || operation == NODE_CLONED) {
        dst.setUserData(key, data, copyOnClone);
      }
    }
  };

  @Inject
  public SanitizingGadgetRewriter(@AllowedTags Provider<Set<String>> allowedTags,
      @AllowedAttributes Provider<Set<String>> allowedAttributes,
      ContentRewriterFeature.Factory rewriterFeatureFactory,
      CajaCssSanitizer cssSanitizer,
      ProxyUriManager proxyUriManager) {
    super(new BasicElementFilter(allowedTags, allowedAttributes),
          new LinkSchemeCheckFilter(),
          new StyleFilter(proxyUriManager, cssSanitizer),
          new LinkFilter(proxyUriManager),
          new ImageFilter(proxyUriManager),
          new TargetFilter());
  }


  @Override
  public void rewrite(Gadget gadget, MutableContent content) throws RewritingException {
    if (gadget.sanitizeOutput()) {
      boolean sanitized = false;
      try {
        super.rewrite(gadget, content);
        sanitized = true;
      } finally {
        // Defensively clean the content in case of failure
        if (!sanitized) {
          content.setContent("");
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

  // Public so it can be used by the old rewriter
  public static Bypass canBypassSanitization(Element element) {
    Bypass bypass = (Bypass) element.getUserData(BYPASS_SANITIZATION_KEY);
    if (bypass == null) {
      bypass = Bypass.NONE;
    }
    return bypass;
  }

  private static abstract class SanitizingWalker implements DomWalker.Visitor {
    protected abstract boolean removeTag(Gadget gadget, Element elem, Uri ctx);
    protected abstract boolean removeAttr(Gadget gadget, Attr attr, Uri ctx);

    public VisitStatus visit(Gadget gadget, Node node) throws RewritingException {
      Element elem;

      switch (node.getNodeType()) {
      case Node.CDATA_SECTION_NODE:
      case Node.TEXT_NODE:
      case Node.ENTITY_REFERENCE_NODE:
        // Never modified.
        return VisitStatus.BYPASS;
      case Node.ELEMENT_NODE:
      case Node.DOCUMENT_NODE:
        // Continues through to follow-up logic.
        elem = (Element)node;
        break;
      case Node.COMMENT_NODE:
      default:
        // Must remove all comments to avoid conditional comment evaluation.
        // There might be other, unknown types as well. Don't trust them.
        return VisitStatus.RESERVE_TREE;
      }

      Bypass bypass = canBypassSanitization(elem);
      if (bypass == Bypass.ALL) {
        // This is double-checked in revisit below to ensure no modification/removal occurs.
        return VisitStatus.RESERVE_TREE;
      } else if (bypass == Bypass.ONLY_SELF) {
        return VisitStatus.BYPASS;
      }

      if (removeTag(gadget, elem, gadget.getSpec().getUrl())) {
        // All reserved trees are removed in revisit.
        return VisitStatus.RESERVE_TREE;
      }

      // Otherwise move on to attributes.
      VisitStatus status = VisitStatus.MODIFY;
      for (Attr attr : toList(elem.getAttributes())) {
        if (removeAttr(gadget, attr, gadget.getSpec().getUrl())) {
          elem.removeAttributeNode(attr);
        }
      }

      return status;
    }

    public boolean revisit(Gadget gadget, List<Node> nodes) throws RewritingException {
      // Remove all reserved nodes, since these are all for which removeTag returned true.
      for (Node node : nodes) {
        if (node.getNodeType() == Node.COMMENT_NODE ||
            canBypassSanitization((Element)node) != Bypass.ALL) {
          node.getParentNode().removeChild(node);
        }
      }
      return true;
    }
  }

  /**
   * Restrict the set of allowed tags and attributes
   */
  static final class BasicElementFilter extends SanitizingWalker {
    private final Provider<Set<String>> allowedTags;
    private final Provider<Set<String>> allowedAttributes;

    private BasicElementFilter(Provider<Set<String>> allowedTags,
                               Provider<Set<String>> allowedAttributes) {
      this.allowedTags = allowedTags;
      this.allowedAttributes = allowedAttributes;
    }

    @Override
    public boolean removeTag(Gadget gadget, Element elem, Uri context) {
      return !allowedTags.get().contains(elem.getNodeName().toLowerCase());
    }

    @Override
    public boolean removeAttr(Gadget gadget, Attr attr, Uri context) {
      return !allowedAttributes.get().contains(attr.getName().toLowerCase());
    }
  }

  /**
   * Enfore that all uri's in the document have either http or https as
   * their scheme
   */
  static class LinkSchemeCheckFilter extends SanitizingWalker {
    private static final Set<String> URI_ATTRIBUTES = ImmutableSet.of("href", "src");

    @Override
    protected boolean removeTag(Gadget gadget, Element elem, Uri ctx) {
      return false;
    }

    @Override
    protected boolean removeAttr(Gadget gadget, Attr attr, Uri ctx) {
      if (URI_ATTRIBUTES.contains(attr.getName().toLowerCase())) {
        try {
          Uri uri = Uri.parse(attr.getValue());
          String scheme = uri.getScheme();
          if (scheme != null && !scheme.equals("http") && !scheme.equals("https")) {
            return true;
          }
        } catch (IllegalArgumentException iae) {
          return true;
        }
      }
      return false;
    }
  }

  /**
   * Enfore that all images in the document are rewritten through the proxy.
   * Prevents issues in IE where the image content contains script
   */
  static final class ImageFilter extends SanitizingWalker {
    private final SanitizingProxyUriManager imageRewriter;

    private ImageFilter(ProxyUriManager proxyUriManager) {
      this.imageRewriter = new SanitizingProxyUriManager(proxyUriManager, "image/*");
    }

    @Override
    protected boolean removeTag(Gadget gadget, Element elem, Uri ctx) {
      return false;
    }

    @Override
    protected boolean removeAttr(Gadget gadget, Attr attr, Uri ctx) {
      if ("img".equalsIgnoreCase(attr.getOwnerElement().getNodeName()) &&
          "src".equalsIgnoreCase(attr.getName())) {
        try {
          Uri uri = Uri.parse(attr.getValue());
          ProxyUriManager.ProxyUri proxiedUri = ProxyUriManager.ProxyUri.fromList(
              gadget, ImmutableList.of(uri)).get(0);
          proxiedUri.setHtmlTagContext(attr.getOwnerElement().getNodeName().toLowerCase());
          attr.setValue(imageRewriter.make(ImmutableList.of(proxiedUri), null)
                .get(0).toString());
        } catch (IllegalArgumentException e) {
          // Invalid Uri, remove.
          return true;
        }
      }
      return false;
    }
  }

  /**
   * Pass the contents of style tags through the CSS sanitizer
   */
  static final class StyleFilter implements DomWalker.Visitor {
    private final SanitizingProxyUriManager imageRewriter;
    private final SanitizingProxyUriManager cssImportRewriter;
    private final CajaCssSanitizer cssSanitizer;

    private StyleFilter(ProxyUriManager proxyUriManager, CajaCssSanitizer cssSanitizer) {
      this.imageRewriter = new SanitizingProxyUriManager(proxyUriManager, "image/*");
      this.cssImportRewriter = new SanitizingProxyUriManager(proxyUriManager, "text/css");
      this.cssSanitizer = cssSanitizer;
    }

    public VisitStatus visit(Gadget gadget, Node node) throws RewritingException {
      if (node.getNodeType() == Node.ELEMENT_NODE &&
          "style".equalsIgnoreCase(node.getNodeName())) {
        cssSanitizer.sanitize((Element) node, gadget.getSpec().getUrl(),
            gadget.getContext(), cssImportRewriter, imageRewriter);
        return VisitStatus.MODIFY;
      }
      return VisitStatus.BYPASS;
    }

    public boolean revisit(Gadget gadget, List<Node> nodes) throws RewritingException {
      return false;
    }
  }

  /**
   * Restrict link tags to stylesheet content only and force the link to
   * be rewritten through the proxy and sanitized
   */
  static final class LinkFilter extends SanitizingWalker {
    private final SanitizingProxyUriManager cssImportRewriter;

    private LinkFilter(ProxyUriManager proxyUriManager) {
      this.cssImportRewriter = new SanitizingProxyUriManager(proxyUriManager, "text/css");
    }

    @Override
    protected boolean removeTag(Gadget gadget, Element elem, Uri ctx) {
      if (!elem.getNodeName().equalsIgnoreCase("link")) {
        return false;
      }
      boolean hasType = false;
      for (Attr attr : toList(elem.getAttributes())) {
        if ("rel".equalsIgnoreCase(attr.getName())) {
          hasType |= "stylesheet".equalsIgnoreCase(attr.getValue());
        } else if ("type".equalsIgnoreCase(attr.getName())) {
          hasType |= "text/css".equalsIgnoreCase(attr.getValue());
        } else if ("href".equalsIgnoreCase(attr.getName())) {
          try {
            ProxyUriManager.ProxyUri proxiedUri = ProxyUriManager.ProxyUri.fromList(gadget,
                  ImmutableList.of(Uri.parse(attr.getValue()))).get(0);
            proxiedUri.setHtmlTagContext(elem.getNodeName().toLowerCase());
            attr.setValue(cssImportRewriter.make(ImmutableList.of(proxiedUri), null)
                .get(0).toString());
          } catch (IllegalArgumentException e) {
            return true;
          }
        }
      }
      return !hasType;
    }

    @Override
    protected boolean removeAttr(Gadget gadget, Attr attr, Uri ctx) {
      return false;
    }
  }

  /**
   * Restrict the value of the target attribute on anchors etc. to
   * _blank or _self or remove the node
   */
  static class TargetFilter extends SanitizingWalker {
    @Override
    protected boolean removeTag(Gadget gadget, Element elem, Uri ctx) {
      return false;
    }

    @Override
    protected boolean removeAttr(Gadget gadget, Attr attr, Uri ctx) {
      if ("target".equalsIgnoreCase(attr.getName())) {
        String value = attr.getValue().toLowerCase();
        if (!("_blank".equals(value) || "_self".equals(value))) {
          return true;
        }
      }
      return false;
    }
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
  @BindingAnnotation
  public @interface AllowedTags { }

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
  @BindingAnnotation
  public @interface AllowedAttributes { }
}
