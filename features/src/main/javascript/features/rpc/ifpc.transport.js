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

/*
 * For all others, we have a fallback mechanism known as "ifpc". IFPC
 * exploits the fact that while same-origin policy prohibits a frame from
 * accessing members on a window not in the same domain, that frame can,
 * however, navigate the window heirarchy (via parent). This is exploited by
 * having a page on domain A that wants to talk to domain B create an iframe
 * on domain B pointing to a special relay file and with a message encoded
 * after the hash (#). This relay, in turn, finds the page on domain B, and
 * can call a receipt function with the message given to it. The relay URL
 * used by each caller is set via the gadgets.rpc.setRelayUrl(..) and
 * *must* be called before the call method is used.
 *
 *   ifpc: Iframe-based method, utilizing a relay page, to send a message.
 *      - No known major browsers still use this method, but it remains
 *        useful as a catch-all fallback for the time being.
 */
if (!gadgets.rpctx.ifpc) {  // make lib resilient to double-inclusion

  gadgets.rpctx.ifpc = function() {
    var iframePool = [];
    var callId = 0;
    var ready;

    var URL_LIMIT = 2000;
    var messagesIn = {};

    /**
   * Encodes arguments for the legacy IFPC wire format.
   *
   * @param {Object} args
   * @return {string} the encoded args.
   */
    function encodeLegacyData(args) {
      var argsEscaped = [];
      for (var i = 0, j = args.length; i < j; ++i) {
        argsEscaped.push(encodeURIComponent(gadgets.json.stringify(args[i])));
      }
      return argsEscaped.join('&');
    }

    /**
   * Helper function to emit an invisible IFrame.
   * @param {string} src SRC attribute of the IFrame to emit.
   * @private
   */
    function emitInvisibleIframe(src) {
      var iframe;
      // Recycle IFrames
      for (var i = iframePool.length - 1; i >= 0; --i) {
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
      window.setTimeout(function() { document.body.appendChild(iframe); }, 0);
    }

    function isMessageComplete(arr, total) {
      for (var i = total - 1; i >= 0; --i) {
        if (typeof arr[i] === 'undefined') {
          return false;
        }
      }
      return true;
    }

    return {
      getCode: function() {
        return 'ifpc';
      },

      isParentVerifiable: function() {
        return true;
      },

      init: function(processFn, readyFn) {
        // No global setup.
        ready = readyFn;
        ready('..', true);  // Ready immediately.
        return true;
      },

      setup: function(receiverId, token) {
        // Indicate readiness to send to receiver.
        ready(receiverId, true);
        return true;
      },

      call: function(targetId, from, rpc) {
        // Retrieve the relay file used by IFPC. Note that
        // this must be set before the call, and so we conduct
        // an extra check to ensure it is not blank.
        var relay = gadgets.rpc.getRelayUrl(targetId);
        ++callId;

        if (!relay) {
          gadgets.warn('No relay file assigned for IFPC');
          return false;
        }

        // The RPC mechanism supports two formats for IFPC (legacy and current).
        var src = null,
            queueOut = [];
        if (rpc.l) {
          // Use legacy protocol.
          // Format: #iframe_id&callId&num_packets&packet_num&block_of_data
          var callArgs = rpc['a'];
          src = [relay, '#', encodeLegacyData([from, callId, 1, 0,
            encodeLegacyData([from, rpc['s'], '', '', from].concat(
                callArgs))])].join('');
          queueOut.push(src);
        } else {
          // Format: #targetId & sourceId@callId & packetNum & packetId & packetData
          src = [relay, '#', targetId, '&', from, '@', callId, '&'].join('');
          var message = encodeURIComponent(gadgets.json.stringify(rpc)),
              payloadLength = URL_LIMIT - src.length,
              numPackets = Math.ceil(message.length/payloadLength),
              packetIdx = 0,
              part;
          while (message.length > 0) {
            part = message.substring(0, payloadLength);
            message = message.substring(payloadLength);
            queueOut.push([src, numPackets, '&', packetIdx, '&', part].join(''));
            packetIdx += 1;
          }
        }

        // Conduct the IFPC call by creating the Iframe with
        // the relay URL and appended message.
        do {
          emitInvisibleIframe(queueOut.shift());
        } while(queueOut.length > 0);
        return true;
      },

      /** Process message from invisible iframe, merging message parts if necessary. */
      _receiveMessage: function(fragment, process) {
        var from = fragment[1],   // in the form of "<from>@<callid>"
            numPackets = parseInt(fragment[2], 10),
            packetIdx = parseInt(fragment[3], 10),
            payload = fragment[fragment.length - 1],
            completed = numPackets === 1;

        // if message is multi-part, store parts in the proper order
        if (numPackets > 1) {
          if (!messagesIn[from]) {
            messagesIn[from] = [];
          }
          messagesIn[from][packetIdx] = payload;
          // check if all parts have been sent
          if (isMessageComplete(messagesIn[from], numPackets)) {
            payload = messagesIn[from].join('');
            delete messagesIn[from];
            completed = true;
          }
        }

        // complete message sent
        if (completed) {
          process(gadgets.json.parse(decodeURIComponent(payload)));
        }
      }
    };
  }();

} // !end of double inclusion guard
