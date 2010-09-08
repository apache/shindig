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

import org.apache.commons.lang.StringEscapeUtils;

import org.apache.shindig.common.servlet.HttpUtil;
import org.apache.shindig.common.servlet.InjectedServlet;
import org.apache.shindig.common.uri.UriBuilder;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.uri.JsUriManager;
import org.apache.shindig.gadgets.uri.UriStatus;

import com.google.inject.Inject;

import java.io.IOException;
import java.util.regex.Pattern;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Simple servlet serving up JavaScript files by their registered aliases.
 * Used by type=URL gadgets in loading JavaScript resources.
 */
public class JsServlet extends InjectedServlet {
  
  private static final long serialVersionUID = 6255917470412008175L;

  static final String ONLOAD_JS_TPL = "(function() {" +
      "var nm='%s';" +
      "if (typeof window[nm]==='function') {" +
      "window[nm]();" +
      '}' +
      "})();";
  private static final Pattern ONLOAD_FN_PATTERN = Pattern.compile("[a-zA-Z0-9_]+");

  private transient JsHandler jsHandler;
  private transient JsUriManager jsUriManager;

  @Inject
  public void setJsHandler(JsHandler jsHandler) {
    checkInitialized();
    this.jsHandler = jsHandler;
  }

  @Inject
  public void setUrlGenerator(JsUriManager jsUriManager) {
    checkInitialized();
    this.jsUriManager = jsUriManager;
  }
  
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws IOException {
    // If an If-Modified-Since header is ever provided, we always say
    // not modified. This is because when there actually is a change,
    // cache busting should occur.
    UriStatus vstatus = null;
    try {
      vstatus = jsUriManager.processExternJsUri(new UriBuilder(req).toUri()).getStatus();
    } catch (GadgetException e) {
      resp.sendError(e.getHttpStatusCode(), e.getMessage());
      return;
    }
    if (req.getHeader("If-Modified-Since") != null &&
        vstatus == UriStatus.VALID_VERSIONED) {
      resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
      return;
    }

    // Get JavaScript content from features aliases request.
    JsHandler.JsHandlerResponse handlerResponse = jsHandler.getJsContent(req);
    StringBuilder jsData = handlerResponse.getJsData();
    boolean isProxyCacheable = handlerResponse.isProxyCacheable();

    // Add onload handler to add callback function.
    String onloadStr = req.getParameter("onload");
    if (onloadStr != null) {
      if (!ONLOAD_FN_PATTERN.matcher(onloadStr).matches()) {
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid onload callback specified");
        return;
      }
      jsData.append(String.format(ONLOAD_JS_TPL, StringEscapeUtils.escapeJavaScript(onloadStr)));
    }

    if (jsData.length() == 0) {
      resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    // post JavaScript content fetching
    postJsContentProcessing(resp, vstatus, isProxyCacheable);

    resp.setContentType("text/javascript; charset=utf-8");
    byte[] response = jsData.toString().getBytes("UTF-8");
    resp.setContentLength(response.length);
    resp.getOutputStream().write(response);
  }

  /**
   * Provides post JavaScript content processing. The default behavior will check the UriStatus and
   * update the response header with cache option.
   * 
   * @param resp The HttpServeltResponse object.
   * @param vstatus The UriStatus object.
   * @param isProxyCacheable boolean true if content is cacheable and false otherwise.
   */
  protected void postJsContentProcessing(HttpServletResponse resp, UriStatus vstatus,
      boolean isProxyCacheable) {
    switch (vstatus) {
      case VALID_VERSIONED:
        // Versioned files get cached indefinitely
        HttpUtil.setCachingHeaders(resp, !isProxyCacheable);
        break;
      case VALID_UNVERSIONED:
        // Unversioned files get cached for 1 hour.
        HttpUtil.setCachingHeaders(resp, 60 * 60, !isProxyCacheable);
        break;
      case INVALID_VERSION:
        // URL is invalid in some way, likely version mismatch.
        // Indicate no-cache forcing subsequent requests to regenerate URLs.
        HttpUtil.setNoCache(resp);
        break;
    }
  }
}
