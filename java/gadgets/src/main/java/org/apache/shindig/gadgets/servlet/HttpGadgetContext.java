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

import org.apache.shindig.auth.AuthInfoUtil;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.RenderingContext;
import org.apache.shindig.gadgets.UserPrefs;
import org.apache.shindig.gadgets.uri.UriCommon;

import com.google.common.collect.Maps;

import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

/**
 * Implements GadgetContext using an HttpServletRequest
 */
public class HttpGadgetContext extends GadgetContext {
  public static final String USERPREF_PARAM_PREFIX = "up_";

  private final HttpServletRequest request;

  private final String container;
  private final Boolean debug;
  private final Boolean ignoreCache;
  private final Locale locale;
  private final Integer moduleId;
  private final RenderingContext renderingContext;
  private final Uri url;
  private final UserPrefs userPrefs;
  private final String view;
  private final String referer;

  public HttpGadgetContext(HttpServletRequest request) {
    this.request = request;

    container = getContainer(request);
    debug = getDebug(request);
    ignoreCache = getIgnoreCache(request);
    locale = getLocale(request);
    moduleId = getModuleId(request);
    renderingContext = getRenderingContext(request);
    url = getUrl(request);
    userPrefs = getUserPrefs(request);
    view = getView(request);
    referer = getReferer();
  }

  @Override
  public String getParameter(String name) {
    return request.getParameter(name);
  }

  @Override
  public String getContainer() {
    return container == null ? super.getContainer() : container;
  }

  @Override
  public String getHost() {
    String host = request.getHeader("Host");
    return host == null ? super.getHost() : host;
  }

  @Override
  public String getHostSchema() {
    String schema = request.getScheme();
    return schema == null ? super.getHostSchema() : schema;
  }

  @Override
  public String getUserIp() {
    String ip = request.getRemoteAddr();
    return ip == null ? super.getUserIp() : ip;
  }

  @Override
  public boolean getDebug() {
    return debug == null ? super.getDebug(): debug;
  }

  @Override
  public boolean getIgnoreCache() {
    if (ignoreCache == null) {
      return super.getIgnoreCache();
    }
    return ignoreCache;
  }

  @Override
  public Locale getLocale() {
    return locale == null ? super.getLocale() : locale;
  }

  @Override
  public long getModuleId() {
    return moduleId == null ? super.getModuleId() : moduleId;
  }

  @Override
  public RenderingContext getRenderingContext() {
    return renderingContext == null ? super.getRenderingContext() : renderingContext;
  }

  @Override
  public SecurityToken getToken() {
    return AuthInfoUtil.getSecurityTokenFromRequest(request);
  }

  @Override
  public Uri getUrl() {
    return url == null ? super.getUrl() : url;
  }

  @Override
  public UserPrefs getUserPrefs() {
    if (userPrefs == null) {
      return super.getUserPrefs();
    }
    return userPrefs;
  }

  @Override
  public String getView() {
    if (view == null) {
      return super.getView();
    }
    return view;
  }

  @Override
  public String getUserAgent() {
    String userAgent = request.getHeader("User-Agent");
    if (userAgent == null) {
      return super.getUserAgent();
    }
    return userAgent;
  }

  @Override
  public String getRepository() {
    String repository = request.getHeader(UriCommon.Param.REPOSITORY_ID.getKey());
    if (repository == null) {
      return super.getRepository();
    }
    return repository;
  }

  @Override
  public String getReferer() {
    String referer = request.getHeader("Referer");
    return referer == null ? super.getReferer() : referer;
  }

  /**
   * @param req
   * @return The container, if set, or null.
   */
  @SuppressWarnings("deprecation")
  private static String getContainer(HttpServletRequest req) {
    String container = req.getParameter(UriCommon.Param.CONTAINER.getKey());
    if (container == null) {
      // The parameter used to be called 'synd' FIXME: schedule removal
      container = req.getParameter(UriCommon.Param.SYND.getKey());
    }
    return container;
  }

  /**
   * @param req
   * @return Debug setting, if set, or null.
   */
  private static Boolean getDebug(HttpServletRequest req) {
    String debug = req.getParameter(UriCommon.Param.DEBUG.getKey());
    if (debug == null) {
      return Boolean.FALSE;
    } else if ("0".equals(debug)) {
      return Boolean.FALSE;
    }
    return Boolean.TRUE;
  }

  /**
   * @param req
   * @return The ignore cache setting, if appropriate params are set, or null.
   */
  private static Boolean getIgnoreCache(HttpServletRequest req) {
    String ignoreCache = req.getParameter(UriCommon.Param.NO_CACHE.getKey());
    if (ignoreCache == null) {
      return Boolean.FALSE;
    } else if ("0".equals(ignoreCache)) {
      return Boolean.FALSE;
    }
    return Boolean.TRUE;
  }

  /**
   * @param req
   * @return The locale, if appropriate parameters are set, or null.
   */
  private static Locale getLocale(HttpServletRequest req) {
    String language = req.getParameter(UriCommon.Param.LANG.getKey());
    String country = req.getParameter(UriCommon.Param.COUNTRY.getKey());
    if (language == null && country == null) {
      return null;
    } else if (language == null) {
      language = "all";
    } else if (country == null) {
      country = "ALL";
    }
    return new Locale(language, country);
  }

  /**
   * @param req
   * @return module id, if specified
   */
  @SuppressWarnings("boxing")
  private static Integer getModuleId(HttpServletRequest req) {
    String mid = req.getParameter("mid");
    if (mid == null) {
      return null;
    }

    try {
      return Integer.parseInt(mid);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  /**
   * @param req
   * @return The rendering context, if appropriate params are set, or null.
   */
  private static RenderingContext getRenderingContext(HttpServletRequest req) {
    String c = req.getParameter(UriCommon.Param.CONTAINER_MODE.getKey());
    if (c == null) {
      return null;
    }
    return RenderingContext.valueOfParam(c);
  }

  /**
   * @param req
   * @return The ignore cache setting, if appropriate params are set, or null.
   */
  private static Uri getUrl(HttpServletRequest req) {
    String url = req.getParameter(UriCommon.Param.URL.getKey());
    if (url == null) {
      return null;
    }
    try {
      return Uri.parse(url);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  /**
   * @param req
   * @return UserPrefs, if any are set for this request.
   */
  @SuppressWarnings("unchecked")
  private static UserPrefs getUserPrefs(HttpServletRequest req) {
    Map<String, String> prefs = Maps.newHashMap();
    Enumeration<String> paramNames = req.getParameterNames();
    if (paramNames == null) {
      return null;
    }
    while (paramNames.hasMoreElements()) {
      String paramName = paramNames.nextElement();
      if (paramName.startsWith(USERPREF_PARAM_PREFIX)) {
        String prefName = paramName.substring(USERPREF_PARAM_PREFIX.length());
        prefs.put(prefName, req.getParameter(paramName));
      }
    }
    return new UserPrefs(prefs);
  }

  /**
   * @param req
   * @return The view, if specified, or null.
   */
  private static String getView(HttpServletRequest req) {
    return req.getParameter(UriCommon.Param.VIEW.getKey());
  }
}
