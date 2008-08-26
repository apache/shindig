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
package org.apache.shindig.social.opensocial.spi;

import org.apache.commons.lang.StringUtils;

public class CollectionOptions {
  private PersonService.SortBy sortBy;
  private PersonService.SortOrder sortOrder;
  private PersonService.FilterType filter;
  private PersonService.FilterOperation filterOperation;
  private String filterValue;
  private int first;
  private int max;

  public PersonService.SortBy getSortBy() {
    return sortBy;
  }

  public void setSortBy(PersonService.SortBy sortBy) {
    this.sortBy = sortBy;
  }

  public PersonService.SortOrder getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(PersonService.SortOrder sortOrder) {
    this.sortOrder = sortOrder;
  }

  public PersonService.FilterType getFilter() {
    return filter;
  }

  public void setFilter(PersonService.FilterType filter) {
    this.filter = filter;
  }

  public PersonService.FilterOperation getFilterOperation() {
    return filterOperation;
  }

  public void setFilterOperation(PersonService.FilterOperation filterOperation) {
    this.filterOperation = filterOperation;
  }

  public String getFilterValue() {
    return filterValue;
  }

  public void setFilterValue(String filterValue) {
    this.filterValue = filterValue;
  }

  public int getFirst() {
    return first;
  }

  public void setFirst(int first) {
    this.first = first;
  }

  public int getMax() {
    return max;
  }

  public void setMax(int max) {
    this.max = max;
  }

  // These are overriden so that EasyMock doesn't throw a fit
  @Override
  public boolean equals(Object o) {
    if (!(o instanceof CollectionOptions)) {
      return false;
    }

    CollectionOptions actual = (CollectionOptions) o;
    return this.sortBy == actual.sortBy
        && this.sortOrder == actual.sortOrder
        && this.filter == actual.filter
        && this.filterOperation == actual.filterOperation
        && StringUtils.equals(this.filterValue, actual.filterValue)
        && this.first == actual.first
        && this.max == actual.max;
  }

  @Override
  public int hashCode() {
    return getHashCode(this.sortBy) + getHashCode(this.sortOrder) + getHashCode(this.filter)
        + getHashCode(this.filterOperation) + getHashCode(this.filterValue)
        + getHashCode(this.first) + getHashCode(this.max);
  }

  private int getHashCode(Object o) {
    return o == null ? 0 : o.hashCode();
  }
}
