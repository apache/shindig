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
package org.apache.shindig.gadgets.config;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.admin.GadgetAdminStore;
import org.apache.shindig.gadgets.features.FeatureRegistry;
import org.apache.shindig.gadgets.rewrite.TemplateRewriter;
import org.apache.shindig.gadgets.spec.Feature;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Populates the core.util configuration, which at present includes the list
 * of features that are supported.
 *
 * @since 2.0.0
 */
@Singleton
public class CoreUtilConfigContributor implements ConfigContributor {

  private final FeatureRegistry registry;
  private final GadgetAdminStore gadgetAdminStore;

  @Inject
  public CoreUtilConfigContributor(final FeatureRegistry registry,
          GadgetAdminStore gadgetAdminStore) {
    this.registry = registry;
    this.gadgetAdminStore = gadgetAdminStore;
  }

  /** {@inheritDoc} */
  public void contribute(Map<String, Object> config, Gadget gadget) {
    // Add gadgets.util support. This is calculated dynamically based on request inputs.
    Collection<Feature> features = gadget.getViewFeatures().values();
    Map<String, Map<String, Object>> featureMap = Maps.newHashMapWithExpectedSize(features.size());
    Set<String> allFeatureNames = registry.getAllFeatureNames();

    for (Feature feature : features) {
      // Skip unregistered features
      if ((!allFeatureNames.contains(feature.getName())) ||
              (!gadgetAdminStore.isAllowedFeature(feature, gadget))) {
        continue;
      }
      // Flatten out the multimap a bit for backwards compatibility:  map keys
      // with just 1 value into the string, treat others as arrays
      Map<String, Object> paramFeaturesInConfig = Maps.newHashMap();
      for (String paramName : feature.getParams().keySet()) {
        Collection<String> paramValues = feature.getParams().get(paramName);
        // Resolve the template URL to convert relative URL to absolute URL relative to gadget URL.
        if (TemplateRewriter.TEMPLATES_FEATURE_NAME.equals(feature.getName())
            && TemplateRewriter.REQUIRE_LIBRARY_PARAM.equals(paramName)) {
          if (paramValues.size() == 1) {
            Uri paramUri = Uri.parse(paramValues.iterator().next().trim());
            paramUri = gadget.getContext().getUrl().resolve(paramUri);
            paramFeaturesInConfig.put(paramName, paramUri.toString());
          } else {
            Collection<String> abReqLibs = Lists.newArrayList();
            for (String libraryUrl : paramValues) {
              Uri paramUri = Uri.parse(libraryUrl.trim());
              paramUri = gadget.getContext().getUrl().resolve(paramUri);
              abReqLibs.add(paramUri.toString());
            }
            paramFeaturesInConfig.put(paramName, abReqLibs);
          }
        } else {
          if (paramValues.size() == 1) {
            paramFeaturesInConfig.put(paramName, paramValues.iterator().next());
          } else {
            paramFeaturesInConfig.put(paramName, paramValues);
          }
        }
      }

      featureMap.put(feature.getName(), paramFeaturesInConfig);
    }
    config.put("core.util", featureMap);
  }

  /** {@inheritDoc} */
  public void contribute(Map<String,Object> config, String container, String host) {
    // not used for container configuration
  }
}
