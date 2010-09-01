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
package org.apache.shindig.social.core.model;

import org.apache.shindig.social.opensocial.model.Group;
import org.apache.shindig.social.opensocial.spi.GroupId;

/**
 * Default Implementation of the {@link org.apache.shindig.social.opensocial.model.Group} model.
 *
 * @since 2.0.0
 */
public class GroupImpl implements Group {

  private GroupId groupId;
  private String title;
  private String description;

  public String getTitle() {
    return title;
  }

  /** {@inheritDoc} */
  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  /** {@inheritDoc} */
  public void setDescription(String description) {
    this.description = description;
  }

  /** {@inheritDoc} */
  public void setId(GroupId groupId) {
    this.groupId = groupId;
  }

  public GroupId getId() {
    return groupId;
  }
}
