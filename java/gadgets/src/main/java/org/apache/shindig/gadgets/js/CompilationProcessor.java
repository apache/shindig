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

import com.google.inject.Inject;

import org.apache.shindig.gadgets.features.ApiDirective;
import org.apache.shindig.gadgets.features.FeatureRegistry.FeatureBundle;
import org.apache.shindig.gadgets.rewrite.js.JsCompiler;

public class CompilationProcessor implements JsProcessor {
  private final JsCompiler compiler;

  @Inject
  public CompilationProcessor(JsCompiler compiler) {
    this.compiler = compiler;
  }

  /**
   * Compile content in the inbound JsResponseBuilder.
   * TODO: Convert JsCompiler to take JsResponseBuilder directly rather than Iterable<JsContent>
   * @throws JsException
   */
  public boolean process(JsRequest request, JsResponseBuilder builder) throws JsException {
    Iterable<JsContent> jsContents = builder.build().getAllJsContent();
    for (JsContent jsc : jsContents) {
      FeatureBundle bundle = jsc.getFeatureBundle();
      if (bundle != null) {
        builder.appendExterns(bundle.getApis(ApiDirective.Type.JS, true));
      }
    }

    JsResponse result = compiler.compile(request.getJsUri(), jsContents,
        builder.build().getExterns());

    builder.clearJs().appendAllJs(result.getAllJsContent());
    builder.setStatusCode(result.getStatusCode());
    builder.addErrors(result.getErrors());
    int refresh = result.getCacheTtlSecs();
    if (refresh > 0) {
      // Allow ttl overwrite by compiler
      builder.setCacheTtlSecs(refresh);
    }
    return true;
  }

}
