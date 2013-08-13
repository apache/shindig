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
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.gadgets.parse.GadgetHtmlParser;
import org.apache.shindig.gadgets.render.CajaResponseRewriter;
import org.apache.shindig.gadgets.render.OpenSocialI18NGadgetRewriter;
import org.apache.shindig.gadgets.render.RenderingGadgetRewriter;
import org.apache.shindig.gadgets.render.SanitizingGadgetRewriter;
import org.apache.shindig.gadgets.render.SanitizingResponseRewriter;
import org.apache.shindig.gadgets.rewrite.ResponseRewriterList.RewriteFlow;
import org.apache.shindig.gadgets.rewrite.image.BasicImageRewriter;
import org.apache.shindig.gadgets.servlet.CajaContentRewriter;
import org.apache.shindig.gadgets.uri.AccelUriManager;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Guice bindings for the rewrite package.
 */
public class RewriteModule extends AbstractModule {
  public static final String ACCEL_CONTAINER = AccelUriManager.CONTAINER;
  public static final String DEFAULT_CONTAINER = ContainerConfig.DEFAULT_CONTAINER;

  // Mapbinder for the map from
  // RewritePath -> [ List of response rewriters ].
  protected MapBinder<RewritePath, List<ResponseRewriter>> mapbinder;

  @Override
  protected void configure() {
    configureGadgetRewriters();
    provideResponseRewriters();
  }

  protected void provideResponseRewriters() {
    mapbinder = MapBinder.newMapBinder(binder(), new TypeLiteral<RewritePath>(){},
                                       new TypeLiteral<List<ResponseRewriter>>() {});

    Provider<List<ResponseRewriter>> accelRewriterList = getResponseRewriters(
        ACCEL_CONTAINER, RewriteFlow.ACCELERATE);
    Provider<List<ResponseRewriter>> requestPipelineRewriterList = getResponseRewriters(
        DEFAULT_CONTAINER, RewriteFlow.REQUEST_PIPELINE);

    addBindingForRewritePath(DEFAULT_CONTAINER, RewriteFlow.REQUEST_PIPELINE);
    addBindingForRewritePath(DEFAULT_CONTAINER, RewriteFlow.DEFAULT);
    addBindingForRewritePath(ACCEL_CONTAINER, RewriteFlow.ACCELERATE);
    addBindingForRewritePath(ACCEL_CONTAINER, RewriteFlow.REQUEST_PIPELINE,
                             requestPipelineRewriterList);
    addBindingForRewritePath(ACCEL_CONTAINER, RewriteFlow.DEFAULT, accelRewriterList);
  }

  protected void addBindingForRewritePath(String container, RewriteFlow rewriteFlow,
                                          Provider<List<ResponseRewriter>> list) {
    RewritePath rewritePath = new RewritePath(container, rewriteFlow);
    mapbinder.addBinding(rewritePath).toProvider(list);
  }

  protected void addBindingForRewritePath(String container, RewriteFlow rewriteFlow) {
    addBindingForRewritePath(container, rewriteFlow, binder().getProvider(
        getKey(container, rewriteFlow)));
  }

  protected Provider<List<ResponseRewriter>> getResponseRewriters(String container,
                                                                  RewriteFlow flow) {
    return binder().getProvider(getKey(container, flow));
  }

  protected Key<List<ResponseRewriter>> getKey(String container, RewriteFlow flow) {
    return Key.get(new TypeLiteral<List<ResponseRewriter>>() {},
                   new RewritePath(container, flow));
  }


  // Provides ResponseRewriterRegistry for DEFAULT flow.
  @Provides
  @Singleton
  @RewriterRegistry(rewriteFlow = RewriteFlow.DEFAULT)
  public ResponseRewriterRegistry provideDefaultList(GadgetHtmlParser parser,
      Provider<Map<RewritePath, Provider<List<ResponseRewriter>>>> rewritePathToRewriterList) {
    return new ContextAwareRegistry(parser, RewriteFlow.DEFAULT,
                                    rewritePathToRewriterList);
  }

