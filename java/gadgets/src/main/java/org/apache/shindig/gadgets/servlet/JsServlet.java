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

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.shindig.common.servlet.HttpUtil;
import org.apache.shindig.common.servlet.InjectedServlet;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.uri.UriBuilder;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.uri.JsUriManager;
import org.apache.shindig.gadgets.uri.UriStatus;
import org.apache.shindig.gadgets.uri.JsUriManager.JsUri;
import org.apache.shindig.gadgets.uri.UriCommon.Param;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Simple servlet serving up JavaScript files by their registered aliases.
 * Used by type=URL gadgets in loading JavaScript resources.
 */
public class JsServlet extends InjectedServlet {
  private static final long serialVersionUID = 6255917470412008175L;

  @VisibleForTesting
  static final String JSLOAD_ONLOAD_ERROR = "jsload must require onload";

  @VisibleForTesting
  static final String ONLOAD_JS_TPL = "(function() {" +
      "var nm='%s';" +
      "if (typeof window[nm]==='function') {" +
      "window[nm]();" +
      '}' +
      "})();";

  @VisibleForTesting
  static final String JSLOAD_JS_TPL = "(function() {" +
      "document.write('<scr' + 'ipt type=\"text/javascript\" src=\"%s\"></scr' + 'ipt>');" +
      "})();"; // Concatenated to avoid some browsers do dynamic script injection.

  private static final Pattern ONLOAD_FN_PATTERN = Pattern.compile("[a-zA-Z0-9_]+");

  private transient JsHandler jsHandler;
  private transient JsUriManager jsUriManager;
  private int jsloadTtlSecs;
  private CachingSetter cachingSetter;

  @VisibleForTesting
  static class CachingSetter {
    public void setCachingHeaders(HttpServletResponse resp, int ttl, boolean noProxy) {
      HttpUtil.setCachingHeaders(resp, ttl, false);
    }
  }

  @Inject
  @VisibleForTesting
  void setCachingSetter(CachingSetter util) {
    this.cachingSetter = util;
  }

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

  @Inject
  public void setJsloadTtlSecs(@Named("shindig.jsload.ttl-secs") int jsloadTtlSecs) {
    this.jsloadTtlSecs = jsloadTtlSecs;
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws IOException {

    JsUri jsUri = null;
    try {
      jsUri = jsUriManager.processExternJsUri(new UriBuilder(req).toUri());
    } catch (GadgetException e) {
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }

    // Don't emit the JS itself; instead emit JS loader script that loads
    // versioned JS. The loader script is much smaller and cacheable for a
    // configurable amount of time.
    if (jsUri.isJsload()) {
      doJsload(jsUri, resp);
      return;
    }

    // If an If-Modified-Since header is ever provided, we always say
    // not modified. This is because when there actually is a change,
    // cache busting should occur.
    if (req.getHeader("If-Modified-Since") != null &&
        jsUri.getStatus() == UriStatus.VALID_VERSIONED) {
      resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
      return;
    }

    // Get JavaScript content from features aliases request.
    JsHandler.Response handlerResponse =
        jsHandler.getJsContent(jsUri, req.getHeader("Host"));
    StringBuilder jsData = handlerResponse.getJsData();
    boolean isProxyCacheable = handlerResponse.isProxyCacheable();

    // Add onload handler to add callback function.
    String onloadStr = req.getParameter(Param.ONLOAD.getKey());
    if (onloadStr != null) {
      if (!ONLOAD_FN_PATTERN.matcher(onloadStr).matches()) {
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid onload callback specified");
        return;
      }
      jsData.append(createOnloadScript(onloadStr));
    }

    if (jsData.length() == 0) {
      resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    // post JavaScript content fetching
    postJsContentProcessing(resp, jsUri.getStatus(), isProxyCacheable);

    resp.setContentType("text/javascript; charset=utf-8");
    byte[] response = jsData.toString().getBytes("UTF-8");
    resp.setContentLength(response.length);
    resp.getOutputStream().write(response);
  }

  private void doJsload(JsUri jsUri, HttpServletResponse resp)
      throws IOException, UnsupportedEncodingException {
    String onloadStr = jsUri.getOnload();

    // Require users to specify &onload=. This ensures a reliable continuation of JS
    // execution. IE asynchronously loads script, before it loads source-scripted and
    // inlined JS.
    if (onloadStr == null) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, JSLOAD_ONLOAD_ERROR);
      return;
    }

    Uri incUri = jsUriManager.makeExternJsUri(jsUri);
    // Remove the JsLoad param so we want get here again
    UriBuilder uriBuilder = new UriBuilder(incUri);
    uriBuilder.removeQueryParameter(Param.JSLOAD.getKey());
    incUri = uriBuilder.toUri();

    int refresh = getCacheTtlSecs(jsUri);
    cachingSetter.setCachingHeaders(resp, refresh, false);

    resp.setContentType("text/javascript; charset=utf-8");
    byte[] incBytes = createJsloadScript(incUri).getBytes("UTF-8");
    resp.setContentLength(incBytes.length);
    resp.getOutputStream().write(incBytes);
  }

  private int getCacheTtlSecs(JsUri jsUri) {
    if (jsUri.isNoCache()) {
      return 0;
    } else {
      Integer jsUriRefresh = jsUri.getRefresh();
      return (jsUriRefresh != null && jsUriRefresh >= 0)
          ? jsUriRefresh : jsloadTtlSecs;
    }
  }


  @VisibleForTesting
  String createOnloadScript(String function) {
    return String.format(ONLOAD_JS_TPL, StringEscapeUtils.escapeJavaScript(function));
  }

  @VisibleForTesting
  String createJsloadScript(Uri uri) {
    String uriString = uri.toString();
    return String.format(JSLOAD_JS_TPL, uriString, uriString);
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
