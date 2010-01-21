/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.gadgets.servlet;

import org.apache.shindig.common.servlet.HttpUtil;
import org.apache.shindig.common.servlet.InjectedServlet;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.UrlGenerator;
import org.apache.shindig.gadgets.UrlValidationStatus;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.render.Renderer;
import org.apache.shindig.gadgets.render.RenderingResults;

import com.google.inject.Inject;

import org.apache.commons.lang.StringEscapeUtils;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet for rendering Gadgets.
 */
public class GadgetRenderingServlet extends InjectedServlet {
  static final int DEFAULT_CACHE_TTL = 60 * 5;
  private Renderer renderer;
  private UrlGenerator urlGenerator;

  @Inject
  public void setRenderer(Renderer renderer) {
    this.renderer = renderer;
  }
  
  @Inject
  public void setUrlGenerator(UrlGenerator gadgetUrlGenerator) {
    this.urlGenerator = gadgetUrlGenerator;
  }

  private void render(HttpServletRequest req, HttpServletResponse resp, UrlValidationStatus urlstatus)
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
    switch (results.getStatus()) {
      case OK:
        if (context.getIgnoreCache() ||
            urlstatus == UrlValidationStatus.INVALID) {
          HttpUtil.setCachingHeaders(resp, 0);
        } else if (urlstatus == UrlValidationStatus.VALID_VERSIONED) {
          // Versioned files get cached indefinitely
          HttpUtil.setCachingHeaders(resp, true);
        } else {
          // Unversioned files get cached for 5 minutes by default, but this can be overridden
          // with a query parameter.
          int ttl = DEFAULT_CACHE_TTL;
          String ttlStr = req.getParameter(ProxyBase.REFRESH_PARAM);
          if (ttlStr != null) {
            ttl = Integer.parseInt(ttlStr);
          }
          HttpUtil.setCachingHeaders(resp, ttl, true);
        }
        resp.getWriter().print(results.getContent());
        break;
      case ERROR:
        resp.setStatus(results.getHttpStatusCode());
        resp.getWriter().print(StringEscapeUtils.escapeHtml(results.getErrorMessage()));
        break;
      case MUST_REDIRECT:
        resp.sendRedirect(results.getRedirect().toString());
        break;
    }
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    // If an If-Modified-Since header is ever provided, we always say
    // not modified. This is because when there actually is a change,
    // cache busting should occur.
    UrlValidationStatus urlstatus = getUrlStatus(req);
    if (req.getHeader("If-Modified-Since") != null &&
        !"1".equals(req.getParameter("nocache")) &&
        urlstatus == UrlValidationStatus.VALID_VERSIONED) {
      resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
      return;
    }
    render(req, resp, urlstatus);
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    render(req, resp, getUrlStatus(req));
  }
  
  private UrlValidationStatus getUrlStatus(HttpServletRequest req) {
    return urlGenerator.validateIframeUrl(
        req.getRequestURL().append('?').append(req.getQueryString()).toString());
  }
}