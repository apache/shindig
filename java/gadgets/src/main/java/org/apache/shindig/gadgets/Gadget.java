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

import org.apache.shindig.gadgets.preload.PreloadedData;
import org.apache.shindig.gadgets.servlet.ProxyBase;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.LocaleSpec;
import org.apache.shindig.gadgets.spec.View;

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
  private GadgetFeatureRegistry gadgetFeatureRegistry;
  private GadgetContext context;
  private GadgetSpec spec;
  private Collection<PreloadedData> preloads;
  private View currentView;
  private Set<String> addedFeatures;
  private Set<String> removedFeatures;

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
  public Gadget setGadgetFeatureRegistry(GadgetFeatureRegistry registry) {
    this.gadgetFeatureRegistry = registry;
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
      if (gadgetFeatureRegistry != null) {
        allGadgetFeatures = Lists.newArrayList();
        for (GadgetFeature gadgetFeature :
               gadgetFeatureRegistry.getFeatures(
                   this.spec.getModulePrefs().getFeatures().keySet())) {
          allGadgetFeatures.add(gadgetFeature.getName());
        }
        // now all features are in reverse order of dependency. So reverse the list.
        Collections.reverse(allGadgetFeatures);
      } else {
        throw new IllegalStateException(
            "setGadgetFeatureRegistry must be called before Gadget.getAllFeatures()");
      }
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
    if (addedFeatures == null) {
      addedFeatures = Sets.newHashSet();
    }
    
    addedFeatures.add(name);
  }
  
  public void removeFeature(String name) {
    if (removedFeatures == null) {
      removedFeatures = Sets.newHashSet();
    }
    
    removedFeatures.add(name);
  }
  
  public Set<String> getAddedFeatures() {
    if (addedFeatures == null) {
      return Collections.emptySet();
    }
    
    return addedFeatures;
  }

  public Set<String> getRemovedFeatures() {
    if (removedFeatures == null) {
      return Collections.emptySet();
    }
    
    return removedFeatures;
  }

  /**
   * Should the gadget content be sanitized on output
   * @return
   */
  public boolean sanitizeOutput() {
    return (getCurrentView() != null &&
        getCurrentView().getType() == View.ContentType.HTML_SANITIZED) ||
        "1".equals(getContext().getParameter(ProxyBase.SANITIZE_CONTENT_PARAM));
  }
}
