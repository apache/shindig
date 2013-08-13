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

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.admin.FeatureAdminData.Type;
import org.apache.shindig.gadgets.features.FeatureRegistry;
import org.apache.shindig.gadgets.features.FeatureRegistryProvider;
import org.apache.shindig.gadgets.spec.Feature;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.caja.util.Sets;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * A simple implementation of a gadget administration store.
 *
 * @since 2.5.0
 */
@Singleton
public class BasicGadgetAdminStore implements GadgetAdminStore {

  // Key in the container config which indicated whether white-listing is turned on.
  private static final String WHITELIST_KEY = "gadgets.admin.enableGadgetWhitelist";

  // Key in the container config which indicates whether feature administration is turned on.
  private static final String ENABLE_FEATURE_ADMIN = "gadgets.admin.enableFeatureAdministration";

  private static final Logger LOG = Logger.getLogger(BasicGadgetAdminStore.class.getName());

  private static final String GADGETS = "gadgets";
  private static final String FEATURES = "features";
  private static final String FEATURES_NAMES = "names";
  private static final String TYPE = "type";
  private static final String RPC = "rpc";
  private static final String ADDITIONAL_RPC_SERVICE_IDS = "additionalServiceIds";
  private static final String BLACKLIST = "blacklist";
  private static final String CORE_FEATURE = "core";

  private ServerAdminData serverAdminData;
  private FeatureRegistryProvider featureRegistryProvider;
  private ContainerConfig config;

  /**
   * Constructor.
   */
  @Inject
  public BasicGadgetAdminStore(FeatureRegistryProvider featureRegistryProvider,
          ContainerConfig config, ServerAdminData serverAdminData) {
    this.serverAdminData = serverAdminData;
    this.featureRegistryProvider = featureRegistryProvider;
    this.config = config;
  }

  /**
   * Inits the store from a JSON String representing the gadget administration information.
   *
   * @param store
   *          a JSON String representing the gadget administration information.
   * @throws GadgetException thrown when the store cannot be initiated.
   */
  public void init(String store) throws GadgetException {
    try {
      JSONObject json = new JSONObject(store);
      Iterator<?> iter = json.keys();
      String container;
      while (iter.hasNext()) {
        container = (String) iter.next();
        serverAdminData.addContainerAdminData(container,
                createContainerData(container, json.getJSONObject(container)));
      }
    } catch (JSONException e) {
      throw new GadgetException(GadgetException.Code.GADGET_ADMIN_STORAGE_ERROR, e);
    }
  }

  /**
   * Creates container security information from a JSON object.
   *
   * @param container
   *          the container the security information is for.
   * @param containerJson
   *          the JSON object representing the information.
   * @return container admin data
   * @throws JSONException
   *           thrown when we cannot get the information from the JSON object
   */
  private ContainerAdminData createContainerData(String container, JSONObject containerJson)
          throws JSONException, GadgetException {
    ContainerAdminData containerData = new ContainerAdminData();
    if (containerJson.has(GADGETS)) {
      containerData = new ContainerAdminData(
              createGadgetAdminDataMap(containerJson.getJSONObject(GADGETS)));
    }
    return containerData;
  }

  /**
   * Creates an RpcAdminData object from a JSON object.
   *
   * @param rpcJson
   *          the JSON object representing the RPC admin data.
   * @return an RpcAdminData object.
   * @throws JSONException thrown when the RpcAdminData object cannot be created.
   */
  private RpcAdminData createRpcAdminData(JSONObject rpcJson) throws JSONException {
    RpcAdminData adminData = new RpcAdminData();
    if(rpcJson.has(ADDITIONAL_RPC_SERVICE_IDS)) {
      JSONArray ids = rpcJson.getJSONArray(ADDITIONAL_RPC_SERVICE_IDS);
      for(int i = 0; i < ids.length(); i++) {
        adminData.addAdditionalRpcServiceId(ids.getString(i));
      }
    }
    return adminData;
  }

  /**
   * Creates a map of gadget administration data.
   *
   * @param gadgetsJson
   *          the JSON object representing the admin data.
   * @return a map of gadget administration data.
   * @throws JSONException
   *           thrown when the map cannot be created.
   */
  private Map<String, GadgetAdminData> createGadgetAdminDataMap(JSONObject gadgetsJson)
          throws JSONException {
    Map<String, GadgetAdminData> map = Maps.newHashMap();
    Iterator<?> keys = gadgetsJson.keys();
    String gadgetUrl;
    JSONObject gadgetJson;
    while (keys.hasNext()) {
      gadgetUrl = (String) keys.next();
      gadgetJson = gadgetsJson.getJSONObject(gadgetUrl);
      map.put(gadgetUrl, createGadgetAdminData(gadgetJson));
    }
    return map;
  }

