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

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.features.FeatureRegistry;
import org.apache.shindig.gadgets.spec.Feature;
import org.apache.shindig.gadgets.spec.ModulePrefs;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Populates the core.util configuration, which at present includes the list
 * of features that are supported.
 *
 * @since 2.0.0
 */
@Singleton
public class CoreUtilConfigContributor implements ConfigContributor {
  private final FeatureRegistry registry;

  @Inject
  public CoreUtilConfigContributor(final FeatureRegistry registry) {
    this.registry = registry;
  }


  /** {@inheritDoc} */
  public void contribute(Map<String, Object> config, Gadget gadget) {
    // Add gadgets.util support. This is calculated dynamically based on request inputs.
    ModulePrefs prefs = gadget.getSpec().getModulePrefs();
    Collection<Feature> features = prefs.getFeatures().values();
    Map<String, Map<String, Object>> featureMap = Maps.newHashMapWithExpectedSize(features.size());
    Set<String> allFeatureNames = registry.getAllFeatureNames();

    for (Feature feature : features) {
      // Skip unregistered features
      if (!allFeatureNames.contains(feature.getName())) {
        continue;
      }
      // Flatten out the multimap a bit for backwards compatibility:  map keys
      // with just 1 value into the string, treat others as arrays
      Map<String, Object> paramFeaturesInConfig = Maps.newHashMap();
      for (String paramName : feature.getParams().keySet()) {
        Collection<String> paramValues = feature.getParams().get(paramName);
        if (paramValues.size() == 1) {
          paramFeaturesInConfig.put(paramName, paramValues.iterator().next());
        } else {
          paramFeaturesInConfig.put(paramName, paramValues);
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
