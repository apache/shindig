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

import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import java.net.URI;

/**
 * Rewrite links to referenced content in stylsheets
 */
public class CSSContentRewriter implements ContentRewriter {

  private final ContentRewriterFeatureFactory rewriterFeatureFactory;
  private final String proxyBaseNoGadget;

  @Inject
  public CSSContentRewriter(ContentRewriterFeatureFactory rewriterFeatureFactory,
      @Named("shindig.content-rewrite.proxy-url")String proxyBaseNoGadget) {
    this.rewriterFeatureFactory = rewriterFeatureFactory;
    this.proxyBaseNoGadget = proxyBaseNoGadget;
  }

  public RewriterResults rewrite(Gadget gadget, MutableContent content) {
    // Not supported
    return null;
  }

  public RewriterResults rewrite(HttpRequest request, HttpResponse original, MutableContent content) {
    if (!RewriterUtils.isCss(request, original)) {
      return null;      
    }
    ContentRewriterFeature feature = rewriterFeatureFactory.get(request);
    content.setContent(CssRewriter.rewrite(content.getContent(), request.getUri().toJavaUri(),
        createLinkRewriter(request.getGadget().toJavaUri(),  feature)));

    return RewriterResults.cacheableIndefinitely();
  }

  protected LinkRewriter createLinkRewriter(URI gadgetUri, ContentRewriterFeature feature) {
    return new ProxyingLinkRewriter(gadgetUri, feature, proxyBaseNoGadget);
  }
}

