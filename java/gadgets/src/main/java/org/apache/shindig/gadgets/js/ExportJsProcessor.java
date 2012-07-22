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
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Provider;

import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.features.FeatureRegistry;
import org.apache.shindig.gadgets.features.FeatureRegistry.FeatureBundle;
import org.apache.shindig.gadgets.features.FeatureRegistryProvider;
import org.apache.shindig.gadgets.uri.JsUriManager.JsUri;

import java.util.List;

public class ExportJsProcessor extends BaseSurfaceJsProcessor implements JsProcessor {

  @VisibleForTesting
  static final String FEATURE_NAME = "exportjs";

  private static final String FUNCTION_NAME = "exportJs";

  @Inject
  public ExportJsProcessor(FeatureRegistryProvider featureRegistryProvider,
      Provider<GadgetContext> context) {
    super(featureRegistryProvider, context);
  }

  public boolean process(JsRequest jsRequest, JsResponseBuilder builder) throws JsException {
    JsUri jsUri = jsRequest.getJsUri();
    ImmutableList.Builder<JsContent> resp = ImmutableList.builder();
    FeatureRegistry featureRegistry = getFeatureRegistry(jsUri);

    boolean needExports = false;
    FeatureBundle last = null;
    if (!jsUri.isJsload()) {
      for (JsContent jsc : builder.build().getAllJsContent()) {
        FeatureBundle current = jsc.getFeatureBundle();
        if (last != null && current != last) {
          needExports |= appendExportJsStatements(resp, jsUri, last);
        }
        resp.add(jsc);
        last = current;
      }
      if (last != null) {
        needExports |= appendExportJsStatements(resp, jsUri, last);
      }
    }

    builder.clearJs();
    if (needExports) {
      builder.appendAllJs(getSurfaceJsContents(featureRegistry, FEATURE_NAME));
    }
    builder.appendAllJs(resp.build());
    return true;
  }

  private boolean appendExportJsStatements(ImmutableList.Builder<JsContent> builder,
      JsUri jsUri, FeatureBundle bundle) {
    List<String> exports = getExports(bundle, jsUri);
    if (!exports.isEmpty()) {
      StringBuilder sb = new StringBuilder();
      for (Input input : generateInputs(exports)) {
        sb.append(toExportStatement(input));
      }
      builder.add(JsContent.fromFeature(sb.toString(), "[generated-symbol-exports]",
          bundle, null));
      return true;
    }
    return false;
  }

  private String toExportStatement(Input input) {
    StringBuilder result = new StringBuilder();

    // Local namespace.
    if (input.namespace != null) {
      result.append(FUNCTION_NAME).append("('").append(input.namespace).append("',[");
      result.append(Joiner.on(',').join(input.components));
      result.append("],{");
      for (int i = 0; i < input.properties.size(); i++) {
        String prop = input.properties.get(i);
        if (i > 0) result.append(',');
        result.append(prop).append(":'").append(prop).append('\'');
      }
      result.append("});");

    // Global/window namespace.
    } else {
      for (String prop : input.properties) {
        result.append(FUNCTION_NAME).append('(');
        result.append('\'').append(prop).append("',[");
        result.append(prop);
        result.append("]);");
      }
    }
    return result.toString();
  }
}
