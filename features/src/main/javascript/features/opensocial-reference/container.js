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

/**
 * @fileoverview Interface for containers of people functionality.
 */


/**
 * Base interface for all containers.
 *
 * @constructor
 * @private
 */
opensocial.Container = function() {};


/**
 * The container instance.
 *
 * @type {Container}
 * @private
 */
opensocial.Container.container_ = null;


/**
 * Set the current container object.
 *
 * @param {opensocial.Container} container The container.
 * @private
 */
opensocial.Container.setContainer = function(container) {
  opensocial.Container.container_ = container;
};


/**
 * Get the current container object.
 *
 * @return {opensocial.Container} container The current container.
 * @private
 */
opensocial.Container.get = function() {
  return opensocial.Container.container_;
};


/**
 * Gets the current environment for this gadget. You can use the environment to
 * query things like what profile fields and surfaces are supported by this
 * container, what parameters were passed to the current gadget and so forth.
 *
 * @return {opensocial.Environment} The current environment.
 *
 * @private
 */
opensocial.Container.prototype.getEnvironment = function() {};

/**
 * Requests the container to send a specific message to the specified users. If
 * the container does not support this method the callback will be called with a
 * opensocial.ResponseItem. The response item will have its error code set to
 * NOT_IMPLEMENTED.
 *
 * @param {Array.<string> | string} recipients An ID, array of IDs, or a
 *     group reference; the supported keys are VIEWER, OWNER, VIEWER_FRIENDS,
 *    OWNER_FRIENDS, or a single ID within one of those groups.
 * @param {opensocial.Message} message The message to send to the specified
 *     users.
 * @param {function(opensocial.ResponseItem)=} opt_callback The function to call once the request has been
 *    processed; either this callback will be called or the gadget will be
 *    reloaded from scratch. This function will be passed one parameter, an
 *    opensocial.ResponseItem. The error code will be set to reflect whether
 *    there were any problems with the request. If there was no error, the
 *    message was sent. If there was an error, you can use the response item's
 *    getErrorCode method to determine how to proceed. The data on the response
 *    item will not be set.
 * @param {Object=} opt_params TODO.
 *
 * @member opensocial
 * @private
 */
opensocial.Container.prototype.requestSendMessage = function(recipients,
    message, opt_callback, opt_params) {
  gadgets.rpc.call(null, 'requestSendMessage', opt_callback, recipients,
      message.toJsonObject(), opt_callback, opt_params);
};


/**
 * Requests the container to share this gadget with the specified users. If the
 * container does not support this method the callback will be called with a
 * opensocial.ResponseItem. The response item will have its error code set to
 * NOT_IMPLEMENTED.
 *
 * @param {Array.<string> | string} recipients An ID, array of IDs, or a
 *     group reference; the supported keys are VIEWER, OWNER, VIEWER_FRIENDS,
 *    OWNER_FRIENDS, or a single ID within one of those groups.
 * @param {opensocial.Message} reason The reason the user wants the gadget to
 *     share itself. This reason can be used by the container when prompting the
 *     user for permission to share the app. It may also be ignored.
 * @param {function(opensocial.ResponseItem)=} opt_callback The function to call once the request has been
 *    processed; either this callback will be called or the gadget will be
 *    reloaded from scratch. This function will be passed one parameter, an
 *    opensocial.ResponseItem. The error code will be set to reflect whether
 *    there were any problems with the request. If there was no error, the
 *    sharing request was sent. If there was an error, you can use the response
 *    item's getErrorCode method to determine how to proceed. The data on the
 *    response item will not be set.
 * @param {Object=} opt_params TODO.
 *
 * @member opensocial
 * @private
 */
opensocial.Container.prototype.requestShareApp = function(recipients, reason,
    opt_callback, opt_params) {
  if (opt_callback) {
    window.setTimeout(function() {
      opt_callback(new opensocial.ResponseItem(
          null, null, opensocial.ResponseItem.Error.NOT_IMPLEMENTED, null));
    }, 0);
  }
};


