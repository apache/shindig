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
package org.apache.shindig.protocol;

import com.google.common.base.Strings;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.servlet.HttpUtil;
import org.apache.shindig.common.util.JsonConversionUtil;
import org.apache.shindig.protocol.multipart.FormDataItem;
import org.apache.shindig.protocol.multipart.MultipartFormParser;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
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

  private MultipartFormParser formParser;

  @Inject
  void setMultipartFormParser(MultipartFormParser formParser) {
    this.formParser = formParser;
  }

  private String jsonRpcResultField = "result";
  private boolean jsonRpcBothFields = false;

  @Inject(optional = true)
  void setJsonRpcResultField(@Named("shindig.json-rpc.result-field")String jsonRpcResultField) {
    this.jsonRpcResultField = jsonRpcResultField;
    jsonRpcBothFields = "both".equals(jsonRpcResultField);
  }

  @Override
  protected void service(HttpServletRequest servletRequest, HttpServletResponse servletResponse)
      throws IOException {
    setCharacterEncodings(servletRequest, servletResponse);
    servletResponse.setContentType(ContentTypes.OUTPUT_JSON_CONTENT_TYPE);

    // only GET/POST
    String method = servletRequest.getMethod();

    if (!("GET".equals(method) || "POST".equals(method))) {
      sendError(servletResponse,
                new ResponseItem(HttpServletResponse.SC_BAD_REQUEST, "Only POST/GET Allowed"));
      return;
    }

    SecurityToken token = getSecurityToken(servletRequest);
    if (token == null) {
      sendSecurityError(servletResponse);
      return;
    }

    try {
      String content = null;
      String callback = null; // for JSONP
      Map<String,FormDataItem> formData = Maps.newHashMap();

      // Get content or deal with JSON-RPC GET
      if ("POST".equals(method)) {
        content = getPostContent(servletRequest, formData);
      } else if (this.isJSONPAllowed && HttpUtil.isJSONP(servletRequest)) {
        content = servletRequest.getParameter("request");
        callback = servletRequest.getParameter("callback");
      } else {
        // GET request, fromRequest() creates the json objects directly.
        JSONObject request = JsonConversionUtil.fromRequest(servletRequest);

        if (request != null) {
          dispatch(request, formData, servletRequest, servletResponse, token, null);
          return;
        }
      }

      if (content == null) {
        sendError(servletResponse, new ResponseItem(HttpServletResponse.SC_BAD_REQUEST, "No content specified"));
        return;
      }

      if (isContentJsonBatch(content)) {
        JSONArray batch = new JSONArray(content);
        dispatchBatch(batch, formData, servletRequest, servletResponse, token, callback);
      } else {
        JSONObject request = new JSONObject(content);
        dispatch(request, formData, servletRequest, servletResponse, token, callback);
      }
    } catch (JSONException je) {
      sendJsonParseError(je, servletResponse);
    } catch (IllegalArgumentException e) {
      // a bad jsonp request..
      sendBadRequest(e, servletResponse);
    }  catch (ContentTypes.InvalidContentTypeException icte) {
      sendBadRequest(icte, servletResponse);
    }
  }

  protected String getPostContent(HttpServletRequest request, Map<String,FormDataItem> formItems)
      throws ContentTypes.InvalidContentTypeException, IOException {
    String content = null;

    ContentTypes.checkContentTypes(ALLOWED_CONTENT_TYPES, request.getContentType());

    if (formParser.isMultipartContent(request)) {
      for (FormDataItem item : formParser.parse(request)) {
        if (item.isFormField() && REQUEST_PARAM.equals(item.getFieldName()) && content == null) {
          // As per spec, in case of a multipart/form-data content, there will be one form field
          // with field name as "request". It will contain the json request. Any further form
          // field or file item will not be parsed out, but will be exposed via getFormItem
          // method of RequestItem.
          if (!Strings.isNullOrEmpty(item.getContentType())) {
            ContentTypes.checkContentTypes(ContentTypes.ALLOWED_JSON_CONTENT_TYPES, item.getContentType());
          }
          content = item.getAsString();
        } else {
          formItems.put(item.getFieldName(), item);
        }
      }
    } else {
      content = IOUtils.toString(request.getInputStream(), request.getCharacterEncoding());
    }
    return content;
  }

  protected void dispatchBatch(JSONArray batch, Map<String, FormDataItem> formItems ,
      HttpServletRequest servletRequest, HttpServletResponse servletResponse,
      SecurityToken token, String callback) throws JSONException, IOException {
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

    // Generate the output
    Writer writer = servletResponse.getWriter();
    if (callback != null) writer.append(callback).append('(');
    jsonConverter.append(writer, result);
    if (callback != null) writer.append(");\n");
  }

  protected void dispatch(JSONObject request, Map<String, FormDataItem> formItems,
      HttpServletRequest servletRequest, HttpServletResponse servletResponse,
      SecurityToken token, String callback) throws JSONException, IOException {
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

    // Generate the output
    Writer writer = servletResponse.getWriter();
    if (callback != null) writer.append(callback).append('(');
    jsonConverter.append(writer, result);
    if (callback != null) writer.append(");\n");
  }

  /**
   *
   */
  protected void addResult(Map<String,Object> result, Object data) {
    if (jsonRpcBothFields) {
      result.put("result", data);
      result.put("data", data);
    } else {
      result.put(jsonRpcResultField, data);
    }
  }

  /**
   * Determine if the content contains a batch request
   *
   * @param content json content or null
   * @return true if content contains is a json array, not a json object or null
   */
  private boolean isContentJsonBatch(String content) {
    if (content == null) return false;
    return ((content.indexOf('[') != -1) && content.indexOf('[') < content.indexOf('{'));
  }
  /**
   * Wrap call to dispatcher to allow for implementation specific overrides
   * and servlet-request contextual handling
   */
  protected RpcHandler getHandler(JSONObject rpc, HttpServletRequest request) {
    return dispatcher.getRpcHandler(rpc);
  }

  protected Object getJSONResponse(String key, ResponseItem responseItem) {
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
        addResult(result, ((DataCollection) response).getEntry());
      } else if (response instanceof RestfulCollection) {
        Map<String, Object> map = Maps.newHashMap();
        RestfulCollection<?> collection = (RestfulCollection<?>) response;
        // Return sublist info
        if (collection.getTotalResults() != collection.getList().size()) {
          map.put("startIndex", collection.getStartIndex());
          map.put("itemsPerPage", collection.getItemsPerPage());
        }
        // always put in totalResults
        map.put("totalResults", collection.getTotalResults());

        // always add metadata for collections
        map.put("filtered", collection.isFiltered());
        map.put("updatedSince", collection.isUpdatedSince());
        map.put("sorted", collection.isSorted());

        map.put("list", collection.getList());
        addResult(result, map);
      } else {
        addResult(result, response);
      }

      // TODO: put "code" for != 200?
    }
    return result;
  }

  /** Map of old-style error titles */
  protected static final Map<Integer, String> errorTitles = ImmutableMap.<Integer, String> builder()
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
  protected Object getErrorJson(ResponseItem responseItem) {
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

    servletResponse.setStatus(responseItem.getErrorCode());
  }

  protected void sendBadRequest(Throwable t, HttpServletResponse response) throws IOException {
    sendError(response, new ResponseItem(HttpServletResponse.SC_BAD_REQUEST,
        "Invalid input - " + t.getMessage()));
  }

  protected void sendJsonParseError(JSONException e, HttpServletResponse response) throws IOException {
    sendError(response, new ResponseItem(HttpServletResponse.SC_BAD_REQUEST,
        "Invalid JSON - " + e.getMessage()));
  }
}
