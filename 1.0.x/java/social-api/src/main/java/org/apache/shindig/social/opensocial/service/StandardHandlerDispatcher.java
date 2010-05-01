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
package org.apache.shindig.social.opensocial.service;

import com.google.inject.Inject;
import com.google.inject.Provider;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import java.util.Map;

/**
 * Default implementation of HandlerDispatcher.  Provides default
 * bindings for the person, activity, and app data handlers.
 */
public class StandardHandlerDispatcher implements HandlerDispatcher {
  private final Map<String, Provider<? extends DataRequestHandler>> handlers;

  /**
   * Creates a dispatcher with the standard handlers.
   * @param personHandlerProvider provider for the person handler
   * @param activityHandlerProvider provider for the activity handler
   * @param appDataHandlerProvider provider for the app data handler
   */
  @Inject
  public StandardHandlerDispatcher(Provider<PersonHandler> personHandlerProvider,
      Provider<ActivityHandler> activityHandlerProvider,
      Provider<AppDataHandler> appDataHandlerProvider) {
    this(ImmutableMap.of(
        DataServiceServlet.PEOPLE_ROUTE, personHandlerProvider,
        DataServiceServlet.ACTIVITY_ROUTE, activityHandlerProvider,
        DataServiceServlet.APPDATA_ROUTE, appDataHandlerProvider));
  }

  /**
   * Creates a dispatcher with a custom list of handlers.
   * @param handlers a map of handlers by service name
   */
  public StandardHandlerDispatcher(Map<String,Provider<? extends DataRequestHandler>> handlers) {
    this.handlers = Maps.newHashMap(handlers);
  }

  /**
   * Gets a handler by service name.
   */
  public DataRequestHandler getHandler(String service) {
    Provider<? extends DataRequestHandler> provider = handlers.get(service);
    if (provider == null) {
      return null;
    }

    return provider.get();
  }

  /**
   * Adds a custom handler.
   */
  public void addHandler(String service, Provider<? extends DataRequestHandler> handler) {
    handlers.put(service, handler);
  }
}
