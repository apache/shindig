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

import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.http.HttpCache;
import org.apache.shindig.gadgets.http.RequestPipeline;
import org.apache.shindig.gadgets.rewrite.DomWalker;
import org.apache.shindig.gadgets.rewrite.DomWalker.Visitor;
import org.apache.shindig.gadgets.uri.ConcatUriManager;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Concatenates non-private and cached script resources.
 * CacheEnforcementVisitor is used to rewrite only non-private & cached scripts.
 * @since 2.0.0
 */
public class ScriptConcatContentRewriter extends DomWalker.Rewriter {
  private final ContentRewriterFeature.Factory featureConfigFactory;
  private final ConcatUriManager concatUriManager;
  private final Executor executor;
  private final RequestPipeline requestPipeline;
  private final HttpCache cache;

  @Inject
  public ScriptConcatContentRewriter(ConcatUriManager concatUriManager,
                                     ContentRewriterFeature.Factory featureConfigFactory,
                                     @Named("shindig.concat.executor") Executor executor,
                                     HttpCache cache,
                                     RequestPipeline requestPipeline) {
    this.concatUriManager = concatUriManager;
    this.featureConfigFactory = featureConfigFactory;
    this.executor = executor;
    this.cache = cache;
    this.requestPipeline = requestPipeline;
  }

  @Override
  protected List<Visitor> makeVisitors(Gadget context, Uri gadgetUri) {
    ContentRewriterFeature.Config config = featureConfigFactory.get(context.getSpec());
    return Arrays.asList(
        new CacheEnforcementVisitor(config, executor, cache, requestPipeline,
            CacheEnforcementVisitor.Tags.SCRIPT),
        new ConcatVisitor.Js(config, concatUriManager));
  }
}
