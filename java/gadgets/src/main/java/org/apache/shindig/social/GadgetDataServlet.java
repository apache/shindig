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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.apache.shindig.social.samplecontainer.StateFileDataHandler;
import org.apache.shindig.social.opensocial.OpenSocialDataHandler;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.GadgetToken;
import org.apache.shindig.gadgets.http.CrossServletState;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;
import java.util.List;
import java.util.ArrayList;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
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
public class GadgetDataServlet extends HttpServlet {
  private static final Logger logger
      = Logger.getLogger("org.apache.shindig.social");

  // TODO: get through injection
  private static List<GadgetDataHandler> handlers
      = new ArrayList<GadgetDataHandler>();

  static {
    // TODO: Should we just use a map from String type to Handler?
    handlers.add(new OpenSocialDataHandler());
    handlers.add(new StateFileDataHandler());
  }

  private CrossServletState servletState;

  @Override
  public void init(ServletConfig config) throws ServletException {
    servletState = CrossServletState.get(config);
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws IOException {

    String requestParam = req.getParameter("request");
    String token = req.getParameter("st");

    DataResponse response;
    try {
      response = new DataResponse(createResponse(requestParam, token));
    } catch (JSONException e) {
      response = new DataResponse(ResponseError.BAD_REQUEST);
    } catch (GadgetException e) {
      logger.info("Request was made with invalid security token: " + token);
      response = new DataResponse(ResponseError.BAD_REQUEST);
    }

    PrintWriter writer = resp.getWriter();
    writer.write(response.toJson().toString());
  }

  private List<ResponseItem> createResponse(String requestParam, String token)
      throws JSONException, GadgetException {
    if (token == null || token.trim().length() == 0) {
      throw new GadgetException(GadgetException.Code.INVALID_GADGET_TOKEN);
    }
    GadgetToken securityToken = servletState.getGadgetSigner().createToken(token);

    // TODO: Improve json input handling. The json request should get auto
    // translated into objects
    List<ResponseItem> responseItems = new ArrayList<ResponseItem>();

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
