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
import org.apache.shindig.social.ResponseError;
import org.apache.shindig.social.ResponseItem;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class DataServiceServlet extends ApiServlet {

  protected static final String FORMAT_PARAM = "format";
  protected static final String ATOM_FORMAT = "atom";

  public static final String PEOPLE_ROUTE = "people";
  public static final String ACTIVITY_ROUTE = "activities";
  public static final String APPDATA_ROUTE = "appdata";

  private static final Logger logger = Logger.getLogger(
      "org.apache.shindig.social.opensocial.spi");

  protected void doGet(HttpServletRequest servletRequest,
      HttpServletResponse servletResponse)
      throws ServletException, IOException {
    doPost(servletRequest, servletResponse);
  }

  protected void doPut(HttpServletRequest servletRequest,
      HttpServletResponse servletResponse)
      throws ServletException, IOException {
    doPost(servletRequest, servletResponse);
  }

  protected void doDelete(HttpServletRequest servletRequest,
      HttpServletResponse servletResponse)
      throws ServletException, IOException {
    doPost(servletRequest, servletResponse);
  }

  protected void doPost(HttpServletRequest servletRequest,
      HttpServletResponse servletResponse)
      throws ServletException, IOException {
    logger.finest("Handling restful request for " + servletRequest.getPathInfo());

    servletRequest.setCharacterEncoding("UTF-8");

    SecurityToken token = getSecurityToken(servletRequest);
    if (token == null) {
      sendError(servletResponse, new ResponseItem(ResponseError.UNAUTHORIZED,
          "The request did not have a proper security token nor oauth message and unauthenticated "
              + "requests are not allowed"));
      return;
    }

    BeanConverter converter = getConverterForRequest(servletRequest);

    handleSingleRequest(servletRequest, servletResponse, token, converter);
  }

  private void sendError(HttpServletResponse servletResponse, ResponseItem responseItem)
      throws IOException {
    servletResponse.sendError(responseItem.getError().getHttpErrorCode(),
        responseItem.getErrorMessage());
  }

  /**
   * Handler for non-batch requests
   */
  private void handleSingleRequest(HttpServletRequest servletRequest,
      HttpServletResponse servletResponse, SecurityToken token,
      BeanConverter converter) throws IOException {
    RestfulRequestItem requestItem = new RestfulRequestItem(servletRequest, token, converter);
    ResponseItem responseItem = getResponseItem(handleRequestItem(requestItem));

    if (responseItem.getError() == null) {
      PrintWriter writer = servletResponse.getWriter();
      writer.write(converter.convertToString(responseItem));
    } else {
      sendError(servletResponse, responseItem);
    }
  }


  BeanConverter getConverterForRequest(HttpServletRequest servletRequest) {
    String formatString = servletRequest.getParameter(FORMAT_PARAM);
    if (ATOM_FORMAT.equals(formatString)) {
      return xmlConverter;
    }
    return jsonConverter;
  }
}
