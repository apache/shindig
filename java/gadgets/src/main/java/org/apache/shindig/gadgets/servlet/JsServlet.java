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

import org.apache.shindig.common.servlet.HttpUtil;
import org.apache.shindig.common.servlet.InjectedServlet;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.RenderingContext;
import org.apache.shindig.gadgets.UrlGenerator;
import org.apache.shindig.gadgets.UrlValidationStatus;
import org.apache.shindig.gadgets.features.FeatureRegistry;
import org.apache.shindig.gadgets.features.FeatureResource;

import com.google.inject.Inject;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Simple servlet serving up JavaScript files by their registered aliases.
 * Used by type=URL gadgets in loading JavaScript resources.
 */
public class JsServlet extends InjectedServlet {

  private FeatureRegistry registry;
  @Inject
  public void setRegistry(FeatureRegistry registry) {
    this.registry = registry;
  }
  
  private UrlGenerator urlGenerator;
  @Inject
  public void setUrlGenerator(UrlGenerator urlGenerator) {
    this.urlGenerator = urlGenerator;
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws IOException {
    // If an If-Modified-Since header is ever provided, we always say
    // not modified. This is because when there actually is a change,
    // cache busting should occur.
    UrlValidationStatus vstatus = urlGenerator.validateJsUrl(
        req.getRequestURL().append('?').append(req.getQueryString()).toString());
    if (req.getHeader("If-Modified-Since") != null &&
        vstatus == UrlValidationStatus.VALID_VERSIONED) {
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

    Set<String> needed = ImmutableSet.of(resourceName.split(":"));

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
    Collection<? extends FeatureResource> resources =
        registry.getFeatureResources(ctx, needed, null);
    StringBuilder jsData = new StringBuilder();
    boolean isProxyCacheable = true;
    for (FeatureResource featureResource : resources) {
      String content = debug ? featureResource.getDebugContent() : featureResource.getContent();
      if (!featureResource.isExternal()) {
        jsData.append(content);
      } else {
        // Support external/type=url feature serving through document.write()
        jsData.append("document.write('<script src=\"").append(content).append("\"></script>");
      }
      isProxyCacheable = isProxyCacheable && featureResource.isProxyCacheable();
      jsData.append(";\n");
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
      case INVALID:
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
