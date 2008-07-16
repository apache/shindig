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

import org.apache.shindig.common.SecurityToken;
import org.apache.shindig.common.SecurityTokenDecoder;
import org.apache.shindig.common.SecurityTokenException;
import org.apache.shindig.common.servlet.InjectedServlet;
import org.apache.shindig.common.servlet.ParameterFetcher;
import org.apache.shindig.social.opensocial.util.BeanJsonConverter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet for handling gadget requests for data. The request accepts one json
 * parameter of the format:
 *
 * request = [{type: <string>, other parameters}, ...]
 *
 * This gets mapped to a list of RequestItems. These items will be passed to one
 * of the registered handlers, which in turn produce ResponseItems. If no
 * handler is found a NOT_IMPLEMENTED ResponseItem will be created.
 *
 * This list of ResponseItems will get passed back to the gadget with this form:
 *
 * responses = [{response: <any json string>, error: <ResponseError>}]
 *
 * This class is meant to work with the logic in jsoncontainer.js.
 */
public class GadgetDataServlet extends InjectedServlet {
  private static final Logger logger
      = Logger.getLogger("org.apache.shindig.social");

  public static final String REQUEST_PARAMETER_NAME = "request";

  private List<GadgetDataHandler> handlers;
  private SecurityTokenDecoder securityTokenDecoder;
  private BeanJsonConverter beanJsonConverter;
  private ParameterFetcher parameterFetcher;

  @Inject
  public void setGadgetDataHandlers(List<GadgetDataHandler> handlers) {
    this.handlers = handlers;
  }

  @Inject
  public void setSecurityTokenDecoder(SecurityTokenDecoder securityTokenDecoder) {
    this.securityTokenDecoder = securityTokenDecoder;
  }

  @Inject
  public void setBeanJsonConverter(BeanJsonConverter beanJsonConverter) {
    this.beanJsonConverter = beanJsonConverter;
  }

  @Inject
  public void setParameterFetcher(@Named("GadgetDataServlet") ParameterFetcher parameterFetcher) {
    this.parameterFetcher = parameterFetcher;
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws IOException {

    req.setCharacterEncoding("UTF-8");

    DataResponse response;
    try {
      response = new DataResponse(createResponse(parameterFetcher.fetch(req)));
    } catch (JSONException e) {
      response = new DataResponse(ResponseError.BAD_REQUEST);
    } catch (SecurityTokenException e) {
      logger.info("Request was made with invalid security token: " + e.getMessage());
      response = new DataResponse(ResponseError.BAD_REQUEST);
    }
    resp.setContentType("application/json; charset=utf-8");
    PrintWriter writer = resp.getWriter();
    Object json = beanJsonConverter.convertToJson(response);
    writer.write(json.toString());
  }

  private List<ResponseItem> createResponse(Map<String, String> parameters)
      throws JSONException, SecurityTokenException {

    final SecurityToken securityToken = securityTokenDecoder.createToken(parameters);

    final String requestParam = parameters.get(REQUEST_PARAMETER_NAME);

    // TODO: Improve json input handling. The json request should get auto
    // translated into objects
    List<ResponseItem> responseItems = Lists.newArrayList();

    JSONArray requestItems = new JSONArray(requestParam);
    int length = requestItems.length();

    for (int i = 0; i < length; i++) {
      JSONObject jsonRequest = requestItems.getJSONObject(i);
      RequestItem requestItem = new RequestItem(jsonRequest.getString("type"),
          jsonRequest, securityToken);

      ResponseItem response = new ResponseItem<Object>(
          ResponseError.NOT_IMPLEMENTED,
          requestItem.getType() + " has not been implemented yet.",
          new JSONObject());

      for (GadgetDataHandler handler : handlers) {
        if (handler.shouldHandle(requestItem.getType())) {
          response = handler.handleRequest(requestItem);
        }
      }

      responseItems.add(response);
    }

    return responseItems;
  }

}
