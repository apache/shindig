/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.shindig.gadgets.http;

import org.apache.shindig.gadgets.BasicGadgetDataCache;
import org.apache.shindig.gadgets.BasicRemoteContentFetcher;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetDataCache;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.GadgetFeatureRegistry;
import org.apache.shindig.gadgets.GadgetServer;
import org.apache.shindig.gadgets.GadgetSpec;
import org.apache.shindig.gadgets.GadgetView;
import org.apache.shindig.gadgets.JsLibrary;
import org.apache.shindig.gadgets.MessageBundle;
import org.apache.shindig.gadgets.RemoteContentFetcher;
import org.apache.shindig.gadgets.UserPrefs;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet for rendering Gadgets, typically in an IFRAME.
 */
public class GadgetRenderingServlet extends HttpServlet {
  private final GadgetServer gadgetServer;
  private final String jsServicePath;
  private static final String USERPREF_PARAM_PREFIX = "up_";
  private static final String LIBS_PARAM_NAME = "libs";
  private static final String JS_FILE_SUFFIX = ".js";
  public static final String DEFAULT_JS_SERVICE_PATH = "js/";

  static {
    GadgetFeatureRegistry.register("analytics", null, AnalyticsFeature.class);
  }

  /**
   * Creates a {@code GadgetRenderingServlet} with default executor,
   * caches, etc.
   */
  public GadgetRenderingServlet() {
    this(Executors.newCachedThreadPool(),
         new BasicGadgetDataCache<MessageBundle>(),
         new BasicGadgetDataCache<GadgetSpec>(),
         new BasicRemoteContentFetcher(1024 * 1024));
  }

  /**
   * A la carte rendering server creation.
   * @param executor
   * @param mbCache
   * @param specCache
   * @param fetcher
   */
  public GadgetRenderingServlet(Executor executor,
                                GadgetDataCache<MessageBundle> mbCache,
                                GadgetDataCache<GadgetSpec> specCache,
                                RemoteContentFetcher fetcher) {
    gadgetServer = new GadgetServer(executor);
    gadgetServer.setMessageBundleCache(mbCache);
    gadgetServer.setSpecCache(specCache);
    gadgetServer.setContentFetcher(fetcher);
    // TODO: make injectable
    jsServicePath = DEFAULT_JS_SERVICE_PATH;
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws IOException {
    String urlParam = req.getParameter("url");
    if (urlParam == null) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                     "Missing required parameter: url");
      return;
    }

