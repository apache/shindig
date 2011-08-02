/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.gadgets.js;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Provider;

import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.features.FeatureRegistry;
import org.apache.shindig.gadgets.features.FeatureRegistry.FeatureBundle;
import org.apache.shindig.gadgets.features.FeatureRegistry.LookupResult;
import org.apache.shindig.gadgets.features.FeatureRegistryProvider;
import org.apache.shindig.gadgets.uri.JsUriManager.JsUri;

public class DeferJsProcessor extends ExportJsProcessor {

  @Inject
  public DeferJsProcessor(FeatureRegistryProvider featureRegistryProvider,
      Provider<GadgetContext> context) {
    super(featureRegistryProvider, context);
  }

  @Override
  public boolean process(JsRequest jsRequest, JsResponseBuilder builder) throws JsException {
    JsUri jsUri = jsRequest.getJsUri();
    ImmutableList.Builder<JsContent> resp = ImmutableList.builder();

    FeatureRegistry featureRegistry;
    try {
      featureRegistry = featureRegistryProvider.get(jsUri.getRepository());
    } catch (GadgetException e) {
      throw new JsException(e.getHttpStatusCode(), e.getMessage());
    }

    boolean neededExportJs = false;
    FeatureBundle last = null;
    if (jsUri.isJsload()) {
      // append all exports for deferred symbols
      neededExportJs = appendExportJsStatementsDeferred(featureRegistry, resp, jsRequest);
    }
    
    builder.clearJs();
    if (neededExportJs) {
      builder.appendAllJs(getExportJsContents(featureRegistry));
    }
    builder.appendAllJs(resp.build());
    return true;
  }

  private boolean appendExportJsStatementsDeferred(FeatureRegistry registry,
      ImmutableList.Builder<JsContent> builder, JsRequest jsRequest) {
    LookupResult lookup = registry.getFeatureResources(context.get(),
        jsRequest.getNewFeatures(), null, false);
    
    boolean neededExports = false;
    for (FeatureBundle bundle : lookup.getBundles()) {
      if (bundle.isSupportDefer()) {
        neededExports |= appendExportJsStatementsForFeature(builder, jsRequest.getJsUri(), bundle);
      }
    }
    
    return neededExports;
  }
}
