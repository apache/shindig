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

import org.apache.shindig.social.opensocial.model.Address;
import org.apache.shindig.social.opensocial.model.BodyType;
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

/**
 * The body type entity, stored in "body_type" 
 *
 */
@Entity
@Table(name = "body_type")
@NamedQuery(name = BodyTypeDb.FINDBY_HEIGHT,
    query = "select b from BodyTypeDb b where b.height = :height ")
public class BodyTypeDb implements BodyType, DbObject {
  /**
   * Name of the JPA query to find BodyType by exact height, mainly used in testing.
   */
  public static final String FINDBY_HEIGHT = "q.bosytype.findbyheight";

  /**
   * name of the height parameter in JPA queries.
   */
  public static final String PARAM_HEIGHT = "height";

  /**
   * The internal object ID used for references to this object. Should be generated 
   * by the underlying storage mechanism
   */
  @Id
  @GeneratedValue(strategy = IDENTITY)
  @Column(name = "oid")
  private long objectId;

  /**
   * An optimistic locking field
   */
  @Version
  @Column(name = "version")
  protected long version;

  @OneToMany(targetEntity = PersonDb.class, mappedBy = "bodyType")
  private List<Person> persons;

  /**
   * model field.
   * @see BodyType
   */
  @Basic
  @Column(name = "build", length = 255)
  private String build;

  /**
   * model field.
   * @see BodyType
   */
  @Basic
  @Column(name = "eye_color", length = 255)
  private String eyeColor;

  /**
   * model field.
   * @see BodyType
   */
  @Basic
  @Column(name = "hair_color", length = 255)
  private String hairColor;

  /**
   * model field.
   * @see BodyType
   */
  @Basic
  @Column(name = "height", length = 255)
  private String height;

  /**
   * model field.
   * @see BodyType
   */
  @Basic
  @Column(name = "weight", length = 255)
  private String weight;

  public String getBuild() {
    return build;
  }

  public void setBuild(String build) {
    this.build = build;
  }

  public String getEyeColor() {
    return eyeColor;
  }

  public void setEyeColor(String eyeColor) {
    this.eyeColor = eyeColor;
  }

  public String getHairColor() {
    return hairColor;
  }

  public void setHairColor(String hairColor) {
    this.hairColor = hairColor;
  }

  public String getHeight() {
    return height;
  }

  public void setHeight(String height) {
    this.height = height;
  }

  public String getWeight() {
    return weight;
  }

  public void setWeight(String weight) {
    this.weight = weight;
  }

  /**
   * @return the objectId
   */
  public long getObjectId() {
    return objectId;
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
}
