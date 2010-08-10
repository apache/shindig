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

  private final String prefix;

  private final ContentRewriterFeature rewriterFeature;

  private final Uri gadgetUri;

  public ProxyingLinkRewriter(Uri gadgetUri, ContentRewriterFeature rewriterFeature,
      String prefix) {
    this.prefix = prefix;
    this.rewriterFeature = rewriterFeature;
    this.gadgetUri = gadgetUri;
  }

  public String rewrite(String link, Uri context) {
    link = link.trim();
    // We shouldnt bother proxying empty URLs
    if (link.length() == 0) {
      return link;
    }

    try {
      Uri linkUri = Uri.parse(link);
      Uri uri = context.resolve(linkUri);
      if (rewriterFeature.shouldRewriteURL(uri.toString())) {
        String result = prefix
            + Utf8UrlCoder.encode(uri.toString())
            + ((gadgetUri == null) ? "" : "&gadget=" + Utf8UrlCoder.encode(gadgetUri.toString()))
            + "&fp="
            + rewriterFeature.getFingerprint();
        if (rewriterFeature.getExpires() != null) {
          result += "&" + ProxyBase.REFRESH_PARAM  + "=" + rewriterFeature.getExpires().toString();
        }
        return result;
      } else {
        return uri.toString();
      }
    } catch (IllegalArgumentException use) {
      // Unrecoverable. Just return link
      return link;
    }
  }
}
