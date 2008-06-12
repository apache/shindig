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
import org.apache.shindig.common.SecurityTokenDecoder;
import org.apache.shindig.common.SecurityToken;
import org.apache.shindig.common.SecurityTokenException;
import org.apache.shindig.common.servlet.InjectedServlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;

public class DataServiceServlet extends InjectedServlet {

  public static enum GroupId {
    ALL("@all"),
    FRIENDS("@friends"),
    SELF("@self"),
    GROUP("not supported yet");

    private final String jsonString;

    GroupId(String jsonString) {
      this.jsonString = jsonString;
    }

    public String getJsonString() {
      return jsonString;
    }

    public static GroupId fromJson(String s) {
      return valueOf(s.substring(1).toUpperCase());
    }
  }

  protected static final String X_HTTP_METHOD_OVERRIDE
      = "X-HTTP-Method-Override";
  protected static final String SECURITY_TOKEN_PARAM = "st";
  protected static final String REQUEST_PARAMETER = "request";

  public static final String PEOPLE_ROUTE = "people";
  public static final String ACTIVITY_ROUTE = "activities";
  public static final String APPDATA_ROUTE = "appdata";

  private static final Logger logger = Logger.getLogger(
      "org.apache.shindig.social.dataservice");

  private SecurityTokenDecoder securityTokenDecoder;
  private Map<String, DataRequestHandler> handlers;

  @Inject
  public void setHandlers(Map<String, DataRequestHandler> handlers) {
    this.handlers = handlers;
  }

  @Inject
  public void setSecurityTokenDecoder(SecurityTokenDecoder
      securityTokenDecoder) {
    this.securityTokenDecoder = securityTokenDecoder;
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
    servletRequest.setCharacterEncoding("UTF-8");

    String route = getRouteFromParameter(servletRequest.getPathInfo());
    DataRequestHandler handler = handlers.get(route);
    if (handler == null) {
      throw new RuntimeException("No handler for route: " + route);
    }

    String method = getHttpMethodFromParameter(servletRequest.getMethod(),
        servletRequest.getParameter(X_HTTP_METHOD_OVERRIDE));
    try {
      SecurityToken token = securityTokenDecoder.createToken(
          servletRequest.getParameter(SECURITY_TOKEN_PARAM));
      handler.handleMethod(method, servletRequest, servletResponse, token);
    } catch (SecurityTokenException e) {
      throw new RuntimeException(
          "Implement error return for bad security token.");
    }

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
