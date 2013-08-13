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
package org.apache.shindig.gadgets.config;

import org.apache.shindig.gadgets.Gadget;

import java.util.Map;

/**
 * Interface used by java classes that can inject javascript configuration information
 * @since 2.0.0
 */
public interface ConfigContributor {
  /**
   * Contribute configuration values for a specific gadget in an iframe.
   * @param config The config mapping of feature to value.
   * @param gadget The gadget to contribute for.
   */
  public void contribute(Map<String,Object> config, Gadget gadget);

  /**
   * Contribute configuration for the container specific javascript. This interface
   * should only support params used by JsServlet
   *
   * @param config The config to add to.
   * @param container The container.
   * @param host The hostname
   */
  public void contribute(Map<String,Object> config, String container, String host);
}
