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
import org.apache.shindig.social.opensocial.spi.GroupId;
import org.apache.shindig.social.opensocial.spi.PersonService;
import org.apache.shindig.social.opensocial.spi.UserId;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

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

  private Map<String, List<String>> params;

  private String postData;

  private SecurityToken token;

  private BeanConverter converter;

  public RequestItem() {
    params = Maps.newHashMap();
  }

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
  public void parseUrlWithTemplate(String urlTemplate) {
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
                + " for singular field " + expectedPart + " in " + expectedUrl);
          }
          this.params.put(expectedPart.substring(1, expectedPart.length() - 1),
              Lists.newArrayList(actualPart));
        }
      }
    }
  }

  public String getAppId() {
    String appId = getParameter(APP_ID);

    if (appId != null && appId.equals(APP_SUBSTITUTION_TOKEN)) {
      return token.getAppId();
    } else {
      return appId;
    }
  }

  public Set<UserId> getUsers() {
    List<String> ids = getListParameter(USER_ID);
    Set<UserId> returnVal = Sets.newLinkedHashSet();
    for (String id : ids) {
      returnVal.add(UserId.fromJson(id));
    }
    return returnVal;
  }

  public GroupId getGroup() {
    return GroupId.fromJson(getParameter(GROUP_ID));
  }

  public int getStartIndex() {
    String startIndex = getParameter(START_INDEX);
    return startIndex == null ? DEFAULT_START_INDEX : Integer.valueOf(startIndex);
  }

  public int getCount() {
    String count = getParameter(COUNT);
    return count == null ? DEFAULT_COUNT : Integer.valueOf(count);
  }

  public PersonService.SortOrder getOrderBy() {
    String orderBy = getParameter(ORDER_BY);
    return orderBy == null
        ? PersonService.SortOrder.topFriends
        : PersonService.SortOrder.valueOf(orderBy);
  }

  public PersonService.FilterType getFilterBy() {
    String filterBy = getParameter(FILTER_BY);
    return filterBy == null
        ? PersonService.FilterType.all
        : PersonService.FilterType.valueOf(filterBy);
  }

  public Set<String> getFields() {
    return getFields(Collections.<String>emptySet());
  }

  public Set<String> getFields(Set<String> defaultValue) {
    List<String> paramValue = getListParameter(FIELDS);
    if (!paramValue.isEmpty()) {
      return Sets.newHashSet(paramValue);
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
