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
 * @fileoverview Provides the top level wave object.
 */

/**
 * @namespace This namespace defines the top level wave object
 * within the Wave Gadgets API.
 */
var wave = wave || {};

/**
 * Constructs a callback given the provided callback
 * and an optional context.
 *
 * @constructor
 * @this {wave.Callback}
 * @class This class is an immutable utility class for handlings callbacks
 *     with variable arguments and an optional context.
 * @param {?(function(wave.State=, Object.<string, string>=)|
 *           function(Array.<wave.Participant>=)|
 *           function(wave.Mode.<number>=)
 *          )} callback A callback function
 *     or null.
 * @param {Object=} opt_context If context is specified, the method will be
 *     called back in the context of that object (optional).
 */
wave.Callback = function(callback, opt_context) {
  this.callback_ = callback;
  this.context_ = opt_context || null;
};

/**
 * Invokes the callback method with any arguments passed.
 *
 * @param {...} var_args
 * @export
 */
wave.Callback.prototype.invoke = function(var_args) {
  if (this.callback_) {
    this.callback_.apply(this.context_, arguments);
  }
};

/**
 * @name wave.Mode
 * @class Identifiers for wave modes exhibited by the blip containing
 *     the gadget.
 * @enum {number}
 * @export
 */
wave.Mode = {
  /**
   * @member wave.Mode
   * @constant
   * @name UNKNOWN
   * @desc The blip containing the gadget is in an unknown mode.
   * In this case, you should not attempt to edit the blip.
   */
  UNKNOWN: 0,
  /**
   * @member wave.Mode
   * @constant
   * @name VIEW
   * @desc The blip containing the gadget is in view, but not edit mode.
   */
  VIEW: 1,
  /**
   * @member wave.Mode
   * @constant
   * @name EDIT
   * @desc Editing the gadget blip
   */
  EDIT: 2,
  /**
   * @member wave.Mode
   * @constant
   * @name DIFF_ON_OPEN
   * @desc The blip containing the gadget has changed since the last time
   * it was opened and the gadget should notify this change to the user.
   */
  DIFF_ON_OPEN: 3,
  /**
   * @member wave.Mode
   * @constant
   * @name PLAYBACK
   * @desc The blip containing the gadget is in playback mode.
   */
  PLAYBACK: 4
};

wave.API_PARAM_ = "wave";

wave.ID_PARAM_ = "waveId";

wave.id_ = null;

wave.viewer_ = null;

wave.host_ = null;

wave.participants_ = [];

wave.participantMap_ = {};

wave.participantCallback_ = new wave.Callback(null);

wave.state_ = null;

wave.stateCallback_ = new wave.Callback(null);

wave.privateState_ = null;

wave.privateStateCallback_ = new wave.Callback(null);

wave.mode_ = null;

wave.modeCallback_ = new wave.Callback(null);

wave.inWaveContainer_ = false;
