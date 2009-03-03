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
package org.apache.shindig.social.opensocial.model;

import org.apache.shindig.protocol.model.Exportablebean;
import org.apache.shindig.social.core.model.MediaItemImpl;

import com.google.inject.ImplementedBy;

/**
 * A container for the media item.
 */
@ImplementedBy(MediaItemImpl.class)
@Exportablebean
public interface MediaItem {

  /**
   * Fields for MediaItem.
   */
  public static enum Field {
    /** the field name for mimeType. */
    MIME_TYPE("mimeType"),
    /** the field name for type. */
    TYPE("type"),
    /** the field name for url. */
    URL("url"),
    /** the thumbnail Url */
    THUMBNAIL_URL("thumbnailUrl");

    /**
     * The field name that the instance represents.
     */
    private final String jsonString;

    /**
     * create a field base on the an element name.
     *
     * @param jsonString the name of the element
     */
    private Field(String jsonString) {
      this.jsonString = jsonString;
    }

    /**
     * @return a string representation of the enum.
     */
    @Override
    public String toString() {
      return this.jsonString;
    }
  }

  /**
   * An enumeration of potential media types.
   */
  public enum Type {
    /** the constant for audio types. */
    AUDIO("audio"),
    /** the constant for image types. */
    IMAGE("image"),
    /** the constant for video types. */
    VIDEO("video");

    /**
     * The field type.
     */
    private final String jsonString;

    /**
     * Construct a field type based on the name.
     *
     * @param jsonString
     */
    private Type(String jsonString) {
      this.jsonString = jsonString;
    }

    /**
     * @return a string representation of the enum.
     */
    @Override
    public String toString() {
      return this.jsonString;
    }
  }

  /**
   * Get the mime type for this Media item.
   *
   * @return the mime type.
   */
  String getMimeType();

  /**
   * Set the mimetype for this Media Item.
   *
   * @param mimeType the mimeType
   */
  void setMimeType(String mimeType);

  /**
   * Get the Type of this media item, either audio, image or video.
   *
   * @return the Type of this media item
   */
  Type getType();

  /**
   * Get the Type of this media item, either audio, image or video.
   *
   * @param type the type of this media item
   */
  void setType(Type type);

  /**
   * Get a URL for the media item.
   *
   * @return the url of the media item
   */
  String getUrl();

  /**
   * Set a URL for the media item.
   *
   * @param url the media item URL
   */
  void setUrl(String url);

  /**
   * Get the thumbnail URL for the media item.
   *
   * @return the thumbnail url of the media item
   */
  String getThumbnailUrl();

  /**
   * Set a thumbnail URL for the media item.
   *
   * @param url the thumbnail URL of the MediaItem
   */
  void setThumbnailUrl(String url);
}
