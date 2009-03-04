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
package org.apache.shindig.gadgets.render;

import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;

/**
 * Guice bindings for the render package.
 */
public class RenderModule extends AbstractModule {
  @Override
  protected void configure() {
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

  }

}
