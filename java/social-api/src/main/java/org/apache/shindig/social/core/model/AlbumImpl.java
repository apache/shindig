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
import org.apache.shindig.social.opensocial.model.Album;
import org.apache.shindig.social.opensocial.model.MediaItem;

import java.util.List;

/**
 * Default Implementation of the {@link org.apache.shindig.social.opensocial.model.Album} object in the model.
 *
 * @since 2.0.0
 */
public class AlbumImpl implements Album {
  private String description;
  private String id;
  private Address location;
  private Integer mediaItemCount;
  private List<String> mediaMimeType;
  private List<MediaItem.Type> mediaType;
  private String ownerId;
  private String thumbnailUrl;
  private String title;

  public AlbumImpl() {
  }

  public String getDescription() {
    return description;
  }

  /** {@inheritDoc} */
  public void setDescription(String description) {
    this.description = description;
  }

  public String getId() {
    return id;
  }

  /** {@inheritDoc} */
  public void setId(String id) {
    this.id = id;
  }

  public Address getLocation() {
    return location;
  }

  /** {@inheritDoc} */
  public void setLocation(Address location) {
    this.location = location;
  }

  public Integer getMediaItemCount() {
    return mediaItemCount;
  }

  /** {@inheritDoc} */
  public void setMediaItemCount(Integer mediaItemCount) {
    this.mediaItemCount = mediaItemCount;
  }

  public List<String> getMediaMimeType() {
    return mediaMimeType;
  }

  /** {@inheritDoc} */
  public void setMediaMimeType(List<String> mediaMimeType) {
    this.mediaMimeType = mediaMimeType;
  }

  public List<MediaItem.Type> getMediaType() {
    return mediaType;
  }

  /** {@inheritDoc} */
  public void setMediaType(List<MediaItem.Type> mediaType) {
    this.mediaType = mediaType;
  }

  public String getOwnerId() {
    return ownerId;
  }

  /** {@inheritDoc} */
  public void setOwnerId(String ownerId) {
    this.ownerId = ownerId;
  }

  public String getThumbnailUrl() {
    return thumbnailUrl;
  }

  /** {@inheritDoc} */
  public void setThumbnailUrl(String thumbnailUrl) {
    this.thumbnailUrl = thumbnailUrl;
  }

  public String getTitle() {
    return title;
  }

  /** {@inheritDoc} */
  public void setTitle(String title) {
    this.title = title;
  }
}
