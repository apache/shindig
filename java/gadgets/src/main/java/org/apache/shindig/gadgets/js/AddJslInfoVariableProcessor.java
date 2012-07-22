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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.features.FeatureRegistry;
import org.apache.shindig.gadgets.features.FeatureRegistryProvider;
import org.apache.shindig.gadgets.uri.JsUriManager.JsUri;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

/**
 * Injects a global ___jsl variable with information about the JS request.
 *
 * Used when loading embedded JS configuration in core.config/config.js.
 */
public class AddJslInfoVariableProcessor implements JsProcessor {
  private static final Logger LOG = Logger.getLogger(AddJslInfoVariableProcessor.class.getName());
  private static final String CODE_ID = "[jsload-code-info]";

  @VisibleForTesting
  static final String BASE_HINT_TEMPLATE = "window['___jsl'] = window['___jsl'] || {};";

  @VisibleForTesting
  static final String FEATURES_HINT_TEMPLATE = "window['___jsl']['f'] = [%s];";

  private final FeatureRegistryProvider featureRegistryProvider;

  @Inject
  public AddJslInfoVariableProcessor(FeatureRegistryProvider featureRegistryProvider) {
    this.featureRegistryProvider = featureRegistryProvider;
  }

  public boolean process(JsRequest jsRequest, JsResponseBuilder builder) {
    JsUri jsUri = jsRequest.getJsUri();
    if (!jsUri.isNohint()) {
      String features = getFeatures(jsUri);
      builder.prependJs(String.format(FEATURES_HINT_TEMPLATE, features), CODE_ID, true);
      builder.prependJs(BASE_HINT_TEMPLATE, CODE_ID);
    }
    return true;
  }

  private String getFeatures(JsUri jsUri) {
    FeatureRegistry registry = null;
    String repository = jsUri.getRepository();
    try {
      registry = featureRegistryProvider.get(repository);
    } catch (GadgetException e) {
      if (LOG.isLoggable(Level.WARNING)) {
        LOG.log(Level.WARNING, "No registry found for repository: " + repository, e);
      }
    }

    if (registry != null) {
      List<String> features = registry.getFeatures(jsUri.getLibs());
      Set<String> encoded = Sets.newTreeSet();
      for (String feature : features) {
        encoded.add('\'' + StringEscapeUtils.escapeEcmaScript(feature) + '\'');
      }

      return StringUtils.join(encoded, ",");
    }

    return "";
  }
}
