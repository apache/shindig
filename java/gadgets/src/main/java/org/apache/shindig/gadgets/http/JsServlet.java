/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.shindig.gadgets.http;

import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.GadgetFeatureFactory;
import org.apache.shindig.gadgets.GadgetFeatureRegistry;
import org.apache.shindig.gadgets.JsFeatureLoader;
import org.apache.shindig.gadgets.JsLibrary;
import org.apache.shindig.gadgets.JsLibraryFeatureFactory;
import org.apache.shindig.gadgets.RenderingContext;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Simple servlet serving up JavaScript files by their registered aliases.
 * Used by type=URL gadgets in loading JavaScript resources.
 */
public class JsServlet extends HttpServlet {
  private GadgetFeatureRegistry registry;

  @Override
  public void init(ServletConfig config) {
    ServletContext context = config.getServletContext();
    String coreFeatures = context.getInitParameter("core-js-features");
    String otherFeatures = context.getInitParameter("other-js-features");
    String jsPath = context.getInitParameter("js-service-path");
    try {
      registry = new GadgetFeatureRegistry(coreFeatures);
      if (otherFeatures != null) {
        JsFeatureLoader jsLoader = new JsFeatureLoader();
        jsLoader.loadFeatures(otherFeatures, registry);
      }
    } catch (GadgetException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws IOException {
    // Use the last component as filename; prefix is ignored
    String path = req.getPathTranslated();
    String[] pathComponents = path.split("/");
    String resourceName = pathComponents[pathComponents.length-1];
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

    if (registry.getIncludedFeatures(needed, found, missing)) {
      String containerParam = req.getParameter("c");
      RenderingContext context;
      context = containerParam != null && containerParam.equals("1") ?
                RenderingContext.CONTAINER :
                RenderingContext.GADGET;

      StringBuilder jsData = new StringBuilder();

      Set<GadgetFeatureRegistry.Entry> done
          = new HashSet<GadgetFeatureRegistry.Entry>();
      do {
        for (GadgetFeatureRegistry.Entry entry : found) {
          if (!done.contains(entry) &&
              done.containsAll(entry.getDependencies())) {
            GadgetFeatureFactory feature = entry.getFeature();
            if (feature instanceof JsLibraryFeatureFactory) {
              JsLibraryFeatureFactory jsLib = (JsLibraryFeatureFactory)feature;
              for (JsLibrary lib : jsLib.getLibraries(context)) {
                // TODO: type url js files fail here.
                if (lib.getType() != JsLibrary.Type.URL) {
                  jsData.append(lib.getContent());
                }
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
    response.setHeader("Cache-Control", "public,max-age=2592000");
    response.setDateHeader("Expires", System.currentTimeMillis()
                                     + 2592000000L);
  }
}
