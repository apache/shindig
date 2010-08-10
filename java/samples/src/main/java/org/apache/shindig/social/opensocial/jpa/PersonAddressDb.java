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

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 *
 */
/*
 * This object connects to a single Address, and to a single organization, 
 * defining the organizations relationship with the address
 */
@Entity
@Table(name="person_address")
@DiscriminatorValue(value="sharedaddress")
public class PersonAddressDb extends AddressDb {
  @Basic
  @Column(name="primary_address")
  private Boolean primary;
  
  @ManyToOne(targetEntity=PersonDb.class)
  @JoinColumn(name="person_id", referencedColumnName="oid")
  private Person person;
  
  @Basic
  @Column(name="type", length=255)
  private String type;


  public PersonAddressDb() {
    // TODO Auto-generated constructor stub
  }


  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }


  public Boolean getPrimary() {
    return primary;
  }

  public void setPrimary(Boolean primary) {
    this.primary = primary;
  }


  /**
   * @return the person
   */
  public Person getPerson() {
    return person;
  }


  /**
   * @param person the person to set
   */
  public void setPerson(Person person) {
    this.person = person;
  }


}
