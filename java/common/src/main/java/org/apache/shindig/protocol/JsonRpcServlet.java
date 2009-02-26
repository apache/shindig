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

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.util.JsonConversionUtil;
import org.apache.shindig.protocol.multipart.FormDataItem;
import org.apache.shindig.protocol.multipart.MultipartFormParser;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * JSON-RPC handler servlet.
 */
public class JsonRpcServlet extends ApiServlet {

  /**
   * In a multipart request, the form item with field name "request" will contain the
   * actual request, per the proposed Opensocial 0.9 specification.
   */
  public static final String REQUEST_PARAM = "request";
  private static final String CONTENT_TYPE_JSON = "application/json";
  
  private MultipartFormParser formParser;

  @Inject
  void setMultipartFormParser(MultipartFormParser formParser) {
    this.formParser = formParser;
  }
  
  @Override
  protected void doGet(HttpServletRequest servletRequest, HttpServletResponse servletResponse)
      throws IOException {
    SecurityToken token = getSecurityToken(servletRequest);
    if (token == null) {
      sendSecurityError(servletResponse);
      return;
    }

    try {
      setCharacterEncodings(servletRequest, servletResponse);
      JSONObject request = JsonConversionUtil.fromRequest(servletRequest);
      dispatch(request, null, servletRequest, servletResponse, token);
    } catch (JSONException je) {
      // FIXME
    }
  }

  @Override
  protected void doPost(HttpServletRequest servletRequest, HttpServletResponse servletResponse)
      throws IOException {
    SecurityToken token = getSecurityToken(servletRequest);
    if (token == null) {
      sendSecurityError(servletResponse);
      return;
    }

    setCharacterEncodings(servletRequest, servletResponse);
    servletResponse.setContentType(CONTENT_TYPE_JSON);

    try {
      String content = null;
      Map<String, FormDataItem> formItems = new HashMap<String, FormDataItem>();
      
      if (formParser.isMultipartContent(servletRequest)) {
        for (FormDataItem item : formParser.parse(servletRequest)) {
          if (item.isFormField() && content == null) {
            // As per spec, in case of a multipart/form-data content, there will be one form field
            // with field name as "request". It will contain the json request. Any further form
            // field or file item will not be parsed out, but will be exposed via getFormItem
            // method of RequestItem. Here we are lenient where a mime part which has content type
            // application/json will be considered as request.
            if (REQUEST_PARAM.equals(item.getFieldName()) ||
                CONTENT_TYPE_JSON.equals(item.getContentType())) {
              content = IOUtils.toString(item.getInputStream());
            }
          } else {
            formItems.put(item.getFieldName(), item);
          }
        }
        
        if (content == null) {
          content = "";
        }
      } else {
        content = IOUtils.toString(servletRequest.getInputStream(),
            servletRequest.getCharacterEncoding());
      }

      if ((content.indexOf('[') != -1) && content.indexOf('[') < content.indexOf('{')) {
        // Is a batch
        JSONArray batch = new JSONArray(content);
        dispatchBatch(batch, formItems, servletRequest, servletResponse, token);
      } else {
        JSONObject request = new JSONObject(content);
        dispatch(request, formItems, servletRequest, servletResponse, token);
      }
    } catch (JSONException je) {
      sendBadRequest(je, servletResponse);
    }
  }

  protected void dispatchBatch(JSONArray batch, Map<String, FormDataItem> formItems ,
      HttpServletRequest servletRequest, HttpServletResponse servletResponse,
      SecurityToken token) throws JSONException, IOException {
    // Use linked hash map to preserve order
    List<Future<?>> responses = Lists.newArrayListWithExpectedSize(batch.length());

    // Gather all Futures.  We do this up front so that
    // the first call to get() comes after all futures are created,
    // which allows for implementations that batch multiple Futures
    // into single requests.
    for (int i = 0; i < batch.length(); i++) {
      JSONObject batchObj = batch.getJSONObject(i);
      responses.add(getHandler(batchObj, servletRequest).execute(formItems, token, jsonConverter));
    }

    // Resolve each Future into a response.
    // TODO: should use shared deadline across each request
    List<Object> result = new ArrayList<Object>(batch.length());
    for (int i = 0; i < batch.length(); i++) {
      JSONObject batchObj = batch.getJSONObject(i);
      String key = null;
      if (batchObj.has("id")) {
        key = batchObj.getString("id");
      }
      result.add(getJSONResponse(key, getResponseItem(responses.get(i))));
    }
    
    jsonConverter.append(servletResponse.getWriter(), result);
  }

  protected void dispatch(JSONObject request, Map<String, FormDataItem> formItems,
      HttpServletRequest servletRequest, HttpServletResponse servletResponse,
      SecurityToken token) throws JSONException, IOException {
    String key = null;
    if (request.has("id")) {
      key = request.getString("id");
    }

    // getRpcHandler never returns null
    Future<?> future = getHandler(request, servletRequest).execute(formItems, token, jsonConverter);

    // Resolve each Future into a response.
    // TODO: should use shared deadline across each request
    ResponseItem response = getResponseItem(future);
    Object result = getJSONResponse(key, response);

    jsonConverter.append(servletResponse.getWriter(), result);
  }

  /**
   * Wrap call to dispatcher to allow for implementation specific overrides
   * and servlet-request contextual handling
   */
  protected RpcHandler getHandler(JSONObject rpc, HttpServletRequest request) {
    return dispatcher.getRpcHandler(rpc);
  }

  private Object getJSONResponse(String key, ResponseItem responseItem) {
    Map<String, Object> result = Maps.newHashMap();
    if (key != null) {
      result.put("id", key);
    }
    if (responseItem.getError() != null) {
      result.put("error", getErrorJson(responseItem));
    } else {
      Object response = responseItem.getResponse();

      if (response instanceof DataCollection) {
        result.put("data", ((DataCollection) response).getEntry());
      } else if (response instanceof RestfulCollection) {
        Map<String, Object> map = Maps.newHashMap();
        RestfulCollection<?> collection = (RestfulCollection<?>) response;
        map.put("startIndex", collection.getStartIndex());
        map.put("totalResults", collection.getTotalResults());
        map.put("list", collection.getEntry());
        result.put("data", map);
      } else {
        result.put("data", response);
      }
    }
    return result;
  }

  // TODO(doll): Refactor the responseItem so that the fields on it line up with this format.
  // Then we can use the general converter to output the response to the client and we won't
  // be harcoded to json.
  private Object getErrorJson(ResponseItem responseItem) {
    Map<String, Object> error = new HashMap<String, Object>(2, 1);
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
    jsonConverter.append(servletResponse.getWriter(), getErrorJson(responseItem));
  }

  private void sendBadRequest(Throwable t, HttpServletResponse response) throws IOException {
    sendError(response, new ResponseItem(ResponseError.BAD_REQUEST,
        "Invalid batch - " + t.getMessage()));
  }
}
