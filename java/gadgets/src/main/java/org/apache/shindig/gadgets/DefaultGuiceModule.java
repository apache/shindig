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
package org.apache.shindig.gadgets;

import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.parse.ParseModule;
import org.apache.shindig.gadgets.preload.HttpPreloader;
import org.apache.shindig.gadgets.preload.PipelinedDataPreloader;
import org.apache.shindig.gadgets.preload.Preloader;
import org.apache.shindig.gadgets.render.RenderingContentRewriter;
import org.apache.shindig.gadgets.render.SanitizedRenderingContentRewriter;
import org.apache.shindig.gadgets.rewrite.CSSContentRewriter;
import org.apache.shindig.gadgets.rewrite.ContentRewriter;
import org.apache.shindig.gadgets.rewrite.HTMLContentRewriter;
import org.apache.shindig.gadgets.servlet.CajaContentRewriter;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Creates a module to supply all of the Basic* classes
 */
public class DefaultGuiceModule extends AbstractModule {

  /** {@inheritDoc} */
  @Override
  protected void configure() {

    ExecutorService service = Executors.newCachedThreadPool();
    bind(Executor.class).toInstance(service);
    bind(ExecutorService.class).toInstance(service);

    this.install(new ParseModule());

    bind(new TypeLiteral<List<ContentRewriter>>(){}).toProvider(ContentRewritersProvider.class);
    bind(new TypeLiteral<List<Preloader>>(){}).toProvider(PreloaderProvider.class);

    TypeLiteral<Set<String>> setLiteral = new TypeLiteral<Set<String>>(){};

    // NOTE: Sanitization only works when using the "full" Neko HTML parser. It is not recommended
    // that you attempt to use sanitization without it.
    bind(setLiteral)
        .annotatedWith(SanitizedRenderingContentRewriter.AllowedTags.class)
        .toInstance(ImmutableSet.of("a", "abbr", "acronym", "area", "b", "bdo", "big", "blockquote",
            "body", "br", "caption", "center", "cite", "code", "col", "colgroup", "dd", "del",
            "dfn", "div", "dl", "dt", "em", "font", "h1", "h2", "h3", "h4", "h5", "h6", "head",
            "hr", "html", "i", "img", "ins", "legend", "li", "map", "ol", "p", "pre", "q", "s",
            "samp", "small", "span", "strike", "strong", "style", "sub", "sup", "table",
            "tbody", "td", "tfoot", "th", "thead", "tr", "tt", "u", "ul"));

    bind(setLiteral)
        .annotatedWith(SanitizedRenderingContentRewriter.AllowedAttributes.class)
        .toInstance(ImmutableSet.of("abbr", "align", "alt", "axis", "bgcolor", "border",
            "cellpadding", "cellspacing", "char", "charoff", "cite", "class", "clear", "color",
            "cols", "colspan", "compact", "coords", "datetime", "dir", "face", "headers", "height",
            "href", "hreflang", "hspace", "id", "ismap", "lang", "longdesc", "name", "nohref",
            "noshade", "nowrap", "rel", "rev", "rowspan", "rules", "scope", "shape", "size", "span",
            "src", "start", "style", "summary", "title", "type", "usemap", "valign", "value",
            "vspace", "width"));

    // We perform static injection on HttpResponse for cache TTLs.
    requestStaticInjection(HttpResponse.class);
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

  private static class PreloaderProvider implements Provider<List<Preloader>> {
    private final List<Preloader> preloaders;

    @Inject
    public PreloaderProvider(HttpPreloader httpPreloader, PipelinedDataPreloader socialPreloader) {
      preloaders = Lists.newArrayList(httpPreloader, socialPreloader);
    }

    public List<Preloader> get() {
      return preloaders;
    }
  }
}
