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

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.RenderingContext;
import org.apache.shindig.gadgets.UserPrefs;

import com.google.common.collect.Maps;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

/**
 * Extracts context from JSON input.
 */
public class JsonRpcGadgetContext extends GadgetContext {
  private final JSONObject context;
  private final JSONObject gadget;

  private final String container;
  private final Boolean debug;
  private final Boolean ignoreCache;
  private final Locale locale;
  private final Long moduleId;
  private final Uri url;
  private final UserPrefs userPrefs;
  private final String view;

  /**
   * @param context Request global parameters.
   * @param gadget Values for the gadget being rendered.
   * @throws JSONException If parameters can't be extracted or aren't correctly formed.
   */
  public JsonRpcGadgetContext(JSONObject context, JSONObject gadget) throws JSONException {
    this.context = context;
    this.gadget = gadget;

    url = getUrl(gadget);
    moduleId = getModuleId(gadget);
    userPrefs = getUserPrefs(gadget);
    locale = getLocale(context);
    view = context.optString("view");
    ignoreCache = context.optBoolean("ignoreCache");
    container = context.optString("container");
    debug = context.optBoolean("debug");
  }

  @Override
  public String getParameter(String name) {
    return gadget.has(name) ? gadget.optString(name) : context.optString(name, null);
  }

  @Override
  public String getContainer() {
    return container == null ? super.getContainer() : container;
  }

  @Override
  public boolean getDebug() {
    return debug == null ? super.getDebug() : debug;
  }
  @Override
  public boolean getIgnoreCache() {
    return ignoreCache == null ? super.getIgnoreCache() : ignoreCache;
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
    return RenderingContext.METADATA;
  }

  @Override
  public Uri getUrl() {
    return url == null ? super.getUrl() : url;
  }

  @Override
  public UserPrefs getUserPrefs() {
    return userPrefs == null ? super.getUserPrefs() : userPrefs;
  }
  @Override
  public String getView() {
    return view == null ? super.getView() : view;
  }

  /**
   * @param obj
   * @return The locale, if appropriate parameters are set, or null.
   */
  private static Locale getLocale(JSONObject obj) {
    String language = obj.optString("language");
    String country = obj.optString("country");
    if (language == null || country == null) {
      return null;
    }
    return new Locale(language, country);
  }

  /**
   * @param json
   * @return module id from the request, or null if not present
   * @throws JSONException
   */
  private static Long getModuleId(JSONObject json) throws JSONException {
    if (json.has("moduleId")) {
      return Long.valueOf(json.getLong("moduleId"));
    }
    return null;
  }

  /**
   *
   * @param json
   * @return URL from the request, or null if not present
   * @throws JSONException
   */
  private static Uri getUrl(JSONObject json) throws JSONException {
    try {
      String url = json.getString("url");
      return Uri.parse(url);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  /**
   * @param json
   * @return UserPrefs, if any are set for this request.
   * @throws JSONException
   */
  @SuppressWarnings("unchecked")
  private static UserPrefs getUserPrefs(JSONObject json) throws JSONException {
    JSONObject prefs = json.optJSONObject("prefs");
    if (prefs == null) {
      return null;
    }
    Map<String, String> p = Maps.newHashMap();
    Iterator i = prefs.keys();
    while (i.hasNext()) {
      String key = (String)i.next();
      p.put(key, prefs.getString(key));
    }
    return new UserPrefs(p);
  }
}
