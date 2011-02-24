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
package org.apache.shindig.gadgets.rewrite.js;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.features.FeatureRegistry;
import org.apache.shindig.gadgets.features.FeatureRegistry.LookupResult;
import org.apache.shindig.gadgets.features.FeatureResource;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Base for a JsCompiler implementation.
 */
public abstract class AbstractJsCompiler implements JsCompiler {

  @VisibleForTesting
  static final String FEATURE_NAME = "exportjs";

  private static final String FUNCTION_NAME = "exportJs";

  private boolean hasInjectedJs = false;
  private final FeatureRegistry featureRegistry;

  @Inject
  public AbstractJsCompiler(FeatureRegistry featureRegistry) {
    this.featureRegistry = featureRegistry;
  }

  private void appendExportJsFeature(StringBuilder out, GadgetContext context) {
    LookupResult lookup = featureRegistry.getFeatureResources(context,
        ImmutableList.of(FEATURE_NAME), null);
    for (FeatureRegistry.FeatureBundle bundle : lookup.getBundles()) {
      for (FeatureResource resource : bundle.getResources()) {
        out.append(resource.getDebugContent());
      }
    }
  }

  public String generateExportStatements(GadgetContext context, List<String> symbols) {
    Collection<Input> inputs = generateInputs(symbols);
    StringBuilder result = new StringBuilder();
    if (!inputs.isEmpty()) {
      if (!hasInjectedJs) {
        appendExportJsFeature(result, context);
        hasInjectedJs = true;
      }
      for (Input input : inputs) {
        result.append(generateExportStatement(input));
      }
    }
    return result.toString();
  }

  private static class Input {
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

  private Collection<Input> generateInputs(List<String> symbols) {
    Map<String, Input> result = Maps.newHashMap();
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

  private String generateExportStatement(Input input) {
    StringBuilder result = new StringBuilder();

    // Local namespace.
    if (input.namespace != null) {
      result.append(FUNCTION_NAME).append("('").append(input.namespace).append("',[");
      result.append(Joiner.on(',').join(input.components));
      result.append("],{");
      for (int i = 0; i < input.properties.size(); i++) {
        String prop = input.properties.get(i);
        if (i > 0) result.append(",");
        result.append(prop).append(":'").append(prop).append("'");
      }
      result.append("});");

    // Global/window namespace.
    } else {
      for (int i = 0; i < input.properties.size(); i++) {
        String prop = input.properties.get(i);
        result.append(FUNCTION_NAME).append("(");
        result.append("'").append(prop).append("',[").append(prop);
        result.append("]);");
      }
    }

    return result.toString();
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
}
