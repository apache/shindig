/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.shindig.gadgets.http;

import org.apache.shindig.gadgets.ProcessingOptions;

import javax.servlet.http.HttpServletRequest;

/**
 * {@code ProcessingOptions} derived from parameters supplied in an
 * {@code HttpServletRequest}.
 */
public class HttpProcessingOptions extends ProcessingOptions {
  private final boolean ignoreCache;
  private final String forceJsLibs;
  private final String syndicator;

  public HttpProcessingOptions(HttpServletRequest req) {
    ignoreCache = getIgnoreCache(req);
    forceJsLibs = getForceJsLibs(req);
    syndicator = getSyndicator(req);
  }

  /** {@inheritDoc} */
  @Override
  public String getForcedJsLibs() {
    return forceJsLibs;
  }

  /** {@inheritDoc} */
  @Override
  public boolean getIgnoreCache() {
    return ignoreCache;
  }

  /** {@inheritDoc} */
  @Override
  public String getSyndicator() {
    if (syndicator == null) {
      return super.getSyndicator();
    }
    return syndicator;
  }

  /**
   * @param req
   * @return Whether or not to ignore the cache.
   */
  protected static boolean getIgnoreCache(HttpServletRequest req) {
    String noCacheParam = req.getParameter("nocache");
    if (noCacheParam == null) {
      noCacheParam = req.getParameter("bpc");
    }
    return "1".equals(noCacheParam);
  }

  /**
   * @param req
   * @return Forced JS libs, or null if no forcing is to be done.
   */
  protected static String getForceJsLibs(HttpServletRequest req) {
    return req.getParameter("libs");
  }

  /**
   * @param req
   * @return Forced JS libs, or null if no forcing is to be done.
   */
  protected static String getSyndicator(HttpServletRequest req) {
    return req.getParameter("syndicator");
  }
}
