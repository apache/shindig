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
import org.apache.shindig.gadgets.GadgetContentFilter;
import org.apache.shindig.gadgets.GadgetDataCache;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.GadgetFeatureRegistry;
import org.apache.shindig.gadgets.GadgetServer;
import org.apache.shindig.gadgets.GadgetSpec;
import org.apache.shindig.gadgets.GadgetView;
import org.apache.shindig.gadgets.JsFeatureLoader;
import org.apache.shindig.gadgets.JsLibrary;
import org.apache.shindig.gadgets.MessageBundle;
import org.apache.shindig.gadgets.ProcessingOptions;
import org.apache.shindig.gadgets.RemoteContentFetcher;
import org.apache.shindig.gadgets.RenderingContext;
import org.apache.shindig.gadgets.UserPrefs;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet for rendering Gadgets, typically in an IFRAME.
 */
public class GadgetRenderingServlet extends HttpServlet {
  private GadgetServer gadgetServer;
  private String jsServicePath;
  private GadgetFeatureRegistry registry;
  private static final String CAJA_PARAM = "caja";
  private static final String USERPREF_PARAM_PREFIX = "up_";
  private static final String LIBS_PARAM_NAME = "libs";
  private static final String JS_FILE_SUFFIX = ".js";
  public static final String DEFAULT_JS_SERVICE_PATH = "js/";

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

  @Override
  @SuppressWarnings("unchecked")
  public void init(ServletConfig config) {
    ServletContext context = config.getServletContext();
    String coreFeatures = context.getInitParameter("core-js-features");
    String otherFeatures = context.getInitParameter("other-js-features");
    String jsPath = context.getInitParameter("js-service-path");
    if (jsPath == null) {
      jsPath = DEFAULT_JS_SERVICE_PATH;
    }
    jsServicePath = jsPath;
    try {
      registry = new GadgetFeatureRegistry(coreFeatures);
      gadgetServer.setGadgetFeatureRegistry(registry);
      if (otherFeatures != null) {
        JsFeatureLoader jsLoader = new JsFeatureLoader();
        jsLoader.loadFeatures(otherFeatures, registry);
      }
    } catch (GadgetException e) {
      e.printStackTrace();
      System.exit(1);
    }
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
    ProcessingOptions options = new ProcessingOptions();
    options.ignoreCache = getIgnoreCache(req);
    options.forceJsLibs = getForceJsLibs(req);

    // Prepare a list of GadgetContentFilters applied to the output
    List<GadgetContentFilter> contentFilters =
        new LinkedList<GadgetContentFilter>();
    if (getUseCaja(req)) {
      contentFilters.add(new CajaContentFilter(uri));
    }

    Gadget gadget = null;
    try {
      gadget = gadgetServer.processGadget(gadgetId,
                                          getPrefsFromRequest(req),
                                          context.getLocale(),
                                          RenderingContext.GADGET,
                                          options);
      outputGadget(gadget, options, contentFilters, resp);
    } catch (GadgetServer.GadgetProcessException e) {
      outputErrors(e, resp);
    }
  }

