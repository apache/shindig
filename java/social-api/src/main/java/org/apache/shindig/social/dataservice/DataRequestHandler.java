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
import org.apache.shindig.social.ResponseItem;
import org.apache.shindig.social.opensocial.util.BeanJsonConverter;

import com.google.inject.Inject;
import com.google.common.collect.Lists;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public abstract class DataRequestHandler {
  protected BeanJsonConverter converter;

  @Inject
  DataRequestHandler(BeanJsonConverter converter) {
    this.converter = converter;
  }

  public void handleMethod(String httpMethod, HttpServletRequest servletRequest,
      HttpServletResponse servletResponse, SecurityToken token)
      throws IOException {
    if (httpMethod == null || httpMethod.length() == 0) {
      throw new IllegalArgumentException("Unserviced Http method type");
    }
    ResponseItem responseItem;
    if (httpMethod.equals("GET")) {
      responseItem = handleGet(servletRequest, token);
    } else if (httpMethod.equals("POST")) {
      responseItem = handlePost(servletRequest, token);
    } else if (httpMethod.equals("PUT")) {
      responseItem = handlePut(servletRequest, token);
    } else if (httpMethod.equals("DELETE")) {
      responseItem = handleDelete(servletRequest, token);
    } else if (httpMethod.equals("HEAD")) {
      responseItem = handleHead(servletRequest);
    } else {
      throw new IllegalArgumentException("Unserviced Http method type");
    }
    if (responseItem.getError() == null) {
      PrintWriter writer = servletResponse.getWriter();
      writer.write(converter.convertToJson(
          responseItem.getResponse()).toString());
    } else {
      // throw an error
    }
  }

  ResponseItem handleHead(HttpServletRequest servletRequest) {
    throw new RuntimeException("Not Implemented");
  }

  abstract ResponseItem handleDelete(HttpServletRequest servletRequest,
      SecurityToken token);

  abstract ResponseItem handlePut(HttpServletRequest servletRequest,
      SecurityToken token);

  abstract ResponseItem handlePost(HttpServletRequest servletRequest,
      SecurityToken token);

  abstract ResponseItem handleGet(HttpServletRequest servletRequest,
      SecurityToken token);

  protected String[] getParamsFromRequest(HttpServletRequest servletRequest) {
    return getQueryPath(servletRequest).split("/");
  }

  private String getQueryPath(HttpServletRequest servletRequest) {
    String pathInfo = servletRequest.getPathInfo();
    int index = pathInfo.indexOf('/', 1);
    return pathInfo.substring(index + 1);
  }

  protected <T extends Enum<T>> T getEnumParam(
      HttpServletRequest servletRequest, String paramName, T defaultValue,
      Class<T> enumClass) {
    String paramValue = servletRequest.getParameter(paramName);
    if (paramValue != null) {
      return Enum.valueOf(enumClass, paramValue);
    }
    return defaultValue;
  }

  protected int getIntegerParam(HttpServletRequest servletRequest,
      String paramName, int defaultValue) {
    String paramValue = servletRequest.getParameter(paramName);
    if (paramValue != null) {
      return new Integer(paramValue);
    }
    return defaultValue;
  }

  protected List<String> getListParam(HttpServletRequest servletRequest,
      String paramName, List<String> defaultValue) {
    String paramValue = servletRequest.getParameter(paramName);
    if (paramValue != null) {
      return Lists.newArrayList(paramValue.split(","));
    }
    return defaultValue;
  }
}
