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

import org.apache.shindig.social.opensocial.model.Person;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

/**
 * IM (Instant Message account) Entity, extends the ListField object (and list_field table), joining
 * on the object ID. Objects of this type will have "list_field_type" set to ImDb in list_field
 */
@Entity
@Table(name = "im")
@PrimaryKeyJoinColumn(name = "oid")
public class ImDb extends ListFieldDb {

  /**
   * The person who owns this IM account. This information is maintained in the database using a
   * join column "person_id" in the im table that points to oid in the person table.
   */
  @ManyToOne(targetEntity = PersonDb.class)
  @JoinColumn(name = "person_id", referencedColumnName = "oid")
  protected Person person;

}
