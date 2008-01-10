/**
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
 * @fileoverview Representation of a person.
 */


/**
 * @class
 * Base interface for all person objects.
 *
 * @name opensocial.Person
 */


/**
 * Base interface for all person objects.
 *
 * @private
 * @constructor
 */
opensocial.Person = function(opt_params, opt_isOwner, opt_isViewer) {
  this.fields_ = opt_params || {};
  this.isOwner_ = opt_isOwner;
  this.isViewer_ = opt_isViewer;
};


/**
 * @static
 * @class
 * All of the fields that a person has. These are the supported keys for the
 * <a href="opensocial.Person.html#getField">Person.getField()</a> method.
 *
 * @name opensocial.Person.Field
 */
/* TODO(doll): This is not the complete set of fields yet. */
opensocial.Person.Field = {
  /**
   * A string ID that can be permanently associated with this person.
   * @member opensocial.Person.Field
   */
  ID : 'id',

  /**
   * A string containing the person's name.
   * @member opensocial.Person.Field
   */
  NAME : 'name',

  /**
   * Person's photo thumbnail URL, specified as a string.
   * @member opensocial.Person.Field
   */
  THUMBNAIL_URL : 'thumbnailUrl',

  /**
   * Person's profile URL, specified as a string.
   * @member opensocial.Person.Field
   */
  PROFILE_URL : 'profileUrl',

  /**
   * Person's age, specified as a number. Not supported by all containers.
   * @member opensocial.Person.Field
   */
  AGE : 'age',

 /**
   * Person's gender, specified as a string. Not supported by all containers.
   * @member opensocial.Person.Field
   */
  GENDER : 'gender'

  // TODO(doll): Add the rest of the profile fields here
};


/**
 * Gets an ID that can be permanently associated with this person.
 *
 * @return {String} The ID
 */
opensocial.Person.prototype.getId = function() {
  return this.getField(opensocial.Person.Field.ID);
};


/**
 * Gets a text display name for this person; guaranteed to return
 * a useful string.
 *
 * @return {String} The display name
 */
opensocial.Person.prototype.getDisplayName = function() {
  return this.getField(opensocial.Person.Field.NAME);
};


/**
 * Gets data for this person that is associated with the specified key.
 *
 * @param {String} key The key to get data for;
 *    keys are defined in <a href="opensocial.Person.Field.html"><code>
 *    Person.Field</code></a>
 * @return {String} The data
 */
opensocial.Person.prototype.getField = function(key) {
  return this.fields_[key];
};


/**
 * Returns true if this person object represents the currently logged in user.
 *
 * @return {Boolean} True if this is the currently logged in user;
 *   otherwise, false
 */
opensocial.Person.prototype.isViewer = function() {
  return !!this.isViewer_;
};


/**
 * Returns true if this person object represents the owner of the current page.
 *
 * @return {Boolean} True if this is the owner of the page;
 *   otherwise, false
 */
opensocial.Person.prototype.isOwner = function() {
  return !!this.isOwner_;
};
