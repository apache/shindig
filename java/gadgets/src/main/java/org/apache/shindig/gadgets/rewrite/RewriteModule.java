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
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import org.apache.shindig.gadgets.parse.GadgetHtmlParser;
import org.apache.shindig.gadgets.render.CajaResponseRewriter;
import org.apache.shindig.gadgets.render.OpenSocialI18NGadgetRewriter;
import org.apache.shindig.gadgets.render.RenderingGadgetRewriter;
import org.apache.shindig.gadgets.render.SanitizingGadgetRewriter;
import org.apache.shindig.gadgets.render.SanitizingResponseRewriter;
import org.apache.shindig.gadgets.rewrite.image.BasicImageRewriter;
import org.apache.shindig.gadgets.servlet.CajaContentRewriter;

import java.util.List;

/**
 * Guice bindings for the rewrite package.
 */
public class RewriteModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(ResponseRewriterRegistry.class)
        .annotatedWith(Names.named("shindig.accelerate.response.rewriter.registry"))
        .to(AccelResponseRewriterRegistry.class);
  }

  @Provides
  @Singleton
  @Named("shindig.rewriters.gadget")
  protected List<GadgetRewriter> provideGadgetRewriters(
      PipelineDataGadgetRewriter pipelineRewriter,
      TemplateRewriter templateRewriter,
      AbsolutePathReferenceRewriter absolutePathRewriter,
      StyleTagExtractorContentRewriter styleTagExtractorRewriter,
      StyleAdjacencyContentRewriter styleAdjacencyRewriter,
      ProxyingContentRewriter proxyingRewriter,
      CajaContentRewriter cajaRewriter,
      SanitizingGadgetRewriter sanitizedRewriter,
      RenderingGadgetRewriter renderingRewriter,
      OpenSocialI18NGadgetRewriter i18nRewriter) {
    return ImmutableList.of(pipelineRewriter, templateRewriter,
        absolutePathRewriter, styleTagExtractorRewriter, styleAdjacencyRewriter, proxyingRewriter,
        cajaRewriter, sanitizedRewriter, renderingRewriter, i18nRewriter);
  }

  @Provides
  @Singleton
  @Named("shindig.rewriters.accelerate")
  protected List<GadgetRewriter> provideAccelRewriters(
      ProxyingContentRewriter proxyingContentRewriter,
      CajaContentRewriter cajaRewriter) {
    return ImmutableList.of(proxyingContentRewriter, cajaRewriter);
  }
  
  // TODO: Clean this up. Ideally we would let the ResponseRewriterRegistry
  // binding create the concrete object instance.
  @Provides
  @Singleton
  @Named("shindig.rewriters.response.pre-cache")
  protected ResponseRewriterRegistry providePreCacheResponseRewritersRegistry(
      GadgetHtmlParser parser,
      @Named("shindig.rewriters.response.pre-cache") List<ResponseRewriter> preCached) {
    return new DefaultResponseRewriterRegistry(preCached, parser);
  }

  @Provides
  @Singleton
  @Named("shindig.rewriters.response.pre-cache")
  protected List<ResponseRewriter> providePreCacheResponseRewriters(
      BasicImageRewriter imageRewriter) {
    return ImmutableList.<ResponseRewriter>of(imageRewriter);
  }

  @Provides
  @Singleton
  protected List<ResponseRewriter> provideResponseRewriters(
      AbsolutePathReferenceRewriter absolutePathRewriter,
      StyleTagExtractorContentRewriter styleTagExtractorRewriter,
      StyleAdjacencyContentRewriter styleAdjacencyRewriter,
      ProxyingContentRewriter proxyingRewriter,
      CssResponseRewriter cssRewriter,
      SanitizingResponseRewriter sanitizedRewriter,
      CajaResponseRewriter cajaRewriter) {
    return ImmutableList.of(
        absolutePathRewriter, styleTagExtractorRewriter, styleAdjacencyRewriter, proxyingRewriter,
        cssRewriter, sanitizedRewriter, cajaRewriter);
  }

  @Provides
  @Singleton
  @Named("shindig.accelerate.response.rewriters")
  protected List<ResponseRewriter> provideAccelResponseRewriters(
      AbsolutePathReferenceRewriter absolutePathReferenceRewriter,
      StyleTagProxyEmbeddedUrlsRewriter styleTagProxyEmbeddedUrlsRewriter,
      ProxyingContentRewriter proxyingContentRewriter) {
    return ImmutableList.of((ResponseRewriter) absolutePathReferenceRewriter,
        (ResponseRewriter) styleTagProxyEmbeddedUrlsRewriter,
        (ResponseRewriter) proxyingContentRewriter);
  }
}
