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
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Provider;

import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.JsCompileMode;
import org.apache.shindig.gadgets.features.ApiDirective;
import org.apache.shindig.gadgets.features.FeatureRegistry;
import org.apache.shindig.gadgets.features.FeatureRegistry.FeatureBundle;
import org.apache.shindig.gadgets.features.FeatureRegistry.LookupResult;
import org.apache.shindig.gadgets.features.FeatureRegistryProvider;
import org.apache.shindig.gadgets.features.FeatureResource;
import org.apache.shindig.gadgets.uri.JsUriManager.JsUri;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public abstract class BaseSurfaceJsProcessor implements JsProcessor {

  protected final FeatureRegistryProvider featureRegistryProvider;
  protected final Provider<GadgetContext> context;

  protected BaseSurfaceJsProcessor(FeatureRegistryProvider featureRegistryProvider,
      Provider<GadgetContext> context) {
    this.featureRegistryProvider = featureRegistryProvider;
    this.context = context;
  }

  protected final FeatureRegistry getFeatureRegistry(JsUri jsUri) throws JsException {
    try {
      return featureRegistryProvider.get(jsUri.getRepository());
    } catch (GadgetException e) {
      throw new JsException(e.getHttpStatusCode(), e.getMessage());
    }
  }

  protected final List<String> getExports(FeatureBundle bundle, JsUri jsUri) {
    // Add exports of bundle, regardless.
    if (jsUri.getCompileMode() == JsCompileMode.CONCAT_COMPILE_EXPORT_ALL) {
      return bundle.getApis(ApiDirective.Type.JS, true);

    // Add exports of bundle if it is an explicitly-specified feature.
    } else if (jsUri.getCompileMode() == JsCompileMode.CONCAT_COMPILE_EXPORT_EXPLICIT) {
      if (jsUri.getLibs().contains(bundle.getName())) {
        return bundle.getApis(ApiDirective.Type.JS, true);
      }
    }

    return Lists.newArrayList();
  }

  protected final List<JsContent> getSurfaceJsContents(
      FeatureRegistry featureRegistry, String featureName) {
    ImmutableList.Builder<JsContent> result = ImmutableList.builder();
    LookupResult lookup = featureRegistry.getFeatureResources(context.get(),
        ImmutableList.of(featureName), null);
    for (FeatureBundle bundle : lookup.getBundles()) {
      for (FeatureResource resource : bundle.getResources()) {
        result.add(JsContent.fromFeature(
            resource.getDebugContent(), resource.getName(),
            bundle, resource));
      }
    }
    return result.build();
  }

  protected Collection<Input> generateInputs(List<String> symbols) {
    Map<String, Input> result = Maps.newLinkedHashMap();
    for (String symbol : symbols) {
      String ns = getNamespace(symbol);
      Input input = result.get(ns);
      if (input == null) {
        input = (ns != null) ? Input.newLocal(ns, expandNamespace(ns)) : Input.newGlobal();
        result.put(ns, input);
      }
      String property = (ns != null) ? getProperty(symbol) : symbol;
      input.properties.add(property);
    }
    return result.values();
  }

  private List<String> expandNamespace(String namespace) {
    List<String> result = Lists.newArrayList();
    for (int from = 0; ;) {
      int idx = namespace.indexOf('.', from);
      if (idx >= 0) {
        result.add(namespace.substring(0, idx));
        from = idx + 1;
      } else {
        result.add(namespace);
        break;
      }
    }
    return result;
  }

  /**
   * Return the namespace for symbol (before last dot). If symbol is global,
   * return null, to indicate "window" namespace.
   */
  private String getNamespace(String symbol) {
    int idx = symbol.lastIndexOf('.');
    return (idx >= 0) ? symbol.substring(0, idx) : null;
  }

  /**
   * Return the property of symbol (after last dot). If symbol is global,
   * return the original string.
   */
  private String getProperty(String symbol) {
    int idx = symbol.lastIndexOf('.');
    return (idx >= 0) ? symbol.substring(idx + 1) : symbol;
  }

  protected static class Input {
    String namespace;
    List<String> components;
    List<String> properties;

    private Input(String namespace, List<String> components) {
      this.namespace = namespace;
      this.components = components;
      this.properties = Lists.newArrayList();
    }

    static Input newGlobal() {
      return new Input(null, ImmutableList.<String>of());
    }

    static Input newLocal(String namespace, List<String> components) {
      return new Input(namespace, components);
    }
  }
}