  /**
   * Creates a gadget administration data.
   *
   * @param gadgetJson
   *          the gadget JSON object.
   * @return gadget administration data.
   * @throws JSONException
   *           thrown when the information cannot found in the JSON object.
   */
  private GadgetAdminData createGadgetAdminData(JSONObject gadgetJson) throws JSONException {
    FeatureAdminData featureData = new FeatureAdminData();
    RpcAdminData rpcData = new RpcAdminData();
    if(gadgetJson.has(FEATURES)) {
      featureData = createFeatureAdminData(gadgetJson.getJSONObject(FEATURES));
    }
    if(gadgetJson.has(RPC)) {
      rpcData = createRpcAdminData(gadgetJson.getJSONObject(RPC));
    }
    return new GadgetAdminData(featureData, rpcData);
  }

  /**
   * Creates the feature admin data.
   *
   * @param featuresJson
   *          The JSON object representing the feature admin data.
   * @return Feature admin data.
   * @throws JSONException Thrown when the JSON cannot be parsed.
   */
  private FeatureAdminData createFeatureAdminData(JSONObject featuresJson) throws JSONException {
    FeatureAdminData data = new FeatureAdminData();
    if (featuresJson.has(FEATURES_NAMES)) {
      JSONArray features = featuresJson.getJSONArray(FEATURES_NAMES);
      for (int i = 0; i < features.length(); i++) {
        data.addFeature(features.getString(i));
      }
    }

    data.setType(Type.WHITELIST);
    if (!data.getFeatures().contains(CORE_FEATURE)) {
      // Add the core feature since every gadget needs this and it can't be disabled
      data.addFeature(CORE_FEATURE);
    }
    if (featuresJson.has(TYPE)) {
      String type = featuresJson.getString(TYPE);
      if (type.equalsIgnoreCase(BLACKLIST)) {
        data.setType(Type.BLACKLIST);
        //We need core for everything so remove it if it is blacklisted
        data.removeFeature(CORE_FEATURE);
      }
    }
    return data;
  }

  public GadgetAdminData getGadgetAdminData(String container, String gadgetUrl) {
    GadgetAdminData data = null;
    if (serverAdminData.hasContainerAdminData(container)) {
      ContainerAdminData containerData = serverAdminData.getContainerAdminData(container);
      if (containerData.hasGadgetAdminData(gadgetUrl)) {
        data = containerData.getGadgetAdminData(gadgetUrl);
      }
    }
    return data;
  }

  public void setGadgetAdminData(String container, String gadgetUrl, GadgetAdminData adminData) {
    if (serverAdminData.hasContainerAdminData(container)) {
      ContainerAdminData containerData = serverAdminData.getContainerAdminData(container);
      containerData.addGadgetAdminData(gadgetUrl, adminData);
    }
  }

  public ContainerAdminData getContainerAdminData(String container) {
    ContainerAdminData data = null;
    if (serverAdminData.hasContainerAdminData(container)) {
      data = serverAdminData.getContainerAdminData(container);
    }
    return data;
  }

  public void setContainerAdminData(String container, ContainerAdminData containerAdminData) {
    serverAdminData.addContainerAdminData(container, containerAdminData);
  }

  public ServerAdminData getServerAdminData() {
    return serverAdminData;
  }

  /**
   * Safely gets the container from the gadget by doing null checks.
   *
   * @param gadget
   *          The gadget to get the container from.
   * @return The container.
   */
  private String getSafeContainerFromGadget(Gadget gadget) {
    GadgetContext context = gadget.getContext();
    if (context != null) {
      return context.getContainer();
    }
    return null;
  }

  /**
   * Safely gets the gadget's URL from the gadget by doing null checks.
   *
   * @param gadget
   *          The gadget to get the URL from.
   * @return The gadget's URL.
   */
  private String getSafeGadgetUrlFromGadget(Gadget gadget) {
    GadgetSpec spec = gadget.getSpec();
    if (spec != null) {
      Uri gadgetUri = spec.getUrl();
      if (gadgetUri != null) {
        return gadgetUri.toString();
      }
    }
    return null;
  }

  public boolean checkFeatureAdminInfo(Gadget gadget) {
    String container = getSafeContainerFromGadget(gadget);
    String gadgetUrl = getSafeGadgetUrlFromGadget(gadget);
    if (container == null || gadgetUrl == null) {
      return false;
    }

    if (!isFeatureAdminEnabled(container)) {
      return true;
    }

    GadgetContext context = gadget.getContext();
    try {
      FeatureRegistry featureRegistry = featureRegistryProvider.get(context.getRepository());
      if (!hasGadgetAdminData(container, gadgetUrl)) {
        return false;
      }

      FeatureAdminData featureAdminData = this.getGadgetAdminData(container, gadgetUrl)
              .getFeatureAdminData();

      Set<String> features = featureAdminData.getFeatures();
      if(featureAdminData.getType() == Type.WHITELIST) {
        //If the admin has specified a whitelist get all the dependencies for the features the admin
        //has whitelisted and add them as well.  Blacklists need to be more specific.
        features = Sets.newHashSet(featureRegistry.getFeatures(features));
      }

      List<String> gadgetFeatures = featureRegistry.getFeatures(getRequiredGadgetFeatures(gadget));

      return areAllFeaturesAllowed(Sets.immutableSet(features),
              gadgetFeatures, featureAdminData);
    } catch (GadgetException e) {
      LOG.log(Level.WARNING, "Exception while getting the FeatureRegistry.");
      return false;
    }

  }