/**
 * Request for the container to make the specified person not a friend.
 *
 * Note: If this is the first activity that has been created for the user and
 * the request is marked as HIGH priority then this call may open a user flow
 * and navigate away from your gadget.
 *
 * @param {Activity} activity The activity to create. The only required field is
 *     title.
 * @param {CreateActivityPriority} priority The priority for this request.
 * @param {function(opensocial.ResponseItem)=} opt_callback Function to call once the request has been
 *    processed.
 * @private
 */
opensocial.Container.prototype.requestCreateActivity = function(activity,
    priority, opt_callback) {
  if (opt_callback) {
    window.setTimeout(function() {
      opt_callback(new opensocial.ResponseItem(
          null, null, opensocial.ResponseItem.Error.NOT_IMPLEMENTED, null));
    }, 0);
  }
};


/**
 * Returns whether the current gadget has access to the specified
 * permission.
 *
 * @param {opensocial.Permission | string} permission The permission.
 * @return {boolean} Whether the gadget has access for the permission.
 *
 * @private
 */
opensocial.Container.prototype.hasPermission = function(permission) {
  return false;
};


/**
 * Requests the user grants access to the specified permissions.
 *
 * @param {Array.<opensocial.Permission>} permissions The permissions to request
 *    access to from the viewer.
 * @param {string} reason Will be displayed to the user as the reason why these
 *    permissions are needed.
 * @param {function(opensocial.ResponseItem)=} opt_callback The function to call once the request has been
 *    processed. This callback will either be called or the gadget will be
 *    reloaded from scratch.
 *
 * @private
 */
opensocial.Container.prototype.requestPermission = function(permissions, reason,
    opt_callback) {
  if (opt_callback) {
    window.setTimeout(function() {
      opt_callback(new opensocial.ResponseItem(
          null, null, opensocial.ResponseItem.Error.NOT_IMPLEMENTED, null));
    }, 0);
  }
};


/**
 * Calls the callback function with a dataResponse object containing the data
 * asked for in the dataRequest object.
 *
 * @param {opensocial.DataRequest} dataRequest Specifies which data to get from
 *    the server.
 * @param {function(opensocial.ResponseItem)} callback Function to call after the data is fetched.
 * @private
 */
opensocial.Container.prototype.requestData = function(dataRequest, callback) {};


/**
 * Creates a new album and returns the ID of the album created.
 *
 * @param {opensocial.IdSpec} idSpec The ID of the used to specify which people/groups
 *   to create an album for.
 * @param {opensocial.Album} album The album to create.
 * @return {Object} A request object.
 * @private
 */
opensocial.Container.prototype.newCreateAlbumRequest = function(idSpec, album) {};

/**
 * Creates a new media item in the album and returns the ID of the album created.
 *
 * @param {opensocial.IdSpec} idSpec The ID of the used to specify which people/groups
 *   to create an album for.
 * @param {string} albumId The ID of album to add the media item to.
 * @param {openSocial.MediaItem} mediaItem The media item instance to add to the album.
 * @return {Object} A request object.
 * @private
 */
opensocial.Container.prototype.newCreateMediaItemRequest = function(idSpec, albumId,
    mediaItem) {};

/**
 * Deletes the album specified.
 *
 * @param {opensocial.IdSpec} idSpec The ID of the used to specify which people/groups
 *   to create an album for.
 * @param {string} albumId The ID of the album to create.
 * @return {Object} A request object.
 * @private
 */
opensocial.Container.prototype.newDeleteAlbumRequest = function(idSpec, albumId) {};

/**
 * Request a profile for the specified person id.
 * When processed, returns a Person object.
 *
 * @param {string} id The id of the person to fetch. Can also be standard
 *    person IDs of VIEWER and OWNER.
 * @param {Object.<opensocial.DataRequest.PeopleRequestFields, Object>=} opt_params
 *    Additional params to pass to the request. This request supports
 *    PROFILE_DETAILS.
 * @return {Object} a request object.
 * @private
 */
