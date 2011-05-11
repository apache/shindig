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
 * Transport for browsers that support native messaging (various implementations
 * of the HTML5 postMessage method). Officially defined at
 * http://www.whatwg.org/specs/web-apps/current-work/multipage/comms.html.
 *
 * postMessage is a native implementation of XDC. A page registers that
 * it would like to receive messages by listening the the "message" event
 * on the window (document in DPM) object. In turn, another page can
 * raise that event by calling window.postMessage (document.postMessage
 * in DPM) with a string representing the message and a string
 * indicating on which domain the receiving page must be to receive
 * the message. The target page will then have its "message" event raised
 * if the domain matches and can, in turn, check the origin of the message
 * and process the data contained within.
 *
 *   wpm: postMessage on the window object.
 *      - Internet Explorer 8+
 *      - Safari 4+
 *      - Chrome 2+
 *      - Webkit nightlies
 *      - Firefox 3+
 *      - Opera 9+
 */
if (!gadgets.rpctx.wpm) {  // make lib resilient to double-inclusion

  gadgets.rpctx.wpm = function() {
    var process, ready;

    function attachBrowserEvent(eventName, callback, useCapture) {
      if (typeof window.addEventListener != 'undefined') {
        window.addEventListener(eventName, callback, useCapture);
      } else if (typeof window.attachEvent != 'undefined') {
        window.attachEvent('on' + eventName, callback);
      }
    }

    function removeBrowserEvent(eventName, callback, useCapture) {
      if (window.removeEventListener) {
        window.removeEventListener(eventName, callback, useCapture);
      } else if (window.detachEvent) {
        window.detachEvent('on' + eventName, callback);
      }
    }

    function onmessage(packet) {
      var rpc = gadgets.json.parse(packet.data);
      if (!rpc || !rpc['f']) {
        return;
      }

      // for security, check origin against expected value
      var origin = gadgets.rpc.getTargetOrigin(rpc['f']);

      // Opera's "message" event does not have an "origin" property (at least,
      // it doesn't in version 9.64;  presumably, it will in version 10).  If
      // event.origin does not exist, use event.domain.  The other difference is that
      // while event.origin looks like <scheme>://<hostname>:<port>, event.domain
      // consists only of <hostname>.
      if (typeof packet.origin !== "undefined" ? packet.origin !== origin :
          packet.domain !== /^.+:\/\/([^:]+).*/.exec(origin)[1]) {
        return;
      }
      process(rpc, packet.origin);
    }

    return {
      getCode: function() {
        return 'wpm';
      },

      isParentVerifiable: function() {
        return true;
      },

      init: function(processFn, readyFn) {
        process = processFn;
        ready = readyFn;

        // Set up native postMessage handler.
        attachBrowserEvent('message', onmessage, false);

        ready('..', true);  // Immediately ready to send to parent.
        return true;
      },

      setup: function(receiverId, token) {
        // Indicate that we're ready to send to the given receiver.
        ready(receiverId, true);
        return true;
      },

      call: function(targetId, from, rpc) {
        // targetOrigin = canonicalized relay URL
        var origin = gadgets.rpc.getTargetOrigin(targetId);
        var targetWin = gadgets.rpc._getTargetWin(targetId);
        if (origin) {
          // Some browsers (IE, Opera) have an implementation of postMessage that is
          // synchronous, although HTML5 specifies that it should be asynchronous.  In
          // order to make all browsers behave consistently, we wrap all postMessage
          // calls in a setTimeout with a timeout of 0.
          window.setTimeout(function() {
              targetWin.postMessage(gadgets.json.stringify(rpc), origin); }, 0);
        } else {
          gadgets.error('No relay set (used as window.postMessage targetOrigin)' +
              ', cannot send cross-domain message');
        }
        return true;
      }
    };
  }();

} // !end of double-inclusion guard
