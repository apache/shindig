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
package org.apache.shindig.gadgets.js;

import java.util.List;
import java.util.Set;

import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.features.ApiDirective;
import org.apache.shindig.gadgets.features.FeatureRegistry;
import org.apache.shindig.gadgets.features.FeatureRegistry.FeatureBundle;
import org.apache.shindig.gadgets.features.FeatureRegistryProvider;
import org.apache.shindig.gadgets.features.FeatureResource;
import org.apache.shindig.gadgets.rewrite.js.JsCompiler;
import org.apache.shindig.gadgets.uri.JsUriManager.JsUri;
import org.apache.shindig.gadgets.uri.UriStatus;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * Retrieves the requested Javascript code using a {@link JsProcessor}.
 */
public class GetJsContentProcessor implements JsProcessor {
  public static final int DEFAULT_VERSIONED_MAXAGE = -1;
  public static final int DEFAULT_UNVERSIONED_MAXAGE = 3600;
  public static final int DEFAULT_INVALID_MAXAGE = 0;

  private final FeatureRegistryProvider registryProvider;
  private final JsCompiler compiler;

  private int versionedMaxAge = DEFAULT_VERSIONED_MAXAGE;
  private int unversionedMaxAge = DEFAULT_UNVERSIONED_MAXAGE;
  private int invalidMaxAge = DEFAULT_INVALID_MAXAGE;

  @Inject
  public GetJsContentProcessor(
      FeatureRegistryProvider registryProvider,
      JsCompiler compiler) {
    this.registryProvider = registryProvider;
    this.compiler = compiler;
  }

  @Inject(optional=true)
  public void setVersionedMaxAge(@Named("shindig.jscontent.versioned.maxage") Integer maxAge) {
    if (maxAge != null) {
      versionedMaxAge = maxAge;
    }
  }
  @Inject(optional=true)
  public void setUnversionedMaxAge(@Named("shindig.jscontent.unversioned.maxage") Integer maxAge) {
    if (maxAge != null) {
      unversionedMaxAge = maxAge;
    }
  }
  @Inject(optional=true)
  public void setInvalidMaxAge(@Named("shindig.jscontent.invalid.maxage") Integer maxAge) {
    if (maxAge != null) {
      invalidMaxAge = maxAge;
    }
  }

  public boolean process(JsRequest request, JsResponseBuilder builder) throws JsException {
    // Get JavaScript content from features aliases request.
    JsUri jsUri = request.getJsUri();
    GadgetContext ctx = new JsGadgetContext(jsUri);

    FeatureRegistry registry;
    try {
      registry = registryProvider.get(jsUri.getRepository());
    } catch (GadgetException e) {
      throw new JsException(e.getHttpStatusCode(), e.getMessage());
    }

    // TODO: possibly warn on unknown/unrecognized libs.
    List<FeatureBundle> requestedBundles = registry.getFeatureResources(
        ctx, jsUri.getLibs(), null).getBundles();
    List<FeatureBundle> loadedBundles = registry.getFeatureResources(
        ctx, jsUri.getLoadedLibs(), null).getBundles();

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
    UriStatus uriStatus = jsUri.getStatus();
    setResponseCacheTtl(builder, uriStatus != null ? uriStatus : UriStatus.VALID_UNVERSIONED);
    return true;
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
        builder.setCacheTtlSecs(versionedMaxAge);
        break;
      case VALID_UNVERSIONED:
        builder.setCacheTtlSecs(unversionedMaxAge);
        break;
      case INVALID_VERSION:
        // URL is invalid in some way, likely version mismatch.
        builder.setCacheTtlSecs(invalidMaxAge);
        break;
    }
  }
}