opensocial.Container.prototype.newFetchPersonRequest = function(id,
    opt_params) {};


/**
 * Used to request friends from the server.
 * When processed, returns a Collection&lt;Person&gt; object.
 *
 * @param {opensocial.IdSpec} idSpec An IdSpec used to specify which people to
 *     fetch. See also <a href="opensocial.IdSpec.html">IdSpec</a>.
 * @param {Object.<opensocial.DataRequest.PeopleRequestFields, Object>=} opt_params
 *    Additional params to pass to the request. This request supports
 *    PROFILE_DETAILS, SORT_ORDER, FILTER, FILTER_OPTIONS, FIRST, and MAX.
 * @return {Object} a request object.
 * @private
 */
opensocial.Container.prototype.newFetchPeopleRequest = function(idSpec,
    opt_params) {};


/**
 * Used to request app data for the given people.
 * When processed, returns a Map&lt;person id, Map&lt;String, String&gt;&gt;
 * object.TODO: All of the data values returned will be valid json.
 *
 * @param {opensocial.IdSpec} idSpec An IdSpec used to specify which people to
 *     fetch. See also <a href="opensocial.IdSpec.html">IdSpec</a>.
 * @param {Array.<string> | string} keys The keys you want data for. This
 *     can be an array of key names, a single key name, or "*" to mean
 *     "all keys".
 * @param {Object.<opensocial.DataRequest.DataRequestFields, Object>}
 *  opt_params Additional
 *    <a href="opensocial.DataRequest.DataRequestFields.html">params</a>
 *    to pass to the request.
 * @return {Object} a request object.
 * @private
 */
opensocial.Container.prototype.newFetchPersonAppDataRequest = function(idSpec,
    keys, opt_params) {};


/**
 * Creates an item to request an update of an app field for the current VIEWER
 * When processed, does not return any data.
 * App Data is stored as a series of key value pairs of strings, scoped per
 * person, per application. Containers supporting this request SHOULD provide
 * at least 10KB of space per user per application for this data.
 *
 * @param {string} key The name of the key.
 * @param {string} value The value.
 * @return {Object} a request object.
 * @private
 */
opensocial.Container.prototype.newUpdatePersonAppDataRequest = function(
    key, value) {};


/**
 * Deletes the given keys from the datastore for the current VIEWER.
 * When processed, does not return any data.
 *
 * @param {Array.<string> | string} keys The keys you want to delete from
 *     the datastore; this can be an array of key names, a single key name,
 *     or "*" to mean "all keys".
 * @return {Object} A request object.
 * @private
 */
opensocial.Container.prototype.newRemovePersonAppDataRequest = function(
    keys) {};

/**
 * Updates the fields for an album specified in the params.
 * The following fields cannot be set: MEDIA_ITEM_COUNT, OWNER_ID, ID.
 *
 * @param {opensocial.IdSpec} idSpec An IdSpec used to specify which people/groups
 *    to own the album.
 * @param {string} albumId The ID of album to update.
 * @param {Object.<opensocial.Album.Field, Object>=} fields The album fields to update.
 * @return {Object} A request object.
 */
opensocial.Container.prototype.newUpdateAlbumRequest = function(idSpec, albumId, fields) {};

/**
 * Updates the fields for a media item specified in the params.
 * The following fields cannot be set: ID, CREATED, ALBUM_ID, FILE_SIZE, NUM_COMMENTS.
 *
 * @param {opensocial.IdSpec} idSpec An IdSpec used to specify which people/groups
 *    own the album/media item.
 * @param {string} albumId The ID of the album containing the media item to update.
 * @param {string} mediaItemId ID of media item to update.
 * @param {Object.<opensocial.MediaItem.Field, Object>=} fields The media item fields to update.
 * @return {Object} A request object.
 */