  /**
   * Renders a successfully processed gadget.
   *
   * @param gadget
   * @param options
   * @param contentFilters
   * @param resp
   * @throws IOException
   * @throws GadgetServer.GadgetProcessException
   */
  private void outputGadget(Gadget gadget,
                            ProcessingOptions options,
                            List<GadgetContentFilter> contentFilters,
                            HttpServletResponse resp)
      throws IOException, GadgetServer.GadgetProcessException {
    switch(gadget.getContentType()) {
    case HTML:
      outputHtmlGadget(gadget, options, contentFilters, resp);
      break;
    case URL:
      outputUrlGadget(gadget, options, resp);
      break;
    default:
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                     "Unexpected reror: unknown gadget type");
      break;
    }
  }

  /**
   * Handles type=html gadget output.
   *
   * @param gadget
   * @param options
   * @param contentFilters
   * @param resp
   * @throws IOException
   * @throws GadgetServer.GadgetProcessException
   */
  private void outputHtmlGadget(Gadget gadget,
                                ProcessingOptions options,
                                List<GadgetContentFilter> contentFilters,
                                HttpServletResponse resp)
      throws IOException, GadgetServer.GadgetProcessException {
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
    StringBuilder externJs = new StringBuilder();
    StringBuilder inlineJs = new StringBuilder();
    String externFmt = "<script src=\"%s\"></script>\n";

    for (JsLibrary library : gadget.getJsLibraries()) {
      if (library.getType() == JsLibrary.Type.URL) {
        // TODO: This case needs to be handled more gracefully by the js
        // servlet. We should probably inline external JS as well.
        externJs.append(String.format(externFmt, library.getContent()));
      } else if (options.forceJsLibs == null) {
        inlineJs.append(library.getContent()).append("\n");
      }
    }

    if (options.forceJsLibs != null) {
      externJs.append(String.format(externFmt,
          DEFAULT_JS_SERVICE_PATH + options.forceJsLibs + JS_FILE_SUFFIX));
    }

    if (inlineJs.length() > 0) {
      markup.append("<script><!--\n").append(inlineJs)
            .append("\n-->\n</script>");
    }

    if (externJs.length() > 0) {
      markup.append(externJs);
    }

    List<GadgetException> gadgetExceptions = new LinkedList<GadgetException>();
    String content = gadget.getContentData();
    for (GadgetContentFilter filter : contentFilters) {
      try {
        content = filter.filter(content);
      } catch (GadgetException e) {
        gadgetExceptions.add(e);
      }
    }
    if (gadgetExceptions.size() > 0) {
      throw new GadgetServer.GadgetProcessException(gadgetExceptions);
    }

    markup.append(content);
    markup.append("<script>gadgets.util.runOnLoadHandlers();</script>");
    markup.append("</body></html>");

    resp.getOutputStream().print(markup.toString());
  }

  private void outputUrlGadget(Gadget gadget,
      ProcessingOptions options, HttpServletResponse resp) throws IOException {
    // UserPrefs portion of query string to tack on
    // TODO: generalize this as injectedArgs on Gadget object
    // TODO: userprefs on the fragment rather than query string
    String prefsQuery = getPrefsQueryString(gadget.getUserPrefValues());
    String libsQuery = null;

    if (options.forceJsLibs == null) {
      libsQuery = getLibsQueryString(gadget.getRequires().keySet());
    } else {
      libsQuery
          = DEFAULT_JS_SERVICE_PATH + options.forceJsLibs + JS_FILE_SUFFIX;
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
      markup.append(" ");
      markup.append(error.getMessage().toString());
      markup.append("\n");
    }
    markup.append("</pre>");
    markup.append("</body></html>");
    resp.getOutputStream().print(markup.toString());
  }

  @SuppressWarnings("unchecked")
  private UserPrefs getPrefsFromRequest(HttpServletRequest req) {
    Map<String, String> prefs = new HashMap<String, String>();
    Enumeration<String> paramNames = req.getParameterNames();
    while (paramNames.hasMoreElements()) {
      String paramName = paramNames.nextElement();
      if (paramName.startsWith(USERPREF_PARAM_PREFIX)) {
        String prefName = paramName.substring(USERPREF_PARAM_PREFIX.length());
        prefs.put(prefName, req.getParameter(paramName));
      }
    }
    return new UserPrefs(prefs);
  }

  private String getPrefsQueryString(UserPrefs prefVals) {
    StringBuilder buf = new StringBuilder();
    for (Map.Entry<String, String> prefEntry : prefVals.getPrefs().entrySet()) {
      buf.append("&");
      try {
        buf.append(USERPREF_PARAM_PREFIX)
               .append(URLEncoder.encode(prefEntry.getKey(), "UTF8"))
               .append("=")
               .append(URLEncoder.encode(prefEntry.getValue(), "UTF8"));
      } catch (UnsupportedEncodingException e) {
        // If UTF8 is somehow not supported, we may as well bail.
        // Not a whole lot we can do without such support.
        throw new RuntimeException("Unexpected error: UTF8 not supported.");
      }
    }
    return buf.toString();
  }

  private String getLibsQueryString(Set<String> features) {
    StringBuilder buf = new StringBuilder();
    buf.append("&").append(LIBS_PARAM_NAME).append("=");
    buf.append(jsServicePath);
    if (features.size() == 0) {
      buf.append("core");
    } else {
      boolean first = true;
      for (String feature : features) {
        if (first) {
          first = false;
        } else {
          buf.append(":");
        }
        buf.append(feature);
      }
    }
    buf.append(JS_FILE_SUFFIX);

    return buf.toString();
  }

  /**
   * @param req
   * @return Whether or not to ignore the cache.
   */
  protected boolean getIgnoreCache(HttpServletRequest req) {
    String noCacheParam = req.getParameter("nocache");
    if (noCacheParam == null) {
      noCacheParam = req.getParameter("bpc");
    }
    return noCacheParam != null && noCacheParam.equals("1");
  }

  /**
   * @param req
   * @return Forced JS libs, or null if no forcing is to be done.
   */
  protected String getForceJsLibs(HttpServletRequest req) {
    return req.getParameter("libs");
  }

  /**
   * @param req
   * @return Whether or not to use caja.
   */
  protected boolean getUseCaja(HttpServletRequest req) {
    String cajaParam = req.getParameter(CAJA_PARAM);
    return cajaParam != null && cajaParam.equals("1");
  }
}
