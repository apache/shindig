/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.shindig.gadgets.render;

import org.apache.shindig.common.ContainerConfig;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.GadgetSpecFactory;
import org.apache.shindig.gadgets.VariableSubstituter;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.View;

import com.google.inject.Inject;

import org.json.JSONArray;
import org.json.JSONException;

import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Validates a rendering request parameters before calling an appropriate renderer.
 */
public class Renderer {
  private static final Logger LOG = Logger.getLogger(Renderer.class.getName());
  private final HtmlRenderer renderer;
  private final GadgetSpecFactory gadgetSpecFactory;
  private final ContainerConfig containerConfig;
  private final VariableSubstituter substituter;

  @Inject
  public Renderer(HtmlRenderer renderer,
                  GadgetSpecFactory gadgetSpecFactory,
                  ContainerConfig containerConfig,
                  VariableSubstituter substituter) {
    this.renderer = renderer;
    this.gadgetSpecFactory = gadgetSpecFactory;
    this.containerConfig = containerConfig;
    this.substituter = substituter;
  }

  /**
   * Attempts to render the requested gadget.
   *
   * @return The results of the rendering attempt.
   *
   * TODO: Localize error messages.
   */
  public RenderingResults render(GadgetContext context) {
    URI url = context.getUrl();

    if (url == null) {
      return RenderingResults.error("Missing or malformed url parameter");
    }

    if (!"http".equalsIgnoreCase(url.getScheme()) && !"https".equalsIgnoreCase(url.getScheme())) {
      return RenderingResults.error("Unsupported scheme (must be http or https).");
    }

    if (!validateParent(context)) {
      return RenderingResults.error("Unsupported parent parameter. Check your container code.");
    }

    // TODO: Locked domain.

    try {
      GadgetSpec spec = gadgetSpecFactory.getGadgetSpec(context);

      // We have to perform all possible substitutions here, because subsequent steps may require
      // access to any arbitrary post-substituted field.
      spec = substituter.substitute(context, spec);

      View view = getView(context, spec);

      if (view == null) {
        return RenderingResults.error("Unable to locate an appropriate view in this gadget. " +
            "Requested: '" + context.getView() + "' Available: " + spec.getViews().keySet());
      }

      Gadget gadget = new Gadget()
          .setContext(context)
          .setSpec(spec)
          .setCurrentView(view);

      if (view.getType() == View.ContentType.URL) {
        return RenderingResults.mustRedirect(getTypeUrlRedirect(gadget));
      }

      // TODO: Validate locked domain.

      return RenderingResults.ok(renderer.render(gadget));
    } catch (RenderingException e) {
      LOG.log(Level.WARNING, "Failed to render gadget " + context.getUrl(), e);
      return RenderingResults.error(e.getLocalizedMessage());
    } catch (GadgetException e) {
      LOG.log(Level.WARNING, "Failed to process gadget " + context.getUrl(), e);
      return RenderingResults.error(e.getLocalizedMessage());
    }
  }

  /**
   * Validates that the parent parameter was acceptable.
   *
   * @return True if the parent parameter is valid for the current container.
   */
  private boolean validateParent(GadgetContext context) {
    String container = context.getContainer();
    String parent = context.getParameter("parent");

    if (parent == null) {
      // If there is no parent parameter, we are still safe because no
      // dependent code ever has to trust it anyway.
      return true;
    }

    try {
      JSONArray parents = containerConfig.getJsonArray(container, "gadgets.parent");
      if (parents == null) {
        return true;
      }
      // We need to check each possible parent parameter against this regex.
      for (int i = 0, j = parents.length(); i < j; ++i) {
        if (Pattern.matches(parents.getString(i), parent)) {
          return true;
        }
      }
    } catch (JSONException e) {
      LOG.log(Level.WARNING, "Configuration error", e);
    }
    return false;
  }

  private Uri getTypeUrlRedirect(Gadget gadget) {
    // TODO: This should probably just call UrlGenerator.getIframeUrl().
    return Uri.fromJavaUri(gadget.getCurrentView().getHref());
  }

  /**
   * Attempts to extract the "current" view for the given gadget.
   */
  private View getView(GadgetContext context, GadgetSpec spec) {
    String viewName = context.getView();
    View view = spec.getView(viewName);
    if (view == null) {
      JSONArray aliases = containerConfig.getJsonArray(context.getContainer(),
          "gadgets.features/views/" + viewName + "/aliases");
      if (aliases != null) {
        for (int i = 0, j = aliases.length(); i < j; ++i) {
          viewName = aliases.optString(i);
          if (viewName != null) {
            view = spec.getView(viewName);
            if (view != null) {
              break;
            }
          }
        }
      }

      if (view == null) {
        view = spec.getView(GadgetSpec.DEFAULT_VIEW);
      }
    }
    return view;
  }
}
