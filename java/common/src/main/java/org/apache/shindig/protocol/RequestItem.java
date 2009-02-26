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
package org.apache.shindig.protocol;

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.protocol.model.FilterOperation;
import org.apache.shindig.protocol.model.SortOrder;
import org.apache.shindig.protocol.multipart.FormDataItem;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * A request to pass to a bound service handler
 */
public interface RequestItem {

  // Common OpenSocial API fields
  String APP_ID = "appId";
  String START_INDEX = "startIndex";
  String COUNT = "count";
  String SORT_BY = "sortBy";
  String SORT_ORDER = "sortOrder";
  String FILTER_BY = "filterBy";
  String FILTER_OPERATION = "filterOp";
  String FILTER_VALUE = "filterValue";
  String FIELDS = "fields";// Opensocial defaults
  int DEFAULT_START_INDEX = 0;
  int DEFAULT_COUNT = 20;
  String APP_SUBSTITUTION_TOKEN = "@app";

  String getAppId();

  Date getUpdatedSince();

  int getStartIndex();

  int getCount();

  String getSortBy();

  SortOrder getSortOrder();

  String getFilterBy();

  FilterOperation getFilterOperation();

  String getFilterValue();

  Set<String> getFields();

  Set<String> getFields(Set<String> defaultValue);

  SecurityToken getToken();

  <T> T getTypedParameter(String parameterName, Class<T> dataTypeClass);

  <T> T getTypedRequest(Class<T> dataTypeClass);

  String getParameter(String paramName);

  String getParameter(String paramName, String defaultValue);

  List<String> getListParameter(String paramName);
  
  FormDataItem getFormMimePart(String partName);
}
