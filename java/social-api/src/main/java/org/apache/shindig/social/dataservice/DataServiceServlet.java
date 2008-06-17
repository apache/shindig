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
package org.apache.shindig.social.dataservice;

import com.google.inject.Inject;
import com.google.inject.Injector;

import org.apache.shindig.common.SecurityTokenDecoder;
import org.apache.shindig.common.SecurityToken;
import org.apache.shindig.common.SecurityTokenException;
import org.apache.shindig.common.servlet.InjectedServlet;
import org.apache.shindig.social.opensocial.util.BeanConverter;
import org.apache.shindig.social.opensocial.util.BeanJsonConverter;
import org.apache.shindig.social.opensocial.util.BeanXmlConverter;
import org.apache.shindig.social.ResponseItem;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;

public class DataServiceServlet extends InjectedServlet {

  protected static final String X_HTTP_METHOD_OVERRIDE = "X-HTTP-Method-Override";
  protected static final String SECURITY_TOKEN_PARAM = "st";
  protected static final String REQUEST_PARAMETER = "request";
  protected static final String FORMAT_PARAM = "format";
  protected static final String ATOM_FORMAT = "atom";

  public static final String PEOPLE_ROUTE = "people";
  public static final String ACTIVITY_ROUTE = "activities";
  public static final String APPDATA_ROUTE = "appdata";

  private static final Logger logger = Logger.getLogger(
      "org.apache.shindig.social.dataservice");

  private SecurityTokenDecoder securityTokenDecoder;
  private Map<String, Class<? extends DataRequestHandler>> handlers;
  private BeanJsonConverter jsonConverter;
  private BeanXmlConverter xmlConverter;

  @Inject
  public void setHandlers(HandlerProvider handlers) {
    this.handlers = handlers.get();
  }

  @Inject
  public void setSecurityTokenDecoder(SecurityTokenDecoder
      securityTokenDecoder) {
    this.securityTokenDecoder = securityTokenDecoder;
  }

  @Inject
  public void setBeanConverters(BeanJsonConverter jsonConverter, BeanXmlConverter xmlConverter) {
    this.jsonConverter = jsonConverter;
    this.xmlConverter = xmlConverter;
  }

  // Only for testing use. Do not override the injector.
  public void setInjector(Injector injector) {
    this.injector = injector;
  }

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
    String path = servletRequest.getPathInfo();
    logger.finest("Handling restful request for " + path);

    servletRequest.setCharacterEncoding("UTF-8");

    String route = getRouteFromParameter(path);
    Class<? extends DataRequestHandler> handlerClass = handlers.get(route);

    if (handlerClass == null) {
      throw new RuntimeException("No handler for route: " + route);
    }

    DataRequestHandler handler = injector.getInstance(handlerClass);
    BeanConverter converter = getConverterForRequest(servletRequest);
    // TODO: Move all conversions out of the handler up into the servlet layer
    handler.setConverter(converter);

    String method = getHttpMethodFromParameter(servletRequest.getMethod(),
        servletRequest.getParameter(X_HTTP_METHOD_OVERRIDE));

    SecurityToken token;
    try {
      token = securityTokenDecoder.createToken(servletRequest.getParameter(SECURITY_TOKEN_PARAM));
    } catch (SecurityTokenException e) {
      throw new RuntimeException(
          "Implement error return for bad security token.");
    }

    ResponseItem responseItem = handler.handleMethod(
        new RequestItem(servletRequest, token, method));

    if (responseItem.getError() == null) {
      PrintWriter writer = servletResponse.getWriter();
      writer.write(converter.convertToString(responseItem.getResponse()));
    } else {
      servletResponse.sendError(responseItem.getError().getHttpErrorCode(),
          responseItem.getErrorMessage());
    }
  }

  /*package-protected*/ BeanConverter getConverterForRequest(HttpServletRequest servletRequest) {
    String formatString = servletRequest.getParameter(FORMAT_PARAM);
    if (!StringUtils.isBlank(formatString) && formatString.equals(ATOM_FORMAT)) {
      return xmlConverter;
    }
    return jsonConverter;
  }

  /*package-protected*/ String getHttpMethodFromParameter(String method,
      String overrideParameter) {
    if (!StringUtils.isBlank(overrideParameter)) {
      return overrideParameter;
    } else {
      return method;
    }
  }

  /*package-protected*/ String getRouteFromParameter(String pathInfo) {
    pathInfo = pathInfo.substring(1);
    int indexOfNextPathSeparator = pathInfo.indexOf("/");
    return indexOfNextPathSeparator != -1 ?
        pathInfo.substring(0, indexOfNextPathSeparator) :
        pathInfo;
  }
}
