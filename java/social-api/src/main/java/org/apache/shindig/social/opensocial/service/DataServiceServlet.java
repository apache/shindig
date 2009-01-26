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
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.social.ResponseError;
import org.apache.shindig.social.opensocial.spi.DataCollection;
import org.apache.shindig.social.opensocial.spi.RestfulCollection;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
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

  public static final String CONTENT_TYPE = "CONTENT_TYPE";

  private static final Logger logger = Logger.getLogger("org.apache.shindig.social.opensocial.spi");

  /** Map from service name to the property name in the container config */
  private static final Map<String, String> SERVICE_TO_SUPPORTED_FIELD_MAP =
    ImmutableMap.of("people", "person", "activities", "activity");


  private ContainerConfig config;

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
    if (logger.isLoggable(Level.FINEST)) logger.finest("Handling restful request for " + servletRequest.getPathInfo());

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

  @Inject
  public void setContainerConfig(ContainerConfig config) {
    this.config = config;
  }

  /**
   * Handler for non-batch requests.
   */
  private void handleSingleRequest(HttpServletRequest servletRequest,
      HttpServletResponse servletResponse, SecurityToken token,
      BeanConverter converter) throws IOException {
    RestfulRequestItem requestItem = new RestfulRequestItem(servletRequest, token, converter);
    ResponseItem responseItem;

    if (requestItem.getUrl().endsWith("/@supportedFields")) {
      responseItem = getSupportedFields(requestItem);
    } else {
      responseItem = getResponseItem(handleRequestItem(requestItem, servletRequest));
    }

    servletResponse.setContentType(converter.getContentType());
    if (responseItem.getError() == null) {
      PrintWriter writer = servletResponse.getWriter();
      Object response = responseItem.getResponse();
      // TODO: ugliness resulting from not using RestfulItem
      if (!(response instanceof DataCollection) && !(response instanceof RestfulCollection)) {
        response = ImmutableMap.of("entry", response);
      }

      writer.write(converter.convertToString(response));
    } else {
      sendError(servletResponse, responseItem);
    }
  }

  private ResponseItem getSupportedFields(RequestItem requestItem) {
    String service = requestItem.getService();
    String configProperty = SERVICE_TO_SUPPORTED_FIELD_MAP.get(service);
    if (configProperty == null) {
      configProperty = service;
    }

    String container = Objects.firstNonNull(requestItem.getToken().getContainer(), "default");
    // TODO: hardcoding opensocial-0.8 is brittle
    List<Object> fields = config.getList(container,
        "${gadgets\\.features.opensocial-0\\.8.supportedFields." + configProperty + '}');

    if (fields.size() == 0) {
      return new ResponseItem(ResponseError.NOT_IMPLEMENTED,"Supported fields not available for" +
      		" service \"" + service + '\"');
    }

    return new ResponseItem(fields);
  }

  BeanConverter getConverterForRequest(HttpServletRequest servletRequest) {
    String formatString = null;
    BeanConverter converter = null;
    String contentType = null;

    try {
      formatString = servletRequest.getParameter(FORMAT_PARAM);
    } catch (Throwable t) {
      // this happens while testing
      if (logger.isLoggable(Level.FINE)) logger.fine("Unexpected error : format param is null " + t.toString());
    }
    try {
      contentType = servletRequest.getHeader(CONTENT_TYPE);
    } catch (Throwable t) {
      //this happens while testing
      if (logger.isLoggable(Level.FINE)) logger.fine("Unexpected error : content type is null " + t.toString());
    }

    if (contentType != null) {
      if (contentType.equals("application/json")) {
        converter = jsonConverter;
      } else if (contentType.equals("application/atom+xml")) {
        converter = atomConverter;
      } else if (contentType.equals("application/xml")) {
        converter = xmlConverter;
      } else if (formatString == null) {
        // takes care of cases where content!= null but is ""
        converter = jsonConverter;
      }
    } else if (formatString != null) {
      if (formatString.equals(ATOM_FORMAT)) {
        converter = atomConverter;
      } else if (formatString.equals(XML_FORMAT)) {
        converter = xmlConverter;
      } else {
        converter = jsonConverter;
      }
    } else {
      converter = jsonConverter;
    }
    return converter;
  }
}
