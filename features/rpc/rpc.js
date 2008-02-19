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

/**
 * @fileoverview Remote procedure call library for gadget-to-container,
 * container-to-gadget, and gadget-to-gadget communication.
 */

var gadgets = gadgets || {};

/**
 * @static
 * @class Provides operations for making rpc calls.
 * @name gadgets.rpc
 */
gadgets.rpc = function() {
  var services = {};
  var iframePool = [];
  var relayUrl = {};
  var useLegacyProtocol = {};
  var callId = 0;
  var callbacks = {};

  // Pick the most efficient RPC relay mechanism
  var relayChannel = typeof document.postMessage === 'function' ? 'dpm' :
                     typeof window.postMessage === 'function' ? 'wpm' :
                     'ifpc';
  if (relayChannel === 'dpm' || relayChannel === 'wpm') {
    document.addEventListener('message', function(packet) {
      // TODO validate packet.domain for security reasons
      process(gadgets.json.parse(packet.data));
    }, false);
  }

  // Default RPC handler
  services[''] = function() {
    throw new Error('Unknown RPC service: ' + this.s);
  };

  // Special RPC handler for callbacks
  services['__cb'] = function(callbackId, result) {
    var callback = callbacks[callbackId];
    if (callback) {
      delete callbacks[callbackId];
      callback(result);
    }
  };

  /**
   * Encodes arguments for the legacy IFPC wire format.
   *
   * @param {Object} args
   * @return {String} the encoded args
   */
  function encodeLegacyData(args) {
    var stringify = gadgets.json.stringify;
    var argsEscaped = [];
    for(var i = 0, j = args.length; i < j; ++i) {
      argsEscaped.push(encodeURIComponent(stringify(args[i])));
    }
    return argsEscaped.join('&');
  }

  /**
   * Helper function to process an RPC request
   * @param {Object} rpc RPC request object
   * @private
   */
  function process(rpc) {
    if (rpc && typeof rpc.s === 'string' && typeof rpc.f === 'string' &&
        rpc.a instanceof Array) {
      var result = (services[rpc.s] || services['']).apply(rpc, rpc.a);
      if (rpc.c) {
        gadgets.rpc.call(rpc.f, '__cb', null, rpc.c, result);
      }
    }
  }

  /**
   * Helper function to emit an invisible IFrame.
   * @param {String} src SRC attribute of the IFrame to emit.
   * @private
   */
  function emitInvisibleIframe(src) {
    var iframe;
    // Recycle IFrames
    for (var i = iframePool.length - 1; i >=0; --i) {
      var ifr = iframePool[i];
      if (ifr && (ifr.recyclable || ifr.readyState === 'complete')) {
        ifr.parentNode.removeChild(ifr);
        if (window.ActiveXObject) {
          // For MSIE, delete any iframes that are no longer being used. MSIE
          // cannot reuse the IFRAME because a navigational click sound will
          // be triggered when we set the SRC attribute.
          // Other browsers scan the pool for a free iframe to reuse.
          iframePool[i] = ifr = null;
          iframePool.splice(i, 1);
        } else {
          ifr.recyclable = false;
          iframe = ifr;
          break;
        }
      }
    }
    // Create IFrame if necessary
    if (!iframe) {
      iframe = document.createElement('iframe');
      iframe.style.border = iframe.style.width = iframe.style.height = '0px';
      iframe.style.visibility = 'hidden';
      iframe.style.position = 'absolute';
      iframe.onload = function() { this.recyclable = true; };
      iframePool.push(iframe);
    }
    iframe.src = src;
    setTimeout(function() { document.body.appendChild(iframe); }, 0);
  }

  // gadgets.config might not be available, such as when serving container js.
  if (gadgets.config) {
    /**
     * Initializes RPC from the provided configuration.
     */
    function init(config) {
      relayUrl['..'] = config.rpc.parentRelayUrl;
      useLegacyProtocol['..'] = !!config.rpc.useLegacyProtocol;
    }

    var requiredConfig = {
      parentRelayUrl : gadgets.config.NonEmptyStringValidator
    };
    gadgets.config.register("rpc", requiredConfig, init);
  }

  return /** @scope gadgets.rpc */ {
    /**
     * Registers an RPC service.
     * @param {String} serviceName Service name to register.
     * @param {Function} handler Service handler.
     *
     * @member gadgets.rpc
     */
    register: function(serviceName, handler) {
      services[serviceName] = handler;
    },

    /**
     * Unregisters an RPC service.
     * @param {String} serviceName Service name to unregister.
     *
     * @member gadgets.rpc
     */
    unregister: function(serviceName) {
      delete services[serviceName];
    },

    /**
     * Registers a default service handler to processes all unknown
     * RPC calls which raise an exception by default.
     * @param {Function} handler Service handler.
     *
     * @member gadgets.rpc
     */
    registerDefault: function(handler) {
      services[''] = handler;
    },

    /**
     * Unregisters the default service handler. Future unknown RPC
     * calls will fail silently.
     *
     * @member gadgets.rpc
     */
    unregisterDefault: function() {
      delete services[''];
    },

    /**
     * Calls an RPC service.
     * @param {String} targetId Module Id of the RPC service provider.
     *                          Empty if calling the parent container.
     * @param {String} serviceName Service name to call.
     * @param {Function|null} callback Callback function (if any) to process
     *                                 the return value of the RPC request.
     * @param {*} var_args Parameters for the RPC request.
     *
     * @member gadgets.rpc
     */
    call: function(targetId, serviceName, callback, var_args) {
      ++callId;
      targetId = targetId || '..';
      if (callback) {
        callbacks[callId] = callback;
      }
      var from;
      if (targetId === '..') {
        from = window.name;
      } else {
        from = '..';
      }

      switch (relayChannel) {
      case 'dpm': // use document.postMessage
        var targetDoc = targetId === '..' ? parent.document :
                                            frames[targetId].document;
        targetDoc.postMessage(rpcData);
        break;
      case 'wpm': // use window.postMessage
        var targetWin = targetId === '..' ? parent : frames[targetId];
        targetWin.postMessage(rpcData);
        break;
      default: // use 'ifpc' as a fallback mechanism
        var relay = gadgets.rpc.getRelayUrl(targetId);

        // TODO split message if too long
        var src;
        if (useLegacyProtocol[targetId]) {
          // #iframe_id&callId&num_packets&packet_num&block_of_data
          var legacyData = [from, serviceName, null, null, from].concat(
              Array.prototype.slice.call(arguments, 3));
          src = [relay, '#', encodeLegacyData([from, '&', callId, '&1&0&',
                 encodeLegacyData(legacyData)])].join('');
        } else {
          var rpcData = gadgets.json.stringify({
            s: serviceName,
            f: from,
            c: callback ? callId : 0,
            a: Array.prototype.slice.call(arguments, 3)
          });
          // # targetId & sourceId@callId & packetNum & packetId & packetData
          src = [relay, '#', targetId, '&', from, '@', callId,
                 '&1&0&', rpcData].join('');
        }
        emitInvisibleIframe(src);
      }
    },

    /**
     * Gets the relay URL of a target frame.
     * @param {String} targetId Name of the target frame.
     * @return {String|undefined} Relay URL of the target frame.
     *
     * @member gadgets.rpc
     */
    getRelayUrl: function(targetId) {
      return relayUrl[targetId];
    },

    /**
     * Sets the relay URL of a target frame.
     * @param {String} targetId Name of the target frame.
     * @param {String} relayUrl Full relay URL of the target frame.
     *
     * @member gadgets.rpc
     */
    setRelayUrl: function(targetId, relayUrl) {
      relayUrl[targetId] = relayUrl;
    },

    /**
     * Gets the RPC relay mechanism.
     * @return {String} RPC relay mechanism. Supported types:
     *                  'wpm' - Use window.postMessage (defined by HTML5)
     *                  'dpm' - Use document.postMessage (defined by an early
     *                          draft of HTML5 and implemented by Opera)
     *                  'ifpc' - Use invisible IFrames
     *
     * @member gadgets.rpc
     */
    getRelayChannel: function() {
      return relayChannel;
    },

    /**
     * Receives and processes an RPC request. (Not to be used directly.)
     * @param {Array.<String>} fragment An RPC request fragment encoded as
     *        an array. The first 4 elements are target id, source id & call id,
     *        total packet number, packet id. The last element stores the actual
     *        JSON-encoded and URI escaped packet data.
     *
     * @member gadgets.rpc
     */
    receive: function(fragment) {
      if (fragment.length > 4) {
        // TODO parse fragment[1..3] to merge multi-fragment messages
        process(gadgets.json.parse(
            decodeURIComponent(fragment[fragment.length - 1])));
      }
    }
  };
}();

