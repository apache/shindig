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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

/**
 * Represents the request items that come from the restful request.
 */
public class RestfulRequestItem extends RequestItem {

  protected static final String X_HTTP_METHOD_OVERRIDE = "X-HTTP-Method-Override";

  private String url;

  private Map<String, List<String>> params;

  private String postData;

  public RestfulRequestItem(String service, String method,
      SecurityToken token, BeanConverter converter) {
    super(service, method, token, converter);
  }

  public RestfulRequestItem(String path, String method, String postData, SecurityToken token,
      BeanConverter converter) {
    super(getServiceFromPath(path), method, token, converter);
    this.postData = postData;
    this.url = path;
    putUrlParamsIntoParameters();
  }

  public RestfulRequestItem(HttpServletRequest servletRequest, SecurityToken token,
      BeanConverter converter) {
    super(getServiceFromPath(servletRequest.getPathInfo()),
        getMethod(servletRequest),
        token, converter);
    this.url = servletRequest.getPathInfo();
    this.params = createParameterMap(servletRequest);

    try {
      ServletInputStream is = servletRequest.getInputStream();
      postData = new String(IOUtils.toByteArray(is));
    } catch (IOException e) {
      throw new RuntimeException("Could not get the post data from the request", e);
    }
  }

  static String getServiceFromPath(String pathInfo) {
    pathInfo = pathInfo.substring(1);
    int indexOfNextPathSeparator = pathInfo.indexOf('/');
    if (indexOfNextPathSeparator != -1) {
      return pathInfo.substring(0, indexOfNextPathSeparator);
    }
    return pathInfo;
  }

  static String getMethod(HttpServletRequest request) {
    String override = request.getParameter(X_HTTP_METHOD_OVERRIDE);
    if (!StringUtils.isBlank(override)) {
      return override;
    } else {
      return request.getMethod();
    }
  }

  private static Map<String, List<String>> createParameterMap(HttpServletRequest servletRequest) {
    Map<String, List<String>> parameters = Maps.newHashMap();

    Enumeration names = servletRequest.getParameterNames();
    while (names.hasMoreElements()) {
      String name = (String) names.nextElement();
      String[] paramValues = servletRequest.getParameterValues(name);
      parameters.put(name, Lists.newArrayList(paramValues));
    }
    return parameters;
  }

  /*
   * Takes any url params out of the url and puts them into the param map.
   * Usually the servlet request code does this for us but the batch request calls have to do it
   * by hand.
   */
  void putUrlParamsIntoParameters() {
    if (this.params == null) {
      this.params = Maps.newHashMap();
    }

    String fullUrl = this.url;
    int queryParamIndex = fullUrl.indexOf('?');

    if (queryParamIndex != -1) {
      this.url = fullUrl.substring(0, queryParamIndex);

      String queryParams = fullUrl.substring(queryParamIndex + 1);
      for (String param : queryParams.split("&")) {
        String[] paramPieces = param.split("=", 2);
        List<String> paramList = this.params.get(paramPieces[0]);
        if (paramList == null) {
          paramList = Lists.newArrayListWithCapacity(1);
          this.params.put(paramPieces[0], paramList);
        }
        if (paramPieces.length == 2) {
          paramList.add(paramPieces[1]);
        } else {
          paramList.add("");
        }
      }
    }
  }

  /**
   * This could definitely be cleaner.. TODO: Come up with a cleaner way to handle all of this
   * code.
   *
   * @param urlTemplate The template the url follows
   */
  public void applyUrlTemplate(String urlTemplate) {
    this.putUrlParamsIntoParameters();

    String[] actualUrl = this.url.split("/");
    String[] expectedUrl = urlTemplate.split("/");

    for (int i = 0; i < actualUrl.length; i++) {
      String actualPart = actualUrl[i];
      String expectedPart = expectedUrl[i];

      if (expectedPart.startsWith("{")) {
        if (expectedPart.endsWith("}+")) {
          // The param can be a repeated field. Use ',' as default separator
          this.params
              .put(expectedPart.substring(1, expectedPart.length() - 2),
                  Lists.newArrayList(actualPart.split(",")));
        } else {
          if (actualPart.indexOf(',') != -1) {
            throw new IllegalArgumentException("Cannot expect plural value " + actualPart
                + " for singular field " + expectedPart + " in " + this.url);
          }
          this.params.put(expectedPart.substring(1, expectedPart.length() - 1),
              Lists.newArrayList(actualPart));
        }
      }
    }
  }


  public <T> T getTypedParameter(String parameterName, Class<T> postDataClass) {
    // We assume the the only typed parameter in a restful request is the post-content
    // and so we simply ignore the parameter name
    return converter.convertToObject(postData, postDataClass);
  }


  Map<String, List<String>> getParameters() {
    return params;
  }

  void setParameter(String paramName, String paramValue) {
    // Ignore nulls
    if (paramValue == null) {
      return;
    }
    this.params.put(paramName, Lists.newArrayList(paramValue));
  }

  void setListParameter(String paramName, List<String> paramValue) {
    this.params.put(paramName, paramValue);
  }

  /**
   * Return a single param value
   */
  public String getParameter(String paramName) {
    List<String> paramValue = this.params.get(paramName);
    if (paramValue != null && !paramValue.isEmpty()) {
      return paramValue.get(0);
    }
    return null;
  }

  public String getParameter(String paramName, String defaultValue) {
    String result = getParameter(paramName);
    if (result == null) {
      return defaultValue;
    }
    return result;
  }

  /**
   * Return a list param value
   */
  public List<String> getListParameter(String paramName) {
    List<String> stringList = this.params.get(paramName);
    if (stringList == null) {
      return Collections.emptyList();
    }
    if (stringList.size() == 1 && stringList.get(0).indexOf(',') != -1) {
      stringList = Arrays.asList(stringList.get(0).split(","));
      this.params.put(paramName, stringList);
    }
    return stringList;
  }
}
