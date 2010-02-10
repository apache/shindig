/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.gadgets.process;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetBlacklist;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.GadgetSpecFactory;
import org.apache.shindig.gadgets.features.FeatureRegistry;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.View;
import org.apache.shindig.gadgets.variables.VariableSubstituter;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;

/**
 * Converts an input Context into an output Gadget.
 */
@Singleton
public class Processor {
  private static final Logger LOG = Logger.getLogger(Processor.class.getName());
  private final GadgetSpecFactory gadgetSpecFactory;
  private final VariableSubstituter substituter;
  private final ContainerConfig containerConfig;
  private final GadgetBlacklist blacklist;
  private final FeatureRegistry featureRegistry;

  @Inject
  public Processor(GadgetSpecFactory gadgetSpecFactory,
                   VariableSubstituter substituter,
                   ContainerConfig containerConfig,
                   GadgetBlacklist blacklist,
                   FeatureRegistry featureRegistry) {
    this.gadgetSpecFactory = gadgetSpecFactory;
    this.substituter = substituter;
    this.blacklist = blacklist;
    this.containerConfig = containerConfig;
    this.featureRegistry = featureRegistry;
  }

  /**
   * Process a single gadget. Creates a gadget from a retrieved GadgetSpec and context object,
   * automatically performing variable substitution on the spec for use elsewhere.
   *
   * @throws ProcessingException If there is a problem processing the gadget.
   */
  public Gadget process(GadgetContext context) throws ProcessingException {
    Uri url = context.getUrl();

    if (url == null) {
      throw new ProcessingException("Missing or malformed url parameter",
          HttpServletResponse.SC_BAD_REQUEST);
    }

    if (!"http".equalsIgnoreCase(url.getScheme()) && !"https".equalsIgnoreCase(url.getScheme())) {
      throw new ProcessingException("Unsupported scheme (must be http or https).",
          HttpServletResponse.SC_FORBIDDEN);
    }

    if (blacklist.isBlacklisted(context.getUrl())) {
      LOG.info("Attempted to render blacklisted gadget: " + context.getUrl());
      throw new ProcessingException("The requested gadget is unavailable",
          HttpServletResponse.SC_FORBIDDEN);
    }

    try {
      GadgetSpec spec = gadgetSpecFactory.getGadgetSpec(context);
      spec = substituter.substitute(context, spec);

      return new Gadget()
          .setContext(context)
          .setGadgetFeatureRegistry(featureRegistry)
          .setSpec(spec)
          .setCurrentView(getView(context, spec));
    } catch (GadgetException e) {
      throw new ProcessingException(e.getMessage(), e, e.getHttpStatusCode());
    }
  }

  /**
   * Attempts to extract the "current" view for the given gadget.
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