opensocial.Container.prototype.newUpdateMediaItemRequest = function(idSpec, albumId,
    mediaItemId, fields) {};

/**
 * Used to request an activity stream from the server.
 *
 * When processed, returns a Collection&lt;Activity&gt;.
 *
 * @param {opensocial.IdSpec} idSpec An IdSpec used to specify which people to
 *     fetch. See also <a href="opensocial.IdSpec.html">IdSpec</a>.
 * @param {Object.<opensocial.DataRequest.ActivityRequestFields, Object>=} opt_params
 *    Additional params to pass to the request.
 * @return {Object} a request object.
 * @private
 */
opensocial.Container.prototype.newFetchActivitiesRequest = function(idSpec,
    opt_params) {};

opensocial.Container.prototype.newFetchAlbumsRequest = function(idSpec, opt_params) {};

/**
 * Creates an item to request media items from the container.
 *
 * @param {opensocial.IdSpec}
 *          idSpec An IdSpec used to specify which media items to fetch.
 * @param {string}
 *          albumId The id of the album to fetch MediaItems from.
 * @param {Object.<Object, Object>=} opt_params Additional parameters to pass to the request.
 * @return {Object} A request object.
 */
opensocial.Container.prototype.newFetchMediaItemsRequest = function(idSpec, opt_params) {};

opensocial.Container.prototype.newFetchMessageCollectionsRequest = function(idSpec, opt_params) {};
opensocial.Container.prototype.newFetchMessagesRequest = function(idSpec, msgCollId, opt_params) {};

/**
 * Creates a new collection with caja support if enabled.
 * @return {opensocial.Collection} the collection object.
 * @private
 */
opensocial.Container.prototype.newCollection = function(array, opt_offset,
    opt_totalSize) {
  return new opensocial.Collection(array, opt_offset, opt_totalSize);
};


/**
 * Creates a new person with caja support if enabled.
 * @return {opensocial.Person} the person object.
 * @private
 */
opensocial.Container.prototype.newPerson = function(opt_params, opt_isOwner,
    opt_isViewer) {
  return new opensocial.Person(opt_params, opt_isOwner, opt_isViewer);
};


/**
 * Get an activity object used to create activities on the server
 *
 * @param {Object.<opensocial.Activity.Field, Object>=} opt_params Any other
 *    fields that should be set on the activity object. All of the defined
 *    Fields are supported.
 * @return {opensocial.Activity} the activity object.
 * @private
 */
opensocial.Container.prototype.newActivity = function(opt_params) {
  return new opensocial.Activity(opt_params);
};

/**
 * Get a collection of images, movies, and audio.
 * Used when creating albums on the server.
 *
 * @param {Object.<opensocial.MediaItem.Field, Object>=} opt_params
 *    Any other fields that should be set on the album object;
 *    all of the defined
 *    <a href="opensocial.Album.Field.html">Field</a>s
 *    are supported.
 *
 * @return {opensocial.Album} the album object.
 * @private
 */
opensocial.Container.prototype.newAlbum = function(opt_params) {
  return new opensocial.Album(opt_params);
};


/**
 * Creates a media item. Represents images, movies, and audio.
 * Used when creating activities on the server.
 *
 * @param {string} mimeType of the media.
 * @param {string} url where the media can be found.
 * @param {Object.<opensocial.MediaItem.Field, Object>=} opt_params
 *    Any other fields that should be set on the media item object.
 *    All of the defined Fields are supported.
 *
 * @return {opensocial.MediaItem} the media item object.
 * @private
 */
opensocial.Container.prototype.newMediaItem = function(mimeType, url,
    opt_params) {
  return new opensocial.MediaItem(mimeType, url, opt_params);
};


