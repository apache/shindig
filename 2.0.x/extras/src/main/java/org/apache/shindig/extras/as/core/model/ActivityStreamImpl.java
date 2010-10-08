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

package org.apache.shindig.extras.as.core.model;

import java.util.List;

import org.apache.shindig.extras.as.opensocial.model.ActivityEntry;
import org.apache.shindig.extras.as.opensocial.model.ActivityStream;

/**
 * A simple, bean-based version of an ActivityStream
 */
public class ActivityStreamImpl implements ActivityStream {

  private String displayName;
  private String language;
  private List<ActivityEntry> entries;
  private String id;
  private String subject;
  
  /**
   * Create a new empty ActivityStream
   */
  public ActivityStreamImpl() {
    this.displayName = null;
    this.language = null;
    this.entries = null;
    this.id = null;
    this.subject = null;
  }

  /** {@inheritDoc} */
  public String getDisplayName() {
    return displayName;
  }

  /** {@inheritDoc} */
  public List<ActivityEntry> getEntries() {
    return entries;
  }

  /** {@inheritDoc} */
  public String getId() {
    return id;
  }

  /** {@inheritDoc} */
  public String getLanguage() {
    return language;
  }

  /** {@inheritDoc} */
  public String getSubject() {
    return subject;
  }

  /** {@inheritDoc} */
  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  /** {@inheritDoc} */
  public void setEntries(List<ActivityEntry> entries) {
    this.entries = entries;
  }

  /** {@inheritDoc} */
  public void setId(String id) {
    this.id = id;
  }

  /** {@inheritDoc} */
  public void setLanguage(String language) {
    this.language = language;
  }

  /** {@inheritDoc} */
  public void setSubject(String subject) {
    this.subject = subject;
  }
}
