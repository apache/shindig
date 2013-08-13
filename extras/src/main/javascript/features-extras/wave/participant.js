/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * 'License'); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * 'AS IS' BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

/**
 * @fileoverview Provides classes for defining and managing participants
 * on a wave.
 */

/**
 * Creates a new participant.
 *
 * @class This class specifies participants on a wave.
 * @constructor
 * Participant information can be dynamically updated (except for  IDs).
 * This includes the thumbnail URL, display name, and  any future extensions to
 * the participant. Instead of storing this information, gadgets should update
 * displayed participant data each time they receive a participant callback.
 * @this {wave.Participant}
 * @param {string=} id Participant id.
 * @param {string=} displayName Participant display name.
 * @param {string=} thumbnailUrl Profile thumbnail URL.
 */
wave.Participant = function(id, displayName, thumbnailUrl) {
  this.id_ = id || '';
  this.displayName_ = displayName || '';
  this.thumbnailUrl_ = thumbnailUrl || '';
};

/**
 * Gets the unique identifier of this participant.
 *
 * @return {string} The participant's id.
 * @export
 */
wave.Participant.prototype.getId = function() {
  return this.id_;
};

/**
 * Gets the human-readable display name of this participant.
 *
 * @return {string} The participant's human-readable display name.
 * @export
 */
wave.Participant.prototype.getDisplayName = function() {
  return this.displayName_;
};

/**
 * Gets the url of the thumbnail image for this participant.
 *
 * @return {string} The participant's thumbnail image url.
 * @export
 */
wave.Participant.prototype.getThumbnailUrl = function() {
  return this.thumbnailUrl_;
};

/**
 * Constructs a Participant object from JSON data.
 *
 * @param {!Object.<string, string>} json JSON object.
 * @return {wave.Participant}
 */
wave.Participant.fromJson_ = function(json) {
  var p = new wave.Participant();
  p.id_ = json['id'];
  p.displayName_ = json['displayName'];
  p.thumbnailUrl_ = json['thumbnailUrl'];
  return p;
};
