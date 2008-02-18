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

package org.apache.shindig.gadgets;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implements the opensocial-0.6 and 0.7 features
 * DO NOT EMULATE THIS BEHAVIOR. This is a kludge to work around the need
 * for every open social container to have their own javascript file.
 */
public class OpenSocialFeatureFactory extends JsLibraryFeatureFactory {
  private final OpenSocialFeature feature;

  /**
   * {@inheritDoc}
   */
  @Override
  public GadgetFeature create() {
    return feature;
  }

  /**
   * Registers the opensocial feature to the given registry.
   *
   * This expects that syndicators are configured with an
   * "opensocial.0.7.location" parameter that points to the location of their
   * open social file.
   *
   * @param registry
   * @param config
   */
  public static void register(GadgetFeatureRegistry registry,
                              SyndicatorConfig config) {
    Map<String, List<JsLibrary>> libs = new HashMap<String, List<JsLibrary>>();
    for (String syndicator : config.getSyndicators()) {
      String js = config.get(syndicator, "opensocial.0.7.location");
      if (js != null && js != "null") {
        JsLibrary.Type type;
        String content;
        if (js.startsWith("http://")) {
          type = JsLibrary.Type.URL;
          content = js;
        } else if (js.startsWith("//")) {
          type = JsLibrary.Type.URL;
          content = js.substring(1);
        } else if (js.startsWith("res://")) {
          type = JsLibrary.Type.RESOURCE;
          content = js.substring(6);
        } else {
          // Assume a resource path; inline js isn't allowed.
          type = JsLibrary.Type.RESOURCE;
          content = js;
        }
        JsLibrary lib = JsLibrary.create(type, content);
        libs.put(syndicator, Arrays.asList(lib));
      }
    }

    // Only register if we have at least one registered syndicator with the
    // feature.
    if (libs.size() > 0) {
      OpenSocialFeature newFeature = new OpenSocialFeature(libs);
      registry.register("opensocial-0.7",
                        Arrays.asList("opensocial-reference"),
                        new OpenSocialFeatureFactory(newFeature));
    }
  }

  private OpenSocialFeatureFactory(OpenSocialFeature feature) {
    this.feature = feature;
  }
}

class OpenSocialFeature extends GadgetFeature {
  final Map<String, List<JsLibrary>> libs;

  /**
   * @param context
   * @return A list of all JS libraries used by this feature
   */
  @Override
  public List<JsLibrary> getJsLibraries(RenderingContext context,
                                        ProcessingOptions options) {
    if (context == RenderingContext.GADGET && options != null) {
      String synd = options.getSyndicator();
      List<JsLibrary> libraries = libs.get(synd);
      if (libraries != null) {
        return libraries;
      }
    }
    return Collections.emptyList();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void process(Gadget gadget, GadgetContext context,
                      Map<String, String> params) {
    ProcessingOptions options = context.getOptions();
    String synd = options.getSyndicator();
    List<JsLibrary> libraries = libs.get(synd);
    if (libraries != null) {
      for (JsLibrary library : libraries) {
        gadget.addJsLibrary(library);
      }
    }
  }

  /**
   * @param libs Maps syndicator to libs files. Only gadget-side since
   *   currently there is no container side OS code.
   */
  public OpenSocialFeature(Map<String, List<JsLibrary>> libs) {
    this.libs = libs;
  }
}
