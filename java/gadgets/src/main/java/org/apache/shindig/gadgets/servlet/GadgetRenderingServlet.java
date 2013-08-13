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

import org.apache.shindig.common.logging.i18n.MessageKeys;
import org.apache.shindig.common.servlet.HttpUtil;
import org.apache.shindig.common.servlet.InjectedServlet;
import org.apache.shindig.common.uri.UriBuilder;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.render.Renderer;
import org.apache.shindig.gadgets.render.RenderingResults;
import org.apache.shindig.gadgets.uri.IframeUriManager;
import org.apache.shindig.gadgets.uri.UriStatus;
import org.apache.shindig.gadgets.uri.UriCommon.Param;

import com.google.common.base.Strings;
import com.google.inject.Inject;

import org.apache.commons.lang3.StringEscapeUtils;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet for rendering Gadgets.
 */
public class GadgetRenderingServlet extends InjectedServlet {

  private static final long serialVersionUID = -5634040113214794888L;

  static final int DEFAULT_CACHE_TTL = 60 * 5;

  //class name for logging purpose
  private static final String classname = GadgetRenderingServlet.class.getName();
  private static final Logger LOG = Logger.getLogger(classname,MessageKeys.MESSAGES);

  protected transient Renderer renderer;
  protected transient IframeUriManager iframeUriManager;

  @Inject
  public void setRenderer(Renderer renderer) {
    checkInitialized();
    this.renderer = renderer;
  }

  @Inject
  public void setIframeUriManager(IframeUriManager iframeUriManager) {
    checkInitialized();
    this.iframeUriManager = iframeUriManager;
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    // If an If-Modified-Since header is ever provided, we always say
    // not modified. This is because when there actually is a change,
    // cache busting should occur.
    UriStatus urlStatus = getUrlStatus(req);
    if (req.getHeader("If-Modified-Since") != null &&
        !"1".equals(req.getParameter("nocache")) &&
        urlStatus == UriStatus.VALID_VERSIONED) {
      resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
      return;
    }
    render(req, resp, urlStatus);
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    render(req, resp, getUrlStatus(req));
  }

  private void render(HttpServletRequest req, HttpServletResponse resp, UriStatus urlstatus)
      throws IOException {
    if (req.getHeader(HttpRequest.DOS_PREVENTION_HEADER) != null) {
      // Refuse to render for any request that came from us.
      // TODO: Is this necessary for any other type of request? Rendering seems to be the only one
      // that can potentially result in an infinite loop.
      resp.sendError(HttpServletResponse.SC_FORBIDDEN);
      return;
    }

    resp.setContentType("text/html");
    resp.setCharacterEncoding("UTF-8");

    GadgetContext context = new HttpGadgetContext(req);
    RenderingResults results = renderer.render(context);

    // process the rendering results
    postGadgetRendering(new PostGadgetRenderingParams(req, resp, urlstatus, context, results));
  }

  /**
   * Implementations that extend this class are strongly discouraged from overriding this method.
   * To customize the behavior please override the hook methods for each of the
   * RenderingResults.Status enum values instead.
   */
  protected void postGadgetRendering(PostGadgetRenderingParams params) throws IOException {
    switch (params.getResults().getStatus()) {
      case OK:
        onOkRenderingResultsStatus(params);
        break;
      case ERROR:
        onErrorRenderingResultsStatus(params);
        break;
      case MUST_REDIRECT:
        onMustRedirectRenderingResultsStatus(params);
        break;
    }
  }

  protected void onOkRenderingResultsStatus(PostGadgetRenderingParams params)
      throws IOException {
    UriStatus urlStatus = params.getUrlStatus();
    HttpServletResponse resp = params.getResponse();
    if (params.getContext().getIgnoreCache() ||
        urlStatus == UriStatus.INVALID_VERSION) {
      HttpUtil.setCachingHeaders(resp, 0);
    } else if (urlStatus == UriStatus.VALID_VERSIONED) {
      // Versioned files get cached indefinitely
      HttpUtil.setCachingHeaders(resp, true);
    } else {
      // Unversioned files get cached for 5 minutes by default, but this can be overridden
      // with a query parameter.
      int ttl = DEFAULT_CACHE_TTL;
      String ttlStr = params.getRequest().getParameter(Param.REFRESH.getKey());
      if (!Strings.isNullOrEmpty(ttlStr)) {
        try {
          ttl = Integer.parseInt(ttlStr);
        } catch (NumberFormatException e) {
          // Ignore malformed TTL value
          if (LOG.isLoggable(Level.INFO)) {
            LOG.logp(Level.INFO, classname, "onOkRenderingResultsStatus", MessageKeys.MALFORMED_TTL_VALUE, new Object[] {ttlStr});
          }
        }
      }
      HttpUtil.setCachingHeaders(resp, ttl, true);
    }
    resp.getWriter().print(params.getResults().getContent());
  }

  protected void onErrorRenderingResultsStatus(PostGadgetRenderingParams params)
      throws IOException {
    HttpServletResponse resp = params.getResponse();
    resp.setStatus(params.getResults().getHttpStatusCode());
    resp.getWriter().print(StringEscapeUtils.escapeHtml4(params.getResults().getErrorMessage()));
  }

  protected void onMustRedirectRenderingResultsStatus(PostGadgetRenderingParams params)
      throws IOException {
     params.getResponse().sendRedirect(params.getResults().getRedirect().toString());
  }

  private UriStatus getUrlStatus(HttpServletRequest req) {
    return iframeUriManager.validateRenderingUri(new UriBuilder(req).toUri());
  }

  /**
   * Contains the input parameters for post rendering methods.
   */
  protected static class PostGadgetRenderingParams {
    private HttpServletRequest req;
    private HttpServletResponse resp;
    private UriStatus urlStatus;
    private GadgetContext context;
    private RenderingResults results;

    public PostGadgetRenderingParams (HttpServletRequest req, HttpServletResponse resp,
      UriStatus urlStatus, GadgetContext context, RenderingResults results) {
      this.req = req;
      this.resp = resp;
      this.urlStatus = urlStatus;
      this.context = context;
      this.results = results;
    }

    public HttpServletRequest getRequest() {
      return req;
    }

    public HttpServletResponse getResponse() {
      return resp;
    }

    public UriStatus getUrlStatus() {
      return urlStatus;
    }

    public GadgetContext getContext() {
      return context;
    }

    public RenderingResults getResults() {
      return results;
    }
  }
}
