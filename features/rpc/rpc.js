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
 * container-to-gadget, and gadget-to-gadget (thru container) communication.
 *
 *
 */

var gadgets = gadgets || {};

/**
 * @static
 * @class Provides operations for making rpc calls.
 * @name gadgets.rpc
 */
gadgets.rpc = function() {
  // General constants.
  var CALLBACK_NAME = '__cb';
  var DEFAULT_NAME = '';

  // Consts for FrameElement.
  var FE_G2C_CHANNEL = '__g2c_rpc';
  var FE_C2G_CHANNEL = '__c2g_rpc';

  var services = {};
  var iframePool = [];
  var relayUrl = {};
  var useLegacyProtocol = {};
  var authToken = {};
  var callId = 0;
  var callbacks = {};
  var setup = {};
  var sameDomain = {};

  var params = {};

  // Load the authentication token for speaking to the container
  // from the gadget's parameters, or default to '0' if not found.
  if (gadgets.util) {
    params = gadgets.util.getUrlParameters();
  }

  authToken['..'] = params.rpctoken || params.ifpctok || 0;

  /*
   * Return a short code representing the best available cross-domain
   * message transport available to the browser.
   *
   * + For those browsers that support native messaging (various implementations
   *   of the HTML5 postMessage method), use that. Officially defined at
   *   http://www.whatwg.org/specs/web-apps/current-work/multipage/comms.html.
   *
   *   postMessage is a native implementation of XDC. A page registers that
   *   it would like to receive messages by listening the the "message" event
   *   on the window (document in DPM) object. In turn, another page can
   *   raise that event by calling window.postMessage (document.postMessage
   *   in DPM) with a string representing the message and a string
   *   indicating on which domain the receiving page must be to receive
   *   the message. The target page will then have its "message" event raised
   *   if the domain matches and can, in turn, check the origin of the message
   *   and process the data contained within.
   *
   *     wpm: postMessage on the window object.
   *        - Internet Explorer 8+
   *        - Safari (latest nightlies as of 26/6/2008)
   *        - Firefox 3+
   *        - Opera 9+
   *
   *     dpm: postMessage on the document object.
   *        - Opera 8+
   *
   * + For Gecko-based browsers, the security model allows a child to call a
   *   function on the frameElement of the iframe, even if the child is in
   *   a different domain. This method is dubbed "frameElement" (fe).
   *
   *   The ability to add and call such functions on the frameElement allows
   *   a bidirectional channel to be setup via the adding of simple function
   *   references on the frameElement object itself. In this implementation,
   *   when the container sets up the authentication information for that gadget
   *   (by calling setAuth(...)) it as well adds a special function on the
   *   gadget's iframe. This function can then be used by the gadget to send
   *   messages to the container. In turn, when the gadget tries to send a
   *   message, it checks to see if this function has its own function stored
   *   that can be used by the container to call the gadget. If not, the
   *   function is created and subsequently used by the container.
   *   Note that as a result, FE can only be used by a container to call a
   *   particular gadget *after* that gadget has called the container at
   *   least once via FE.
   *
   *     fe: Gecko-specific frameElement trick.
   *        - Firefox 1+
   *
   * + For all others, we have a fallback mechanism known as "ifpc". IFPC
   *   exploits the fact that while same-origin policy prohibits a frame from
   *   accessing members on a window not in the same domain, that frame can,
   *   however, navigate the window heirarchy (via parent). This is exploited by
   *   having a page on domain A that wants to talk to domain B create an iframe
   *   on domain B pointing to a special relay file and with a message encoded
   *   after the hash (#). This relay, in turn, finds the page on domain B, and
   *   can call a receipt function with the message given to it. The relay URL
   *   used by each caller is set via the gadgets.rpc.setRelayUrl(..) and
   *   *must* be called before the call method is used.
   *
   *     ifpc: Iframe-based method, utilizing a relay page, to send a message.
   */
  function getRelayChannel() {
    return typeof window.postMessage === 'function' ? 'wpm' :
           typeof document.postMessage === 'function' ? 'dpm' :
           navigator.product === 'Gecko' ? 'fe' :
           'ifpc';
  }

  /**
   * Conducts any initial global work necessary to setup the
   * channel type chosen.
   */
  function setupChannel() {
    // If the channel type is one of the native
    // postMessage based ones, setup the handler to receive
    // messages.
    if (relayChannel === 'dpm' || relayChannel === 'wpm') {
      window.addEventListener('message', function(packet) {
        // TODO validate packet.domain for security reasons
        process(gadgets.json.parse(packet.data));
      }, false);
    }
  }

  // Pick the most efficient RPC relay mechanism
  var relayChannel = getRelayChannel();

  // Conduct any setup necessary for the chosen channel.
  setupChannel();

  // Create the Default RPC handler.
  services[DEFAULT_NAME] = function() {
    throw new Error('Unknown RPC service: ' + this.s);
  };

  // Create a Special RPC handler for callbacks.
  services[CALLBACK_NAME] = function(callbackId, result) {
    var callback = callbacks[callbackId];
    if (callback) {
      delete callbacks[callbackId];
      callback(result);
    }
  };

  /**
   * Conducts any frame-specific work necessary to setup
   * the channel type chosen. This method is called when
   * the container page first registers the gadget in the
   * RPC mechanism. Gadgets, in turn, will complete the setup
   * of the channel once they send their first messages.
   */
  function setupFrame(frameId) {
    if (setup[frameId]) {
      return;
    }

    if (relayChannel === 'fe') {
      try {
        var frame = document.getElementById(frameId);
        frame[FE_G2C_CHANNEL] = function(args) {
          process(gadgets.json.parse(args));
        };
      } catch (e) {
        // Something went wrong. System will fallback to
        // IFPC.
      }
    }

    setup[frameId] = true;
  }

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
    //
    // RPC object contents:
    //   s: Service Name
    //   f: From
    //   c: The callback ID or 0 if none.
    //   a: The arguments for this RPC call.
    //   t: The authentication token.
    //
    if (rpc && typeof rpc.s === 'string' && typeof rpc.f === 'string' &&
        rpc.a instanceof Array) {

      // Validate auth token.
      if (authToken[rpc.f]) {
        // We allow type coercion here because all the url params are strings.
        if (authToken[rpc.f] != rpc.t) {
          throw new Error("Invalid auth token.");
        }
      }

      // If there is a callback for this service, attach a callback function
      // to the rpc context object for asynchronous rpc services.
      //
      // Synchronous rpc request handlers should simply ignore it and return a
      // value as usual.
      // Asynchronous rpc request handlers, on the other hand, should pass its
      // result to this callback function and not return a value on exit.
      //
      // For example, the following rpc handler passes the first parameter back
      // to its rpc client with a one-second delay.
      //
      // function asyncRpcHandler(param) {
      //   var me = this;
      //   setTimeout(function() {
      //     me.callback(param);
      //   }, 1000);
      // }
      if (rpc.c) {
        rpc.callback = function(result) {
          gadgets.rpc.call(rpc.f, CALLBACK_NAME, null, rpc.c, result);
        };
      }

      // Call the requested RPC service.
      var result = (services[rpc.s] ||
                    services[DEFAULT_NAME]).apply(rpc, rpc.a);

      // If the rpc request handler returns a value, immediately pass it back
      // to the callback. Otherwise, do nothing, assuming that the rpc handler
      // will make an asynchronous call later.
      if (rpc.c && typeof result != 'undefined') {
        gadgets.rpc.call(rpc.f, CALLBACK_NAME, null, rpc.c, result);
      }
    }
  }

  /**
   * Attempts to conduct an RPC call to the specified
   * target with the specified data via the FrameElement
   * method. If this method fails, the system attempts again
   * using the known default of IFPC.
   *
   * @param {String} targetId Module Id of the RPC service provider.
   * @param {String} serviceName Service name to call.
   * @param {String} from Module Id of the calling provider.
   * @param {Object} rpcData The RPC data for this call.
   * @param {Array.<Object>} callArgs Original arguments to call()
   */
  function callFrameElement(targetId, serviceName, from, rpcData, callArgs) {
    try {
      if (from != '..') {
        // Call from gadget to the container.
        var fe = window.frameElement;

        if (typeof fe[FE_G2C_CHANNEL] === 'function') {
          // Complete the setup of the FE channel if need be.
          if (typeof fe[FE_G2C_CHANNEL][FE_C2G_CHANNEL] !== 'function') {
            fe[FE_G2C_CHANNEL][FE_C2G_CHANNEL] = function(args) {
              process(gadgets.json.parse(args));
            };
          }

          // Conduct the RPC call.
          fe[FE_G2C_CHANNEL](rpcData);
          return;
        }
      } else {
        // Call from container to gadget[targetId].
        var frame = document.getElementById(targetId);

        if (typeof frame[FE_G2C_CHANNEL] === 'function' &&
            typeof frame[FE_G2C_CHANNEL][FE_C2G_CHANNEL] === 'function') {

          // Conduct the RPC call.
          frame[FE_G2C_CHANNEL][FE_C2G_CHANNEL](rpcData);
          return;
        }
      }
    } catch (e) {
    }

    // If we have reached this point, something has failed
    // with the FrameElement method, so we default to using
    // IFPC for this call.
    callIfpc(targetId, serviceName, from, rpcData, callArgs);
  }

  /**
   * Conducts an RPC call to the specified
   * target with the specified data via the IFPC
   * method.
   *
   * @param {String} targetId Module Id of the RPC service provider.
   * @param {String} serviceName Service name to call.
   * @param {String} from Module Id of the calling provider.
   * @param {Object} rpcData The RPC data for this call.
   * @param {Array.<Object>} callArgs Original arguments to call()
   */
  function callIfpc(targetId, serviceName, from, rpcData, callArgs) {
    // Retrieve the relay file used by IFPC. Note that
    // this must be set before the call, and so we conduct
    // an extra check to ensure it is not blank.
    var relay = gadgets.rpc.getRelayUrl(targetId);

    if (!relay) {
      throw new Error('No relay file assigned for IFPC');
    }

    // The RPC mechanism supports two formats for IFPC (legacy and current).
    var src = null;
    if (useLegacyProtocol[targetId]) {
      // Format: #iframe_id&callId&num_packets&packet_num&block_of_data
      src = [relay, '#', encodeLegacyData([from, callId, 1, 0,
             encodeLegacyData([from, serviceName, '', '', from].concat(
               callArgs))])].join('');
    } else {
      // Format: #targetId & sourceId@callId & packetNum & packetId & packetData
      src = [relay, '#', targetId, '&', from, '@', callId,
             '&1&0&', encodeURIComponent(rpcData)].join('');
    }

    // Conduct the IFPC call by creating the Iframe with
    // the relay URL and appended message.
    emitInvisibleIframe(src);
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
      try {
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
      } catch (e) {
        // Ignore; IE7 throws an exception when trying to read readyState and
        // readyState isn't set.
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

  /**
   * Attempts to make an rpc by calling the target's receive method directly.
   * This works when gadgets are rendered on the same domain as their container,
   * a potentially useful optimization for trusted content which keeps
   * RPC behind a consistent interface.
   * @param {String} target Module id of the rpc service provider
   * @param {String} from Module id of the caller (this)
   * @param {String} callbackId Id of the call
   * @param {String} rpcData JSON-encoded RPC payload
   * @return
   */
  function callSameDomain(target, rpc) {
    if (typeof sameDomain[target] === 'undefined') {
      // Seed with a negative, typed value to avoid
      // hitting this code path repeatedly
      sameDomain[target] = false;
      var targetEl = null;
      if (target === '..') {
        targetEl = parent;
      } else {
        targetEl = frames[target];
      }
      try {
        // If this succeeds, then same-domain policy applied
        sameDomain[target] = targetEl.gadgets.rpc.receiveSameDomain;
      } catch (e) {
        // Usual case: different domains
      }
    }

    if (typeof sameDomain[target] === 'function') {
      // Call target's receive method
      sameDomain[target](rpc);
      return true;
    }

    return false;
  }

  // gadgets.config might not be available, such as when serving container js.
  if (gadgets.config) {
    /**
     * Initializes RPC from the provided configuration.
     */
    function init(config) {
      // Allow for wild card parent relay files as long as it's from a
      // white listed domain. This is enforced by the rendering servlet.
      if (config.rpc.parentRelayUrl.substring(0, 7) === 'http://') {
        relayUrl['..'] = config.rpc.parentRelayUrl;
      } else {
        // It's a relative path, and we must append to the parent.
        // We're relying on the server validating the parent parameter in this
        // case. Because of this, parent may only be passed in the query, not
        // the fragment.
        var params = document.location.search.substring(0).split("&");
        var parentParam = "";
        for (var i = 0, param; param = params[i]; ++i) {
          // Only the first parent can be validated.
          if (param.indexOf("parent=") === 0) {
            parentParam = decodeURIComponent(param.substring(7));
            break;
          }
        }
        relayUrl['..'] = parentParam + config.rpc.parentRelayUrl;
      }
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
      if (serviceName == CALLBACK_NAME) {
        throw new Error("Cannot overwrite callback service");
      }

      if (serviceName == DEFAULT_NAME) {
        throw new Error("Cannot overwrite default service:"
                        + " use registerDefault");
      }

      services[serviceName] = handler;
    },

    /**
     * Unregisters an RPC service.
     * @param {String} serviceName Service name to unregister.
     *
     * @member gadgets.rpc
     */
    unregister: function(serviceName) {
      if (serviceName == CALLBACK_NAME) {
        throw new Error("Cannot delete callback service");
      }

      if (serviceName == DEFAULT_NAME) {
        throw new Error("Cannot delete default service:"
                        + " use unregisterDefault");
      }

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

      // Default to the container calling.
      var from = '..';

      if (targetId === '..') {
        from = window.name;
      }

      // Not used by legacy, create it anyway...
      var rpc = {
        s: serviceName,
        f: from,
        c: callback ? callId : 0,
        a: Array.prototype.slice.call(arguments, 3),
        t: authToken[targetId]
      };

      // If target is on the same domain, call method directly
      if (callSameDomain(targetId, rpc)) {
        return;
      }

      var rpcData = gadgets.json.stringify(rpc);

      var channelType = relayChannel;

      // If we are told to use the legacy format, then we must
      // default to IFPC.
      if (useLegacyProtocol[targetId]) {
        channelType = 'ifpc';
      }

      switch (channelType) {
        case 'dpm': // use document.postMessage.
          var targetDoc = targetId === '..' ? parent.document :
                                              frames[targetId].document;
          targetDoc.postMessage(rpcData);
          break;

        case 'wpm': // use window.postMessage.
          var targetWin = targetId === '..' ? parent : frames[targetId];
          targetWin.postMessage(rpcData, "*");
          break;

        case 'fe': // use FrameElement.
          callFrameElement(targetId, serviceName, from, rpcData, rpc.a);
          break;

        default: // use 'ifpc' as a fallback mechanism.
          callIfpc(targetId, serviceName, from, rpcData, rpc.a);
          break;
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
     * @param {String} url Full relay URL of the target frame.
     * @param {Boolean} opt_useLegacy True if this relay needs the legacy IFPC
     *     wire format.
     *
     * @member gadgets.rpc
     */
    setRelayUrl: function(targetId, url, opt_useLegacy) {
      relayUrl[targetId] = url;
      useLegacyProtocol[targetId] = !!opt_useLegacy;
    },

    /**
     * Sets the auth token of a target frame.
     * @param {String} targetId Name of the target frame.
     * @param {String} token The authentication token to use for all
     *     calls to or from this target id.
     *
     * @member gadgets.rpc
     */
    setAuthToken: function(targetId, token) {
      authToken[targetId] = token;
      setupFrame(targetId);
    },

    /**
     * Gets the RPC relay mechanism.
     * @return {String} RPC relay mechanism. See above for
     *   a list of supported types.
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
    },

    /**
     * Receives and processes an RPC request sent via the same domain.
     * (Not to be used directly). Converts the inbound rpc object's
     * Array into a local Array to pass the process() Array test.
     * @param {Object} rpc RPC object containing all request params
     */
    receiveSameDomain: function(rpc) {
      // Pass through to local process method but converting to a local Array
      rpc.a = Array.prototype.slice.call(rpc.a);
      window.setTimeout(function() { process(rpc) }, 0);
    }
  };
}();

