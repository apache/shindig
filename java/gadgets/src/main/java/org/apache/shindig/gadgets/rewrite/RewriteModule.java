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

import org.apache.shindig.gadgets.render.OpenSocialI18NGadgetRewriter;
import org.apache.shindig.gadgets.render.RenderingGadgetRewriter;
import org.apache.shindig.gadgets.render.SanitizingGadgetRewriter;
import org.apache.shindig.gadgets.render.SanitizingRequestRewriter;
import org.apache.shindig.gadgets.servlet.CajaContentRewriter;

import java.util.List;

import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

/**
 * Guice bindings for the rewrite package.
 */
public class RewriteModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(new TypeLiteral<List<GadgetRewriter>>(){})
        .annotatedWith(Names.named("shindig.rewriters.gadget"))
        .toProvider(GadgetRewritersProvider.class);
    bind(new TypeLiteral<List<GadgetRewriter>>(){})
        .annotatedWith(Names.named("shindig.rewriters.accelerate"))
        .toProvider(AccelRewritersProvider.class);
    bind(new TypeLiteral<List<RequestRewriter>>(){}).toProvider(RequestRewritersProvider.class);
  }

  private static class GadgetRewritersProvider implements Provider<List<GadgetRewriter>> {
    private final List<GadgetRewriter> rewriters;

    @Inject
    private GadgetRewritersProvider(PipelineDataGadgetRewriter pipelineRewriter,
        TemplateRewriter templateRewriter,
        HTMLContentRewriter optimizingRewriter,
        CssRequestRewriter cssRewriter,
        CajaContentRewriter cajaRewriter,
        SanitizingGadgetRewriter sanitizedRewriter,
        RenderingGadgetRewriter renderingRewriter,
        OpenSocialI18NGadgetRewriter i18nRewriter) {
      rewriters = Lists.newArrayList();
      rewriters.add(pipelineRewriter);
      rewriters.add(templateRewriter);
      rewriters.add(optimizingRewriter);
      rewriters.add(cajaRewriter);
      rewriters.add(sanitizedRewriter);
      rewriters.add(renderingRewriter);
      rewriters.add(i18nRewriter);
    }

    public List<GadgetRewriter> get() {
      return rewriters;
    }
  }

  private static class AccelRewritersProvider implements Provider<List<GadgetRewriter>> {
    private final List<GadgetRewriter> rewriters;

    @Inject
    private AccelRewritersProvider(
        HTMLContentRewriter optimizingRewriter,
        CajaContentRewriter cajaRewriter) {
      rewriters = Lists.newArrayList();
      rewriters.add(optimizingRewriter);
      rewriters.add(cajaRewriter);
    }

    public List<GadgetRewriter> get() {
      return rewriters;
    }
  }

  private static class RequestRewritersProvider implements Provider<List<RequestRewriter>> {
    private final List<RequestRewriter> rewriters;

    @Inject
    private RequestRewritersProvider(HTMLContentRewriter optimizingRewriter,
        CssRequestRewriter cssRewriter,
        SanitizingRequestRewriter sanitizedRewriter) {
      rewriters = Lists.newArrayList();
      rewriters.add(optimizingRewriter);
      rewriters.add(cssRewriter);
      rewriters.add(sanitizedRewriter);
    }

    public List<RequestRewriter> get() {
      return rewriters;
    }
  }
}
