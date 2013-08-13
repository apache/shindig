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
package org.apache.shindig.gadgets.rewrite.js;

import com.google.inject.ImplementedBy;

import org.apache.shindig.gadgets.features.FeatureRegistry.FeatureBundle;
import org.apache.shindig.gadgets.js.JsContent;
import org.apache.shindig.gadgets.js.JsResponse;
import org.apache.shindig.gadgets.uri.JsUriManager.JsUri;

/**
 * Compiler to pre-process each feature independently and compile a
 * concatenation of pre-processed data.
 */
@ImplementedBy(DefaultJsCompiler.class)
public interface JsCompiler {

  /**
   * Pre-process feature JS.
   * @param jsUri The JS uri making the request.
   * @param bundle The feature bundle.
   * @return Processed feature JS.
   */
  Iterable<JsContent> getJsContent(JsUri jsUri, FeatureBundle bundle);

  /**
   * Compiles the provided code with the provided list of external symbols.
   * @param jsUri The JS uri making the request.
   * @param content The raw/pre-processed JS code.
   * @param externs The externs.
   * @return A compilation result object.
   */
  JsResponse compile(JsUri jsUri, Iterable<JsContent> content, String externs);
}
