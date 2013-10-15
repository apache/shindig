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
package org.apache.shindig.social.core.util.atom;


import org.apache.shindig.protocol.DataCollection;
import org.apache.shindig.protocol.RestfulCollection;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * represents an atom:feed entry
 */
public class AtomFeed {

  private Collection<AtomEntry> entry;
  @SuppressWarnings("unused")
  private int startIndex;
  @SuppressWarnings("unused")
  private int totalResults;
  @SuppressWarnings("unused")
  private int itemsPerPage;
  @SuppressWarnings("unused")
  private String author;
  @SuppressWarnings("unused")
  private String title;
  @SuppressWarnings("unused")
  private String updated;
  @SuppressWarnings("unused")
  private String id;
  @SuppressWarnings("unused")
  private AtomLink link;

  public static final String AUTHOR = "author";
  public static final String TITLE = "title";
  public static final String UPDATED = "updated";
  public static final String ID = "id";
  public static final String URL = "url";

  /**
   * @param obj
   */
  @SuppressWarnings("unchecked")
  public AtomFeed(Object obj) {
    Preconditions.checkNotNull(obj);
    if (obj instanceof RestfulCollection<?>) {
      RestfulCollection<?> r = (RestfulCollection<?>) obj;
      entry = Lists.newArrayList();
      List<?> entryList = r.getList();
      for (Object o : entryList) {
        entry.add(new AtomEntry(o));
      }
      startIndex = r.getStartIndex();
      totalResults = r.getTotalResults();
      itemsPerPage = r.getItemsPerPage();
      author = (r.get(AUTHOR)==null) ? "?" : r.get(AUTHOR).toString();
      title = (r.get(TITLE)==null) ? "?" : r.get(TITLE).toString();
      id = (r.get(ID)==null) ? "?" : r.get(ID).toString();
      updated = (r.get(UPDATED)==null) ? "" : r.get(UPDATED).toString();

      if (r.get(URL)!=null) {
        link = new AtomLink("self", r.get(URL).toString());
      }

    } else if (obj instanceof Map) {
      Map<?, ?> m = (Map<?, ?>) obj;
      entry = Lists.newArrayList();
      for ( Entry<?, ?> o : m.entrySet()) {
        entry.add(new AtomEntry(o));
      }
      startIndex = 0;
      totalResults = entry.size();
      itemsPerPage = entry.size();
    } else if ( obj instanceof DataCollection ) {
      DataCollection dc = (DataCollection) obj;
      entry = Lists.newArrayList();
      for ( Entry<String, Map<String,Object>> o : dc.getEntry().entrySet()) {
        entry.add(new AtomEntry(o));
      }
      startIndex = 0;
      totalResults = entry.size();
      itemsPerPage = entry.size();
    } else {
      entry = ImmutableList.of(new AtomEntry(obj));
      startIndex = 0;
      totalResults = 1;
      itemsPerPage = 1;
    }
  }

}
