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
package org.apache.shindig.gadgets.http;

import org.apache.shindig.gadgets.GadgetFeature;
import org.apache.shindig.gadgets.GadgetFeatureFactory;
import org.apache.shindig.gadgets.GadgetFeatureRegistry;
import org.apache.shindig.gadgets.GadgetServerConfigReader;
import org.apache.shindig.gadgets.JsLibrary;
import org.apache.shindig.gadgets.ProcessingOptions;
import org.apache.shindig.gadgets.RenderingContext;
import org.apache.shindig.gadgets.SyndicatorConfig;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Simple servlet serving up JavaScript files by their registered aliases.
 * Used by type=URL gadgets in loading JavaScript resources.
 */
public class JsServlet extends HttpServlet {
  private CrossServletState servletState;
  private static final long START_TIME = System.currentTimeMillis();

  @Override
  public void init(ServletConfig config) throws ServletException {
    servletState = CrossServletState.get(config);
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws IOException {
    // If an If-Modified-Since header is ever provided, we always say
    // not modified. This is because when there actually is a change,
    // cache busting should occur.
    if (req.getHeader("If-Modified-Since") != null) {
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

    Set<String> needed = new HashSet<String>();
    if (resourceName.contains(":")) {
      needed.addAll(Arrays.asList(resourceName.split(":")));
    } else {
      needed.add(resourceName);
    }

    Set<GadgetFeatureRegistry.Entry> found
        = new HashSet<GadgetFeatureRegistry.Entry>();
    Set<String> missing = new HashSet<String>();

    GadgetFeatureRegistry registry
        = servletState.getGadgetServer().getConfig().getFeatureRegistry();
    if (registry.getIncludedFeatures(needed, found, missing)) {
      String containerParam = req.getParameter("c");
      RenderingContext context;
      context = "1".equals(containerParam) ?
                RenderingContext.CONTAINER :
                RenderingContext.GADGET;

      StringBuilder jsData = new StringBuilder();

      Set<GadgetFeatureRegistry.Entry> done
          = new HashSet<GadgetFeatureRegistry.Entry>();

      ProcessingOptions opts = new HttpProcessingOptions(req);
      Set<String> features = new HashSet<String>(found.size());

      // TODO: This doesn't work correctly under JDK 1.5, but it does under 1.6
      do {
        for (GadgetFeatureRegistry.Entry entry : found) {
          if (!done.contains(entry) &&
              done.containsAll(entry.getDependencies())) {
            features.add(entry.getName());
            GadgetFeatureFactory factory = entry.getFeature();
            GadgetFeature feature = factory.create();
            for (JsLibrary lib : feature.getJsLibraries(context, opts)) {
              // TODO: type url js files fail here.
              if (lib.getType() != JsLibrary.Type.URL) {
                jsData.append(lib.getContent());
              }
            }
            done.add(entry);
          }
        }
      } while (done.size() != found.size());

      if (jsData.length() == 0) {
        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return;
      }

      GadgetServerConfigReader serverConfig
          = servletState.getGadgetServer().getConfig();
      SyndicatorConfig syndConf = serverConfig.getSyndicatorConfig();
      JSONObject syndFeatures = syndConf.getJsonObject(opts.getSyndicator(),
                                                       "gadgets.features");

      if (syndFeatures != null) {
        String[] featArray = features.toArray(new String[features.size()]);
        try {
          JSONObject featureConfig = new JSONObject(syndFeatures, featArray);
          jsData.append("gadgets.config.init(")
                .append(featureConfig.toString())
                .append(");");
        } catch (JSONException e) {
          throw new RuntimeException(e);
        }
      }

      setCachingHeaders(resp);
      resp.setContentType("text/javascript");
      resp.setContentLength(jsData.length());
      resp.getOutputStream().write(jsData.toString().getBytes());
    } else {
      resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
    }
  }

  /**
   * Sets HTTP headers that instruct the browser to cache indefinitely.
   * Implementations should take care to use cache-busting techniques on the
   * url.
   *
   * @param response The HTTP response
   */
  private void setCachingHeaders(HttpServletResponse response) {

    // Most browsers accept this. 2030 is the last round year before
    // the end of time.
    response.setHeader("Expires", "Tue, 01 Jan 2030 00:00:01 GMT");

    // IE seems to need this (10 years should be enough).
    response.setHeader("Cache-Control", "public,max-age=315360000");

    // Firefox requires this for certain cases.
    response.setDateHeader("Last-Modified", START_TIME);
  }
}
