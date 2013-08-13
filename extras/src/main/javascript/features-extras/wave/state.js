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
 * @fileoverview Provides classes for defining and managing the
 * synchronized gadget state.
 */

/**
 * Creates a new state object to hold properties of the gadget.
 *
 * @constructor
 * @class This class contains state properties of the Gadget.
 * @this {wave.State}
 * @param {string=} opt_rpc rpc name
 * @export
 */
wave.State = function(opt_rpc) {
  this.setState_(null);
  this.rpc_ = opt_rpc === undefined ? 'wave_gadget_state' : opt_rpc;
};

/**
 * Retrieve a value from the synchronized state.
 * As of now, get always returns a string. This will change at some point
 * to return whatever was set.
 *
 * @param {string} key Value for the specified key to retrieve.
 * @param {?string=} opt_default Optional default value if non-existant
 *     (optional).
 * @return {?string} Object for the specified key or null if not found.
 * @export
 */
wave.State.prototype.get = function(key, opt_default) {
  if (key in this.state_) {
    return this.state_[key];
  }
  return opt_default === undefined ? null: opt_default;
};

/**
 * Retrieve the valid keys for the synchronized state.
 *
 * @return {Array.<string>} set of keys
 * @export
 */
wave.State.prototype.getKeys = function() {
  var keys = [];
  for (var key in this.state_) {
    keys.push(key);
  }
  return keys;
};

/**
 * Updates the state delta. This is an asynchronous call that
 * will update the state and not take effect immediately. Creating
 * any key with a null value will attempt to delete the key.
 *
 * @param {!Object.<string, ?string>} delta Map of key-value pairs representing
 * a delta of keys to update.
 * @export
 */
wave.State.prototype.submitDelta = function(delta) {
  gadgets.rpc.call(null, this.rpc_, null, delta);
};

/**
 * Submits delta that contains only one key-value pair. Note that if value is
 * null the key will be removed from the state.
 * See submitDelta(delta) for semantic details.
 *
 * @param {string} key
 * @param {?string} value
 * @export
 */
wave.State.prototype.submitValue = function(key, value) {
  var delta = {};
  delta[key] = value;
  this.submitDelta(delta);
};

/**
 * Submits a delta to remove all key-values in the state.
 *
 * @export
 */
wave.State.prototype.reset = function() {
  var delta = {};
  for (var key in this.state_) {
    delta[key] = null;
  }
  this.submitDelta(delta);
};

/**
 * Pretty prints the current state object. Note this is a debug method
 * only.
 *
 * @return {string} The stringified state.
 * @export
 */
wave.State.prototype.toString = function() {
  return wave.util.printJson(this.state_, true);
};

/**
 * Set the state object to the given value.
 *
 * @param {Object.<string, string>} state
 */
wave.State.prototype.setState_ = function(state) {
  this.state_ = state || {};
};

/**
 * Calculate a delta object that would turn this state into the state given
 * in the parameter when applied to this state.
 *
 * @param {!Object.<string, string>} state
 * @return {!Object.<string, string>} delta
 */
wave.State.prototype.calculateDelta_ = function(state) {
  var delta = {};
  for (var key in state) {
    var hasKey = this.state_.hasOwnProperty(key);
    if (!hasKey || (this.state_[key] != state[key])) {
      delta[key] = state[key];
    }
  }
  for (var key in this.state_) {
    if (!state.hasOwnProperty(key)) {
      delta[key] = null;
    }
  }
  return delta;
};

/**
 * Apply the given delta object to this state.
 *
 * @param {!Object.<string, string>} delta
 */
wave.State.prototype.applyDelta_ = function(delta) {
  this.state_ = this.state_ || {};
  for (var key in delta) {
    if (delta[key] != null) {
      this.state_[key] = delta[key];
    } else {
      delete this.state_[key];
    }
  }
};
