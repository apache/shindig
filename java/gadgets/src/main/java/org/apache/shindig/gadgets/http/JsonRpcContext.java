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

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Handles pulling out context from a JSON object.
 */
public class JsonRpcContext {
  private final String language;
  private final String country;
  private final String view;
  private final boolean ignoreCache;

  public String getLanguage() {
    return language;
  }

  public String getCountry() {
    return country;
  }

  public String getView() {
    return view;
  }

  public boolean getIgnoreCache() {
    return ignoreCache;
  }

  /**
   * @param json
   * @throws JSONException
   */
  public JsonRpcContext(JSONObject json) throws JSONException {
    language = json.getString("language");
    country = json.getString("country");
    view = json.getString("view");
    if (json.has("ignoreCache")) {
      ignoreCache = json.getBoolean("ignoreCache");
    } else {
      ignoreCache = false;
    }
  }
}
