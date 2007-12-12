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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class BasicGadgetHttpServlet extends HttpServlet {
  private static final Executor executor = Executors.newCachedThreadPool();
  private static final GadgetDataCache<MessageBundle> messageBundleCache
      = new BasicGadgetDataCache<MessageBundle>();
  private static final GadgetDataCache<GadgetSpec> specCache
      = new BasicGadgetDataCache<GadgetSpec>();
  private static final RemoteContentFetcher fetcher
      = new BasicRemoteContentFetcher(1024 * 1024);
  private final GadgetServer gadgetServer;

  static {
    GadgetFeatureRegistry.register("analytics", null, AnalyticsFeature.class);
  }

  public BasicGadgetHttpServlet() {
    gadgetServer = new GadgetServer(executor);
    gadgetServer.setMessageBundleCache(messageBundleCache);
    gadgetServer.setSpecCache(specCache);
    gadgetServer.setContentFetcher(fetcher);
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws IOException {
    String urlParam = req.getParameter("url");
    if (urlParam == null) {
      // TODO: emit some sort of reasonable result - 404?
    }

    URL url = null;
    try {
      url = new URL(urlParam);
    } catch (MalformedURLException e) {
      // TODO: emit appropriate error
    }

    int moduleId = 0;
    String mid = req.getParameter("mid");
    if (mid != null) {
      moduleId = Integer.parseInt(mid);
    }

    BasicHttpContext context = new BasicHttpContext(req);
    GadgetView.ID gadgetId = new Gadget.GadgetId(url, moduleId);

    Gadget gadget = null;
    try {
      gadget = gadgetServer.processGadget(gadgetId,
                                          UserPrefs.EMPTY,
                                          context.getLocale(),
                                          GadgetServer.RenderingContext.GADGET);
    } catch (GadgetServer.GadgetProcessException e) {
      outputErrors(e, resp);
    }

    outputGadget(gadget, resp);
  }

  private void outputGadget(Gadget gadget, HttpServletResponse resp)
      throws IOException {
    resp.setContentType("text/html");
    StringBuilder markup = new StringBuilder();
    markup.append("<html><body>");
    for (JsLibrary library : gadget.getJsLibraries()) {
      markup.append(library.toString());
    }
    // TODO: support type=URL?
    markup.append(gadget.getContentData());
    markup.append("<script>_IG_TriggerEvent(\"domload\");</script>");
    markup.append("</body></html>");
    resp.getOutputStream().print(markup.toString());
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
}

