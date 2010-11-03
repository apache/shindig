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

import org.apache.shindig.common.JsonSerializer;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.RenderingContext;
import org.apache.shindig.gadgets.config.ConfigContributor;
import org.apache.shindig.gadgets.features.FeatureRegistry;
import org.apache.shindig.gadgets.features.FeatureResource;
import org.apache.shindig.gadgets.uri.JsUriManager.JsUri;

import java.util.Collection;
import java.util.Map;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Provide processing logic for the JsServlet to serve the JavsScript as features request.
 * This class will allow separation of flow and serving logic for easy customization.
 * 
 */
@Singleton
public class JsHandler {
  protected final FeatureRegistry registry;
  protected final ContainerConfig containerConfig;
  protected final Map<String, ConfigContributor> configContributors;

  @Inject
  public JsHandler(FeatureRegistry registry, ContainerConfig containerConfig,
      Map<String, ConfigContributor> configContributors) {
    this.registry = registry;
    this.containerConfig = containerConfig;
    this.configContributors = configContributors;
  }

  /**
   * Get the content of the feature resources and push it to jsData.
   * 
   * @param req The HttpServletRequest object.
   * @param ctx GadgetContext object.
   * @param needed Set of requested feature names.
   * @return JsHandlerResponse object that contains JavaScript data and cacheable flag.
   */
  protected Response getJsContent(final JsUri jsUri, String host) {
    GadgetContext ctx = new JsGadgetContext(jsUri);
    StringBuilder jsData = new StringBuilder();
    Collection<String> needed = jsUri.getLibs();
    Collection<? extends FeatureResource> resources =
        registry.getFeatureResources(ctx, needed, null);
    String container = ctx.getContainer();
    boolean isProxyCacheable = true;

    for (FeatureResource featureResource : resources) {
      String content = jsUri.isDebug() ? featureResource.getDebugContent() : featureResource.getContent();
      if (!featureResource.isExternal()) {
        jsData.append(content);
      } else {
        // Support external/type=url feature serving through document.write()
        jsData.append("document.write('<script src=\"").append(content).append("\"></script>')");
      }
      isProxyCacheable = isProxyCacheable && featureResource.isProxyCacheable();
      jsData.append(";\n");
    }

    if (ctx.getRenderingContext() == RenderingContext.CONTAINER) {
      // Append some container specific things
      Map<String, Object> features = containerConfig.getMap(container, "gadgets.features");
      Map<String, Object> config =
          Maps.newHashMapWithExpectedSize(features == null ? 2 : features.size() + 2);

      if (features != null) {
        // Discard what we don't care about.
        for (String name : registry.getFeatures(needed)) {
          Object conf = features.get(name);
          // Add from containerConfig
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
    return new Response(jsData, isProxyCacheable);
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
