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

import org.apache.shindig.social.opensocial.jpa.api.DbObject;
import org.apache.shindig.social.opensocial.model.ListField;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.MappedSuperclass;
import javax.persistence.Table;
import javax.persistence.Version;

/**
 * List fields represent storage of list of fields potentially with a preferred or primary value.
 * This is the base storage class for all list fields, stored in the lsit_field table. Classes will
 * extend this class using a join strategy and setting the list_field_type column to the type of
 * class represented by the record. If there is no type it defaults to ListFieldDb.
 */
@MappedSuperclass
@Table(name="list_field")
@Inheritance(strategy=InheritanceType.JOINED)
@DiscriminatorColumn(name="list_field_type", length=30, discriminatorType=DiscriminatorType.STRING)
@DiscriminatorValue(value="ListFieldDb")
public class ListFieldDb implements ListField, DbObject {
  /**
   * The internal object ID used for references to this object. Should be generated 
   * by the underlying storage mechanism
   */
  @Id
  @GeneratedValue(strategy=IDENTITY)
  @Column(name="oid")
  protected long objectId;
  
  /**
   * An optimistic locking field.
   */
  @Version
  @Column(name="version")
  protected long version;

  
  /**
   * model field.
   * @see org.apache.shindig.social.opensocial.model.ListField
   */
  @Basic
  @Column(name="field_type", length=255)
  protected String type;
  
  /**
   * model field.
   * @see org.apache.shindig.social.opensocial.model.ListField
   */
  @Basic
  @Column(name="field_value", length=255)
  protected String value;
  
  /**
   * model field.
   * @see org.apache.shindig.social.opensocial.model.ListField
   */
  @Basic
  @Column(name="primary_field")
  protected Boolean primary;

  /**
   * Create a list field.
   */
  public ListFieldDb() { }

  /**
   * Create a list field, specifying the type and the value.
   * @param type the type or name of the field
   * @param value the value contained in the field.
   */
  public ListFieldDb(String type, String value) {
    this.type = type;
    this.value = value;
  }

  /** 
   * {@inheritDoc}
   * @see org.apache.shindig.social.opensocial.model.ListField#getType()
   */
  public String getType() {
    return type;
  }

  /** 
   * {@inheritDoc}
   * @see org.apache.shindig.social.opensocial.model.ListField#setType(java.lang.String)
   */
  public void setType(String type) {
    this.type = type;
  }

  /** 
   * {@inheritDoc}
   * @see org.apache.shindig.social.opensocial.model.ListField#getValue()
   */
  public String getValue() {
    return value;
  }

  /** 
   * {@inheritDoc}
   * @see org.apache.shindig.social.opensocial.model.ListField#setValue(java.lang.String)
   */
  public void setValue(String value) {
    this.value = value;
  }

  /** 
   * {@inheritDoc}
   * @see org.apache.shindig.social.opensocial.model.ListField#getPrimary()
   */
  public Boolean getPrimary() {
    return primary;
  }

  /** 
   * {@inheritDoc}
   * @see org.apache.shindig.social.opensocial.model.ListField#setPrimary(java.lang.Boolean)
   */
  public void setPrimary(Boolean primary) {
    this.primary = primary;
  }

  /** 
   * {@inheritDoc}
   * @see org.apache.shindig.social.opensocial.jpa.api.DbObject#getObjectId()
   */
  public long getObjectId() {
    return objectId;
  }

}
