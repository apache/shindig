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

import org.apache.shindig.common.ContainerConfig;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.uri.UriBuilder;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpResponse;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Base class for proxy-based handlers.
 */
public abstract class ProxyBase {
  public static final String URL_PARAM = "url";
  public static final String REFRESH_PARAM = "refresh";
  public static final String GADGET_PARAM = "gadget";
  public static final String CONTAINER_PARAM = "container";
  // Old form container name, retained for legacy compatibility.
  public static final String SYND_PARAM = "synd";

  // Public because of rewriter. Rewriter should be cleaned up.
  public static final String REWRITE_MIME_TYPE_PARAM = "rewriteMime";

  /**
   * Validates the given url.
   *
   * @return A URI representing a validated form of the url.
   * @throws GadgetException If the url is not valid.
   */
  protected Uri validateUrl(String urlToValidate) throws GadgetException {
    if (urlToValidate == null) {
      throw new GadgetException(GadgetException.Code.INVALID_PARAMETER,
          "url parameter is missing.");
    }
    try {
      UriBuilder url = UriBuilder.parse(urlToValidate);
      if (!"http".equals(url.getScheme()) && !"https".equals(url.getScheme())) {
        throw new GadgetException(GadgetException.Code.INVALID_PARAMETER,
            "Invalid request url scheme in url: " + urlToValidate +
            "; only \"http\" and \"https\" supported.");
      }
      if (url.getPath() == null || url.getPath().length() == 0) {
        url.setPath("/");
      }
      return url.toUri();
    } catch (IllegalArgumentException e) {
      throw new GadgetException(GadgetException.Code.INVALID_PARAMETER,
          "url parameter is not a valid url.");
    }
  }

  /**
   * Extracts the first parameter from the parameter map with the given name.
   *
   * @param request The request to extract parameters from.
   * @param name The name of the parameter to retrieve.
   * @param defaultValue The default value to use if the parameter is not set.
   * @return The parameter, if found, or defaultValue
   */
  protected String getParameter(HttpServletRequest request, String name, String defaultValue) {
    String ret = request.getParameter(name);
    return ret == null ? defaultValue : ret;
  }

  /**
   * Extracts the container name from the request.
   */
  protected String getContainer(HttpServletRequest request) {
    String container = getParameter(request, CONTAINER_PARAM, null);
    if (container == null) {
      container = getParameter(request, SYND_PARAM, ContainerConfig.DEFAULT_CONTAINER);
    }
    return container;
  }

  /**
   * Sets cache control headers for the response.
   */
  protected void setResponseHeaders(HttpServletRequest request,
      HttpServletResponse response, HttpResponse results) {
    int refreshInterval = 0;
    if (results.isStrictNoCache()) {
      refreshInterval = 0;
    } else  if (request.getParameter(REFRESH_PARAM) != null) {
      refreshInterval =  Integer.valueOf(request.getParameter(REFRESH_PARAM));
    } else {
      refreshInterval = Math.max(60 * 60, (int)(results.getCacheTtl() / 1000L));
    }
    HttpUtil.setCachingHeaders(response, refreshInterval);
    // We're skipping the content disposition header for flash due to an issue with Flash player 10
    // This does make some sites a higher value phishing target, but this can be mitigated by
    // additional referer checks.
    if (!"application/x-shockwave-flash".equalsIgnoreCase(results.getHeader("Content-Type"))) {
      response.setHeader("Content-Disposition", "attachment;filename=p.txt");
    }
  }

  /**
   * Processes the given request.
   */
  abstract public void fetch(HttpServletRequest request, HttpServletResponse response)
      throws GadgetException, IOException;
}
