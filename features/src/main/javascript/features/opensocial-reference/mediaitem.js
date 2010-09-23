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
 * @class
 * Represents images, movies, and audio.
 * Create a <code>MediaItem</code> object using the
 * <a href="opensocial.html#newMediaItem">
 * opensocial.newMediaItem()</a> method.
 *
 * @name opensocial.MediaItem
 */

/**
 * Represents images, movies, and audio.
 *
 * @param {string} mimeType The media's type.
 * @param {string} url The media's location.
 * @param {Object.<opensocial.MediaItem.Field, Object>=} opt_params
 *    Any other fields that should be set on the media item object.
 *    All of the defined Fields are supported.
 * @constructor
 * @private
 */
opensocial.MediaItem = function(mimeType, url, opt_params) {
  this.fields_ = {};
  if (opt_params) {
    for (var k in opt_params) {
      if (opt_params.hasOwnProperty(k)) {
        this.fields_[k] = opt_params[k];
      }
    }
  }
  this.fields_[opensocial.MediaItem.Field.MIME_TYPE] = mimeType;
  this.fields_[opensocial.MediaItem.Field.URL] = url;
};


/**
 * @static
 * @class
 * The possible types of media items.
 *
 * <p>
 * <b>See also:</b>
 * <a href="opensocial.MediaItem.Field.html">
 * opensocial.MediaItem.Field</a>
 * </p>
 *
 * @name opensocial.MediaItem.Type
 * @enum {string}
 * @const
 */
opensocial.MediaItem.Type = {
  /** @member opensocial.MediaItem.Type */
  IMAGE: 'image',
  /** @member opensocial.MediaItem.Type */
  VIDEO: 'video',
  /** @member opensocial.MediaItem.Type */
  AUDIO: 'audio'
};


/**
 * @static
 * @class
 * All of the fields that media items have.
 *
 * <p>
 * <b>See also:</b>
 * <a href="opensocial.MediaItem.html#getField">
 * opensocial.MediaItem.getField()</a>
 * </p>
 *
 * @name opensocial.MediaItem.Field
 */
opensocial.MediaItem.Field = {
		
  /**
   * The album to which the media item belongs, specified as a String.
   * @member opensocial.MediaItem.Field
   */
  ALBUM_ID : 'albumId',
 	
  /**
   * The creation time associated with the media item - assigned by container in UTC, specified as a String.
   * @member opensocial.MediaItem.Field
  */
  CREATED : 'created',
  	
  /**
   * The description of the media item, specified as a String.
   * 
   * @member opensocial.MediaItem.Field
   */
  DESCRIPTION : 'description',
  	
  /**
   * An integer specified for audio/video clips - playtime length in seconds, set to -1/not defined if unknown.
   * @member opensocial.MediaItem.Field
   */
  DURATION : 'duration',
  	
  /**
   * A long specified the number of bytes (set to -1/undefined if unknown).
   * @member opensocial.MediaItem.Field
   */
  FILE_SIZE : 'fileSize',
  	
  /**
   * An id associated with the media item, specified as a String.
   * @member opensocial.MediaItem.Field
   */
  ID : 'id',

  /**
   * A language associated with the media item in ISO 639-3 format, specified as a String. 
   * @member opensocial.MediaItem.Field
   */
  LANGUAGE : 'language',

  /**
   * An update time associated with the media item - assigned by container in UTC, specified as a String.
   * @member opensocial.MediaItem.Field
   */
  LAST_UPDATED : 'lastUpdated',

  /**
   * A location corresponding to the media item, specified as a <a href="opensocial.MediaItem.html"> object.
   * @member opensocial.MediaItem.Field
   */
  LOCATION : 'location',
			
  /**
   * The MIME type of media, specified as a String.
   * @member opensocial.MediaItem.Field
   */
  MIME_TYPE: 'mimeType',
  
  /**
   * A number of comments on the photo, specified as a integer.
   * @member opensocial.MediaItem.Field
   */
  NUM_COMMENTS : 'numComments',
  
  /**
   * A number of views for the media item, specified as a integer.
   * @member opensocial.MediaItem.Field
   */
  NUM_VIEWS : 'numViews',
  
  /**
   * A number of votes received for voting, specified as a integer.
   * @member opensocial.MediaItem.Field
   */
  NUM_VOTES : 'numVotes',
  
  /**
   * An average rating of the media item on a scale of 0-10, specified as a integer.
   * @member opensocial.MediaItem.Field
   */
  RATING : 'rating',
  
  /**
   * A string specified for streaming/live content - time when the content is available.
   * @member opensocial.MediaItem.Field
   */
  START_TIME : 'startTime',
  
  /**
   * An array of string (IDs) of people tagged in the media item, specified as an array of Strings.
   * @member opensocial.MediaItem.Field
   */
  TAGGED_PEOPLE : 'taggedPeople',
  
  /**
   * Tags associated with this media item, specified as an array of Strings.
   * @member opensocial.MediaItem.Field
   */
  TAGS : 'tags',
  
  /**
   * URL to a thumbnail image of the media item, specified as a String.
   * @member opensocial.MediaItem.Field
   */
  THUMBNAIL_URL : 'thumbnailUrl',
  
  /**
   * A string describing the media item, specified as a String.
   * @member opensocial.MediaItem.Field
   */
  TITLE : 'title',
	  
  /**
   * The type of media, specified as a
   * <a href="opensocial.MediaItem.Type.html">
   * <code>MediaItem.Type</code></a> object.
   * @member opensocial.MediaItem.Field
   */
  TYPE : 'type',

  /**
   * A string specifying the URL where the media can be found.
   * @member opensocial.MediaItem.Field
   */
  URL: 'url'
};


/**
 * Gets the media item data that's associated with the specified key.
 *
 * @param {string} key The key to get data for; see the
 *   <a href="opensocial.MediaItem.Field.html">Field</a> class
 *   for possible values.
 * @return {string} The data.
 */
opensocial.MediaItem.prototype.getField = function(key, opt_params) {
  return opensocial.Container.getField(this.fields_, key, opt_params);
};


/**
 * Sets data for this media item associated with the given key.
 *
 * @param {string} key The key to set data for.
 * @param {string} data The data to set.
 */
opensocial.MediaItem.prototype.setField = function(key, data) {
  return (this.fields_[key] = data);
};
