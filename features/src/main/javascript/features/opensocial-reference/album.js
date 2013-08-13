/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/*global opensocial */

/**
 * @fileoverview Representation of an album.
 */

/**
 * @class
 * Represents collection of media item images, movies, and audio.
 * Create a <code>Album</code> object using the <a href="opensocial.html#newAlbum">
 * opensocial.newAlbum()</a> method.
 *
 * @name opensocial.Album
 */

/**
 * Base interface for collection of media items.
 *
 * @param {Object.<opensocial.Album.Field, Object>=} opt_params
 *    Any other fields that should be set on the message object.
 *    All of the defined Fields are supported.
 * @private
 * @constructor
 * @deprecated since 1.0 see http://opensocial-resources.googlecode.com/svn/spec/1.0/Social-Gadget.xml#rfc.section.A.6.
 */
opensocial.Album = function(opt_params) {
  this.fields_ = opt_params || {};
};

/**
 * @static
 * @class
 * All of the fields that an Album can have.
 *
 * @name opensocial.Album.Field
 * @deprecated since 1.0 see http://opensocial-resources.googlecode.com/svn/spec/1.0/Social-Gadget.xml#rfc.section.A.7.
 */
opensocial.Album.Field = {
  /**
   * A description of the album.
   *
   * @member opensocial.Album.Field
   */
  DESCRIPTION: 'description',

  /**
   * A unique identifier for the album.
   *
   * @member opensocial.Album.Field
   */
  ID: 'id',

  /**
   * A location corresponding to the album as opensocial.Address.
   *
   * @member opensocial.Album.Field
   */
  LOCATION: 'location',

  /**
   * The number of items in the album.
   *
   * @member opensocial.Album.Field
   */
  MEDIA_ITEM_COUNT: 'mediaItemCount',


  /**
   * The types of MediaItems in the Album.
   *
   * @member opensocial.Album.Field
   */
  MEDIA_MIME_TYPE: 'mediaMimeType',


  /**
   * The types of MediaItems in the album.
   *
   * @member opensocial.Album.Field
   */
  MEDIA_TYPE: 'mediaType',


  /**
   * The string ID of the owner of the album.
   *
   * @member opensocial.Album.Field
   */
  OWNER_ID: 'ownerId',

  /**
   * URL to a thumbnail cover of the album as string.
   *
   * @member opensocial.Album.Field
   */
  THUMBNAIL_URL: 'thumbnailUrl',

  /**
   * The title of the album.
   *
   * @member opensocial.Album.Field
   */
  TITLE: 'title'
};

/**
 * Gets the album data that's associated with the specified key.
 *
 * @param {string} key The key to get data for;
 *   see the <a href="opensocial.Album.Field.html">Field</a> class
 * for possible values.
 * @param {Object.<opensocial.DataRequest.DataRequestFields, Object>}
 *  opt_params Additional
 *    <a href="opensocial.DataRequest.DataRequestFields.html">params</a>
 *    to pass to the request.
 * @return {string} The data.
 * @member opensocial.Album
 * @deprecated since 1.0 see http://opensocial-resources.googlecode.com/svn/spec/1.0/Social-Gadget.xml#rfc.section.A.6.1.1.
 */
opensocial.Album.prototype.getField = function(key, opt_params) {
  return opensocial.Container.getField(this.fields_, key, opt_params);
};


/**
 * Sets data for this album associated with the given key.
 *
 * @param {string} key The key to set data for.
 * @param {string} data The data to set.
 * @deprecated since 1.0 see http://opensocial-resources.googlecode.com/svn/spec/1.0/Social-Gadget.xml#rfc.section.A.6.1.2.
 */
opensocial.Album.prototype.setField = function(key, data) {
  return this.fields_[key] = data;
};
