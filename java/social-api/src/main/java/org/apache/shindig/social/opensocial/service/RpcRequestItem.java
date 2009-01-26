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
package org.apache.shindig.social.opensocial.service;

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.social.opensocial.spi.SocialSpiException;
import org.apache.shindig.social.ResponseError;

import com.google.common.collect.Lists;
import com.google.common.collect.ImmutableList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.List;

/**
 * A JSON-RPC specific implementation of RequestItem
 */
public class RpcRequestItem extends RequestItem {

  private JSONObject data;

  static String getService(String rpcMethod) {
    return rpcMethod.substring(0, rpcMethod.indexOf('.'));
  }

  static String getOperation(String rpcMethod) {
    return rpcMethod.substring(rpcMethod.indexOf('.') + 1);
  }

  public RpcRequestItem(JSONObject rpc, SecurityToken token,
      BeanConverter converter) throws JSONException {
    super(getService(rpc.getString("method")),
        getOperation(rpc.getString("method")),
        token, converter);
    if (rpc.has("params")) {
      this.data = rpc.getJSONObject("params");
    } else {
      this.data = new JSONObject();
    }
  }

  @Override
  public String getParameter(String paramName) {
    try {
      if (data.has(paramName)) {
        return data.getString(paramName);
      } else {
        return null;
      }
    } catch (JSONException je) {
      throw new SocialSpiException(ResponseError.BAD_REQUEST, je.getMessage(), je);
    }
  }

  @Override
  public String getParameter(String paramName, String defaultValue) {
    try {
      if (data.has(paramName)) {
        return data.getString(paramName);
      } else {
        return defaultValue;
      }
    } catch (JSONException je) {
      throw new SocialSpiException(ResponseError.BAD_REQUEST, je.getMessage(), je);
    }
  }

  @Override
  public List<String> getListParameter(String paramName) {
    try {
      if (data.has(paramName)) {
        if (data.get(paramName) instanceof JSONArray) {
          JSONArray jsonArray = data.getJSONArray(paramName);
          List<String> returnVal = Lists.newArrayListWithExpectedSize(jsonArray.length());
          for (int i = 0; i < jsonArray.length(); i++) {
            returnVal.add(jsonArray.getString(i));
          }
          return returnVal;
        } else {
          // Allow up-conversion of non-array to array params.
          return ImmutableList.of(data.getString(paramName));
        }
      } else {
        return Collections.emptyList();
      }
    } catch (JSONException je) {
      throw new SocialSpiException(ResponseError.BAD_REQUEST, je.getMessage(), je);
    }
  }

  @Override
  public <T> T getTypedParameter(String parameterName, Class<T> dataTypeClass) {
    try {
      return converter.convertToObject(data.get(parameterName).toString(), dataTypeClass);
    } catch (JSONException je) {
      throw new SocialSpiException(ResponseError.BAD_REQUEST, je.getMessage(), je);
    }
  }

  @Override
  public<T> T getTypedParameters(Class<T> dataTypeClass) {
    return converter.convertToObject(data.toString(), dataTypeClass);
  }

  @Override
  public void applyUrlTemplate(String urlTemplate) {
    // No params in the URL
  }

  /** Method used only by tests */
  void setParameter(String paramName, String param) {
    try {
      data.put(paramName, param);
    } catch (JSONException je) {
      throw new IllegalArgumentException(je);
    }
  }

  /** Method used only by tests */
  void setJsonParameter(String paramName, JSONObject param) {
    try {
      data.put(paramName, param);
    } catch (JSONException je) {
      throw new IllegalArgumentException(je);
    }
  }

  /** Method used only by tests */
  void setListParameter(String paramName, List<String> params) {
    try {
      JSONArray arr = new JSONArray(params);
      data.put(paramName, arr);
    } catch (JSONException je) {
      throw new IllegalArgumentException(je);
    }
  }
}
