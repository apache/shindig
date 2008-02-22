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
package org.apache.shindig.gadgets;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Enables basic JsLibrary loading without having to define new classes.
 *
 * Usage:
 * JsLibrary.register(...);
 * JsLibrary mylib = JsLibrary.registered("name", "name");
 * GadgetFeatureRegistry.register("name", null,
 *     new JsLibraryFeatureFactory(mylib));
 */
public class JsLibraryFeatureFactory implements GadgetFeatureFactory {
  private final JsLibraryFeature feature;

  public GadgetFeature create() {
    return feature;
  }

  /**
   * @param libraries The libraries to serve when this feature is used.
   */
  public JsLibraryFeatureFactory(
      Map<RenderingContext, Map<String, List<JsLibrary>>> libraries) {
    this.feature = new JsLibraryFeature(libraries);
  }
  protected JsLibraryFeatureFactory() {
    feature = null;
  }
}

class JsLibraryFeature extends GadgetFeature {
  final Map<RenderingContext, Map<String, List<JsLibrary>>> libraries;

  /**
   * Creates a JsLibraryFeature with multiple gadget and/or container libraries.
   * @param libraries
   */
  public JsLibraryFeature(
      Map<RenderingContext, Map<String, List<JsLibrary>>> libraries) {
    // TODO: technically we should copy this, but since currently the callers
    // always pass us something that won't be modified anyway, we're safe.
    // Copying this structure is painful.
    this.libraries = Collections.unmodifiableMap(libraries);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<JsLibrary> getJsLibraries(RenderingContext context,
                                        ProcessingOptions options) {
    List<JsLibrary> libs = null;

    if (context == null || options == null) {
      // for this special case we return all JS libraries in a single list.
      libs = new LinkedList<JsLibrary>();
      for (Map.Entry<RenderingContext, Map<String, List<JsLibrary>>> i :
          libraries.entrySet()) {
        for (Map.Entry<String, List<JsLibrary>> e : i.getValue().entrySet()) {
          libs.addAll(e.getValue());
        }
      }
    } else {
      Map<String, List<JsLibrary>> contextLibs = libraries.get(context);
      if (contextLibs != null) {
        libs = contextLibs.get(options.getSyndicator());
        if (libs == null) {
          // Try default.
          libs = contextLibs.get(SyndicatorConfig.DEFAULT_SYNDICATOR);
        }
      }
    }

    if (libs == null) {
      return Collections.emptyList();
    }
    return libs;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void process(Gadget gadget, GadgetContext context,
      Map<String, String> params) throws GadgetException {
    super.process(gadget, context, params);
    for (JsLibrary library : getJsLibraries(context.getRenderingContext(),
                                            context.getOptions())) {
      gadget.addJsLibrary(library);
    }
  }
}