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

import com.google.common.base.Strings;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shindig.common.logging.i18n.MessageKeys;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.apache.shindig.gadgets.parse.caja.CajaCssParser;
import org.apache.shindig.gadgets.uri.ProxyUriManager;
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

import com.google.caja.lexer.ParseException;
import com.google.caja.parser.AbstractParseTreeNode;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.Visitor;
import com.google.caja.parser.css.CssTree;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

/**
 * Rewrite links to referenced content in a stylesheet
 *
 * @since 2.0.0
 */
public class CssResponseRewriter implements ResponseRewriter {

  //class name for logging purpose
  private static final String classname = CssResponseRewriter.class.getName();
  private static final Logger LOG = Logger.getLogger(classname,MessageKeys.MESSAGES);

  private final CajaCssParser cssParser;
  protected final ProxyUriManager proxyUriManager;
  protected final ContentRewriterFeature.Factory rewriterFeatureFactory;

  @Inject
  public CssResponseRewriter(CajaCssParser cssParser,
      ProxyUriManager proxyUriManager, ContentRewriterFeature.Factory rewriterFeatureFactory) {
    this.cssParser = cssParser;
    this.proxyUriManager = proxyUriManager;
    this.rewriterFeatureFactory = rewriterFeatureFactory;
  }

  public void rewrite(HttpRequest request, HttpResponseBuilder original, Gadget gadget)
          throws RewritingException {
    ContentRewriterFeature.Config config = rewriterFeatureFactory.get(request);
    if (!RewriterUtils.isCss(request, original)) {
      return;
    }

    String css = original.getContent();
    StringWriter sw = new StringWriter((css.length() * 110) / 100);
    rewrite(new StringReader(css), request.getUri(),
        new UriMaker(proxyUriManager, config), sw, false,
            DomWalker.makeGadget(request).getContext());
    original.setContent(sw.toString());
  }

