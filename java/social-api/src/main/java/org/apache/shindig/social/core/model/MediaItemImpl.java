/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.shindig.social.core.model;

import org.apache.shindig.social.opensocial.model.Address;
import org.apache.shindig.social.opensocial.model.MediaItem;

/**
 * see
 * <a href="http://www.opensocial.org/Technical-Resources/opensocial-spec-v09/OpenSocial-Specification.html#opensocial.MediaItem.Field">
 * opensocial.MediaItem.Field</a>
 */
public class MediaItemImpl implements MediaItem {
  private String albumId;
  private String created;
  private String description;
  private String duration;
  private String fileSize;
  private String id;
  private String language;
  private String lastUpdated;
  private Address location;
  private String mimeType;
  private String numComments;
  private String numViews;
  private String numVotes;
  private String rating;
  private String startTime;
  private String taggedPeople;
  private String tags;
  private String thumbnailUrl;
  private String title;
  private Type type;
  private String url;

  public MediaItemImpl() {
  }

  public MediaItemImpl(String mimeType, Type type, String url) {
    this.mimeType = mimeType;
    this.type = type;
    this.url = url;
  }

  public String getMimeType() {
    return mimeType;
  }

  /** {@inheritDoc} */
  public void setMimeType(String mimeType) {
    this.mimeType = mimeType;
  }

  public Type getType() {
    return type;
  }

  public void setType(Type type) {
    this.type = type;
  }

  public String getUrl() {
    return url;
  }

  /** {@inheritDoc} */
  public void setUrl(String url) {
    this.url = url;
  }

  public String getThumbnailUrl() {
    return this.thumbnailUrl;
  }

  /** {@inheritDoc} */
  public void setThumbnailUrl(String url) {
    this.thumbnailUrl = url;
  }

  public String getAlbumId() {
    return albumId;
  }

  /** {@inheritDoc} */
  public void setAlbumId(String albumId) {
    this.albumId = albumId;
  }

  public String getCreated() {
    return created;
  }

  /** {@inheritDoc} */
  public void setCreated(String created) {
    this.created = created;
  }

  public String getDescription() {
    return description;
  }

  /** {@inheritDoc} */
  public void setDescription(String description) {
    this.description = description;
  }

  public String getDuration() {
    return duration;
  }

  /** {@inheritDoc} */
  public void setDuration(String duration) {
    this.duration = duration;
  }

  public String getFileSize() {
    return fileSize;
  }

  /** {@inheritDoc} */
  public void setFileSize(String fileSize) {
    this.fileSize = fileSize;
  }

  public String getId() {
    return id;
  }

  /** {@inheritDoc} */
  public void setId(String id) {
    this.id = id;
  }

  public String getLanguage() {
    return language;
  }

  /** {@inheritDoc} */
  public void setLanguage(String language) {
    this.language = language;
  }

  public String getLastUpdated() {
    return lastUpdated;
  }

  /** {@inheritDoc} */
  public void setLastUpdated(String lastUpdated) {
    this.lastUpdated = lastUpdated;
  }

  public Address getLocation() {
    return location;
  }

  /** {@inheritDoc} */
  public void setLocation(Address location) {
    this.location = location;
  }

  public String getNumComments() {
    return numComments;
  }

  /** {@inheritDoc} */
  public void setNumComments(String numComments) {
    this.numComments = numComments;
  }

  public String getNumViews() {
    return numViews;
  }

  /** {@inheritDoc} */
  public void setNumViews(String numViews) {
    this.numViews = numViews;
  }

  public String getNumVotes() {
    return numVotes;
  }

  /** {@inheritDoc} */
  public void setNumVotes(String numVotes) {
    this.numVotes = numVotes;
  }

  public String getRating() {
    return rating;
  }

  /** {@inheritDoc} */
  public void setRating(String rating) {
    this.rating = rating;
  }

  public String getStartTime() {
    return startTime;
  }

  /** {@inheritDoc} */
  public void setStartTime(String startTime) {
    this.startTime = startTime;
  }

  public String getTaggedPeople() {
    return taggedPeople;
  }

  /** {@inheritDoc} */
  public void setTaggedPeople(String taggedPeople) {
    this.taggedPeople = taggedPeople;
  }

  public String getTags() {
    return tags;
  }

  /** {@inheritDoc} */
  public void setTags(String tags) {
    this.tags = tags;
  }

  public String getTitle() {
    return title;
  }
  /** {@inheritDoc} */
  public void setTitle(String title) {
    this.title = title;
  }
}
