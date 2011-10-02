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
import java.util.Set;

import com.google.caja.util.Maps;
import com.google.common.base.Objects;

/**
 * Container's administration data.
 *
 * @version $Id: $
 */
public class ContainerAdminData {
  private Map<String, GadgetAdminData> gadgetAdminMap;

  /**
   * Constructor
   */
  public ContainerAdminData() {
    this(null);
  }

  /**
   * Constructor
   *
   * @param gadgetAdminMap
   *          map of gadget URLs to gadget admin data.
   */
  public ContainerAdminData(Map<String, GadgetAdminData> gadgetAdminMap) {
    if (gadgetAdminMap == null) {
      gadgetAdminMap = Maps.newHashMap();
    }
    this.gadgetAdminMap = gadgetAdminMap;
  }

  /**
   * Adds gadget administration information for this container.
   *
   * @param gadgetUrl
   *          the URL of the gadget the admin data is for.
   * @param toAdd
   *          the administration data for the gadget.
   */
  public void addGadgetAdminData(String gadgetUrl, GadgetAdminData toAdd) {
    if (gadgetUrl != null) {
      if (toAdd == null) {
        toAdd = new GadgetAdminData();
      }
      this.gadgetAdminMap.put(gadgetUrl, toAdd);
    }
  }

  /**
   * Removes the gadget administration data.
   *
   * @param gadgetUrl
   *          the gadget URL.
   *
   * @return The gadget administration data that was removed, or null if there was not gadget
   *         administration data associated with that gadget URL.
   */
  public GadgetAdminData removeGadgetAdminData(String gadgetUrl) {
    return this.gadgetAdminMap.remove(gadgetUrl);
  }

  /**
   * Gets the gadget admin data for a given gadget.
   *
   * @param gadgetUrl
   *          the URL to the gadget to get the administration data for.
   * @return the gadget admin data.
   */
  public GadgetAdminData getGadgetAdminData(String gadgetUrl) {
    GadgetAdminData match = this.gadgetAdminMap.get(gadgetUrl);
    if(match != null) {
      return match;
    }

    String key = gadgetUrl != null ? getGadgetAdminDataKey(gadgetUrl) : null;
    return this.gadgetAdminMap.get(key);
  }

  /**
   * Gets the gadget admin map.
   *
   * @return the gadget admin map.
   */
  public Map<String, GadgetAdminData> getGadgetAdminMap() {
    return this.gadgetAdminMap;
  }

  /**
   * Clears the gadget administration data.
   */
  public void clearGadgetAdminData() {
    this.gadgetAdminMap.clear();
  }

  /**
   * Determines whether there is administration data for a gadget.
   *
   * @param gadgetUrl
   *          the gadget URL to check.
   * @return true if there is administration data for a gadget false otherwise.
   */
  public boolean hasGadgetAdminData(String gadgetUrl) {
    if (this.gadgetAdminMap.keySet().contains(gadgetUrl)) {
      return true;
    }

    return gadgetUrl != null ? getGadgetAdminDataKey(gadgetUrl) != null : false;
  }

  /**
   * Gets the key in the map for the gadget URL.
   *
   * @param gadgetUrl
   *          The gadget URL.
   * @return The key in the map for the gadget URL.
   */
  private String getGadgetAdminDataKey(String gadgetUrl) {
    Set<String> gadgetUrls = this.gadgetAdminMap.keySet();
    String key = null;
    for (String url : gadgetUrls) {
      if (url.endsWith("*") && gadgetUrl.startsWith(url.substring(0, url.length() - 1))) {
        if (key == null || (key != null && key.length() < url.length())) {
          key = url;
        }
      }
    }
    return key;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ContainerAdminData) {
      ContainerAdminData test = (ContainerAdminData) obj;
      Map<String, GadgetAdminData> testGadgetAdminMap = test.getGadgetAdminMap();
      return testGadgetAdminMap.equals(this.getGadgetAdminMap());

    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(this.gadgetAdminMap);
  }
}
