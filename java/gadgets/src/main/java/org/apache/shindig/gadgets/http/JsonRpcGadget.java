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

package org.apache.shindig.gadgets.http;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents an individual gadget.
 */
public class JsonRpcGadget {
  private final String url;
  public String getUrl() {
    return url;
  }

  private final int moduleId;
  public int getModuleId() {
    return moduleId;
  }

  private final Map<String, String> userPrefs;
  public Map<String, String> getUserPrefs() {
    return userPrefs;
  }

  public JsonRpcGadget(JSONObject json) throws JSONException {
    url = json.getString("url");
    moduleId = json.getInt("moduleId");

    JSONObject tmpObj = json.optJSONObject("userPrefs");
    if (tmpObj == null) {
     userPrefs = Collections.emptyMap();
    } else {
      JSONArray keyNames = tmpObj.names();
      Map<String, String> prefs = new HashMap<String, String>();
      for (int i = 0, j = keyNames.length(); i < j; ++i) {
        String key = keyNames.getString(i);
        String value = tmpObj.getString(key);
        prefs.put(key, value);
      }
      userPrefs = Collections.unmodifiableMap(prefs);
    }
  }
}
