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
import org.apache.shindig.gadgets.RenderingContext;
import org.apache.shindig.gadgets.config.ConfigContributor;
import org.apache.shindig.gadgets.features.FeatureRegistry;
import org.apache.shindig.gadgets.features.FeatureResource;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
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
   * Get the JavaScript content from the feature name aliases.
   *
   * @param req the HttpRequest object.
   * @return JsHandlerResponse object that contains JavaScript data and cacheable flag.
   */
  public JsHandlerResponse getJsContent(final HttpServletRequest req) {   
    // get the Set of features needed from request
    Set<String> needed = getFeaturesNeeded(req);

    // get the GadgetContext instance
    GadgetContext ctx = getGadgetContext(req);

    // get js data from feature resoources.
    return getFeatureResourcesContent(req, ctx, needed);
  }

  /**
   * Get the Set of feature names from the request.
   *
   * @param req the HttpServletRequest object.
   * @return Set of names of needed JavaScript as feature aliases from the request.
   */
  protected Set<String> getFeaturesNeeded(final HttpServletRequest req) {
    // Use the last component as filename; prefix is ignored
    String uri = req.getRequestURI();
    // We only want the file name part. There will always be at least 1 slash
    // (the server root), so this is always safe.
    String resourceName = uri.substring(uri.lastIndexOf('/') + 1);
    if (resourceName.endsWith(".js")) {
      // Lop off the suffix for lookup purposes
      resourceName = resourceName.substring(0, resourceName.length() - ".js".length());
    }

    Set<String> needed = ImmutableSet.copyOf(StringUtils.split(resourceName, ':'));
    return needed;
  }

  /**
   * Get the GadgetContext to be used when calling FeatureRegistry.getFeatureResources.
   * 
   * @param req the HttpServletRequest object.
   * @return GadgetContext instance.
   */
  protected GadgetContext getGadgetContext(final HttpServletRequest req) {
    String containerParam = req.getParameter("container");
    String containerStr = req.getParameter("c");

    final RenderingContext context = "1".equals(containerStr) ?
        RenderingContext.CONTAINER : RenderingContext.GADGET;
    final String container =
        containerParam != null ? containerParam : ContainerConfig.DEFAULT_CONTAINER;

    return new JsGadgetContext(context, container);
  }

  /**
   * Get the content of the feature resources and push it to jsData.
   * 
   * @param req The HttpServletRequest object.
   * @param ctx GadgetContext object.
   * @param needed Set of requested feature names.
   * @return JsHandlerResponse object that contains JavaScript data and cacheable flag.
   */
  protected JsHandlerResponse getFeatureResourcesContent(final HttpServletRequest req,
      final GadgetContext ctx, Set<String> needed) {
    StringBuilder jsData = new StringBuilder();
    Collection<? extends FeatureResource> resources =
        registry.getFeatureResources(ctx, needed, null);
    String debugStr = req.getParameter("debug");
    boolean debug = "1".equals(debugStr);
    String container = ctx.getContainer();
    boolean isProxyCacheable = true;

    for (FeatureResource featureResource : resources) {
      String content = debug ? featureResource.getDebugContent() : featureResource.getContent();
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
            contributor.contribute(config, container, req.getHeader("Host"));
          }
        }
        jsData.append("gadgets.config.init(").append(JsonSerializer.serialize(config)).append(");\n");
      }
    }
    return new JsHandlerResponse(jsData, isProxyCacheable);
  }

  /**
   * Define the response data from JsHandler.
   */
  public static class JsHandlerResponse {
    private final boolean isProxyCacheable;
    private final StringBuilder jsData;

    public JsHandlerResponse (StringBuilder jsData, boolean isProxyCacheable) {
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

    public JsGadgetContext(RenderingContext renderingContext, String container) {
      this.renderingContext = renderingContext;
      this.container = container;
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
