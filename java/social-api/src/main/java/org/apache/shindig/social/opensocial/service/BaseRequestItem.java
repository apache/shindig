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
import org.apache.shindig.social.ResponseError;
import org.apache.shindig.social.opensocial.spi.GroupId;
import org.apache.shindig.social.opensocial.spi.PersonService;
import org.apache.shindig.social.opensocial.spi.SocialSpiException;
import org.apache.shindig.social.opensocial.spi.UserId;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Default implementation of RequestItem
 */
public class BaseRequestItem implements RequestItem {

  final SecurityToken token;
  final BeanConverter converter;
  final Map<String,Object> parameters;

  public BaseRequestItem(Map<String, String[]> parameters,
      SecurityToken token,
      BeanConverter converter) {
    this.token = token;
    this.converter = converter;
    this.parameters = Maps.newHashMap();
    for (Map.Entry<String, String[]> entry : parameters.entrySet()) {
      if  (entry.getValue() == null) {
        setParameter(entry.getKey(), null);
      } else if (entry.getValue().length == 1) {
        setParameter(entry.getKey(), entry.getValue()[0]);
      } else {
        setParameter(entry.getKey(), Lists.newArrayList(entry.getValue()));
      }
    }
  }

  public BaseRequestItem(JSONObject parameters,
      SecurityToken token,
      BeanConverter converter) {
    try {
      this.parameters = Maps.newHashMap();
      Iterator keys = parameters.keys();
      while (keys.hasNext()) {
        String key = (String)keys.next();
        this.parameters.put(key, parameters.get(key));
      }
      this.token = token;
      this.converter = converter;
    } catch (JSONException je) {
      throw new SocialSpiException(ResponseError.INTERNAL_ERROR, je.getMessage(), je);
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

  public Date getUpdatedSince() {
    String updatedSince = getParameter("updatedSince");
    if (updatedSince == null)
      return null;

    DateTime date = new DateTime(updatedSince);

    return date.toDate();
  }

  public Set<UserId> getUsers() {
    List<String> ids = getListParameter(USER_ID);
    if (ids.isEmpty()) {
      if (token.getViewerId() != null) {
        // Assume @me
        ids = Lists.newArrayList("@me");
      } else {
        throw new IllegalArgumentException("No userId provided and viewer not available");
      }
    }
    Set<UserId> userIds = Sets.newLinkedHashSet();
    for (String id : ids) {
      userIds.add(UserId.fromJson(id));
    }
    return userIds;
  }


  public GroupId getGroup() {
    return GroupId.fromJson(getParameter(GROUP_ID, "@self"));
  }

  public int getStartIndex() {
    String startIndex = getParameter(START_INDEX);
    try {
      return startIndex == null ? DEFAULT_START_INDEX
          : Integer.valueOf(startIndex);
    } catch (NumberFormatException nfe) {
      throw new SocialSpiException(ResponseError.BAD_REQUEST,
          "Parameter " + START_INDEX + " (" + startIndex + ") is not a number.");
    }
  }

  public int getCount() {
    String count = getParameter(COUNT);
    try {
      return count == null ? DEFAULT_COUNT : Integer.valueOf(count);
    } catch (NumberFormatException nfe) {
      throw new SocialSpiException(ResponseError.BAD_REQUEST,
           "Parameter " + COUNT + " (" + count + ") is not a number.");
    }
  }

  public String getSortBy() {
    String sortBy = getParameter(SORT_BY);
    return sortBy == null ? PersonService.TOP_FRIENDS_SORT : sortBy;
  }

  public PersonService.SortOrder getSortOrder() {
    String sortOrder = getParameter(SORT_ORDER);
    try {
      return sortOrder == null
            ? PersonService.SortOrder.ascending
            : PersonService.SortOrder.valueOf(sortOrder);
    } catch (IllegalArgumentException iae) {
      throw new SocialSpiException(ResponseError.BAD_REQUEST,
           "Parameter " + SORT_ORDER + " (" + sortOrder + ") is not valid.");
    }
  }

  public String getFilterBy() {
    return getParameter(FILTER_BY);
  }

  public PersonService.FilterOperation getFilterOperation() {
    String filterOp = getParameter(FILTER_OPERATION);
    try {
      return filterOp == null
          ? PersonService.FilterOperation.contains
          : PersonService.FilterOperation.valueOf(filterOp);
    } catch (IllegalArgumentException iae) {
      throw new SocialSpiException(ResponseError.BAD_REQUEST,
           "Parameter " + FILTER_OPERATION + " (" + filterOp + ") is not valid.");
    }
  }

  public String getFilterValue() {
    String filterValue = getParameter(FILTER_VALUE);
    return filterValue == null ? "" : filterValue;
  }

  public Set<String> getFields() {
    return getFields(Collections.<String>emptySet());
  }

  public Set<String> getFields(Set<String> defaultValue) {
    Set<String> result = ImmutableSet.copyOf(getListParameter(FIELDS));
    if (result.isEmpty()) {
      return defaultValue;
    }
    return result;
  }


  public SecurityToken getToken() {
    return token;
  }

  public <T> T getTypedParameter(String parameterName, Class<T> dataTypeClass) {
    return converter.convertToObject(getParameter(parameterName), dataTypeClass);
  }

  public String getParameter(String paramName) {
    Object param = this.parameters.get(paramName);
    if (param instanceof List) {
      if (((List)param).isEmpty()) {
        return null;
      } else {
        param = ((List)param).get(0);
      }
    }
    if (param == null) {
      return null;
    }
    return param.toString();
  }

  public String getParameter(String paramName, String defaultValue) {
    String param = getParameter(paramName);
    if (param == null) {
      return defaultValue;
    }
    return param;
  }

  public List<String> getListParameter(String paramName) {
    Object param = this.parameters.get(paramName);
    if (param == null) {
      return Collections.emptyList();
    }
    if (param instanceof String && ((String)param).indexOf(',') != -1) {
      List<String> listParam = Arrays.asList(((String)param).split(","));
      this.parameters.put(paramName, listParam);
      return listParam;
    }
    else if (param instanceof List) {
      List<String> listParam = (List<String>)param;
      return listParam;
    } else if (param instanceof JSONArray) {
      try {
        JSONArray jsonArray = (JSONArray)param;
        List<String> returnVal = Lists.newArrayListWithExpectedSize(jsonArray.length());
        for (int i = 0; i < jsonArray.length(); i++) {
          returnVal.add(jsonArray.getString(i));
        }
        return returnVal;
      } catch (JSONException je) {
        throw new SocialSpiException(ResponseError.BAD_REQUEST, je.getMessage(), je);
      }
    } else {
      // Allow up-conversion of non-array to array params.
      return Lists.newArrayList(param.toString());
    }
  }


  // Exposed for testing only
  void setParameter(String paramName, Object paramValue) {
    if (paramValue instanceof String[]) {
      String[] arr = (String[])paramValue;
      if (arr.length == 1) {
        this.parameters.put(paramName, arr[0]);
      } else {
        this.parameters.put(paramName, Lists.newArrayList(arr));
      }
    } else if (paramValue instanceof String) {
      String stringValue = (String)paramValue;
      if (stringValue.length() > 0) {
        this.parameters.put(paramName, stringValue);
      }
    } else {
      this.parameters.put(paramName, paramValue);
    }
  }
}
