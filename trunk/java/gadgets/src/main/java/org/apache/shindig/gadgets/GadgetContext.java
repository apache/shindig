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
package org.apache.shindig.gadgets;

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.gadgets.spec.GadgetSpec;

import java.util.Locale;

/**
 * Bundles together context data for the current request with server config data.
 *
 * TODO: This should probably be called "GadgetRequest" instead of GadgetContext, since it is
 * actually serving as abstraction over different request types.
 */
public class GadgetContext {
  private final GadgetContext delegate;

  public GadgetContext() {
    this(null);
  }

  public GadgetContext(GadgetContext delegate) {
    this.delegate = delegate;
  }

  /**
   * @param name The parameter to get data for.
   * @return The parameter set under the given name, or null.
   */
  public String getParameter(String name) {
    return delegate == null ? null : delegate.getParameter(name);
  }

  /**
   * @return The url for this gadget.
   */
  public Uri getUrl() {
    return delegate == null ? null : delegate.getUrl();
  }

  /**
   * @return The module id for this request.
   */
  public long getModuleId() {
    return delegate == null ? 0 : delegate.getModuleId();
  }

  /**
   * @return The locale for this request.
   */
  public Locale getLocale() {
    return delegate == null ? GadgetSpec.DEFAULT_LOCALE : delegate.getLocale();
  }

  /**
   * @return The rendering context for this request.
   */
  public RenderingContext getRenderingContext() {
    return delegate == null ? RenderingContext.GADGET : delegate.getRenderingContext();
  }

  /**
   * @return Whether or not to bypass caching behavior for the current request.
   */
  public boolean getIgnoreCache() {
    return delegate != null && delegate.getIgnoreCache();
  }

  /**
   * @return The container of the current request.
   */
  public String getContainer() {
    return delegate == null ? ContainerConfig.DEFAULT_CONTAINER : delegate.getContainer();
  }

  /**
   * @return The host for which the current request is being made.
   */
  public String getHost() {
    return delegate == null ? null : delegate.getHost();
  }

  /**
   * @return The host schema for which the current request is being made.
   */
  public String getHostSchema() {
    return delegate == null ? null : delegate.getHostSchema();
  }

  /**
   * @return The IP Address for the current user.
   */
  public String getUserIp() {
    return delegate == null ? null : delegate.getUserIp();
  }

  /**
   * @return Whether or not to show debug output.
   */
  public boolean getDebug() {
    return delegate != null && delegate.getDebug();
  }

  /**
   * @return Name of view to show
   */
  public String getView() {
    return delegate == null ? GadgetSpec.DEFAULT_VIEW : delegate.getView();
  }

  /**
   * @return The user prefs for the current request.
   */
  public UserPrefs getUserPrefs() {
    return delegate == null ? UserPrefs.EMPTY : delegate.getUserPrefs();
  }

  /**
   * @return The token associated with this request
   */
  public SecurityToken getToken() {
    return delegate == null ? null : delegate.getToken();
  }

  /**
   * @return The user agent string, or null if not present.
   */
  public String getUserAgent() {
    return delegate == null ? null : delegate.getUserAgent();
  }

  /**
   * @return Whether the gadget output should be sanitized.
   */
  public boolean getSanitize() {
    return delegate == null ? false : delegate.getSanitize();
  }

  /**
   * @return Whether the gadget output should be cajoled.
   */
  public boolean getCajoled() {
    return delegate == null ? false : delegate.getCajoled();
  }

  /**
   * @return return the feature js repository if available
   */
  public String getRepository() {
    return delegate == null ? null : delegate.getRepository();
  }

  public String getReferer() {
      return delegate == null ? null : delegate.getReferer();
  }
}

