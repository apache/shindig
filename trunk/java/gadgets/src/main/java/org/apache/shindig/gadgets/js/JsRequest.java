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

import org.apache.shindig.gadgets.features.FeatureRegistry;
import org.apache.shindig.gadgets.uri.JsUriManager.JsUri;

import com.google.common.collect.Lists;

import java.util.Iterator;
import java.util.List;

/**
 * Data about a JavaScript request.
 *
 * This class is instantiated via {@link JsRequestBuilder}.
 */
public class JsRequest {

  private final JsUri jsUri;
  private final String host;
  private final boolean inCache;
  private final FeatureRegistry registry;
  private List<String> allFeatures;
  private List<String> newFeatures;
  private List<String> loadedFeatures;

  JsRequest(JsUri jsUri, String host, boolean inCache, FeatureRegistry registry) {
    this.jsUri = jsUri;
    this.host = host;
    this.inCache = inCache;
    this.registry = registry;
  }

  /**
   * @return this request's {@link JsUri}.
   */
  public JsUri getJsUri() {
    return jsUri;
  }

  /**
   * @return the host this request was directed to.
   */
  public String getHost() {
    return host;
  }

  /**
   * @return whether the client has this JS code in the cache.
   */
  public boolean isInCache() {
    return inCache;
  }

  /**
   * @return All features encapsulated by this request, including deps, in dep order.
   */
  public List<String> getAllFeatures() {
    initFeaturesLists();
    return allFeatures;
  }

  /**
   * @return Features to be newly returned by this request (all - loaded), in dep order.
   */
  public List<String> getNewFeatures() {
    initFeaturesLists();
    return newFeatures;
  }

  /**
   * @return Full list of all features previously loaded before this request, in dep order.
   */
  public List<String> getLoadedFeatures() {
    initFeaturesLists();
    return loadedFeatures;
  }

  private void initFeaturesLists() {
    if (allFeatures == null) {
      // Lazy-initialize these, to avoid computation where not needed.
      allFeatures = registry.getFeatures(jsUri.getLibs());
      loadedFeatures = registry.getFeatures(jsUri.getLoadedLibs());
      newFeatures = Lists.newLinkedList();
      for (String candidate : allFeatures) {
        if (!loadedFeatures.contains(candidate)) {
          newFeatures.add(candidate);
        }
      }
    }
  }
}
