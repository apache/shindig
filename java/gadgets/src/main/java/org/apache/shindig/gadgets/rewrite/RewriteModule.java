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

import org.apache.shindig.gadgets.render.OpenSocialI18NGadgetRewriter;
import org.apache.shindig.gadgets.render.RenderingGadgetRewriter;
import org.apache.shindig.gadgets.render.old.SanitizingGadgetRewriter;
import org.apache.shindig.gadgets.render.old.SanitizingRequestRewriter;
import org.apache.shindig.gadgets.rewrite.image.BasicImageRewriter;
import org.apache.shindig.gadgets.rewrite.old.CssRequestRewriter;
import org.apache.shindig.gadgets.rewrite.old.HTMLContentRewriter;
import org.apache.shindig.gadgets.servlet.CajaContentRewriter;

import java.util.List;

/**
 * Guice bindings for the rewrite package.
 */
public class RewriteModule extends AbstractModule {

  @Override
  protected void configure() {
  }

  @Provides
  @Singleton
  @Named("shindig.rewriters.gadget")
  protected List<GadgetRewriter> provideGadgetRewriters(
      PipelineDataGadgetRewriter pipelineRewriter,
      TemplateRewriter templateRewriter,
      HTMLContentRewriter optimizingRewriter,
      CssRequestRewriter cssRewriter,
      CajaContentRewriter cajaRewriter,
      SanitizingGadgetRewriter sanitizedRewriter,
      RenderingGadgetRewriter renderingRewriter,
      OpenSocialI18NGadgetRewriter i18nRewriter) {
    return ImmutableList.of(pipelineRewriter, templateRewriter, optimizingRewriter, cajaRewriter, sanitizedRewriter, renderingRewriter, i18nRewriter);
  }

  @Provides
  @Singleton
  @Named("shindig.rewriters.accelerate")
  protected List<GadgetRewriter> provideAccelRewriters(
      HTMLContentRewriter optimizingRewriter,
      CajaContentRewriter cajaRewriter) {
    return ImmutableList.of(optimizingRewriter, cajaRewriter);
  }

  @Provides
  @Singleton
  protected List<RequestRewriter> provideRequestRewriters(
      HTMLContentRewriter optimizingRewriter,
      CssRequestRewriter cssRewriter,
      SanitizingRequestRewriter sanitizedRewriter) {
    return ImmutableList.of(optimizingRewriter, cssRewriter, sanitizedRewriter);
  }
  
  @Provides
  @Singleton
  protected List<ResponseRewriter> provideResponseRewriters(
      BasicImageRewriter imageRewriter) {
    return ImmutableList.<ResponseRewriter>of(imageRewriter);
  }
}
