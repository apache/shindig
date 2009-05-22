/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

var gadgets = gadgets || {};
gadgets.rpctx = gadgets.rpctx || {};

/**
 * Similar transport to WPM, but uses document element instead.
 * This was implemented by Opera 8 before the WPM standard was complete.
 *
 *     dpm: postMessage on the document object.
 *        - Opera 8+
 */
gadgets.rpctx.Dpm = function() {
  var ready;
  return {
    getCode: function() {
      return 'dpm';
    },

    isParentVerifiable: function() {
      return false;
    },

    init: function(processFn, readyFn) {
      ready = readyFn;
      // Set up native postMessage handler.
      window.addEventListener('message', function(packet) {
        // TODO validate packet.domain for security reasons
        processFn(gadgets.json.parse(packet.data));
      }, false);
      ready('..', true);  // Ready immediately.
      return true;
    },

    setup: function(receiverId, token) {
      // See WPM setup comment.
      if (receiverId === '..') {
        gadgets.rpc.call(receiverId, gadgets.rpc.ACK);
      }
      return true;
    },

    call: function(targetId, from, rpc) {
      var targetDoc = targetId === '..' ? parent.document :
                                          frames[targetId].document;
      targetDoc.postMessage(gadgets.json.stringify(rpc));
      return true;
    }
  };
}();
