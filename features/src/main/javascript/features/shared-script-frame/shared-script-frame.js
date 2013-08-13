/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

/**
 * @fileoverview This feature provides a means for multiple instances of a gadget to share
 *   a single frame for loading script code.
 *
 *   This is the gadget specific code.
 */

gadgets.script = (function(){

  /**
   * Obtain a reference to the script frame by name.
   *
   * @param {string} name The name of the frame to request.
   * @return {?Window} The requested frame, or null if not available.
   */
  var getFrameByName = function(name) {
    return name ? window.open('', name) : null;
  };

  /**
   * Make RPC call to obtain the script frame name, then call the callback with the
   * passing in a reference to the script frame.
   *
   * @param {function(?Window)} callback The callback function that the gadget passes in.
   *   This function is passed the shared-script-frame window.  The param may be undefined
   *   if something goes wrong, gadgets should verify.
   */
  var getScriptFrame = function(callback) {
    gadgets.rpc.call(null, 'get_script_frame_name', function(name) {
      callback(getFrameByName(name));
    });
  };

  return {
    getScriptFrame: getScriptFrame
  };
})();