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

import java.util.HashMap;
import java.util.List;

/**
 * Data structure representing a Rest response.
 */
public class RestfulCollection<T> extends HashMap<String, Object>{
  private List<T> list;
  private int startIndex;
  private int totalResults;
  private int itemsPerPage;

  private boolean filtered = false;
  private boolean sorted = false;
  private boolean updatedSince = false;

  /**
   * Creates a new RestfulCollection that includes a complete set of entries.
   *
   * Default values for startIndex, totalResults, itemsPerPage and filtering parameters are automatically set.
   *
   * @param entry a list of entries
   */
  public RestfulCollection(List<T> entry) {
    this(entry, 0, entry.size(), entry.size());
    this.filtered=true;
    this.sorted=true;
    this.updatedSince=true;
    put("filtered", true);
    put("sorted", true);
    put("updatedSince", true);
  }

  /**
   * Create a paginated collection response.
   *
   * @param list paginated entries
   * @param startIndex the index corresponding to the first element of {entry}
   * @param totalResults the total size of the resultset
   * @param itemsPerPage the size of the pagination, generally set to the user-specified count parameter. Clamped to the totalResults size automatically
   *
   * @since 1.1-BETA4
   */
  public RestfulCollection(List<T> list, int startIndex, int totalResults, int itemsPerPage) {
    this.list = list;
    this.startIndex = startIndex;
    this.totalResults = totalResults;
    this.itemsPerPage = Math.min(itemsPerPage, totalResults);
    put("list", list);
    put("startIndex", startIndex);
    put("totalResults", totalResults);
    put("itemsPerPage", this.itemsPerPage);
  }


  /**
   * Helper constructor for un-paged collection,
   * Use {@link #RestfulCollection(java.util.List, int, int, int)} in paginated context
   */
  public RestfulCollection(List<T> entry, int startIndex, int totalResults) {
    this(entry, startIndex, totalResults, entry.size());
  }

  public List<T> getList() {
    return list;
  }

  public void setList(List<T> list) {
    this.list = list;
    put("list", list);
  }

  public int getStartIndex() {
    return startIndex;
  }

  public void setStartIndex(int startIndex) {
    this.startIndex = startIndex;
    put("startIndex", startIndex);
  }

  public int getTotalResults() {
    return totalResults;
  }

  public void setItemsPerPage(int itemsPerPage) {
    this.itemsPerPage = itemsPerPage;
    put("itemsPerPage", itemsPerPage);
  }

  public int getItemsPerPage() {
    return itemsPerPage;
  }

  public void setTotalResults(int totalResults) {
    this.totalResults = totalResults;
    put("totalResults", totalResults);
  }

  public boolean isFiltered() {
    return filtered;
  }

  public void setFiltered(boolean filtered) {
    this.filtered = filtered;
    put("filtered", filtered);
  }

  public boolean isSorted() {
    return sorted;
  }

  public void setSorted(boolean sorted) {
    this.sorted = sorted;
    put("sorted", sorted);
  }

  public boolean isUpdatedSince() {
    return updatedSince;
  }

  public void setUpdatedSince(boolean updatedSince) {
    this.updatedSince = updatedSince;
    put("updatedSince", updatedSince);
  }
}
