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

import org.apache.shindig.social.opensocial.model.Account;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;
import javax.persistence.GeneratedValue;
import javax.persistence.Version;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * Storage object for the Person. Stored in a table "account" which is also used by
 * joined extension classes, the account usage column defines the class where the extension
 * is used, but in most cases addresses are shared and this column has the value "sharedaccount"
 * For more information on the API see {@link Account}
 */
@Entity
@Table(name = "account")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "account_usage")
@DiscriminatorValue(value = "sharedaccount")
public class AccountDb implements Account, DbObject {
  /**
   * The internal object ID used for references to this object. Should be generated
   * by the underlying storage mechanism
   */
  @Id
  @GeneratedValue(strategy = IDENTITY)
  @Column(name = "oid")
  protected long objectId;

  /**
   * An optimistic locking field.
   */
  @Version
  @Column(name = "version")
  protected long version;

  /**
   * model field.
   * @see Account
   */
  @Basic
  @Column(name = "domain", length = 255)
  protected String domain;

  /**
   * model field.
   * @see Account
   */
  @Basic
  @Column(name = "user_id", length = 255)
  protected String userId;

  /**
   * model field.
   * @see Account
   */
  @Basic
  @Column(name = "username", length = 255)
  protected String username;

  public AccountDb() {
  }

  public AccountDb(String domain, String userId, String username) {
    this.domain = domain;
    this.userId = userId;
    this.username = username;
  }

  public String getDomain() {
    return domain;
  }

  public void setDomain(String domain) {
    this.domain = domain;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  /**
   * @return the objectId
   */
  public long getObjectId() {
    return objectId;
  }

}
