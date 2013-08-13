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

import java.net.MalformedURLException;
import java.net.URL;
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
  private static final String STAR = "*";
  private static final String HTTP = "http";
  private static final String HTTPS = "https";
  private static final int HTTP_PORT = 80;
  private static final int HTTPS_PORT = 443;

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
    String normalizedGadgetUrl = createUrlWithPort(gadgetUrl);
    String key = null;
    for (String url : gadgetUrls) {
      String normalizedUrl = createUrlWithPort(url);
      if (normalizedUrl.endsWith(STAR)
              && normalizedGadgetUrl.startsWith(normalizedUrl.substring(0,
                      normalizedUrl.length() - 1))) {
        if (key == null || (key != null && key.length() < normalizedUrl.length())) {
          key = url;
        }
      } else if (normalizedUrl.equals(normalizedGadgetUrl)) {
        key = url;
        break;
      }
    }
    return key;
  }

  /**
   * Creates a new URL with the default port if one is not already there.
   *
   * @param gadgetUrl
   *          The gadget URL to add the port to.
   * @return A new URL with the default port.
   */
  private String createUrlWithPort(String gadgetUrl) {
    try {
      URL origUrl = new URL(gadgetUrl);
      URL urlWithPort = null;
      String origHost = origUrl.getHost();
      if (origUrl.getPort() <= 0 && origHost != null && origHost.length() != 0
              && !STAR.equals(origHost)) {
        if (origUrl.getProtocol().equalsIgnoreCase(HTTP)) {
          urlWithPort = new URL(origUrl.getProtocol(), origUrl.getHost(), HTTP_PORT, origUrl.getFile());
        }
        else if (origUrl.getProtocol().equalsIgnoreCase(HTTPS)) {
          urlWithPort = new URL(origUrl.getProtocol(), origUrl.getHost(), HTTPS_PORT, origUrl.getFile());
        }
        return urlWithPort == null ? origUrl.toString() : urlWithPort.toString();
      } else {
        return origUrl.toString();
      }
    } catch (MalformedURLException e) {
      return gadgetUrl;
    }
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
