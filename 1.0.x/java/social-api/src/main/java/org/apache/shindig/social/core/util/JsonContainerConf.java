/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.social.core.util;

import java.util.LinkedList;
import java.util.List;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.google.inject.Inject;

import org.apache.shindig.common.ContainerConfig;
import org.apache.shindig.common.ContainerConfigException;
import org.apache.shindig.common.JsonContainerConfig;
import org.apache.shindig.social.ResponseError;
import org.apache.shindig.social.opensocial.spi.SocialSpiException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JsonContainerConf implements ContainerConf {

  private final static Logger logger = Logger.getLogger(JsonContainerConf.class
      .getName());
  private static String containerjs = "res://containers/default/container.js";
  private static String container = "default";
  private JSONObject containerJsonObject;
  private ContainerConfig config;
  private List<String> activityFieldsList;
  private JSONArray personFieldsJsonArray;

  @Inject
  public JsonContainerConf() {
    try {
      this.config = new JsonContainerConfig(containerjs);
    } catch (ContainerConfigException e) {
      logger.log(Level.SEVERE, "Not able to load container.js", e);
    }
  }

  public Object getContainerObject() throws ContainerConfigException {
    if (this.containerJsonObject == null) {
      containerJsonObject = (JSONObject) config.getJson(container, null);
    }
    return containerJsonObject;
  }

  public synchronized List<String> getActivityFieldsList()
      throws ContainerConfigException {
    populateActivityFieldList();
    return activityFieldsList;
  }

  @SuppressWarnings("unchecked")
  public void populateActivityFieldList() throws ContainerConfigException {
    if (containerJsonObject == null) {
      containerJsonObject = (JSONObject) getContainerObject();
    }
    if (this.activityFieldsList != null) {
      return;
    }
    JSONObject gfJsonObject;
    try {
      gfJsonObject = containerJsonObject.getJSONObject("gadgets.features");
    } catch (JSONException jsone) {
      throw new SocialSpiException(ResponseError.INTERNAL_ERROR,
          "Error while retrieving "
              + "JsonObject for gadgets.features from container.js", jsone);
    }
    Iterator itr = gfJsonObject.keys();
    String osKey = null;
    while (itr.hasNext()) {
      osKey = itr.next().toString();
      if (osKey.startsWith("opensocial")) {
        break;
      }
    }
    JSONObject osJsonObjectValue = null;
    JSONObject supportFieldsJsonObjectValue = null;
    JSONArray activityFields = null;
    try {
      osJsonObjectValue = gfJsonObject.getJSONObject(osKey);
      supportFieldsJsonObjectValue = osJsonObjectValue
          .getJSONObject("supportedFields");
      activityFields = supportFieldsJsonObjectValue.getJSONArray("activity");
      activityFieldsList = new LinkedList<String>();
      for (int i = 0; i < activityFields.length(); i++) {
        activityFieldsList.add(activityFields.get(i).toString());
      }
    } catch (JSONException jsone) {
      throw new SocialSpiException(ResponseError.INTERNAL_ERROR, jsone
          .toString(), jsone);
    }
  }

  public synchronized Object getPersonFields() throws ContainerConfigException {
    JSONObject gfJsonObject;
    JSONObject jsonObject = (JSONObject) this.getContainerObject();

    try {
      gfJsonObject = jsonObject.getJSONObject("gadgets.features");
    } catch (JSONException jsone) {
      throw new SocialSpiException(ResponseError.INTERNAL_ERROR,
          "Error while retrieving "
              + "JsonObject for gadgets.features from container.js", jsone);
    }
    Iterator<?> itr = gfJsonObject.keys();
    String osKey = null;
    while (itr.hasNext()) {
      osKey = itr.next().toString();
      if (osKey.startsWith("opensocial")) {
        break;
      }
    }
    JSONObject osJsonObjectValue = null;

    try {
      osJsonObjectValue = gfJsonObject.getJSONObject(osKey);
      JSONObject personFieldsJsonObject = osJsonObjectValue
          .getJSONObject("supportedFields");
      personFieldsJsonArray = personFieldsJsonObject.getJSONArray("person");
    } catch (JSONException jsone) {
      throw new SocialSpiException(ResponseError.INTERNAL_ERROR, jsone
          .toString(), jsone);
    }
    return personFieldsJsonArray;
  }
}
