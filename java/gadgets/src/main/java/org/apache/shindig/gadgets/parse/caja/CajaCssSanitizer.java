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
package org.apache.shindig.gadgets.parse.caja;

import com.google.common.base.Strings;
import org.apache.shindig.common.logging.i18n.MessageKeys;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.rewrite.DomWalker;
import org.apache.shindig.gadgets.uri.ProxyUriManager;

import com.google.caja.lang.css.CssSchema;
import com.google.caja.parser.AbstractParseTreeNode;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.Visitor;
import com.google.caja.parser.css.CssTree;
import com.google.caja.reporting.SimpleMessageQueue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;

import org.w3c.dom.Element;

import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Sanitize a CSS tree using Caja. Strip properties and functions that represent
 * ways to execute script. Specifically
 *
 * - Use Caja's CSS property whitelist
 * - Use Caja's CSS function whitelist
 * - Force @import through the proxy and require sanitization. If they cant be parsed, remove them
 * - Force @url references to have the HTTP/HTTPS protocol
 */
public class CajaCssSanitizer {
  //class name for logging purpose
  private static final String classname = CajaCssSanitizer.class.getName();
  private static final Logger LOG = Logger.getLogger(classname,MessageKeys.MESSAGES);

  private static final Set<String> ALLOWED_URI_SCHEMES = ImmutableSet.of("http", "https");

  private final CajaCssParser parser;

  private final CssSchema schema;

  @Inject
  public CajaCssSanitizer(CajaCssParser parser) {
    this.parser = parser;
    schema = CssSchema.getDefaultCss21Schema(new SimpleMessageQueue());
  }

  /**
   * Sanitize the CSS content of a style tag.
   * @param content to sanitize
   * @param linkContext url of containing content
   * @param gadgetContext The gadget context.
   * @param importRewriter to rewrite @imports to sanitizing proxy
   * @param imageRewriter to rewrite images to sanitizing proxy
   * @return Sanitized css.
   */
  public String sanitize(String content, Uri linkContext, GadgetContext gadgetContext,
                         ProxyUriManager importRewriter, ProxyUriManager imageRewriter) {
    try {
      CssTree.StyleSheet stylesheet = parser.parseDom(content, linkContext);
      sanitize(stylesheet, linkContext, gadgetContext, importRewriter, imageRewriter);
      // Write the rewritten CSS back into the element
      return parser.serialize(stylesheet);
    } catch (GadgetException ge) {
      // Failed to parse stylesheet so log and continue
      if (LOG.isLoggable(Level.INFO)) {
        LOG.logp(Level.INFO, classname, "sanitize", MessageKeys.FAILED_TO_PARSE);
        LOG.log(Level.INFO, ge.getMessage(), ge);
      }
      return "";
    }
  }

  /**
   * Sanitize the CSS content of a style tag.
   * @param styleElem to sanitize
   * @param linkContext url of containing content
   * @param gadgetContext The gadget context.
   * @param importRewriter to rewrite @imports to sanitizing proxy
   * @param imageRewriter to rewrite images to sanitizing proxy
   */
  public void sanitize(Element styleElem, Uri linkContext, GadgetContext gadgetContext,
                       ProxyUriManager importRewriter, ProxyUriManager imageRewriter) {
    String content = null;
    try {
      CssTree.StyleSheet stylesheet =
        parser.parseDom(styleElem.getTextContent(), linkContext);
      sanitize(stylesheet, linkContext, gadgetContext, importRewriter, imageRewriter);
      // Write the rewritten CSS back into the element
      content = parser.serialize(stylesheet);
    } catch (GadgetException ge) {
      // Failed to parse stylesheet so log and continue
        if (LOG.isLoggable(Level.INFO)) {
          LOG.logp(Level.INFO, classname, "sanitize", MessageKeys.FAILED_TO_PARSE);
          LOG.log(Level.INFO, ge.getMessage(), ge);
        }
    }
    if (Strings.isNullOrEmpty(content)) {
      // Remove the owning node
      styleElem.getParentNode().removeChild(styleElem);
    } else {
      styleElem.setTextContent(content);
    }
  }