  /**
   * Rewrite the given CSS content and optionally extract the import references.
   * @param content CSS content
   * @param source Uri of content
   * @param uriMaker a Uri Maker
   * @param writer Output
   * @param extractImports If true remove the import statements from the output and return their
   *            referenced URIs.
   * @param gadgetContext The gadgetContext
   *
   * @return Empty list of extracted import URIs.
   */
  public List<String> rewrite(Reader content, Uri source, UriMaker uriMaker, Writer writer,
      boolean extractImports, GadgetContext gadgetContext) throws RewritingException {
    try {
      String original = IOUtils.toString(content);
      try {
        CssTree.StyleSheet stylesheet = cssParser.parseDom(original, source);
        List<String> stringList = rewrite(stylesheet, source, uriMaker, extractImports,
            gadgetContext);
        // Serialize the stylesheet
        cssParser.serialize(stylesheet, writer);
        return stringList;
      } catch (GadgetException ge) {
        if (ge.getCause() instanceof ParseException) {
          if (LOG.isLoggable(Level.WARNING)) {
            LOG.logp(Level.WARNING, classname, "rewrite", MessageKeys.CAJA_CSS_PARSE_FAILURE, new Object[] {ge.getCause().getMessage(),source});
          }
          writer.write(original);
          return Collections.emptyList();
        } else {
          throw new RewritingException(ge, ge.getHttpStatusCode());
        }
      }
    } catch (IOException ioe) {
      throw new RewritingException(ioe, HttpResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Rewrite the CSS content in a style DOM node.
   * @param styleNode Rewrite the CSS content of this node
   * @param source Uri of content
   * @param uriMaker a UriMaker
   * @param extractImports If true remove the import statements from the output and return their
   *            referenced URIs.
   * @param gadgetContext The gadgetContext
   * @return Empty list of extracted import URIs.
   */
  public List<String> rewrite(Element styleNode, Uri source, UriMaker uriMaker,
      boolean extractImports, GadgetContext gadgetContext) throws RewritingException {
    try {
      CssTree.StyleSheet stylesheet =
        cssParser.parseDom(styleNode.getTextContent(), source);
      List<String> imports = rewrite(stylesheet, source, uriMaker, extractImports, gadgetContext);
      // Write the rewritten CSS back into the element
      String content = cssParser.serialize(stylesheet);
      if (Strings.isNullOrEmpty(content) || StringUtils.isWhitespace(content)) {
        // Remove the owning node
        styleNode.getParentNode().removeChild(styleNode);
      } else {
        styleNode.setTextContent(content);
      }
      return imports;
    } catch (GadgetException ge) {
      if (ge.getCause() instanceof ParseException) {
    	if (LOG.isLoggable(Level.INFO)) {
          LOG.logp(Level.WARNING, classname, "rewrite", MessageKeys.CAJA_CSS_PARSE_FAILURE, new Object[] {ge.getCause().getMessage(),source});
        }
        return Collections.emptyList();
      } else {
        throw new RewritingException(ge, ge.getHttpStatusCode());
      }
    }
  }

  /**
   * Rewrite the CSS DOM in place.
   * @param styleSheet To rewrite
   * @param source  Uri of content
   * @param uriMaker a UriMaker
   * @param extractImports If true remove the import statements from the output and return their
   *            referenced URIs.
   * @return Empty list of extracted import URIs.
   */
  public static List<String> rewrite(CssTree.StyleSheet styleSheet, final Uri source,
      final UriMaker uriMaker, final boolean extractImports, final GadgetContext gadgetContext) {
    final List<String> imports = Lists.newLinkedList();
    final List<CssTree.UriLiteral> skip = Lists.newLinkedList();

    styleSheet.acceptPreOrder(new Visitor() {
      public boolean visit(AncestorChain<?> chain) {
        if (chain.node instanceof CssTree.Import) {
          CssTree.Import importNode = (CssTree.Import) chain.node;
          CssTree.UriLiteral uriLiteral = importNode.getUri();
          skip.add(importNode.getUri());
          if (extractImports) {
            imports.add(uriLiteral.getValue());
            ((AbstractParseTreeNode) chain.getParentNode()).removeChild(chain.node);
          } else {
            String rewritten = rewriteUri(uriMaker, uriLiteral.getValue(), source, gadgetContext);
            uriLiteral.setValue(rewritten);
          }
        } else if (chain.node instanceof CssTree.UriLiteral &&
            !skip.contains(chain.node)) {
          CssTree.UriLiteral uriDecl = (CssTree.UriLiteral) chain.node;
          String rewritten = rewriteUri(uriMaker, uriDecl.getValue(), source, gadgetContext);
          uriDecl.setValue(rewritten);
        }
        return true;
      }}, null);

    return imports;
  }

  private static String rewriteUri(UriMaker uriMaker, String input, Uri context,
      GadgetContext gadgetContext) {
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
    ProxyUriManager.ProxyUri proxyUri =
        new ProxyUriManager.ProxyUri(DomWalker.makeGadget(gadgetContext), inboundUri);
    return uriMaker.make(proxyUri, context).toString();
  }

  public static UriMaker uriMaker(ProxyUriManager wrapped, ContentRewriterFeature.Config config) {
    return new UriMaker(wrapped, config);
  }

  public static class UriMaker {
    protected final ProxyUriManager wrapped;
    protected final ContentRewriterFeature.Config config;

    public UriMaker(ProxyUriManager wrapped, ContentRewriterFeature.Config config) {
      this.wrapped = wrapped;
      this.config = config;
    }

    public Uri make(ProxyUriManager.ProxyUri uri, Uri context) {
      if (config.shouldRewriteURL(uri.getResource().toString())) {
        List<ProxyUriManager.ProxyUri> puris = Lists.newArrayList(uri);
        List<Uri> returned = wrapped.make(puris, null);
        return returned.get(0);
      }
      return context.resolve(uri.getResource());
    }
  }
}
