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
import org.apache.shindig.gadgets.preload.Preloads;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.LocaleSpec;
import org.apache.shindig.gadgets.spec.Preload;
import org.apache.shindig.gadgets.spec.View;

import com.google.common.collect.Maps;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * Intermediary representation of all state associated with processing
 * of a single gadget request.
 */
public class Gadget {
  private GadgetContext context;
  private GadgetSpec spec;
  private Preloads preloads;
  private View currentView;
  private String content;

  @Deprecated
  private Collection<JsLibrary> jsLibraries;

  @Deprecated
  private Map<Preload, Future<HttpResponse>> preloadMap;

  public Gadget() { }

  /**
   * @deprecated Use default ctor and setter methods instead.
   *
   * TODO: Remove this entirely. The only code paths using it should be for the old rendering
   * pipeline, so this can be removed once that's gone.
   */
  @Deprecated
  public Gadget(GadgetContext context, GadgetSpec spec,
      Collection<JsLibrary> jsLibraries, ContainerConfig containerConfig,
      GadgetHtmlParser contentParser) {
    this.preloadMap = Maps.newHashMap();
    this.context = context;
    this.spec = spec;
    this.jsLibraries = jsLibraries;
    currentView = getView(containerConfig);
    if (currentView != null) {
      content = currentView.getContent();
    }
  }

  /**
   * @param context The request that the gadget is being processed for.
   */
  public Gadget setContext(GadgetContext context) {
    this.context = context;
    return this;
  }

  public GadgetContext getContext() {
    return context;
  }

  /**
   * @param spec The spec for the gadget that is being processed.
   */
  public Gadget setSpec(GadgetSpec spec) {
    this.spec = spec;
    return this;
  }

  public GadgetSpec getSpec() {
    return spec;
  }

  /**
   * Sets the current content of the rendered output of this gadget.
   */
  public Gadget setContent(String content) {
    this.content = content;
    return this;
  }

  public String getContent() {
    return content;
  }

  /**
   * @param preloads The preloads for the gadget that is being processed.
   */
  public Gadget setPreloads(Preloads preloads) {
    this.preloads = preloads;
    return this;
  }

  public Preloads getPreloads() {
    return preloads;
  }

  public Gadget setCurrentView(View currentView) {
    this.currentView = currentView;
    return this;
  }

  /**
   * @return The View applicable for the current request.
   */
  public View getCurrentView() {
    return currentView;
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

  /**
   * @return A mutable collection of JsLibrary objects attached to this Gadget.
   */
  @Deprecated
  public Collection<JsLibrary> getJsLibraries() {
    return jsLibraries;
  }

  /**
   * @return A mutable map of preloads.
   */
  @Deprecated
  public Map<Preload, Future<HttpResponse>> getPreloadMap() {
    return preloadMap;
  }

  /**
   * Attempts to extract the "current" view for this gadget.
   *
   * @param config The container configuration; used to look for any view name
   *        aliases for the container specified in the context.
   */
  @Deprecated
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
}