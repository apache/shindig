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

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.rewrite.DomWalker.Visitor;
import org.apache.shindig.gadgets.uri.ConcatUriManager;
import org.apache.shindig.gadgets.uri.ProxyUriManager;

import java.util.List;

/**
 * Rewrites gadget content to force resources through the /proxy endpoint.
 *
 * @since 2.0.0
 */
public class ProxyingContentRewriter extends DomWalker.Rewriter {
  private final ContentRewriterFeature.Factory featureConfigFactory;
  private final ProxyUriManager proxyUriManager;
  private final ConcatUriManager concatUriManager;

  @Inject
  public ProxyingContentRewriter(ContentRewriterFeature.Factory featureConfigFactory,
      ProxyUriManager proxyUriManager, ConcatUriManager concatUriManager) {
    this.featureConfigFactory = featureConfigFactory;
    this.proxyUriManager = proxyUriManager;
    this.concatUriManager = concatUriManager;
  }

  @Override
  protected List<Visitor> makeVisitors(Gadget context, Uri gadgetUri) {
    ContentRewriterFeature.Config config = featureConfigFactory.get(context.getSpec());
    // Note that concat is including with proxy in order to prevent
    // proxying the rewritten concat url
    // Basically Url rewritters should all be in one dom walker.
    return ImmutableList.of(
        new ConcatVisitor.Js(config, concatUriManager),
        new ConcatVisitor.Css(config, concatUriManager),
        new ProxyingVisitor(config, proxyUriManager,
                            ProxyingVisitor.Tags.SCRIPT,
                            ProxyingVisitor.Tags.STYLESHEET,
                            ProxyingVisitor.Tags.EMBEDDED_IMAGES));
  }
}
