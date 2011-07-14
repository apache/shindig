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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
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

public class ExportJsProcessor implements JsProcessor {

  @VisibleForTesting
  static final String FEATURE_NAME = "exportjs";

  private static final String FUNCTION_NAME = "exportJs";

  private final FeatureRegistryProvider featureRegistryProvider;
  private final Provider<GadgetContext> context;
  private final boolean deferredMode;

  @Inject
  public ExportJsProcessor(FeatureRegistryProvider featureRegistryProvider,
      Provider<GadgetContext> context) {
    this(featureRegistryProvider, context, false);
  }
  
  private ExportJsProcessor(FeatureRegistryProvider featureRegistryProvider,
      Provider<GadgetContext> context, boolean defer) {
    this.featureRegistryProvider = featureRegistryProvider;
    this.context = context;
    this.deferredMode = defer;
  }
  
  public JsProcessor getDeferredInstance() {
    return new ExportJsProcessor(featureRegistryProvider, context, true);
  }

  public boolean process(JsRequest jsRequest, JsResponseBuilder builder) throws JsException {
    JsUri jsUri = jsRequest.getJsUri();
    ImmutableList.Builder<JsContent> resp = ImmutableList.builder();

    FeatureRegistry featureRegistry;
    try {
      featureRegistry = featureRegistryProvider.get(jsUri.getRepository());
    } catch (GadgetException e) {
      throw new JsException(e.getHttpStatusCode(), e.getMessage());
    }

    boolean neededExportJs = false;
    FeatureBundle last = null;
    if (!jsUri.isJsload()) {
      for (JsContent jsc : builder.build().getAllJsContent()) {
        FeatureBundle current = jsc.getFeatureBundle();
        if (last != null && current != last) {
          neededExportJs |= appendExportJsStatementsForFeature(resp, jsUri, last);
        }
        resp.add(jsc);
        last = current;
      }
      if (last != null) {
        neededExportJs |= appendExportJsStatementsForFeature(resp, jsUri, last);
      }
    } else if (deferredMode) {
      // append all exports for deferred symbols
      neededExportJs = appendExportJsStatementsDeferred(featureRegistry, resp, jsRequest);
    }
    
    builder.clearJs();
    if (neededExportJs) {
      builder.appendAllJs(getExportJsContents(featureRegistry));
    }
    builder.appendAllJs(resp.build());
    return true;
  }

  private boolean appendExportJsStatementsForFeature(ImmutableList.Builder<JsContent> builder,
      JsUri jsUri, FeatureBundle bundle) {
    List<String> exports = Lists.newArrayList();

    // Add exports of bundle, regardless.
    if (jsUri.getCompileMode() == JsCompileMode.CONCAT_COMPILE_EXPORT_ALL) {
      exports = bundle.getApis(ApiDirective.Type.JS, true);

    // Add exports of bundle if it is an explicitly-specified feature.
    } else if (jsUri.getCompileMode() == JsCompileMode.CONCAT_COMPILE_EXPORT_EXPLICIT) {
      if (jsUri.getLibs().contains(bundle.getName())) {
        exports = bundle.getApis(ApiDirective.Type.JS, true);
      }
    }
    
    if (!exports.isEmpty()) {
      StringBuilder sb = new StringBuilder();
      for (Input input : generateInputs(exports)) {
        sb.append(input.toExportStatement(jsUri.isJsload()));
      }
      builder.add(JsContent.fromFeature(sb.toString(),
          "[generated-symbol-exports]", bundle, null));
      return true;
    }
    return false;
  }

  private boolean appendExportJsStatementsDeferred(FeatureRegistry registry,
      ImmutableList.Builder<JsContent> builder, JsRequest jsRequest) {
    LookupResult lookup = registry.getFeatureResources(context.get(),
        jsRequest.getNewFeatures(), null, false);
    
    boolean neededExports = false;
    for (FeatureBundle bundle : lookup.getBundles()) {
      if (bundle.isSupportDefer()) {
        neededExports |= appendExportJsStatementsForFeature(builder, jsRequest.getJsUri(), bundle);
      }
    }
    
    return neededExports;
  }

  private List<JsContent> getExportJsContents(FeatureRegistry featureRegistry) {
    ImmutableList.Builder<JsContent> result = ImmutableList.builder();
    LookupResult lookup = featureRegistry.getFeatureResources(context.get(),
        ImmutableList.of(FEATURE_NAME), null);
    for (FeatureBundle bundle : lookup.getBundles()) {
      for (FeatureResource resource : bundle.getResources()) {
        result.add(JsContent.fromFeature(
            resource.getDebugContent(), resource.getName(),
            bundle, resource));
      }
    }
    return result.build();
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

    public String toExportStatement(boolean isJsload) {
      StringBuilder result = new StringBuilder();

      // Local namespace.
      if (namespace != null) {
        result.append(FUNCTION_NAME).append("('").append(namespace).append("',[");
        result.append(isJsload ? "window." : "");
        result.append(Joiner.on(',').join(components));
        result.append("],{");
        for (int i = 0; i < properties.size(); i++) {
          String prop = properties.get(i);
          if (i > 0) result.append(",");
          result.append(prop).append(":'").append(prop).append("'");
        }
        result.append("}");
        if (isJsload) {
          result.append(",1");
        }
        result.append(");");

      // Global/window namespace.
      } else {
        for (String prop : properties) {
          result.append(FUNCTION_NAME).append("(");
          result.append("'").append(prop).append("',[");
          result.append(isJsload ? "window." : "");
          result.append(prop);
          result.append("]");
          if (isJsload) {
            result.append(",{},1");
          }
          result.append(");");
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

}
