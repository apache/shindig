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

package org.apache.shindig.gadgets.http;

import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContentFilter;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.GadgetFeatureRegistry;
import org.apache.shindig.gadgets.GadgetServerConfigReader;
import org.apache.shindig.gadgets.JsLibrary;
import org.apache.shindig.gadgets.SyndicatorConfig;
import org.apache.shindig.gadgets.spec.Feature;
import org.apache.shindig.gadgets.spec.LocaleSpec;
import org.apache.shindig.gadgets.spec.MessageBundle;
import org.apache.shindig.gadgets.spec.ModulePrefs;
import org.apache.shindig.gadgets.spec.View;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Handles processing a single http request
 */
public class GadgetRenderer {
  private static final String CAJA_PARAM = "caja";
  private static final String LIBS_PARAM_NAME = "libs";
  private static final Logger logger
      = Logger.getLogger("org.apache.shindig.gadgets");
  public static final String STRICT_MODE_DOCTYPE = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">";

  private final HttpServletRequest request;
  private final HttpServletResponse response;
  private final CrossServletState state;
  private final GadgetContext context;
  private final List<GadgetContentFilter> filters;

  /**
   * Processes a single rendering request and produces output html or errors.
   *
   * @throws IOException
   */
  public void process() throws IOException {
    URI url = context.getUrl();

    if (url == null) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST,
          "Missing or malformed url parameter");
      return;
    }

    if (!"http".equals(url.getScheme()) && !"https".equals(url.getScheme())) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                         "Unsupported scheme (must be http or https).");
      return;
    }

    if (!validateParent()) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST,
          "Unsupported parent parameter. Check your syndicator code.");
      return;
    }

    if (getUseCaja(request)) {
      filters.add(new CajaContentFilter(url));
    }

    try {
      Gadget gadget = state.getGadgetServer().processGadget(context);
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
   *
   * @param gadget
   * @throws IOException
   * @throws GadgetException
   */
  private void outputGadget(Gadget gadget) throws IOException, GadgetException {
    String viewName = context.getView();
    View view = gadget.getSpec().getView(viewName);
    if (view == null) {
      throw new GadgetException(GadgetException.Code.UNKNOWN_VIEW_SPECIFIED,
          "No appropriate view could be found for this gadget");
    }
    switch(view.getType()) {
      case HTML:
        outputHtmlGadget(gadget, view);
        break;
      case URL:
        outputUrlGadget(gadget, view);
        break;
    }
  }

  /**
   * Handles type=html gadget output.
   *
   * @param gadget
   * @param view
   * @throws IOException
   * @throws GadgetException
   */
  private void outputHtmlGadget(Gadget gadget, View view)
      throws IOException, GadgetException {
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

    StringBuilder externJs = new StringBuilder();
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
    if (libs.size() > 0) {
      String jsUrl = state.getJsUrl(libs, context);
      markup.append(String.format(externFmt, jsUrl));

      // Transitive dependencies must be added. This will always include core
      // so is therefore always "safe".
      Set<GadgetFeatureRegistry.Entry> deps
          = new HashSet<GadgetFeatureRegistry.Entry>();
      Set<String> dummy = new HashSet<String>();
      GadgetFeatureRegistry registry
          = state.getGadgetServer().getConfig().getFeatureRegistry();
      registry.getIncludedFeatures(libs, deps, dummy);
      for (GadgetFeatureRegistry.Entry dep : deps) {
        libs.add(dep.getName());
      }
    }

    // Inline any libs that weren't forced
    for (JsLibrary library : gadget.getJsLibraries()) {
      JsLibrary.Type type = library.getType();
      if (library.getType().equals(JsLibrary.Type.URL)) {
        externJs.append(String.format(externFmt, library.getContent()));
      } else {
        if (!libs.contains(library.getFeature())) {
          // already pulled this file in from the shared contents.
          if (context.getDebug()) {
            inlineJs.append(library.getDebugContent());
          } else {
            inlineJs.append(library.getContent());
          }
        }
      }
      inlineJs.append(";\n");
    }

    for (JsLibrary library : gadget.getJsLibraries()) {
      libs.add(library.getFeature());
    }

    appendJsConfig(gadget, libs, inlineJs);

    // message bundles for prefs object.
    MessageBundle bundle = gadget.getMessageBundle();

    String msgs = new JSONObject(bundle.getMessages()).toString();
    inlineJs.append("gadgets.Prefs.setMessages_(").append(msgs).append(");");

    if (inlineJs.length() > 0) {
      markup.append("<script><!--\n").append(inlineJs)
            .append("\n-->\n</script>");
    }

    if (externJs.length() > 0) {
      markup.append(externJs);
    }

    List<GadgetException> gadgetExceptions = new LinkedList<GadgetException>();

    String content = view.getContent();
    for (GadgetContentFilter filter : filters) {
      content = filter.filter(content);
    }

    markup.append(content)
          .append("<script>gadgets.util.runOnLoadHandlers();</script>")
          .append("</body></html>");
    if (request.getParameter("v") != null) {
      // Versioned files get cached indefinitely
      HttpUtil.setCachingHeaders(response);
    } else if ("1".equals(request.getParameter("nocache"))) {
      HttpUtil.setCachingHeaders(response, 0);
    } else {
      // Unversioned files get cached for 5 minutes.
      // TODO: This should be configurable
      HttpUtil.setCachingHeaders(response, 60 * 5);
    }
    response.getWriter().print(markup.toString());
  }

  /**
   * Outputs a url type gadget by redirecting.
   *
   * @param gadget
   * @param view
   * @throws IOException
   */
  private void outputUrlGadget(Gadget gadget,  View view) throws IOException {
    // TODO: generalize this as injectedArgs on Gadget object

    // Preserve existing query string parameters.
    URI href = view.getHref();
    String queryStr = href.getQuery();
    StringBuilder query = new StringBuilder(queryStr == null ? "" : queryStr);

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
                     query.toString(),
                     href.getFragment());
    } catch (URISyntaxException e) {
      // Not really ever going to happen; input values are already OK.
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                         e.getMessage());
    }
    response.sendRedirect(href.toString());
  }

  /**
   * Displays errors.
   * @param error
   * @throws IOException
   */
  private void outputErrors(GadgetException error) throws IOException {
    // Log the errors here for now. We might want different severity levels
    // for different error codes.
    logger.log(Level.INFO, "Failed to render gadget", error);
    String message = error.getMessage();
    if (message == null || message.length() == 0) {
      message = "Failed to render gadget: " + error.getCode().toString();
    }
    response.getWriter().print(message);
  }

  /**
   * Appends libs to the query string.
   * @param libs
   * @param query
   */
  private void appendLibsToQuery(Set<String> libs, StringBuilder query) {
    query.append('&')
         .append(LIBS_PARAM_NAME)
         .append('=')
         .append(state.getJsUrl(libs, context));
  }

  /**
   * @param req
   * @return Whether or not to use caja.
   */
  protected boolean getUseCaja(HttpServletRequest req) {
    String cajaParam = request.getParameter(CAJA_PARAM);
    return "1".equals(cajaParam);
  }

  /**
   * Appends javascript configuration to the bottom of an existing script block.
   *
   * Appends special configuration for gadgets.util.hasFeature and
   * gadgets.util.getFeatureParams to the output js.
   *
   * This can't be handled via the normal configuration mechanism because it is
   * something that varies per request.
   *
   * Only explicitly <Require>'d and <Optional> features will be added.
   *
   * @param gadget
   * @param reqs The features you require.
   * @param js Existing js, to which the configuration will be appended.
   */
  private void appendJsConfig(Gadget gadget, Set<String> reqs,
      StringBuilder js) {
    GadgetServerConfigReader config = state.getGadgetServer().getConfig();
    SyndicatorConfig syndConf = config.getSyndicatorConfig();
    JSONObject json = HttpUtil.getJsConfig(syndConf, context, reqs);
    // Add gadgets.util support. This is calculated dynamically based on
    // request inputs.
    ModulePrefs prefs = gadget.getSpec().getModulePrefs();
    JSONObject featureMap = new JSONObject();
    try {
      for (Feature feature : prefs.getFeatures().values()) {
        featureMap.put(feature.getName(), feature.getParams());
      }
      json.put("core.util", featureMap);
    } catch (JSONException e) {
      // Shouldn't be possible.
      throw new RuntimeException(e);
    }
    js.append("gadgets.config.init(").append(json.toString()).append(");");
  }

  /**
   * Validates that the parent parameter was acceptable.
   *
   * @return True if the parent parameter is valid for the current
   *     syndicator.
   */
  private boolean validateParent() {
    String syndicator = request.getParameter("synd");
    if (syndicator == null) {
      syndicator = SyndicatorConfig.DEFAULT_SYNDICATOR;
    }

    String parent = request.getParameter("parent");

    if (parent == null) {
      // If there is no parent parameter, we are still safe because no
      // dependent code ever has to trust it anyway.
      return true;
    }

    SyndicatorConfig syndConf
        = state.getGadgetServer().getConfig().getSyndicatorConfig();

    try {
      JSONArray parents = syndConf.getJsonArray(syndicator, "gadgets.parent");

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


  public GadgetRenderer(HttpServletRequest request,
                        HttpServletResponse response,
                        CrossServletState state) {
    this.request = request;
    this.response = response;
    this.state = state;
    context = new HttpGadgetContext(request);
    filters = new LinkedList<GadgetContentFilter>();
  }
}
