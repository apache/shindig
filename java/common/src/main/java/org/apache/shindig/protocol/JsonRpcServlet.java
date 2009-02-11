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
package org.apache.shindig.protocol;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.util.JsonConversionUtil;

import com.google.common.collect.Lists;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Future;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * JSON-RPC handler servlet.
 */
public class JsonRpcServlet extends ApiServlet {

  @Override
  protected void doGet(HttpServletRequest servletRequest,
      HttpServletResponse servletResponse)
      throws ServletException, IOException {
    SecurityToken token = getSecurityToken(servletRequest);
    if (token == null) {
      sendSecurityError(servletResponse);
      return;
    }

    try {
      setCharacterEncodings(servletRequest, servletResponse);
      JSONObject request = JsonConversionUtil.fromRequest(servletRequest);
      dispatch(request, servletRequest, servletResponse, token);
    } catch (JSONException je) {
      // FIXME
    }
  }

  @Override
  protected void doPost(HttpServletRequest servletRequest,
      HttpServletResponse servletResponse)
      throws ServletException, IOException {
    SecurityToken token = getSecurityToken(servletRequest);
    if (token == null) {
      sendSecurityError(servletResponse);
      return;
    }

    setCharacterEncodings(servletRequest, servletResponse);
    servletResponse.setContentType("application/json");

    try {
      String content = IOUtils.toString(servletRequest.getInputStream(),
          servletRequest.getCharacterEncoding());
      if ((content.indexOf('[') != -1) && content.indexOf('[') < content.indexOf('{')) {
        // Is a batch
        JSONArray batch = new JSONArray(content);
        dispatchBatch(batch, servletRequest, servletResponse, token);
      } else {
        JSONObject request = new JSONObject(content);
        dispatch(request, servletRequest, servletResponse, token);
      }
    } catch (JSONException je) {
      sendBadRequest(je, servletResponse);
    }
  }

  protected void dispatchBatch(JSONArray batch, HttpServletRequest servletRequest,
      HttpServletResponse servletResponse, SecurityToken token) throws JSONException, IOException {
    // Use linked hash map to preserve order
    List<Future<?>> responses = Lists.newArrayListWithExpectedSize(batch.length());

    // Gather all Futures.  We do this up front so that
    // the first call to get() comes after all futures are created,
    // which allows for implementations that batch multiple Futures
    // into single requests.
    for (int i = 0; i < batch.length(); i++) {
      JSONObject batchObj = batch.getJSONObject(i);
      responses.add(getHandler(batchObj, servletRequest).execute(token, jsonConverter));
    }

    // Resolve each Future into a response.
    // TODO: should use shared deadline across each request
    JSONArray result = new JSONArray();
    for (int i = 0; i < batch.length(); i++) {
      JSONObject batchObj = batch.getJSONObject(i);
      String key = null;
      if (batchObj.has("id")) {
        key = batchObj.getString("id");
      }
      result.put(getJSONResponse(key, getResponseItem(responses.get(i))));
    }
    servletResponse.getWriter().write(result.toString());
  }

  protected void dispatch(JSONObject request, HttpServletRequest servletRequest,
      HttpServletResponse servletResponse, SecurityToken token) throws JSONException, IOException {
    String key = null;
    if (request.has("id")) {
      key = request.getString("id");
    }

    // getRpcHandler never returns null
    Future future = getHandler(request, servletRequest).execute(token, jsonConverter);

    // Resolve each Future into a response.
    // TODO: should use shared deadline across each request
    ResponseItem response = getResponseItem(future);
    JSONObject result = getJSONResponse(key, response);
    servletResponse.getWriter().write(result.toString());
  }

  /**
   * Wrap call to dispatcher to allow for implementation specific overrides
   * and servlet-request contextual handling
   */
  protected RpcHandler getHandler(JSONObject rpc, HttpServletRequest request) {
    return dispatcher.getRpcHandler(rpc);
  }

  private JSONObject getJSONResponse(String key, ResponseItem responseItem) throws JSONException {
    JSONObject result = new JSONObject();
    if (key != null) {
      result.put("id", key);
    }
    if (responseItem.getError() != null) {
      result.put("error", getErrorJson(responseItem));
    } else {
      Object response = responseItem.getResponse();
      Object converted = jsonConverter.convertToJson(response);

      if (response instanceof RestfulCollection) {
        // FIXME this is a little hacky because of the field names in the RestfulCollection
        ((JSONObject)converted).put("list", ((JSONObject)converted).remove("entry"));
        result.put("data", converted);
      } else if (response instanceof DataCollection) {
        if (((JSONObject)converted).has("entry")) {
          result.put("data", ((JSONObject)converted).get("entry"));
        }
      } else {
        result.put("data", converted);
      }
    }
    return result;
  }

  // TODO(doll): Refactor the responseItem so that the fields on it line up with this format.
  // Then we can use the general converter to output the response to the client and we won't
  // be harcoded to json.
  private JSONObject getErrorJson(ResponseItem responseItem) throws JSONException {
    JSONObject error = new JSONObject();
    error.put("code", responseItem.getError().getHttpErrorCode());

    String message = responseItem.getError().toString();
    if (StringUtils.isNotBlank(responseItem.getErrorMessage())) {
      message += ": " + responseItem.getErrorMessage();
    }
    error.put("message", message);
    return error;
  }

  @Override
  protected void sendError(HttpServletResponse servletResponse, ResponseItem responseItem)
      throws IOException {
    try {
      JSONObject error = getErrorJson(responseItem);
      servletResponse.getWriter().write(error.toString());
    } catch (JSONException je) {
      // This really shouldn't ever happen
      servletResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "Error generating error response " + je.getMessage());
    }
  }

  private void sendBadRequest(Throwable t, HttpServletResponse response) throws IOException {
    sendError(response, new ResponseItem(ResponseError.BAD_REQUEST,
        "Invalid batch - " + t.getMessage()));
  }
}
