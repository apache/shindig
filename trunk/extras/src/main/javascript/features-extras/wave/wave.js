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
 * @fileoverview Provides access to the wave API in gadgets.
 *
 * Clients can access the wave API by the wave object.
 *
 * Example:
 * <pre>
 *   var state;
 *   var privateState;
 *   var viewer;
 *   var participants;
 *   if (wave && wave.isInWaveContainer()) {
 *     state = wave.getState();
 *     privateState = wave.getPrivateState();
 *     viewer = wave.getViewer();
 *     participants = wave.getParticipants();
 *   }
 * </pre>
 */

/**
 * Checks the wave parameter to determine whether the gadget container claims
 * to be wave-aware.
 */
wave.checkWaveContainer_ = function() {
  var params = gadgets.util.getUrlParameters();
  wave.inWaveContainer_ =
      (params.hasOwnProperty(wave.API_PARAM_) && params[wave.API_PARAM_]);
  wave.id_ = (params.hasOwnProperty(wave.ID_PARAM_) && params[wave.ID_PARAM_]);
};

/**
 * Indicates whether the gadget runs inside a wave container.
 *
 * @return {boolean} whether the gadget runs inside a wave container
 * @export
 */
wave.isInWaveContainer = function() {
  return wave.inWaveContainer_;
};

/**
 * Participant callback relay.
 *
 * @param {{myId: string, authorId: string,
 *          participants: Object.<string, string>}} data participants object.
 */
wave.receiveWaveParticipants_ = function(data) {
  wave.viewer_ = null;
  wave.host_ = null;
  wave.participants_ = [];
  wave.participantMap_ = {};
  var myId = data['myId'];
  var hostId = data['authorId'];
  var participants = data['participants'];
  for (var id in participants) {
    var p = wave.Participant.fromJson_(participants[id]);
    if (id == myId) {
      wave.viewer_ = p;
    }
    if (id == hostId) {
      wave.host_ = p;
    }
    wave.participants_.push(p);
    wave.participantMap_[id] = p;
  }
  if (!wave.viewer_ && myId) {
    // In this case, the viewer has not yet been added to the participant
    // list, and so did not have a complete Participant object created.
    // Let's create it here.
    var p = new wave.Participant(myId, myId);
    wave.viewer_ = p;
    wave.participants_.push(p);
    wave.participantMap_[myId] = p;
  }
  wave.participantCallback_.invoke(wave.participants_);
};

/**
 * State callback relay.
 *
 * @param {!Object.<string, string>} data raw state data object.
 */
wave.receiveState_ = function(data) {
  wave.state_ = wave.state_ || new wave.State('wave_gadget_state');
  var delta = wave.state_.calculateDelta_(data);
  wave.state_.setState_(data);
  wave.stateCallback_.invoke(wave.state_, delta);
};

/**
 * Private state callback relay.
 *
 * @param {!Object.<string, string>} data raw state data object.
 */
wave.receivePrivateState_ = function(data) {
  wave.privateState_ =
      wave.privateState_ || new wave.State('wave_private_gadget_state');
  var delta = wave.privateState_.calculateDelta_(data);
  wave.privateState_.setState_(data);
  wave.privateStateCallback_.invoke(wave.privateState_, delta);
};

/**
 * State delta callback relay.
 *
 * @param {!Object.<string, string>} delta the delta object.
 */
wave.receiveStateDelta_ = function(delta) {
  wave.state_ = wave.state_ || new wave.State('wave_gadget_state');
  wave.state_.applyDelta_(delta);
  wave.stateCallback_.invoke(wave.state_, delta);
};

/**
 * Private state delta callback relay.
 *
 * @param {!Object.<string, string>} delta the delta object.
 */
wave.receivePrivateStateDelta_ = function(delta) {
  wave.privateState_ =
      wave.privateState_ || new wave.State('wave_private_gadget_state');
  wave.privateState_.applyDelta_(delta);
  wave.privateStateCallback_.invoke(wave.privateState_, delta);
};

/**
 * Mode callback relay.
 *
 * @param {!Object.<string, string>} data raw mode object.
 */
wave.receiveMode_ = function(data) {
  wave.mode_ = data || {};
  wave.modeCallback_.invoke(wave.getMode());
};

/**
 * Get the <code>Participant</code> whose client renders this gadget.
 *
 * @return {wave.Participant} the viewer (null if not known)
 * @export
 */
wave.getViewer = function() {
  return wave.viewer_;
};

/**
 * Returns the <code>Participant</code> who added this gadget
 * to the blip.
 * Note that the host may no longer be in the participant list.
 *
 * @return {wave.Participant} host (null if not known)
 * @export
 */
wave.getHost = function() {
  return wave.host_;
};

/**
 * Returns a list of <code>Participant</code>s on the Wave.
 *
 * @return {Array.<wave.Participant>} Participant list.
 * @export
 */
wave.getParticipants = function() {
  return wave.participants_;
};

/**
 * Returns a <code>Participant</code> with the given id.
 *
 * @param {string} id The id of the participant to retrieve.
 * @return {wave.Participant} The participant with the given id.
 * @export
 */
wave.getParticipantById = function(id) {
  return wave.participantMap_[id];
};

/**
 * Returns the gadget state as a <code>wave.State</code> object.
 *
 * @return {wave.State} gadget state (null if not known)
 * @export
 */
wave.getState = function() {
  return wave.state_;
};

