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

import com.google.common.collect.ImmutableSet;

import com.google.common.collect.Maps;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.shindig.common.JsonSerializer;
import org.apache.shindig.common.servlet.HttpUtil;
import org.apache.shindig.common.servlet.InjectedServlet;
import org.apache.shindig.common.uri.UriBuilder;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.RenderingContext;
import org.apache.shindig.gadgets.config.ConfigContributor;
import org.apache.shindig.gadgets.features.FeatureRegistry;
import org.apache.shindig.gadgets.features.FeatureResource;
import org.apache.shindig.gadgets.uri.JsUriManager;
import org.apache.shindig.gadgets.uri.UriStatus;

import com.google.inject.Inject;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Simple servlet serving up JavaScript files by their registered aliases.
 * Used by type=URL gadgets in loading JavaScript resources.
 */
public class JsServlet extends InjectedServlet {
  
  private static final long serialVersionUID = 6255917470412008175L;

  static final String ONLOAD_JS_TPL = "(function() {" +
      "var nm='%s';" +
      "if (typeof window[nm]==='function') {" +
      "window[nm]();" +
      '}' +
      "})();";
  private static final Pattern ONLOAD_FN_PATTERN = Pattern.compile("[a-zA-Z0-9_]+");

  private transient FeatureRegistry registry;
  private transient JsUriManager jsUriManager;
  private transient ContainerConfig containerConfig;
  private transient Map<String, ConfigContributor> configContributors;

  @Inject
  public void setRegistry(FeatureRegistry registry) {
    checkInitialized();
    this.registry = registry;
  }

  @Inject
  public void setUrlGenerator(JsUriManager jsUriManager) {
    checkInitialized();
    this.jsUriManager = jsUriManager;
  }

  @Inject
  public void setContainerConfig(ContainerConfig containerConfig) {
    checkInitialized();
    this.containerConfig = containerConfig;
  }

  @Inject
  public void setConfigContributors(Map<String, ConfigContributor> configContributors) {
    checkInitialized();
    this.configContributors = configContributors;
  }

  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws IOException {
    // If an If-Modified-Since header is ever provided, we always say
    // not modified. This is because when there actually is a change,
    // cache busting should occur.
    UriStatus vstatus;
    try {
      vstatus = jsUriManager.processExternJsUri(new UriBuilder(req).toUri()).getStatus();
    } catch (GadgetException e) {
      resp.sendError(e.getHttpStatusCode(), e.getMessage());
      return;
    }
    if (req.getHeader("If-Modified-Since") != null &&
        vstatus == UriStatus.VALID_VERSIONED) {
      resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
      return;
    }

    // Use the last component as filename; prefix is ignored
    String uri = req.getRequestURI();
    // We only want the file name part. There will always be at least 1 slash
    // (the server root), so this is always safe.
    String resourceName = uri.substring(uri.lastIndexOf('/') + 1);
    if (resourceName.endsWith(".js")) {
      // Lop off the suffix for lookup purposes
      resourceName = resourceName.substring(
          0, resourceName.length() - ".js".length());
    }

    Set<String> needed = ImmutableSet.copyOf(StringUtils.split(resourceName, ':'));

    String debugStr = req.getParameter("debug");
    String containerParam = req.getParameter("container");
    String containerStr = req.getParameter("c");

    boolean debug = "1".equals(debugStr);
    final RenderingContext context = "1".equals(containerStr) ?
        RenderingContext.CONTAINER : RenderingContext.GADGET;
    final String container =
        containerParam != null ? containerParam : ContainerConfig.DEFAULT_CONTAINER;

    GadgetContext ctx = new GadgetContext() {
      @Override
      public RenderingContext getRenderingContext() {
        return context;
      }

      @Override
      public String getContainer() {
        return container;
      }
    };

    Collection<? extends FeatureResource> resources = registry.getFeatureResources(ctx, needed, null);
    StringBuilder jsData = new StringBuilder();
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

    if (context == RenderingContext.CONTAINER) {
      // Append some container specific things

      Map<String, Object> features = containerConfig.getMap(container, "gadgets.features");
      Map<String, Object> config = Maps.newHashMapWithExpectedSize(features == null ? 2 : features.size() + 2);

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

    String onloadStr = req.getParameter("onload");
    if (onloadStr != null) {
      if (!ONLOAD_FN_PATTERN.matcher(onloadStr).matches()) {
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid onload callback specified");
        return;
      }
      jsData.append(String.format(ONLOAD_JS_TPL, StringEscapeUtils.escapeJavaScript(onloadStr)));
    }

    if (jsData.length() == 0) {
      resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    switch (vstatus) {
      case VALID_VERSIONED:
        // Versioned files get cached indefinitely
        HttpUtil.setCachingHeaders(resp, !isProxyCacheable);
        break;
      case VALID_UNVERSIONED:
        // Unversioned files get cached for 1 hour.
        HttpUtil.setCachingHeaders(resp, 60 * 60, !isProxyCacheable);
        break;
      case INVALID_VERSION:
        // URL is invalid in some way, likely version mismatch.
        // Indicate no-cache forcing subsequent requests to regenerate URLs.
        HttpUtil.setNoCache(resp);
        break;
    }
    resp.setContentType("text/javascript; charset=utf-8");
    byte[] response = jsData.toString().getBytes("UTF-8");
    resp.setContentLength(response.length);
    resp.getOutputStream().write(response);
  }
}
