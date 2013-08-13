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

import com.google.common.collect.ImmutableList;

import org.apache.shindig.gadgets.features.FeatureRegistry.FeatureBundle;

public class SeparatorCommentingProcessor implements JsProcessor {

  public boolean process(JsRequest jsRequest, JsResponseBuilder builder) {
    ImmutableList.Builder<JsContent> jsBuilder = ImmutableList.builder();

    FeatureBundle lastFeature = null;
    for (JsContent js : builder.build().getAllJsContent()) {
      FeatureBundle feature = js.getFeatureBundle();

      // Entering a new feature, from none/text.
      if (lastFeature == null && feature != null) {
        jsBuilder.add(newComment(feature, true));

      // Entering a new feature, from another feature.
      } else if (lastFeature != null && feature != null && lastFeature != feature) {
        jsBuilder.add(newComment(lastFeature, false));
        jsBuilder.add(newComment(feature, true));

      // Entering a text, from a feature
      } else if (lastFeature != null && feature == null) {
        jsBuilder.add(newComment(lastFeature, false));
      }
      jsBuilder.add(js);
      lastFeature = feature;
    }
    // If there is a last feature.
    if (lastFeature != null) {
      jsBuilder.add(newComment(lastFeature, false));
    }
    builder.clearJs().appendAllJs(jsBuilder.build());
    return true;
  }

  private JsContent newComment(FeatureBundle bundle, boolean start) {
    String tag = start ? "start" : "end";
    return JsContent.fromFeature(
        "\n/* [" + tag + "] feature=" + bundle.getName() + " */\n",
        "[comment-marker-" + tag + ']', bundle, null);
  }

}
