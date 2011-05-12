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

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import org.apache.shindig.gadgets.features.FeatureRegistry.FeatureBundle;
import org.apache.shindig.gadgets.features.FeatureResource;

/**
 * Wrapper around JavaScript providing a way to track its provenance.
 * Other metadata may be added as well, such as annotations regarding compilation,
 * obfuscation, and so on.
 */
public class JsContent {
  private final String content;
  private final String source;
  private final FeatureBundle bundle;
  private final FeatureResource resource;
  
  public static JsContent fromText(String content, String source) {
    return new JsContent(content, source, null, null);
  }

  public static JsContent fromFeature(String content, String source,
      FeatureBundle bundle, FeatureResource resource) {
    return new JsContent(content, source, bundle, resource);
  }
  
  private JsContent(String content, String source,
      FeatureBundle bundle, FeatureResource resource) {
    this.content = content;
    this.source = source;
    this.bundle = bundle;
    this.resource = resource;
  }

  public String get() {
    return content;
  }

  public String getSource() {
    return source;
  }

  public FeatureBundle getFeatureBundle() {
    return bundle;
  }
  
  public FeatureResource getFeatureResource() {
    return resource;
  }
}
