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
package org.apache.shindig.social;

import org.apache.shindig.gadgets.GadgetToken;

import org.json.JSONObject;

/**
 * Represents the request items that come from the json. Each RequestItem should
 * map to one ResponseItem.
 */
public class RequestItem {
  private String type;
  private JSONObject params;
  private GadgetToken token;

  public RequestItem(String type, JSONObject params, GadgetToken token) {
    this.type = type;
    this.params = params;
    this.token = token;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public JSONObject getParams() {
    return params;
  }

  public void setParams(JSONObject params) {
    this.params = params;
  }

  public GadgetToken getToken() {
    return token;
  }

  public void setToken(GadgetToken token) {
    this.token = token;
  }
}