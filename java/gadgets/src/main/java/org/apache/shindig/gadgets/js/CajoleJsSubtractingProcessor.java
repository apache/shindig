// Copyright 2011 Google Inc. All Rights Reserved.

package org.apache.shindig.gadgets.js;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;

import org.apache.shindig.gadgets.features.FeatureRegistry.FeatureBundle;
import org.apache.shindig.gadgets.features.FeatureResource;

import java.util.Map;

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
public class CajoleJsSubtractingProcessor implements JsProcessor {

  @VisibleForTesting
  static final String CAJOLE_ATTRIB_KEY = "cajole";

  @VisibleForTesting
  static final String CAJOLE_ATTRIB_VALUE = "1";

  public boolean process(JsRequest jsRequest, JsResponseBuilder builder) {
    if (!jsRequest.getJsUri().cajoleContent()) {
      ImmutableList.Builder<JsContent> listBuilder = ImmutableList.builder();
      for (JsContent js : builder.build().getAllJsContent()) {
        if (!isCajole(js)) {
          listBuilder.add(js);
        }
      }
      builder.clearJs().appendAllJs(listBuilder.build());
    }
    return true;
  }

  private boolean isCajole(JsContent js) {
    FeatureResource resource = js.getFeatureResource();
    if (resource != null) {
      Map<String, String> attribs = resource.getAttribs();
      if (attribs != null) {
        String attrib = attribs.get(CAJOLE_ATTRIB_KEY);
        return CAJOLE_ATTRIB_VALUE.equals(attrib);
      }
    }
    return false;
  }
}
