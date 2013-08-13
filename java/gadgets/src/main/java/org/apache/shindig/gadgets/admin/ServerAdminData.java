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
package org.apache.shindig.gadgets.admin;

import java.util.Map;

import com.google.caja.util.Maps;
import com.google.common.base.Objects;
import com.google.inject.Inject;
import org.apache.shindig.common.Nullable;

/**
 * Administration data for the server.
 *
 * @version $Id: $
 */
public class ServerAdminData {
  private Map<String, ContainerAdminData> containerAdminDataMap;

  /**
   * Constructor.
   */
  @Inject
  public ServerAdminData() {
    this(null);
  }

  /**
   * Constructor.
   *
   * @param containerAdminMap
   *          a map of container IDs to container.
   */
  public ServerAdminData(@Nullable Map<String, ContainerAdminData> containerAdminMap) {
    this.containerAdminDataMap = (containerAdminMap != null) ? containerAdminMap :
        Maps.<String, ContainerAdminData>newHashMap();
  }

  /**
   * Gets the given containers administration data.
   *
   * @param container
   *          the id of the container.
   * @return the administration data for the container.
   */
  public ContainerAdminData getContainerAdminData(String container) {
    container = container != null ? container.toLowerCase() : container;
    return this.containerAdminDataMap.get(container);
  }

  /**
   * Removes container administration data.
   *
   * @param container
   *          the container id.
   */
  public void removeContainerAdminData(String container) {
    this.containerAdminDataMap.remove(container);
  }

  /**
   * Adds administration data for a container.
   *
   * @param container
   *          the container id the admin data is for.
   * @param toAdd
   *          the admin data to add.
   */
  public void addContainerAdminData(String container, ContainerAdminData toAdd) {
    if (container != null) {
      if (toAdd == null) {
        toAdd = new ContainerAdminData();
      }
      this.containerAdminDataMap.put(container.toLowerCase(), toAdd);
    }
  }

  /**
   * Gets the map of container IDs to container admin data.
   *
   * @return the map of container IDs to container admin data.
   */
  public Map<String, ContainerAdminData> getContainerAdminDataMap() {
    return this.containerAdminDataMap;
  }

  /**
   * Clears all the container administration data.
   */
  public void clearContainerAdminData() {
    this.containerAdminDataMap.clear();
  }

  /**
   * Determines whether there is administration data for the container.
   *
   * @param container
   *          the container to check.
   * @return true if there is administration data false otherwise.
   */
  public boolean hasContainerAdminData(String container) {
    container = container != null ? container.toLowerCase() : container;
    return this.containerAdminDataMap.keySet().contains(container);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ServerAdminData) {
      ServerAdminData test = (ServerAdminData) obj;
      return test.getContainerAdminDataMap().equals(this.containerAdminDataMap);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(this.containerAdminDataMap);
  }
}
