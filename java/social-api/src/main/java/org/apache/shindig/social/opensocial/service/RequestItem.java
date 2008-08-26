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

import com.google.common.collect.Sets;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.Set;

/**
 * Abstract base type for social API requests.
 */
public abstract class RequestItem {

  // Common OpenSocial API fields
  public static final String APP_ID = "appId";

  public static final String USER_ID = "userId";

  public static final String GROUP_ID = "groupId";

  public static final String START_INDEX = "startIndex";

  public static final String COUNT = "count";

  public static final String SORT_BY = "sortBy";
  public static final String SORT_ORDER = "sortOrder";

  public static final String FILTER_BY = "filterBy";
  public static final String FILTER_OPERATION = "filterOp";
  public static final String FILTER_VALUE = "filterValue";

  public static final String FIELDS = "fields";

  // Opensocial defaults
  public static final int DEFAULT_START_INDEX = 0;

  public static final int DEFAULT_COUNT = 20;

  public static final String APP_SUBSTITUTION_TOKEN = "@app";

  private final SecurityToken token;

  protected final BeanConverter converter;

  private final String operation;

  private final String service;

  public RequestItem(String service, String operation, SecurityToken token,
      BeanConverter converter) {
    this.service = service;
    this.operation = operation;
    this.token = token;
    this.converter = converter;
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
    if (ids.isEmpty()) {
      if (token.getViewerId() != null) {
        // Assume @me
        ids = Lists.newArrayList(token.getViewerId());
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
    return startIndex == null ? DEFAULT_START_INDEX
        : Integer.valueOf(startIndex);
  }

  public int getCount() {
    String count = getParameter(COUNT);
    return count == null ? DEFAULT_COUNT : Integer.valueOf(count);
  }

  public String getSortBy() {
    String sortBy = getParameter(SORT_BY);
    return sortBy == null ? PersonService.TOP_FRIENDS_SORT : sortBy;
  }

  public PersonService.SortOrder getSortOrder() {
    String sortOrder = getParameter(SORT_ORDER);
    return sortOrder == null
        ? PersonService.SortOrder.ascending
        : PersonService.SortOrder.valueOf(sortOrder);
  }

  public String getFilterBy() {
    return getParameter(FILTER_BY);
  }

  public PersonService.FilterOperation getFilterOperation() {
    String filterOp = getParameter(FILTER_OPERATION);
    return filterOp == null
        ? PersonService.FilterOperation.contains
        : PersonService.FilterOperation.valueOf(filterOp);
  }

  public String getFilterValue() {
    String filterValue = getParameter(FILTER_VALUE);
    return filterValue == null ? "" : filterValue;
  }

  public Set<String> getFields() {
    return getFields(Sets.<String>newHashSet());
  }

  public Set<String> getFields(Set<String> defaultValue) {
    Set result = Sets.newHashSet();
    result.addAll(getListParameter(FIELDS));
    if (result.isEmpty()) {
      return defaultValue;
    }
    return result;
  }

  public String getOperation() {
    return operation;
  }

  public String getService() {
    return service;
  }

  public SecurityToken getToken() {
    return token;
  }

  public abstract <T> T getTypedParameter(String parameterName, Class<T> postDataClass);

  public abstract void applyUrlTemplate(String urlTemplate);

  public abstract String getParameter(String paramName);

  public abstract String getParameter(String paramName, String defaultValue);

  public abstract List<String> getListParameter(String paramName);
}
