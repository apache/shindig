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

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for RpcServlet.
 */
public class RpcServletTest extends Assert {
  private RpcServlet servlet;
  private JsonRpcHandler handler;

  @Before
  public void setUp() throws Exception {
    servlet = new RpcServlet();
    handler = createMock(JsonRpcHandler.class);
    servlet.setJsonRpcHandler(handler);
    servlet.setJSONPAllowed(true);
  }

  @Test
  public void testDoGetNormal() throws Exception {
    HttpServletRequest request = createGetRequest("{\"gadgets\":[]}",
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz._");
    HttpServletResponse response = createHttpResponse("Content-Disposition",
        "attachment;filename=rpc.txt", "application/json; charset=utf-8",
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz._({\"GADGETS\":[]})",
        HttpServletResponse.SC_OK);
    JSONObject handlerResponse = new JSONObject("{\"GADGETS\":[]}");
    expect(handler.process(isA(JSONObject.class))).andReturn(handlerResponse);
    replay(handler);
    servlet.doGet(request, response);
    verify(response);
  }

  @Test
  public void testDisallowJSONP() throws Exception {
    servlet.setJSONPAllowed(false);
    HttpServletRequest request = createGetRequest("{\"gadgets\":[]}",null);
    HttpServletResponse response = createHttpResponse("Content-Disposition",
        "attachment;filename=rpc.txt", "application/json; charset=utf-8",
        "{\"GADGETS\":[]}", HttpServletResponse.SC_OK);
    JSONObject handlerResponse = new JSONObject("{\"GADGETS\":[]}");
    expect(handler.process(isA(JSONObject.class))).andReturn(handlerResponse);
    replay(handler);
    servlet.doGet(request, response);
    verify(response);
    servlet.setJSONPAllowed(true);
  }

  @Test
  public void testDoGetWithHandlerRpcException() throws Exception {
    HttpServletRequest request = createGetRequest("{\"gadgets\":[]}", "function");
    HttpServletResponse response = createHttpResponse("rpcExceptionMessage",
        HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    expect(handler.process(isA(JSONObject.class))).andThrow(
        new RpcException("rpcExceptionMessage"));
    replay(handler);
    servlet.doGet(request, response);
    verify(response);
  }

  @Test
  public void testDoGetWithHandlerJsonException() throws Exception {
    HttpServletRequest request = createGetRequest("{\"gadgets\":[]}", "function");
    HttpServletResponse response = createHttpResponse("Malformed JSON request.",
        HttpServletResponse.SC_BAD_REQUEST);
    expect(handler.process(isA(JSONObject.class))).andThrow(new JSONException("json"));
    replay(handler);
    servlet.doGet(request, response);
    verify(response);
  }

  @Test
  public void testDoGetWithMissingReqParam() throws Exception {
    HttpServletRequest request = createGetRequest(null, "function");
    HttpServletResponse response = createHttpResponse(null, HttpServletResponse.SC_BAD_REQUEST);
    servlet.doGet(request, response);
    verify(response);
  }

  @Test
  public void testDoGetWithMissingCallbackParam() throws Exception {
    HttpServletRequest request = createGetRequest("{\"gadgets\":[]}", null);
    HttpServletResponse response = createHttpResponse(null, HttpServletResponse.SC_BAD_REQUEST);
    servlet.doGet(request, response);
    verify(response);
  }

  @Test
  public void testDoGetWithBadCallbackParamValue() throws Exception {
    HttpServletRequest request = createGetRequest("{\"gadgets\":[]}", "/'!=");
    HttpServletResponse response = createHttpResponse(null, HttpServletResponse.SC_BAD_REQUEST);
    servlet.doGet(request, response);
    verify(response);
  }

  private HttpServletRequest createGetRequest(String reqParamValue, String callbackParamValue) {
    HttpServletRequest result = createMock(HttpServletRequest.class);
    expect(result.getMethod()).andReturn("GET").anyTimes();
    expect(result.getCharacterEncoding()).andReturn("UTF-8").anyTimes();
    expect(result.getParameter(RpcServlet.GET_REQUEST_REQ_PARAM))
        .andReturn(reqParamValue).anyTimes();
    expect(result.getParameter(RpcServlet.GET_REQUEST_CALLBACK_PARAM))
        .andReturn(callbackParamValue).anyTimes();
    replay(result);
    return result;
  }

  private HttpServletResponse createHttpResponse(String response, int httpStatusCode)
    throws IOException {
    return createHttpResponse(null, null, null, response, httpStatusCode);
  }

  private HttpServletResponse createHttpResponse(String header1, String header2,
      String contentType, String response, int httpStatusCode) throws IOException {
    HttpServletResponse result = createMock(HttpServletResponse.class);
    PrintWriter writer = createMock(PrintWriter.class);
    if (response != null) {
      expect(result.getWriter()).andReturn(writer);
      writer.write(response);
    }
    if (header1 != null && header2 != null) {
      result.setHeader(header1, header2);
    }
    if (contentType != null) {
      result.setContentType(contentType);
    }
    result.setStatus(httpStatusCode);
    replay(result, writer);
    return result;
  }
}
