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

import org.apache.shindig.gadgets.render.RenderingContentRewriter;
import org.apache.shindig.gadgets.render.SanitizedRenderingContentRewriter;
import org.apache.shindig.gadgets.servlet.CajaContentRewriter;

import java.util.List;

import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;

/**
 * Guice bindings for the rewrite package.
 */
public class RewriteModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(new TypeLiteral<List<ContentRewriter>>(){}).toProvider(ContentRewritersProvider.class);
  }

  private static class ContentRewritersProvider implements Provider<List<ContentRewriter>> {
    private final List<ContentRewriter> rewriters;

    @Inject
    public ContentRewritersProvider(HTMLContentRewriter optimizingRewriter,
                                    CSSContentRewriter cssRewriter,
                                    CajaContentRewriter cajaRewriter,
                                    SanitizedRenderingContentRewriter sanitizedRewriter,
                                    RenderingContentRewriter renderingRewriter) {
      rewriters = Lists.newArrayList();
      rewriters.add(optimizingRewriter);
      rewriters.add(cssRewriter);
      rewriters.add(cajaRewriter);
      rewriters.add(sanitizedRewriter);
      rewriters.add(renderingRewriter);
    }

    public List<ContentRewriter> get() {
      return rewriters;
    }
  }
}