  /**
   * Sanitize the given CSS tree in-place by removing all non-whitelisted function calls
   * @param css DOM root
   * @param linkContext url of containing content
   * @param gadgetContext The gadget context.
   * @param importRewriter to rewrite links to sanitizing proxy
   * @param imageRewriter to rewrite links to the sanitizing proxy
   */
  public void sanitize(CssTree css, final Uri linkContext, final GadgetContext gadgetContext,
                       final ProxyUriManager importRewriter, final ProxyUriManager imageRewriter) {
    css.acceptPreOrder(new Visitor() {
      public boolean visit(AncestorChain<?> ancestorChain) {
        if (ancestorChain.node instanceof CssTree.Property) {
          if (!schema.isPropertyAllowed(((CssTree.Property) ancestorChain.node).
              getPropertyName())) {
            // Remove offending property
            if (LOG.isLoggable(Level.FINE)) {
              LOG.log(Level.FINE, "Removing property "
                  + ((CssTree.Property) ancestorChain.node).getPropertyName());
            }
            clean(ancestorChain);
          }
        } else if (ancestorChain.node instanceof CssTree.FunctionCall) {
          if (!schema.isFunctionAllowed(((CssTree.FunctionCall)ancestorChain.node).getName())) {
            // Remove offending node
            if (LOG.isLoggable(Level.FINE)) {
              LOG.log(Level.FINE, "Removing function "
                  + ((CssTree.FunctionCall) ancestorChain.node).getName());
            }
            clean(ancestorChain);
          }
        } else if (ancestorChain.node instanceof CssTree.UriLiteral &&
            !(ancestorChain.getParentNode() instanceof CssTree.Import)) {
          String uri = ((CssTree.UriLiteral)ancestorChain.node).getValue();
          if (isValidUri(uri)) {
            // Assume the URI is for an image. Rewrite it using the image link rewriter
            ((CssTree.UriLiteral)ancestorChain.node).setValue(
                rewriteUri(imageRewriter, uri, linkContext, gadgetContext));
          } else {
            // Remove offending node
            if (LOG.isLoggable(Level.FINE)) {
              LOG.log(Level.FINE, "Removing invalid URI " + uri);
            }
            clean(ancestorChain);
          }
        } else if (ancestorChain.node instanceof CssTree.Import) {
          CssTree.Import importDecl = (CssTree.Import) ancestorChain.node;
          String uri = importDecl.getUri().getValue();
          if (isValidUri(uri)) {
            importDecl.getUri().setValue(rewriteUri(importRewriter, uri, linkContext,
                gadgetContext));
          } else {
            if (LOG.isLoggable(Level.FINE)) {
              LOG.log(Level.FINE, "Removing invalid URI " + uri);
            }
            clean(ancestorChain);
          }
        }
        return true;
      }
    }, null);
  }

  private static String rewriteUri(ProxyUriManager proxyUriManager, String input,
                                   final Uri context, GadgetContext gadgetContext) {
    Uri inboundUri;
    try {
      inboundUri = Uri.parse(input);
    } catch (IllegalArgumentException e) {
      // Don't rewrite at all.
      return input;
    }
    if (context != null) {
      inboundUri = context.resolve(inboundUri);
    }

    List<ProxyUriManager.ProxyUri> uris = ImmutableList.of(
        new ProxyUriManager.ProxyUri(DomWalker.makeGadget(new GadgetContext(gadgetContext) {
          @Override
          public Uri getUrl() {
            return context;
          }
        }), inboundUri));
    List<Uri> rewritten = proxyUriManager.make(uris, null);
    return rewritten.get(0).toString();
  }

  private boolean isValidUri(String uri) {
    try {
      String scheme = Uri.parse(uri).getScheme();
      return Strings.isNullOrEmpty(scheme) ||
          ALLOWED_URI_SCHEMES.contains(scheme.toLowerCase());
    } catch (RuntimeException re) {
      if (LOG.isLoggable(Level.FINE)) {
        LOG.log(Level.FINE, "Failed to parse URI in CSS " + uri, re);
      }
    }
    return false;
  }

  /**
   * recurse up through chain to find a safe clean point
   * @param chain chain of nodes
   */
  private static void clean(AncestorChain<?> chain) {
    if (chain == null) {
      return;
    }
    if (chain.node instanceof CssTree.Declaration ||
        chain.node instanceof CssTree.Import) {
      if (chain.getParentNode() instanceof CssTree.UserAgentHack) {
        clean(chain.parent);
      } else {
        // Remove the entire subtree
        ((AbstractParseTreeNode)chain.getParentNode()).removeChild(chain.node);
      }
    } else {
      clean(chain.parent);
    }
  }
}
