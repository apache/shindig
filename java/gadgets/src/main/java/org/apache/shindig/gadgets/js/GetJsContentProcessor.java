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

package org.apache.shindig.gadgets.js;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.features.ApiDirective;
import org.apache.shindig.gadgets.features.FeatureRegistry;
import org.apache.shindig.gadgets.features.FeatureRegistry.FeatureBundle;
import org.apache.shindig.gadgets.features.FeatureResource;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.rewrite.js.JsCompiler;
import org.apache.shindig.gadgets.uri.JsUriManager.JsUri;
import org.apache.shindig.gadgets.uri.UriStatus;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Retrieves the requested Javascript code using a {@link JsProcessor}.
 */
public class GetJsContentProcessor implements JsProcessor {
  private static final Joiner UNKNOWN_FEATURE_ERR = Joiner.on(", ");

  private final FeatureRegistry registry;
  private final JsCompiler compiler;

  @Inject
  public GetJsContentProcessor(
      FeatureRegistry registry,
      JsCompiler compiler) {
    this.registry = registry;
    this.compiler = compiler;
  }

  public boolean process(JsRequest request, JsResponseBuilder builder) throws JsException {
    // Get JavaScript content from features aliases request.
    JsUri jsUri = request.getJsUri();
    GadgetContext ctx = new JsGadgetContext(jsUri);

    List<FeatureBundle> requestedBundles = getAllBundles(ctx, jsUri.getLibs(), false);
    List<FeatureBundle> loadedBundles = getAllBundles(ctx, jsUri.getLoadedLibs(), true);

    Set<String> loadedFeatures = Sets.newHashSet();
    for (FeatureBundle bundle : loadedBundles) {
      loadedFeatures.add(bundle.getName());
      builder.appendExterns(bundle.getApis(ApiDirective.Type.JS, true));
      builder.appendExterns(bundle.getApis(ApiDirective.Type.JS, false));
    }

    // Collate all JS desired for the current request.
    boolean isProxyCacheable = true;

    for (FeatureBundle bundle : requestedBundles) {
      // Exclude all transitively-dependent loaded features.
      if (loadedFeatures.contains(bundle.getName())) {
        continue;
      }
      builder.appendAllJs(compiler.getJsContent(jsUri, bundle));
      for (FeatureResource featureResource : bundle.getResources()) {
        isProxyCacheable = isProxyCacheable && featureResource.isProxyCacheable();
      }
    }

    builder.setProxyCacheable(isProxyCacheable);
    setResponseCacheTtl(builder, jsUri.getStatus());
    return true;
  }

  private List<FeatureBundle> getAllBundles(GadgetContext ctx, Collection<String> requested,
      boolean loaded) throws JsException {
    List<String> unsupported = Lists.newLinkedList();
    FeatureRegistry.LookupResult lookup = registry.getFeatureResources(ctx, requested, unsupported);
    if (!unsupported.isEmpty()) {
      String message = loaded ? "loaded" : "requested";
      throw new JsException(HttpResponse.SC_BAD_REQUEST,
          "Unknown " + message + " feature(s): " + UNKNOWN_FEATURE_ERR.join(unsupported));
    }
    return lookup.getBundles();
  }

  /**
   * Sets the cache TTL depending on the value of the {@link UriStatus} object.
   *
   * @param builder The {@link JsResponseBuilder} object.
   * @param vstatus The {@link UriStatus} object.
   */
  protected void setResponseCacheTtl(JsResponseBuilder builder, UriStatus vstatus) {
    switch (vstatus) {
      case VALID_VERSIONED:
        // Versioned files get cached indefinitely
        builder.setCacheTtlSecs(-1);
        break;
      case VALID_UNVERSIONED:
        // Unversioned files get cached for 1 hour.
        builder.setCacheTtlSecs(60 * 60);
        break;
      case INVALID_VERSION:
        // URL is invalid in some way, likely version mismatch.
        // Indicate no-cache forcing subsequent requests to regenerate URLs.
        builder.setCacheTtlSecs(0);
        break;
    }
  }
}
