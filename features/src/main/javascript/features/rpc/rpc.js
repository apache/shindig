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
 */

/**
 * gadgets.rpc Transports
 *
 * All transports are stored in object gadgets.rpctx, and are provided
 * to the core gadgets.rpc library by various build rules.
 *
 * Transports used by core gadgets.rpc code to actually pass messages.
 * Each transport implements the same interface exposing hooks that
 * the core library calls at strategic points to set up and use
 * the transport.
 *
 * The methods each transport must implement are:
 * + getCode(): returns a string identifying the transport. For debugging.
 * + isParentVerifiable(): indicates (via boolean) whether the method
 *     has the property that its relay URL verifies for certain the
 *     receiver's protocol://host:port.
 * + init(processFn, readyFn): Performs any global initialization needed. Called
 *     before any other gadgets.rpc methods are invoked. processFn is
 *     the function in gadgets.rpc used to process an rpc packet. readyFn is
 *     a function that must be called when the transport is ready to send
 *     and receive messages bidirectionally. Returns
 *     true if successful, false otherwise.
 * + setup(receiverId, token): Performs per-receiver initialization, if any.
 *     receiverId will be '..' for gadget-to-container. Returns true if
 *     successful, false otherwise.
 * + call(targetId, from, rpc): Invoked to send an actual
 *     message to the given targetId, with the given serviceName, from
 *     the sender identified by 'from'. Payload is an rpc packet. Returns
 *     true if successful, false otherwise.
 */

