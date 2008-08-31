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

import org.apache.shindig.common.SecurityToken;
import org.apache.shindig.common.SecurityTokenDecoder;
import org.apache.shindig.common.SecurityTokenException;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.RenderingContext;
import org.apache.shindig.gadgets.UserPrefs;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

/**
 * Implements GadgetContext using an HttpServletRequest
 */
public class HttpGadgetContext extends GadgetContext {
  public static final String USERPREF_PARAM_PREFIX = "up_";

  private final HttpServletRequest request;
  private final SecurityTokenDecoder tokenDecoder;

  private final String container;
  private final Boolean debug;
  private final Boolean ignoreCache;
  private final Locale locale;
  private final Integer moduleId;
  private final RenderingContext renderingContext;
  private final String tokenString;
  private final URI url;
  private final UserPrefs userPrefs;
  private final String view;

  public HttpGadgetContext(HttpServletRequest request, SecurityTokenDecoder tokenDecoder) {
    this.request = request;
    this.tokenDecoder = tokenDecoder;

    container = getContainer(request);
    debug = getDebug(request);
    ignoreCache = getIgnoreCache(request);
    locale = getLocale(request);
    moduleId = getModuleId(request);
    renderingContext = getRenderingContext(request);
    // TODO: This shouldn't be depending on MakeRequest at all.
    tokenString = request.getParameter(MakeRequestHandler.SECURITY_TOKEN_PARAM);
    url = getUrl(request);
    userPrefs = getUserPrefs(request);
    view = getView(request);
  }

  @Override
  public String getParameter(String name) {
    return request.getParameter(name);
  }

  @Override
  public String getContainer() {
    if (container == null) {
      return super.getContainer();
    }
    return container;
  }

  @Override
  public boolean getDebug() {
    if (debug == null) {
      return super.getDebug();
    }
    return debug;
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
    if (locale == null) {
      return super.getLocale();
    }
    return locale;
  }

  @Override
  public int getModuleId() {
    if (moduleId == null) {
      return super.getModuleId();
    }
    return moduleId;
  }

  @Override
  public RenderingContext getRenderingContext() {
    if (renderingContext == null) {
      return super.getRenderingContext();
    }
    return renderingContext;
  }

  @Override
  public SecurityToken getToken() throws GadgetException {
    if (tokenString == null || tokenString.length() == 0) {
      return super.getToken();
    } else {
      try {
        Map<String, String> tokenMap
            = Collections.singletonMap(SecurityTokenDecoder.SECURITY_TOKEN_NAME, tokenString);
        return tokenDecoder.createToken(tokenMap);
      } catch (SecurityTokenException e) {
        throw new GadgetException(
            GadgetException.Code.INVALID_SECURITY_TOKEN, e);
      }
    }
  }

  @Override
  public URI getUrl() {
    if (url == null) {
      return super.getUrl();
    }
    return url;
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

  /**
   * @param req
   * @return The container, if set, or null.
   */
  private static String getContainer(HttpServletRequest req) {
    String container = req.getParameter("container");
    if (container == null) {
      // The parameter used to be called 'synd' FIXME: schedule removal
      container = req.getParameter("synd");
    }
    return container;
  }

  /**
   * @param req
   * @return Debug setting, if set, or null.
   */
  private static Boolean getDebug(HttpServletRequest req) {
    String debug = req.getParameter("debug");
    if (debug == null) {
      return null;
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
    String ignoreCache = req.getParameter("nocache");
    if (ignoreCache == null) {
      return null;
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
    String language = req.getParameter("lang");
    String country = req.getParameter("country");
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
  private static Integer getModuleId(HttpServletRequest req) {
    String mid = req.getParameter("mid");
    if (mid == null) {
      return null;
    }
    return Integer.parseInt(mid);
  }

  /**
   * @param req
   * @return The rendering context, if appropriate params are set, or null.
   */
  private static RenderingContext getRenderingContext(HttpServletRequest req) {
    String c = req.getParameter("c");
    if (c == null) {
      return null;
    }
    return c.equals("1") ? RenderingContext.CONTAINER : RenderingContext.GADGET;
  }

  /**
   * @param req
   * @return The ignore cache setting, if appropriate params are set, or null.
   */
  private static URI getUrl(HttpServletRequest req) {
    String url = req.getParameter("url");
    if (url == null) {
      return null;
    }
    try {
      return new URI(url);
    } catch (URISyntaxException e) {
      return null;
    }
  }

  /**
   * @param req
   * @return UserPrefs, if any are set for this request.
   */
  @SuppressWarnings("unchecked")
  private static UserPrefs getUserPrefs(HttpServletRequest req) {
    Map<String, String> prefs = new HashMap<String, String>();
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
    return req.getParameter("view");
  }
}
