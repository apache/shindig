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

import com.google.common.collect.Lists;

import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.RenderingContext;
import org.apache.shindig.gadgets.features.ApiDirective;
import org.apache.shindig.gadgets.features.FeatureRegistry;
import org.apache.shindig.gadgets.features.FeatureRegistry.FeatureBundle;
import org.apache.shindig.gadgets.features.FeatureRegistry.LookupResult;
import org.apache.shindig.gadgets.rewrite.js.JsCompiler;

import com.google.inject.Inject;

import java.util.List;

public class CompilationProcessor implements JsProcessor {
  private final JsCompiler compiler;
  private final FeatureRegistry registry;
  
  @Inject
  public CompilationProcessor(JsCompiler compiler, FeatureRegistry registry) {
    this.compiler = compiler;
    this.registry = registry;
  }
      
  /**
   * Compile content in the inbound JsResponseBuilder.
   * TODO: Convert JsCompiler to take JsResponseBuilder directly rather than Iterable<JsContent>
   */
  public boolean process(JsRequest request, JsResponseBuilder builder)
      throws JsException {
    List<String> featureExterns = getFeatureExterns(builder.build().getAllJsContent(), request);
    JsResponse result = compiler.compile(request.getJsUri(),
        builder.build().getAllJsContent(), featureExterns);
    builder.clearJs().appendAllJs(result.getAllJsContent());
    return true;
  }

  private List<String> getFeatureExterns(Iterable<JsContent> content, final JsRequest request) {
    List<String> result = Lists.newArrayList();
    List<String> featureNames = Lists.newArrayList();
    for (JsContent js : content) {
      if (js.getFeature() != null) {
        featureNames.add(js.getFeature());
      }
    }
    LookupResult lookup = registry.getFeatureResources(new GadgetContext() {
      @Override
      public RenderingContext getRenderingContext() {
        return request.getJsUri().getContext();
      }
      @Override
      public String getContainer() {
        return request.getJsUri().getContainer();
      }
    }, featureNames, Lists.<String>newArrayList());
    for (FeatureBundle bundle : lookup.getBundles()) {
      result.addAll(bundle.getApis(ApiDirective.Type.JS, false));
    }
    return result;
  }
}
