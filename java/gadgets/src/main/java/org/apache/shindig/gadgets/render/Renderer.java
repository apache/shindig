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

import org.apache.shindig.common.logging.i18n.MessageKeys;
import org.apache.shindig.common.uri.Uri;
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

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Validates a rendering request parameters before calling an appropriate renderer.
 */
public class Renderer {
  //class name for logging purpose
  private static final String classname = Renderer.class.getName();
  private static final Logger LOG = Logger.getLogger(classname,MessageKeys.MESSAGES);

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
      return RenderingResults.error("Unsupported parent parameter. Check your container code.",
          HttpServletResponse.SC_BAD_REQUEST);
    }

    try {
      Gadget gadget = processor.process(context);

      GadgetSpec gadgetSpec = gadget.getSpec();
      if (gadget.getCurrentView() == null) {
        return RenderingResults.error("Unable to locate an appropriate view in this gadget. " +
            "Requested: '" + gadget.getContext().getView() +
            "' Available: " + gadgetSpec.getViews().keySet(), HttpServletResponse.SC_NOT_FOUND);
      }

      if (gadget.getCurrentView().getType() == View.ContentType.URL) {
        if (gadget.requiresCaja()) {
          return RenderingResults.error("Caja does not support url type gadgets.",
            HttpServletResponse.SC_BAD_REQUEST);
        } else if (gadget.sanitizeOutput()) {
          return RenderingResults.error("Type=url gadgets cannot be sanitized.",
            HttpServletResponse.SC_BAD_REQUEST);
        }
        return RenderingResults.mustRedirect(gadget.getCurrentView().getHref());
      }

      if (!lockedDomainService.isGadgetValidForHost(context.getHost(), gadget, context.getContainer())) {
        return RenderingResults.error("Invalid domain for host (" + context.getHost()
                + ") and gadget (" + gadgetSpec.getUrl() + ")",
                HttpServletResponse.SC_BAD_REQUEST);
      }

      return RenderingResults.ok(renderer.render(gadget));
    } catch (RenderingException e) {
      return logError("render", context.getUrl(), e.getHttpStatusCode(), e);
    } catch (ProcessingException e) {
      return logError("render", context.getUrl(), e.getHttpStatusCode(), e);
    } catch (RuntimeException e) {
      if (e.getCause() instanceof GadgetException) {
        return logError("render", context.getUrl(), ((GadgetException)e.getCause()).getHttpStatusCode(),
            e.getCause());
      }
      throw e;
    }
  }

  private RenderingResults logError(String methodname, Uri gadgetUrl, int statusCode, Throwable t) {
    if (LOG.isLoggable(Level.INFO)) {
      LOG.logp(Level.INFO, classname, methodname, MessageKeys.FAILED_TO_RENDER, new Object[] {gadgetUrl,t.getMessage()});
    }
    return RenderingResults.error(t.getMessage(), statusCode);
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
    if (parents.isEmpty()) {
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
