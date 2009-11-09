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
import org.apache.shindig.gadgets.rewrite.ContentRewriterFeature;
import org.apache.shindig.gadgets.rewrite.ContentRewriterUris;
import org.apache.shindig.gadgets.rewrite.ProxyingLinkRewriter;
import org.apache.shindig.gadgets.servlet.ProxyBase;

/**
 * Forcible rewrite the link through the proxy and force sanitization with
 * an expected mime type
 */
public class SanitizingProxyingLinkRewriter extends ProxyingLinkRewriter {

  private final String expectedMime;

  public SanitizingProxyingLinkRewriter(ContentRewriterUris rewriterUris,
      Uri gadgetUri, ContentRewriterFeature rewriterFeature, String container,
      String expectedMime, boolean debug, boolean nocache) {
    super(rewriterUris, gadgetUri, rewriterFeature, container, debug, nocache);
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