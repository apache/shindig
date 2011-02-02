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
package org.apache.shindig.gadgets.servlet;

import org.apache.commons.lang.StringUtils;
import org.apache.shindig.common.JsonSerializer;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.JsCompileMode;
import org.apache.shindig.gadgets.RenderingContext;
import org.apache.shindig.gadgets.config.ConfigContributor;
import org.apache.shindig.gadgets.features.ApiDirective;
import org.apache.shindig.gadgets.features.FeatureRegistry;
import org.apache.shindig.gadgets.features.FeatureResource;
import org.apache.shindig.gadgets.rewrite.js.JsCompiler;
import org.apache.shindig.gadgets.uri.JsUriManager.JsUri;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.caja.util.Join;
import com.google.caja.util.Sets;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Provide processing logic for the JsServlet to serve the JavsScript as features request.
 * This class will allow separation of flow and serving logic for easy customization.
 */
@Singleton
public class JsHandler {
  private static final Collection<String> EMPTY_SET = Sets.newHashSet();

  protected final FeatureRegistry registry;
  protected final ContainerConfig containerConfig;
  protected final Map<String, ConfigContributor> configContributors;
  private JsCompiler compiler = null;

  @Inject
  public JsHandler(
      FeatureRegistry registry,
      ContainerConfig containerConfig,
      Map<String, ConfigContributor> configContributors) {
    this.registry = registry;
    this.containerConfig = containerConfig;
    this.configContributors = configContributors;
  }

  @Inject(optional = true)
  public void setSupportCompiler(JsCompiler compiler) {
    this.compiler = compiler;
  }

  protected boolean willCompile(JsUri jsUri) {
    JsCompileMode mode = jsUri.getCompileMode();
    return (compiler != null) && !jsUri.isDebug() && (
       mode == JsCompileMode.ALL_RUN_TIME ||
       mode == JsCompileMode.EXPLICIT_RUN_TIME);
  }

  /**
   * Get the content of the feature resources and push it to jsData.
   *
   * @param req The HttpServletRequest object.
   * @param ctx GadgetContext object.
   * @param needed Set of requested feature names.
   * @return JsHandlerResponse object that contains JavaScript data and cacheable flag.
   */
  public Response getJsContent(final JsUri jsUri, String host) {
    GadgetContext ctx = new JsGadgetContext(jsUri);
    Collection<String> needed = jsUri.getLibs();
    String container = ctx.getContainer();
    boolean isProxyCacheable = true;

    FeatureRegistry.LookupResult lookup = registry.getFeatureResources(ctx, needed, null);

    // Quick-and-dirty implementation of incremental JS loading.
    Collection<String> alreadyLoaded = EMPTY_SET;
    Collection<String> alreadyHaveLibs = jsUri.getLoadedLibs();
    if (alreadyHaveLibs.size() > 0) {
      alreadyLoaded = registry.getFeatures(alreadyHaveLibs);
    }

    // Collate all JS desired for the current request.
    boolean willCompile = willCompile(jsUri);
    StringBuilder jsData = new StringBuilder();
    List<String> allExterns = Lists.newArrayList();
    Set<String> allExports = Sets.newHashSet();

    for (FeatureRegistry.FeatureBundle bundle : lookup.getBundles()) {
      if (alreadyLoaded.contains(bundle.getName())) continue;
      for (FeatureResource featureResource : bundle.getResources()) {
        String content = jsUri.isDebug() || willCompile
           ? featureResource.getDebugContent() : featureResource.getContent();
        if (content == null) content = "";
        if (!featureResource.isExternal()) {
          jsData.append(content);
        } else {
          // Support external/type=url feature serving through document.write()
          jsData.append("document.write('<script src=\"").append(content).append("\"></script>')");
        }
        isProxyCacheable = isProxyCacheable && featureResource.isProxyCacheable();
        jsData.append(";\n");
      }

      if (willCompile) {

        List<String> rawExports = Lists.newArrayList();

        // Add exports of bundle, regardless.
        if (jsUri.getCompileMode() == JsCompileMode.ALL_RUN_TIME) {
          appendExportsForBundle(bundle, rawExports);

        // Add exports of bundle if it is an explicitly-specified feature.
        } else if (jsUri.getCompileMode() == JsCompileMode.EXPLICIT_RUN_TIME) {
          if (needed.contains(bundle.getName())) {
            appendExportsForBundle(bundle, rawExports);
          }
        }

        // Add externs for this bundle.
        appendExternsForBundle(bundle, allExterns);

        Collections.sort(rawExports);
        String prevExport = null;
        for (String export : rawExports) {
          if (!export.equals(prevExport)) {
            String[] pieces = StringUtils.split(export, "\\.");
            String base = "window";
            for (int i = 0; i < pieces.length; ++i) {
              String symExported = (i == 0) ? pieces[0] : base + "." + pieces[i];
              if (!allExports.contains(symExported)) {
                String curExport = base + "['" + pieces[i] + "']=" + symExported + ";\n";
                jsData.append(curExport);
                allExports.add(symExported);
              }
              base = symExported;
            }
          }
          prevExport = export;
        }
      }
    }

    // Compile if desired. Specific compiler options are provided to the JsCompiler instance.
    if (willCompile) {
      StringBuilder compiled = new StringBuilder();
      JsCompiler.Result result = compiler.compile(jsData.toString(), allExterns);
      String code = result.getCode();
      if (code != null) {
        compiled.append(code);
        jsData = compiled;
      } else {
        System.err.println("JS Compilation error: " + Join.join(", ", result.getErrors()));
      }
    }

    // Append gadgets.config initialization if not in standard gadget mode.
    if (ctx.getRenderingContext() != RenderingContext.GADGET) {
      // Append some container specific things
      Map<String, Object> features = containerConfig.getMap(container, "gadgets.features");
      Map<String, Object> config =
          Maps.newHashMapWithExpectedSize(features == null ? 2 : features.size() + 2);

      if (features != null) {
        // Discard what we don't care about.
        for (String name : registry.getFeatures(needed)) {
          Object conf = features.get(name);
          // Add from containerConfig.
          if (conf != null) {
            config.put(name, conf);
          }
          ConfigContributor contributor = configContributors.get(name);
          if (contributor != null) {
            contributor.contribute(config, container, host);
          }
        }
        jsData.append("gadgets.config.init(").append(JsonSerializer.serialize(config)).append(");\n");
      }
    }

    // Wrap up the response.
    return new Response(jsData, isProxyCacheable);
  }

  private void appendExternsForBundle(FeatureRegistry.FeatureBundle bundle, List<String> externs) {
    for (ApiDirective api : bundle.getApis()) {
      if (api.getType() == ApiDirective.Type.JS && api.isUses()) {
        externs.add(api.getValue());
      }
    }
  }

  private void appendExportsForBundle(FeatureRegistry.FeatureBundle bundle, List<String> exports) {
    for (ApiDirective api : bundle.getApis()) {
      if (api.getType() == ApiDirective.Type.JS && api.isExports()) {
        exports.add(api.getValue());
      }
    }
  }

  /**
   * Define the response data from JsHandler.
   */
  public static class Response {
    private final boolean isProxyCacheable;
    private final StringBuilder jsData;

    public Response(StringBuilder jsData, boolean isProxyCacheable) {
      this.jsData = jsData;
      this.isProxyCacheable = isProxyCacheable;
    }

    public boolean isProxyCacheable() {
      return isProxyCacheable;
    }

    public StringBuilder getJsData() {
      return jsData;
    }
  }

  /**
   * GadgetContext for JsHandler called by FeatureRegistry when fetching the resources.
   */
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
