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
import org.apache.shindig.gadgets.http.ContentFetcherFactory;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.oauth.OAuthArguments;
import org.apache.shindig.gadgets.preload.PreloaderService;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.View;

import com.google.inject.Inject;

import org.json.JSONArray;
import org.json.JSONException;

/**
 * Handles producing output markup for a gadget based on the provided context.
 */
public class Renderer {
  private final GadgetSpecFactory gadgetSpecFactory;
  private final ContentFetcherFactory fetcher;
  private final PreloaderService preloader;
  private final ContainerConfig containerConfig;

  @Inject
  public Renderer(GadgetSpecFactory gadgetSpecFactory,
                  ContentFetcherFactory fetcher,
                  PreloaderService preloader,
                  ContainerConfig containerConfig) {
    this.gadgetSpecFactory = gadgetSpecFactory;
    this.fetcher = fetcher;
    this.preloader = preloader;
    this.containerConfig = containerConfig;
  }

  /**
   * Render the gadget into a string by performing the following steps:
   *
   * - Retrieve gadget specification information (GadgetSpec, MessageBundle, etc.)
   *
   * - Fetch any preloaded data needed to handle the request, as handled by Preloader.
   *
   * - Perform rewriting operations on the output content, handled by Rewriter.
   *
   * @param context The context for the gadget rendering operation.
   * @return The rendered gadget content
   * @throws RenderingException if any issues arise that prevent rendering.
   */
  public String render(GadgetContext context) throws RenderingException {
    try {
      GadgetSpec spec = gadgetSpecFactory.getGadgetSpec(context);

      Gadget gadget = new Gadget()
          .setContext(context)
          .setSpec(spec);

      // TODO: Move Gadget.getView into a utility method so that the correct view can be pulled from
      // the gadget with aliasing done automatically.
      View view = getView(context, spec);

      if (view == null) {
        throw new RenderingException("Unable to locate an appropriate view in this gadget. " +
            "Requested: '" + context.getView() + "' Available: " + spec.getViews().keySet());
      }

      if (view.getType() == View.ContentType.URL) {
        throw new RenderingException("Attempted to render a url-type gadget.");
      }

      gadget.setPreloads(preloader.preload(context, spec));

      if (view.getHref() == null) {
        return view.getContent();
      } else {
        // TODO: Add current url to GadgetContext to support transitive proxying.
        HttpRequest request = new HttpRequest(Uri.fromJavaUri(view.getHref()))
            .setSecurityToken(context.getToken())
            .setOAuthArguments(new OAuthArguments(view))
            .setAuthType(view.getAuthType())
            .setContainer(context.getContainer())
            .setGadget(Uri.fromJavaUri(context.getUrl()));
        HttpResponse response = fetcher.fetch(request);
        return response.getResponseAsString();
      }
    } catch (GadgetException e) {
      throw new RenderingException(e);
    }
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
        try {
          for (int i = 0, j = aliases.length(); i < j; ++i) {
            viewName = aliases.getString(i);
            view = spec.getView(viewName);
            if (view != null) {
              break;
            }
          }
        } catch (JSONException e) {
          view = null;
        }
      }

      if (view == null) {
        view = spec.getView(GadgetSpec.DEFAULT_VIEW);
      }
    }
    return view;
  }
}
