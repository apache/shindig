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
import org.apache.shindig.social.opensocial.model.Activity;
import org.apache.shindig.social.opensocial.model.MediaItem;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.PostLoad;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;

import java.util.List;

/**
 * Media items are stored in the media_item table, Items may be shared amongst activities and are
 * related to people.
 */
@Entity
@Table(name = "media_item")
public class MediaItemDb implements MediaItem, DbObject {
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

  /**
   * The list of activities which this media item is reference in, this relationship is specified by
   * the java property mediaItems in the class ActivityDb.
   *
   * @see ActivityDb for more information on this mapping.
   */
  @ManyToMany(targetEntity = ActivityDb.class, mappedBy = "mediaItems")
  protected List<Activity> activities;

  /**
   * model field.
   * @see org.apache.shindig.social.opensocial.model.MediaItem
   */
  @Basic
  @Column(name = "mime_type", length = 255)
  private String mimeType;

  /**
   * model field.
   * @see org.apache.shindig.social.opensocial.model.MediaItem
   */
  @Basic
  @Column(name = "media_type")
  private String typeDb;

  /**
   * model field.
   * @see org.apache.shindig.social.opensocial.model.MediaItem
   */
  @Transient
  private Type type;

  /**
   * model field.
   * @see org.apache.shindig.social.opensocial.model.MediaItem
   */
  @Basic
  @Column(name = "url", length = 255)
  private String url;

  /**
   * Create a new blank media item.
   */
  public MediaItemDb() {
  }

  /**
   * Create a media item specifying the mimeType, type and url.
   * @param mimeType the mime type of the media item.
   * @param type the type of the media items (see the specification)
   * @param url the url pointing to the media item.
   */
  public MediaItemDb(String mimeType, Type type, String url) {
    this.mimeType = mimeType;
    this.type = type;
    this.url = url;
  }

  /** 
   * {@inheritDoc}
   * @see org.apache.shindig.social.opensocial.model.MediaItem#getMimeType()
   */
  public String getMimeType() {
    return mimeType;
  }

  /** 
   * {@inheritDoc}
   * @see org.apache.shindig.social.opensocial.model.MediaItem#setMimeType(java.lang.String)
   */
  public void setMimeType(String mimeType) {
    this.mimeType = mimeType;
  }

  /** 
   * {@inheritDoc}
   * @see org.apache.shindig.social.opensocial.model.MediaItem#getType()
   */
  public Type getType() {
    return type;
  }

  /** 
   * {@inheritDoc}
   * @see org.apache.shindig.social.opensocial.model.MediaItem#setType(org.apache.shindig.social.opensocial.model.MediaItem.Type)
   */
  public void setType(Type type) {
    this.type = type;
  }

  /** 
   * {@inheritDoc}
   * @see org.apache.shindig.social.opensocial.model.MediaItem#getUrl()
   */
  public String getUrl() {
    return url;
  }

  /** 
   * {@inheritDoc}
   * @see org.apache.shindig.social.opensocial.model.MediaItem#setUrl(java.lang.String)
   */
  public void setUrl(String url) {
    this.url = url;
  }

  /** 
   * {@inheritDoc}
   * @see org.apache.shindig.social.opensocial.jpa.api.DbObject#getObjectId()
   */
  public long getObjectId() {
    return objectId;
  }

  /**
   * a hook into the pre persist phase of JPA to convert type into the db representation.
   */
  @PrePersist
  public void populateDbFields() {
    typeDb = type.toString();
  }

  /**
   * A hook into the load to convert the type in the Db into the Type Enum.
   */
  @PostLoad
  public void loadTransientFields() {
    type = Type.valueOf(typeDb);
  }
}
