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

import com.google.caja.lexer.HtmlTokenType;
import com.google.caja.lexer.Token;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.apache.shindig.common.util.Utf8UrlCoder;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.parse.GadgetHtmlNode;
import org.apache.shindig.gadgets.parse.GadgetHtmlParser;
import org.apache.shindig.gadgets.parse.ParsedHtmlNode;
import org.apache.shindig.gadgets.servlet.ProxyBase;
import org.apache.shindig.gadgets.spec.GadgetSpec;

/**
 * Rewrites HTML, subsuming a significant amount of rewriting
 * functionality previously broken out as HtmlTagTransformers.
 * Uses parse tree rather than lexer to achieve its ends.
 */
public class ParseTreeHtmlRewriter {
  
  private GadgetHtmlParser htmlParser;

  public ParseTreeHtmlRewriter(GadgetHtmlParser htmlParser) {
    this.htmlParser = htmlParser;
  }

  public void rewrite(String content, URI source,
      GadgetSpec spec, ContentRewriterFeature rewriterFeature,
      LinkRewriter linkRewriter, Writer writer) {
    List<GadgetHtmlNode> nodes = getParsedHtmlNodes(content);
    if (nodes == null) {
      try {
        // Can't rewrite something that's malformed:
        // leave it alone.
        writer.append(content);
      } catch (IOException e) {
        e.printStackTrace();
      }
      return;
    }
    
    // Temporary implementation: in-class three-pass rewriting,
    // with each method performing the equivalent of one
    // post-refactored ContentRewriter
    transformLinkingTags(nodes, source, linkRewriter);
    
    if (getConcatUrl() != null &&
        rewriterFeature != null &&
        rewriterFeature.getIncludedTags().contains("script")) {
      transformConsolidateJSTags(nodes, source, spec, rewriterFeature);
    }
    
    if (rewriterFeature != null &&
        rewriterFeature.getIncludedTags().contains("style")) {
      transformStyleTags(nodes, source, linkRewriter);
    }
    
    try {
      for (GadgetHtmlNode node : nodes) {
        node.render(writer);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  // Formerly LinkingTagRewriter
  private static final Map<String, Set<String>> tagAttributeTargets =
      getDefaultTargets();

  void transformLinkingTags(List<GadgetHtmlNode> nodes, URI gadgetUri,
       LinkRewriter linkRewriter) {
    Queue<GadgetHtmlNode> nodesToProcess =
      new LinkedList<GadgetHtmlNode>();
    nodesToProcess.addAll(nodes);
  
    while (!nodesToProcess.isEmpty()) {
      GadgetHtmlNode curNode = nodesToProcess.remove();
      if (!curNode.isText()) {
        // Depth-first iteration over children. Order doesn't matter anyway.
        nodesToProcess.addAll(curNode.getChildren());
        
        Set<String> curTagAttrs =
            tagAttributeTargets.get(curNode.getTagName().toLowerCase());
        if (curTagAttrs != null) {
          for (String attrKey : curNode.getAttributeKeys()) {
            if (curTagAttrs.contains(attrKey.toLowerCase())) {
              String attrValue = curNode.getAttributeValue(attrKey);
            
              // Attribute marked for rewriting: do it!
              curNode.setAttribute(attrKey,
                  linkRewriter.rewrite(attrValue, gadgetUri));
            }
          }
        }
      }
    }
  }
  
  private static Map<String, Set<String>> getDefaultTargets() {
    Map<String, Set<String>> targets  = new HashMap<String, Set<String>>();
    targets.put("img", new HashSet<String>(Arrays.asList("src")));
    targets.put("embed", new HashSet<String>(Arrays.asList("src")));
    targets.put("link", new HashSet<String>(Arrays.asList("href")));
    return targets;
  }
  
  // Formerly JavascriptTagMerger
  private final static int MAX_URL_LENGTH = 1500;

  void transformConsolidateJSTags(List<GadgetHtmlNode> nodes,
      URI gadgetUri, GadgetSpec spec, ContentRewriterFeature rewriterFeature) {
    // Add top-level node and remove it afterward to simplify
    // algorithm to: pop node, process children, continue
    GadgetHtmlNode wrapper = new GadgetHtmlNode("wrapper", null);
    for (GadgetHtmlNode node : nodes) {
      wrapper.appendChild(node);
    }
    doJSTagConsolidation(getJsConcatBase(spec, rewriterFeature), wrapper, gadgetUri);
    
    // Replace nodes list with wrapper children.
    // This stuff will be cleaned up with rewriter modularization by introducing
    // a well-defined top-level node for processing purposes.
    nodes.clear();
    for (GadgetHtmlNode node : wrapper.getChildren()) {
      // Dissociate from parent.
      wrapper.removeChild(node);
      nodes.add(node);
    }
  }
  
  String getJsConcatBase(GadgetSpec spec, ContentRewriterFeature rewriterFeature) {
    return getConcatUrl() +
           ProxyBase.REWRITE_MIME_TYPE_PARAM +
           "=text/javascript&" +
           "gadget=" +
           Utf8UrlCoder.encode(spec.getUrl().toString()) +
           "&fp=" +
           rewriterFeature.getFingerprint() +
           '&';
  }
  
  // TODO: add type="text/javascript" to script tag, with tests
  // (requires guaranteed attribute ordering)
  private void doJSTagConsolidation(String concatBase,
      GadgetHtmlNode sourceNode, URI baseUri) {
    // Bootstrap queue of children over which to iterate,
    // ie. lists of siblings to potentially combine
    Queue<GadgetHtmlNode> nodesToProcess =
        new LinkedList<GadgetHtmlNode>();
    nodesToProcess.add(sourceNode);
    
    while (!nodesToProcess.isEmpty()) {
      GadgetHtmlNode parentNode = nodesToProcess.remove();
      if (!parentNode.isText()) {
        List<GadgetHtmlNode> childList = parentNode.getChildren();
        
        // Iterate over children next in depth-first fashion.
        // Text nodes (such as <script src> processed here) will be ignored.
        nodesToProcess.addAll(childList);
        
        List<GadgetHtmlNode> toRemove = new ArrayList<GadgetHtmlNode>();
        List<URI> scripts = new ArrayList<URI>();
        boolean processScripts = false;
        for (int i = 0; i < childList.size(); ++i) {
          GadgetHtmlNode cur = childList.get(i);
        
          // Find consecutive <script src=...> tags
          if (!cur.isText() &&
               cur.getTagName().equalsIgnoreCase("script") &&
               cur.hasAttribute("src")) {
            URI scriptUri = null;
            try {
              scriptUri =
                  baseUri.resolve(new URI(cur.getAttributeValue("src")));
            } catch (URISyntaxException use) {
              // Same behavior as JavascriptTagMerger
              // Perhaps switch to ignoring script src instead?
              throw new RuntimeException(use);
            }
            scripts.add(scriptUri);
            toRemove.add(cur);
          } else if (cur.isText() && cur.getText().matches("\\s*")) {
            toRemove.add(cur);
          } else if (scripts.size() > 0) {
            processScripts = true;
          }
          
          if (i == (childList.size() - 1)) {
            processScripts = true;
          }
        
          if (processScripts && scripts.size() > 0) {
            // Tags found. Concatenate scripts together.
            List<URI> concatUris = getConcatenatedUris(concatBase, scripts);
            
            // Insert concatenated nodes before first match
            for (URI concatUri : concatUris) {
              GadgetHtmlNode newScript = new GadgetHtmlNode("script", null);
              newScript.setAttribute("src", concatUri.toString());
              parentNode.insertBefore(newScript, toRemove.get(0));
            }
            
            // Remove contributing match nodes
            for (GadgetHtmlNode remove : toRemove) {
              parentNode.removeChild(remove);
            }
            
            processScripts = false;
            scripts.clear();
            toRemove.clear();
          }
        }
      }
    }
  }
  
  private List<URI> getConcatenatedUris(String concatBase, List<URI> uris) {
    List<URI> concatUris = new LinkedList<URI>();
    int paramIndex = 1;
    StringBuilder builder = null;
    int maxUriLen = MAX_URL_LENGTH + concatBase.length();
    try {
      int uriIx = 0, lastUriIx = (uris.size() - 1);
      for (URI uri : uris) {
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
          concatUris.add(new URI(builder.toString()));
          builder = null;
          paramIndex = 0;
        }
        ++paramIndex;
        ++uriIx;
      }
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
    return concatUris;
  }
  
  protected String getConcatUrl() {
    return "/gadgets/concat?";
  }
  
  // Formerly StyleTagRewriter
  void transformStyleTags(List<GadgetHtmlNode> nodes, URI gadgetUri,
      LinkRewriter linkRewriter) {
    Queue<GadgetHtmlNode> nodesToProcess =
      new LinkedList<GadgetHtmlNode>();
    nodesToProcess.addAll(nodes);
  
    while (!nodesToProcess.isEmpty()) {
      GadgetHtmlNode curNode = nodesToProcess.remove();
      if (!curNode.isText()) {
        // Depth-first iteration over children. Order doesn't matter anyway.
        nodesToProcess.addAll(curNode.getChildren());
        
        if (curNode.getTagName().equalsIgnoreCase("style")) {
          String styleText = getNodeChildText(curNode);
          curNode.clearChildren();
          curNode.appendChild(new GadgetHtmlNode(
              CssRewriter.rewrite(styleText, gadgetUri, linkRewriter)));
        }
      }
    }
  }
  
  private static String getNodeChildText(GadgetHtmlNode node) {
    // TODO: move this to GadgetHtmlNode as a helper
    StringBuilder builder = new StringBuilder();
    for (GadgetHtmlNode child : node.getChildren()) {
      if (child.isText()) {
        builder.append(child.getText());
      }
    }
    return builder.toString();
  }
  
  private List<GadgetHtmlNode> getParsedHtmlNodes(String source) {
    List<ParsedHtmlNode> parsed = null;
    try {
      parsed = htmlParser.parse(source);
    } catch (GadgetException e) {
      // Can't rewrite something that can't be parsed
      return null;
    }
    
    if (parsed == null) {
      return null;
    }
    
    List<GadgetHtmlNode> nodes = new LinkedList<GadgetHtmlNode>();
    for (ParsedHtmlNode parsedNode : parsed) {
      nodes.add(new GadgetHtmlNode(parsedNode));
    }
    return nodes;
  }


  public static String producePreTokenSeparator(Token<HtmlTokenType> token,
      Token<HtmlTokenType> lastToken) {
    if (token.type == HtmlTokenType.ATTRNAME) {
      return " ";
    }
    if (token.type == HtmlTokenType.ATTRVALUE &&
        lastToken != null &&
        lastToken.type == HtmlTokenType.ATTRNAME) {
      return "=";
    }
    return "";
  }


  public static String producePostTokenSeparator(Token<HtmlTokenType> token,
      Token<HtmlTokenType> lastToken) {
    return "";
  }

}
