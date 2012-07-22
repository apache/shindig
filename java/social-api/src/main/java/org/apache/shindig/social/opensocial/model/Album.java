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
package org.apache.shindig.social.opensocial.model;

import org.apache.shindig.protocol.model.Exportablebean;
import org.apache.shindig.social.core.model.AlbumImpl;

import com.google.inject.ImplementedBy;

import java.util.List;

/**
 * <p>
 * The Album API describes the collection of MediaItems of images, movies, and audio.
 * </p>
 * <p>
 * Please see <a href="http://www.opensocial.org/Technical-Resources/opensocial-spec-v09/OpenSocial-Specification.html#opensocial.Album.Field">
 * http://www.opensocial.org/Technical-Resources/opensocial-spec-v09/OpenSocial-Specification.html#opensocial.Album.Field</a>
 * for details about the supported fields.
 * </p>
 *
 * @since 2.0.0
 */
@ImplementedBy(AlbumImpl.class)
@Exportablebean
public interface Album {

  /**
   * The fields that represent the Album object in json form.
   */
  public static enum Field {
    DESCRIPTION("description"),
    ID("id"),
    LOCATION("location"),
    MEDIA_ITEM_COUNT("mediaItemCount"),
    MEDIA_MIME_TYPE("mediaMimeType"),
    MEDIA_TYPE("mediaType"),
    OWNER_ID("ownerId"),
    THUMBNAIL_URL("thumbnailUrl"),
    TITLE("title");

    /**
     * The json field that the instance represents.
     */
    private final String jsonString;

    /**
     * create a field base on the a json element.
     *
     * @param jsonString the name of the element
     */
    private Field(String jsonString) {
      this.jsonString = jsonString;
    }

    /**
     * emit the field as a json element.
     *
     * @return the field name
     */
    @Override
    public String toString() {
      return this.jsonString;
    }
  }

  /**
   * Get a string specifying the description of this album.
   *
   * @return a string specifying the description of this album.
   */
  String getDescription();

  /**
   * Set the description of this album.
   *
   * @param description a string specifying the description of this album.
   */
  void setDescription(String description);

  /**
   * Get a string ID specifying the unique identifier of this album.
   *
   * @return a string ID specifying the unique identifier of this album.
   */
  String getId();

  /**
   * Set a string ID specifying a unique identifier of this album.
   *
   * @param id a string ID specifying the unique identifier of this album.
   */
  void setId(String id);

  /**
   * Get address location of this album.
   *
   * @return an Address specifying the location of this album.
   */
  Address getLocation();

  /**
   * Set the address location of this album.
   *
   * @param location an Address specifying the location of this album.
   */
  void setLocation(Address location);

  /**
   * Get the number of items in the album.
   *
   * @return an integer specifying the number of items in the album
   */
  Integer getMediaItemCount();

  /**
   * Set the number of items in the album.
   *
   * @param mediaItemCount an integer specifying the number of items in the album.
   */
  void setMediaItemCount(Integer mediaItemCount);

  /**
   * Get the identifying mime-types of the items in the album.
   *
   * @return a List of strings specifying the mime-types of the items in the album.
   */
  List<String> getMediaMimeType();

  /**
   * Set the identifying mime-types of the items in the album.
   *
   * @param mediaMimeType a List of strings specifying the mime-types of the items in the album.
   */
  void setMediaMimeType(List<String> mediaMimeType);

  /**
   * Get the list of media item types for the items in the album.
   *
   * @return a List of MediaItem.Type specifying the media item types for items in the album.
   */
  List<MediaItem.Type> getMediaType();

  /**
   * Set the list of media item types for the items in the album.
   *
   * @param mediaType List of MediaItem.Type specifying media item types for items in the album.
   */
  void setMediaType(List<MediaItem.Type> mediaType);

  /**
   * Get the ID of the owner of the album.
   *
   * @return a string identifying the ID of the owner of the album.
   */
  String getOwnerId();

  /**
   * Set the string ID of the owner of the album.
   *
   * @param ownerId a string ID that identify the owner of the album.
   */
  void setOwnerId(String ownerId);

  /**
   * Get the URL to the thumbnail cover image for the album.
   *
   * @return a string specifying the URL for thumbnail cover image of the album.
   */
  String getThumbnailUrl();

  /**
   * Set the URL of the thumbnail cover image for the album.
   *
   * @param thumbnailUrl a string specifying the URL for thumbnail cover image of the album.
   */
  void setThumbnailUrl(String thumbnailUrl);

  /**
   * Get the title of the album.
   *
   * @return a string specifying the tile of the album.
   */
  String getTitle();

  /**
   * Set the title of the album.
   *
   * @param title a string specifying the title of the album.
   */
  void setTitle(String title);
}
