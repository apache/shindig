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

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.GadgetSpecFactory;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.spec.GadgetSpec;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import java.net.URI;
import java.util.Collections;
import java.util.Set;

/**
 * Factory for content rewriter features
 */
@Singleton
public class ContentRewriterFeatureFactory {

  private final GadgetSpecFactory specFactory;
  private final String includeUrls;
  private final String excludeUrls;
  private final String expires;
  private final Set<String> includeTags;

  private ContentRewriterFeature defaultFeature;

  @Inject
  public ContentRewriterFeatureFactory(
      GadgetSpecFactory specFactory,
      @Named("shindig.content-rewrite.include-urls")String includeUrls,
      @Named("shindig.content-rewrite.exclude-urls")String excludeUrls,
      @Named("shindig.content-rewrite.expires")String expires,
      @Named("shindig.content-rewrite.include-tags")String includeTags) {
    this.specFactory = specFactory;
    this.includeUrls = includeUrls;
    this.excludeUrls = excludeUrls;
    this.expires = expires;
    this.includeTags = Sets.newHashSet();
    for (String s : includeTags.split(",")) {
      if (s != null && s.trim().length() > 0) {
        this.includeTags.add(s.trim().toLowerCase());
      }
    }
    defaultFeature = new ContentRewriterFeature(null, includeUrls, excludeUrls, expires,
        this.includeTags);
  }

  public ContentRewriterFeature getDefault() {
    return defaultFeature;
  }

  public ContentRewriterFeature get(HttpRequest request) {
    Uri gadgetUri = request.getGadget();
    GadgetSpec spec;
    if (gadgetUri != null) {
      final URI gadgetJavaUri = gadgetUri.toJavaUri();
      try {
        GadgetContext context = new GadgetContext() {
          @Override
          public URI getUrl() {
            return gadgetJavaUri;
          }
        };
        
        spec = specFactory.getGadgetSpec(context);
        if (spec != null) {
          return get(spec);
        }
      } catch (GadgetException ge) {
        return defaultFeature;
      }
    }
    return defaultFeature;
  }

  public ContentRewriterFeature get(GadgetSpec spec) {
    ContentRewriterFeature rewriterFeature =
        (ContentRewriterFeature)spec.getAttribute("content-rewriter");
    if (rewriterFeature != null) return rewriterFeature;
    rewriterFeature
        = new ContentRewriterFeature(spec, includeUrls, excludeUrls, expires, includeTags);
    spec.setAttribute("content-rewriter", rewriterFeature);
    return rewriterFeature;
  }

  /**
   * Create a rewriter feature that allows all URIs to be rewritten.
   */
  public ContentRewriterFeature createRewriteAllFeature(int ttl) {
    return new ContentRewriterFeature(null,
        ".*", "", (ttl == -1) ? "HTTP" : Integer.toString(ttl),
        Collections.<String>emptySet());
  }
}
