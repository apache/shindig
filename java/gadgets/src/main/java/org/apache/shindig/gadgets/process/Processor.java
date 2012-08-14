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
package org.apache.shindig.gadgets.process;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;

import org.apache.shindig.common.logging.i18n.MessageKeys;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.GadgetSpecFactory;
import org.apache.shindig.gadgets.admin.GadgetAdminStore;
import org.apache.shindig.gadgets.features.FeatureRegistry;
import org.apache.shindig.gadgets.features.FeatureRegistryProvider;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.View;
import org.apache.shindig.gadgets.variables.VariableSubstituter;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Converts an input Context into an output Gadget.
 */
@Singleton
public class Processor {
  //class name for logging purpose
  private static final String classname = Processor.class.getName();
  private static final Logger LOG = Logger.getLogger(classname,MessageKeys.MESSAGES);
  private final GadgetSpecFactory gadgetSpecFactory;
  private final VariableSubstituter substituter;
  private final ContainerConfig containerConfig;
  private final GadgetAdminStore gadgetAdminStore;
  private final FeatureRegistryProvider featureRegistryProvider;

  @Inject
  public Processor(GadgetSpecFactory gadgetSpecFactory,
                   VariableSubstituter substituter,
                   ContainerConfig containerConfig,
                   GadgetAdminStore gadgetAdminStore,
                   FeatureRegistryProvider featureRegistryProvider) {
    this.gadgetSpecFactory = gadgetSpecFactory;
    this.substituter = substituter;
    this.gadgetAdminStore = gadgetAdminStore;
    this.containerConfig = containerConfig;
    this.featureRegistryProvider = featureRegistryProvider;
  }

  protected void validateGadgetUrl(Uri url) throws ProcessingException {
    if (!"http".equalsIgnoreCase(url.getScheme()) && !"https".equalsIgnoreCase(url.getScheme())) {
      throw new ProcessingException("Unsupported scheme (must be http or https).",
          HttpServletResponse.SC_FORBIDDEN);
    }
  }

  /**
   * Process a single gadget. Creates a gadget from a retrieved GadgetSpec and context object,
   * automatically performing variable substitution on the spec for use elsewhere.
   *
   * @throws ProcessingException If there is a problem processing the gadget.
   */
  public Gadget process(GadgetContext context) throws ProcessingException {
    GadgetSpec spec;
    FeatureRegistry featureRegistry;

    try {
      Uri url = gadgetSpecFactory.getGadgetUri(context);

      if (url == null) {
        throw new ProcessingException("Missing or malformed url parameter",
            HttpServletResponse.SC_BAD_REQUEST);
      }

      validateGadgetUrl(url);
      if (!gadgetAdminStore.isWhitelisted(context.getContainer(), url.toString())) {
        if (LOG.isLoggable(Level.INFO)) {
          LOG.logp(Level.INFO, classname, "process", MessageKeys.RENDER_NON_WHITELISTED_GADGET, new Object[] {url});
        }
        throw new ProcessingException("The requested gadget is not authorized for this container",
            HttpServletResponse.SC_FORBIDDEN);
      }

      spec = gadgetSpecFactory.getGadgetSpec(context);
      spec = substituter.substitute(context, spec);

      if (context.getSanitize()) {
        spec = spec.removeUrlViews();
      }

      featureRegistry = featureRegistryProvider.get(context.getRepository());
    } catch (GadgetException e) {
      throw new ProcessingException(e.getMessage(), e, e.getHttpStatusCode());
    }

    return new Gadget()
        .setContext(context)
        .setGadgetFeatureRegistry(featureRegistry)
        .setSpec(spec)
        .setCurrentView(getView(context, spec));
  }

  /**
   * Attempts to extract the "current" view for the given gadget.
   *
   * There is common container JavaScript code that performs this same type of aliasing check before
   * render. If the common container is being used, the view should never have to be aliased here.
   */
  private View getView(GadgetContext context, GadgetSpec spec) {
    String viewName = context.getView();
    View view = spec.getView(viewName);
    if (view == null) {
      String container = context.getContainer();
      String property = "${Cur['gadgets.features'].views['" + viewName + "'].aliases}";
      for (Object alias : containerConfig.getList(container, property)) {
        viewName = alias.toString();
        view = spec.getView(viewName);
        if (view != null) {
          return view;
        }
      }
    }
    if (view == null) {
      view = spec.getView(GadgetSpec.DEFAULT_VIEW);
    }
    return view;
  }
}
