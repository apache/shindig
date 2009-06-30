/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.gadgets.parse.caja;

import org.apache.commons.lang.StringUtils;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.rewrite.LinkRewriter;

import com.google.caja.lang.css.CssSchema;
import com.google.caja.parser.AbstractParseTreeNode;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.Visitor;
import com.google.caja.parser.css.CssTree;
import com.google.caja.reporting.SimpleMessageQueue;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;

import org.w3c.dom.Element;

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

  private static final Logger logger = Logger.getLogger(CajaCssSanitizer.class.getName());

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
   * @param importRewriter to rewrite @imports to sanitizing proxy
   * @param importRewriter to rewrite images to sanitizing proxy
   */
  public String sanitize(String content, Uri linkContext, LinkRewriter importRewriter,
      LinkRewriter imageRewriter) {
    try {
      CssTree.StyleSheet stylesheet = parser.parseDom(content);
      sanitize(stylesheet, linkContext, importRewriter, imageRewriter);
      // Write the rewritten CSS back into the element
      return parser.serialize(stylesheet);
    } catch (GadgetException ge) {
      // Failed to parse stylesheet so log and continue
      logger.log(Level.INFO, "Failed to parse stylesheet", ge);
      return "";
    }
  }

  /**
   * Sanitize the CSS content of a style tag.
   * @param styleElem to sanitize
   * @param linkContext url of containing content
   * @param importRewriter to rewrite @imports to sanitizing proxy
   * @param importRewriter to rewrite images to sanitizing proxy
   */
  public void sanitize(Element styleElem, Uri linkContext, LinkRewriter importRewriter,
      LinkRewriter imageRewriter) {
    String content = null;
    try {
      CssTree.StyleSheet stylesheet = parser.parseDom(styleElem.getTextContent());
      sanitize(stylesheet, linkContext, importRewriter, imageRewriter);
      // Write the rewritten CSS back into the element
      content = parser.serialize(stylesheet);
    } catch (GadgetException ge) {
      // Failed to parse stylesheet so log and continue
      logger.log(Level.INFO, "Failed to parse stylesheet", ge);
    }
    if (StringUtils.isEmpty(content)) {
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
   * @param importRewriter to rewrite links to sanitizing proxy
   * @param imageRewriter to rewrite links to the sanitizing proxy
   */
  public void sanitize(CssTree css, final Uri linkContext, final LinkRewriter importRewriter,
      final LinkRewriter imageRewriter) {
    css.acceptPreOrder(new Visitor() {
      public boolean visit(AncestorChain<?> ancestorChain) {
        if (ancestorChain.node instanceof CssTree.Property) {
          if (!schema.isPropertyAllowed(((CssTree.Property) ancestorChain.node).
              getPropertyName())) {
            // Remove offending property
            if (logger.isLoggable(Level.FINE)) {
              logger.log(Level.FINE, "Removing property "
                  + ((CssTree.Property) ancestorChain.node).getPropertyName());
            }
            clean(ancestorChain);
          }
        } else if (ancestorChain.node instanceof CssTree.FunctionCall) {
          if (!schema.isFunctionAllowed(((CssTree.FunctionCall)ancestorChain.node).getName())) {
            // Remove offending node
            if (logger.isLoggable(Level.FINE)) {
              logger.log(Level.FINE, "Removing function "
                  + ((CssTree.FunctionCall) ancestorChain.node).getName());
            }
            clean(ancestorChain);
          }
        } else if (ancestorChain.node instanceof CssTree.UriLiteral &&
            !(ancestorChain.getParentNode() instanceof CssTree.Import)) {
          boolean validUri = false;
          String uri = ((CssTree.UriLiteral)ancestorChain.node).getValue();
          try {
            String scheme = Uri.parse(uri).getScheme();
            validUri = StringUtils.isEmpty(scheme) ||
                ALLOWED_URI_SCHEMES.contains(scheme.toLowerCase());
          } catch (RuntimeException re) {
            if (logger.isLoggable(Level.FINE)) {
              logger.log(Level.FINE, "Failed to parse URI in CSS " + uri, re);
            }
          }
          if (!validUri) {
            // Remove offending node
            if (logger.isLoggable(Level.FINE)) {
              logger.log(Level.FINE, "Removing invalid URI "
                  + ((CssTree.UriLiteral) ancestorChain.node).getValue());
            }
            clean(ancestorChain);
          } else {
            // Assume the URI is for an image. Rewrite it using the image link rewriter
            ((CssTree.UriLiteral)ancestorChain.node).setValue(
                imageRewriter.rewrite(uri, linkContext));
          }
        } else if (ancestorChain.node instanceof CssTree.Import) {
          CssTree.Import importDecl = (CssTree.Import) ancestorChain.node;
          importDecl.getUri()
              .setValue(importRewriter.rewrite(importDecl.getUri().getValue(), linkContext));
        }
        return true;
      }
    }, null);
  }

  /**
   * recurse up through chain to find a safe clean point
   * @param chain chain of nodes
   */
  private static void clean(AncestorChain<?> chain) {
    if (chain.node instanceof CssTree.Declaration ||
        chain.node instanceof CssTree.Import) {
      // Remove the entire subtree
      ((AbstractParseTreeNode)chain.getParentNode()).removeChild(chain.node);
    } else {
      clean(chain.parent);
    }
  }
}
