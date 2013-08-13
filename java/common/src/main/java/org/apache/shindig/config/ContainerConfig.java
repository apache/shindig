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
 * Container configurations are used to support multiple, independent
 * configurations in the same server instance. Global configuration values are
 * handled via traditional mechanisms such as properties files or command-line
 * flags bound through Guice's @Named annotation.
 *
 * This interface is implemented by two classes:
 *
 * - {@link BasicContainerConfig} provides configuration inheritance;
 * - {@link ExpressionContainerConfig} extends {@link BasicContainerConfig} and
 * also provides expression evaluation in string values and properties.
 *
 * Container configurations are stored by default in JSON format, for easy
 * sharing with the code found in the PHP implementation of Shindig, and for
 * easy readability. They can be loaded with the methods in
 * {@link JsonContainerConfigLoader}.
 */
@ImplementedBy(JsonContainerConfig.class)
public interface ContainerConfig {

  public static final String PARENT_KEY = "parent";
  // TODO: Rename this to simply "container", gadgets.container is unnecessary.
  public static final String CONTAINER_KEY = "gadgets.container";
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
   * @return The configuration property stored under the given name for the
   *         given container.
   */
  Object getProperty(String container, String name);

  /**
   * @return The configuration property stored under the given name for the
   *         given container, or null if it is not defined or not a string.
   */
  String getString(String container, String name);

  /**
   * @return The configuration property stored under the given name for the
   *         given container, or 0 if it is not defined or not a number.
   */
  int getInt(String container, String name);


  /**
   * @return The configuration property stored under the given name for the
   *         given container, or false if it is not defined or not a boolean.
   */
  boolean getBool(String container, String name);

  /**
   * @return The configuration property stored under the given name for the
   *         given container, or an empty list if it is not defined or not a
   *         list.
   */
  <T> List<T> getList(String container, String name);

  /**
   * @return The configuration property stored under the given name for the
   *         given container, or an empty map if it is not defined or not a map.
   */
  <T> Map<String, T> getMap(String container, String name);

  /**
   * Creates a new transaction to create, modify or remove containers.
   *
   * @return The new transaction object.
   */
  Transaction newTransaction();

  /**
   * Adds an observer that will be notified when the configuration changes.
   *
   * @param observer The observer to be notified.
   * @param notifyNow If true, the observer will receive an immediate
   *        notification for the current configuration.
   */
  void addConfigObserver(ConfigObserver observer, boolean notifyNow);

  /**
   * A transaction object allows to create, modify and remove one or more
   * containers at a time.
   */
  interface Transaction {

    /**
     * Clears the container configuration before performing the other operations
     * in the transaction.
     *
     * @return The transaction object, to allow chaining operations.
     */
    Transaction clearContainers();

    /**
     * Adds or modifies a container configuration.
     *
     * A container's names are specified in the gadgets.container property. If
     * it is an array, a copy of the container will be created for each name in
     * that property.
     *
     * @param container The container's new configuration, as a map from
     *        property name to property contents.
     * @return The transaction object, to allow chaining operations.
     */
    Transaction addContainer(Map<String, Object> container);

    /**
     * Removes a container configuration.
     *
     * @param name The name of the container to remove.
     * @return The transaction object, to allow chaining operations.
     */
    Transaction removeContainer(String name);

    /**
     * Performs all the transaction operations on the container configuration.
     *
     * @throws ContainerConfigException If there was a problem applying the new
     *         configuration. If this exception is thrown, the existing
     *         configuration will not be modified.
     */
    void commit() throws ContainerConfigException;
  }

  /**
   * Interface for objects that get notified when container configurations are
   * changed.
   */
  interface ConfigObserver {

    /**
     * Notifies the object that some container configurations have been added or
     * modified.
     *
     * @param config The ContainerConfig object where the configuration was
     *        changed.
     * @param changed The names of the containers that have been added or
     *        modified.
     * @param removed The names of the containers that have been removed.
     */
    void containersChanged(
        ContainerConfig config, Collection<String> changed, Collection<String> removed);
  }
}
