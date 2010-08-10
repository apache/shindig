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

package org.apache.shindig.config;

import com.google.inject.ImplementedBy;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Represents a container configuration.
 *
 * Container configurations are used to support multiple, independent configurations in the same
 * server instance. Global configuration values are handled via traditional mechanisms such as
 * properties files or command-line flags bound through Guice's @Named annotation.
 *
 * The default container configuration implementation is intended to be shared with the code found
 * in the PHP implementation of Shindig. It uses a simple JSON format inteded for easy readability.
 *
 * get* can take either a simple property name (foo), or an EL expression (${foo.bar}).
 */
@ImplementedBy(JsonContainerConfig.class)
public interface ContainerConfig {
  public static final String DEFAULT_CONTAINER = "default";

  /**
   * @return The set of all containers that are currently registered.
   */
  Collection<String> getContainers();

  /**
   * Fetch all properties for the given container configuration.
   */
  Map<String, Object> getProperties(String container);

  /**
   * @return The configuration property stored under the given name for the given container.
   */
  Object getProperty(String container, String name);

  /**
   * @return The configuration property stored under the given name for the given container, or null
   * if it is not defined or not a string.
   */
  String getString(String container, String name);

  /**
   * @return The configuration property stored under the given name for the given container, or 0
   * if it is not defined or not a number.
   */
  int getInt(String container, String name);


  /**
   * @return The configuration property stored under the given name for the given container, or
   * false if it is not defined or not a boolean.
   */
  boolean getBool(String container, String name);

  /**
   * @return The configuration property stored under the given name for the given container, or an
   * empty list if it is not defined or not a list.
   */
  <T> List<T> getList(String container, String name);

  /**
   * @return The configuration property stored under the given name for the given container, or an
   * empty map if it is not defined or not a map.
   */
  <T> Map<String, T> getMap(String container, String name);
}
