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
package org.apache.shindig.gadgets.uri;

import org.apache.shindig.common.util.HashUtil;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.RenderingContext;
import org.apache.shindig.gadgets.features.FeatureRegistry;
import org.apache.shindig.gadgets.features.FeatureResource;
import org.apache.shindig.gadgets.uri.JsUriManager.JsUri;
import org.apache.shindig.gadgets.uri.JsUriManager.Versioner;

import com.google.common.collect.Maps;
import com.google.inject.Inject;

import java.util.List;
import java.util.Map;

/**
 * Straightforward versioner for collections of requested features to extern.
 * This implementation covers non-dynamic JS use cases pretty well, so it's set
 * as the default implementation for the system.
 */
public class DefaultJsVersioner implements Versioner {
  private final FeatureRegistry registry;
  private final Map<List<FeatureResource>, String> versionCache;

  @Inject
  public DefaultJsVersioner(FeatureRegistry registry) {
    this.registry = registry;
    this.versionCache = Maps.newHashMap();
  }

  public String version(final JsUri jsUri) {
    GadgetContext ctx = new GadgetContext() {
      @Override
      public String getContainer() {
        return jsUri.getContainer();
      }

      @Override
      public RenderingContext getRenderingContext() {
        return jsUri.getContext();
      }
    };

    // Registry itself will cache these requests.
    List<FeatureResource> resources =
        registry.getFeatureResources(ctx, jsUri.getLibs(), null).getResources();
    if (versionCache.containsKey(resources)) {
      return versionCache.get(resources);
    }

    StringBuilder jsBuf = new StringBuilder();
    for (FeatureResource resource : resources) {
      jsBuf.append(resource.getContent()).append(resource.getDebugContent());
    }

    String checksum = HashUtil.checksum(jsBuf.toString().getBytes());
    versionCache.put(resources, checksum);
    return checksum;
  }

  public UriStatus validate(JsUri jsUri, String version) {
    if (version == null || version.length() == 0) {
      return UriStatus.VALID_UNVERSIONED;
    }

    // Punt up to version(), utilizing its cache.
    String expectedVersion = version(jsUri);
    if (version.equals(expectedVersion)) {
      return UriStatus.VALID_VERSIONED;
    }

    return UriStatus.INVALID_VERSION;
  }

}
