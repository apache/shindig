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
package org.apache.shindig.social.opensocial.service;

import org.apache.shindig.common.SecurityToken;
import org.apache.shindig.social.ResponseItem;
import org.apache.shindig.social.ResponseError;
import org.apache.shindig.social.opensocial.spi.DataCollection;
import org.apache.shindig.social.opensocial.spi.RestfulCollection;

import com.google.common.collect.Maps;
import com.google.common.collect.Lists;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;
import java.util.List;
import java.util.concurrent.Future;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * JSON-RPC handler servlet.
 */
public class JsonRpcServlet extends ApiServlet {

  protected void doGet(HttpServletRequest servletRequest,
      HttpServletResponse servletResponse)
      throws ServletException, IOException {
    try {
      JSONObject request = JsonConversionUtils.fromRequest(servletRequest);
      SecurityToken token = getSecurityToken(servletRequest);
      dispatch(request, servletResponse, token);
    } catch (JSONException je) {
      // FIXME
    }
  }

  protected void doPost(HttpServletRequest servletRequest,
      HttpServletResponse servletResponse)
      throws ServletException, IOException {
    SecurityToken token = getSecurityToken(servletRequest);
    try {
      String content = IOUtils.toString(servletRequest.getReader());
      if ((content.indexOf('[') != -1) && content.indexOf('[') < content.indexOf('{')) {
        //Is a batch
        JSONArray batch = new JSONArray(content);
        dispatchBatch(batch, servletResponse, token);
      } else {
        JSONObject request = new JSONObject(content);
        dispatch(request, servletResponse, token);
      }
    } catch (JSONException je) {
      sendBadRequest(je, servletResponse);
    }

  }

  private void dispatchBatch(JSONArray batch, HttpServletResponse servletResponse,
      SecurityToken token) throws JSONException, IOException {
    // Use linked hash map to preserve order
    List<Future<? extends ResponseItem>> responses = Lists.newArrayListWithExpectedSize(batch.length());

    // Gather all Futures.  We do this up front so that
    // the first call to get() comes after all futures are created,
    // which allows for implementations that batch multiple Futures
    // into single requests.
    for (int i = 0; i < batch.length(); i++) {
      JSONObject batchObj = batch.getJSONObject(i);
      RpcRequestItem requestItem = new RpcRequestItem(batchObj, token, jsonConverter);
      responses.add(handleRequestItem(requestItem));
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

  private void dispatch(JSONObject request, HttpServletResponse servletResponse,
      SecurityToken token) throws JSONException, IOException {
    String key = null;
    if (request.has("id")) {
      key = request.getString("id");
    }
    RpcRequestItem requestItem = new RpcRequestItem(request, token, jsonConverter);

    // Resolve each Future into a response.
    // TODO: should use shared deadline across each request
    ResponseItem response = getResponseItem(handleRequestItem(requestItem));
    JSONObject result = getJSONResponse(key, response);
    servletResponse.getWriter().write(result.toString());
  }

  private JSONObject getJSONResponse(String key, ResponseItem response) throws JSONException {
    JSONObject result = new JSONObject();
    if (key != null) {
      result.put("id", key);
    }
    if (response.getError() != null) {
      JSONObject error = new JSONObject();
      error.put("code", response.getError().getHttpErrorCode());
      error.put("message", response.getErrorMessage());
      result.put("error", error);
    } else {
      if (response.getResponse() instanceof RestfulCollection) {
        //FIXME this is a little hacky because of the field names in the DataCollection
        JSONObject coll = (JSONObject) jsonConverter.convertToJson(response.getResponse());
        coll.put("list", coll.remove("entry"));
        result.put("data", coll);
      } else if (response.getResponse() instanceof DataCollection) {
        //FIXME this is a little hacky because of the field names in the DataCollection
        JSONObject coll = (JSONObject) jsonConverter.convertToJson(response.getResponse());
        result.put("data", coll.get("entry"));
      } else {
        result.put("data", jsonConverter.convertToJson(response.getResponse()));
      }
    }
    return result;
  }

  private void sendBadRequest(Throwable t, HttpServletResponse response) throws IOException {
    try {
      JSONObject error = new JSONObject();
      error.put("code", ResponseError.BAD_REQUEST.getHttpErrorCode());
      error.put("message", "Invalid batch - " + t.getMessage());
      response.getWriter().write(error.toString());
    } catch (JSONException je) {
      // This really shouldnt ever happen
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "Error generating error response " + je.getMessage());
    }
  }

}
