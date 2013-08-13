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
package org.apache.shindig.social.opensocial.spi;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.shindig.protocol.RequestItem;
import org.apache.shindig.protocol.model.FilterOperation;
import org.apache.shindig.protocol.model.SortOrder;

import com.google.common.base.Objects;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Data structure representing many of the RPC/REST requests we receive.
 */
public class CollectionOptions {
  private String sortBy;
  private SortOrder sortOrder;
  private String filter;
  private FilterOperation filterOperation;
  private String filterValue;
  private int first;
  private int max;
  private Date updatedSince;

  private Map<String, String> optionalParameters;
  private static final String[] predefinedParameters= {
      RequestItem.COUNT,
      RequestItem.FIELDS,
      RequestItem.FILTER_BY,
      RequestItem.FILTER_OPERATION,
      RequestItem.FILTER_VALUE,
      RequestItem.SORT_BY,
      RequestItem.SORT_ORDER,
      RequestItem.START_INDEX,
      "userId",
      "groupId"
    };
  public CollectionOptions() {}

  public CollectionOptions(RequestItem request) {
    this.sortBy = request.getSortBy();
    this.sortOrder = request.getSortOrder();
    this.setFilter(request.getFilterBy());
    this.setFilterOperation(request.getFilterOperation());
    this.setFilterValue(request.getFilterValue());
    this.setFirst(request.getStartIndex());
    this.setMax(request.getCount());
    this.setUpdatedSince(request.getUpdatedSince());
    Set<String> parameterNames = Sets.newHashSet(request.getParameterNames());
    parameterNames.removeAll(Arrays.asList(predefinedParameters));
    Map<String, String> optionalParameters = Maps.newHashMap();
    for (String parameter : parameterNames) {
      optionalParameters.put(parameter, request.getParameter(parameter));
    }
    this.setOptionalParameters(optionalParameters);
  }
  /**
   * This sortBy can be any field of the object being sorted or the special js sort of topFriends.
   * @return The field to sort by
   */
  public String getSortBy() {
    return sortBy;
  }

  public void setSortBy(String sortBy) {
    this.sortBy = sortBy;
  }

  public SortOrder getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(SortOrder sortOrder) {
    this.sortOrder = sortOrder;
  }

  /**
   * <p>
   * This filter can be any field of the object being filtered or the special js filters,
   * hasApp or topFriends.
   * Other special Filters are
   * </p>
   * <dl>
   * <dt>all</dt>
   * <dd>Retrieves all friends</dd>
   * <dt>hasApp</dt>
   * <dd>Retrieves all friends with any data for this application.</dd>
   * <dt>'topFriends</dt>
   * <dd>Retrieves only the user's top friends.</dd>
   * <dt>isFriendsWith</dt>
   * <dd>Will filter the people requested by checking if they are friends with
   * the given <a href="opensocial.IdSpec.html">idSpec</a>. Expects a
   *    filterOptions parameter to be passed with the following fields defined:
   *  - idSpec The <a href="opensocial.IdSpec.html">idSpec</a> that each person
   *        must be friends with.</dd>
   * </dl>
   * @return The field to filter by
   */
  public String getFilter() {
    return filter;
  }

  public void setFilter(String filter) {
    this.filter = filter;
  }

  public FilterOperation getFilterOperation() {
    return filterOperation;
  }

  public void setFilterOperation(FilterOperation filterOperation) {
    this.filterOperation = filterOperation;
  }

  /**
   * Where a field filter has been specified (ie a non special filter) then this is the value of the
   * filter. The exception is the isFriendsWith filter where this contains the value of the id who
   * the all the results need to be friends with.
   *
   * @return the filter value
   */
  public String getFilterValue() {
    return filterValue;
  }

  public void setFilterValue(String filterValue) {
    this.filterValue = filterValue;
  }

  /**
   * When paginating, the index of the first item to fetch.
   * @return the value of first
   */
  public int getFirst() {
    return first;
  }

  public void setFirst(int first) {
    this.first = first;
  }


  /**
   * The maximum number of items to fetch; defaults to 20. If set to a larger
   * number, a container may honor the request, or may limit the number to a
   * container-specified limit of at least 20.
   * @return the value of max
   */
  public int getMax() {
    return max;
  }

  public void setMax(int max) {
    this.max = max;
  }

  public Date getUpdatedSince() {
    return updatedSince;
  }

  public void setUpdatedSince(Date updatedSince) {
    this.updatedSince = updatedSince;
  }


  // These are overriden so that EasyMock doesn't throw a fit
  @Override
  public boolean equals(final Object o) {
    if (o == this) {
      return true;
    }

    if (!(o instanceof CollectionOptions)) {
      return false;
    }

    CollectionOptions actual = (CollectionOptions) o;
    return Objects.equal(this.sortBy, actual.sortBy)
        && this.sortOrder == actual.sortOrder
        && Objects.equal(this.filter, actual.filter)
        && this.filterOperation == actual.filterOperation
        && Objects.equal(this.filterValue, actual.filterValue)
        && this.first == actual.first
        && this.max == actual.max;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(this.sortBy, this.sortOrder, this.filter,
        this.filterOperation, this.filterValue, this.first, this.max);
  }

  public Map<String, String> getOptionalParameter() {
    return this.optionalParameters;
  }

  private void setOptionalParameters(Map<String, String> optionalParameters) {
    this.optionalParameters = optionalParameters;
  }

}
