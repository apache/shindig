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

import static javax.persistence.GenerationType.IDENTITY;

import org.apache.shindig.social.opensocial.model.Name;
import org.apache.shindig.social.opensocial.model.Person;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;

import java.util.List;

@Entity
@Table(name="name")
@NamedQuery(name=NameDb.FINDBY_FAMILY_NAME,query="select n from NameDb n where n.familyName = :familyName ")
public class NameDb implements Name, DbObject {
  public static final String FINDBY_FAMILY_NAME = "q.name.findbyfamilyname";
  public static final String PARAM_FAMILY_NAME = "familyName";

  @Id
  @GeneratedValue(strategy=IDENTITY)
  @Column(name="oid")
  private long objectId;

  @Version
  @Column(name="version")
  protected long version;

  
  @OneToMany(targetEntity=PersonDb.class,mappedBy="name")
  private List<Person> persons;

  @Basic
  @Column(name="additional_name", length=255)
  private String additionalName;
  
  @Basic
  @Column(name="family_name", length=255)
  private String familyName;
  
  @Basic
  @Column(name="given_name", length=255)
  private String givenName;
  
  @Basic
  @Column(name="honorific_prefix", length=255)
  private String honorificPrefix;
  
  @Basic
  @Column(name="honorific_suffix", length=255)
  private String honorificSuffix;
  
  @Basic
  @Column(name="formatted", length=255)
  private String formatted;

  public NameDb() {
  }

  public NameDb(String formatted) {
    this.formatted = formatted;
  }


  public String getAdditionalName() {
    return additionalName;
  }

  public void setAdditionalName(String additionalName) {
    this.additionalName = additionalName;
  }

  public String getFamilyName() {
    return familyName;
  }

  public void setFamilyName(String familyName) {
    this.familyName = familyName;
  }

  public String getGivenName() {
    return givenName;
  }

  public void setGivenName(String givenName) {
    this.givenName = givenName;
  }

  public String getHonorificPrefix() {
    return honorificPrefix;
  }

  public void setHonorificPrefix(String honorificPrefix) {
    this.honorificPrefix = honorificPrefix;
  }

  public String getHonorificSuffix() {
    return honorificSuffix;
  }

  public void setHonorificSuffix(String honorificSuffix) {
    this.honorificSuffix = honorificSuffix;
  }

  /**
   * @return the objectId
   */
  public long getObjectId() {
    return objectId;
  }

  /**
   * @param objectId the objectId to set
   */
  public void setObjectId(long objectId) {
    this.objectId = objectId;
  }

  /**
   * @return the persons
   */
  public List<Person> getPersons() {
    return persons;
  }

  /**
   * @param persons the persons to set
   */
  public void setPersons(List<Person> persons) {
    this.persons = persons;
   
  }

  /* (non-Javadoc)
   * @see org.apache.shindig.social.opensocial.model.Name#getFormatted()
   */
  public String getFormatted() {
    return formatted;
  }

  /* (non-Javadoc)
   * @see org.apache.shindig.social.opensocial.model.Name#setFormatted(java.lang.String)
   */
  public void setFormatted(String formatted) {
    this.formatted = formatted;
  }
}