  // Provides ResponseRewriterRegistry for REQUEST_PIPELINE flow.
  @Provides
  @Singleton
  @RewriterRegistry(rewriteFlow = RewriteFlow.REQUEST_PIPELINE)
  public ResponseRewriterRegistry provideRequestPipelineList(GadgetHtmlParser parser,
      Provider<Map<RewritePath, Provider<List<ResponseRewriter>>>> rewritePathToRewriterList) {
    return new ContextAwareRegistry(parser, RewriteFlow.REQUEST_PIPELINE,
                                    rewritePathToRewriterList);
  }

  // Provides ResponseRewriterRegistry for ACCELERATE flow.
  @Provides
  @Singleton
  @RewriterRegistry(rewriteFlow = RewriteFlow.ACCELERATE)
  public ResponseRewriterRegistry provideAccelerateList(GadgetHtmlParser parser,
      Provider<Map<RewritePath, Provider<List<ResponseRewriter>>>> rewritePathToRewriterList) {
    return new ContextAwareRegistry(parser, RewriteFlow.ACCELERATE,
                                    rewritePathToRewriterList);
  }

  protected void configureGadgetRewriters() {
    Multibinder<GadgetRewriter> multibinder = Multibinder.newSetBinder(binder(),
        GadgetRewriter.class, Names.named("shindig.rewriters.gadget.set"));
    multibinder.addBinding().to(PipelineDataGadgetRewriter.class);
    multibinder.addBinding().to(TemplateRewriter.class);
    multibinder.addBinding().to(AbsolutePathReferenceRewriter.class);
    multibinder.addBinding().to(StyleTagExtractorContentRewriter.class);
    multibinder.addBinding().to(StyleAdjacencyContentRewriter.class);
    multibinder.addBinding().to(ProxyingContentRewriter.class);
    multibinder.addBinding().to(CajaContentRewriter.class);
    multibinder.addBinding().to(SanitizingGadgetRewriter.class);
    multibinder.addBinding().to(RenderingGadgetRewriter.class);
    multibinder.addBinding().to(OpenSocialI18NGadgetRewriter.class);
  }

  @Provides
  @Singleton
  @Named("shindig.rewriters.gadget")
  protected List<GadgetRewriter> provideGadgetRewriters(
      @Named("shindig.rewriters.gadget.set") Set<GadgetRewriter> gadgetRewritersSet) {
    // Multibinding promise order within a binding module
    return ImmutableList.copyOf(gadgetRewritersSet);
  }

  @Provides
  @Singleton
  @Named("shindig.rewriters.accelerate")
  protected List<GadgetRewriter> provideAccelRewriters(
      ProxyingContentRewriter proxyingContentRewriter,
      CajaContentRewriter cajaRewriter) {
    return ImmutableList.of(proxyingContentRewriter, cajaRewriter);
  }

  // Provides the list of rewriters to be applied for REQUEST_PIPELINE flow.
  @Provides
  @Singleton
  @ResponseRewriterList(rewriteFlow = RewriteFlow.REQUEST_PIPELINE)
  protected List<ResponseRewriter> providePreCacheResponseRewriters(
      BasicImageRewriter imageRewriter) {
    return ImmutableList.<ResponseRewriter>of(imageRewriter);
  }

  // Provides the list of rewriters to be applied for DEFAULT flow.
  @Provides
  @Singleton
  @ResponseRewriterList(rewriteFlow = RewriteFlow.DEFAULT)
  protected List<ResponseRewriter> provideDefaultRewriters(
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

  // Provides the list of rewriters to be applied for ACCELERATE flow for
  // accel container.
  @Provides
  @Singleton
  @ResponseRewriterList(rewriteFlow = RewriteFlow.ACCELERATE,
                        container = AccelUriManager.CONTAINER)
  protected List<ResponseRewriter> provideAccelResponseRewriters(
      AbsolutePathReferenceRewriter absolutePathReferenceRewriter,
      StyleTagProxyEmbeddedUrlsRewriter styleTagProxyEmbeddedUrlsRewriter,
      ProxyingContentRewriter proxyingContentRewriter) {
    return ImmutableList.<ResponseRewriter>of(
        absolutePathReferenceRewriter,
        styleTagProxyEmbeddedUrlsRewriter,
        proxyingContentRewriter);
  }
}
