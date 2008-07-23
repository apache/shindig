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
import org.apache.shindig.social.core.util.BeanConverter;
import org.apache.shindig.social.opensocial.spi.GroupId;
import org.apache.shindig.social.opensocial.spi.PersonService;
import org.apache.shindig.social.opensocial.spi.UserId;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.io.IOUtils;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;

/**
 * Represents the request items that come from the restful request.
 */
public class RequestItem {
  // Common OpenSocial RESTful fields
  public static final String APP_ID = "appId";
  public static final String USER_ID = "userId";
  public static final String GROUP_ID = "groupId";
  public static final String START_INDEX = "startIndex";
  public static final String COUNT = "count";
  public static final String ORDER_BY = "orderBy";
  public static final String FILTER_BY = "filterBy";
  public static final String FIELDS = "fields";

  // OpenSocial defaults
  public static final int DEFAULT_START_INDEX = 0;
  public static final int DEFAULT_COUNT = 20;

  public static final String APP_SUBSTITUTION_TOKEN = "@app";

  private String url;
  private String method;
  private Map<String, String> params;
  private String postData;

  private SecurityToken token;
  private BeanConverter converter;

  public RequestItem() { }

  public RequestItem(HttpServletRequest servletRequest, SecurityToken token, String method,
      BeanConverter converter) {
    this.url = servletRequest.getPathInfo();
    this.params = createParameterMap(servletRequest);
    this.token = token;

    this.method = method;
    this.converter = converter;

    try {
      ServletInputStream is = servletRequest.getInputStream();
      postData = new String(IOUtils.toByteArray(is));
    } catch (IOException e) {
      throw new RuntimeException("Could not get the post data from the request", e);
    }
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
        if (paramPieces.length == 2) {
          this.params.put(paramPieces[0], paramPieces[1]);
        } else {
          this.params.put(paramPieces[0], "");
        }
      }
    }
  }

  /**
   * This could definitely be cleaner..
   * TODO: Come up with a cleaner way to handle all of this code.
   *
   * @param urlTemplate The template the url follows
   */
  public void parseUrlWithTemplate(String urlTemplate) {
    this.putUrlParamsIntoParameters();

    String[] actualUrl = this.url.split("/");
    String[] expectedUrl = urlTemplate.split("/");

    for (int i = 0; i < actualUrl.length; i++) {
      String actualPart = actualUrl[i];
      String expectedPart = expectedUrl[i];

      if (expectedPart.startsWith("{")) {
        this.params.put(expectedPart.substring(1, expectedPart.length() - 1), actualPart);
      }
    }
  }

  public String getAppId() {
    String appId = params.get(APP_ID);

    if (appId != null && appId.equals(APP_SUBSTITUTION_TOKEN)) {
      return token.getAppId();
    } else {
      return appId;
    }
  }

  public UserId getUser() {
    return UserId.fromJson(params.get(USER_ID));
  }

  public GroupId getGroup() {
    return GroupId.fromJson(params.get(GROUP_ID));
  }

  public int getStartIndex() {
    String startIndex = params.get(START_INDEX);
    return startIndex == null ? DEFAULT_START_INDEX : Integer.valueOf(startIndex);
  }

  public int getCount() {
    String count = params.get(COUNT);
    return count == null ? DEFAULT_COUNT : Integer.valueOf(count);
  }

  public PersonService.SortOrder getOrderBy() {
    String orderBy = params.get(ORDER_BY);
    return orderBy == null
        ? PersonService.SortOrder.topFriends
        : PersonService.SortOrder.valueOf(orderBy);
  }

  public PersonService.FilterType getFilterBy() {
    String filterBy = params.get(FILTER_BY);
    return filterBy == null
        ? PersonService.FilterType.all
        : PersonService.FilterType.valueOf(filterBy);
  }

  public Set<String> getFields() {
    return getFields(Sets.<String>newHashSet());
  }

  public Set<String> getFields(Set<String> defaultValue) {
    String paramValue = params.get(FIELDS);
    if (paramValue != null) {
      return Sets.newHashSet(paramValue.split(","));
    }
    return defaultValue;
  }

  public <T> T getPostData(Class<T> postDataClass) {
    return converter.convertToObject(postData, postDataClass);
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
    return params;
  }

  public void setParameters(Map<String, String> parameters) {
    this.params = parameters;
  }

  public String getPostData() {
    return postData;
  }

  public void setPostData(String postData) {
    this.postData = postData;
  }

  public SecurityToken getToken() {
    return token;
  }

  public void setToken(SecurityToken token) {
    this.token = token;
  }

  public BeanConverter getConverter() {
    return converter;
  }

  public void setConverter(BeanConverter converter) {
    this.converter = converter;
  }
}
