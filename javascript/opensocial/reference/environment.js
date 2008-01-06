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
 * @fileoverview Representation of a environment.
 */


/**
 * @class
 * Represents the current environment for a gadget.
 *
 * <p>
 * <b>See also:</b>
 * <a href="opensocial.html#getEnvironment">opensocial.getEnvironment()</a>,
 *
 * @name opensocial.Environment
 */


/**
 * Base interface for all environment objects.
 *
 * @param {String} domain The current domain
 * @param {opensocial.Surface} surface The current surface
 * @param {Array.&lt;Surface&gt;} supportedSurfaces
 *    The surfaces supported by this container
 * @param {Map.&lt;String, Map.&lt;String, Boolean&gt;&gt;} supportedFields
 *    The fields supported by this container
 * @param {Map.&lt;String, String&gt;} opt_params
 *    The params this gadget has access to
 *
 * @private
 * @constructor
 */
opensocial.Environment = function(domain, surface, supportedSurfaces,
    supportedFields, opt_params) {
  this.domain = domain;
  this.surface = surface;
  this.supportedSurfaces = supportedSurfaces;
  this.supportedFields = supportedFields;
  this.params = opt_params || {};
};


/**
 * Returns the current domain &mdash;
 * for example, "orkut.com" or "myspace.com".
 *
 * @return {String} The domain
 */
opensocial.Environment.prototype.getDomain = function() {
  return this.domain;
};


/**
 * Returns the current surface.
 *
 * @return {opensocial.Surface}
 *    The current <a href="opensocial.Surface.html">surface</a>
 */
opensocial.Environment.prototype.getSurface = function() {
  return this.surface;
};


/**
 * Returns an array of all the supported surfaces.
 *
 * @return {Array.&lt;opensocial.Surface&gt;}
 *    All supported <a href="opensocial.Surface.html">surfaces</a>
 */
opensocial.Environment.prototype.getSupportedSurfaces = function() {
  return this.supportedSurfaces;
};


/**
 * Returns the parameters passed into this gadget.
 *
 * @return {Map.&lt;String, String&gt;} The parameter map
 */
opensocial.Environment.prototype.getParams = function() {
  return this.params;
};


/**
 * @static
 * @class
 *
 * The types of objects in this container.
 * 
 * <p>
 * <b>See also:</b>
 * <a href="opensocial.Environment.html#supportsField">
 * <code>Environment.supportsField()</code></a>
 *
 * @name opensocial.Environment.ObjectType
 */
opensocial.Environment.ObjectType = {
  /**
   * @member opensocial.Environment.ObjectType
   */
  PERSON : 'person',
  /**
   * @member opensocial.Environment.ObjectType
   */
  ACTIVITY : 'activity',
  /**
   * @member opensocial.Environment.ObjectType
   */
  ACTIVITY_MEDIA_ITEM : 'activityMediaItem'
};


/**
 * Returns true if the specified field is supported in this container on the
 * given object type.
 *
 * @param {opensocial.Environment.ObjectType} objectType
 *    The <a href="opensocial.Environment.ObjectType.html">object type</a>
 *    to check for the field
 * @param {String} fieldName The name of the field to check for
 * @return {Boolean} True if the field is supported on the specified object type
 */
opensocial.Environment.prototype.supportsField = function(objectType,
    fieldName) {
  var supportedObjectFields = this.supportedFields[objectType] || [];
  return !!supportedObjectFields[fieldName];
};


/**
 * Returns true if the specified function is supported in this container.
 *
 * @param {String} functionName The function name
 * @return {Boolean} True if this container supports the function
 */
opensocial.Environment.prototype.hasCapability = function(functionName) {
  var splitNames = functionName.split(".");
  var parentObject = window;

  for (var i = 0; i < splitNames.length; i++) {
    var childObject = parentObject[splitNames[i]];
    if (!childObject) {
      return false;
    } else {
      parentObject = childObject;
    }
  }
  return true;
};
