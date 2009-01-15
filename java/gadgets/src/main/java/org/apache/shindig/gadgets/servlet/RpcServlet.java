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

import org.apache.commons.io.IOUtils;
import org.apache.shindig.common.servlet.InjectedServlet;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.inject.Inject;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Handles RPC metadata requests.
 */
public class RpcServlet extends InjectedServlet {
  static final String GET_REQUEST_REQ_PARAM = "req";
  static final String GET_REQUEST_CALLBACK_PARAM = "callback";
  // Starts with alpha or underscore, followed by alphanum, underscore or period
  static final Pattern GET_REQUEST_CALLBACK_PATTERN = Pattern.compile("[A-Za-z_][A-Za-z0-9_\\.]+");

  private static final int POST_REQUEST_MAX_SIZE = 1024 * 128;
  private static final Logger logger = Logger.getLogger("org.apache.shindig.gadgets");

  private JsonRpcHandler jsonHandler;

  @Inject
  public void setJsonRpcHandler(JsonRpcHandler jsonHandler) {
    this.jsonHandler = jsonHandler;
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    String reqValue;
    String callbackValue;

    try {
      reqValue = validateParameterValue(request, GET_REQUEST_REQ_PARAM);
      callbackValue = validateParameterValue(request, GET_REQUEST_CALLBACK_PARAM);
      if (!GET_REQUEST_CALLBACK_PATTERN.matcher(callbackValue).matches()) {
        throw new IllegalArgumentException("Wrong format for parameter '" +
            GET_REQUEST_CALLBACK_PARAM + "' specified. Expected: " +
            GET_REQUEST_CALLBACK_PATTERN.toString());
      }

    } catch (IllegalArgumentException e) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      logger.log(Level.INFO, e.getMessage(), e);
      return;
    }

    Result result = process(request, response, reqValue.getBytes());
    response.getWriter().write(result.isSuccess()
        ? callbackValue + '(' + result.getOutput() + ')'
        : result.getOutput());
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException {

    int length = request.getContentLength();
    if (length <= 0) {
      logger.info("No Content-Length specified.");
      response.setStatus(HttpServletResponse.SC_LENGTH_REQUIRED);
      return;
    }
    if (length > POST_REQUEST_MAX_SIZE) {
      logger.info("Request size too large: " + length);
      response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
      return;
    }

    ServletInputStream is = request.getInputStream();
    byte[] body = IOUtils.toByteArray(is);
    if (body.length != length) {
      logger.info("Wrong size. Length: " + length + " real: " + body.length);
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }

    Result result = process(request, response, body);
    response.getWriter().write(result.getOutput());
  }

  private String validateParameterValue(HttpServletRequest request, String parameter)
      throws IllegalArgumentException {
    String result = request.getParameter(parameter);
    if (result == null) {
      throw new IllegalArgumentException("No parameter '" + parameter + "' specified.");
    }
    return result;
  }

  private Result process(HttpServletRequest request, HttpServletResponse response, byte[] body) {
    try {
      String encoding = getRequestCharacterEncoding(request);
      JSONObject req = new JSONObject(new String(body, encoding));
      JSONObject resp = jsonHandler.process(req);
      response.setStatus(HttpServletResponse.SC_OK);
      response.setContentType("application/json; charset=utf-8");
      response.setHeader("Content-Disposition", "attachment;filename=rpc.txt");
      return new Result(resp.toString(), true);
    } catch (UnsupportedEncodingException e) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      logger.log(Level.INFO, e.getMessage(), e);
      return new Result("Unsupported input character set", false);
    } catch (JSONException e) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return new Result("Malformed JSON request.", false);
    } catch (RpcException e) {
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      logger.log(Level.INFO, e.getMessage(), e);
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
