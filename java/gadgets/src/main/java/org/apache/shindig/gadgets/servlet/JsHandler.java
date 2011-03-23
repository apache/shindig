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

import com.google.caja.util.Sets;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.shindig.common.JsonSerializer;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.RenderingContext;
import org.apache.shindig.gadgets.config.ConfigContributor;
import org.apache.shindig.gadgets.features.ApiDirective;
import org.apache.shindig.gadgets.features.FeatureRegistry;
import org.apache.shindig.gadgets.features.FeatureResource;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.js.JsException;
import org.apache.shindig.gadgets.js.JsResponse;
import org.apache.shindig.gadgets.js.JsResponseBuilder;
import org.apache.shindig.gadgets.rewrite.js.JsCompiler;
import org.apache.shindig.gadgets.uri.JsUriManager.JsUri;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provide processing logic for the JsServlet to serve the JavsScript as features request.
 * This class will allow separation of flow and serving logic for easy customization.
 */
@Singleton
public class JsHandler {
  private static final String CONFIG_INIT_ID = "[config-injection]";
  private static final Collection<String> EMPTY_SET = Sets.newHashSet();
  private static final Logger LOG = Logger.getLogger(JsHandler.class.getName());
  private static final Joiner UNKNOWN_FEATURE_ERR = Joiner.on(", ");

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

  /**
   * Get the content of the feature resources and push it to jsData.
   *
   * @param jsUri A JsUri object that describes the resources to get.
   * @param host The name of the host the request was directed to.
   * @return JsHandlerResponse object that contains JavaScript data and cacheable flag.
   */
  public JsResponse getJsContent(final JsUri jsUri, String host) throws JsException {
    GadgetContext ctx = new JsGadgetContext(jsUri);
    Collection<String> needed = jsUri.getLibs();

    List<String> unsupported = Lists.newLinkedList();
    FeatureRegistry.LookupResult lookup = registry.getFeatureResources(ctx, needed, unsupported);
    if (!unsupported.isEmpty()) {
      throw new JsException(HttpResponse.SC_BAD_REQUEST,
          "Unknown feature" + (unsupported.size() > 1 ? "s" : "") +
          ": " + UNKNOWN_FEATURE_ERR.join(unsupported));
    }

    // Quick-and-dirty implementation of incremental JS loading.
    Collection<String> alreadyLoaded = EMPTY_SET;
    Collection<String> alreadyHaveLibs = jsUri.getLoadedLibs();
    if (!alreadyHaveLibs.isEmpty()) {
      alreadyLoaded = registry.getFeatures(alreadyHaveLibs);
    }

    // Collate all JS desired for the current request.
    boolean isProxyCacheable = true;
    JsResponseBuilder responseBuilder = new JsResponseBuilder();
    List<String> allExterns = Lists.newArrayList();

    // Pre-process each feature.
    for (FeatureRegistry.FeatureBundle bundle : lookup.getBundles()) {
      if (alreadyLoaded.contains(bundle.getName())) continue;
      responseBuilder.appendJs(compiler.getJsContent(jsUri, bundle));
      allExterns.addAll(bundle.getApis(ApiDirective.Type.JS, false));
      for (FeatureResource featureResource : bundle.getResources()) {
        isProxyCacheable = isProxyCacheable && featureResource.isProxyCacheable();
      }
    }
    responseBuilder.setProxyCacheable(isProxyCacheable);

    // Compile all pre-processed features.
    JsResponse result = compiler.compile(jsUri, responseBuilder.build().getAllJsContent(), allExterns);

    String code = result.toJsString();
    if (code != null) {
      responseBuilder.setJs(code, "[compiled]");
    } else {
      warn(result);
    }

    // Append gadgets.config initialization if not in standard gadget mode.
    if (ctx.getRenderingContext() != RenderingContext.GADGET) {
      String container = ctx.getContainer();

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
        // TODO: Convert this to a JsProcessor.
        responseBuilder.appendJs(
            "gadgets.config.init(" + JsonSerializer.serialize(config) + ");\n", CONFIG_INIT_ID);
      }
    }

    // Wrap up the response.
    return responseBuilder.build();
  }

  protected void warn(JsResponse result) {
    LOG.log(Level.WARNING, "Continuing with un-compiled content. " +
        "JS Compilation error: " + Joiner.on(", ").join(result.getErrors()));
  }

  /**
   * GadgetContext for JsHandler called by FeatureRegistry when fetching the resources.
   */
  protected static class JsGadgetContext extends GadgetContext {
    private final RenderingContext renderingContext;
    private final String container;
    private final boolean debug;

    public JsGadgetContext(JsUri ctx) {
      this.renderingContext = ctx.getContext();
      this.container = ctx.getContainer();
      this.debug = ctx.isDebug();
    }

    @Override
    public RenderingContext getRenderingContext() {
      return renderingContext;
    }

    @Override
    public String getContainer() {
      return container;
    }

    @Override
    public boolean getDebug() {
      return debug;
    }
  }
}
