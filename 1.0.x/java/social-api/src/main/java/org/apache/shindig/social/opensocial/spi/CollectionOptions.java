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

import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.apache.shindig.social.opensocial.service.RequestItem;

public class CollectionOptions {
  private String sortBy;
  private PersonService.SortOrder sortOrder;
  private String filter;
  private PersonService.FilterOperation filterOperation;
  private String filterValue;
  private int first;
  private int max;
  private Date updatedSince;

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

  public PersonService.SortOrder getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(PersonService.SortOrder sortOrder) {
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

  public PersonService.FilterOperation getFilterOperation() {
    return filterOperation;
  }

  public void setFilterOperation(PersonService.FilterOperation filterOperation) {
    this.filterOperation = filterOperation;
  }

  /**
   * Where a field filter has been specified (ie a non special filter) then this is the value of the
   * filter. The exception is the isFriendsWith filter where this contains the value of the id who
   * the all the results need to be friends with.
   *
   * @return
   */
  public String getFilterValue() {
    return filterValue;
  }

  public void setFilterValue(String filterValue) {
    this.filterValue = filterValue;
  }

  /**
   * When paginating, the index of the first item to fetch.
   * @return
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
   * @return
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
  public boolean equals(Object o) {
    if (!(o instanceof CollectionOptions)) {
      return false;
    }

    CollectionOptions actual = (CollectionOptions) o;
    return StringUtils.equals(this.sortBy, actual.sortBy)
        && this.sortOrder == actual.sortOrder
        && StringUtils.equals(this.filter, actual.filter)
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
