/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.shindig.protocol;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.protocol.conversion.BeanConverter;
import org.apache.shindig.protocol.conversion.BeanJsonConverter;
import org.apache.shindig.protocol.model.FilterOperation;
import org.apache.shindig.protocol.model.SortOrder;
import org.apache.shindig.protocol.multipart.FormDataItem;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.base.Preconditions;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

/**
 * Default implementation of RequestItem
 */
public class BaseRequestItem implements RequestItem {

  protected final SecurityToken token;
  final BeanConverter converter;
  final Map<String,Object> parameters;
  final Map<String, FormDataItem> formItems;
  final BeanJsonConverter jsonConverter;
  Map<String,Object> attributes;

  public BaseRequestItem(Map<String, String[]> parameters,
      SecurityToken token,
      BeanConverter converter,
      BeanJsonConverter jsonConverter) {
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
    this.jsonConverter = jsonConverter;
    this.formItems = null;
  }

  public BaseRequestItem(JSONObject parameters,
      Map<String, FormDataItem> formItems,
      SecurityToken token,
      BeanConverter converter,
      BeanJsonConverter jsonConverter) {
    try {
      this.parameters = Maps.newHashMap();
      @SuppressWarnings("unchecked")
      // JSONObject keys are always strings
      Iterator<String> keys = parameters.keys();
      while (keys.hasNext()) {
        String key = keys.next();
        this.parameters.put(key, parameters.get(key));
      }
      this.token = token;
      this.converter = converter;
      this.formItems = formItems;
    } catch (JSONException je) {
      throw new ProtocolException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, je.getMessage(), je);
    }
    this.jsonConverter = jsonConverter;
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

  public String getSortBy() {
    return getParameter(SORT_BY);
  }

  public SortOrder getSortOrder() {
    String sortOrder = getParameter(SORT_ORDER);
    try {
      return sortOrder == null
            ? SortOrder.ascending
            : SortOrder.valueOf(sortOrder);
    } catch (IllegalArgumentException iae) {
      throw new ProtocolException(HttpServletResponse.SC_BAD_REQUEST,
           "Parameter " + SORT_ORDER + " (" + sortOrder + ") is not valid.");
    }
  }

  public String getFilterBy() {
    return getParameter(FILTER_BY);
  }

  public int getStartIndex() {
    String startIndex = getParameter(START_INDEX);
    try {
      return startIndex == null ? DEFAULT_START_INDEX
          : Integer.valueOf(startIndex);
    } catch (NumberFormatException nfe) {
      throw new ProtocolException(HttpServletResponse.SC_BAD_REQUEST,
          "Parameter " + START_INDEX + " (" + startIndex + ") is not a number.");
    }
  }

  public int getCount() {
    String count = getParameter(COUNT);
    try {
      return count == null ? DEFAULT_COUNT : Integer.valueOf(count);
    } catch (NumberFormatException nfe) {
      throw new ProtocolException(HttpServletResponse.SC_BAD_REQUEST,
           "Parameter " + COUNT + " (" + count + ") is not a number.");
    }
  }

  public FilterOperation getFilterOperation() {
    String filterOp = getParameter(FILTER_OPERATION);
    try {
      return filterOp == null
          ? FilterOperation.contains
          : FilterOperation.valueOf(filterOp);
    } catch (IllegalArgumentException iae) {
      throw new ProtocolException(HttpServletResponse.SC_BAD_REQUEST,
           "Parameter " + FILTER_OPERATION + " (" + filterOp + ") is not valid.");
    }
  }

  public String getFilterValue() {
    String filterValue = getParameter(FILTER_VALUE);
    return Objects.firstNonNull(filterValue, "");
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
    try {
      String json = getParameter(parameterName);
      if (json == null) {
        throw new ProtocolException(HttpServletResponse.SC_BAD_REQUEST, "missing data for " + parameterName);
      }
      return converter.convertToObject(json, dataTypeClass);
    } catch (RuntimeException e) {
      if (e.getCause() instanceof JSONException)
        throw new ProtocolException(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
      throw e;
    }
  }

  public <T> T getTypedRequest(Class<T> dataTypeClass) {
    try {
      return jsonConverter.convertToObject(new JSONObject(this.parameters).toString(),
          dataTypeClass);
    } catch (RuntimeException e) {
      if (e.getCause() instanceof JSONException)
        throw new ProtocolException(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
      throw e;
    }
  }

  public String getParameter(String paramName) {
    Object param = this.parameters.get(paramName);
    if (param instanceof List<?>) {
      if (((List<?>)param).isEmpty()) {
        return null;
      } else {
        param = ((List<?>)param).get(0);
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

  public Map<String, Object> getParameters() {
    return Collections.unmodifiableMap(this.parameters);
  }

  public List<String> getListParameter(String paramName) {
    Object param = this.parameters.get(paramName);
    if (param == null) {
      return Collections.emptyList();
    }
    if (param instanceof String && ((String)param).indexOf(',') != -1) {
      List<String> listParam = ImmutableList.copyOf(Splitter.on(',').split((String) param));
      this.parameters.put(paramName, listParam);
      return listParam;
    }
    else if (param instanceof List<?>) {
      // Assume it's a list of strings.  This is not type-safe.
      return (List<String>) param;
    } else if (param instanceof JSONArray) {
      try {
        JSONArray jsonArray = (JSONArray)param;
        List<String> returnVal = Lists.newArrayListWithCapacity(jsonArray.length());
        for (int i = 0; i < jsonArray.length(); i++) {
          returnVal.add(jsonArray.getString(i));
        }
        return returnVal;
      } catch (JSONException je) {
        throw new ProtocolException(HttpServletResponse.SC_BAD_REQUEST, je.getMessage(), je);
      }
    } else {
      // Allow up-conversion of non-array to array params.
      return Lists.newArrayList(param.toString());
    }
  }

  @VisibleForTesting
  public void setParameter(String paramName, Object paramValue) {
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

  public FormDataItem getFormMimePart(String partName) {
    if (formItems != null) {
      return formItems.get(partName);
    } else {
      return null;
    }
  }

  private Map<String,Object> getAttributeMap() {
     if (this.attributes == null){
       this.attributes = Maps.newHashMap();
     }
     return attributes;
  }

  public Object getAttribute(String val) {
    Preconditions.checkNotNull(val);
    return getAttributeMap().get(val);
  }

  public void setAttribute(String val, Object obj) {
    Preconditions.checkNotNull(val);
    if (obj == null) {
      getAttributeMap().remove(val);
    } else {
      getAttributeMap().put(val, obj);
    }
  }

  public Set<String> getParameterNames() {
    return this.parameters.keySet();
  }
}
