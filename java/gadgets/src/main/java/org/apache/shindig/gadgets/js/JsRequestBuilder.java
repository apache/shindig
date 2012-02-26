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
package org.apache.shindig.gadgets.js;

import com.google.inject.Inject;

import org.apache.shindig.common.uri.UriBuilder;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.features.FeatureRegistry;
import org.apache.shindig.gadgets.uri.JsUriManager;
import org.apache.shindig.gadgets.uri.JsUriManager.JsUri;

import javax.servlet.http.HttpServletRequest;

/**
 * Builds {@link JsRequest} instances.
 */
public class JsRequestBuilder {

  private final JsUriManager jsUriManager;
  private final FeatureRegistry registry;

  @Inject
  public JsRequestBuilder(JsUriManager jsUriManager,
                          FeatureRegistry registry) {
    this.jsUriManager = jsUriManager;
    this.registry = registry;
  }

  /**
   * Builds a {@link JsRequest} instance from the given request object.
   *
   * @param request The originating HTTP request object.
   * @return The corresponding JsRequest object.
   * @throws GadgetException If there was a problem parsing the URI.
   */
  public JsRequest build(HttpServletRequest request) throws GadgetException {
    JsUri jsUri = jsUriManager.processExternJsUri(new UriBuilder(request).toUri());
    String host = request.getHeader("Host");
    boolean inCache = request.getHeader("If-Modified-Since") != null;
    return build(jsUri, host, inCache);
  }

  /**
   * Builds a {@link JsRequest} instance for a given JsUri/host pair.
   * @param jsUri JsUri encapsulating the request.
   * @param host Host context for the request.
   * @return The corresponding JsRequest.
   */
  public JsRequest build(JsUri jsUri, String host) {
    return build(jsUri, host, false);
  }

  protected JsRequest build(JsUri jsUri, String host, boolean inCache) {
    return new JsRequest(jsUri, host, inCache, registry);
  }
}
