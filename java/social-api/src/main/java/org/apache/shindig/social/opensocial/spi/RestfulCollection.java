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

import java.util.List;

public class RestfulCollection<T> {
  private List<T> entry;
  private int startIndex;
  private int totalResults;

  private boolean filtered = true;
  private boolean sorted = true;
  private boolean updatedSince = true;


  public RestfulCollection(List<T> entry) {
    this(entry, 0, entry.size());
  }

  public RestfulCollection(List<T> entry, int startIndex, int totalResults) {
    this.entry = entry;
    this.startIndex = startIndex;
    this.totalResults = totalResults;
  }

  public List<T> getEntry() {
    return entry;
  }

  public void setEntry(List<T> entry) {
    this.entry = entry;
  }

  public int getStartIndex() {
    return startIndex;
  }

  public void setStartIndex(int startIndex) {
    this.startIndex = startIndex;
  }

  public int getTotalResults() {
    return totalResults;
  }

  public void setTotalResults(int totalResults) {
    this.totalResults = totalResults;
  }

  public boolean isFiltered() {
    return filtered;
  }

  public void setFiltered(boolean filtered) {
    this.filtered = filtered;
  }

  public boolean isSorted() {
    return sorted;
  }

  public void setSorted(boolean sorted) {
    this.sorted = sorted;
  }

  public boolean isUpdatedSince() {
    return updatedSince;
  }

  public void setUpdatedSince(boolean updatedSince) {
    this.updatedSince = updatedSince;
  }
}
