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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

import java.util.List;
import java.util.Map;

import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.gadgets.Gadget;

public class DefaultConfigProcessor implements ConfigProcessor {
  @VisibleForTesting
  static final String GADGETS_FEATURES_KEY = "gadgets.features";

  private final Map<String, ConfigContributor> featureContributors;
  private final List<ConfigContributor> globalContributors;
  private final ContainerConfig containerConfig;

  @Inject
  public DefaultConfigProcessor(
      Map<String, ConfigContributor> featureContributors,
      ContainerConfig containerConfig) {
    this.featureContributors = featureContributors;
    this.globalContributors = Lists.newLinkedList();
    this.containerConfig = containerConfig;
  }

  @Inject(optional = true)
  public void setGlobalContributors(List<ConfigContributor> globalContribs) {
    globalContributors.addAll(globalContribs);
  }

  public Map<String, Object> getConfig(String container, List<String> features, String host,
      Gadget gadget) {
    Map<String, Object> config = Maps.newHashMap();

    // Perform global config
    for (ConfigContributor contrib : globalContributors) {
      contribute(contrib, config, container, host, gadget);
    }

    // Append some container specific things
    Map<String, Object> featureConfig = containerConfig.getMap(container, GADGETS_FEATURES_KEY);

    if (featureConfig != null) {
      // Discard what we don't care about.
      for (String name : features) {
        Object conf = featureConfig.get(name);
        // Add from containerConfig.
        if (conf != null) {
          config.put(name, conf);
        }
        contribute(featureContributors.get(name), config, container, host, gadget);
      }
    }
    return config;
  }

  private void contribute(ConfigContributor contrib, Map<String, Object> config, String container,
      String host, Gadget gadget) {
    if (contrib != null) {
      if (host != null) {
        contrib.contribute(config, container, host);
      } else if (gadget != null) {
        contrib.contribute(config, gadget);
      }
    }
  }

}
