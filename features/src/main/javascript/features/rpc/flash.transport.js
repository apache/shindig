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

gadgets.rpctx = gadgets.rpctx || {};

/**
 * Transport for browsers that utilizes a small Flash bridge and
 * Flash's ExternalInterface methods to transport messages securely,
 * and with guarantees provided on sender identity. This largely emulates wpm.
 *
 *   flash: postMessage on the window object.
 *        - Internet Explorer 6/7
 *        - In theory, any browser supporting Flash 8 and above,
 *          though embed code is tailored to IE only to reduce size.
 *        + (window.postMessage takes precedence where available)
 */
if (!gadgets.rpctx.flash) {  // make lib resilient to double-inclusion

  gadgets.rpctx.flash = function() {
    // TODO: provide method to override this
    var swfUrl = "//flashrpc.googleusercontent.com/xpc.swf";
    var process = null;
    var ready = null;
    var secureReceivers = {};
    var relayHandle = null;

    var LOADER_TIMEOUT_MS = 100;
    var MAX_LOADER_RETRIES = 100;
    var pendingHandshakes = [];
    var setupHandle = null;
    var setupAttempts = 0;

    var myLoc = window.location.href;

    function relayLoader() {
      if (relayHandle === null && document.body) {
        relayHandle = document.createElement('embed');
        relayHandle.allowScriptAccess = true;
        relayHandle.src = swfUrl + '?origin=' + myLoc;
        document.body.appendChild(relayHandle);
      }
      ++setupAttempts;
      if (setupHandle !== null &&
          (relayHandle !== null || setupAttempts >= MAX_LOADER_RETRIES)) {
        window.clearTimeout(setupHandle);
      }
    }

    function flushHandshakes() {
      if (relayHandle !== null) {
        while (pendingHandshakes.length > 0) {
          var shake = pendingHandshakes.shift();
          relayHandle.setup(shake.id, shake.role);
        }
      }
    }

    return {
      // "core" transport methods
      getCode: function() {
        return 'flash';
      },

      isParentVerifiable: function() {
        return true;
      },

      init: function(processFn, readyFn) {
        process = processFn;
        ready = readyFn;
        return true;
      },

      setup: function(receiverId, token, forceSecure) {
        // Perform all setup, including embedding of relay SWF (a one-time
        // per Window operation), in this method. We cannot assume document.body
        // exists however, since child-to-parent setup is often done in head.
        // Thus we simply enqueue a setup pair and attempt to complete them all.
        // If body already exists then this enqueueing will immediately flush;
        // otherwise polling will occur until the SWF has completed loading, at
        // which point all connections will complete their handshake.
        secureReceivers[receiverId] = !!forceSecure;
        var role = targetId === ".." ? "OUTER" : "INNER";
        pendingHandshakes.push({ id: targetId, role: role });
        if (relayHandle === null && setupHandle === null) {
          setupHandle = window.setTimeout(relayLoader, LOADER_TIMEOUT_MS);
        }
        flushHandshakes();
        return true;
      },

      call: function(targetId, from, rpc) {
        var targetOrigin = gadgets.rpc.getTargetOrigin(targetId);;
        relayHandle["sendMessage_" + targetId](gadgets.json.stringify(rpc), targetOrigin);
        return true;
      },

      // Methods called by relay SWF. Should be considered private.
      _receiveMessage: function(targetId, message, fromOrigin, toOrigin) {
        // TODO: validate targetId for sanity, and fromOrigin/toOrigin for domain verification.
        window.setTimeout(function() { process(gadgets.json.parse(message)); }, 0);
      },

      _ready: function() {
        flushHandshakes();
        if (setupHandle !== null) {
          window.clearTimeout(setupHandle);
        }
        setupHandle = null;
      }
    };
  }();

} // !end of double-inclusion guard