    URI uri = null;
    try {
      uri = new URI(urlParam);
    } catch (URISyntaxException e) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                     String.format("Malformed URL %s, reason: %s",
                                   urlParam,
                                   e.getMessage()));
      return;
    }

    int moduleId = 0;
    String mid = req.getParameter("mid");
    if (mid != null) {
      moduleId = Integer.parseInt(mid);
    }

    BasicHttpContext context = new BasicHttpContext(req);
    GadgetView.ID gadgetId = new Gadget.GadgetId(uri, moduleId);

    Gadget gadget = null;
    try {
      gadget = gadgetServer.processGadget(gadgetId,
                                          getPrefsFromRequest(req),
                                          context.getLocale(),
                                          GadgetServer.RenderingContext.GADGET);
    } catch (GadgetServer.GadgetProcessException e) {
      outputErrors(e, resp);
    }

    outputGadget(gadget, resp);
  }

  private void outputGadget(Gadget gadget, HttpServletResponse resp)
      throws IOException {
    switch(gadget.getContentType()) {
    case HTML:
      outputHtmlGadget(gadget, resp);
      break;
    case URL:
      outputUrlGadget(gadget, resp);
      break;
    default:
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                     "Unexpected reror: unknown gadget type");
      break;
    }
  }

  private void outputHtmlGadget(Gadget gadget, HttpServletResponse resp)
      throws IOException {
    resp.setContentType("text/html");

    StringBuilder markup = new StringBuilder();
    markup.append("<html><head>");
    markup.append("<style type=\"text/css\">" +
                  "body,td,div,span,p{font-family:arial,sans-serif;}" +
                  "a {color:#0000cc;}a:visited {color:#551a8b;}" +
                  "a:active {color:#ff0000;}" +
                  "body{margin: 0px;padding: 0px;background-color:white;}" +
                  "</style>");
    markup.append("</head><body>");
    for (JsLibrary library : gadget.getJsLibraries()) {
      markup.append(library.toString());
    }
    JSONObject json = new JSONObject();
    try {
      json.put("proxyUrl", "http://www.gmodules.com/ig/proxy?url=%url%");
      json.put("jsonProxyUrl", "/gadgets/proxy?url=%url%&output=js");
    } catch (JSONException e) {
      // This shouldn't ever happen.
    }
    markup.append("<script>gadgets.IO.init(" +json.toString()+ ");</script>");
    markup.append(gadget.getContentData());
    // TODO: use renamespaced API

    markup.append("</body></html>");

    resp.getOutputStream().print(markup.toString());
  }

  private void outputUrlGadget(Gadget gadget, HttpServletResponse resp)
      throws IOException {
    // UserPrefs portion of query string to tack on
    // TODO: generalize this as injectedArgs on Gadget object
    // TODO: userprefs on the fragment rather than query string
    String prefsQuery = getPrefsQueryString(gadget.getUserPrefValues());
    String libsQuery = null;
    try {
      libsQuery = getLibsQueryString(gadget.getJsLibraries());
    } catch (GadgetException e) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
      return;
    }

    URI redirURI = gadget.getContentHref();
    try {
      redirURI = new URI(redirURI.getScheme(),
                         redirURI.getUserInfo(),
                         redirURI.getHost(),
                         redirURI.getPort(),
                         redirURI.getPath(),
                         redirURI.getQuery() + prefsQuery + libsQuery,
                         redirURI.getFragment());
    } catch (URISyntaxException e) {
      // Not really ever going to happen; input values are already OK.
      resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                     e.getMessage());
    }
    resp.sendRedirect(redirURI.toString());
  }

  private void outputErrors(GadgetServer.GadgetProcessException errs,
                            HttpServletResponse resp)
      throws IOException {
    // TODO: make this way more robust
    StringBuilder markup = new StringBuilder();
    markup.append("<html><body>");
    markup.append("<pre>");
    for (GadgetException error : errs.getComponents()) {
      markup.append(error.getCode().toString());
      markup.append("\n");
    }
    markup.append("</pre>");
    markup.append("</body></html>");
    resp.getOutputStream().print(markup.toString());
  }

  private UserPrefs getPrefsFromRequest(HttpServletRequest req) {
    Map<String, String> prefs = new HashMap<String, String>();
    Enumeration paramNames = req.getParameterNames();
    while (paramNames.hasMoreElements()) {
      String paramName = (String)paramNames.nextElement();
      if (paramName.startsWith(USERPREF_PARAM_PREFIX)) {
        String prefName = paramName.substring(USERPREF_PARAM_PREFIX.length());
        prefs.put(prefName, req.getParameter(paramName));
      }
    }
    return new UserPrefs(prefs);
  }

  private String getPrefsQueryString(Map<String, String> prefVals) {
    StringBuilder builder = new StringBuilder();
    for (Map.Entry<String, String> prefEntry : prefVals.entrySet()) {
      builder.append("&");
      try {
        builder.append(USERPREF_PARAM_PREFIX +
                       URLEncoder.encode(prefEntry.getKey(), "UTF8") +
                       "=" +
                       URLEncoder.encode(prefEntry.getValue(), "UTF8"));
      } catch (UnsupportedEncodingException e) {
        // If UTF8 is somehow not supported, we may as well bail.
        // Not a whole lot we can do without such support.
        throw new RuntimeException("Unexpected error: UTF8 not supported.");
      }
    }
    return builder.toString();
  }

  private String getLibsQueryString(List<JsLibrary> jsLibs)
      throws GadgetException {
    StringBuilder libBuilder = new StringBuilder();
    libBuilder.append("&").append(LIBS_PARAM_NAME).append("=");
    libBuilder.append(jsServicePath);
    StringBuilder errBuilder = new StringBuilder();
    int numErrs = 0;
    for (int i = 0; i < jsLibs.size(); ++i) {
      if (i > 0) {
        // Builds a consolidated lib spec, collapsing several features into
        // a single <script src="libspec"/> tag load for type=URL gadgets
        libBuilder.append(JsLibrary.ALIAS_SEPARATOR);
      }
      JsLibrary jsLib = jsLibs.get(i);
      String alias = jsLib.getAlias();
      if (alias == null) {
        numErrs++;
        errBuilder.append(" ").append(jsLib.getFeature());
      }
    }
    libBuilder.append(JS_FILE_SUFFIX);

    if (numErrs > 0) {
      throw new GadgetException(
          GadgetException.Code.UNSUPPORTED_FEATURE,
          String.format("The following feature%s not supported " +
                        "for type=url gadgets:%s",
                        numErrs == 1 ? " is" : "s are",
                        errBuilder.toString()));
    }

    return libBuilder.toString();
  }
}