if (!window['gadgets']['rpc']) { // make lib resilient to double-inclusion

  /**
   * @static
   * @namespace Provides operations for making rpc calls.
   * @name gadgets.rpc
   */
  gadgets.rpc = function() {
    /**
     * @const
     * @private
     */
    var CALLBACK_NAME = '__cb';

    /**
     * @const
     * @private
     */
    var DEFAULT_NAME = '';

    /** Exported constant, for use by transports only.
     * @const
     * @type {string}
     * @member gadgets.rpc
     */
    var ACK = '__ack';

    /**
     * Timeout and number of attempts made to setup a transport receiver.
     * @const
     * @private
     */
    var SETUP_FRAME_TIMEOUT = 500;

    /**
     * @const
     * @private
     */
    var SETUP_FRAME_MAX_TRIES = 10;

    /**
     * @const
     * @private
     */
    var ID_ORIGIN_DELIMITER = '|';

    /**
     * @const
     * @private
     */
    var RPC_KEY_CALLBACK = 'callback';

    /**
     * @const
     * @private
     */
    var RPC_KEY_ORIGIN = 'origin';
    var RPC_KEY_REFERRER = 'referer';

    var services = {};
    var relayUrl = {};
    var useLegacyProtocol = {};
    var authToken = {};
    var callId = 0;
    var callbacks = {};
    var setup = {};
    var sameDomain = {};
    var params = {};
    var receiverTx = {};
    var earlyRpcQueue = {};
    var passReferrerDirection = null;
    var passReferrerContents = null;

    // isGadget =~ isChild for the purposes of rpc (used only in setup).
    var isChild = (window.top !== window.self);

    // Set the current rpc ID from window.name immediately, to prevent
    // shadowing of window.name by a "var name" declaration, or similar.
    var rpcId = window.name;

    var securityCallback = function() {};
    var LOAD_TIMEOUT = 0;
    var FRAME_PHISH = 1;
    var FORGED_MSG = 2;

    // Fallback transport is simply a dummy impl that emits no errors
    // and logs info on calls it receives, to avoid undesired side-effects
    // from falling back to IFPC or some other transport.
    var console = window['console'];
    var clog = console && console.log && function(msg) { console.log(msg); } || function(){};
    var fallbackTransport = (function() {
      function logFn(name) {
        return function() {
          clog(name + ': call ignored');
        };
      }
      return {
        'getCode': function() { return 'noop'; },
        // Not really, but prevents transport assignment to IFPC.
        'isParentVerifiable': function() { return true; },
        'init': logFn('init'),
        'setup': logFn('setup'),
        'call': logFn('call')
      };
    })();

    // Load the authentication token for speaking to the container
    // from the gadget's parameters, or default to '0' if not found.
    if (gadgets.util) {
      params = gadgets.util.getUrlParameters();
    }

    /**
     * Return a transport representing the best available cross-domain
     * message-passing mechanism available to the browser.
     *
     * <p>Transports are selected on a cascading basis determined by browser
     * capability and other checks. The order of preference is:
     * <ol>
     * <li> wpm: Uses window.postMessage standard.
     * <li> dpm: Uses document.postMessage, similar to wpm but pre-standard.
     * <li> nix: Uses IE-specific browser hacks.
     * <li> rmr: Signals message passing using relay file's onresize handler.
     * <li> fe: Uses FF2-specific window.frameElement hack.
     * <li> ifpc: Sends messages via active load of a relay file.
     * </ol>
     * <p>See each transport's commentary/documentation for details.
     * @return {Object}
     * @member gadgets.rpc
     */
    function getTransport() {
      if (params['rpctx'] == 'flash') return gadgets.rpctx.flash;
      if (params['rpctx'] == 'rmr') return gadgets.rpctx.rmr;
      return typeof window.postMessage === 'function' ? gadgets.rpctx.wpm :
          typeof window.postMessage === 'object' ? gadgets.rpctx.wpm :
          window.ActiveXObject ? (gadgets.rpctx.flash ? gadgets.rpctx.flash : gadgets.rpctx.nix) :
          navigator.userAgent.indexOf('WebKit') > 0 ? gadgets.rpctx.rmr :
          navigator.product === 'Gecko' ? gadgets.rpctx.frameElement :
          gadgets.rpctx.ifpc;
    }

    /**
     * Function passed to, and called by, a transport indicating it's ready to
     * send and receive messages.
     */
    function transportReady(receiverId, readySuccess) {
      if (receiverTx[receiverId]) return;
      var tx = transport;
      if (!readySuccess) {
        tx = fallbackTransport;
      }
      receiverTx[receiverId] = tx;

      // If there are any early-queued messages, send them now directly through
      // the needed transport.
      var earlyQueue = earlyRpcQueue[receiverId] || [];
      for (var i = 0; i < earlyQueue.length; ++i) {
        var rpc = earlyQueue[i];
        // There was no auth/rpc token set before, so set it now.
        rpc['t'] = getAuthToken(receiverId);
        tx.call(receiverId, rpc['f'], rpc);
      }

      // Clear the queue so it won't be sent again.
      earlyRpcQueue[receiverId] = [];
    }

    //  Track when this main page is closed or navigated to a different location
    // ("unload" event).
    //  NOTE: The use of the "unload" handler here and for the relay iframe
    // prevents the use of the in-memory page cache in modern browsers.
    // See: https://developer.mozilla.org/en/using_firefox_1.5_caching
    // See: http://webkit.org/blog/516/webkit-page-cache-ii-the-unload-event/
    var mainPageUnloading = false,
        hookedUnload = false;

    function hookMainPageUnload() {
      if (hookedUnload) {
        return;
      }
      function onunload() {
        mainPageUnloading = true;
      }

      // TODO: use common helper
      if (typeof window.addEventListener != 'undefined') {
        window.addEventListener('unload', onunload, false);
      } else if (typeof window.attachEvent != 'undefined') {
        window.attachEvent('onunload', onunload);
      }

      hookedUnload = true;
    }

    function relayOnload(targetId, sourceId, token, data, relayWindow) {
      // Validate auth token.
      if (!authToken[sourceId] || authToken[sourceId] !== token) {
        gadgets.error('Invalid auth token. ' + authToken[sourceId] + ' vs ' + token);
        securityCallback(sourceId, FORGED_MSG);
      }

      relayWindow.onunload = function() {
        if (setup[sourceId] && !mainPageUnloading) {
          securityCallback(sourceId, FRAME_PHISH);
          gadgets.rpc.removeReceiver(sourceId);
        }
      };
      hookMainPageUnload();

      data = gadgets.json.parse(decodeURIComponent(data));
    }

    /**
     * Helper function that performs actual processing of an RPC request.
     * Origin is passed in separately to ensure that it cannot be spoofed,
     * and guard code in the method ensures the same before dispatching
     * any service handler.
     * @param {Object} rpc RPC request object.
     * @param {String} opt_sender RPC sender, if available and with a verified origin piece.
     * @private
     */
    function process(rpc, opt_sender) {
      //
      // RPC object contents:
      //   s: Service Name
      //   f: From
      //   c: The callback ID or 0 if none.
      //   a: The arguments for this RPC call.
      //   t: The authentication token.
      //
      if (rpc && typeof rpc['s'] === 'string' && typeof rpc['f'] === 'string' &&
          rpc['a'] instanceof Array) {

        // Validate auth token.
        if (authToken[rpc['f']]) {
          // We don't do type coercion here because all entries in the authToken
          // object are strings, as are all url params. See setupReceiver(...).
          if (authToken[rpc['f']] !== rpc['t']) {
            gadgets.error('Invalid auth token. ' + authToken[rpc['f']] + ' vs ' + rpc['t']);
            securityCallback(rpc['f'], FORGED_MSG);
          }
        }

        if (rpc['s'] === ACK) {
          // Acknowledgement API, used to indicate a receiver is ready.
          window.setTimeout(function() { transportReady(rpc['f'], true); }, 0);
          return;
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
        if (rpc['c']) {
          rpc[RPC_KEY_CALLBACK] = function(result) {
            gadgets.rpc.call(rpc['f'], CALLBACK_NAME, null, rpc['c'], result);
          };
        }

        // Set the requestor origin.
        // If not passed by the transport, then this simply sets to undefined.
        if (opt_sender) {
          var origin = getOrigin(opt_sender);
          rpc[RPC_KEY_ORIGIN] = opt_sender;
          var referrer = rpc['r'];
          if (!referrer || getOrigin(referrer) != origin) {
            // Transports send along as much info as they can about the sender
            // of the message; 'origin' is the origin component alone, while
            // 'referrer' is a best-effort field set from available information.
            // The second clause simply verifies that referrer is valid.
            referrer = opt_sender;
          }
          rpc[RPC_KEY_REFERRER] = referrer; 
        }

        // Call the requested RPC service.
        var result = (services[rpc['s']] ||
            services[DEFAULT_NAME]).apply(rpc, rpc['a']);

        // If the rpc request handler returns a value, immediately pass it back
        // to the callback. Otherwise, do nothing, assuming that the rpc handler
        // will make an asynchronous call later.
        if (rpc['c'] && typeof result !== 'undefined') {
          gadgets.rpc.call(rpc['f'], CALLBACK_NAME, null, rpc['c'], result);
        }
      }
    }

    /**
     * Helper method returning a canonicalized protocol://host[:port] for
     * a given input URL, provided as a string. Used to compute convenient
     * relay URLs and to determine whether a call is coming from the same
     * domain as its receiver (bypassing the try/catch capability detection
     * flow, thereby obviating Firebug and other tools reporting an exception).
     *
     * @param {string} url Base URL to canonicalize.
     * @memberOf gadgets.rpc
     */
    function getOrigin(url) {
      if (!url) {
        return '';
      }
      url = url.toLowerCase();
      if (url.indexOf('//') == 0) {
        url = window.location.protocol + url;
      }
      if (url.indexOf('://') == -1) {
        // Assumed to be schemaless. Default to current protocol.
        url = window.location.protocol + '//' + url;
      }
      // At this point we guarantee that "://" is in the URL and defines
      // current protocol. Skip past this to search for host:port.
      var host = url.substring(url.indexOf('://') + 3);

      // Find the first slash char, delimiting the host:port.
      var slashPos = host.indexOf('/');
      if (slashPos != -1) {
        host = host.substring(0, slashPos);
      }

      var protocol = url.substring(0, url.indexOf('://'));

      // Use port only if it's not default for the protocol.
      var portStr = '';
      var portPos = host.indexOf(':');
      if (portPos != -1) {
        var port = host.substring(portPos + 1);
        host = host.substring(0, portPos);
        if ((protocol === 'http' && port !== '80') ||
            (protocol === 'https' && port !== '443')) {
          portStr = ':' + port;
        }
      }

      // Return <protocol>://<host>[<port>]
      return protocol + '://' + host + portStr;
    }

    /*
     * Makes a sibling id in the format of "/<siblingFrameId>|<siblingOrigin>".
     */
    function makeSiblingId(id, opt_origin) {
      return '/' + id + (opt_origin ? ID_ORIGIN_DELIMITER + opt_origin : '');
    }

    /*
     * Parses an iframe id.  Returns null if not a sibling id or
     *   {id: <siblingId>, origin: <siblingOrigin>} otherwise.
     */
    function parseSiblingId(id) {
      if (id.charAt(0) == '/') {
        var delimiter = id.indexOf(ID_ORIGIN_DELIMITER);
        var siblingId = delimiter > 0 ? id.substring(1, delimiter) : id.substring(1);
        var origin = delimiter > 0 ? id.substring(delimiter + 1) : null;
        return {id: siblingId, origin: origin};
      } else {
        return null;
      }
    }

    function getTargetWin(id) {
      if (typeof id === 'undefined' ||
          id === '..') {
        return window.parent;
      }

      var siblingId = parseSiblingId(id);
      if (siblingId) {
        return window.top.frames[siblingId.id];
      }

      // Cast to a String to avoid an index lookup.
      id = String(id);

      // Try window.frames first
      var target = window.frames[id];
      if (target) {
        return target;
      }

      // Fall back to getElementById()
      target = document.getElementById(id);
      if (target && target.contentWindow) {
        return target.contentWindow;
      }

      return null;
    }

    function getTargetOrigin(id) {
      var targetRelay = null;
      var relayUrl = getRelayUrl(id);
      if (relayUrl) {
        targetRelay = relayUrl;
      } else {
        var siblingId = parseSiblingId(id);
        if (siblingId) {
          // sibling
          targetRelay = siblingId.origin;
        } else if (id == '..') {
          // parent
          targetRelay = params['parent'];
        } else {
          // child
          targetRelay = document.getElementById(id).src;
        }
      }

      return getOrigin(targetRelay);
    }

    // Pick the most efficient RPC relay mechanism.
    var transport = getTransport();

    // Create the Default RPC handler.
    services[DEFAULT_NAME] = function() {
      clog('Unknown RPC service: ' + this.s);
    };

    // Create a Special RPC handler for callbacks.
    services[CALLBACK_NAME] = function(callbackId, result) {
      var callback = callbacks[callbackId];
      if (callback) {
        delete callbacks[callbackId];
        callback.call(this, result);
      }
    };

    /**
     * Conducts any frame-specific work necessary to setup
     * the channel type chosen. This method is called when
     * the container page first registers the gadget in the
     * RPC mechanism. Gadgets, in turn, will complete the setup
     * of the channel once they send their first messages.
     */
    function setupFrame(frameId, token) {
      if (setup[frameId] === true) {
        return;
      }

      if (typeof setup[frameId] === 'undefined') {
        setup[frameId] = 0;
      }

      var tgtFrame = getTargetWin(frameId);
      if (frameId === '..' || tgtFrame != null) {
        if (transport.setup(frameId, token) === true) {
          setup[frameId] = true;
          return;
        }
      }

      if (setup[frameId] !== true && setup[frameId]++ < SETUP_FRAME_MAX_TRIES) {
        // Try again in a bit, assuming that frame will soon exist.
        window.setTimeout(function() { setupFrame(frameId, token); },
                        SETUP_FRAME_TIMEOUT);
      } else {
        // Fail: fall back for this gadget.
        receiverTx[frameId] = fallbackTransport;
        setup[frameId] = true;
      }
    }

    /**
     * Attempts to make an rpc by calling the target's receive method directly.
     * This works when gadgets are rendered on the same domain as their container,
     * a potentially useful optimization for trusted content which keeps
     * RPC behind a consistent interface.
     *
     * @param {string} target Module id of the rpc service provider.
     * @param {Object} rpc RPC data.
     * @return {boolean}
     */
    function callSameDomain(target, rpc) {
      if (typeof sameDomain[target] === 'undefined') {
        // Seed with a negative, typed value to avoid
        // hitting this code path repeatedly.
        sameDomain[target] = false;
        var targetRelay = getRelayUrl(target);
        if (getOrigin(targetRelay) !== getOrigin(window.location.href)) {
          // Not worth trying -- avoid the error and just return.
          return false;
        }

        var targetEl = getTargetWin(target);
        try {
          // If this succeeds, then same-domain policy applied
          var targetGadgets = targetEl['gadgets'];
          sameDomain[target] = targetGadgets.rpc.receiveSameDomain;
        } catch (e) {
          // Shouldn't happen due to origin check. Caught to emit more
          // meaningful error to the caller. Consider emitting in non-opt mode.
          // gadgets.log('Same domain call failed: parent= incorrectly set.');
        }
      }

      if (typeof sameDomain[target] === 'function') {
        // Call target's receive method
        sameDomain[target](rpc);
        return true;
      }

      return false;
    }

    /**
     * Gets the relay URL of a target frame.
     * @param {string} targetId Name of the target frame.
     * @return {string|undefined} Relay URL of the target frame.
     *
     * @member gadgets.rpc
     */
    function getRelayUrl(targetId) {
      var url = relayUrl[targetId];
      // Some RPC methods (wpm, for one) are unhappy with schemeless URLs.
      if (url && url.substring(0, 1) === '/') {
        if (url.substring(1, 2) === '/') {    // starts with '//'
          url = document.location.protocol + url;
        } else {    // relative URL, starts with '/'
          url = document.location.protocol + '//' + document.location.host + url;
        }
      }
      return url;
    }

    /**
     * Sets the relay URL of a target frame.
     * @param {string} targetId Name of the target frame.
     * @param {string} url Full relay URL of the target frame.
     *
     * @member gadgets.rpc
     * @deprecated
     */
    function setRelayUrl(targetId, url, opt_useLegacy) {
      // Make URL absolute if necessary
      if (!/http(s)?:\/\/.+/.test(url)) {
        if (url.indexOf('//') == 0) {
          url = window.location.protocol + url;
        } else if (url.charAt(0) == '/') {
          url = window.location.protocol + '//' + window.location.host + url;
        } else if (url.indexOf('://') == -1) {
          // Assumed to be schemaless. Default to current protocol.
          url = window.location.protocol + '//' + url;
        }
      }
      relayUrl[targetId] = url;
      useLegacyProtocol[targetId] = !!opt_useLegacy;
    }

    /**
     * Helper method to retrieve the authToken for a given gadget.
     * Not to be used directly.
     * @member gadgets.rpc
     * @return {string}
     */
    function getAuthToken(targetId) {
      return authToken[targetId];
    }

    /**
     * Sets the auth token of a target frame.
     * @param {string} targetId Name of the target frame.
     * @param {string} token The authentication token to use for all
     *     calls to or from this target id.
     *
     * @member gadgets.rpc
     * @deprecated
     */
    function setAuthToken(targetId, token) {
      token = token || '';

      // Coerce token to a String, ensuring that all authToken values
      // are strings. This ensures correct comparison with URL params
      // in the process(rpc) method.
      authToken[targetId] = String(token);

      setupFrame(targetId, token);
    }

    function setReferrerConfig(cfg) {
      var passReferrer = cfg['passReferrer'] || "";
      var prParts = passReferrer.split(":", 2);
      passReferrerDirection = prParts[0] || "none";
      passReferrerContents = prParts[1] || "origin";
    }

    function setupContainedContext(rpctoken, opt_parent) {
      function init(config) {
        var cfg = config ? config['rpc'] : {};
        var useLegacy = String(cfg['useLegacyProtocol']) === 'true';
        setReferrerConfig(cfg);

        // Parent-relative only.
        var parentRelayUrl = cfg['parentRelayUrl'] || '';
        parentRelayUrl = getOrigin(params['parent'] || opt_parent) + parentRelayUrl;
        setRelayUrl('..', parentRelayUrl, useLegacy);

        if (useLegacy) {
          transport = gadgets.rpctx.ifpc;
          transport.init(process, transportReady);
        }

        setAuthToken('..', rpctoken);
      }

      // Check to see if we know the parent yet.
      // In almost all cases we will, since the parent param is provided.
      // However, it's possible that the lib doesn't yet know, but is
      // initialized in forced fashion later.
      if (!params['parent'] && opt_parent) {
        // Handles the forced initialization case.
        init({});
        return;
      }

      // Handles the standard gadgets.config.init() case.
      gadgets.config.register('rpc', null, init);
    }

    function setupChildIframe(gadgetId, opt_frameurl, opt_authtoken) {
      if (gadgetId.charAt(0) != '/') {
        // only set up child (and not sibling) iframe
        if (!gadgets.util) {
          return;
        }
        var childIframe = document.getElementById(gadgetId);
        if (!childIframe) {
          throw new Error('Cannot set up gadgets.rpc receiver with ID: ' + gadgetId +
              ', element not found.');
        }
      }

      // The "relay URL" can either be explicitly specified or is set as
      // the child IFRAME URL's origin
      var childSrc = childIframe && childIframe.src;
      var relayUrl = opt_frameurl || gadgets.rpc.getOrigin(childSrc);
      setRelayUrl(gadgetId, relayUrl);

      // The auth token is parsed from child params (rpctoken) or overridden.
      var childParams = gadgets.util.getUrlParameters(childSrc);
      var rpctoken = opt_authtoken || childParams['rpctoken'];
      setAuthToken(gadgetId, rpctoken);
    }

    /**
     * Sets up the gadgets.rpc library to communicate with the receiver.
     * <p>This method replaces setRelayUrl(...) and setAuthToken(...)
     *
     * <p>Simplified instructions - highly recommended:
     * <ol>
     * <li> Generate &lt;iframe id="&lt;ID&gt;" src="...#parent=&lt;PARENTURL&gt;&rpctoken=&lt;RANDOM&gt;"/&gt;
     *      and add to DOM.
     * <li> Call gadgets.rpc.setupReceiver("&lt;ID>");
     *      <p>All parent/child communication initializes automatically from here.
     *         Naturally, both sides need to include the library.
     * </ol>
     *
     * <p>Detailed container/parent instructions:
     * <ol>
     * <li> Create the target IFRAME (eg. gadget) with a given &lt;ID> and params
     *    rpctoken=<token> (eg. #rpctoken=1234), which is a random/unguessbable
     *    string, and parent=&lt;url>, where &lt;url> is the URL of the container.
     * <li> Append IFRAME to the document.
     * <li> Call gadgets.rpc.setupReceiver(&lt;ID>)
     * <p>[Optional]. Strictly speaking, you may omit rpctoken and parent. This
     *             practice earns little but is occasionally useful for testing.
     *             If you omit parent, you MUST pass your container URL as the 2nd
     *             parameter to this method.
     * </ol>
     *
     * <p>Detailed gadget/child IFRAME instructions:
     * <ol>
     * <li> If your container/parent passed parent and rpctoken params (query string
     *    or fragment are both OK), you needn't do anything. The library will self-
     *    initialize.
     * <li> If "parent" is omitted, you MUST call this method with targetId '..'
     *    and the second param set to the parent URL.
     * <li> If "rpctoken" is omitted, but the container set an authToken manually
     *    for this frame, you MUST pass that ID (however acquired) as the 2nd param
     *    to this method.
     * </ol>
     *
     * @member gadgets.rpc
     * @param {string} targetId
     * @param {string=} opt_receiverurl
     * @param {string=} opt_authtoken
     */
    function setupReceiver(targetId, opt_receiverurl, opt_authtoken) {
      if (targetId === '..') {
        // Gadget/IFRAME to container.
        var rpctoken = opt_authtoken || params['rpctoken'] || params['ifpctok'] || '';
        setupContainedContext(rpctoken, opt_receiverurl);
      } else {
        // Container to child.
        setupChildIframe(targetId, opt_receiverurl, opt_authtoken);
      }
    }

    function getReferrer(targetId) {
      if (passReferrerDirection === "bidir" ||
          (passReferrerDirection === "c2p" && targetId === "..") ||
          (passReferrerDirection === "p2c" && targetId !== "..")) {
        var href = window.location.href;
        var lopOff = "?";  // default = origin
        if (passReferrerContents === "query") {
          lopOff = "#";
        } else if (passReferrerContents === "hash") {
          return href;
        }
        var lastIx = href.lastIndexOf(lopOff);
        lastIx = lastIx === -1 ? href.length : lastIx;
        return href.substring(0, lastIx);
      }
      return null;
    }

    return /** @scope gadgets.rpc */ {
      config: function(config) {
        if (typeof config.securityCallback === 'function') {
          securityCallback = config.securityCallback;
        }
      },

      /**
       * Registers an RPC service.
       * @param {string} serviceName Service name to register.
       * @param {function(Object,Object)} handler Service handler.
       *
       * @member gadgets.rpc
       */
      register: function(serviceName, handler) {
        if (serviceName === CALLBACK_NAME || serviceName === ACK) {
          throw new Error('Cannot overwrite callback/ack service');
        }

        if (serviceName === DEFAULT_NAME) {
          throw new Error('Cannot overwrite default service:'
                        + ' use registerDefault');
        }

        services[serviceName] = handler;
      },

      /**
       * Unregisters an RPC service.
       * @param {string} serviceName Service name to unregister.
       *
       * @member gadgets.rpc
       */
      unregister: function(serviceName) {
        if (serviceName === CALLBACK_NAME || serviceName === ACK) {
          throw new Error('Cannot delete callback/ack service');
        }

        if (serviceName === DEFAULT_NAME) {
          throw new Error('Cannot delete default service:'
                        + ' use unregisterDefault');
        }

        delete services[serviceName];
      },

      /**
       * Registers a default service handler to processes all unknown
       * RPC calls which raise an exception by default.
       * @param {function(Object,Object)} handler Service handler.
       *
       * @member gadgets.rpc
       */
      registerDefault: function(handler) {
        services[DEFAULT_NAME] = handler;
      },

      /**
       * Unregisters the default service handler. Future unknown RPC
       * calls will fail silently.
       *
       * @member gadgets.rpc
       */
      unregisterDefault: function() {
        delete services[DEFAULT_NAME];
      },

      /**
       * Forces all subsequent calls to be made by a transport
       * method that allows the caller to verify the message receiver
       * (by way of the parent parameter, through getRelayUrl(...)).
       * At present this means IFPC or WPM.
       * @member gadgets.rpc
       */
      forceParentVerifiable: function() {
        if (!transport.isParentVerifiable()) {
          transport = gadgets.rpctx.ifpc;
        }
      },

      /**
       * Calls an RPC service.
       * @param {string} targetId Module Id of the RPC service provider.
       *                          Empty if calling the parent container.
       * @param {string} serviceName Service name to call.
       * @param {function()|null} callback Callback function (if any) to process
       *                                 the return value of the RPC request.
       * @param {*} var_args Parameters for the RPC request.
       *
       * @member gadgets.rpc
       */
      call: function(targetId, serviceName, callback, var_args) {
        targetId = targetId || '..';
        // Default to the container calling.
        var from = '..';

        if (targetId === '..') {
          from = rpcId;
        } else if (targetId.charAt(0) == '/') {
          // sending to sibling
          from = makeSiblingId(rpcId, gadgets.rpc.getOrigin(window.location.href));
        }

        ++callId;
        if (callback) {
          callbacks[callId] = callback;
        }

        var rpc = {
          's': serviceName,
          'f': from,
          'c': callback ? callId : 0,
          'a': Array.prototype.slice.call(arguments, 3),
          't': authToken[targetId],
          'l': useLegacyProtocol[targetId]
        };

        var referrer = getReferrer(targetId);
        if (referrer) {
          rpc['r'] = referrer;
        }

        if (targetId !== '..' &&
            parseSiblingId(targetId) == null &&  // sibling never in the document
            !document.getElementById(targetId)) {
          // The target has been removed from the DOM. Don't even try.
          return;
        }

        // If target is on the same domain, call method directly
        if (callSameDomain(targetId, rpc)) {
          return;
        }

        // Attempt to make call via a cross-domain transport.
        // Retrieve the transport for the given target - if one
        // target is misconfigured, it won't affect the others.
        // In the case of a sibling relay, channel is not found
        // in the receiverTx map but in the transport itself.
        var channel = receiverTx[targetId];
        if (!channel && parseSiblingId(targetId) !== null) {
          // Sibling-to-sibling communication; use default trasport
          // (in practice, wpm) despite not being ready()-indicated.
          channel = transport;
        }

        if (!channel) {
          // Not set up yet. Enqueue the rpc for such time as it is.
          if (!earlyRpcQueue[targetId]) {
            earlyRpcQueue[targetId] = [rpc];
          } else {
            earlyRpcQueue[targetId].push(rpc);
          }
          return;
        }

        // If we are told to use the legacy format, then we must
        // default to IFPC.
        if (useLegacyProtocol[targetId]) {
          channel = gadgets.rpctx.ifpc;
        }

        if (channel.call(targetId, from, rpc) === false) {
          // Fall back to IFPC. This behavior may be removed as IFPC is as well.
          receiverTx[targetId] = fallbackTransport;
          transport.call(targetId, from, rpc);
        }
      },

      getRelayUrl: getRelayUrl,
      setRelayUrl: setRelayUrl,
      setAuthToken: setAuthToken,
      setupReceiver: setupReceiver,
      getAuthToken: getAuthToken,

      // Note: Does not delete iframe
      removeReceiver: function(receiverId) {
        delete relayUrl[receiverId];
        delete useLegacyProtocol[receiverId];
        delete authToken[receiverId];
        delete setup[receiverId];
        delete sameDomain[receiverId];
        delete receiverTx[receiverId];
      },

      /**
       * Gets the RPC relay mechanism.
       * @return {string} RPC relay mechanism. See above for
       *   a list of supported types.
       *
       * @member gadgets.rpc
       */
      getRelayChannel: function() {
        return transport.getCode();
      },

      /**
       * Receives and processes an RPC request. (Not to be used directly.)
       * Only used by IFPC.
       * @param {Array.<string>} fragment An RPC request fragment encoded as
       *        an array. The first 4 elements are target id, source id & call id,
       *        total packet number, packet id. The last element stores the actual
       *        JSON-encoded and URI escaped packet data.
       *
       * @member gadgets.rpc
       * @deprecated
       */
      receive: function(fragment, otherWindow) {
        if (fragment.length > 4) {
          transport._receiveMessage(fragment, process);
        } else {
          relayOnload.apply(null, fragment.concat(otherWindow));
        }
      },

      /**
       * Receives and processes an RPC request sent via the same domain.
       * (Not to be used directly). Converts the inbound rpc object's
       * Array into a local Array to pass the process() Array test.
       * @param {Object} rpc RPC object containing all request params.
       * @member gadgets.rpc
       */
      receiveSameDomain: function(rpc) {
        // Pass through to local process method but converting to a local Array
        rpc['a'] = Array.prototype.slice.call(rpc['a']);
        window.setTimeout(function() { process(rpc); }, 0);
      },

      // Helper method to get the protocol://host:port of an input URL.
      // see docs above
      getOrigin: getOrigin,
      getTargetOrigin: getTargetOrigin,

      /**
       * Internal-only method used to initialize gadgets.rpc.
       * @member gadgets.rpc
       */
      init: function() {
        // Conduct any global setup necessary for the chosen transport.
        // Do so after gadgets.rpc definition to allow transport to access
        // gadgets.rpc methods.
        if (transport.init(process, transportReady) === false) {
          transport = fallbackTransport;
        }
        if (isChild) {
          setupReceiver('..');
        } else {
          gadgets.config.register('rpc', null, function(config) { setReferrerConfig(config['rpc'] || {}); });
        }
      },

      /** Returns the window keyed by the ID. null/".." for parent, else child */
      _getTargetWin: getTargetWin,

      /** Parses a sibling id into {id: <siblingId>, origin: <siblingOrigin>} */
      _parseSiblingId: parseSiblingId,

      ACK: ACK,

      RPC_ID: rpcId || "..",

      SEC_ERROR_LOAD_TIMEOUT: LOAD_TIMEOUT,
      SEC_ERROR_FRAME_PHISH: FRAME_PHISH,
      SEC_ERROR_FORGED_MSG: FORGED_MSG
    };
  }();

  // Initialize library/transport.
  gadgets.rpc.init();

} // !end of double-inclusion guard
