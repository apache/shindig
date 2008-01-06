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
 * @fileoverview Representation of a surface.
 */


/**
 * @class
 * Base interface for all surface objects.
 *
 * @name opensocial.Surface
 */


/**
 * Base interface for all surface objects.
 *
 * @private
 * @constructor
 */
opensocial.Surface = function(name, opt_isPrimaryContent) {
  this.name = name;
  this.isPrimaryContentValue = !!opt_isPrimaryContent;
};


/**
 * Returns the name of this surface.
 *
 * @return {String} The surface name
 */
opensocial.Surface.prototype.getName = function() {
  // TODO(doll): Should we doc these names as enums?
  return this.name;
};


/**
 * Returns true if the gadget is the primary content on this surface.
 * On a canvas page
 * this is most likely true; on a profile page, it is most likely false.
 *
 * @return {boolean} True if the gadget is the primary content; otherwise, false
 */
opensocial.Surface.prototype.isPrimaryContent = function() {
  return this.isPrimaryContentValue;
};
