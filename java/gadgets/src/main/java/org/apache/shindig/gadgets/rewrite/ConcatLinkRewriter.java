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

import com.google.common.collect.Lists;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.util.Utf8UrlCoder;
import org.apache.shindig.gadgets.servlet.ProxyBase;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.LinkedHashSet;
import java.util.List;

public class ConcatLinkRewriter {
  private final static int MAX_URL_LENGTH = 1500;

  private final ContentRewriterUris rewriterUris;

  private final ContentRewriterFeature rewriterFeature;
  private final Uri gadgetUri;
  private final String container;
  private final boolean debug;
  private final boolean ignoreCache;

  public ConcatLinkRewriter(ContentRewriterUris rewriterUris, Uri gadgetUri,
      ContentRewriterFeature rewriterFeature, String container, boolean debug,
      boolean ignoreCache) {
    this.rewriterUris = rewriterUris;
    this.rewriterFeature = rewriterFeature;
    this.gadgetUri = gadgetUri;
    this.container = container;
    this.debug = debug;
    this.ignoreCache = ignoreCache;
  }

  public List<Uri> rewrite(String mimeType, LinkedHashSet<Uri> uris) {
    String concatBase = getConcatBase(gadgetUri, rewriterFeature, mimeType,
        container);
    List<Uri> concatUris = Lists.newLinkedList();
    int paramIndex = 1;
    StringBuilder builder = null;
    int maxUriLen = MAX_URL_LENGTH + concatBase.length();
    try {
      int uriIx = 0;
      //
      for (Uri uri : uris) {
        String uriStr = uri.toString();
        if (builder != null && builder.length() + uriStr.length() > maxUriLen) {
          // The next one will go over limit
          concatUris.add(Uri.parse(builder.toString()));
          builder = null;
          paramIndex = 1;
          
          // If the current uri is too long, simply don't rewrite, since
          // concat is for developers benefit
          if(uriStr.length() > MAX_URL_LENGTH) {
            concatUris.add(uri);
            continue;
          }
        } 

        if (paramIndex == 1) {
          builder = new StringBuilder(concatBase);
          if (debug)
            builder.append("debug=1&");
          if (ignoreCache)
            builder.append("nocache=1&");
          if (rewriterFeature.getExpires() != null) {
            builder.append(ProxyBase.REFRESH_PARAM).append('=').append(rewriterFeature.getExpires().toString()).append('&');
          }
        } else {
          builder.append('&');
        }
        builder.append(paramIndex).append('=').append(
            URLEncoder.encode(uriStr, "UTF-8"));
        ++paramIndex;
        ++uriIx;
      }
      if (builder != null)
        concatUris.add(Uri.parse(builder.toString()));
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
    return concatUris;
  }

  protected String getConcatBase(Uri gadgetUri, ContentRewriterFeature feature,
      String mimeType, String container) {
    String concatBaseNoGadget = rewriterUris.getConcatBase(container);
    return concatBaseNoGadget
        + ProxyBase.REWRITE_MIME_TYPE_PARAM
        + '='
        + mimeType
        + ((gadgetUri == null) ? "" : "&gadget="
            + Utf8UrlCoder.encode(gadgetUri.toString())) + "&fp="
        + feature.getFingerprint() + '&';
  }

}
