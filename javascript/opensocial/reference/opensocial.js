/**
 * Copyright 2007 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @fileoverview Browser environment for interacting with people.
 */


/**
 * @static
 * @class
 * Namespace for top-level people functions.
 *
 * @name opensocial
 */

/**
 * Namespace for top level people functions.
 *
 * @private
 * @constructor (note: a constructor for JsDoc purposes)
 */
var opensocial = function() {};


/**
 * Takes an activity and tries to create it,
 * without waiting for the operation to complete.
 * Optionally calls a function when the operation completes.
 * <p>
 * <b>See also:</b>
 * <a href="#newActivity">newActivity()</a>
 * </p>
 *
 * <p class="note">
 * <b>Note:</b>
 * If this is the first activity that has been created for the user and
 * the request is marked as HIGH priority then this call may open a user flow
 * and navigate away from your gadget.
 *
 * @param {opensocial.Activity} activity The <a href="opensocial.Activity.html">
 *    activity</a> to create
 * @param {opensocial.CreateActivityPriority} priority The
 *    <a href="opensocial.CreateActivityPriority.html">priority</a> for this
 *    request
 * @param {Function} opt_callback The function to call once the request has been
 *    processed. This callback will either be called or the gadget will be
 *    reloaded from scratch
 *
 * @member opensocial
 */
opensocial.requestCreateActivity = function(activity, priority, opt_callback) {
 opensocial.Container.get().requestCreateActivity(activity, priority,
     opt_callback);
};


/**
 * @static
 * @class
 * The priorities a create activity request can have.
 * <p><b>See also:</b>
 * <a href="opensocial.html#requestCreateActivity">
 * opensocial.requestCreateActivity()</a>
 * </p>
 *
 * @name opensocial.CreateActivityPriority
 */
opensocial.CreateActivityPriority = {
  /**
   * If the activity is of high importance, it will be created even if this
   * requires asking the user for permission. This may cause the container to
   * open a user flow which may navigate away from your gagdet.
   *
   * @member opensocial.CreateActivityPriority
   */
  HIGH : 'HIGH',

  /**
   * If the activity is of low importance, it will not be created if the
   * user has not given permission for the current app to create activities.
   * With this priority, the requestCreateActivity call will never open a user
   * flow.
   *
   * @member opensocial.CreateActivityPriority
   */
  LOW : 'LOW'
};


/**
 * Returns true if the current gadget has access to the specified
 * permission.
 *
 * @param {opensocial.Permission} permission
 *    The <a href="opensocial.Permission.html">permission</a>
 * @return {Boolean}
 *    True if the gadget has access for the permission; false if it doesn't
 *
 * @member opensocial
 */
opensocial.hasPermission = function(permission) {
  return opensocial.Container.get().hasPermission(permission);
};


/**
 * Requests the user to grant access to the specified permissions.
 *
 * @param {Array.&lt;opensocial.Permission&gt;} permissions
 *    The <a href="opensocial.Permission.html">permissions</a> to request
 *    from the viewer
 * @param {String} reason Displayed to the user as the reason why these
 *    permissions are needed
 * @param {Function} opt_callback The function to call once the request has been
 *    processed; either this callback will be called or the gadget will be
 *    reloaded from scratch
 *
 * @member opensocial
 */
opensocial.requestPermission = function(permissions, reason, opt_callback) {
  opensocial.Container.get().requestPermission(permissions, reason,
      opt_callback);
};


/**
 * @static
 * @class
 *
 * The permissions an app can ask for.
 *
 * <p>
 * <b>See also:</b>
 * <a href="opensocial.html#hasPermission">
 * <code>opensocial.hasPermission()</code></a>,
 * <a href="opensocial.html#requestPermission">
 * <code>opensocial.requestPermission()</code></a>
 *
 * @name opensocial.Permission
 */
opensocial.Permission = {
  /**
   * Access to the viewer person object
   *
   * @member opensocial.Permission
   */
  VIEWER : 'viewer'
};


/**
 * Attempts to navigate to this gadget on a different surface. If the container
 * supports parameters will pass the optional parameters along to the gadget on
 * the new surface.
 *
 * @param {opensocial.Surface} surface The surface to navigate to
 * @param {Map.&lt;String, String&gt;} opt_params
 *    Params to pass to the gadget after it
 *    has been navigated to on the surface
 *
 * @member opensocial
 */
/* TODO(doll): Do we want a real Surface object here? or just the name? */
opensocial.requestNavigateTo = function(surface, opt_params) {
  return opensocial.Container.get().requestNavigateTo(surface, opt_params);
};


/**
 * Gets the current environment for this gadget. You can use the environment to
 * make queries such as what profile fields and surfaces are supported by this
 * container, what parameters were passed to the current gadget, and so on.
 *
 * @return {opensocial.Environment}
 *    The current <a href="opensocial.Environment.html">environment</a>
 *
 * @member opensocial
 */
opensocial.getEnvironment = function() {
  return opensocial.Container.get().getEnvironment();
};


/**
 * Fetches content from the provided URL and feeds that content into the
 * callback function.
 * @param {String} url The URL where the content is located
 * @param {Function} callback The function to call with the data from the URL
 *     once it is fetched
 * @param {Map.&lt;opensocial.ContentRequestParameters, Object&gt;} opt_params
 *     Additional
 *     <a href="opensocial.ContentRequestParameters.html">parameters</a>
 *     to pass to the request
 *
 * @member opensocial
 */
opensocial.makeRequest = function(url, callback, opt_params) {
  opensocial.Container.get().makeRequest(url, callback, opt_params);
};


/**
 * TODO(doll): Rename authentication to authorization, also add headers param
 *
 * @static
 * @class
 * Used by the
 * <a href="opensocial.html#makeRequest">
 * <code>opensocial.makeRequest()</code></a> method.
 * @name opensocial.ContentRequestParameters
 */
