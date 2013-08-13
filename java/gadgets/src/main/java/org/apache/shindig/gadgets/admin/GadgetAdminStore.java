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

import java.util.Set;

import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.spec.Feature;

/**
 * Interface for working with the store of gadget administration data.
 *
 * @since 2.5.0
 */
public interface GadgetAdminStore {

  /**
   * Gets the administration data for a gadget in a container.
   *
   * @param container
   *          the container id.
   * @param gadgetUrl
   *          the gadget URL.
   * @return the administration data for a gadget in a container
   */
  public GadgetAdminData getGadgetAdminData(String container, String gadgetUrl);

  /**
   * Sets gadget administration data for a gadget in a container.
   *
   * @param container
   *          the container id.
   * @param gadgetUrl
   *          the gadget URL.
   * @param adminData
   *          administration data.
   */
  public void setGadgetAdminData(String container, String gadgetUrl, GadgetAdminData adminData);

  /**
   * Gets container administration data.
   *
   * @param container
   *          the container to get the administration data for.
   * @return container administration data.
   */
  public ContainerAdminData getContainerAdminData(String container);

  /**
   * Sets the container administration data..
   *
   * @param container
   *          the container to set the administration data for.
   * @param containerAdminData
   *          the container administration data.
   */
  public void setContainerAdminData(String container, ContainerAdminData containerAdminData);

  /**
   * Gets the administration data for the server.
   *
   * @return the administration data for the server.
   */
  public ServerAdminData getServerAdminData();

  /**
   * Checks the feature administration data for a gadget.
   *
   * @param gadget
   *          The gadget to check.
   * @return true if the gadget is allowed to use all the features it requires false otherwise.
   */
  public boolean checkFeatureAdminInfo(Gadget gadget);

  /**
   * If feature administration is enabled for the given container then check to see if the feature
   * is allowed. If it is not allowed the feature code will not be loaded in the container so we
   * should not put it in the config.
   *
   * @param feature
   *          The feature to check.
   * @param gadget
   *          The gadget to check.
   * @return true if the feature is allowed to be used by the gadget in the given container.
   */
  public boolean isAllowedFeature(Feature feature, Gadget gadget);

  /**
   * Determines whether a gadget is on the whitelist of trusted gadgets set by the admin.
   *
   * @param container
   *          The container id.
   * @param gadgetUrl
   *          The gadget URL.
   * @return true if the gadget is on the whitelist, false otherwise.
   */
  public boolean isWhitelisted(String container, String gadgetUrl);

  /**
   * Gets additional RPC service IDs to allow for the gadget.
   *
   * @param gadget
   *          The gadget to get the IDs for.
   * @return The set of additional RPC service IDs to allow for the gadget.
   */
  public Set<String> getAdditionalRpcServiceIds(Gadget gadget);
}
