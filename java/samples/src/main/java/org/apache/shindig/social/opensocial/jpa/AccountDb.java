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

@Entity
@Table(name = "account")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "account_usage")
@DiscriminatorValue(value = "sharedaccount")
public class AccountDb implements Account, DbObject {
  @Id
  @GeneratedValue(strategy = IDENTITY)
  @Column(name = "oid")
  protected long objectId;

  @Version
  @Column(name = "version")
  protected long version;

  @Basic
  @Column(name = "domain", length = 255)
  protected String domain;

  @Basic
  @Column(name = "user_id", length = 255)
  protected String userId;

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

  /**
   * @param objectId the objectId to set
   */
  public void setObjectId(long objectId) {
    this.objectId = objectId;
  }
}
