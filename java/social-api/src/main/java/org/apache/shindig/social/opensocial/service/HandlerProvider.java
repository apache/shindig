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

import com.google.inject.Provider;
import com.google.common.collect.Maps;

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
public class HandlerProvider implements Provider<Map<String,
    Class<? extends DataRequestHandler>>> {
  
  private Map<String, Class<? extends DataRequestHandler>> handlers;
  
  private static Map<String, Class<? extends DataRequestHandler>> DEFAULT_HANDLERS =
      Maps.immutableMap(
          DataServiceServlet.PEOPLE_ROUTE, PersonHandler.class,
          DataServiceServlet.ACTIVITY_ROUTE, ActivityHandler.class,
          DataServiceServlet.APPDATA_ROUTE, AppDataHandler.class);
  
  protected HandlerProvider(boolean useDefaultProviders) {
    handlers = Maps.newHashMap(useDefaultProviders ? DEFAULT_HANDLERS : null);
  }
  
  public void addHandler(String path, Class<? extends DataRequestHandler> handler) {
    handlers.put(path, handler);
  }
  
  public HandlerProvider(Map<String,Class<? extends DataRequestHandler>> handlers) {
    this.handlers = Maps.newHashMap(handlers);
  }
  
  public Map<String, Class<? extends DataRequestHandler>> get() {
    return handlers;
  }
  
  public static HandlerProvider defaultProviders() {
    return new HandlerProvider(DEFAULT_HANDLERS);
  }
}
