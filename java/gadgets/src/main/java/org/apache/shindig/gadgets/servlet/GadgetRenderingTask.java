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

package org.apache.shindig.gadgets.servlet;

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.auth.SecurityTokenDecoder;
import org.apache.shindig.common.ContainerConfig;
import org.apache.shindig.common.util.Utf8UrlCoder;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContentFilter;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.GadgetFeature;
import org.apache.shindig.gadgets.GadgetFeatureRegistry;
import org.apache.shindig.gadgets.GadgetServer;
import org.apache.shindig.gadgets.JsLibrary;
import org.apache.shindig.gadgets.LockedDomainService;
import org.apache.shindig.gadgets.MessageBundleFactory;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.spec.Feature;
import org.apache.shindig.gadgets.spec.LocaleSpec;
import org.apache.shindig.gadgets.spec.MessageBundle;
import org.apache.shindig.gadgets.spec.ModulePrefs;
import org.apache.shindig.gadgets.spec.Preload;
import org.apache.shindig.gadgets.spec.View;

import com.google.inject.Inject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Represents a single rendering task
 */
public class GadgetRenderingTask {

  protected static final int DEFAULT_CACHE_TTL = 60 * 5;
  protected static final String CAJA_PARAM = "caja";

  protected static final String LIBS_PARAM_NAME = "libs";

  protected static final Logger logger
      = Logger.getLogger("org.apache.shindig.gadgets");

  protected static final String STRICT_MODE_DOCTYPE
      = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">";

  private HttpServletRequest request;

  private HttpServletResponse response;

  private final GadgetServer server;

  private final MessageBundleFactory messageBundleFactory;

  private final GadgetFeatureRegistry registry;

  private final ContainerConfig containerConfig;

  private final UrlGenerator urlGenerator;

  private final SecurityTokenDecoder tokenDecoder;

  private GadgetContext context;

  private final List<GadgetContentFilter> filters;

  private final LockedDomainService domainLocker;

  private String container = null;

