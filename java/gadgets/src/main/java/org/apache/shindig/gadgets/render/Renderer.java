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

import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.LockedDomainService;
import org.apache.shindig.gadgets.process.ProcessingException;
import org.apache.shindig.gadgets.process.Processor;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.View;

import com.google.inject.Inject;

import java.net.URI;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Validates a rendering request parameters before calling an appropriate renderer.
 */
public class Renderer {
  private static final Logger LOG = Logger.getLogger(Renderer.class.getName());
  private final Processor processor;
  private final HtmlRenderer renderer;
  private final ContainerConfig containerConfig;
  private final LockedDomainService lockedDomainService;

  @Inject
  public Renderer(Processor processor,
                  HtmlRenderer renderer,
                  ContainerConfig containerConfig,
                  LockedDomainService lockedDomainService) {
    this.processor = processor;
    this.renderer = renderer;
    this.containerConfig = containerConfig;
    this.lockedDomainService = lockedDomainService;
  }

  /**
   * Attempts to render the requested gadget.
   *
   * @return The results of the rendering attempt.
   *
   * TODO: Localize error messages.
   */
  public RenderingResults render(GadgetContext context) {
    if (!validateParent(context)) {
      return RenderingResults.error("Unsupported parent parameter. Check your container code.");
    }

    try {
      Gadget gadget = processor.process(context);

      if (gadget.getCurrentView() == null) {
        return RenderingResults.error("Unable to locate an appropriate view in this gadget. " +
            "Requested: '" + gadget.getContext().getView() +
            "' Available: " + gadget.getSpec().getViews().keySet());
      }

      if (gadget.getCurrentView().getType() == View.ContentType.URL) {
        return RenderingResults.mustRedirect(gadget.getCurrentView().getHref());
      }

      GadgetSpec spec = gadget.getSpec();
      if (!lockedDomainService.gadgetCanRender(context.getHost(), spec, context.getContainer())) {
        return RenderingResults.error("Invalid domain");
      }

      return RenderingResults.ok(renderer.render(gadget));
    } catch (RenderingException e) {
      return logError(context.getUrl(), e);
    } catch (ProcessingException e) {
      return logError(context.getUrl(), e);
    } catch (RuntimeException e) {
      if (e.getCause() instanceof GadgetException) {
        return logError(context.getUrl(), e.getCause());
      }
      throw e;
    }
  }

  private RenderingResults logError(URI gadgetUrl, Throwable t) {
    LOG.info("Failed to render gadget " + gadgetUrl + ": " + t.getMessage());
    return RenderingResults.error(t.getMessage());
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

    List<Object> parents = containerConfig.getList(container, "gadgets.parent");
    if (parents.size() == 0) {
      // Allow all.
      return true;
    }

    // We need to check each possible parent parameter against this regex.
    for (Object pattern : parents) {
      if (Pattern.matches(pattern.toString(), parent)) {
        return true;
      }
    }

    return false;
  }
}
