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

package org.apache.shindig.social.opensocial.service;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.common.collect.Maps;

import java.util.Collections;
import java.util.Map;

/**
 * Class to inject handlers for data requests.
 * 
 * You should add your own binding for HandlerProvider if you are customizing
 * request handling.
 * 
 * This is a convenience shell on top of
 * Provider<Map<String, Class<? extends DataRequestHandler>>> along
 * with a default set of handlers.
 */
public class HandlerProvider implements Provider<Map<String, Provider<? extends DataRequestHandler>>> {
  private final Map<String, Provider<? extends DataRequestHandler>> handlers;

  /**
   * Constructor for the default handlers.
   */
  @Inject
  public HandlerProvider(Provider<PersonHandler> personHandlerProvider,
      Provider<ActivityHandler> activityHandlerProvider,
      Provider<AppDataHandler> appDataHandlerProvider) {
    this(Maps.immutableMap(
        DataServiceServlet.PEOPLE_ROUTE, personHandlerProvider,
        DataServiceServlet.ACTIVITY_ROUTE, activityHandlerProvider,
        DataServiceServlet.APPDATA_ROUTE, appDataHandlerProvider));
  }

  public HandlerProvider(Map<String,Provider<? extends DataRequestHandler>> handlers) {
    this.handlers = Maps.newHashMap(handlers);
  }

  public void addHandler(String path, Provider<? extends DataRequestHandler> handler) {
    handlers.put(path, handler);
  }
  
  public Map<String, Provider<? extends DataRequestHandler>> get() {
    return Collections.unmodifiableMap(handlers);
  }
}
