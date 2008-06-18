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

import org.apache.shindig.common.SecurityToken;

import com.google.common.collect.Maps;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Enumeration;

/**
 * Represents the request items that come from the restful request.
 */
public class RequestItem {
  private String url;
  private String method;
  private Map<String, String> parameters;
  private SecurityToken token;

  public RequestItem() { }

  public RequestItem(String url, Map<String, String> parameters, SecurityToken token,
      String method) {
    this.url = url;
    this.parameters = parameters;
    this.token = token;
    this.method = method;
  }

  public RequestItem(HttpServletRequest servletRequest, SecurityToken token, String method) {
    this(servletRequest.getPathInfo(), createParameterMap(servletRequest), token, method);
  }

  private static Map<String, String> createParameterMap(HttpServletRequest servletRequest) {
    Map<String, String> parameters = Maps.newHashMap();

    Enumeration names = servletRequest.getParameterNames();
    while (names.hasMoreElements()) {
      String name = (String) names.nextElement();
      parameters.put(name, servletRequest.getParameter(name));
    }

    return parameters;
  }

  /*
   * Takes any url params out of the url and puts them into the param map.
   * Usually the servlet request code does this for us but the batch request calls have to do it
   * by hand.
   */
  public void parseUrlParamsIntoParameters() {
    if (this.parameters == null) {
      this.parameters = Maps.newHashMap();
    }

    String fullUrl = this.url;
    int queryParamIndex = fullUrl.indexOf("?");

    if (queryParamIndex != -1) {
      this.url = fullUrl.substring(0, queryParamIndex);

      String queryParams = fullUrl.substring(queryParamIndex + 1);
      for (String param : queryParams.split("&")) {
        String[] paramPieces = param.split("=", 2);
        this.parameters.put(paramPieces[0], paramPieces.length == 2 ? paramPieces[1] : "");
      }
    }
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getMethod() {
    return method;
  }

  public void setMethod(String method) {
    this.method = method;
  }

  public Map<String, String> getParameters() {
    return parameters;
  }

  public void setParameters(Map<String, String> parameters) {
    this.parameters = parameters;
  }

  public SecurityToken getToken() {
    return token;
  }

  public void setToken(SecurityToken token) {
    this.token = token;
  }
}