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
package org.apache.shindig.social.opensocial.jpa;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

/**
 * This is a property of a group, extending the listfield type, and using the type property to
 * perform the mapping. Main storage is in the listfield table, but group property stores the
 * details of the properties of the group.
 */
@Entity
@Table(name = "group_property")
@PrimaryKeyJoinColumn(name = "oid")
public class GroupPropertyDb extends ListFieldDb {
  /**
   * The group relationship connected with this property.
   */
  @ManyToOne(targetEntity = GroupDb.class)
  @JoinColumn(name = "group_id", referencedColumnName = "oid")
  protected GroupDb group;

  /**
   * @return the group
   */
  public GroupDb getGroup() {
    return group;
  }

  /**
   * @param group the group to set
   */
  public void setGroup(GroupDb group) {
    this.group = group;
  }
  



}
