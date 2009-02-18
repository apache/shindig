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
package org.apache.shindig.gadgets.rewrite;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.parse.caja.CajaCssParser;

import com.google.caja.lexer.ParseException;
import com.google.caja.parser.AbstractParseTreeNode;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.Visitor;
import com.google.caja.parser.css.CssTree;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.w3c.dom.Element;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Rewrite links to referenced content in a stylesheet
 */
public class CSSContentRewriter implements ContentRewriter {

  private static final Logger logger = Logger.getLogger(CSSContentRewriter.class.getName());

  private final ContentRewriterFeatureFactory rewriterFeatureFactory;
  private final String proxyBaseNoGadget;
  private final CajaCssParser cssParser;

  @Inject
  public CSSContentRewriter(ContentRewriterFeatureFactory rewriterFeatureFactory,
      @Named("shindig.content-rewrite.proxy-url")String proxyBaseNoGadget,
      CajaCssParser cssParser) {
    this.rewriterFeatureFactory = rewriterFeatureFactory;
    this.proxyBaseNoGadget = proxyBaseNoGadget;
    this.cssParser = cssParser;
  }

  public RewriterResults rewrite(Gadget gadget, MutableContent content) {
    // Not supported
    return null;
  }

  public RewriterResults rewrite(HttpRequest request, HttpResponse original,
      MutableContent content) {
    if (!RewriterUtils.isCss(request, original)) {
      return null;
    }
    ContentRewriterFeature feature = rewriterFeatureFactory.get(request);
    String css = content.getContent();
    StringWriter sw = new StringWriter((css.length() * 110) / 100);
    rewrite(new StringReader(css), request.getUri(),
        createLinkRewriter(request.getGadget(), feature), sw, false);
    content.setContent(sw.toString());

    return RewriterResults.cacheableIndefinitely();
  }

  /**
   * Rewrite the given CSS content and optionally extract the import references.
   * @param content CSS content
   * @param source Uri of content
   * @param rewriter Rewrite urls
   * @param writer Output
   * @param extractImports If true remove the import statements from the output and return their
   *            referenced URIs.
   * @return Empty list of extracted import URIs.
   */
  public List<String> rewrite(Reader content, Uri source,
      LinkRewriter rewriter, Writer writer, boolean extractImports) {
    try {
      String original = IOUtils.toString(content);
      try {
        CssTree.StyleSheet stylesheet = cssParser.parseDom(original);
        List<String> stringList = rewrite(stylesheet, source, rewriter, extractImports);
        // Serialize the stylesheet
        cssParser.serialize(stylesheet, writer);
        return stringList;
      } catch (GadgetException ge) {
        if (ge.getCause() instanceof ParseException) {
          logger.log(Level.WARNING,
              "Caja CSS parse failure: " + ge.getCause().getMessage() + " for " + source);
          writer.write(original);
          return Collections.emptyList();
        } else {
          throw new RuntimeException(ge);
        }
      }
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  /**
   * Rewrite the CSS content in a style DOM node.
   * @param styleNode Rewrite the CSS content of this node
   * @param source Uri of content
   * @param rewriter Rewrite urls
   * @param extractImports If true remove the import statements from the output and return their
   *            referenced URIs.
   * @return Empty list of extracted import URIs.
   */
  public List<String> rewrite(Element styleNode, Uri source,
      LinkRewriter rewriter, boolean extractImports) {
    try {
      CssTree.StyleSheet stylesheet = cssParser.parseDom(styleNode.getTextContent());
      List<String> imports = rewrite(stylesheet, source, rewriter, extractImports);
      // Write the rewritten CSS back into the element
      String content = cssParser.serialize(stylesheet);
      if (StringUtils.isEmpty(content)) {
        // Remove the owning node
        styleNode.getParentNode().removeChild(styleNode);
      } else {
        styleNode.setTextContent(content);
      }
      return imports;
    } catch (GadgetException ge) {
      if (ge.getCause() instanceof ParseException) {
        logger.log(Level.WARNING,
              "Caja CSS parse failure: " + ge.getCause().getMessage() + " for " + source);
        return Collections.emptyList();
      } else {
        throw new RuntimeException(ge);
      }
    }
  }

  /**
   * Rewrite the CSS DOM in place.
   * @param styleSheet To rewrite
   * @param source  Uri of content
   * @param rewriter  Rewrite urls
   * @param extractImports If true remove the import statements from the output and return their
   *            referenced URIs.
   * @return Empty list of extracted import URIs.
   */
  public static List<String> rewrite(CssTree.StyleSheet styleSheet, final Uri source,
      final LinkRewriter rewriter, final boolean extractImports) {
    final List<String> imports = Lists.newArrayList();

    styleSheet.acceptPostOrder(new Visitor() {
      public boolean visit(AncestorChain<?> chain) {
        if (extractImports && chain.node instanceof CssTree.Import) {
          // Extract the import statment, add its URI to the returned list
          // and remove it from the DOM.
          CssTree.Import importNode = (CssTree.Import) chain.node;
          imports.add(importNode.getUri().getValue());
          ((AbstractParseTreeNode) chain.getParentNode()).removeChild(chain.node);
        } else if (chain.node instanceof CssTree.UriLiteral) {
          // If were not extracting imports then rewrite its link
          if (!(chain.getParentNode() instanceof CssTree.Import && extractImports)) {
            String rewritten = rewriter
                .rewrite(((CssTree.UriLiteral) chain.node).getValue(), source);
            ((CssTree.UriLiteral) chain.node).setValue(rewritten);
          }
        }
        return true;
      }}, null);

    return imports;
  }

  protected LinkRewriter createLinkRewriter(Uri gadgetUri, ContentRewriterFeature feature) {
    return new ProxyingLinkRewriter(gadgetUri, feature, proxyBaseNoGadget);
  }
}

