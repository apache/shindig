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
  private JsLibraryFeature feature;

  public GadgetFeature create() {
    return feature;
  }

  /**
   * @param context
   * @return A list of all JS libraries used by this feature
   */
  public List<JsLibrary> getLibraries(RenderingContext context) {
    return context == RenderingContext.GADGET ?
        feature.gadgetLibraries :
        feature.containerLibraries;
  }

  public JsLibraryFeatureFactory(JsLibrary gadgetLibrary,
                                 JsLibrary containerLibrary) {
    this.feature = new JsLibraryFeature(gadgetLibrary, containerLibrary);
  }
  public JsLibraryFeatureFactory(List<JsLibrary> gadgetLibraries,
                                 List<JsLibrary> containerLibraries) {
    this.feature = new JsLibraryFeature(gadgetLibraries, containerLibraries);
  }
}

class JsLibraryFeature implements GadgetFeature {
  List<JsLibrary> containerLibraries;
  List<JsLibrary> gadgetLibraries;

  /**
   * Creates a JsLibraryFeature with a single library for gadget & container.
   *
   * @param gadgetLibrary The library for the gadget, may be null.
   * @param containerLibrary The library for the container, may be null.
   */
  public JsLibraryFeature(JsLibrary gadgetLibrary, JsLibrary containerLibrary) {
    if (gadgetLibrary == null) {
      gadgetLibraries = Collections.emptyList();
    } else {
      gadgetLibraries = new LinkedList<JsLibrary>();
      gadgetLibraries.add(gadgetLibrary);
      gadgetLibraries = Collections.unmodifiableList(gadgetLibraries);
    }

    if (containerLibrary == null) {
      containerLibraries = Collections.emptyList();
    } else {
      containerLibraries = new LinkedList<JsLibrary>();
      containerLibraries.add(containerLibrary);
      containerLibraries = Collections.unmodifiableList(containerLibraries);
    }
  }

  /**
   * Creates a JsLibraryFeature with multiple gadget and/or container libraries.
   * @param gLibraries Libraries to serve for the gadget.
   * @param cLibraries Libraries to serve for the container.
   */
  public JsLibraryFeature(List<JsLibrary> gLibraries,
                          List<JsLibrary> cLibraries) {
    if (gLibraries == null) {
      gadgetLibraries = Collections.emptyList();
    } else {
      gadgetLibraries = new LinkedList<JsLibrary>();
      gadgetLibraries.addAll(gLibraries);
      gadgetLibraries = Collections.unmodifiableList(gadgetLibraries);
    }

    if (cLibraries == null) {
      containerLibraries = Collections.emptyList();
    } else {
      containerLibraries = new LinkedList<JsLibrary>();
      containerLibraries.addAll(cLibraries);
      containerLibraries = Collections.unmodifiableList(containerLibraries);
    }
  }

  /**
   * {@inheritDoc}
   */
  public void prepare(GadgetView gadget, GadgetContext context,
      Map<String, String> params) {
    // Do nothing.
  }

  /**
   * {@inheritDoc}
   */
  public void process(Gadget gadget, GadgetContext context,
      Map<String, String> params) {
    List<JsLibrary> libraries;
    if (context.getRenderingContext() == RenderingContext.GADGET) {
      libraries = gadgetLibraries;
    } else {
      libraries = containerLibraries;
    }
    for (JsLibrary library : libraries) {
      gadget.addJsLibrary(library);
    }
  }
}