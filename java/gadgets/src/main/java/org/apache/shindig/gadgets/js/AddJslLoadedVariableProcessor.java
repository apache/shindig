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

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.features.FeatureRegistry;
import org.apache.shindig.gadgets.features.FeatureRegistryProvider;
import org.apache.shindig.gadgets.uri.JsUriManager.JsUri;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

/**
 * Injects a global ___jsl.l variable with information about the JS request.
 *
 * Used when loading embedded JS configuration in core.config/config.js.
 */
public class AddJslLoadedVariableProcessor implements JsProcessor {
  private static final Logger LOG = Logger.getLogger(AddJslLoadedVariableProcessor.class.getName());
  private static final String CODE_ID = "[jsload-loaded-info]";

  @VisibleForTesting
  static final String TEMPLATE =
      "window['___jsl']['l'] = (window['___jsl']['l'] || []).concat(%s);";

  private final FeatureRegistryProvider featureRegistryProvider;

  @Inject
  public AddJslLoadedVariableProcessor(FeatureRegistryProvider featureRegistryProvider) {
    this.featureRegistryProvider = featureRegistryProvider;
  }

  public boolean process(JsRequest jsRequest, JsResponseBuilder builder) throws JsException {
    JsUri jsUri = jsRequest.getJsUri();

    FeatureRegistry registry = null;
    String repository = jsUri.getRepository();
    try {
      registry = featureRegistryProvider.get(jsUri.getRepository());
    } catch (GadgetException e) {
      if (LOG.isLoggable(Level.WARNING)) {
        LOG.log(Level.WARNING, "No registry found for repository: " + repository, e);
      }
    }

    if (registry != null && !jsUri.isNohint()) {
      Set<String> allfeatures = registry.getAllFeatureNames();

      Set<String> libs = Sets.newTreeSet();
      libs.addAll(jsUri.getLibs());
      libs.removeAll(jsUri.getLoadedLibs());
      libs.retainAll(allfeatures);

      String array = toArrayString(libs);
      builder.appendJs(String.format(TEMPLATE, array), CODE_ID, true);
    }
    return true;
  }

  private String toArrayString(Set<String> bundles) {
    StringBuilder builder = new StringBuilder();
    for (String bundle : bundles) {
      if (builder.length() > 0) builder.append(',');
      builder.append('\'').append(StringEscapeUtils.escapeEcmaScript(bundle)).append('\'');
    }
    return '[' + builder.toString() + ']';
  }
}
