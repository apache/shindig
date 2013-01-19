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
package org.apache.shindig.gadgets.servlet;

import com.google.common.base.Preconditions;
import org.apache.commons.io.IOUtils;
import org.apache.shindig.common.servlet.HttpUtil;
import org.apache.shindig.common.servlet.InjectedServlet;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles RPC metadata requests.
 */
public class RpcServlet extends InjectedServlet {

  private static final long serialVersionUID = 1382573217773582182L;

  static final String GET_REQUEST_REQ_PARAM = "req";
  static final String GET_REQUEST_CALLBACK_PARAM = "callback";

  private static final Logger LOG = Logger.getLogger("org.apache.shindig.gadgets.servlet.RpcServlet");

  private transient JsonRpcHandler jsonHandler;
  private Boolean isJSONPAllowed;

  @Inject
  public void setJsonRpcHandler(JsonRpcHandler jsonHandler) {
    checkInitialized();
    this.jsonHandler = jsonHandler;
  }

  @Inject
  public void setJSONPAllowed(
      @Named("shindig.allowJSONP") Boolean isJSONPAllowed) {
    this.isJSONPAllowed = isJSONPAllowed;
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    String reqValue;
    String callbackValue;

    try {
      if (this.isJSONPAllowed) {
        HttpUtil.isJSONP(request);
        callbackValue = validateParameterValue(request, GET_REQUEST_CALLBACK_PARAM);
      } else {
        callbackValue = validateParameterValueNull(request, GET_REQUEST_CALLBACK_PARAM);
      }
      reqValue = validateParameterValue(request, GET_REQUEST_REQ_PARAM);
    } catch (IllegalArgumentException e) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      LOG.log(Level.INFO, e.getMessage(), e);
      return;
    }

    Result result = process(request, response, reqValue);
    if (result.isSuccess()) {
      if (callbackValue != null) {
        response.getWriter().write(callbackValue + '(' + result.getOutput() + ')');
      } else {
        response.getWriter().write(result.getOutput());
      }
    } else {
      response.getWriter().write(result.getOutput());
    }
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    try{
      InputStreamReader is = new InputStreamReader(request.getInputStream(),
          getRequestCharacterEncoding(request));
      String body = IOUtils.toString(is);
      Result result = process(request, response, body);
      response.getWriter().write(result.getOutput());
    } catch (UnsupportedEncodingException e) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      LOG.log(Level.INFO, e.getMessage(), e);
      response.getWriter().write("Unsupported input character set");
    }
  }

  private String validateParameterValue(HttpServletRequest request, String parameter)
      throws IllegalArgumentException {
    String result = request.getParameter(parameter);
    Preconditions.checkArgument(result != null, "No parameter '%s' specified", parameter);
    return result;
  }

  private String validateParameterValueNull(HttpServletRequest request, String parameter)
      throws IllegalArgumentException {
    String result = request.getParameter(parameter);
    Preconditions.checkArgument(result == null, "Wrong parameter '%s' found", parameter);
    return result;
  }

  private Result process(HttpServletRequest request, HttpServletResponse response, String body) {
    try {
      JSONObject req = new JSONObject(body);
      JSONObject resp = jsonHandler.process(req);
      response.setStatus(HttpServletResponse.SC_OK);
      response.setContentType("application/json; charset=utf-8");
      response.setHeader("Content-Disposition", "attachment;filename=rpc.txt");
      return new Result(resp.toString(), true);
    } catch (JSONException e) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return new Result("Malformed JSON request.", false);
    } catch (RpcException e) {
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      LOG.log(Level.INFO, e.getMessage(), e);
      return new Result(e.getMessage(), false);
    }
  }

  private String getRequestCharacterEncoding(HttpServletRequest request) {
    String encoding = request.getCharacterEncoding();
    if (encoding == null) {
      encoding = "UTF-8";
    }
    return encoding;
  }

  private static class Result {
    private final String output;
    private final boolean success;

    public Result(String output, boolean success) {
      this.output = output;
      this.success = success;
    }

    public String getOutput() {
      return output;
    }

    public boolean isSuccess() {
      return success;
    }
  }
}