  /**
   * Gets all required gadget features.
   *
   * @param gadget
   *          The gadget to get the gadget features for.
   * @return The required gadget features.
   */
  private List<String> getRequiredGadgetFeatures(Gadget gadget) {
    List<String> featureNames = Lists.newArrayList();
    List<Feature> features = gadget.getSpec().getModulePrefs().getAllFeatures();
    for (Feature feature : features) {
      if (feature.getRequired()) {
        featureNames.add(feature.getName());
      }
    }
    return featureNames;
  }

  /**
   * Checks the features for a gadget to see if they are allowed.
   *
   * @param featuresForGadget
   *          a set of features that the admin has either whitelist or blacklisted.
   * @param gadgetFeatures
   *          a list of features required by the gadget.
   * @param featureAdminData
   *          the feature admin data for the gadget.
   * @return true if all the features for the gadget are allowed, false otherwise.
   */
  private boolean areAllFeaturesAllowed(Set<String> featuresForGadget, List<String> gadgetFeatures,
          FeatureAdminData featureAdminData) {
    switch (featureAdminData.getType()) {
    case BLACKLIST:
      for (String feature : gadgetFeatures) {
        if (featuresForGadget.contains(feature)) {
          return false;
        }
      }

      break;
    case WHITELIST:
    default:
      return featuresForGadget.containsAll(gadgetFeatures);
    }
    return true;
  }

  public boolean isAllowedFeature(Feature feature, Gadget gadget) {
    String container = getSafeContainerFromGadget(gadget);
    String gadgetUrl = getSafeGadgetUrlFromGadget(gadget);
    if (container == null || gadgetUrl == null) {
      return false;
    }
    if (!isFeatureAdminEnabled(container)) {
      return true;
    }
    if (!hasGadgetAdminData(container, gadgetUrl)) {
      // If feature administration is not enabled assume the feature is allowed
      return false;
    }
    GadgetAdminData gadgetAdminData = getGadgetAdminData(container, gadgetUrl);

    FeatureAdminData featureAdminData = gadgetAdminData.getFeatureAdminData();
    String featureName = feature.getName();
    switch (featureAdminData.getType()) {
    case BLACKLIST:
      return !featureAdminData.getFeatures().contains(featureName);
    case WHITELIST:
    default:
      return featureAdminData.getFeatures().contains(featureName);
    }
  }

  /**
   * Determines whether we have gadget administration data for a gadget.
   *
   * @param container
   *          The container the gadget is in.
   * @param gadgetUrl
   *          The gadget to check.
   * @return true if we do have gadget administration data false otherwise.
   */
  private boolean hasGadgetAdminData(String container, String gadgetUrl) {
    return this.getGadgetAdminData(container, gadgetUrl) != null;
  }

  public boolean isWhitelisted(String container, String gadgetUrl) {
    if (isWhitelistingEnabled(container)) {
      return hasGadgetAdminData(container, gadgetUrl);
    } else {
      // If the white list checking is not enabled just assume it is there
      return true;
    }
  }

  /**
   * Determines whether whitelisting is enabled for a container.
   *
   * @param container
   *          The container to check.
   * @return true if whitelisting is enabled for the container false otherwise.
   */
  private boolean isWhitelistingEnabled(String container) {
    return config.getBool(container, WHITELIST_KEY);
  }

  /**
   * Determines whether feature administration is enabled for a container.
   *
   * @param container
   *          The container to check.
   * @return true if feature administration is enabled for the container false otherwise.
   */
  private boolean isFeatureAdminEnabled(String container) {
    return config.getBool(container, ENABLE_FEATURE_ADMIN);
  }

  public Set<String> getAdditionalRpcServiceIds(Gadget gadget) {
    GadgetAdminData gadgetData = this.getGadgetAdminData(getSafeContainerFromGadget(gadget),
            getSafeGadgetUrlFromGadget(gadget));
    Set<String> ids = Sets.newHashSet();
    if(gadgetData != null) {
      ids.addAll(gadgetData.getRpcAdminData().getAdditionalRpcServiceIds());
    }
    return ids;
  }
}
