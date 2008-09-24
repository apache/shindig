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

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.social.opensocial.spi.DataCollection;
import org.apache.shindig.social.opensocial.spi.RestfulCollection;

import com.google.common.collect.Maps;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class DataServiceServlet extends ApiServlet {

  protected static final String FORMAT_PARAM = "format";
  protected static final String ATOM_FORMAT = "atom";
  protected static final String XML_FORMAT = "xml";

  public static final String PEOPLE_ROUTE = "people";
  public static final String ACTIVITY_ROUTE = "activities";
  public static final String APPDATA_ROUTE = "appdata";

  private static final Logger logger = Logger.getLogger(
      "org.apache.shindig.social.opensocial.spi");

  @Override
  protected void doGet(HttpServletRequest servletRequest,
      HttpServletResponse servletResponse)
      throws ServletException, IOException {
    doPost(servletRequest, servletResponse);
  }

  @Override
  protected void doPut(HttpServletRequest servletRequest,
      HttpServletResponse servletResponse)
      throws ServletException, IOException {
    doPost(servletRequest, servletResponse);
  }

  @Override
  protected void doDelete(HttpServletRequest servletRequest,
      HttpServletResponse servletResponse)
      throws ServletException, IOException {
    doPost(servletRequest, servletResponse);
  }

  @Override
  protected void doPost(HttpServletRequest servletRequest,
      HttpServletResponse servletResponse)
      throws ServletException, IOException {
    logger.finest("Handling restful request for " + servletRequest.getPathInfo());

    setCharacterEncodings(servletRequest, servletResponse);

    SecurityToken token = getSecurityToken(servletRequest);
    if (token == null) {
      sendSecurityError(servletResponse);
      return;
    }

    BeanConverter converter = getConverterForRequest(servletRequest);

    handleSingleRequest(servletRequest, servletResponse, token, converter);
  }

  @Override
  protected void sendError(HttpServletResponse servletResponse, ResponseItem responseItem)
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
    ResponseItem responseItem = getResponseItem(handleRequestItem(requestItem, servletRequest));

    if (responseItem.getError() == null) {
      PrintWriter writer = servletResponse.getWriter();
      Object response = responseItem.getResponse();
      // TODO: ugliness resulting from not using RestfulItem
      if (!(response instanceof DataCollection) && !(response instanceof RestfulCollection)) {
        response = Maps.immutableMap("entry", response);
      }

      writer.write(converter.convertToString(response));
    } else {
      sendError(servletResponse, responseItem);
    }
  }


  BeanConverter getConverterForRequest(HttpServletRequest servletRequest) {
    String formatString = servletRequest.getParameter(FORMAT_PARAM);
    if (ATOM_FORMAT.equals(formatString)) {
      return atomConverter;
    }

    else if (XML_FORMAT.equals(formatString)) {
      return xmlConverter;
    }

    return jsonConverter;
  }
}
