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

import static javax.persistence.CascadeType.MERGE;
import static javax.persistence.CascadeType.PERSIST;
import static javax.persistence.CascadeType.REFRESH;
import static javax.persistence.GenerationType.IDENTITY;

import org.apache.shindig.social.opensocial.jpa.api.DbObject;
import org.apache.shindig.social.opensocial.model.Activity;
import org.apache.shindig.social.opensocial.model.Address;
import org.apache.shindig.social.opensocial.model.MediaItem;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
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
   * model field
   * @see org.apache.shindig.social.opensocial.model.MediaItem
   */
  @Basic
  @Column(name = "thumbnail_url", length = 255)
  private String thumbnailUrl;

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
   * model field.
   * @see org.apache.shindig.social.opensocial.model.MediaItem
   */
  @Basic
  @Column(name = "album_id")
  private String albumId;

  /**
   * model field.
   * @see org.apache.shindig.social.opensocial.model.MediaItem
   */
  @Basic
  @Column(name = "created")
  private String created;

  /**
   * model field.
   * @see org.apache.shindig.social.opensocial.model.MediaItem
   */
  @Basic
  @Column(name = "description")
  private String description;

  /**
   * model field.
   * @see org.apache.shindig.social.opensocial.model.MediaItem
   */
  @Basic
  @Column(name = "title")
  private String title;


  /**
   * model field.
   * @see org.apache.shindig.social.opensocial.model.MediaItem
   */
  @Basic
  @Column(name = "duration")
  private String duration;

  /**
   * model field.
   * @see org.apache.shindig.social.opensocial.model.MediaItem
   */
  @Basic
  @Column(name = "file_size")
  private String fileSize;

  /**
   * model field.
   * @see org.apache.shindig.social.opensocial.model.MediaItem
   */
  @Basic
  @Column(name = "id")
  private String id;


  /**
   * model field.
   * @see org.apache.shindig.social.opensocial.model.MediaItem
   */
  @Basic
  @Column(name = "language")
  private String language;

  /**
   * model field.
   * @see org.apache.shindig.social.opensocial.model.MediaItem
   */
  @Basic
  @Column(name = "last_updated")
  private String lastUpdated;

  /**
   * model field.
   * @see org.apache.shindig.social.opensocial.model.MediaItem
   */
  @ManyToOne(targetEntity = AddressDb.class, cascade = { PERSIST, MERGE, REFRESH })
  @JoinColumn(name = "address_id", referencedColumnName = "oid")
  private Address location;

  /**
   * model field.
   * @see org.apache.shindig.social.opensocial.model.MediaItem
   */
  @Basic
  @Column(name = "num_comments")
  private String numComments;


  /**
   * model field.
   * @see org.apache.shindig.social.opensocial.model.MediaItem
   */
  @Basic
  @Column(name = "num_views")
  private String numViews;

  /**
   * model field.
   * @see org.apache.shindig.social.opensocial.model.MediaItem
   */
  @Basic
  @Column(name = "num_votes")
  private String numVotes;

  /**
   * model field.
   * @see org.apache.shindig.social.opensocial.model.MediaItem
   */
  @Basic
  @Column(name = "rating")
  private String rating;


  /**
   * model field.
   * @see org.apache.shindig.social.opensocial.model.MediaItem
   */
  @Basic
  @Column(name = "start_time")
  private String startTime;

  /**
   * model field.
   * @see org.apache.shindig.social.opensocial.model.MediaItem
   */
  @Basic
  @Column(name = "tagged_people")
  private String taggedPeople;

  /**
   * model field.
   * @see org.apache.shindig.social.opensocial.model.MediaItem
   */
  @Basic
  @Column(name = "tags")
  private String tags;

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
   * @see org.apache.shindig.social.opensocial.model.MediaItem#getThumbnailUrl()
   */
  public String getThumbnailUrl() {
    return thumbnailUrl;
  }

  /**
   * {@inheritDoc}
   * @see org.apache.shindig.social.opensocial.model.MediaItem#setThumbnailUrl(java.lang.String)
   */
  public void setThumbnailUrl(String url) {
    this.thumbnailUrl = url;
  }

  /**
   * {@inheritDoc}
   */
  public String getAlbumId() {
    return albumId;
  }

  /**
   * {@inheritDoc}
   */
  public void setAlbumId(String albumId) {
    this.albumId = albumId;
  }

  /**
   * {@inheritDoc}
   */
  public String getCreated() {
    return created;
  }

  /**
   * {@inheritDoc}
   */
  public void setCreated(String created) {
    this.created = created;
  }

  /**
   * {@inheritDoc}
   */
  public String getDescription() {
    return description;
  }

  /**
   * {@inheritDoc}
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * {@inheritDoc}
   */
  public String getDuration() {
    return duration;
  }

  /**
   * {@inheritDoc}
   */
  public void setDuration(String duration) {
    this.duration = duration;
  }

  /**
   * {@inheritDoc}
   */
  public String getFileSize() {
    return fileSize;
  }

  /**
   * {@inheritDoc}
   */
  public void setFileSize(String fileSize) {
    this.fileSize = fileSize;
  }

  /**
   * {@inheritDoc}
   */
  public String getId() {
    return id;
  }

  /**
   * {@inheritDoc}
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * {@inheritDoc}
   */
  public String getLanguage() {
    return language;
  }

  /**
   * {@inheritDoc}
   */
  public void setLanguage(String language) {
    this.language = language;
  }

  /**
   * {@inheritDoc}
   */
  public String getLastUpdated() {
    return lastUpdated;
  }

  public void setLastUpdated(String lastUpdated) {
    this.lastUpdated = lastUpdated;
  }

  /**
   * {@inheritDoc}
   */
  public Address getLocation() {
    return location;
  }

  /**
   * {@inheritDoc}
   */
  public void setLocation(Address location) {
    this.location = location;
  }

  /**
   * {@inheritDoc}
   */
  public String getNumComments() {
    return numComments;
  }

  /**
   * {@inheritDoc}
   */
  public void setNumComments(String numComments) {
    this.numComments = numComments;
  }

  /**
   * {@inheritDoc}
   */
  public String getNumViews() {
    return numViews;
  }

  /**
   * {@inheritDoc}
   */
  public void setNumViews(String numViews) {
    this.numViews = numViews;
  }

  /**
   * {@inheritDoc}
   */
  public String getNumVotes() {
    return numVotes;
  }

  /**
   * {@inheritDoc}
   */
  public void setNumVotes(String numVotes) {
    this.numVotes = numVotes;
  }

  /**
   * {@inheritDoc}
   */
  public String getRating() {
    return rating;
  }

  /**
   * {@inheritDoc}
   */
  public void setRating(String rating) {
    this.rating = rating;
  }

  /**
   * {@inheritDoc}
   */
  public String getStartTime() {
    return startTime;
  }

  /**
   * {@inheritDoc}
   */
  public void setStartTime(String startTime) {
    this.startTime = startTime;
  }

  /**
   * {@inheritDoc}
   */
  public String getTaggedPeople() {
    return taggedPeople;
  }

  /**
   * {@inheritDoc}
   */
  public void setTaggedPeople(String taggedPeople) {
    this.taggedPeople = taggedPeople;
  }

  /**
   * {@inheritDoc}
   */
  public String getTags() {
    return tags;
  }

  /**
   * {@inheritDoc}
   */
  public void setTags(String tags) {
    this.tags = tags;
  }

  /**
   * {@inheritDoc}
   */
  public String getTitle() {
    return title;
  }

  /**
   * {@inheritDoc}
   */
  public void setTitle(String title) {
    this.title = title;
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
