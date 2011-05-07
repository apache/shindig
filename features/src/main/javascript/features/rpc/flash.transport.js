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
    var swfId = "___xpcswf";
    var swfUrl = null;
    var usingFlash = false;
    var processFn = null;
    var readyFn = null;
    var relayHandle = null;

    var LOADER_TIMEOUT_MS = 100;
    var MAX_LOADER_RETRIES = 50;
    var pendingHandshakes = [];
    var setupHandle = null;
    var setupAttempts = 0;

    var SWF_CHANNEL_READY = "_scr";
    var SWF_CONFIRMED_PARENT = "_pnt";
    var READY_TIMEOUT_MS = 100;
    var MAX_READY_RETRIES = 50;
    var readyAttempts = 0;
    var readyHandle = null;
    var readyMsgs = {};

    var myLoc = window.location.protocol + "//" + window.location.host;
    var JSL_NS = '___jsl';
    var METHODS_NS = '_fm';
    var bucketNs;

    function setupMethodBucket() {
      window[JSL_NS] = window[JSL_NS] || {};
      var container = window[JSL_NS];
      var bucket = container[METHODS_NS] = {};
      bucketNs = JSL_NS + "." + METHODS_NS;
      return bucket;
    }

    var methodBucket = setupMethodBucket();

    function exportMethod(method, requestedName) {
      var exported = function() {
        method.apply({}, arguments);
      };
      methodBucket[requestedName] = methodBucket[requestedName] || exported;
      return bucketNs + "." + requestedName;
    }

    function getChannelId(receiverId) {
      return receiverId === ".." ? gadgets.rpc.RPC_ID : receiverId;
    }

    function getRoleId(targetId) {
      return targetId === ".." ? "INNER" : "OUTER";
    }

    function init(config) {
      if (usingFlash) {
        swfUrl = config['rpc']['commSwf'] || "/xpc.swf";
      }
    }
    gadgets.config.register('rpc', null, init);

    function relayLoader() {
      if (relayHandle === null && document.body && swfUrl) {
        var theSwf = swfUrl + '?cb=' + Math.random() + '&origin=' + myLoc + '&jsl=1';

        var containerDiv = document.createElement('div');
        containerDiv.style.height = '1px';
        containerDiv.style.width = '1px';
        var html = '<object height="1" width="1" id="' + swfId +
            '" type="application/x-shockwave-flash">' +
            '<param name="allowScriptAccess" value="always"></param>' +
            '<param name="movie" value="' + theSwf + '"></param>' +
            '<embed type="application/x-shockwave-flash" allowScriptAccess="always" ' +
            'src="' + theSwf + '" height="1" width="1"></embed>' +
            '</object>';

        document.body.appendChild(containerDiv);
        containerDiv.innerHTML = html;

        relayHandle = containerDiv.firstChild;
      }
      ++setupAttempts;
      if (setupHandle !== null &&
          (relayHandle !== null || setupAttempts >= MAX_LOADER_RETRIES)) {
        window.clearTimeout(setupHandle);
      } else {
        // Either document.body doesn't yet exist or config doesn't.
        // In either case the relay handle isn't set up properly yet, and
        // so should be retried.
        setupHandle = window.setTimeout(relayLoader, LOADER_TIMEOUT_MS);
      }
    }

    function childReadyPoller() {
      // Attempt sending a message to parent indicating that child is ready
      // to receive messages. This only occurs after the SWF indicates that
      // its setup() method has been successfully called and completed, and
      // only in child context.
      if (readyMsgs[".."]) return;
      sendChannelReady("..");
      readyAttempts++;
      if (readyAttempts >= MAX_READY_RETRIES && readyHandle !== null) {
        window.clearTimeout(readyHandle);
        readyHandle = null;
      } else {
        // Try again later. The handle will be cleared during receipt of
        // the setup ACK.
        readyHandle = window.setTimeout(childReadyPoller, READY_TIMEOUT_MS);
      }
    }

    function flushHandshakes() {
      if (relayHandle !== null) {
        while (pendingHandshakes.length > 0) {
          var shake = pendingHandshakes.shift();
          var targetId = shake.targetId;
          relayHandle['setup'](shake.token, getChannelId(targetId), getRoleId(targetId));
        }
      }
    }

    function ready() {
      flushHandshakes();
      if (setupHandle !== null) {
        window.clearTimeout(setupHandle);
      }
      setupHandle = null;
    }
    exportMethod(ready, 'ready');

    function setupDone() {
      // Called by SWF only for role_id = "INNER" ie when initializing to parent.
      // Instantiates a polling handshake mechanism which ensures that any enqueued
      // messages remain so until each side is ready to send.
      if (!readyMsgs[".."] && readyHandle === null) {
        readyHandle = window.setTimeout(childReadyPoller, READY_TIMEOUT_MS);
      }
    }
    exportMethod(setupDone, 'setupDone');

    function call(targetId, from, rpc) {
      var targetOrigin = gadgets.rpc.getTargetOrigin(targetId);
      var rpcKey = gadgets.rpc.getAuthToken(targetId);
      var handleKey = "sendMessage_" + getChannelId(targetId) + "_" + rpcKey + "_" + getRoleId(targetId);
      var messageHandler = relayHandle[handleKey];
      messageHandler.call(relayHandle, gadgets.json.stringify(rpc), targetOrigin);
      return true;
    }

    function receiveMessage(message, fromOrigin, toOrigin) {
      var jsonMsg = gadgets.json.parse(message);
      var channelReady = jsonMsg[SWF_CHANNEL_READY];
      if (channelReady) {
        // Special message indicating that a ready message has been received, indicating
        // the sender is now prepared to receive messages. This type of message is instigated
        // by child context in polling fashion, and is responded-to by parent context(s).
        // If readyHandle is non-null, then it should first be cleared.
        // This method is OK to call twice, if it occurs in a race.
        readyFn(channelReady, true);
        readyMsgs[channelReady] = true;
        if (channelReady !== "..") {
          // Child-to-parent: immediately signal that parent is ready.
          // Now that we know that child can receive messages, it's enough to send once.
          sendChannelReady(channelReady, true);
        }
        return;
      }
      window.setTimeout(function() { processFn(jsonMsg, fromOrigin); }, 0);
    }
    exportMethod(receiveMessage, 'receiveMessage');

    function sendChannelReady(receiverId, opt_isParentConfirmation) {
      var myId = gadgets.rpc.RPC_ID;
      var readyAck = {};
      readyAck[SWF_CHANNEL_READY] = opt_isParentConfirmation ? ".." : myId;
      readyAck[SWF_CONFIRMED_PARENT] = myId;
      call(receiverId, myId, readyAck);
    }

    return {
      // "core" transport methods
      getCode: function() {
        return 'flash';
      },

      isParentVerifiable: function() {
        return true;
      },

      init: function(processIn, readyIn) {
        processFn = processIn;
        readyFn = readyIn;
        usingFlash = true;
        return true;
      },

      setup: function(receiverId, token) {
        // Perform all setup, including embedding of relay SWF (a one-time
        // per Window operation), in this method. We cannot assume document.body
        // exists however, since child-to-parent setup is often done in head.
        // Thus we simply enqueue a setup pair and attempt to complete them all.
        // If body already exists then this enqueueing will immediately flush;
        // otherwise polling will occur until the SWF has completed loading, at
        // which point all connections will complete their handshake.
        pendingHandshakes.push({ token: token, targetId: receiverId });
        if (relayHandle === null && setupHandle === null) {
          setupHandle = window.setTimeout(relayLoader, LOADER_TIMEOUT_MS);
        }
        flushHandshakes();
        return true;
      },

      call: call,

      // Methods called by relay SWF. Should be considered private.
      _receiveMessage: receiveMessage,
      _ready: ready,
      _setupDone: setupDone
    };
  }();

} // !end of double-inclusion guard
