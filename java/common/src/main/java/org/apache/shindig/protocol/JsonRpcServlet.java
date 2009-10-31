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
import org.apache.shindig.protocol.multipart.FormDataItem;
import org.apache.shindig.protocol.multipart.MultipartFormParser;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

/**
 * JSON-RPC handler servlet.
 */
public class JsonRpcServlet extends ApiServlet {

  public static final Set<String> ALLOWED_CONTENT_TYPES =
      new ImmutableSet.Builder<String>().addAll(ContentTypes.ALLOWED_JSON_CONTENT_TYPES)
          .addAll(ContentTypes.ALLOWED_MULTIPART_CONTENT_TYPES).build();

  /**
   * In a multipart request, the form item with field name "request" will contain the
   * actual request, per the proposed Opensocial 0.9 specification.
   */
  public static final String REQUEST_PARAM = "request";
  
  /**
   * Error code for JSON-RPC requests with an error parsing JSON.
   */
  public static final int SC_JSON_PARSE_ERROR = -32700;
  
  private MultipartFormParser formParser;

  @Inject
  void setMultipartFormParser(MultipartFormParser formParser) {
    this.formParser = formParser;
  }
  
  @Override
  protected void doGet(HttpServletRequest servletRequest, HttpServletResponse servletResponse)
      throws IOException {
    setCharacterEncodings(servletRequest, servletResponse);
    servletResponse.setContentType(ContentTypes.OUTPUT_JSON_CONTENT_TYPE);

    SecurityToken token = getSecurityToken(servletRequest);
    if (token == null) {
      sendSecurityError(servletResponse);
      return;
    }

    try {
      JSONObject request = JsonConversionUtil.fromRequest(servletRequest);
      dispatch(request, null, servletRequest, servletResponse, token);
    } catch (JSONException je) {
      sendJsonParseError(je, servletResponse);
    }
  }

  @Override
  protected void doPost(HttpServletRequest servletRequest, HttpServletResponse servletResponse)
      throws IOException {
    setCharacterEncodings(servletRequest, servletResponse);
    servletResponse.setContentType(ContentTypes.OUTPUT_JSON_CONTENT_TYPE);

    try {
      checkContentTypes(ALLOWED_CONTENT_TYPES, servletRequest.getContentType());
      SecurityToken token = getSecurityToken(servletRequest);
      if (token == null) {
        sendSecurityError(servletResponse);
        return;
      }

      String content = null;
      Map<String, FormDataItem> formItems = Collections.emptyMap();  // default for most requests

      if (formParser.isMultipartContent(servletRequest)) {
        formItems = new HashMap<String, FormDataItem>();
        for (FormDataItem item : formParser.parse(servletRequest)) {
          if (item.isFormField() && REQUEST_PARAM.equals(item.getFieldName()) && content == null) {
            // As per spec, in case of a multipart/form-data content, there will be one form field
            // with field name as "request". It will contain the json request. Any further form
            // field or file item will not be parsed out, but will be exposed via getFormItem
            // method of RequestItem.
            if (!StringUtils.isEmpty(item.getContentType())) {
              checkContentTypes(ContentTypes.ALLOWED_JSON_CONTENT_TYPES, item.getContentType());
            }
            content = item.getAsString();
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
      sendJsonParseError(je, servletResponse);
    } catch (ContentTypes.InvalidContentTypeException icte) {
      sendBadRequest(icte, servletResponse);
    }
  }

  protected void dispatchBatch(JSONArray batch, Map<String, FormDataItem> formItems ,
      HttpServletRequest servletRequest, HttpServletResponse servletResponse,
      SecurityToken token) throws JSONException, IOException {
    // Use linked hash map to preserve order
    List<Future<?>> responses = Lists.newArrayListWithCapacity(batch.length());

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

  Object getJSONResponse(String key, ResponseItem responseItem) {
    Map<String, Object> result = Maps.newHashMap();
    if (key != null) {
      result.put("id", key);
    }
    if (responseItem.getErrorCode() < 200 ||
        responseItem.getErrorCode() >= 400) {
      result.put("error", getErrorJson(responseItem));
    } else {
      Object response = responseItem.getResponse();
      if (response instanceof DataCollection) {
        result.put("data", ((DataCollection) response).getEntry());
      } else if (response instanceof RestfulCollection) {
        Map<String, Object> map = Maps.newHashMap();
        RestfulCollection<?> collection = (RestfulCollection<?>) response;
        // Return sublist info
        if (collection.getTotalResults() != collection.getEntry().size()) {
          map.put("totalResults", collection.getTotalResults());
          map.put("startIndex", collection.getStartIndex());
          map.put("itemsPerPage", collection.getItemsPerPage());
        }

        if (!collection.isFiltered())
          map.put("filtered", collection.isFiltered());

        if (!collection.isUpdatedSince())
          map.put("updatedSince", collection.isUpdatedSince());

        if (!collection.isSorted())
          map.put("sorted", collection.isUpdatedSince());

        map.put("list", collection.getEntry());
        result.put("data", map);
      } else {
        result.put("data", response);
      }

      // TODO: put "code" for != 200?
    }
    return result;
  }

  /** Map of old-style error titles */
  private static final Map<Integer, String> errorTitles = ImmutableMap.<Integer, String> builder()
     .put(HttpServletResponse.SC_NOT_IMPLEMENTED, "notImplemented")
     .put(HttpServletResponse.SC_UNAUTHORIZED, "unauthorized")
     .put(HttpServletResponse.SC_FORBIDDEN, "forbidden")
     .put(HttpServletResponse.SC_BAD_REQUEST, "badRequest")
     .put(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "internalError")
     .put(HttpServletResponse.SC_EXPECTATION_FAILED, "limitExceeded")
     .build();
        
  // TODO(doll): Refactor the responseItem so that the fields on it line up with this format.
  // Then we can use the general converter to output the response to the client and we won't
  // be harcoded to json.
  private Object getErrorJson(ResponseItem responseItem) {
    Map<String, Object> error = new HashMap<String, Object>(2, 1);
    error.put("code", responseItem.getErrorCode());

    String message = errorTitles.get(responseItem.getErrorCode());
    if (message == null) {
      message = responseItem.getErrorMessage();
    } else {
      if (StringUtils.isNotBlank(responseItem.getErrorMessage())) {
        message += ": " + responseItem.getErrorMessage();
      }
    }
    
    if (StringUtils.isNotBlank(message)) {
      error.put("message", message);
    }

    if (responseItem.getResponse() != null) {
      error.put("data", responseItem.getResponse());
    }

    return error;
  }

  @Override
  protected void sendError(HttpServletResponse servletResponse, ResponseItem responseItem)
      throws IOException {
    jsonConverter.append(servletResponse.getWriter(), getErrorJson(responseItem));
  }

  private void sendBadRequest(Throwable t, HttpServletResponse response) throws IOException {
    sendError(response, new ResponseItem(HttpServletResponse.SC_BAD_REQUEST,
        "Invalid batch - " + t.getMessage()));
  }

  private void sendJsonParseError(JSONException e, HttpServletResponse response) throws IOException {
    sendError(response, new ResponseItem(SC_JSON_PARSE_ERROR,
        "Invalid JSON - " + e.getMessage()));
  }
}
