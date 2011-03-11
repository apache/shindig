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
import org.apache.shindig.gadgets.JsCompileMode;
import org.apache.shindig.gadgets.RenderingContext;
import org.apache.shindig.gadgets.features.ApiDirective;
import org.apache.shindig.gadgets.features.FeatureRegistry;
import org.apache.shindig.gadgets.features.FeatureRegistry.FeatureBundle;
import org.apache.shindig.gadgets.features.FeatureRegistry.LookupResult;
import org.apache.shindig.gadgets.features.FeatureResource;
import org.apache.shindig.gadgets.uri.JsUriManager.JsUri;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * JsCompiler implementation for dynamic (or run-time) feature JS compilation.
 */
public class ExportJsCompiler extends DefaultJsCompiler {

  @VisibleForTesting
  static final String FEATURE_NAME = "exportjs";

  private static final String FUNCTION_NAME = "exportJs";

  private final FeatureRegistry featureRegistry;

  @Inject
  public ExportJsCompiler(FeatureRegistry featureRegistry) {
    this.featureRegistry = featureRegistry;
  }

  @Override
  public String getJsContent(JsUri jsUri, FeatureBundle bundle) {
    StringBuilder builder = new StringBuilder();
    builder.append("\n/* feature=").append(bundle.getName()).append(" */\n");
    builder.append("(function() {");
    builder.append(super.getJsContent(jsUri, bundle));
    appendExportsForFeature(builder, jsUri, bundle);
    builder.append("})();");
    builder.append("\n/* feature=").append(bundle.getName()).append(" */\n");
    return builder.toString();
  }

  @Override
  public Result compile(JsUri jsUri, String content, List<String> externs) {
    GadgetContext ctx = new JsGadgetContext(jsUri);
    StringBuilder builder = new StringBuilder();
    builder.append(getExportJsFeature(ctx));
    builder.append(content);
    return new Result(builder.toString());
  }

  @Override
  protected String getFeatureContent(JsUri jsUri, FeatureResource resource) {
    return resource.getDebugContent();
  }

  private void appendExportsForFeature(StringBuilder out, JsUri jsUri, FeatureBundle bundle) {
    List<String> exports = Lists.newArrayList();

    // Add exports of bundle, regardless.
    if (jsUri.getCompileMode() == JsCompileMode.ALL_RUN_TIME) {
      exports = bundle.getApis(ApiDirective.Type.JS, true);

    // Add exports of bundle if it is an explicitly-specified feature.
    } else if (jsUri.getCompileMode() == JsCompileMode.EXPLICIT_RUN_TIME) {
      if (jsUri.getLibs().contains(bundle.getName())) {
        exports = bundle.getApis(ApiDirective.Type.JS, true);
      }
    }

    for (Input input : generateInputs(exports)) {
      out.append(input.toExportStatement());
    }
  }

  private String getExportJsFeature(GadgetContext context) {
    StringBuilder builder = new StringBuilder();
    LookupResult lookup = featureRegistry.getFeatureResources(context,
        ImmutableList.of(FEATURE_NAME), null);
    for (FeatureBundle bundle : lookup.getBundles()) {
      for (FeatureResource resource : bundle.getResources()) {
        builder.append(resource.getDebugContent());
      }
    }
    return builder.toString();
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

    public String toExportStatement() {
      StringBuilder result = new StringBuilder();

      // Local namespace.
      if (namespace != null) {
        result.append(FUNCTION_NAME).append("('").append(namespace).append("',[");
        result.append(Joiner.on(',').join(components));
        result.append("],{");
        for (int i = 0; i < properties.size(); i++) {
          String prop = properties.get(i);
          if (i > 0) result.append(",");
          result.append(prop).append(":'").append(prop).append("'");
        }
        result.append("});");

      // Global/window namespace.
      } else {
        for (int i = 0; i < properties.size(); i++) {
          String prop = properties.get(i);
          result.append(FUNCTION_NAME).append("(");
          result.append("'").append(prop).append("',[").append(prop);
          result.append("]);");
        }
      }
      return result.toString();
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

  protected static class JsGadgetContext extends GadgetContext {
    private final RenderingContext renderingContext;
    private final String container;

    public JsGadgetContext(JsUri ctx) {
      this.renderingContext = ctx.getContext();
      this.container = ctx.getContainer();
    }

    @Override
    public RenderingContext getRenderingContext() {
      return renderingContext;
    }

    @Override
    public String getContainer() {
      return container;
    }
  }
}
