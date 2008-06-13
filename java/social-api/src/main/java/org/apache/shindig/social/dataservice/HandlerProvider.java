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
package org.apache.shindig.social.dataservice;

import com.google.inject.Inject;
import com.google.common.collect.Maps;

import java.util.Map;

// TODO: We may want to extract an interface here for easier overriding
// For now you can subclass it and inject with guice
public class HandlerProvider {
  Map<String, Class<? extends DataRequestHandler>> handlers;

  @Inject
  public HandlerProvider(PersonHandler peopleHandler, ActivityHandler activityHandler,
      AppDataHandler appDataHandler) {
    handlers = Maps.newHashMap();
    handlers.put(DataServiceServlet.PEOPLE_ROUTE, peopleHandler.getClass());
    handlers.put(DataServiceServlet.ACTIVITY_ROUTE, activityHandler.getClass());
    handlers.put(DataServiceServlet.APPDATA_ROUTE, appDataHandler.getClass());
  }

  public Map<String, Class<? extends DataRequestHandler>> get() {
    return handlers;
  }
}
