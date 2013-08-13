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
package org.apache.shindig.gadgets;

import org.apache.shindig.common.util.OpenSocialVersion;
import org.apache.shindig.gadgets.features.FeatureRegistry;
import org.apache.shindig.gadgets.preload.PreloadedData;
import org.apache.shindig.gadgets.spec.Feature;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.LocaleSpec;
import org.apache.shindig.gadgets.spec.View;
import org.apache.shindig.gadgets.uri.UriCommon;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
/**
 * Intermediary representation of all state associated with processing
 * of a single gadget request.
 */
public class Gadget {
  private FeatureRegistry featureRegistry;
  private GadgetContext context;
  private GadgetSpec spec;
  private Collection<PreloadedData> preloads;
  private View currentView;
  private Set<String> directFeatureDeps;

  /**
   * @param context The request that the gadget is being processed for.
   */
  public Gadget setContext(GadgetContext context) {
    this.context = context;
    directFeatureDeps = null;  //New context means View may have changed
    allGadgetFeatures = null;
    return this;
  }

  public GadgetContext getContext() {
    return context;
  }

  /**
   * @param registry The gadget feature registry to use to find dependent
   *                 features.
   */
  public synchronized Gadget setGadgetFeatureRegistry(FeatureRegistry registry) {
    this.featureRegistry = registry;
    return this;
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
   * Returns open social specification version for this Gadget
   * @return Version for this Gadget
   */
  public OpenSocialVersion getSpecificationVersion(){
    if(spec != null){
      return spec.getSpecificationVersion();
    }
    return null;
  }

  /**
   * Returns if the doctype attribute is set to quirksmode.
   * Needed to override default OpenSocial 2.0 behavior which is to render in standards mode,
   * may not be possible to honor this attribute when inlining (caja)
   *
   * @return TRUE if this Gadget should be rendered in browser quirks mode
   */
  public boolean useQuirksMode(){
    if(spec != null){
      return GadgetSpec.DOCTYPE_QUIRKSMODE.equals(spec.getModulePrefs().getDoctype());
    }
    return false;
  }

  /**
   * @param preloads The preloads for the gadget that is being processed.
   */
  public Gadget setPreloads(Collection<PreloadedData> preloads) {
    this.preloads = preloads;
    return this;
  }

  public Collection<PreloadedData> getPreloads() {
    return preloads;
  }

  /**
   * List of all features this spec depends on (including all transitive
   * dependencies).
   */
  private List<String> allGadgetFeatures;
  public synchronized List<String> getAllFeatures() {
    if (allGadgetFeatures == null) {
      Preconditions.checkState(featureRegistry != null, "setGadgetFeatureRegistry must be called before Gadget.getAllFeatures()");
      allGadgetFeatures = featureRegistry.getFeatures(Lists.newArrayList(getDirectFeatureDeps()));
    }
    return allGadgetFeatures;
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
    View view = getCurrentView();
    String viewName = (view == null) ? GadgetSpec.DEFAULT_VIEW : view.getName();
    return spec.getModulePrefs().getLocale(context.getLocale(), viewName);
  }

  private void initializeFeatureDeps() {
    if (directFeatureDeps == null) {
      directFeatureDeps = Sets.newHashSet();
      // If we have context, lets generate the correct set of views.
      if (context != null) {
        directFeatureDeps.addAll(spec.getModulePrefs()
            .getViewFeatures(context.getView()).keySet());
      } else {
        directFeatureDeps.addAll(spec.getModulePrefs().getFeatures().keySet());
      }
    }
  }

  public void addFeature(String name) {
    initializeFeatureDeps();
    directFeatureDeps.add(name);
  }

  public void removeFeature(String name) {
    initializeFeatureDeps();
    directFeatureDeps.remove(name);
  }

  public Set<String> getDirectFeatureDeps() {
    initializeFeatureDeps();
    return Collections.unmodifiableSet(directFeatureDeps);
  }

  /**
   * Convenience method that returns Map of features to load for gadget's current view
   *
   * @return a map of ModuleSpec/Require and ModuleSpec/Optional elements to Feature
   */
  public Map<String, Feature> getViewFeatures() {
    View view = getCurrentView();
    String name = (view == null) ? GadgetSpec.DEFAULT_VIEW : view.getName();

    return spec.getModulePrefs().getViewFeatures(name);
  }

  /**
   * Should the gadget content be sanitized on output
   * @return
   */
  public boolean sanitizeOutput() {
    return (getCurrentView() != null &&
        getCurrentView().getType() == View.ContentType.HTML_SANITIZED) ||
        "1".equals(getContext().getParameter(UriCommon.Param.SANITIZE.getKey()));
  }

  /**
   * True if the gadget opts into caja or the container forces caja
   */
  public boolean requiresCaja() {
    if ("1".equals(
        getContext().getParameter(UriCommon.Param.CAJOLE.getKey()))) {
      return true;
    }
    if (featureRegistry != null) {
      return getAllFeatures().contains("caja");
    } else {
      return getViewFeatures().containsKey("caja");
    }
  }
}
