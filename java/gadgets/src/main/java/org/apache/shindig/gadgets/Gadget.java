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

import com.google.common.base.Preconditions;
import org.apache.shindig.gadgets.features.FeatureRegistry;
import org.apache.shindig.gadgets.preload.PreloadedData;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.LocaleSpec;
import org.apache.shindig.gadgets.spec.View;
import org.apache.shindig.gadgets.uri.UriCommon;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
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
    this.directFeatureDeps = Sets.newHashSet(spec.getModulePrefs().getFeatures().keySet());
    return this;
  }

  public GadgetSpec getSpec() {
    return spec;
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
      allGadgetFeatures = featureRegistry.getFeatures(Lists.newArrayList(directFeatureDeps));
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
    return spec.getModulePrefs().getLocale(context.getLocale());
  }
  
  public void addFeature(String name) {
    directFeatureDeps.add(name);
  }
  
  public void removeFeature(String name) {
    directFeatureDeps.remove(name);
  }
  
  public Set<String> getDirectFeatureDeps() {
    return Collections.unmodifiableSet(directFeatureDeps);
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
}
