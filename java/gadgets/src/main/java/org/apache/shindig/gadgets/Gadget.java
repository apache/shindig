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
package org.apache.shindig.gadgets;

import org.apache.shindig.common.ContainerConfig;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.parse.GadgetHtmlParser;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.LocaleSpec;
import org.apache.shindig.gadgets.spec.Preload;
import org.apache.shindig.gadgets.spec.View;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * Intermediary representation of all state associated with processing
 * of a single gadget request.
 */
public class Gadget extends MutableHtmlContent {
  private final GadgetContext context;
  
  /**
   * @return The context in which this gadget was created.
   */
  public GadgetContext getContext() {
    return context;
  }

  private final GadgetSpec spec;
  
  /**
   * @return The spec from which this gadget was originally built.
   */
  public GadgetSpec getSpec() {
    return spec;
  }

  private final Collection<JsLibrary> jsLibraries;
  
  /**
   * @return A mutable collection of JsLibrary objects attached to this Gadget.
   */
  public Collection<JsLibrary> getJsLibraries() {
    return jsLibraries;
  }

  private final Map<Preload, Future<HttpResponse>> preloads
      = new HashMap<Preload, Future<HttpResponse>>();
  
  /**
   * @return A mutable map of preloads.
   */
  public Map<Preload, Future<HttpResponse>> getPreloadMap() {
    return preloads;
  }

  /**
   * Convenience function for getting the locale spec for the current context.
   *
   * Identical to:
   * Locale locale = gadget.getContext().getLocale();
   * gadget.getSpec().getModulePrefs().getLocale(locale);
   */
  public LocaleSpec getLocale() {
    return spec.getModulePrefs().getLocale(context.getLocale());
  }
  
  private final View currentView;
  
  /**
   * @return The (immutable) View applicable for the current request (part of GadgetSpec).
   */
  public View getCurrentView() {
    return currentView;
  }

  /**
   * Attempts to extract the "current" view for this gadget.
   *
   * @param config The container configuration; used to look for any view name
   *        aliases for the container specified in the context.
   */
  View getView(ContainerConfig config) {
    String viewName = context.getView();
    View view = spec.getView(viewName);
    if (view == null) {
      JSONArray aliases = config.getJsonArray(context.getContainer(),
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
  public Gadget(GadgetContext context, GadgetSpec spec,
      Collection<JsLibrary> jsLibraries, ContainerConfig containerConfig,
      GadgetHtmlParser contentParser) {
    super(contentParser);
    
    this.context = context;
    this.spec = spec;
    this.jsLibraries = jsLibraries;
    this.currentView = getView(containerConfig);
    if (this.currentView != null) {
      // View might be invalid or associated with no content (type=URL)
      setContent(this.currentView.getContent());
    } else {
      setContent(null);
    }
  }
}