opensocial.ContentRequestParameters = {
  /**
   * The method to use when fetching content from the URL;
   * defaults to <code>MethodType.GET</a></code>.
   * Specified as a 
   * <a href="opensocial.ContentRequestParameters.MethodType.html">MethodType</a>.
   *
   * @member opensocial.ContentRequestParameters
   */
  METHOD : 'method',

  /**
   * The type of content that lives at the URL;
   * defaults to <code>ContentType.HTML</code>.
   * Specified as a
   * <a href="opensocial.ContentRequestParameters.ContentType.html">
   * ContentType</a>.
   *
   * @member opensocial.ContentRequestParameters
   */
  CONTENT_TYPE : 'contentType',

  /**
   * The type of authentication to use when fetching the content;
   * defaults to <code>AuthenticationType.NONE</code>.
   * Specified as an
   * <a href="opensocial.ContentRequestParameters.AuthenticationType.html">
   * AuthenticationType</a>.
   *
   * @member opensocial.ContentRequestParameters
   */
  AUTHENTICATION : 'authentication',

  /**
   * If the content is a feed, the number of entries to fetch;
   * defaults to 3.
   * Specified as a <code>Number</code>.
   *
   * @member opensocial.ContentRequestParameters
   */
  NUM_ENTRIES : 'numEntries',

  /**
   * If the content is a feed, whether to fetch summaries for that feed;
   * defaults to false.
   * Specified as a <code>Boolean</code>.
   *
   * @member opensocial.ContentRequestParameters
   */
  GET_SUMMARIES : 'getSummaries'
};


/**
 * @static
 * @class
 * Used by
 * <a href="opensocial.ContentRequestParameters.html">
 * ContentRequestParameters</a>.
 * @name opensocial.ContentRequestParameters.MethodType
 */
opensocial.ContentRequestParameters.MethodType = {
  /** @member opensocial.ContentRequestParameters.MethodType */
  GET : 'get',

  /** @member opensocial.ContentRequestParameters.MethodType */
  POST : 'post'
};


/**
 * @static
 * @class
 * Used by
 * <a href="opensocial.ContentRequestParameters.html">
 * ContentRequestParameters</a>.
 * @name opensocial.ContentRequestParameters.ContentType
 */
opensocial.ContentRequestParameters.ContentType = {
  /**
   * Returns text.
   * @member opensocial.ContentRequestParameters.ContentType
   */
  HTML : 'html',

  /**
   * Returns a dom object.
   * @member opensocial.ContentRequestParameters.ContentType
   */
  XML : 'xml',

  /**
   * Returns a json object.
   * @member opensocial.ContentRequestParameters.ContentType
   */
   FEED : 'feed'
};


/**
 * @static
 * @class
 * Used by
 * <a href="opensocial.ContentRequestParameters.html">
 * ContentRequestParameters</a>.
 * @name opensocial.ContentRequestParameters.AuthenticationType
 */
opensocial.ContentRequestParameters.AuthenticationType = {
  /** @member opensocial.ContentRequestParameters.AuthenticationType */
  NONE : 'none',

  /** @member opensocial.ContentRequestParameters.AuthenticationType */
  SIGNED : 'signed',

  /** @member opensocial.ContentRequestParameters.AuthenticationType */
  AUTHENTICATED : 'authenticated'
};


/**
 * Creates a data request object to use for sending and fetching data from the
 * server.
 *
 * @return {opensocial.DataRequest} The
 *    <a href="opensocial.DataRequest.html">request</a> object
 * @member opensocial
 */
opensocial.newDataRequest = function() {
  return opensocial.Container.get().newDataRequest();
};


/**
 * Creates an activity object,
 * which represents an activity on the server.
 * <p>
 * <b>See also:</b>
 * <a href="#requestCreateActivity">requestCreateActivity()</a>,
 * </p>
 *
 * @param {String} title The title of an activity
 * @param {Map.&lt;opensocial.Activity.Field, Object&gt;} opt_params Any other
 *    fields that should be set on the activity object; all of the defined
 *    <a href="opensocial.Activity.Field.html">Field</a>s are supported
 * @return {opensocial.Activity} The new
 *    <a href="opensocial.Activity.html">activity</a> object
 * @member opensocial
 */
opensocial.newActivity = function(title, opt_params) {
  return opensocial.Container.get().newActivity(title, opt_params);
};


/**
 * Creates a media item associated with an activity.
 * Represents images, movies, and audio.
 * Used when creating activities on the server.
 *
 * @param {String} mimeType
 *    <a href="opensocial.Activity.MediaItem.Type.html">MIME type</a> of the
 *    media
 * @param {String} url Where the media can be found
 * @param {Map.&lt;opensocial.Activity.MediaItem.Field, Object&gt;} opt_params
 *    Any other fields that should be set on the media item object;
 *    all of the defined
 *    <a href="opensocial.Activity.MediaItem.Field.html">Field</a>s
 *    are supported
 *
 * @return {opensocial.Activity.MediaItem} The new
 *    <a href="opensocial.Activity.MediaItem.html">media item</a> object
 * @member opensocial
 */
opensocial.newActivityMediaItem = function(mimeType, url, opt_params) {
  return opensocial.Container.get().newActivityMediaItem(mimeType,
      url, opt_params);
};


// TODO(doll): Util function - pull up the gadgets inherits in shindig so that
// opensocial and gadgets use the same one
Function.prototype.inherits = function(parentCtor) {
  function tempCtor() {};
  tempCtor.prototype = parentCtor.prototype;
  this.superClass_ = parentCtor.prototype;
  this.prototype = new tempCtor();
  this.prototype.constructor = this;
};
