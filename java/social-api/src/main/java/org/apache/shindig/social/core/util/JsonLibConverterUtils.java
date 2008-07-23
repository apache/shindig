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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 *
 */
public class JsonLibConverterUtils {
  protected static final Log log = LogFactory
  .getLog(JsonLibConverterUtils.class);

  
  /**
   * @param jsonObject
   */
  public static final void dumpJsonObject(JSONObject jsonObject, String indent) {
    for (Object key : jsonObject.keySet()) {
      Object value = jsonObject.get(key);
      if (value instanceof JSONObject) {
        log.info(indent + key + ":JSONObject");
        dumpJsonObject((JSONObject) value, indent + "  ");
      } else if (value instanceof JSONArray) {
        log.info(indent + key + ":JSONArray " + ((JSONArray) value).size());
        dumpJsonArray((JSONArray) value, indent + "  ");
      } else {
        log.info(indent + key + ":" + value + ":"
            + (value == null ? "na" : value.getClass()));
      }
    }
  }

  
  /**
   * @param value
   * @param string
   */
  static void dumpJsonArray(JSONArray array, String indent) {
    for (Object value : array) {
      if (value instanceof JSONObject) {
        log.info(indent + ":JSONObject");
        dumpJsonObject((JSONObject) value, indent + "  ");
      } else if (value instanceof JSONArray) {
        log.info(indent + ":JSONArray " + ((JSONArray) value).size());
        dumpJsonArray((JSONArray) value, indent + "  ");
      } else {
        log.info(indent + ":" + value + ":"
            + (value == null ? "na" : value.getClass()));
      }
    }
  }
}
