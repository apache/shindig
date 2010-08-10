/**
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

/**
 * @fileoverview  Tame and expose wave.* API to cajoled gadgets.
 */

var tamings___ = tamings___ || [];
var caja___;
var ___;
tamings___.push(function(imports) {
  // wave.Mode is an object literal that holds only constants
  ___.grantRead(wave, 'Mode');

  /**
   * The following taming of wave.Callback and wave.Callback.invoke
   * is needed because:
   *   - wave.Callback is exposed to cajoled code
   *   - wave.Callback.invoke is exposed to cajoled code
   *   - the wave api invokes some callbacks constructed by itself
   *     and others constructed by cajoled code
   */
  function SafeCallback(tameCallback, opt_tameContext) {
   var okCallback = {apply: ___.markFuncFreeze(function(ignored, args) {
     return ___.callPub(tameCallback, 'apply', [opt_tameContext, args]);
   })};
   return new wave.Callback(okCallback, ___.USELESS);
  }

  SafeCallback.prototype = wave.Callback.prototype;
  wave.Callback.prototype.constructor = SafeCallback;
  ___.markCtor(SafeCallback, Object, 'Callback');
  ___.primFreeze(SafeCallback);
  ___.tamesTo(wave.Callback, SafeCallback);

  ___.handleGenericMethod(SafeCallback.prototype, 'invoke', function(var_args) {
   return ___.callPub(this.callback_, 'apply', [___.tame(this.context_),
                                                Array.slice(arguments, 0)]);
  });

  caja___.whitelistCtors([
    [wave, 'Participant', Object],
    [wave, 'State', Object]
  ]);

  caja___.whitelistMeths([
    [wave.Participant, 'getDisplayName'],
    [wave.Participant, 'getId'],
    [wave.Participant, 'getThumbnailUrl'],

    [wave.State, 'get'],
    [wave.State, 'getKeys'],
    [wave.State, 'reset'],
    [wave.State, 'submitDelta'],
    [wave.State, 'submitValue'],
    [wave.State, 'toString']
  ]);

  caja___.whitelistFuncs([
    [wave, 'getHost'],
    [wave, 'getMode'],
    [wave, 'getParticipantById'],
    [wave, 'getParticipants'],
    [wave, 'getState'],
    [wave, 'getTime'],
    [wave, 'getViewer'],
    [wave, 'isInWaveContainer'],
    [wave, 'log'],
    [wave, 'setModeCallback'],
    [wave, 'setParticipantCallback'],
    [wave, 'setStateCallback'],

    [wave.util, 'printJson']
  ]);

  imports.outers.wave = ___.tame(wave);
  ___.grantRead(imports.outers, 'wave');
});