/**
 * Returns the private gadget state as a <code>wave.State</code> object.
 *
 * @return {wave.State} private gadget state (null if not known)
 * @export
 */
wave.getPrivateState = function() {
  return wave.privateState_;
};

/**
 * Returns the gadget <code>wave.Mode</code>.
 *
 * @return {wave.Mode} gadget mode.
 * @export
 */
wave.getMode = function() {
  if (wave.mode_) {
    var playback = wave.mode_['${playback}'];
    var edit = wave.mode_['${edit}'];
    if ((playback != null) && (edit != null)) {
      if (playback == '1') {
        return wave.Mode.PLAYBACK;
      } else if (edit == '1') {
        return wave.Mode.EDIT;
      } else {
        return wave.Mode.VIEW;
      }
    }
  }
  return wave.Mode.UNKNOWN;
};

/**
 * Returns the playback state of the wave/wavelet/gadget.
 * Note: For compatibility UNKNOWN mode identified as PLAYBACK.
 *
 * @return {boolean} whether the gadget is in the playback state
 * @deprecated Use wave.getMode().
 * @export
 */
wave.isPlayback = function() {
  var mode = wave.getMode();
  return (mode == wave.Mode.PLAYBACK) || (mode == wave.Mode.UNKNOWN);
};

/**
 * Sets the gadget state update callback. If the state is already received
 * from the container, the callback is invoked immediately to report the
 * current gadget state. Only invoke callback can be defined. Consecutive calls
 * would remove the old callback and set the new one.
 *
 * @param {function(wave.State=, Object.<string, string>=)} callback function
 * @param {Object=} opt_context the object that receives the callback
 * @export
 */
wave.setStateCallback = function(callback, opt_context) {
  wave.stateCallback_ = new wave.Callback(callback, opt_context);
  if (wave.state_) {
    wave.stateCallback_.invoke(wave.state_, wave.state_.state_);
  }
};

/**
 * Sets the private gadget state update callback. Works similarly to
 * setStateCallback but handles the private state events.
 *
 * @param {function(wave.State=, Object.<string, string>=)} callback function
 * @param {Object=} opt_context the object that receives the callback
 * @export
 */
wave.setPrivateStateCallback = function(callback, opt_context) {
  wave.privateStateCallback_ = new wave.Callback(callback, opt_context);
  if (wave.privateState_) {
    wave.privateStateCallback_.invoke(
        wave.privateState_, wave.privateState_.state_);
  }
};

/**
 * Sets the participant update callback. If the participant information is
 * already received, the callback is invoked immediately to report the
 * current participant information. Only one callback can be defined.
 * Consecutive calls would remove old callback and set the new one.
 *
 * @param {function(Array.<wave.Participant>)} callback function
 * @param {Object=} [opt_context] the object that receives the callback
 * @export
 */
wave.setParticipantCallback = function(callback, opt_context) {
  wave.participantCallback_ = new wave.Callback(callback, opt_context);
  if (wave.participants_) {
    wave.participantCallback_.invoke(wave.participants_);
  }
};

/**
 * Sets the mode change callback.
 *
 * @param {function(wave.Mode)} callback function
 * @param {Object=} [opt_context] the object that receives the callback
 * @export
 */
wave.setModeCallback = function(callback, opt_context) {
  wave.modeCallback_ = new wave.Callback(callback, opt_context);
  if (wave.mode_) {
    wave.modeCallback_.invoke(wave.getMode());
  }
};

/**
 * Retrieves the current time of the viewer.
 *
 * TODO: Define the necessary gadget <-> container communication and
 * implement playback time.
 *
 * @return {number} The gadget time.
 * @export
 */
wave.getTime = function() {
  // For now just return the current time.
  return new Date().getTime();
};

/**
 * Requests the container to output a log message.
 *
 * @param {string} message The message to output to the log.
 * @export
 */
wave.log = function(message) {
  gadgets.rpc.call(null, 'wave_log', null, message || '');
};

/**
 * Requests the container to update the snippet visible in wave digest.
 *
 * @param {string} snippet Snippet to associate with the gadget.
 * @export
 */
wave.setSnippet = function(snippet) {
  gadgets.rpc.call(null, 'set_snippet', null, snippet || '');
};

/**
 * Returns serialized wave ID or null if not known.
 *
 * @return {?string} Serialized wave ID.
 * @export
 */
wave.getWaveId = function() {
  return wave.id_;
};

/**
 * Internal initialization.
 */
wave.internalInit_ = function() {
  wave.checkWaveContainer_();
  if (wave.isInWaveContainer()) {
    gadgets.rpc.register('wave_participants', wave.receiveWaveParticipants_);
    gadgets.rpc.register('wave_gadget_state', wave.receiveState_);
    gadgets.rpc.register('wave_state_delta', wave.receiveStateDelta_);
    gadgets.rpc.register(
        'wave_private_gadget_state', wave.receivePrivateState_);
    gadgets.rpc.register(
        'wave_private_state_delta', wave.receivePrivateStateDelta_);
    gadgets.rpc.register('wave_gadget_mode', wave.receiveMode_);
    gadgets.rpc.call(null, 'wave_enable', null, '1.0');
  }
};

/**
 * Sets up the wave gadget variables and callbacks.
 */
(wave.init_ = function() {
  if (window['gadgets']) {
    gadgets.util.registerOnLoadHandler(function() {
      wave.internalInit_();
    });
  }
})();
