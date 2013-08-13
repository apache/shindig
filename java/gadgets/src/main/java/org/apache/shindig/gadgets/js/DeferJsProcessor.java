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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Provider;

import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.features.FeatureRegistry;
import org.apache.shindig.gadgets.features.FeatureRegistry.FeatureBundle;
import org.apache.shindig.gadgets.features.FeatureRegistry.LookupResult;
import org.apache.shindig.gadgets.features.FeatureRegistryProvider;
import org.apache.shindig.gadgets.uri.JsUriManager.JsUri;

import java.util.List;

public class DeferJsProcessor extends BaseSurfaceJsProcessor implements JsProcessor {

  @VisibleForTesting
  static final String FEATURE_NAME = "deferjs";

  private static final String FUNCTION_NAME = "deferJs";

  @Inject
  public DeferJsProcessor(FeatureRegistryProvider featureRegistryProvider,
      Provider<GadgetContext> context) {
    super(featureRegistryProvider, context);
  }

  public boolean process(JsRequest jsRequest, JsResponseBuilder builder) throws JsException {
    JsUri jsUri = jsRequest.getJsUri();
    ImmutableList.Builder<JsContent> resp = ImmutableList.builder();
    FeatureRegistry featureRegistry = getFeatureRegistry(jsUri);

    boolean needDefers = false;
    if (jsUri.isJsload()) {
      // append all exports for deferred symbols
      List<FeatureBundle> bundles = getSupportDeferBundles(featureRegistry, jsRequest);
      for (FeatureBundle bundle : bundles) {
        needDefers |= appendDeferJsStatements(resp, jsRequest.getJsUri(), bundle);
      }
    }

    // TODO: Instead of clearing, do a replacement of feature impl with defer stubs.
    // Clearing has an effect of ignoring previous processors work.
    if (needDefers) {
      builder.appendAllJs(getSurfaceJsContents(featureRegistry, FEATURE_NAME));
    }
    builder.appendAllJs(resp.build());
    return true;
  }

  private boolean appendDeferJsStatements(ImmutableList.Builder<JsContent> builder,
       JsUri jsUri, FeatureBundle bundle) {
    List<String> exports = getExports(bundle, jsUri);
    if (!exports.isEmpty()) {
      StringBuilder sb = new StringBuilder();
      for (Input input : generateInputs(exports)) {
        sb.append(toDeferStatement(input));
      }
      builder.add(JsContent.fromFeature(sb.toString(), "[generated-symbol-exports]",
          bundle, null));
      return true;
    }
    return false;
  }

  private String toDeferStatement(Input input) {
    StringBuilder result = new StringBuilder();

    // Local namespace.
    if (input.namespace != null) {
      result.append(FUNCTION_NAME).append("('").append(input.namespace).append("',[");
      for (int i = 0; i < input.properties.size(); i++) {
        String prop = input.properties.get(i);
        if (i > 0) result.append(',');
        result.append('\'').append(prop).append('\'');
      }
      result.append("]);");

    // Global/window namespace.
    } else {
      for (String prop : input.properties) {
        result.append(FUNCTION_NAME).append("('").append(prop).append("');");
      }
    }
    return result.toString();
  }

  private List<FeatureBundle> getSupportDeferBundles(FeatureRegistry registry, JsRequest jsRequest) {
    List<FeatureBundle> result = Lists.newArrayList();
    LookupResult lookup = registry.getFeatureResources(context.get(),
      jsRequest.getNewFeatures(), null, false);
    for (FeatureBundle bundle : lookup.getBundles()) {
      if (bundle.isSupportDefer()) {
        result.add(bundle);
      }
    }
    return result;
  }
}
