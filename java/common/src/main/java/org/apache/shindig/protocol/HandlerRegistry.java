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
package org.apache.shindig.protocol;

import com.google.inject.ImplementedBy;

import org.json.JSONObject;

import java.util.Set;

/**
 * Registry of REST and RPC handlers for the set of available services
 */
@ImplementedBy(DefaultHandlerRegistry.class)
public interface HandlerRegistry {

  /**
   * Add a set of handlers to the registry
   * @param handlers
   */
  void addHandlers(Set<Object> handlers);

  /**
   * @param rpc The rpc to dispatch
   * @return the handler
   */
  RpcHandler getRpcHandler(JSONObject rpc);

  /**
   * @param path Path of the service
   * @param method The HTTP method
   * @return the handler
   */
  RestHandler getRestHandler(String path, String method);

  /**
   * @return The list of available services
   */
  Set<String> getSupportedRestServices();

  /**
   * @return The list of available services
   */
  Set<String> getSupportedRpcServices();
}
