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

import java.util.Date;
import java.util.List;

import org.apache.shindig.social.opensocial.jpa.api.DbObject;
import org.apache.shindig.social.opensocial.model.Message;
import org.apache.shindig.social.opensocial.model.Url;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.PostLoad;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;

/**
 * Messages are stored in the message table.
 */
@Entity
@Table(name="message")
public class MessageDb implements Message, DbObject {
  /**
   * The internal object ID used for references to this object. Should be generated
   * by the underlying storage mechanism
   */
  @Id
  @GeneratedValue(strategy=IDENTITY)
  @Column(name="oid")
  protected long objectId;

  /**
   * An optimistic locking field
   */
  @Version
  @Column(name="version")
  protected long version;

  /**
   * model field.
   * @see org.apache.shindig.social.opensocial.model.Message
   */
  @Basic
  @Column(name="body", length=255)
  protected String body;

  /**
   * model field.
   * @see org.apache.shindig.social.opensocial.model.Message
   */
  @Basic
  @Column(name="title", length=255)
  protected String title;

  /**
   * model field. (database representation of type)
   * @see org.apache.shindig.social.opensocial.model.Message
   */
  @Basic
  @Column(name="message_type")
  protected String typeDb;

  /**
   * model field.
   * @see org.apache.shindig.social.opensocial.model.Message
   */
  @Transient
  protected Type type;

  /**
   * create an empty message.
   */
  public MessageDb() {
  }

  /**
   * Create a message object with body, title and type.
   * @param initBody the body of the message.
   * @param initTitle the title of the message.
   * @param initType the type of the message.
   */
  public MessageDb(String initBody, String initTitle, Type initType) {
    this.body = initBody;
    this.title = initTitle;
    this.type = initType;
  }

  /**
   * {@inheritDoc}
   * @see org.apache.shindig.social.opensocial.model.Message#getBody()
   */
  public String getBody() {
    return this.body;
  }

  /**
   * {@inheritDoc}
   * @see org.apache.shindig.social.opensocial.model.Message#setBody(java.lang.String)
   */
  public void setBody(String newBody) {
    this.body = newBody;
  }

  /**
   * {@inheritDoc}
   * @see org.apache.shindig.social.opensocial.model.Message#getTitle()
   */
  public String getTitle() {
    return this.title;
  }

  /**
   * {@inheritDoc}
   * @see org.apache.shindig.social.opensocial.model.Message#setTitle(java.lang.String)
   */
  public void setTitle(String newTitle) {
    this.title = newTitle;
  }

  /**
   * {@inheritDoc}
   * @see org.apache.shindig.social.opensocial.model.Message#getType()
   */
  public Type getType() {
    return type;
  }

  /**
   * {@inheritDoc}
   * @see org.apache.shindig.social.opensocial.model.Message#setType(org.apache.shindig.social.opensocial.model.Message.Type)
   */
  public void setType(Type newType) {
    this.type = newType;
  }

  /**
   * {@inheritDoc}
   * @see org.apache.shindig.social.opensocial.model.Message#sanitizeHTML(java.lang.String)
   */
  public String sanitizeHTML(String htmlStr) {
    return htmlStr;
  }

  /**
   * {@inheritDoc}
   * @see org.apache.shindig.social.opensocial.jpa.api.DbObject#getObjectId()
   */
  public long getObjectId() {
    return objectId;
  }

  /**
   *
   */
  @PrePersist
  public void populateDbFields() {
    typeDb = type.toString();
  }

  /**
   *
   */
  @PostLoad
  public void loadTransientFields() {
    type = Type.valueOf(typeDb);
  }

  /** {@inheritDoc} */
  public String getAppUrl() {
    // TODO Auto-generated method stub
    return null;
  }

  /** {@inheritDoc} */
  public String getBodyId() {
    // TODO Auto-generated method stub
    return null;
  }

  /** {@inheritDoc} */
  public List<String> getCollectionIds() {
    // TODO Auto-generated method stub
    return null;
  }

  /** {@inheritDoc} */
  public String getId() {
    // TODO Auto-generated method stub
    return null;
  }

  /** {@inheritDoc} */
  public String getInReplyTo() {
    // TODO Auto-generated method stub
    return null;
  }

  /** {@inheritDoc} */
  public List<String> getRecipients() {
    // TODO Auto-generated method stub
    return null;
  }

  /** {@inheritDoc} */
  public List<String> getReplies() {
    // TODO Auto-generated method stub
    return null;
  }

  /** {@inheritDoc} */
  public String getSenderId() {
    // TODO Auto-generated method stub
    return null;
  }

  /** {@inheritDoc} */
  public Status getStatus() {
    // TODO Auto-generated method stub
    return null;
  }

  /** {@inheritDoc} */
  public Date getTimeSent() {
    // TODO Auto-generated method stub
    return null;
  }

  /** {@inheritDoc} */
  public String getTitleId() {
    // TODO Auto-generated method stub
    return null;
  }

  /** {@inheritDoc} */
  public Date getUpdated() {
    // TODO Auto-generated method stub
    return null;
  }

  /** {@inheritDoc} */
  public List<Url> getUrls() {
    // TODO Auto-generated method stub
    return null;
  }

  /** {@inheritDoc} */
  public void setAppUrl(String url) {
    // TODO Auto-generated method stub
  }

  /** {@inheritDoc} */
  public void setBodyId(String bodyId) {
    // TODO Auto-generated method stub
  }

  /** {@inheritDoc} */
  public void setCollectionIds(List<String> collectionIds) {
    // TODO Auto-generated method stub
  }

  /** {@inheritDoc} */
  public void setId(String id) {
    // TODO Auto-generated method stub
  }

  /** {@inheritDoc} */
  public void setInReplyTo(String parentId) {
    // TODO Auto-generated method stub
  }

  /** {@inheritDoc} */
  public void setRecipients(List<String> recipients) {
    // TODO Auto-generated method stub
  }

  /** {@inheritDoc} */
  public void setSenderId(String senderId) {
    // TODO Auto-generated method stub
  }

  /** {@inheritDoc} */
  public void setStatus(Status status) {
    // TODO Auto-generated method stub
  }

  /** {@inheritDoc} */
  public void setTimeSent(Date timeSent) {
    // TODO Auto-generated method stub
  }

  /** {@inheritDoc} */
  public void setTitleId(String titleId) {
    // TODO Auto-generated method stub
  }

  /** {@inheritDoc} */
  public void setUpdated(Date updated) {
    // TODO Auto-generated method stub
  }

  /** {@inheritDoc} */
  public void setUrls(List<Url> urls) {
    // TODO Auto-generated method stub
  }
}
