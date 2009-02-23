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

import org.apache.shindig.common.servlet.InjectedServlet;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.gadgets.GadgetFeature;
import org.apache.shindig.gadgets.GadgetFeatureRegistry;
import org.apache.shindig.gadgets.JsLibrary;
import org.apache.shindig.gadgets.RenderingContext;

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

  private GadgetFeatureRegistry registry;
  @Inject
  public void setRegistry(GadgetFeatureRegistry registry) {
    this.registry = registry;
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws IOException {
    // If an If-Modified-Since header is ever provided, we always say
    // not modified. This is because when there actually is a change,
    // cache busting should occur.
    if (req.getHeader("If-Modified-Since") != null &&
        req.getParameter("v") != null) {
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
    String container = req.getParameter("container");
    String containerStr = req.getParameter("c");

    boolean debug = "1".equals(debugStr);
    if (container == null) {
      container = ContainerConfig.DEFAULT_CONTAINER;
    }
    RenderingContext context = "1".equals(containerStr) ?
        RenderingContext.CONTAINER : RenderingContext.GADGET;

    Collection<GadgetFeature> features = registry.getFeatures(needed);
    StringBuilder jsData = new StringBuilder();
    for (GadgetFeature feature : features) {
      for (JsLibrary lib : feature.getJsLibraries(context, container)) {
        if (lib.getType() != JsLibrary.Type.URL) {
          if (debug) {
            jsData.append(lib.getDebugContent());
          } else {
            jsData.append(lib.getContent());
          }
          jsData.append(";\n");
        }
      }
    }


    if (jsData.length() == 0) {
      resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    if (req.getParameter("v") != null) {
      // Versioned files get cached indefinitely
      HttpUtil.setCachingHeaders(resp);
    } else {
      // Unversioned files get cached for 1 hour.
      HttpUtil.setCachingHeaders(resp, 60 * 60);
    }
    resp.setContentType("text/javascript; charset=utf-8");
    byte[] response = jsData.toString().getBytes("UTF-8");
    resp.setContentLength(response.length);
    resp.getOutputStream().write(response);
  }
}