  /**
   * Processes a single rendering request and produces output html or errors.
   */
  public void process(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    this.request = request;
    this.response = response;
    context = new HttpGadgetContext(request, tokenDecoder);

    URI url = context.getUrl();

    if (url == null) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST,
          "Missing or malformed url parameter");
      return;
    }

    if (!"http".equalsIgnoreCase(url.getScheme()) && !"https".equalsIgnoreCase(url.getScheme())) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST,
          "Unsupported scheme (must be http or https).");
      return;
    }

    if (!validateParent()) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST,
          "Unsupported parent parameter. Check your container code.");
      return;
    }

    boolean addedCaja = false;
    if (getUseCaja(request)) {
      filters.add(new CajaContentFilter(url));
      addedCaja = true;
    }

    try {
      Gadget gadget = server.processGadget(context);
      if (!addedCaja &&
          gadget.getSpec().getModulePrefs().getFeatures().containsKey("caja")) {
        filters.add(new CajaContentFilter(url));
      }
      outputGadget(gadget);
    } catch (GadgetException e) {
      outputErrors(e);
    } catch (Exception e) {
      logger.log(Level.WARNING, "Unhandled exception ", e);
      outputErrors(new GadgetException(
          GadgetException.Code.INTERNAL_SERVER_ERROR,
          "There was a problem rendering the gadget: " + e.getMessage()));
    }
  }

  /**
   * Renders a successfully processed gadget.
   */
  private void outputGadget(Gadget gadget) throws IOException, GadgetException {
    View view = gadget.getView(containerConfig);
    if (view == null) {
      throw new GadgetException(GadgetException.Code.UNKNOWN_VIEW_SPECIFIED,
          "No appropriate view could be found for gadget: " + gadget.getSpec().getUrl());
    }
    switch (view.getType()) {
      case HTML:
        outputHtmlGadget(gadget, view);
        break;
      case URL:
        outputUrlGadget(gadget, view);
        break;
    }
  }

  /**
   * Redirect a type=html gadget to a locked domain if necessary.
   *
   * @return true if the request was handled, false if the request can proceed
   */
  private boolean mustRedirectToLockedDomain(Gadget gadget)
      throws IOException {

    String host = request.getHeader("Host");
    String container = context.getContainer();
    if (domainLocker.gadgetCanRender(host, gadget, container)) {
      return false;
    }

    // Gadget tried to render on wrong domain.
    String gadgetUrl = context.getUrl().toString();
    String required = domainLocker.getLockedDomainForGadget(
        gadgetUrl, container);
    String redir =
        request.getScheme() + "://" +
            required +
            request.getServletPath() + '?' +
            request.getQueryString();
    logger.info("Redirecting gadget " + context.getUrl() + " from domain " +
        host + " to domain " + redir);
    response.sendRedirect(redir);

    return true;
  }

  /**
   * Handles type=html gadget output.
   */
  private void outputHtmlGadget(Gadget gadget, View view)
      throws IOException, GadgetException {
    if (mustRedirectToLockedDomain(gadget)) {
      return;
    }

    response.setContentType("text/html; charset=UTF-8");
    StringBuilder markup = new StringBuilder();

    if (!view.getQuirks()) {
      markup.append(STRICT_MODE_DOCTYPE);
    }

    // TODO: Substitute gadgets.skins values in here.
    String boilerPlate
        = "<html><head><style type=\"text/css\">" +
        "body,td,div,span,p{font-family:arial,sans-serif;}" +
        "a {color:#0000cc;}a:visited {color:#551a8b;}" +
        "a:active {color:#ff0000;}" +
        "body{margin: 0px;padding: 0px;background-color:white;}" +
        "</style></head>";
    markup.append(boilerPlate);
    LocaleSpec localeSpec = gadget.getSpec().getModulePrefs().getLocale(
        gadget.getContext().getLocale());
    if (localeSpec == null) {
      markup.append("<body>");
    } else {
      markup.append("<body dir=\"")
          .append(localeSpec.getLanguageDirection())
          .append("\">");
    }

    StringBuilder inlineJs = new StringBuilder();
    String externFmt = "<script src=\"%s\"></script>";
    String forcedLibs = request.getParameter("libs");
    Set<String> libs = new HashSet<String>();
    if (forcedLibs != null) {
      if (forcedLibs.trim().length() == 0) {
        libs.add("core");
      } else {
        libs.addAll(Arrays.asList(forcedLibs.split(":")));
      }
    }

    // Forced libs are always done first.
    if (!libs.isEmpty()) {
      String jsUrl = urlGenerator.getBundledJsUrl(libs, context);
      markup.append(String.format(externFmt, jsUrl));

      // Transitive dependencies must be added. This will always include core
      // so is therefore always "safe".
      Collection<GadgetFeature> features = registry.getFeatures(libs);
      for (GadgetFeature dep : features) {
        libs.add(dep.getName());
      }
    }

    // Inline any libs that weren't forced
    for (JsLibrary library : gadget.getJsLibraries()) {
      if (library.getType().equals(JsLibrary.Type.URL)) {
        if (inlineJs.length() > 0) {
          markup.append("<script><!--\n").append(inlineJs)
              .append("\n-->\n</script>");
          inlineJs.setLength(0);
        }
        markup.append(String.format(externFmt, library.getContent()));
      } else {
        if (!libs.contains(library.getFeature())) {
          // already pulled this file in from the shared contents.
          if (context.getDebug()) {
            inlineJs.append(library.getDebugContent());
          } else {
            inlineJs.append(library.getContent());
          }
          inlineJs.append(";\n");
        }
      }
    }

    for (JsLibrary library : gadget.getJsLibraries()) {
      libs.add(library.getFeature());
    }

    appendJsConfig(gadget, libs, inlineJs);

    // message bundles for prefs object.
    GadgetContext context = gadget.getContext();
    MessageBundle bundle = messageBundleFactory.getBundle(
        gadget.getSpec(), context.getLocale(), context.getIgnoreCache());

    String msgs = new JSONObject(bundle.getMessages()).toString();
    inlineJs.append("gadgets.Prefs.setMessages_(").append(msgs).append(");");

    appendPreloads(gadget, inlineJs);

    if (inlineJs.length() > 0) {
      markup.append("<script><!--\n").append(inlineJs)
          .append("\n-->\n</script>");
    }

    String content = view.getContent();
    for (GadgetContentFilter filter : filters) {
      content = filter.filter(content);
    }

    markup.append(content)
        .append("<script>gadgets.util.runOnLoadHandlers();</script>")
        .append("</body></html>");
    if (context.getIgnoreCache()) {
      HttpUtil.setCachingHeaders(response, 0);
    } else if (request.getParameter("v") != null) {
      // Versioned files get cached indefinitely
      HttpUtil.setCachingHeaders(response, true);
    } else {
      // Unversioned files get cached for 5 minutes.
      // TODO: This should be configurable
      HttpUtil.setCachingHeaders(response, DEFAULT_CACHE_TTL, true);
    }
    response.getWriter().print(markup.toString());
  }

  /**
   * Outputs a url type gadget by redirecting.
   */
  private void outputUrlGadget(Gadget gadget, View view) throws IOException {
    // TODO: generalize this as injectedArgs on Gadget object

    // Preserve existing query string parameters.
    URI href = view.getHref();
    String queryStr = href.getQuery();
    StringBuilder query = new StringBuilder(queryStr == null ? "" : queryStr);
    String fragment = href.getFragment();

    // TODO: figure out a way to make this work with forced libs.
    Set<String> libs
        = gadget.getSpec().getModulePrefs().getFeatures().keySet();
    appendLibsToQuery(libs, query);

    try {
      href = new URI(href.getScheme(),
          href.getUserInfo(),
          href.getHost(),
          href.getPort(),
          href.getPath(),
          null,
          null);
    } catch (URISyntaxException e) {
      // Not really ever going to happen; input values are already OK.
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          e.getMessage());
    }
    // Necessary to avoid double-URL-encoding of the JavaScript bundle portion of the query.
    StringBuilder redirectHref = new StringBuilder(href.toString());
    if (query.toString() != null) {
      redirectHref.append('?');
      redirectHref.append(query.toString());
    }
    if (fragment != null) {
      redirectHref.append('#');
      redirectHref.append(fragment);
    }
    response.sendRedirect(redirectHref.toString());
  }

  /**
   * Displays errors for failed GadgetRendering. Also sets the caching headers to insure that our
   * server is not hurt by failing backends when nocache is not set.
   */
  private void outputErrors(GadgetException error) throws IOException {
    // Log the errors here for now. We might want different severity levels
    // for different error codes.
    logger.log(Level.INFO, "Failed to render gadget", error);
    String message = error.getMessage();
    if (message == null || message.length() == 0) {
      message = "Failed to render gadget: " + error.getCode().toString();
    }

    // cache this for 1 minute
    // TODO: make this a container specific configuration option
    if (!"1".equals(request.getParameter("nocache"))) {
      HttpUtil.setCachingHeaders(response, 60);
    }

    response.getWriter().print(message);
  }

  /**
   * Appends libs to the query string.
   */
  private void appendLibsToQuery(Set<String> libs, StringBuilder query) {
    query.append('&')
        .append(LIBS_PARAM_NAME)
        .append('=')
        .append(Utf8UrlCoder.encode(urlGenerator.getBundledJsParam(libs, context)));
  }

  /**
   * @return Whether or not to use caja.
   */
  protected boolean getUseCaja(HttpServletRequest req) {
    String cajaParam = request.getParameter(CAJA_PARAM);
    return "1".equals(cajaParam);
  }

  /**
   * Appends javascript configuration to the bottom of an existing script block.
   *
   * Appends special configuration for gadgets.util.hasFeature and gadgets.util.getFeatureParams to
   * the output js.
   *
   * This can't be handled via the normal configuration mechanism because it is something that
   * varies per request.
   *
   * Only explicitly <Require>'d and <Optional> features will be added.
   *
   * @param reqs The features you require.
   * @param js   Existing js, to which the configuration will be appended.
   * @throws GadgetException if there is a problem with the gadget auth token
   */
  private void appendJsConfig(Gadget gadget, Set<String> reqs,
      StringBuilder js) throws GadgetException {
    JSONObject json = HttpUtil.getJsConfig(containerConfig, context, reqs);
    // Add gadgets.util support. This is calculated dynamically based on
    // request inputs.
    ModulePrefs prefs = gadget.getSpec().getModulePrefs();
    JSONObject featureMap = new JSONObject();
    try {
      for (Feature feature : prefs.getFeatures().values()) {
        featureMap.put(feature.getName(), feature.getParams());
      }
      json.put("core.util", featureMap);

      // Add authentication token config
      SecurityToken authToken = context.getToken();
      if (authToken != null) {
        JSONObject authConfig = new JSONObject();
        String updatedToken = authToken.getUpdatedToken();
        if (updatedToken != null) {
          authConfig.put("authToken", updatedToken);
        }
        String trustedJson = authToken.getTrustedJson();
        if (trustedJson != null) {
          authConfig.put("trustedJson", trustedJson);
        }
        json.put("shindig.auth", authConfig);
      }
    } catch (JSONException e) {
      // Shouldn't be possible.
      throw new RuntimeException(e);
    }

    js.append("gadgets.config.init(").append(json.toString()).append(");\n");
  }

  /**
   * Appends data from <Preload> elements to make them available to gadgets.io.
   */
  private void appendPreloads(Gadget gadget, StringBuilder inlineJs) {
    // Output preloads. We will allow the gadget render to continue
    // even if a preload fails
    JSONObject resp = new JSONObject();
    for (Map.Entry<Preload, Future<HttpResponse>> entry
        : gadget.getPreloadMap().entrySet()) {
      Preload preload = entry.getKey();
      try {
        HttpResponse response = entry.getValue().get();
        // Use raw param as key as URL may have to be decoded
        JSONObject jsonEntry = new JSONObject();
        jsonEntry.put("body", response.getResponseAsString())
            .put("rc", response.getHttpStatusCode());
        // Merge in additional response data
        for (Map.Entry<String, String> metadata : response.getMetadata().entrySet()) {
          jsonEntry.put(metadata.getKey(), metadata.getValue());
        }
        resp.put(entry.getKey().getHref().toString(), jsonEntry);
      } catch (JSONException e) {
        logger.log(
            Level.INFO, "Error outputting preload for " + preload.getHref(), e);
      } catch (InterruptedException e) {
        logger.log(
            Level.INFO, "Error scheduling preload for " + preload.getHref(), e);
      } catch (ExecutionException e) {
        logger.log(Level.INFO,
            "Error executing preload for " + preload.getHref(), e.getCause());
      }
    }
    inlineJs.append("gadgets.io.preloaded_ = ").append(resp.toString())
        .append(";\n");
  }

  /**
   * Gets the container for the current request.
   */
  private String getContainerForRequest() {
    if (container != null) {
      return container;
    }
    container = request.getParameter("container");
    if (container == null) {
      // The parameter used to be called 'synd' FIXME: schedule removal
      container = request.getParameter("synd");
      if (container == null) {
        container = ContainerConfig.DEFAULT_CONTAINER;
      }
    }
    return container;
  }

  /**
   * Validates that the parent parameter was acceptable.
   *
   * @return True if the parent parameter is valid for the current container.
   */
  private boolean validateParent() {
    String container = getContainerForRequest();

    String parent = request.getParameter("parent");

    if (parent == null) {
      // If there is no parent parameter, we are still safe because no
      // dependent code ever has to trust it anyway.
      return true;
    }

    try {
      JSONArray parents
          = containerConfig.getJsonArray(container, "gadgets.parent");

      if (parents == null) {
        return true;
      } else {
        // We need to check each possible parent parameter against this regex.
        for (int i = 0, j = parents.length(); i < j; ++i) {
          // TODO: Should patterns be cached? Recompiling every request
          // seems wasteful.
          if (Pattern.matches(parents.getString(i), parent)) {
            return true;
          }
        }
      }
    } catch (JSONException e) {
      logger.log(Level.WARNING, "Configuration error", e);
    }
    return false;
  }

  @Inject
  public GadgetRenderingTask(GadgetServer server,
      MessageBundleFactory messageBundleFactory,
      GadgetFeatureRegistry registry,
      ContainerConfig containerConfig,
      UrlGenerator urlGenerator,
      SecurityTokenDecoder tokenDecoder,
      LockedDomainService lockedDomainService) {
    this.server = server;
    this.messageBundleFactory = messageBundleFactory;
    this.registry = registry;
    this.containerConfig = containerConfig;
    this.urlGenerator = urlGenerator;
    this.tokenDecoder = tokenDecoder;
    this.domainLocker = lockedDomainService;
    filters = new LinkedList<GadgetContentFilter>();
  }
}