/**
 * Creates a media item associated with an activity.
 * Represents images, movies, and audio.
 * Used when creating activities on the server.
 *
 * @param {string} body The main text of the message.
 * @param {Object.<opensocial.Message.Field, Object>=} opt_params
 *    Any other fields that should be set on the message object;
 *    all of the defined
 *    <a href="opensocial.Message.Field.html">Field</a>s
 *    are supported.
 *
 * @return {opensocial.Message} The new
 *    <a href="opensocial.Message.html">message</a> object.
 * @private
 */
opensocial.Container.prototype.newMessage = function(body, opt_params) {
  return new opensocial.Message(body, opt_params);
};


/**
 * Creates an IdSpec object.
 *
 * @param {Object.<opensocial.IdSpec.Field, Object>} params
 *    Parameters defining the id spec.
 * @return {opensocial.IdSpec} The new
 *     <a href="opensocial.IdSpec.html">IdSpec</a> object.
 * @private
 */
opensocial.Container.prototype.newIdSpec = function(params) {
  return new opensocial.IdSpec(params);
};


/**
 * Creates a NavigationParameters object.
 *
 * @param {Object.<opensocial.NavigationParameters.Field, Object>} params
 *     Parameters defining the navigation.
 * @return {opensocial.NavigationParameters} The new
 *     <a href="opensocial.NavigationParameters.html">NavigationParameters</a>
 *     object.
 * @private
 */
opensocial.Container.prototype.newNavigationParameters = function(params) {
  return new opensocial.NavigationParameters(params);
};


/**
 * Creates a new response item with caja support if enabled.
 * @return {opensocial.ResponseItem} the response item object.
 * @private
 */
opensocial.Container.prototype.newResponseItem = function(originalDataRequest,
    data, opt_errorCode, opt_errorMessage) {
  return new opensocial.ResponseItem(originalDataRequest, data, opt_errorCode,
      opt_errorMessage);
};


/**
 * Creates a new data response with caja support if enabled.
 * @return {opensocial.DataResponse} the data response object.
 * @private
 */
opensocial.Container.prototype.newDataResponse = function(responseItems,
    opt_globalError) {
  return new opensocial.DataResponse(responseItems, opt_globalError);
};


/**
 * Get a data request object to use for sending and fetching data from the
 * server.
 *
 * @return {opensocial.DataRequest} the request object.
 * @private
 */
opensocial.Container.prototype.newDataRequest = function() {
  return new opensocial.DataRequest();
};


/**
 * Get a new environment object.
 *
 * @return {opensocial.Environment} the environment object.
 * @private
 */
opensocial.Container.prototype.newEnvironment = function(domain,
    supportedFields) {
  return new opensocial.Environment(domain, supportedFields);
};

/**
 * Invalidates all resources cached for the current viewer.
 */
opensocial.Container.prototype.invalidateCache = function() {
};

/**
 * Returns true if the specified value is an array
 * @param {Object} val Variable to test.
 * @return {boolean} Whether variable is an array.
 * @private
 */
opensocial.Container.isArray = function(val) {
  return val instanceof Array;
};


/**
 * Returns the value corresponding to the key in the fields map. Escapes
 * the value appropriately.
 * @param {Object.<string, Object>} fields All of the values mapped by key.
 * @param {string} key The key to get data for.
 * @param {Object.<opensocial.DataRequest.DataRequestFields, Object>}
 *  opt_params Additional
 *    <a href="opensocial.DataRequest.DataRequestFields.html">params</a>
 *    to pass to the request.
 * @return {string} The data.
 * @private
 */
opensocial.Container.getField = function(fields, key, opt_params) {
  var value = fields[key];
  return opensocial.Container.escape(value, opt_params, false);
};

opensocial.Container.escape = function(value, opt_params, opt_escapeObjects) {
  if (opt_params && opt_params[opensocial.DataRequest.DataRequestFields.ESCAPE_TYPE] == opensocial.EscapeType.NONE) {
    return value;
  } else {
    return gadgets.util.escape(value, opt_escapeObjects);
  }
};
