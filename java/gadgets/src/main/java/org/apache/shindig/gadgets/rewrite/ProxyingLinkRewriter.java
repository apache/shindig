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

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.util.Utf8UrlCoder;
import org.apache.shindig.gadgets.servlet.ProxyBase;

/**
 * Simple link rewriter that will rewrite a link to the form http://www.host.com/proxy/url=<url
 * encoded link>&gadget=<gadget spec url>&fp=<fingeprint of rewriting rule>
 */
public class ProxyingLinkRewriter implements LinkRewriter {

  protected final ContentRewriterUris rewriterUris;
  protected final ContentRewriterFeature rewriterFeature;
  protected final Uri gadgetUri;
  protected final String container;
  protected final boolean debug;
  protected final boolean ignoreCache;
  
  public ProxyingLinkRewriter(ContentRewriterUris rewriterUris, Uri gadgetUri,
      ContentRewriterFeature rewriterFeature, String container, boolean debug,
      boolean ignoreCache) {
    this.rewriterUris = rewriterUris;
    this.rewriterFeature = rewriterFeature;
    this.gadgetUri = gadgetUri;
    this.container = container;
    this.debug = debug;
    this.ignoreCache = ignoreCache;
  }

  public String rewrite(String link, Uri context) {
    String prefix = rewriterUris.getProxyBase(container);
    Uri parsedPrefix = Uri.parse(prefix);
    link = link.trim();
    // We shouldnt bother proxying empty URLs
    if (link.length() == 0) {
      return link;
    }

    try {
      Uri linkUri = processLink(parsedPrefix, Uri.parse(link));
      Uri uri = context.resolve(linkUri);
      if (rewriterFeature.shouldRewriteURL(uri.toString())) {
        StringBuilder result = new StringBuilder();
        result.append(prefix);
        result.append(Utf8UrlCoder.encode(uri.toString()));
        result.append(((gadgetUri == null) ? "" : "&gadget=" + Utf8UrlCoder.encode(gadgetUri.toString()))); 
        result.append("&fp=");
        result.append(rewriterFeature.getFingerprint());
        if(debug)
          result.append("&debug=1");
        if(ignoreCache)
          result.append("&nocache=1");
        if (rewriterFeature.getExpires() != null) {
          result.append('&').append(ProxyBase.REFRESH_PARAM).append('=').append(rewriterFeature.getExpires().toString());
        }
        return result.toString();
      } else {
        return uri.toString();
      }
    } catch (IllegalArgumentException use) {
      // Unrecoverable. Just return link
      return link;
    }
  }

  /**
   * Preprocess link to avoid double-proxying
   */
  private Uri processLink(Uri parsedPrefix, Uri original) {
    if (parsedPrefix.getPath().equals(original.getPath())) {
      // Link is already rewritten to the proxy so extract the url param
      return Uri.parse(original.getQueryParameter("url"));
    }
    return original;
  }
}
