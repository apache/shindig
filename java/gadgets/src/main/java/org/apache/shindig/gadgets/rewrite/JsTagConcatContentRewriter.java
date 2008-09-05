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

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.apache.shindig.common.util.Utf8UrlCoder;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.parse.GadgetHtmlNode;
import org.apache.shindig.gadgets.servlet.ProxyBase;
import org.apache.shindig.gadgets.spec.GadgetSpec;

public class JsTagConcatContentRewriter implements ContentRewriter {
  private final static int MAX_URL_LENGTH = 1500;
  
  private final ContentRewriterFeature.Factory rewriterFeatureFactory;
  private final String concatUrlBase;
  
  private static final String DEFAULT_CONCAT_URL_BASE = "/gadgets/concat?";
  
  public JsTagConcatContentRewriter(ContentRewriterFeature.Factory rewriterFeatureFactory,
      String concatUrlBase) {
    this.rewriterFeatureFactory = rewriterFeatureFactory;
    if (concatUrlBase != null) {
      this.concatUrlBase = concatUrlBase;
    } else {
      this.concatUrlBase = DEFAULT_CONCAT_URL_BASE;
    }
  }

  public HttpResponse rewrite(HttpRequest request, HttpResponse original) {
    // TODO Auto-generated method stub
    return null;
  }

  public void rewrite(Gadget gadget) {
    ContentRewriterFeature rewriterFeature = rewriterFeatureFactory.get(gadget.getSpec());
    if (!rewriterFeature.isRewriteEnabled() ||
        !rewriterFeature.getIncludedTags().contains("script")) {
      return;
    }
    
    // Bootstrap queue of children over which to iterate,
    // ie. lists of siblings to potentially combine
    Queue<GadgetHtmlNode> nodesToProcess =
        new LinkedList<GadgetHtmlNode>();
    nodesToProcess.add(gadget.getParseTree());

    
    String concatBase = getJsConcatBase(gadget.getSpec(), rewriterFeature);
    
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
                  gadget.getSpec().getUrl().resolve(new URI(cur.getAttributeValue("src")));
            } catch (URISyntaxException use) {
              // Same behavior as JavascriptTagMerger
              // Perhaps switch to ignoring script src instead?
              throw new RuntimeException(use);
            }
            scripts.add(scriptUri);
            toRemove.add(cur);
          } else if (scripts.size() > 0 && cur.isText() && cur.getText().matches("\\s*")) {
            // Whitespace after one or more scripts. Ignore and remove.
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
  
  String getJsConcatBase(GadgetSpec spec, ContentRewriterFeature rewriterFeature) {
    return concatUrlBase +
           ProxyBase.REWRITE_MIME_TYPE_PARAM +
           "=text/javascript&" +
           "gadget=" +
           Utf8UrlCoder.encode(spec.getUrl().toString()) +
           "&fp=" +
           rewriterFeature.getFingerprint() +
           '&';
  }

}
