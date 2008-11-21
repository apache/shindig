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

import org.apache.shindig.common.ContainerConfig;

import com.google.common.collect.Maps;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
/**
 * Represents a feature available to gadget authors.
 *
 * Features are registered declaratively in feature.xml files and loaded at
 * server startup time.
 *
 * Some features may require server-side functionality. These features are
 * triggered at different points throughout the code.
 */
public class GadgetFeature {

  private final String name;
  private final Map<RenderingContext, Map<String, List<JsLibrary>>> libraries;
  private final Collection<String> dependencies;

  /**
   * @return The name of this feature.
   */
  public String getName() {
    return name;
  }

  /**
   * @return All dependencies of this feature.
   */
  public Collection<String> getDependencies() {
    return dependencies;
  }

  /**
   * Adds a new dependency to the graph.
   */
  public void addDependency(String dependency) {
    synchronized (dependencies) {
      dependencies.add(dependency);
    }
  }

  /**
   * Adds multiple new dependencies to the graph.
   */
  public void addDependencies(Collection<String> dependencies) {
    synchronized (this.dependencies) {
      this.dependencies.addAll(dependencies);
    }
  }

  /**
   * Provides javavscript libraries needed to satisfy the requirements for this
   * feature.
   *
   * @param context The context in which the gadget is being used.
   * @param container The container to get libraries for.
   * @return The collection of libraries needed for the provided context.
   */
  public List<JsLibrary> getJsLibraries(RenderingContext context, String container) {
    List<JsLibrary> libs = null;

    if (context == null) {
      // For this special case we return all JS libraries in a single list.
      // This is usually only used for debugging or at startup, so it's ok
      // that we're creating new objects every time.
      libs = new LinkedList<JsLibrary>();
      for (Map<String, List<JsLibrary>> ctx : libraries.values()) {
        for (List<JsLibrary> lib : ctx.values()) {
          libs.addAll(lib);
        }
      }
    } else {
      Map<String, List<JsLibrary>> contextLibs = libraries.get(context);
      if (contextLibs != null) {
        libs = contextLibs.get(container);
        if (libs == null) {
          // Try default.
          libs = contextLibs.get(ContainerConfig.DEFAULT_CONTAINER);
        }
      }
    }

    if (libs == null) {
      return Collections.emptyList();
    }
    return libs;
  }

  /**
   * Simplified ctor that registers a set of libraries for all contexts and
   * the default container. Used for testing.
   */
  public GadgetFeature(String name, List<JsLibrary> libraries,
      Collection<String> dependencies) {
    this.name = name;
    this.libraries = Maps.newEnumMap(RenderingContext.class);
    for (RenderingContext context : RenderingContext.values()) {
      Map<String, List<JsLibrary>> container = Maps.newHashMap();
      container.put(ContainerConfig.DEFAULT_CONTAINER, libraries);
      this.libraries.put(context, container);
    }
    this.dependencies = dependencies;
  }

  public GadgetFeature(String name,
      Map<RenderingContext, Map<String, List<JsLibrary>>> libraries,
      Collection<String> dependencies) {
    this.name = name;
    this.libraries = libraries;
    this.dependencies = dependencies;
  }
}